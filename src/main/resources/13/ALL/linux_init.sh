#!/bin/bash
mkdir -p /var/log/init_scripts
mkdir -p /var/log/user_scripts
initLog="/var/log/init_scripts/init_$(date +"%Y_%m_%d_%I_%M_%p").log"
userScriptLog="/var/log/user_scripts/user_$(date +"%Y_%m_%d_%I_%M_%p").log"

exec &>> "$initLog"
set -x

#--------------------------------------------------------------------------
# Variables
#--------------------------------------------------------------------------
CHEF_CLIENT_VER=18.2.7

CONF_URL=@VAR_CONFIG_URL
VIRT_TYPE=@VAR_VIRT_TYPE
NODENAME=@VAR_NODENAME
ACS_ENABLE=@VAR_ACS_ENABLE
CHEF_PROJECT=@VAR_PROJECT_CHEF
CHEF_SRV=@VAR_CHEF_SERVER
CHEF_ENV=@VAR_CHEF_ENV
CHEF_ORG=@VAR_CHEF_ORG_NAME
SSH_USER=@VAR_SSH_USER
USER_SCRIPT='@VAR_USER_SCRIPT'
NOTIF_URL=@VAR_NOTIF_URL
OS_CHECKSUM=@VAR_NOTIF_CHECKSUM
LUMINATE_SCRIPT=@VAR_LUMINATE_SCRIPT
STORAGE_URL=@VAR_STORAGE_URL 
CHEF_CERTS_URL=@VAR_CHEF_CERTS_URL

if [[ "$NODENAME" == "@VAR"* ]]; then
    NODENAME=$(curl -sS http://169.254.169.254/latest/meta-data/instance-id)
fi

CHEF_HANDLER_URL="$STORAGE_URL/chef/start_handler.rb?name=$NODENAME"

if [[ "$LUMINATE_SCRIPT" != "@VAR"* ]]; then
    #curl -sSLo - "$CONF_URL$LUMINATE_SCRIPT" | bash -s --
cat > /lib/systemd/system/luminate.service << EOF
[Unit]
Description=Install Luminate
[Service]
Type=simple
ExecStart=/bin/bash -c '/usr/bin/curl -sSLo - $CONF_URL$LUMINATE_SCRIPT | /bin/bash -s --'

[Install]
WantedBy=multi-user.target
EOF

cat > /lib/systemd/system/luminate.timer << EOF
[Unit]
Description=Execute every time after boot
After=sshd.service
Requires=sshd.service

[Timer]
OnActiveSec=20
Unit=luminate.service

[Install]
WantedBy=multi-user.target
EOF
systemctl start luminate.timer
fi

serviceAction () {
if [ -e /usr/bin/systemctl ]; then
    /usr/bin/systemctl $2 $1.service
elif [ -e /bin/systemctl ]; then
    /bin/systemctl $2 $1.service
elif [ -e /usr/sbin/service ]; then
    /usr/sbin/service $1 $2
elif [ -e /sbin/service ]; then
    /sbin/service $1 $2
else
    /etc/init.d/$1 $2
fi
}

if [ ! -e /etc/ssh/sshd_config.d ]; then
    mkdir /etc/ssh/sshd_config.d
fi
echo "CASignatureAlgorithms ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa" > /etc/ssh/sshd_config.d/CASignatureAlgorithms.conf
serviceAction sshd reload

#--------------------------------------------------------------------------
# Enable auto configuration
#--------------------------------------------------------------------------
if [ "$ACS_ENABLE" == "true" ] && [ ! -f '/etc/chef/client.pem' ]; then
    rm -rf /etc/chef/
    #--------------------------------------------------------------------------
    # Download and Install Chef-Client
    #--------------------------------------------------------------------------

    # official Chef Software Install Script -> https://docs.chef.io/chef_install_script/
    curl -Lk --retry 3 --retry-delay 5 --max-time 15 https://omnitruck.chef.io/install.sh | bash -s -- -v "$CHEF_CLIENT_VER"
    mkdir -p /var/log/chef/ /etc/epconfig /etc/chef/ /etc/chef/trusted_certs/
    
    #--------------------------------------------------------------------------
    # Download and store $CHEF_ORG-validator.pem and Chef-Server certificate
    #--------------------------------------------------------------------------
    result_code=$(curl -k --retry 3 --retry-delay 5 --max-time 15 -w "%{http_code}" -o certsbundle -s "$CHEF_CERTS_URL/$NODENAME")
    counter=0
    while [[ "$result_code" != "200" && $counter -lt 3 ]]; do
        sleep 60
        counter=$((counter + 1))
        result_code=$(curl -k --retry 3 --retry-delay 5 --max-time 15 -w "%{http_code}" -o certsbundle -s "$CHEF_CERTS_URL/$NODENAME")
    done
    
    awk '{print $0 > "/etc/chef/file" NR}' RS='\\;' certsbundle
    rm certsbundle
    mv /etc/chef/file1 /etc/chef/$CHEF_ORG-validator.pem
    mv /etc/chef/file2 /etc/chef/file_with_databag_secret
    
    #--------------------------------------------------------------------------
    # Create role.json and client.rb
    #--------------------------------------------------------------------------
    echo "{ \"run_list\": [ \"role[base]\" ] }" >/etc/chef/role.json

    cat << EOF >> /etc/chef/client.rb
chef_server_url "https://$CHEF_SRV/organizations/$CHEF_ORG"
node_name  "$NODENAME"
log_location "/var/log/chef/client.log"
log_level  :info
validation_client_name "$CHEF_ORG-validator"
validation_key "/etc/chef/$CHEF_ORG-validator.pem"
verify_api_cert true

begin
  require "/etc/chef/start_handler.rb"
  start_handlers << Chef::EpHandler::StartHandler.new()
rescue LoadError => e
  Chef::Log.warn e
end
EOF
    
    [ "$CHEF_PROJECT" == "false" ] && echo "environment  \"$CHEF_ENV\"" >>/etc/chef/client.rb
    /opt/chef/bin/knife ssl fetch -c /etc/chef/client.rb

    #--------------------------------------------------------------------------
    # Create Scheduler to run Chef-client and Run it
    #--------------------------------------------------------------------------
    delay=$((RANDOM % 4 + 4))
    echo "*/$delay * * * * /usr/bin/chef-client --once" | crontab -
    sleep 20
    
    if [ "$CHEF_PROJECT" = "false" ]; then
        echo "{\"eporch2\":{\"api_path\":\"$CONF_URL\"},\"run_list\":[\"role[base]\"],\"chef_environment\":\"$CHEF_ENV\"}" >/etc/chef/data.json
    else
        echo "{\"eporch2\":{\"api_path\":\"$CONF_URL\"},\"run_list\":[\"role[base]\"]}" >/etc/chef/data.json
    fi
    
    curl -sSo - "$CHEF_HANDLER_URL" > "$CHEF_BASE/start_handler.rb"
    
    cat > /lib/systemd/system/chef-client-run.service << EOF
[Unit]
Description=Run chef-client
[Service]
Type=simple
ExecStart=/bin/bash -c "/usr/bin/chef-client -j /etc/chef/data.json --once --chef-license accept" 

[Install]
WantedBy=multi-user.target
EOF

    cat > /lib/systemd/system/chef-client-run.timer << EOF
[Unit]
Description=Execute every time after boot

[Timer]
OnActiveSec=1
Unit=chef-client-run.service

[Install]
WantedBy=multi-user.target
EOF
    systemctl start chef-client-run.timer    
    
    #/usr/bin/chef-client -j /etc/chef/data.json --once --chef-license accept &
fi

#--------------------------------------------------------------------------
# Enable if 
#--------------------------------------------------------------------------
if [ "$VIRT_TYPE" == "OPEN_STACK" ]; then
    #--------------------------------------------------------------------------
    # Preinstall user certs
    #--------------------------------------------------------------------------
    # if [[ "$SSH_USER" != "@VAR"* ]]; then
    #     # create default user
    #     useradd ${SSH_USER} --create-home --shell /bin/bash --user-group --skel /etc/skel
    #     curl -s http://169.254.169.254/openstack/latest/meta_data.json | grep -o '"data": "[^"]*' | grep -o '[^"]*$' >>/home/${SSH_USER}/.ssh/authorized_keys
    #     chmod 600 /home/${SSH_USER}/.ssh/authorized_keys
    #     chown ${SSH_USER}. /home/${SSH_USER} -R
    #     echo "${SSH_USER}  ALL=(ALL:ALL) NOPASSWD: ALL" >>/etc/sudoers
    # fi
    # #--------------------------------------------------------------------------
    # # Join to AD
    # #--------------------------------------------------------------------------
    # python /etc/check_meta.py
    #--------------------------------------------------------------------------
    # Confirm stating
    #--------------------------------------------------------------------------
    UUID=$(curl -s http://169.254.169.254/openstack/latest/meta_data.json | grep -o '"uuid": "[^"]*' | grep -o '[^"]*$')
    curl "${NOTIF_URL}?name=${NODENAME}&uuid=${UUID}&checksum=${OS_CHECKSUM}"
fi

#--------------------------------------------------------------------------
# If User script exist
#--------------------------------------------------------------------------
if [[ "$USER_SCRIPT" != "@VAR"* ]]; then
    ((ITER = 0))
    mkdir -p /var/log/user_scripts
    IFS=';' read -ra user_scripts <<< "$USER_SCRIPT"
    for script_str in "${user_scripts[@]}"
    do
        IFS=':' read -ra script <<< "$script_str"
        IFS='#' read -ra params <<< "${script[1]}"
        cat > /lib/systemd/system/USER_SCRIPT$ITER.service << EOF
[Unit]
Description=Install USER_SCRIPT$ITER
[Service]
Type=simple
ExecStart=/bin/bash -c "/bin/bash -s ${params[@]} < <(/usr/bin/curl -o - $CONF_URL${script[0]}) &> $userScriptLog"


[Install]
WantedBy=multi-user.target
EOF

cat > /lib/systemd/system/USER_SCRIPT$ITER.timer << EOF
[Unit]
Description=Execute every time after boot
After=sshd.service
Requires=sshd.service

[Timer]
OnActiveSec=20
Unit=USER_SCRIPT$ITER.service

[Install]
WantedBy=multi-user.target
EOF
        systemctl start USER_SCRIPT$ITER.timer
    ((ITER=ITER+1))
    done
fi

if [[ "$LUMINATE_SCRIPT" != "@VAR"* ]]; then
    sleep 120
    rm -f /lib/systemd/system/luminate.service
    rm -f /lib/systemd/system/luminate.timer
fi

if [[ "$USER_SCRIPT" != "@VAR"* ]]; then
    sleep 120
    ((ITER = 0))
    for script_str in "${user_scripts[@]}"
    do
       rm -f /lib/systemd/system/USER_SCRIPT$ITER.timer
       rm -f /lib/systemd/system/USER_SCRIPT$ITER.service
       ((ITER=ITER+1))
    done
   systemctl daemon-reload
fi
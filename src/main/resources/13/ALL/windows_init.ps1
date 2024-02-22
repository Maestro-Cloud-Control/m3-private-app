Start-Transcript -Path C:\transcript.txt

#--------------------------------------------------------------------------
# Global Variables
#--------------------------------------------------------------------------
$CHEF_CLIENT_VER = "18.2.7"

$EnableLog = "true"
$CHEF_BASE = "C:\chef"
$PathToLogFile = "C:\epconfig.log"
$PathToUserLogFile = "C:\user_script.log"
$ChefRole = @("role[base]")

$NODENAME = "@VAR_NODENAME"
$CONF_URL = "@VAR_CONFIG_URL"

if ($NODENAME -match "@") {
  $NODENAME = (New-Object System.Net.WebClient).DownloadString('http://169.254.169.254/latest/meta-data/instance-id')
}

$ACS_ENABLE = "@VAR_ACS_ENABLE"
$PROJECT_CHEF = "@VAR_PROJECT_CHEF"
$CHEF_SRV = "@VAR_CHEF_SERVER"
$CHEF_ENV = "@VAR_CHEF_ENV"
$CHEF_ORG = "@VAR_CHEF_ORG_NAME"
$USER_SCRIPT = "@VAR_USER_SCRIPT"
$STORAGE_URL="@VAR_STORAGE_URL"
$CHEF_CERTS_URL="@VAR_CHEF_CERTS_URL"

$CHEF_HANDLER_URL = "$STORAGE_URL/chef/start_handler.rb?name=$NODENAME"
$DelPass_URL = "$CONF_URL/api/openstack/metadata/admin?name=$NODENAME"

#----------------------------------------------------------------------------
# Write-EpLog - replaces Write-Host and optionally logs
#----------------------------------------------------------------------------
function Write-EpLog () {
  [CmdletBinding()]
  param (
    [Parameter(Position = 0)][String]$Message,
    [Parameter(Position = 1)][String]$LogLevel = "INFO"
  )
  if ($EnableLog -eq "true") {
    $now = Get-Date
    $FormattedDate = $now.ToString("yyyy-MM-dd HH:mm:ss")
    # write to the console
    Switch ( $LogLevel ) {
      "INFO" {
        Write-Host "$Message"
      }
      "ERROR" {
        Write-Host "Error:  $Message" -ForegroundColor "red"
      }
      "WARN" {
        Write-Host "Warning: $Message" -ForegroundColor "yellow"
      }
      "SUCCESS" {
        Write-Host "$Message" -ForegroundColor "green"
      }
      default {
        Write-Host "$Message" -ForegroundColor "red"
      }
    }

    # Update log file
    if ( $PathToLogFile ) {
      try {
        Add-Content $PathToLogFile "$FormattedDate - $LogLevel - $Message"
      }
      catch {
        $ErrorMessage = $_.Exception.Message
        Write-Host $ErrorMessage -ForegroundColor "red"
      }
    }
  }
}

function Test-ScheduledTask {
  param (
    $TaskName
  )
  Get-ScheduledTask | Where-Object { $_.TaskName -like $TaskName }
}

function Install-ChefClient {
  [CmdletBinding()]
  param(
    [Parameter()]
    [string] $Version
  )

  # official Chef Software Install Script -> https://omnitruck.chef.io/install.ps1
  . { Invoke-WebRequest -useb https://omnitruck.chef.io/install.ps1 } | Invoke-Expression; install -v $Version
}

# Add scheduled task
function Add-Schedule ( $TaskName, $cmdParms ) {
  if ( Test-ScheduledTask $TaskName ) {
    Write-EpLog "Scheduler task $TaskName already added, skip."
  }
  else {
    Write-EpLog "Create Scheduler to run $TaskName"
    # TODO
    # review schedule
    $schedule_args = @(
      "/Create"
      "/TN"
      $TaskName
      "/SC"
      "DAILY"
      "/DU"
      "0023:59"
      "/RI"
      "5"
      "/RL"
      "HIGHEST"
      "/RU"
      "system"
      "/TR"
      "`"'%windir%\System32\cmd.exe' /c $cmdParms`""
    )
    Start-Process "schtasks" -ArgumentList $schedule_args -Wait -NoNewWindow -Passthru
    Write-EpLog "Scheduler for $TaskName created"
  }
}

# --------------------------------------------------------------------
# Begin
# --------------------------------------------------------------------

# Force use TLS 1.2
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
# Fix "The server committed a protocol violation. Section=ResponseHeader Detail=CR must be followed by LF"
# https://codeday.me/en/qa/20190306/13456.html
try {
  $netAssembly = [Reflection.Assembly]::GetAssembly([System.Net.Configuration.SettingsSection])
  if ( $netAssembly ) {
    $bindingFlags = [Reflection.BindingFlags] "Static,GetProperty,NonPublic"
    $settingsType = $netAssembly.GetType("System.Net.Configuration.SettingsSectionInternal")
    $instance = $settingsType.InvokeMember("Section", $bindingFlags, $null, $null, @())
    if ( $instance ) {
      $bindingFlags = "NonPublic", "Instance"
      $useUnsafeHeaderParsingField = $settingsType.GetField("useUnsafeHeaderParsing", $bindingFlags)
      if ( $useUnsafeHeaderParsingField ) {
        $useUnsafeHeaderParsingField.SetValue($instance, $true)
      }
    }
  }
}
catch {
  $ErrorMessage = $_.Exception.Message
  Write-EpLog $ErrorMessage -LogLevel "ERROR"
}

if ($ACS_ENABLE -eq "true" -or -Not ($USER_SCRIPT -Like '@VAR*')) {
  Invoke-WebRequest -Uri 'https://download.sysinternals.com/files/PSTools.zip' -OutFile $env:TEMP\PSTools.zip
  Expand-Archive $env:TEMP\PSTools.zip -DestinationPath $env:TEMP\PSTools -Force
  Write-EpLog "PSTools downloaded and installed" -LogLevel "SUCCESS"
}

#----------------------------------------------------------------------------
# Refreshing chef base folder
#----------------------------------------------------------------------------
if ( $ACS_ENABLE -eq "true" ) {
  try {
    if ( Get-ChildItem -Path $CHEF_BASE -ErrorAction SilentlyContinue ) {
      Write-EpLog "Folder $CHEF_BASE contains files."
    }
    else {
      Write-EpLog "Folder $CHEF_BASE is empty."
      Remove-Item -Force -Recurse -path $CHEF_BASE -ErrorAction SilentlyContinue
      Write-EpLog "Path $CHEF_BASE has been deleted." "SUCCESS"
      New-Item $CHEF_BASE -type directory
      Write-EpLog "Path $CHEF_BASE has been created."
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }

  #--------------------------------------------------------------------------
  # Downloading and Installing Chef-Client
  #--------------------------------------------------------------------------
  Write-EpLog "Download and Install Chef-Client"
  try {
    if ( Get-Service 'Chef-Client' -ErrorAction SilentlyContinue ) {
      Write-EpLog "Service Chef-Client exist, skipping." -LogLevel "WARN"
    }
    else {
      Write-EpLog "Chef-Client installation started."
      Install-ChefClient -Version $CHEF_CLIENT_VER
      Write-EpLog "Chef-Client installed."
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }

  #--------------------------------------------------------------------------
  # Download organization validator key and Chef-Server certificate
  #--------------------------------------------------------------------------
  Write-EpLog "Downloading $CHEF_ORG-validator.pem and Chef-Server certificate"
  try {
    if (!( Test-Path "$CHEF_BASE\$CHEF_ORG-validator.pem" )) {
      $certs = (New-Object System.Net.WebClient).DownloadString("$CHEF_CERTS_URL")
      $certs = $certs.Split(";")
      if ( $certs[0] -like "-----BEGIN RSA PRIVATE KEY-----*" ) {
        Out-File -InputObject $certs[0] -FilePath "$CHEF_BASE\$CHEF_ORG-validator.pem" -Encoding ascii
        Out-File -InputObject $certs[1] -FilePath "$CHEF_BASE\file2" -Encoding ascii
        Out-File -InputObject $certs[1] -FilePath "$CHEF_BASE\file_with_databag_secret" -Encoding ascii
      }
      if ( Test-Path "$CHEF_BASE\$CHEF_ORG-validator.pem" ) {
        Write-EpLog "File $CHEF_BASE\$CHEF_ORG-validator.pem has been created." -LogLevel "SUCCESS"
      }
      else {
        Write-EpLog "File $CHEF_ORG-validator.pem does not exist" -LogLevel "ERROR"
      }
    }
    else {
      Write-EpLog "File $CHEF_BASE\$CHEF_ORG-validator.pem exist, skipping ." -LogLevel "WARN"
    }

    if ( $PROJECT_CHEF -eq "true" ) {
      if (!( Test-Path "$CHEF_BASE\trusted_certs\$CHEF_SRV.crt" )) {
        if ( $certs[1] -like "-----BEGIN CERTifICATE-----*" ) {
          New-Item $CHEF_BASE\trusted_certs -type directory
          Out-File -InputObject $certs[1] -FilePath "$CHEF_BASE\trusted_certs\$CHEF_SRV.crt" -Encoding ascii
        }
        if ( Test-Path "$CHEF_BASE\trusted_certs\$CHEF_SRV.crt" ) {
          Write-EpLog "File $CHEF_BASE\trusted_certs\$CHEF_SRV.crt has been created." -LogLevel "SUCCESS"
        }
        else {
          Write-EpLog "File $CHEF_BASE\trusted_certs\$CHEF_SRV.crt does not exist" -LogLevel "ERROR"
        }
      }
      else {
        Write-EpLog "File $CHEF_BASE\trusted_certs\$CHEF_SRV.crt exist, skipping ." -LogLevel "WARN"
      }
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }

  #--------------------------------------------------------------------------
  # Downloading start handler
  #--------------------------------------------------------------------------
  try {
    if (!( Test-Path "$CHEF_BASE\start_handler.rb" )) {
      $handler = (New-Object System.Net.WebClient).DownloadString("$CHEF_HANDLER_URL")
      Out-File -InputObject $handler -FilePath "$CHEF_BASE\start_handler.rb" -Encoding ascii
      Write-EpLog "File $CHEF_BASE\start_handler.rb has been created." -LogLevel "SUCCESS"
    }
    else {
      Write-EpLog "File $CHEF_BASE\start_handler.rb, skipping ." -LogLevel "WARN"
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }
  #--------------------------------------------------------------------------
  # Generating client.rb
  #--------------------------------------------------------------------------
  Write-EpLog "Generating client.rb"
  try {
    if (!( Test-Path "$CHEF_BASE\client.rb" )) {
      @(
        "chef_server_url 'https://$CHEF_SRV/organizations/$CHEF_ORG'"
        "node_name '$NODENAME'"
        "validation_client_name '$CHEF_ORG-validator'"
        "validation_key '$CHEF_BASE\$CHEF_ORG-validator.pem'"
        "log_location '$CHEF_BASE\client.log'"
        "log_level :info"
        "begin"
        "  require '$CHEF_BASE\start_handler.rb'"
        "  start_handlers << Chef::EpHandler::StartHandler.new()"
        "rescue LoadError => e"
        "  Chef::Log.warn e"
        "end"
        if ( $PROJECT_CHEF -ne "true" ) {
          "environment '$CHEF_ENV'"
          "verify_api_cert true"
        }
        else {
          "ssl_verify_mode :verify_none"
        }
      ) | `
        Out-File -FilePath "$CHEF_BASE\client.rb" -Encoding ascii
      if ( Test-Path "$CHEF_BASE\client.rb" ) {
        Write-EpLog "File $CHEF_BASE\client.rb has been created." -LogLevel "SUCCESS"
      }
      else {
        Write-EpLog "File does not created $CHEF_BASE\client.rb" -LogLevel "ERROR"
      }
    }
    else {
      Write-EpLog "File $CHEF_BASE\client.rb exist, skipping" -LogLevel "WARN"
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }
  #--------------------------------------------------------------------------
  # Fetch SSL certificates
  #--------------------------------------------------------------------------
  if ( $PROJECT_CHEF -eq "true" ) {
    Write-EpLog "Fetch SSL certificates"
    try {
      $ssl_fetch_bin = "c:\opscode\chef\bin\knife.bat"
      $ssl_fetch_args = @(
        "ssl"
        "fetch"
        "-c"
        ('"{0}\client.rb"' -f $CHEF_BASE)
      )
      $ssl_fetch_exitcode = (Start-Process $ssl_fetch_bin -ArgumentList $ssl_fetch_args -Wait -NoNewWindow -Passthru).ExitCode
      if (!( $ssl_fetch_exitcode -eq 0 )) {
        Start-Sleep -s 60
        $ssl_fetch_exitcode = (Start-Process $ssl_fetch_bin -ArgumentList $ssl_fetch_args -Wait -NoNewWindow -Passthru).ExitCode
      }
    }
    catch {
      $ErrorMessage = $_.Exception.Message
      Write-EpLog $ErrorMessage -LogLevel "ERROR"
    }
  }
  #--------------------------------------------------------------------------
  # Create data.json
  #--------------------------------------------------------------------------
  Write-EpLog "Create data.json"
  try {
    if (!( Test-Path "$CHEF_BASE\data.json" )) {
      @{
        "eporch2"          = @{
          "api_path" = $CONF_URL
        }
        "run_list"         = $ChefRole
        "chef_environment" = $(if ($PROJECT_CHEF -eq "true") {
          "_default"
        }
        else {
          $CHEF_ENV
        })
      } | `
        ConvertTo-Json | Out-File -FilePath "$CHEF_BASE\data.json" -Encoding ascii
      if ( Test-Path "$CHEF_BASE\data.json" ) {
        Write-EpLog "File $CHEF_BASE\data.json has been created." -LogLevel "SUCCESS"
      }
      else {
        Write-EpLog "File Does not create $CHEF_BASE\data.json" -LogLevel "ERROR"
      }
    }
    else {
      Write-EpLog "File $CHEF_BASE\data.json exist, skipping" -LogLevel "WARN"
    }
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }
  #--------------------------------------------------------------------------
  # Create Scheduler to run Chef-client
  #--------------------------------------------------------------------------
  try {
    Add-Schedule -TaskName "Chef-Client" -cmdParms "C:\opscode\chef\embedded\bin\ruby.exe C:\opscode\chef\bin\chef-client --config C:\chef\client.rb"
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }
  #--------------------------------------------------------------------------
  # Run chef-client as system user first time with data json
  #--------------------------------------------------------------------------
  try {
    Start-Process -FilePath "$env:TEMP\PSTools\PsExec64.exe" -ArgumentList '-s -accepteula C:\opscode\chef\embedded\bin\ruby.exe C:\opscode\chef\bin\chef-client --chef-license accept-silent --logfile C:\chef\client.log --config C:\chef\client.rb -j C:\chef\data.json' -Wait -NoNewWindow
    Write-EpLog "Chef client executed" -LogLevel "SUCCESS"
  }
  catch {
    $ErrorMessage = $_.Exception.Message
    Write-EpLog $ErrorMessage -LogLevel "ERROR"
  }
}

try {
  Write-EpLog "Removing admin password"
  Invoke-WebRequest -UseBasicParsing -Method DELETE -Uri $DelPass_URL
}
catch {
  $ErrorMessage = $_.Exception.Message
  Write-EpLog $ErrorMessage -LogLevel "ERROR"
}

# Execute user script
try {
  if (-Not ($USER_SCRIPT -Like '@VAR*')) {
    $USER_SCRIPT.Split(';') | ForEach-Object {
      $script = $_.Split(':')
      $script_url = $script[0]
      $script_name = $script_url.Split('/')[-1]
      if ([bool]$script[1]) {
        $params = $script[1].Split('#')
      } else {
        $params = @()
      }
      (New-Object System.Net.WebClient).DownloadFile( "$CONF_URL$script_url", "$env:TEMP\$script_name" )
      $pinfo = New-Object System.Diagnostics.ProcessStartInfo
      $pinfo.FileName = "$env:TEMP\PSTools\PsExec64.exe"
      if ($script_name -match '.*\.(bat|cmd)$') {
        $pinfo.Arguments = "-s -accepteula -w $env:TEMP cmd /c $script_name $([string]$params)"
      } else {
        $pinfo.Arguments = "-s -accepteula -w $env:TEMP powershell -ExecutionPolicy Bypass -File .\$script_name $([string]$params)"
      }
      $pinfo.RedirectStandardOutput = $true
      $pinfo.UseShellExecute = $false
      $p = New-Object System.Diagnostics.Process
      $p.StartInfo = $pinfo
      $p.Start() | Out-Null
      $p.WaitForExit()
      $stdout = $p.StandardOutput.ReadToEnd()
      Add-Content $PathToUserLogFile "Invoking $script_name"
      Add-Content $PathToUserLogFile "$stdout"
      Add-Content $PathToUserLogFile "exit code: $($p.ExitCode)"
    }
  }
}
catch {
  $ErrorMessage = $_.Exception.Message
  Write-EpLog $ErrorMessage -LogLevel "ERROR"
}

Write-EpLog "Init script executed successfully" -LogLevel "SUCCESS"
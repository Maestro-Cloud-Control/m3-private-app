#!/bin/bash
set -euo pipefail

KEY="$SSH"          # public key line or empty
DNS_IP="$DNS"            # e.g. 10.0.0.53 or empty
DNS_ZONE="$DOMAIN"    # e.g. azin.com or empty
ENV_FILE="/init.env"

log(){ echo "[init] $*"; }

cat >"$ENV_FILE" <<EOF
SSH_KEY=${KEY}
DNS_IP=${DNS_IP}
DNS_ZONE=${DNS_ZONE}
EOF
chmod 600 "$ENV_FILE"
log "Wrote env file: $ENV_FILE"

#-----------ssh-------------
if [[ -n "$KEY" ]]; then
  mkdir -p ~/.ssh
  chmod 700 ~/.ssh
  touch ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  echo "$KEY" >> ~/.ssh/authorized_keys
  log "Appended SSH key to ~/.ssh/authorized_keys"
else
  log "SSH_KEY empty, skipping authorized_keys"
fi

#-----------network-------------
# Only act if both are non-empty
if [[ -z "${DNS_IP}" || -z "${DNS_ZONE}" || "${DNS_IP}" == "__DNS_IP__" || "${DNS_ZONE}" == "__DNS_ZONE__" ]]; then
    log "DNS configuration not provided, skipping network config"
    exit 0
fi

# Pick active connection (ethernet preferred)
CONN="$(nmcli -t -f NAME,TYPE connection show --active | awk -F: '$2 ~ /ethernet|802-3-ethernet/ {print $1; exit}')"
log "Selected connection: ${CONN}"
if [[ -z "${CONN}" ]]; then
  log "Ethernet connection not found, picking any active connection"
  CONN="$(nmcli -t -f NAME connection show --active | head -n1 || true)"
fi
[[ -z "${CONN}" ]] && exit 0

# Apply DNS settings: use our DNS, ignore DHCP DNS, set search domain
nmcli connection modify "${CONN}" ipv4.ignore-auto-dns yes
nmcli connection modify "${CONN}" ipv4.dns "${DNS_IP}"
nmcli connection modify "${CONN}" ipv4.dns-search "${DNS_ZONE}"

# Apply without reboot
nmcli connection up "${CONN}" >/dev/null 2>&1 || true
log "Applied DNS settings to connection: ${CONN}"

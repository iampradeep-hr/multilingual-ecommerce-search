#!/bin/sh
set -eu

dns_server="$(awk '$1 == "nameserver" { print $2; exit }' /etc/resolv.conf)"
if [ -z "$dns_server" ]; then
    echo "Could not find a DNS resolver in /etc/resolv.conf." >&2
    exit 1
fi

case "$dns_server" in
    *:*) dns_server="[$dns_server]" ;;
esac

sed -i "s|__NGINX_DNS_SERVER__|$dns_server|g" /etc/nginx/conf.d/default.conf

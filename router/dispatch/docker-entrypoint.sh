#!/bin/bash
set -e

# if command starts with an option, prepend qdrouterd
if [ "${1:0:1}" = '-' ]; then
    set -- qdrouterd "$@"
fi

if [ "$1" = "qdrouterd" ]; then
    echo "123456" | saslpasswd2 -f /var/lib/qdrouterd/sasl/qdrouterd.sasldb -p admin
    set -- "$@" "--config" "/var/lib/qdrouterd/qdrouterd.conf"
fi

# else default to run whatever the user wanted like "bash"
exec "$@"

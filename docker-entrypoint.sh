#!/bin/bash
set -e

if [[ "$1" =~ '-' ]]; then
    CMD="/idea-cli-inspector $@"
    echo "Calling $CMD"
    exec sudo -H -n -u ideainspect bash -l -c "$CMD"
fi

echo "Executing $@"
exec "$@"
#!/bin/bash
# dSeries Password Decryption Script
# Usage: decrypt_password.sh <dSeries_install_dir> <encrypted_password>

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: decrypt_password.sh <dSeries_install_dir> <encrypted_password>"
    echo "Example: decrypt_password.sh /opt/CA/ESPdSeriesWAServer_R12_4 rRQcfTTiTZPX3plzpBwmvA=="
    exit 1
fi

CAWA_HOME="$1"
ENCRYPTED_PASSWORD="$2"

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run decryption utility with dSeries libraries
"$CAWA_HOME/jre/bin/java" -cp "$CAWA_HOME/lib/*:$SCRIPT_DIR" DecryptPassword "$ENCRYPTED_PASSWORD"

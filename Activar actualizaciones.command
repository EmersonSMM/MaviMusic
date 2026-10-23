#!/bin/zsh
set -eu
cd -- "${0:A:h}"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
script_status=0
python3 scripts/updates/activate_github.py "$@" || script_status=$?
printf '\nPulsa Intro para cerrar esta ventana… '
read -r reply || true
exit "$script_status"

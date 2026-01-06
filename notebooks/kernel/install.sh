#!/bin/bash
#
# Install PL|ESQL Jupyter Kernel
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KERNEL_DIR="$HOME/.local/share/jupyter/kernels/plesql"

echo "Installing PL|ESQL Jupyter Kernel..."

# Install Python dependencies
pip install jupyter ipykernel requests pandas --quiet

# Create kernel directory
mkdir -p "$KERNEL_DIR"

# Copy kernel files
cp "$SCRIPT_DIR/plesql_kernel.py" "$KERNEL_DIR/"
cp "$SCRIPT_DIR/kernel.json" "$KERNEL_DIR/"

# Update kernel.json to use absolute path
python3 -c "
import json
import os
kernel_json = '$KERNEL_DIR/kernel.json'
with open(kernel_json, 'r') as f:
    data = json.load(f)
data['argv'] = ['python3', '$KERNEL_DIR/plesql_kernel.py', '-f', '{connection_file}']
with open(kernel_json, 'w') as f:
    json.dump(data, f, indent=4)
"

echo "✔ Kernel installed at $KERNEL_DIR"
echo ""
echo "To verify: jupyter kernelspec list"
echo "To start:  jupyter notebook"


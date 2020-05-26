#!/bin/bash
conda env create -n $1 -f configuration.yml
source ~/anaconda3/etc/profile.d/conda.sh
conda activate $1
(pip install -r requirements.txt --verbose)
beakerx-kernel-scala install
echo To activate this environment, use:
echo      
echo      conda activate $1
echo      

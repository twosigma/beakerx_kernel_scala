# BeakerX: Beaker kernel scala for Jupyter  

### Build and Install (linux and mac)

```
cd ./scala-dist
conda env create -n beakerx -f configuration.yml
conda activate beakerx # For conda versions prior to 4.6, run: source activate beakerx
(pip install -r requirements.txt --verbose)
beakerx-kernel-scala install
cd ..
```

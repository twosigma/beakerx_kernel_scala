# Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

'''Installs BeakerX into a Jupyter and Python environment.'''

import json
import os
import pkg_resources
import shutil
import subprocess
import tempfile

from string import Template


def _kernel_name():
    return "scala"


def _base_classpath_for(kernel):
    return pkg_resources.resource_filename(
        'beakerx_scala', os.path.join('kernel', kernel))


def _classpath():
    return pkg_resources.resource_filename(
        'beakerx_scala', os.path.join('kernel', 'lib', '*'))


def _copy_tree(src, dst):
    if os.path.exists(dst):
        shutil.rmtree(dst)
    shutil.copytree(src, dst)


def _install_kernels():
    base_classpath = _classpath()
    classpath = json.dumps(os.pathsep.join([base_classpath]))
    template = pkg_resources.resource_string(
        'beakerx_scala', os.path.join('kernel', 'kernel.json'))
    contents = Template(template.decode()).substitute(PATH=classpath)

    with tempfile.TemporaryDirectory() as tmpdir:
        kernel_dir = os.path.join(tmpdir, _kernel_name())
        os.mkdir(kernel_dir)
        with open(os.path.join(kernel_dir, 'kernel.json'), 'w') as f:
            f.write(contents)
        install_cmd = [
            'jupyter', 'kernelspec', 'install',
            '--sys-prefix', '--replace',
            '--name', _kernel_name(), kernel_dir
        ]
        subprocess.check_call(install_cmd)


def _uninstall_kernels():
    uninstall_cmd = [
        'jupyter', 'kernelspec', 'remove', _kernel_name(), '-y', '-f'
    ]
    try:
        subprocess.check_call(uninstall_cmd)
    except subprocess.CalledProcessError:
        pass  # uninstal_cmd prints the appropriate message


def install(args):
    _install_kernels()


def uninstall(args):
    _uninstall_kernels()


if __name__ == "__main__":
    install()

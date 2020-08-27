#!/usr/bin/env python
# coding: utf-8

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

import os
from setuptools import setup, find_packages

from setupbase import (
    create_cmdclass,
    run_gradle,
    get_version
)


cmdclass = create_cmdclass(develop_wrappers=[
    'java'
], distribute_wrappers=[
    'java'
])

cmdclass['java'] = run_gradle(cmd='install', skip_tests=False)

setup_args = dict(
    name='beakerx_kernel_scala',
    description='BeakerX: Beaker Extensions for Jupyter Notebook',
    long_description='BeakerX: Beaker Extensions for Jupyter Notebook',
    version=get_version(os.path.join('beakerx_scala', '_version.py')),
    author='Two Sigma Open Source, LLC',
    author_email='beakerx-feedback@twosigma.com',
    url='http://beakerx.com',
    keywords=[
        'ipython',
        'jupyter',
        'widgets',
        'java',
        'clojure',
        'groovy',
        'scala',
        'kotlin',
        'sql',
    ],
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'Framework :: IPython',
        'Intended Audience :: Developers',
        'Intended Audience :: Science/Research',
        'Topic :: Multimedia :: Graphics',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
    ],
    entry_points={
        'console_scripts': [
            'beakerx_kernel_scala = beakerx_scala:run'
        ]
    },
    package_data={
        'beakerx_scala': [
            'kernel/*/kernel.json'
        ]
    },
    python_requires='>=3',
    zip_safe=False,
    include_package_data=True,
    packages=find_packages(),
    cmdclass=cmdclass
)

if __name__ == '__main__':
    setup(**setup_args)

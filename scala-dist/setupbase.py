#!/usr/bin/env python
# coding: utf-8

# Copyright 2020 TWO SIGMA OPEN SOURCE, LLC
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

"""
This file originates from the 'jupyter-packaging' package, and
contains a set of useful utilities for installing node modules
within a Python package.
"""
import functools
import os
import pipes
import sys
from distutils import log
from setuptools import Command
from setuptools.command.develop import develop
from setuptools.command.sdist import sdist
from setuptools.command.bdist_egg import bdist_egg
from subprocess import check_call

try:
    from wheel.bdist_wheel import bdist_wheel
except ImportError:
    bdist_wheel = None

if sys.platform == 'win32':
    from subprocess import list2cmdline
else:
    def list2cmdline(cmd_list):
        return ' '.join(map(pipes.quote, cmd_list))

# ---------------------------------------------------------------------------
# Top Level Variables
# ---------------------------------------------------------------------------


here = os.path.abspath(os.path.dirname(sys.argv[0]))
root = os.path.abspath(os.path.join(here, os.pardir))
kernel_path = os.path.join(root, './')

# ---------------------------------------------------------------------------
# Public Functions
# ---------------------------------------------------------------------------
def get_version(path):
    version = {}
    with open(os.path.join(here, path)) as f:
        exec (f.read(), {}, version)
    return version['__version__']


def update_package_data(distribution):
    """update build_py options to get package_data changes"""
    build_py = distribution.get_command_obj('build_py')
    build_py.finalize_options()



def create_cmdclass(develop_wrappers=None, distribute_wrappers=None, data_dirs=None):
    """Create a command class with the given optional wrappers.
    Parameters
    ----------
    develop_wrapper: list(str), optional
        The cmdclass names to run before running other commands
    distribute_wrappers: list(str), optional
        The cmdclass names to run before running other commands
    data_dirs: list(str), optional.
        The directories containing static data.
    """
    develop_wrappers = develop_wrappers or []
    distribute_wrappers = distribute_wrappers or []
    data_dirs = data_dirs or []
    develop_wrapper = functools.partial(wrap_command, develop_wrappers, data_dirs)
    distribute_wrapper = functools.partial(wrap_command, distribute_wrappers, data_dirs)
    cmdclass = dict(
        develop=develop_wrapper(develop, strict=True),
        sdist=distribute_wrapper(sdist, strict=True),
        bdist_egg=bdist_egg if 'bdist_egg' in sys.argv else bdist_egg_disabled
    )
    if bdist_wheel:
        cmdclass['bdist_wheel'] = bdist_wheel
    return cmdclass


class BaseCommand(Command):
    """Empty command because Command needs subclasses to override too much"""
    user_options = []

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def get_inputs(self):
        return []

    def get_outputs(self):
        return []


def run_gradle(path=kernel_path, cmd='install', skip_tests=False):
    """Return a Command for running gradle scripts.

    Parameters
    ----------
    path: str, optional
        The base path of the node package.  Defaults to the repo root.
    cmd: str, optional
        The command to run with gradlew.
    """
    class Gradle(BaseCommand):
        description = 'Run gradle script'

        def skip_test_option(self, skip):
            if skip:
                return '-Dskip.tests=True'
            else:
                return '-Dskip.tests=False'

        def run(self):
            run([('' if sys.platform == 'win32' else './') + 'gradlew', '--no-daemon', cmd,
                 self.skip_test_option(skip_tests)], cwd=path)

    return Gradle


def run(cmd, *args, **kwargs):
    """Echo a command before running it.  Defaults to repo as cwd"""
    log.info('> ' + list2cmdline(cmd))
    kwargs.setdefault('cwd', here)
    kwargs.setdefault('shell', sys.platform == 'win32')
    if not isinstance(cmd, list):
        cmd = cmd.split()
    return check_call(cmd, *args, **kwargs)

def wrap_command(cmds, data_dirs, cls, strict=True):
    """Wrap a setup command
    Parameters
    ----------
    cmds: list(str)
        The names of the other commands to run prior to the command.
    strict: boolean, optional
        Wether to raise errors when a pre-command fails.
    """

    class WrappedCommand(cls):

        def run(self):
            if not getattr(self, 'uninstall', None):
                try:
                    [self.run_command(cmd) for cmd in cmds]
                except Exception:
                    if strict:
                        raise
                    else:
                        pass

            result = cls.run(self)
            data_files = []
            for dname in data_dirs:
                data_files.extend(get_data_files(dname))
            # update data-files in case this created new files
            self.distribution.data_files = data_files
            # also update package data
            update_package_data(self.distribution)
            return result

    return WrappedCommand

class bdist_egg_disabled(bdist_egg):
    """Disabled version of bdist_egg
    Prevents setup.py install performing setuptools' default easy_install,
    which it should never ever do.
    """

    def run(self):
        sys.exit("Aborting implicit building of eggs. Use `pip install .` " +
                 " to install from source.")

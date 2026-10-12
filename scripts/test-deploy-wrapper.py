#!/usr/bin/env python3
"""Regression checks for the limited deployment entry point."""
import importlib.util
import io
import os
from pathlib import Path
import tarfile
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('deploy', Path(__file__).resolve().parents[1] / 'ops/deploy/bg-foot-deploy.py')
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeployArchiveTest(unittest.TestCase):
    def check_member(self, member):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / 'web.tgz'
            with tarfile.open(archive, 'w:gz') as output:
                output.addfile(member, io.BytesIO(b'x' * member.size))
            deploy.validate_archive(archive)

    def test_regular_assets_are_allowed(self):
        self.check_member(tarfile.TarInfo('./assets/index.js'))

    def test_paths_outside_web_root_are_rejected(self):
        for name in ('../../etc/cron.d/job', '/etc/cron.d/job', './assets/../../../root/job'):
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.check_member(tarfile.TarInfo(name))

    def test_links_and_special_files_are_rejected(self):
        for kind in (tarfile.SYMTYPE, tarfile.LNKTYPE, tarfile.FIFOTYPE):
            member = tarfile.TarInfo('assets/link')
            member.type = kind
            member.linkname = '/etc/passwd'
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                self.check_member(member)

    def test_symlink_artifacts_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / 'source'
            output = Path(folder) / 'output'
            source.mkdir()
            output.mkdir()
            (source / 'app.jar').symlink_to('/etc/passwd')
            fd = os.open(source, os.O_RDONLY | os.O_DIRECTORY)
            try:
                with self.assertRaises(OSError):
                    deploy.copy_artifacts(fd, output, os.getuid())
            finally:
                os.close(fd)


if __name__ == '__main__':
    unittest.main()

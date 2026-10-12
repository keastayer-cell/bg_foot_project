#!/usr/bin/python3
"""Install uploaded BG Foot artifacts using root-owned deployment policy."""
import argparse
import os
from pathlib import Path, PurePosixPath
import pwd
import re
import shutil
import stat
import subprocess
import tarfile
import tempfile

UPLOAD_ROOT = Path('/var/lib/bg-foot-upload')
STAGING_ROOT = Path('/var/lib/bg-foot-deploy')
POLICY_ROOT = Path('/usr/local/lib/bg-foot-deploy')
ARTIFACTS = ('app.jar', 'mailer.jar', 'web-dist.tgz', 'git-sha')
POLICY_FILES = ('deploy-test-remote.sh', 'smoke-check.sh', 'db-backup.sh', 'validate-server-environment.sh')
MAX_FILE_SIZE = 256 * 1024 * 1024


def validate_archive(path):
    with tarfile.open(path, 'r:gz') as archive:
        members = archive.getmembers()
        if len(members) > 10000 or sum(item.size for item in members) > 512 * 1024 * 1024:
            raise ValueError('Frontend archive exceeds release limits')
        for member in members:
            name = PurePosixPath(member.name)
            if name.is_absolute() or '..' in name.parts or not (member.isfile() or member.isdir()):
                raise ValueError('Frontend archive contains an unsafe member')


def copy_artifacts(source_fd, destination, owner_uid):
    for name in ARTIFACTS:
        fd = os.open(name, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK, dir_fd=source_fd)
        with os.fdopen(fd, 'rb') as source:
            info = os.fstat(source.fileno())
            if not stat.S_ISREG(info.st_mode) or info.st_uid != owner_uid or info.st_size > MAX_FILE_SIZE:
                raise ValueError('Invalid release artifact: ' + name)
            with (destination / name).open('wb') as output:
                shutil.copyfileobj(source, output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('sha')
    args = parser.parse_args()
    if os.geteuid() != 0 or not re.fullmatch(r'[0-9a-f]{40}', args.sha):
        parser.error('Root and a full lowercase Git commit SHA are required')
    owner_uid = pwd.getpwnam('bgfootdeploy').pw_uid
    directory_flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW
    with tempfile.TemporaryDirectory(prefix=args.sha + '-', dir=STAGING_ROOT) as name:
        staging = Path(name)
        base_fd = os.open(UPLOAD_ROOT, directory_flags)
        try:
            release_fd = os.open(args.sha, directory_flags, dir_fd=base_fd)
            try:
                source_fd = os.open('release', directory_flags, dir_fd=release_fd)
                try:
                    copy_artifacts(source_fd, staging, owner_uid)
                finally:
                    os.close(source_fd)
            finally:
                os.close(release_fd)
        finally:
            os.close(base_fd)
        if (staging / 'git-sha').read_text().strip() != args.sha:
            raise ValueError('Release SHA does not match its upload directory')
        validate_archive(staging / 'web-dist.tgz')
        # Uploaded shell scripts and systemd units are intentionally never executed.
        for filename in POLICY_FILES:
            shutil.copy2(POLICY_ROOT / filename, staging / filename)
        shutil.copytree(POLICY_ROOT / 'ops', staging / 'ops')
        env = {
            'PATH': '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
            'HOME': '/root',
            'RELEASE_ID': args.sha,
            'DEPLOY_ENV': 'test',
            'LOCAL_BASE_URL': 'http://127.0.0.1',
            'LOCAL_MAILER_URL': 'http://127.0.0.1:18090',
        }
        subprocess.run(['/bin/bash', str(staging / 'deploy-test-remote.sh'), str(staging)], env=env, check=True)


if __name__ == '__main__':
    main()

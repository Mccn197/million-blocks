#!/usr/bin/env python3
"""Offline-only world snapshots; restore preserves the displaced world and never deletes it."""
import argparse
import contextlib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
import time


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def files(root):
    result = {}
    for path in sorted(root.rglob('*')):
        if path.is_symlink():
            raise ValueError('Symlink refused: ' + str(path))
        if path.is_file() and path.name != 'session.lock':
            result[path.relative_to(root).as_posix()] = digest(path)
    if 'level.dat' not in result:
        raise ValueError('Not a Minecraft world: level.dat missing')
    return result


def contained(child, parent):
    return child == parent or parent in child.parents


@contextlib.contextmanager
def offline(world):
    lock = world / 'session.lock'
    if not lock.is_file() or lock.is_symlink():
        raise ValueError('Existing regular session.lock required; offline state cannot be verified')
    with lock.open('r+b') as f:
        try:
            # Java FileChannel.lock uses POSIX record locks; flock would not conflict with it.
            fcntl.lockf(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as e:
            raise ValueError('World is loaded/locked; stop the server first') from e
        # Also refuse a running Java process whose cwd is this server directory.
        for proc in Path('/proc').glob('[0-9]*'):
            try:
                cwd = (proc / 'cwd').resolve()
                exe = (proc / 'exe').resolve().name
                if cwd == world.parent and exe.startswith('java'):
                    raise ValueError('Java server process still running in ' + str(cwd))
            except (OSError, RuntimeError):
                continue
        yield


def snapshot(world, destination):
    if destination.exists():
        raise ValueError('Snapshot destination already exists')
    destination.parent.mkdir(parents=True, exist_ok=True)
    with offline(world):
        before = files(world)
        stage = Path(tempfile.mkdtemp(prefix='.mb-snapshot-', dir=destination.parent))
        try:
            shutil.copytree(world, stage / 'world', ignore=shutil.ignore_patterns('session.lock'))
            if files(stage / 'world') != before or files(world) != before:
                raise ValueError('World changed during copy')
            (stage / 'manifest.json').write_text(json.dumps({'schema': 1, 'world_name': world.name,
                'created_unix': time.time(), 'sha256': before}, indent=2))
            stage.rename(destination)
        finally:
            if stage.exists():
                shutil.rmtree(stage)  # Only this newly created temporary directory.
    return destination


def verify(snapshot_dir):
    manifest = json.loads((snapshot_dir / 'manifest.json').read_text())
    if manifest['schema'] != 1 or files(snapshot_dir / 'world') != manifest['sha256']:
        raise ValueError('Snapshot checksum verification failed')
    return manifest


def restore(world, source, rescue):
    manifest = verify(source)
    if manifest['world_name'] != world.name:
        raise ValueError('Snapshot belongs to another world')
    if rescue.exists():
        raise ValueError('Rescue destination already exists')
    rescue.parent.mkdir(parents=True, exist_ok=True)
    if world.parent.stat().st_dev != rescue.parent.stat().st_dev:
        raise ValueError('Rescue must be on the same filesystem for atomic rename')
    with offline(world):
        stage = Path(tempfile.mkdtemp(prefix='.mb-restore-', dir=world.parent))
        try:
            shutil.copytree(source / 'world', stage / 'world')
            if files(stage / 'world') != manifest['sha256']:
                raise ValueError('Staged restore verification failed')
            # Reserve backup path against concurrent restore attempts.
            rescue.mkdir()
            try:
                world.rename(rescue / 'world')
                try:
                    (stage / 'world').rename(world)
                    (world / 'session.lock').touch(exist_ok=False)
                except BaseException:
                    if not world.exists():
                        (rescue / 'world').rename(world)
                    raise
                (rescue / 'manifest.json').write_text(json.dumps({'schema': 1,
                    'world_name': world.name, 'sha256': files(rescue / 'world')}, indent=2))
            except BaseException:
                # Preserve every existing world, including partially restored output.
                raise
        finally:
            if stage.exists():
                shutil.rmtree(stage)
    return rescue


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['save', 'verify', 'restore'])
    parser.add_argument('--world', type=Path)
    parser.add_argument('--snapshot', required=True, type=Path)
    parser.add_argument('--rescue', type=Path)
    parser.add_argument('--confirm')
    args = parser.parse_args()
    source = args.snapshot.absolute()
    if source.is_symlink():
        parser.error('Snapshot symlink refused')
    if args.operation == 'verify':
        verify(source)
        print('Snapshot SHA-256 verified')
        return
    if args.world is None or args.confirm != 'OFFLINE:' + args.world.name:
        parser.error('Required: --world PATH --confirm OFFLINE:<world-name>; stop server first')
    world = args.world.absolute()
    if world.is_symlink() or not world.is_dir():
        parser.error('Existing world directory required; symlinks refused')
    if contained(source.resolve(), world.resolve()) or contained(world.resolve(), source.resolve()):
        parser.error('Snapshot and world paths must not overlap')
    if args.operation == 'save':
        print('Saved:', snapshot(world, source))
    else:
        if args.rescue is None:
            parser.error('--rescue PATH required: the current world will be preserved there')
        rescue = args.rescue.absolute()
        for other in [world, source]:
            if contained(rescue.resolve(), other.resolve()) or contained(other.resolve(), rescue.resolve()):
                parser.error('World, snapshot and rescue paths must not overlap')
        print('Restored; current world preserved:', restore(world, source, rescue))


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as exc:
        sys.exit('REFUSED: ' + str(exc))

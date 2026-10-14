import importlib.util
from pathlib import Path
import tempfile
import unittest
import subprocess
import sys
spec = importlib.util.spec_from_file_location('snap', Path(__file__).resolve().parents[1] / 'scripts/world_snapshot.py')
snap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snap)

class SnapshotTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.world = self.root / 'survival'
        self.world.mkdir()
        (self.world / 'level.dat').write_bytes(b'pristine world')
        (self.world / 'session.lock').touch()
        self.snapshot = self.root / 'backup'

    def test_roundtrip_preserves_destroyed_world(self):
        snap.snapshot(self.world, self.snapshot)
        (self.world / 'level.dat').write_bytes(b'destroyed world')
        rescue = self.root / 'rescue'
        snap.restore(self.world, self.snapshot, rescue)
        self.assertEqual((self.world / 'level.dat').read_bytes(), b'pristine world')
        self.assertEqual((rescue / 'world/level.dat').read_bytes(), b'destroyed world')
        snap.verify(self.snapshot)
        snap.verify(rescue)

    def test_tamper_refused_without_changing_world(self):
        snap.snapshot(self.world, self.snapshot)
        (self.snapshot / 'world/level.dat').write_bytes(b'tampered')
        with self.assertRaises(ValueError):
            snap.restore(self.world, self.snapshot, self.root / 'rescue')
        self.assertEqual((self.world / 'level.dat').read_bytes(), b'pristine world')

    def test_loaded_world_lock_refused(self):
        code = "import fcntl,sys; f=open(sys.argv[1],'r+b');fcntl.lockf(f,fcntl.LOCK_EX);print('locked',flush=True);sys.stdin.read()"
        process = subprocess.Popen([sys.executable, '-c', code, str(self.world / 'session.lock')], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
        try:
            self.assertEqual(process.stdout.readline().strip(), 'locked')
            with self.assertRaises(ValueError): snap.snapshot(self.world, self.snapshot)
            self.assertFalse(self.snapshot.exists())
        finally:
            process.communicate('done')

    def test_existing_backup_refused(self):
        snap.snapshot(self.world, self.snapshot)
        with self.assertRaises(ValueError): snap.snapshot(self.world, self.snapshot)

    def test_symlink_refused(self):
        (self.world / 'escape').symlink_to(self.root)
        with self.assertRaises(ValueError): snap.snapshot(self.world, self.snapshot)

    def test_cli_requires_confirmation(self):
        result = subprocess.run([sys.executable, str(Path(snap.__file__)), 'save', '--world', str(self.world), '--snapshot', str(self.snapshot)],capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(self.snapshot.exists())

if __name__ == '__main__': unittest.main()

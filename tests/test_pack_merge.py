import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
spec=importlib.util.spec_from_file_location('merge',Path(__file__).resolve().parents[1]/'scripts/merge_resourcepacks.py')
merge=importlib.util.module_from_spec(spec);spec.loader.exec_module(merge)

class PackMergeTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
    def pack(self,name,files,format=88):
        p=self.root/name
        with zipfile.ZipFile(p,'w') as z:
            z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':format,'description':name},'overlays':{'entries':[]}}))
            for name,data in files.items():z.writestr(name,data)
        return p
    def test_preserves_betterhud_shaders_metadata_and_item_files(self):
        bh=self.pack('bh.zip',{'assets/betterhud/font/hud.json':'font','assets/minecraft/shaders/core/custom.fsh':'shader'})
        items=self.pack('items.zip',{'assets/millionblocks/items/bore_x.json':'item'})
        out=self.root/'merged.zip';self.assertEqual(merge.merge(bh,items,out),4)
        data=merge.read_pack(out)
        self.assertEqual(json.loads(data['pack.mcmeta'])['pack']['description'],'bh.zip')
        self.assertEqual(data['assets/minecraft/shaders/core/custom.fsh'],b'shader')
        self.assertIn('assets/millionblocks/items/bore_x.json',data)
        with self.assertRaises(ValueError):merge.merge(bh,items,out)
    def test_conflicting_resources_are_refused(self):
        bh=self.pack('bh.zip',{'assets/a.json':'one'});items=self.pack('items.zip',{'assets/a.json':'two'})
        out=self.root/'merged.zip'
        with self.assertRaises(ValueError):merge.merge(bh,items,out)
        self.assertFalse(out.exists())
    def test_wrong_format_and_path_traversal_are_refused(self):
        bh=self.pack('bh.zip',{},format=90);items=self.pack('items.zip',{})
        with self.assertRaises(ValueError):merge.merge(bh,items,self.root/'merged.zip')
        bad=self.pack('bad.zip',{'../escape.txt':'unsafe'})
        with self.assertRaises(ValueError):merge.read_pack(bad)

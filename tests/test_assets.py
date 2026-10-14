import base64
import json
from pathlib import Path
import re
import unittest
from PIL import Image
import yaml
ROOT=Path(__file__).resolve().parents[1]
PACK=ROOT/'assets/resourcepack'
NS=PACK/'assets/millionblocks'
BH=ROOT/'assets/betterhud-plugin/plugins/BetterHud'
NAMES=['prismbreaker','quakehammer','detonation_buddy','blastcaster','bore_x','redline_mk1','starfall_beacon','void_engine','shop_portal']

class AssetTests(unittest.TestCase):
    def test_all_json_yaml_and_png_parse(self):
        for path in (ROOT/'assets').rglob('*'):
            if path.suffix in ['.json','.bbmodel','.mcmeta']:
                with self.subTest(file=str(path)):json.loads(path.read_text())
            elif path.suffix in ['.yml','.yaml']:
                with self.subTest(file=str(path)):yaml.safe_load(path.read_text())
            elif path.suffix=='.png':
                with self.subTest(file=str(path)):
                    with Image.open(path) as im:im.verify()

    def test_editable_models_and_exports_agree(self):
        for name in NAMES:
            with self.subTest(model=name):
                bb=json.loads((ROOT/'assets/blockbench'/f'{name}.bbmodel').read_text())
                model=json.loads((NS/'models/item'/f'{name}.json').read_text())
                self.assertGreater(len(bb['elements']),25)
                self.assertEqual(len(bb['elements']),len(model['elements']))
                ids={e['uuid'] for e in bb['elements']}
                self.assertEqual(len(ids),len(bb['elements']))
                self.assertEqual(set(bb['outliner'][0]['children']),ids)
                tex=bb['textures'][0]
                self.assertEqual(base64.b64decode(tex['source'].split(',')[1]),(NS/'textures/item'/tex['name']).read_bytes())
                for source,export in zip(bb['elements'],model['elements']):
                    self.assertEqual(source['from'],export['from']);self.assertEqual(source['to'],export['to'])
                    for i in range(3):
                        self.assertLess(source['from'][i],source['to'][i])
                        self.assertGreaterEqual(source['from'][i],-16);self.assertLessEqual(source['to'][i],32)
                    for face,data in source['faces'].items():
                        self.assertEqual(data['texture'],0)
                        self.assertEqual(export['faces'][face]['uv'],[v/4 for v in data['uv']])
                        self.assertTrue(all(0<=v<=64 for v in data['uv']))

    def test_modern_item_models_and_texture_references_resolve(self):
        self.assertEqual(len(list((NS/'items').glob('*.json'))),12)
        for path in (NS/'items').glob('*.json'):
            data=json.loads(path.read_text())['model']
            self.assertEqual(data['type'],'minecraft:model')
            namespace,model=data['model'].split(':')
            target=PACK/'assets'/namespace/'models'/(model+'.json')
            self.assertTrue(target.is_file())
            for value in json.loads(target.read_text())['textures'].values():
                namespace,texture=value.split(':')
                self.assertTrue((PACK/'assets'/namespace/'textures'/(texture+'.png')).is_file())

    def test_font_textures_and_unicode_cells_resolve(self):
        for provider in json.loads((NS/'font/hud.json').read_text())['providers']:
            namespace,texture=provider['file'].split(':')
            with Image.open(PACK/'assets'/namespace/'textures'/texture) as im:
                self.assertEqual(im.width%len(provider['chars'][0]),0)
                self.assertEqual(im.height%len(provider['chars']),0)
            self.assertLessEqual(provider['ascent'],provider['height'])

    def test_betterhud_references_placeholders_and_condition_types(self):
        images=yaml.safe_load((BH/'images/millionblocks-amber.yml').read_text())
        texts=yaml.safe_load((BH/'texts/millionblocks-amber.yml').read_text())
        layouts=yaml.safe_load((BH/'layouts/millionblocks-amber.yml').read_text())
        huds=yaml.safe_load((BH/'huds/millionblocks-amber.yml').read_text())
        registered={'millionblocks_count':'string','millionblocks_goal':'string','millionblocks_percent':'string',
                    'millionblocks_multiplier':'string','millionblocks_crack':'number','millionblocks_visible':'boolean','millionblocks_icon':'boolean'}
        self.assertFalse(huds['mb_amber']['default'])
        gui=huds['mb_amber']['layouts']['1']['gui'];self.assertEqual(gui,{'x':0,'y':0})
        for definition in images.values():self.assertTrue((BH/'assets'/definition['file']).is_file())
        for layout in layouts.values():
            for im in layout['images'].values():self.assertIn(im['name'],images)
            for text in layout['texts'].values():
                self.assertIn(text['name'],texts)
                for placeholder in re.findall(r'\[([^\]]+)\]',text['pattern']):self.assertIn(placeholder,registered)
        def walk(value):
            if isinstance(value,dict):
                if 'conditions' in value:
                    for condition in value['conditions'].values():
                        kind=registered[condition['first']]
                        if kind=='boolean':self.assertIn(condition['second'],['true','false'])
                        elif kind=='number':float(condition['second'])
                        self.assertIn(condition['operation'],['==','!=','<','>','<=','>='])
                for child in value.values():walk(child)
        walk(layouts);walk(huds)
        self.assertEqual(sum(im['name'].startswith('mb_crack_') for im in layouts['mb_amber_layout']['images'].values()),5)

    def test_animation_rigs_reference_real_editable_bones(self):
        files=list((ROOT/'assets/blockbench/animated').glob('*.bbmodel'));self.assertEqual(len(files),4)
        for path in files:
            rig=json.loads(path.read_text())
            groups=set()
            def bones(nodes):
                for node in nodes:
                    if isinstance(node,dict):groups.add(node['uuid']);bones(node.get('children',[]))
            bones(rig['outliner'])
            self.assertEqual(rig['meta']['model_format'],'free')
            for anim in rig['animations']:
                self.assertGreater(anim['length'],0)
                for bone,track in anim['animators'].items():
                    self.assertIn(bone,groups)
                    self.assertGreaterEqual(len(track['keyframes']),2)
                    self.assertTrue(all(0<=f['time']<=anim['length'] for f in track['keyframes']))

    def test_pack_targets_minecraft_26_2(self):
        pack=json.loads((PACK/'pack.mcmeta').read_text())['pack']
        self.assertEqual(pack['pack_format'],88);self.assertEqual(pack['min_format'],[88,0]);self.assertEqual(pack['max_format'],[88,0])

if __name__=='__main__':unittest.main()

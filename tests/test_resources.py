"""Fast resource-contract tests, not a substitute for Loom or an in-game test."""
import json
import re
import struct
import subprocess
import sys
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
DATA=RES/'data/myfirstmod'
ASSETS=RES/'assets/myfirstmod'

def load(path): return json.loads(path.read_text())

class ResourceTests(unittest.TestCase):
    def test_all_json_parses(self):
        for path in RES.rglob('*.json'):
            with self.subTest(path=str(path)): load(path)

    def test_dimension_is_not_a_vanilla_preset(self):
        dimension=load(DATA/'dimension/null_realm.json')
        self.assertEqual(dimension['generator']['settings'],'myfirstmod:null_realm')
        biomes=dimension['generator']['biome_source']['biomes']
        self.assertEqual(len(biomes),3)
        for biome in biomes:
            self.assertTrue((DATA/('worldgen/biome/'+biome['biome'].split(':')[1]+'.json')).exists())
        noise=load(DATA/'worldgen/noise_settings/null_realm.json')
        self.assertEqual(noise['noise']['min_y'],-64)
        self.assertEqual(noise['noise']['height'],384)
        self.assertNotIn('minecraft:end',json.dumps(noise))
        self.assertEqual(len(noise['noise_router']),15)
        kind=load(DATA/'dimension_type/null_realm.json')
        self.assertTrue(kind['infiniburn'].startswith('#'))

    def test_biome_features_resolve(self):
        for path in (DATA/'worldgen/biome').glob('*.json'):
            biome=load(path)
            self.assertEqual(len(biome['features']),11)
            for stage in biome['features']:
                for feature in stage:
                    placed=load(DATA/('worldgen/placed_feature/'+feature.split(':')[1]+'.json'))
                    configured=load(DATA/('worldgen/configured_feature/'+placed['feature'].split(':')[1]+'.json'))
                    self.assertIn(configured['type'],['myfirstmod:realm_ruins','myfirstmod:realm_flora'])
            self.assertEqual({s['type'] for s in biome['spawners']['monster']},
                             {'myfirstmod:rift_sentinel','myfirstmod:shardstalker'})

    def test_item_models_and_translations(self):
        source=(ROOT/'src/main/java/dev/qynl/myfirstmod/item/ModItems.java').read_text()
        language=load(ASSETS/'lang/en_us.json')
        for name in re.findall(r'register\("([a-z_]+)"',source):
            self.assertTrue((ASSETS/f'models/item/{name}.json').exists(),name)
            self.assertIn('item.myfirstmod.'+name,language)
            self.assertIn('item.myfirstmod.'+name+'.tooltip',language)
        for path in list((ROOT/'src/main/java').rglob('*.java'))+list((ROOT/'src/client/java').rglob('*.java')):
            for key in re.findall(r'Text.translatable\("([a-z0-9_.]+)"\s*[,)]',path.read_text()):
                self.assertIn(key,language,str(path))
        for attack in range(1,8): self.assertIn('attack.myfirstmod.'+str(attack),language)

    def test_local_model_references_and_textures(self):
        for path in (ASSETS/'models').rglob('*.json'):
            model=load(path)
            references=[model.get('parent','')]+[o['model'] for o in model.get('overrides',[])]
            for ref in references:
                if ref.startswith('myfirstmod:'):
                    self.assertTrue((ASSETS/('models/'+ref.split(':')[1]+'.json')).exists(),ref)
            for texture in model.get('textures',{}).values():
                if texture.startswith('myfirstmod:'):
                    self.assertTrue((ASSETS/('textures/'+texture.split(':')[1]+'.png')).exists(),texture)

    def test_compass_frames(self):
        model=load(ASSETS/'models/item/arena_compass.json')
        overrides=model['overrides']
        self.assertEqual(len(overrides),32)
        self.assertEqual([o['predicate']['angle'] for o in overrides],[i/32 for i in range(32)])
        for path in (ASSETS/'textures').rglob('*.png'):
            content=path.read_bytes()
            self.assertEqual(content[:8],b'\x89PNG\r\n\x1a\n')
            w,h=struct.unpack('>II',content[16:24])
            self.assertIn(w,[16,32,64]);self.assertIn(h,[16,32,64])

    def test_recipe_and_loot_version_contracts(self):
        self.assertFalse((DATA/'loot_tables').exists())
        self.assertFalse((DATA/'recipes').exists())
        upgrade=load(DATA/'recipe/ascended_nullblade.json')
        self.assertEqual(upgrade['type'],'minecraft:smithing_transform')
        self.assertEqual(upgrade['base']['item'],'myfirstmod:nullblade')
        self.assertEqual(upgrade['result']['id'],'myfirstmod:ascended_nullblade')
        self.assertEqual(load(DATA/'loot_table/entities/null_warden.json')['pools'],[])
        for entity in ['rift_sentinel','shardstalker']:
            self.assertTrue((DATA/f'loot_table/entities/{entity}.json').exists())

    def test_generators_are_reproducible(self):
        paths=list(RES.rglob('*'))
        before={str(p.relative_to(RES)):p.read_bytes() for p in paths if p.is_file()}
        for script in ['generate_art.py','generate_realm_data.py','generate_loot.py']:
            subprocess.run([sys.executable,str(ROOT/'scripts'/script)],check=True)
        after={str(p.relative_to(RES)):p.read_bytes() for p in RES.rglob('*') if p.is_file()}
        self.assertEqual(before,after)

if __name__=='__main__': unittest.main()

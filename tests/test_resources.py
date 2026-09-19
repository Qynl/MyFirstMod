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
        self.assertEqual(len(biomes),4)
        for biome in biomes:
            self.assertTrue((DATA/('worldgen/biome/'+biome['biome'].split(':')[1]+'.json')).exists())
        noise=load(DATA/'worldgen/noise_settings/null_realm.json')
        self.assertEqual(noise['noise']['min_y'],-64)
        self.assertEqual(noise['noise']['height'],384)
        self.assertNotIn('minecraft:end',json.dumps(noise))
        self.assertEqual(len(noise['noise_router']),15)
        kind=load(DATA/'dimension_type/null_realm.json')
        self.assertTrue(kind['infiniburn'].startswith('#'))
        self.assertIn('has_ceiling',kind)
        light=kind['monster_spawn_light_level']
        self.assertIn('min_inclusive',light)
        self.assertIn('max_inclusive',light)
        self.assertNotIn('value',light)

    def test_biome_features_resolve(self):
        for path in (DATA/'worldgen/biome').glob('*.json'):
            biome=load(path)
            self.assertEqual(len(biome['features']),11)
            for stage in biome['features']:
                for feature in stage:
                    placed=load(DATA/('worldgen/placed_feature/'+feature.split(':')[1]+'.json'))
                    configured=load(DATA/('worldgen/configured_feature/'+placed['feature'].split(':')[1]+'.json'))
                    self.assertIn(configured['type'],['myfirstmod:realm_ruins','myfirstmod:realm_flora','myfirstmod:realm_resources','myfirstmod:waystone_shrine','myfirstmod:rift_observatory','myfirstmod:mourning_cathedral'])
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
            self.assertIn(w,[16,32,64,128,256]);self.assertIn(h,[16,32,64,128,256])

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

    def test_endgame_recipes_and_journal_components(self):
        journal=load(DATA/'recipe/expedition_journal.json')['result']
        self.assertEqual(journal['id'],'minecraft:written_book')
        self.assertTrue(journal['components']['minecraft:custom_data']['NullJournal'])
        self.assertIn('minecraft:written_book_content',journal['components'])
        aegis=load(DATA/'recipe/rift_aegis.json')
        self.assertEqual({entry['item'] for entry in aegis['ingredients']},
                         {'minecraft:shield','myfirstmod:warden_crest','myfirstmod:resonance_matrix'})
        self.assertEqual(aegis['result']['id'],'myfirstmod:rift_aegis')
        self.assertTrue((DATA/'loot_table/chests/vault_cache.json').exists())

    def test_advancement_and_recipe_links(self):
        for path in (DATA/'advancement').rglob('*.json'):
            advancement=load(path)
            parent=advancement.get('parent','')
            if parent.startswith('myfirstmod:'):
                self.assertTrue((DATA/('advancement/'+parent.split(':')[1]+'.json')).exists())
            for recipe in advancement.get('rewards',{}).get('recipes',[]):
                self.assertTrue((DATA/('recipe/'+recipe.split(':')[1]+'.json')).exists())

    def test_dynamic_hud_translation_parameters(self):
        language=load(ASSETS/'lang/en_us.json')
        for key in ['hud.myfirstmod.trial_active','hud.myfirstmod.trial_rest','journal.myfirstmod.progress']:
            self.assertEqual(language[key].count('%s'),5,key)
        for oath in range(4):self.assertIn('oath.myfirstmod.'+str(oath),language)
        for route in range(3):self.assertIn('route.myfirstmod.'+str(route),language)

    def test_wilds_blocks_are_complete(self):
        source=(ROOT/'src/main/java/dev/qynl/myfirstmod/block/ModBlocks.java').read_text()
        names=set(re.findall(r'(?:stone|building)\("([a-z_]+)"',source))
        self.assertEqual(len(names),17)
        language=load(ASSETS/'lang/en_us.json')
        for name in names:
            self.assertTrue((ASSETS/f'blockstates/{name}.json').exists(),name)
            self.assertTrue((ASSETS/f'models/block/{name}.json').exists(),name)
            self.assertTrue((ASSETS/f'models/item/{name}.json').exists(),name)
            self.assertTrue((DATA/f'loot_table/blocks/{name}.json').exists(),name)
            self.assertIn('block.myfirstmod.'+name,language)

    def test_ore_harvesting_contracts(self):
        mineable=load(RES/'data/minecraft/tags/block/mineable/pickaxe.json')['values']
        iron=load(RES/'data/minecraft/tags/block/needs_iron_tool.json')['values']
        for ore,material in [('resonite_ore','raw_resonite'),('prism_ore','prism_dust'),('cinder_ore','cinder_pearl')]:
            self.assertIn('myfirstmod:'+ore,mineable)
            self.assertIn('myfirstmod:'+ore,iron)
            table=load(DATA/f'loot_table/blocks/{ore}.json')
            children=table['pools'][0]['entries'][0]['children']
            self.assertEqual(children[0]['name'],'myfirstmod:'+ore)
            self.assertIn('minecraft:silk_touch',json.dumps(children[0]))
            self.assertEqual(children[1]['name'],'myfirstmod:'+material)
            self.assertIn('minecraft:fortune',json.dumps(children[1]))

    def test_all_custom_recipe_items_exist(self):
        source=(ROOT/'src/main/java/dev/qynl/myfirstmod/item/ModItems.java').read_text()
        blocks=(ROOT/'src/main/java/dev/qynl/myfirstmod/block/ModBlocks.java').read_text()
        known=set(re.findall(r'register\("([a-z_]+)"',source)) | set(re.findall(r'(?:stone|building)\("([a-z_]+)"',blocks))
        def check(value):
            if isinstance(value,dict):
                for key,entry in value.items():
                    if key in ['id','item'] and isinstance(entry,str) and entry.startswith('myfirstmod:'):
                        self.assertIn(entry.split(':')[1],known)
                    check(entry)
            elif isinstance(value,list):
                for entry in value:check(entry)
        for recipe in (DATA/'recipe').glob('*.json'):check(load(recipe))

    def test_caves_and_armor_assets(self):
        noise=load(DATA/'worldgen/noise_settings/null_realm.json')
        self.assertEqual(noise['default_block']['Name'],'myfirstmod:nullstone')
        cave=noise['noise_router']['final_density']['argument']['argument']
        self.assertEqual(cave['type'],'minecraft:range_choice')
        self.assertGreater(cave['min_inclusive'],-59)
        for layer in [1,2]:
            self.assertTrue((ASSETS/f'textures/models/armor/resonite_layer_{layer}.png').exists())
        self.assertTrue((DATA/'worldgen/configured_feature/waystone_shrine.json').exists())

    def test_convergence_resources(self):
        for biome in (DATA/'worldgen/biome').glob('*.json'):
            self.assertIn('myfirstmod:rift_observatory',json.dumps(load(biome)))
        self.assertTrue((DATA/'worldgen/configured_feature/rift_observatory.json').exists())
        self.assertEqual(len(load(ASSETS/'blockstates/hush_nursery.json')['variants']),4)
        self.assertTrue((ASSETS/'textures/entity/rift_herald.png').exists())
        for rune in ['vigor_rune','gale_rune','focus_rune']:
            self.assertIn('myfirstmod:astral_core',json.dumps(load(DATA/f'recipe/{rune}.json')))

    def test_pilgrimage_resources(self):
        language=load(ASSETS/'lang/en_us.json')
        for biome in (DATA/'worldgen/biome').glob('*.json'):
            self.assertIn('myfirstmod:mourning_cathedral',json.dumps(load(biome)))
            self.assertIn('region.myfirstmod.'+biome.stem,language)
        for name in ['funerary_seal','mourning_reliquary']:
            self.assertEqual(set(load(ASSETS/f'blockstates/{name}.json')['variants']),{'rite=0','rite=1','rite=2','rite=3'})
        for name,size in [('rift_sentinel',(256,64)),('shardstalker',(128,64))]:
            raw=(ASSETS/f'textures/entity/{name}_pilgrimage.png').read_bytes()
            self.assertEqual(struct.unpack('>II',raw[16:24]),size)
        self.assertTrue((DATA/'loot_table/chests/pilgrim_cache.json').exists())
        self.assertFalse((DATA/'recipe/mourning_ember.json').exists(),'Embers must remain exploration rewards')

    def test_generators_are_reproducible(self):
        paths=list(RES.rglob('*'))
        before={str(p.relative_to(RES)):p.read_bytes() for p in paths if p.is_file()}
        for script in ['generate_art.py','generate_realm_data.py','generate_loot.py','generate_wilds.py','generate_convergence.py','generate_pilgrimage.py']:
            subprocess.run([sys.executable,str(ROOT/'scripts'/script)],check=True)
        after={str(p.relative_to(RES)):p.read_bytes() for p in RES.rglob('*') if p.is_file()}
        self.assertEqual(before,after)

if __name__=='__main__': unittest.main()

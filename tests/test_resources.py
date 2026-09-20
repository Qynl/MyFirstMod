"""Fast resource-contract tests, not a substitute for Loom or an in-game test."""
import gzip
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
        self.assertEqual(len(biomes),8)
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
                    self.assertIn(configured['type'],['myfirstmod:realm_ruins','myfirstmod:realm_flora','myfirstmod:realm_resources','myfirstmod:waystone_shrine','myfirstmod:rift_observatory','myfirstmod:mourning_cathedral','myfirstmod:forgotten_memorial','myfirstmod:realm_scenery','myfirstmod:region_signatures','myfirstmod:drowned_archive'])
            monsters={s['type'] for s in biome['spawners']['monster']}
            self.assertTrue({'myfirstmod:rift_sentinel','myfirstmod:shardstalker'}<=monsters,path.name)
            self.assertEqual('minecraft:drowned' in monsters,path.stem=='drowned_stacks')
            creatures={s['type'] for s in biome['spawners'].get('creature',[])}
            self.assertEqual('myfirstmod:veil_wisp' in creatures,path.stem in ('veil_highlands','shard_spires'))

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
        self.assertEqual(len(names),23)
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

    def test_remembrance_contracts(self):
        for biome in (DATA/'worldgen/biome').glob('*.json'):
            self.assertIn('myfirstmod:forgotten_memorial',json.dumps(load(biome)))
        lang=load(ASSETS/'lang/en_us.json')
        for i in range(7):self.assertIn('contract.myfirstmod.'+str(i),lang)
        for i in range(6):self.assertIn('memory.myfirstmod.'+str(i),lang)
        for i in range(4):self.assertIn('vow.myfirstmod.'+str(i),lang)
        self.assertTrue((DATA/'loot_table/chests/memorial_cache.json').exists())
        for seal in ['iron_vow','ember_vow','mist_vow']:
            self.assertEqual(json.dumps(load(DATA/f'recipe/{seal}.json')).count('myfirstmod:memory_shard'),2)

    def test_current_readme_links_and_portal_assets(self):
        readme=(ROOT/'README.md').read_text()
        version=re.search(r'mod_version=(.+)',(ROOT/'gradle.properties').read_text()).group(1)
        self.assertIn(version,readme)
        for link in re.findall(r'\]\(([^)]+)\)',readme):
            if '://' not in link and not link.startswith('#'):
                self.assertTrue((ROOT/link.split('#')[0]).exists(),link)
        for generator in (ROOT/'scripts').glob('generate_*.py'):
            self.assertIn(generator.name,readme)
        self.assertFalse((DATA/'recipe/portal_frame.json').exists())
        gate=load(ASSETS/'lang/en_us.json')['journal.myfirstmod.gate']
        self.assertIn('Echo Shard',gate)
        self.assertIn('22x8',gate)
        self.assertNotIn('4-wide',gate)
        self.assertEqual(set(load(ASSETS/'blockstates/void_portal.json')['variants']),{'axis=x','axis=z'})
        self.assertTrue((ASSETS/'textures/block/void_portal.png.mcmeta').exists())
        for biome in (DATA/'worldgen/biome').glob('*.json'):
            self.assertIn('myfirstmod:realm_scenery',json.dumps(load(biome)))
            self.assertIn('ambient_sound',load(biome)['effects'])

    def test_gallery_is_reproducible_and_labeled(self):
        names=['item-gallery.svg','creature-gallery.svg','biome-palettes.svg','ancient-city-gateway.svg']
        before={name:(ROOT/'docs/images'/name).read_bytes() for name in names}
        subprocess.run([sys.executable,str(ROOT/'scripts/generate_gallery.py')],check=True)
        for name in names:
            content=(ROOT/'docs/images'/name).read_bytes()
            self.assertTrue(content==before[name], name+" is not reproducible")
            import xml.etree.ElementTree as ET
            tree=ET.fromstring(content)
            self.assertEqual(tree.attrib['role'],'img')
            self.assertIsNotNone(tree.find('{http://www.w3.org/2000/svg}desc'))
        models=before['creature-gallery.svg'].decode()
        for name in ['NullWarden','GraveRegent','RootboundPrior','RiftHerald','RiftSentinel','Shardstalker']:
            self.assertIn('data-model="'+name+'"',models)
        self.assertIn('not gameplay screenshots',models)

    def test_hollow_keep_resources(self):
        dimension=load(DATA/'dimension/hollow_keep.json')
        self.assertEqual(dimension['generator']['type'],'minecraft:flat')
        self.assertEqual(dimension['generator']['settings']['structure_overrides'],[])
        self.assertEqual(load(DATA/'loot_table/entities/grave_regent.json')['pools'],[])
        self.assertIn('myfirstmod:regent_crest',json.dumps(load(DATA/'recipe/requiem_glaive.json')))
        self.assertEqual(struct.unpack('>II',(ASSETS/'textures/entity/grave_regent.png').read_bytes()[16:24]),(128,128))
        for name in ['keep_gate','keep_ward','keep_heart']:
            self.assertTrue((ASSETS/f'blockstates/{name}.json').exists())
        lang=load(ASSETS/'lang/en_us.json')
        for attack in range(1,4):self.assertIn('regent.myfirstmod.attack.'+str(attack),lang)

    def test_kingdom_structure_and_dungeon_contracts(self):
        structure=load(DATA/'worldgen/structure/rootbound_monastery.json')
        self.assertEqual(structure['type'],'myfirstmod:rootbound_monastery')
        self.assertEqual(set(structure),{'type','biomes','step','spawn_overrides','terrain_adaptation'})
        self.assertEqual(structure['biomes'],'#myfirstmod:has_rootbound_monastery')
        self.assertEqual(load(DATA/'tags/worldgen/biome/has_rootbound_monastery.json')['values'],['myfirstmod:hushed_grove'])
        pool=load(DATA/'worldgen/template_pool/rootbound_monastery.json')
        self.assertEqual(pool['elements'][0]['element']['location'],'myfirstmod:rootbound_monastery')
        placement=load(DATA/'worldgen/structure_set/rootbound_monastery.json')['placement']
        self.assertGreater(placement['spacing'],placement['separation'])
        self.assertEqual(load(DATA/'loot_table/entities/rootbound_prior.json')['pools'],[])
        language=load(ASSETS/'lang/en_us.json')
        for key in ['entity.myfirstmod.rootbound_prior','block.myfirstmod.root_heart','block.myfirstmod.cloister_bell',
                    'block.myfirstmod.root_reliquary','item.myfirstmod.rootbound_seal','item.myfirstmod.briarbrand',
                    'item.myfirstmod.briarbrand.tooltip','journal.myfirstmod.monastery','monastery.myfirstmod.riddle',
                    'monastery.myfirstmod.bell','monastery.myfirstmod.spent','monastery.myfirstmod.active',
                    'monastery.myfirstmod.blocked','monastery.myfirstmod.peaceful','monastery.myfirstmod.begin',
                    'monastery.myfirstmod.reward','message.myfirstmod.gate_charging','message.myfirstmod.gate_cancelled',
                    'prior.myfirstmod.recovery']:
            self.assertIn(key,language)
        for attack in range(1,4):self.assertIn('prior.myfirstmod.attack.'+str(attack),language)
        for name in ['root_heart','cloister_bell','root_reliquary']:
            variants=set(load(ASSETS/f'blockstates/{name}.json')['variants'])
            self.assertEqual(variants,{f'bells={b},facing={f}' for b in range(4) for f in ['north','east','south','west']})
            self.assertTrue((ASSETS/f'textures/block/{name}.png').exists())
        for item in ['rootbound_seal','briarbrand']:
            self.assertTrue((ASSETS/f'models/item/{item}.json').exists())
            self.assertTrue((ASSETS/f'textures/item/{item}.png').exists())
        self.assertEqual(struct.unpack('>II',(ASSETS/'textures/entity/rootbound_prior.png').read_bytes()[16:24]),(128,128))
        recipe=load(DATA/'recipe/briarbrand.json')
        self.assertIn('myfirstmod:rootbound_seal',json.dumps(recipe))
        self.assertEqual(load(DATA/'advancement/recipes/briarbrand.json')['rewards']['recipes'],['myfirstmod:briarbrand'])
        self.assertIn('journal.myfirstmod.monastery',(ROOT/'src/main/java/dev/qynl/myfirstmod/realm/ExpeditionJournal.java').read_text())
        self.assertEqual(self.monastery_blocks()[((23,1,10))],'myfirstmod:root_heart')

    def monastery_blocks(self):
        """Decode the real structure NBT so geometry regressions fail here, not in-game."""
        raw=gzip.decompress((DATA/'structure/rootbound_monastery.nbt').read_bytes())
        self.assertEqual(raw[0],10);self.assertEqual(raw[1:3],b'\0\0','Unnamed root compound')
        pos=[3]
        def read(kind):
            if kind==1:pos[0]+=1;return raw[pos[0]-1]
            if kind==3:pos[0]+=4;return struct.unpack('>i',raw[pos[0]-4:pos[0]])[0]
            if kind==8:
                length=struct.unpack('>H',raw[pos[0]:pos[0]+2])[0];pos[0]+=2
                value=raw[pos[0]:pos[0]+length].decode();pos[0]+=length;return value
            if kind==9:
                inner=raw[pos[0]];pos[0]+=1;count=read(3)
                return [read(inner) for _ in range(count)]
            if kind==10:
                out={}
                while raw[pos[0]]:
                    kind=raw[pos[0]];pos[0]+=1;name=read(8);out[name]=read(kind)
                pos[0]+=1;return out
            raise AssertionError('Unsupported NBT tag '+str(kind))
        root=read(10)
        self.assertEqual(root['size'],[47,18,47])
        self.assertEqual(root['DataVersion'],3955)
        palette=[entry['Name'] for entry in root['palette']]
        blocks={}
        for entry in root['blocks']:
            key=tuple(entry['pos'])
            self.assertNotIn(key,blocks,'Duplicate structure block position')
            self.assertLess(entry['state'],len(palette))
            blocks[key]=palette[entry['state']]
            if 'nbt' in entry:blocks[(key,'nbt')]=entry['nbt']
        self.assertEqual(len([k for k in blocks if isinstance(k[0],int)]),47*47*18)
        return blocks

    def test_monastery_template_is_playable(self):
        blocks=self.monastery_blocks()
        for position,expected in [((7,1,26),'myfirstmod:cloister_bell'),((39,7,26),'myfirstmod:cloister_bell'),
                                  ((23,1,43),'myfirstmod:waystone'),((23,1,27),'minecraft:iron_bars'),
                                  ((23,1,35),'minecraft:air'),((39,1,39),'minecraft:dark_oak_stairs'),
                                  ((39,2,39),'minecraft:air'),((39,3,39),'minecraft:air'),
                                  ((39,6,29),'myfirstmod:hush_planks'),((5,1,35),'minecraft:spawner'),
                                  ((42,7,24),'minecraft:spawner'),((9,1,24),'minecraft:chest'),
                                  ((36,7,30),'minecraft:chest'),((23,2,46),'minecraft:air'),((0,3,23),'minecraft:mossy_stone_bricks')]:
            self.assertEqual(blocks[position],expected,position)
        self.assertEqual(blocks[((5,1,35),'nbt')]['SpawnData']['entity']['id'],'myfirstmod:rift_sentinel')
        self.assertEqual(blocks[((42,7,24),'nbt')]['SpawnData']['entity']['id'],'myfirstmod:shardstalker')
        for chest in [(9,1,24),(36,7,30)]:
            self.assertEqual(blocks[(chest,'nbt')]['LootTable'],'myfirstmod:chests/monastery_cache')
        # Every telegraphed attack path stays inside the open chapterhouse: lane, crown arms and headroom.
        for forward in range(1,10):
            for side in [-1,0,1]:
                self.assertEqual(blocks[(23+side,1,10+forward)],'minecraft:air',(side,forward))
        for radius in range(3,8):
            for dx,dz in [(radius,0),(-radius,0),(0,radius),(0,-radius)]:
                self.assertEqual(blocks[(23+dx,1,10+dz)],'minecraft:air',(dx,dz))
        for y in [2,3,4]:
            self.assertEqual(blocks[(23,y,10)],'minecraft:air',y)
        # Four root pillars flank (never cross) the telegraphed paths and act as partial cover.
        pillars={(16,4),(30,4),(16,18),(30,18)}
        for x in range(15,32):
            for z in range(3,20):
                expected='myfirstmod:hushwood' if (x,z) in pillars else 'myfirstmod:root_heart' if (x,z)==(23,10) else 'minecraft:air'
                self.assertEqual(blocks[(x,1,z)],expected,(x,z))

    def test_generators_are_reproducible(self):
        paths=list(RES.rglob('*'))
        before={str(p.relative_to(RES)):p.read_bytes() for p in paths if p.is_file()}
        for script in ['generate_art.py','generate_realm_data.py','generate_loot.py','generate_wilds.py','generate_convergence.py','generate_pilgrimage.py','generate_remembrance.py','generate_keep.py','generate_kingdom.py']:
            subprocess.run([sys.executable,str(ROOT/'scripts'/script)],check=True)
        after={str(p.relative_to(RES)):p.read_bytes() for p in RES.rglob('*') if p.is_file()}
        self.assertEqual(before,after)

if __name__=='__main__': unittest.main()

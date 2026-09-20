"""Contract tests for the 2.0.0-alpha.6 refinement pass: build set, remembrance, elites."""
import json, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/myfirstmod'
TAGS=ROOT/'src/main/resources/data/minecraft/tags'
ASSETS=ROOT/'src/main/resources/assets/myfirstmod'
SRC=ROOT/'src/main/java/dev/qynl/myfirstmod'
def load(p):return json.loads(Path(p).read_text())
STONES=['nullstone','polished_nullstone','nullstone_bricks','veilstone','prismstone','cinderstone']
PREFIX={'nullstone':'nullstone','polished_nullstone':'polished_nullstone','nullstone_bricks':'nullstone_brick',
        'veilstone':'veilstone','prismstone':'prismstone','cinderstone':'cinderstone','hush_planks':'hush_plank'}
PIECES=[(f'{PREFIX[b]}_{p}',b,p in ('stairs','slab','wall') and True) for b in STONES for p in ['stairs','slab','wall']]
PIECES+=[('hush_plank_stairs','hush_planks',False),('hush_plank_slab','hush_planks',False)]
LANDMARKS=['veil_watchtower','brine_chapel','geode_garden','slag_camp','caravan_wreck','echo_fissure','heartwood_circle','fen_shrine']
class BuildSetTests(unittest.TestCase):
    def test_every_piece_has_blockstate_models_item_loot_recipe(self):
        for name,base,stone in PIECES:
            blockstate=load(ASSETS/f'blockstates/{name}.json')
            models=set()
            if 'variants' in blockstate:
                models|={v['model'] for v in blockstate['variants'].values()}
            else:
                models|={part['apply']['model'] for part in blockstate['multipart']}
            for model in models:
                self.assertTrue((ASSETS/f"models/{model.split(':')[1]}.json").exists(),model)
            self.assertTrue((ASSETS/f'models/item/{name}.json').exists())
            self.assertTrue((DATA/f'loot_table/blocks/{name}.json').exists())
            recipe=load(DATA/f'recipe/{name}.json')
            self.assertEqual(recipe['key']['#']['item'],f'myfirstmod:{base}')
            self.assertEqual((DATA/f'recipe/{name}_from_stonecutting.json').exists(),stone)
    def test_blockstate_shapes(self):
        for name,base,stone in PIECES:
            blockstate=load(ASSETS/f'blockstates/{name}.json')
            if name.endswith('_stairs'):self.assertEqual(len(blockstate['variants']),40)
            elif name.endswith('_wall'):self.assertEqual(len(blockstate['multipart']),9)
            else:self.assertEqual(set(blockstate['variants']),{'type=bottom','type=top','type=double'})
    def test_slab_double_loot_doubles(self):
        for name,base,stone in PIECES:
            if not name.endswith('_slab'):continue
            loot=load(DATA/f'loot_table/blocks/{name}.json')
            function=loot['pools'][0]['entries'][0]['functions'][0]
            self.assertEqual(function['count'],2)
            self.assertEqual(function['conditions'][0]['properties'],{'type':'double'})
    def test_tags_and_lang(self):
        pickaxe=set(load(TAGS/'block/mineable/pickaxe.json')['values'])
        axe=set(load(TAGS/'block/mineable/axe.json')['values'])
        lang=load(ASSETS/'lang/en_us.json')
        for name,base,stone in PIECES:
            self.assertIn(f'myfirstmod:{name}',pickaxe if stone else axe)
            self.assertIn(f'block.myfirstmod.{name}',lang)
    def test_remembrance_advancements(self):
        for kind in LANDMARKS:
            advancement=load(DATA/f'advancement/landmarks/{kind}.json')
            self.assertEqual(advancement['parent'],'myfirstmod:enter_realm')
            self.assertEqual(advancement['criteria']['discover']['trigger'],'minecraft:impossible')
            self.assertEqual(advancement['rewards']['experience'],50)
        crown=load(DATA/'advancement/landmarks/cartographer_of_the_null.json')
        self.assertEqual(crown['criteria']['complete']['trigger'],'minecraft:impossible')
        self.assertEqual(crown['rewards']['loot'],['myfirstmod:chests/wayfarer_bounty'])
        self.assertTrue((DATA/'loot_table/chests/wayfarer_bounty.json').exists())
    def test_java_contracts(self):
        discovery=(SRC/'realm/LandmarkDiscovery.java').read_text()
        for cache in ['watch_cache','chapel_cache','geode_cache','camp_cache','caravan_cache','fissure_cache','circle_cache','shrine_cache']:
            self.assertIn(f'"{cache}"',discovery)
        for kind in LANDMARKS:
            self.assertIn(f'"landmarks/"+',discovery)
        blocks=(SRC/'block/ModBlocks.java').read_text()
        for name,base,stone in PIECES:
            self.assertIn(f'"{name}"',blocks)
        self.assertIn('LandmarkDiscovery.register();',(SRC/'MyFirstMod.java').read_text())
        landmarks=(SRC/'realm/KingdomLandmarks.java').read_text()
        for token in ['The Brine Keeper','Warden of the Fissure','hand_items','hand_drop_chances','active_effects','minecraft:trident']:
            self.assertIn(token,landmarks)
if __name__=='__main__':unittest.main()

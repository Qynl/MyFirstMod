"""Contract tests for the eight landmark families of 2.0.0-alpha.5."""
import json, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/myfirstmod'
ASSETS=ROOT/'src/main/resources/assets/myfirstmod'
SRC=ROOT/'src/main/java/dev/qynl/myfirstmod'
def load(p):return json.loads(Path(p).read_text())
LANDMARKS=['veil_watchtower','brine_chapel','geode_garden','slag_camp','caravan_wreck','echo_fissure','heartwood_circle','fen_shrine']
MATRIX={'hushed_grove':['heartwood_circle'],'veil_highlands':['veil_watchtower'],'luminous_fen':['fen_shrine'],
        'drowned_stacks':['brine_chapel'],'prism_wastes':['geode_garden'],'shard_spires':['geode_garden'],
        'cinder_steps':['slag_camp'],'ember_vents':['slag_camp']}
LOOT={'veil_watchtower':'watch_cache','brine_chapel':'chapel_cache','geode_garden':'geode_cache','slag_camp':'camp_cache',
      'caravan_wreck':'caravan_cache','echo_fissure':'fissure_cache','heartwood_circle':'circle_cache','fen_shrine':'shrine_cache'}
class LandmarkTests(unittest.TestCase):
    def test_every_landmark_has_feature_and_placement(self):
        for name in LANDMARKS:
            configured=load(DATA/f'worldgen/configured_feature/{name}.json')
            self.assertEqual(configured['type'],'myfirstmod:'+name)
            placed=load(DATA/f'worldgen/placed_feature/{name}.json')
            self.assertEqual(placed['feature'],'myfirstmod:'+name)
            self.assertEqual(placed['placement'][0]['type'],'minecraft:rarity_filter')
    def test_biome_membership_matrix(self):
        for biome in MATRIX:
            features=[f for stage in load(DATA/f'worldgen/biome/{biome}.json')['features'] for f in stage]
            for landmark in LANDMARKS:
                self.assertEqual('myfirstmod:'+landmark in features,landmark in MATRIX[biome]+['caravan_wreck','echo_fissure'],(biome,landmark))
    def test_caches_exist_with_guaranteed_roll(self):
        for name in LANDMARKS:
            table=load(DATA/f'loot_table/chests/{LOOT[name]}.json')
            self.assertEqual(table['type'],'minecraft:chest')
            self.assertEqual(table['pools'][0]['rolls'],1)
            self.assertTrue(table['pools'][1]['entries'])
    def test_java_contracts_per_landmark(self):
        source=(SRC/'realm/KingdomLandmarks.java').read_text()
        for name in LANDMARKS:
            self.assertIn('Identifier.of("myfirstmod","'+name+'")',source)
        for table in LOOT.values():
            self.assertIn('"'+table+'"',source)
        self.assertIn('"chests/"+table',source)
        self.assertIn('spawner(w,s.add(-3,0,-3),"minecraft:drowned")',source)
        self.assertIn('spawner(w,s.add(0,-2,2),"myfirstmod:rift_sentinel")',source)
        self.assertIn('static boolean reserved(int cx,int cz)',source)
        self.assertIn('KingdomLandmarks.register();',(SRC/'realm/RealmFeatures.java').read_text())
    def test_journal_landmarks_page(self):
        self.assertIn('journal.myfirstmod.landmarks',(SRC/'realm/ExpeditionJournal.java').read_text())
        language=load(ASSETS/'lang/en_us.json')
        self.assertIn('LANDMARKS OF THE KINGDOM',language['journal.myfirstmod.landmarks'])
if __name__=='__main__':unittest.main()

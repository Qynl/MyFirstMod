"""Contract tests for the Silent Capital finale: unique citadel, Crown Gate, court, crown."""
import gzip, json, struct, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/myfirstmod'
ASSETS=ROOT/'src/main/resources/assets/myfirstmod'
SRC=ROOT/'src/main/java/dev/qynl/myfirstmod'
def load(p):return json.loads(Path(p).read_text())
def parse_nbt(path):
    raw=gzip.decompress(Path(path).read_bytes())
    pos=3
    def read_str():
        nonlocal pos
        n=struct.unpack('>H',raw[pos:pos+2])[0];pos+=2
        v=raw[pos:pos+n].decode();pos+=n;return v
    def skip_payload(kind):
        nonlocal pos
        if kind==8:read_str()
        elif kind==3:pos+=4
        elif kind==10:
            while True:
                t=raw[pos];pos+=1
                if t==0:return
                read_str();skip_payload(t)
        elif kind==9:
            inner=raw[pos];pos+=1
            n=struct.unpack('>i',raw[pos:pos+4])[0];pos+=4
            for _ in range(n):skip_payload(inner)
        else:raise AssertionError(kind)
    assert raw[0]==10
    pos=3
    def collect(kind,key=None):
        nonlocal pos
        if kind==8:
            v=read_str();return v
        if kind==3:
            v=struct.unpack('>i',raw[pos:pos+4])[0];pos+=4;return v
        if kind==10:
            out={}
            while True:
                t=raw[pos];pos+=1
                if t==0:return out
                k=read_str();out[k]=collect(t,k)
        if kind==9:
            inner=raw[pos];pos+=1
            n=struct.unpack('>i',raw[pos:pos+4])[0];pos+=4
            return [collect(inner) for _ in range(n)]
        raise AssertionError(kind)
    root=collect(10)
    return root
class CapitalTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.nbt=parse_nbt(DATA/'structure/silent_capital.nbt')
    def test_template_is_a_real_citadel(self):
        self.assertEqual(self.nbt['size'],[63,10,63])
        names=[e['Name'] for e in self.nbt['palette']]
        for token in ['myfirstmod:crown_gate','myfirstmod:waystone','myfirstmod:nullstone_bricks','myfirstmod:veilstone','myfirstmod:prismstone','myfirstmod:hush_leaves','minecraft:spawner']:
            self.assertIn(token,names)
        self.assertGreater(len(self.nbt['blocks']),20000)
    def test_gate_throne_and_waystone_positions(self):
        palette=self.nbt['palette']
        def at(x,y,z):
            for b in self.nbt['blocks']:
                if b['pos']==[x,y,z]:return palette[b['state']]
            return None
        self.assertEqual(at(31,1,1)['Name'],'myfirstmod:crown_gate')
        self.assertEqual(at(31,1,1)['Properties'],{'phase':'0'})
        self.assertEqual(at(31,1,3)['Name'],'myfirstmod:waystone')
        self.assertEqual(at(31,5,38)['Name'],'myfirstmod:nullstone_bricks')
        self.assertEqual(at(31,6,55)['Name'],'myfirstmod:polished_nullstone')
        self.assertEqual(at(31,1,55)['Name'],'minecraft:spawner')
        entry=[b for b in self.nbt['blocks'] if b['pos']==[31,1,55]][0]
        self.assertEqual(entry['nbt']['SpawnData']['entity']['id'],'myfirstmod:rift_sentinel')
    def test_structure_is_unique_and_routed(self):
        structure=load(DATA/'worldgen/structure/silent_capital.json')
        self.assertEqual(structure['type'],'myfirstmod:silent_capital')
        self.assertEqual(structure['terrain_adaptation'],'beard_thin')
        tag=load(DATA/'tags/worldgen/biome/has_silent_capital.json')
        self.assertEqual(len(tag['values']),8)
        pool=load(DATA/'worldgen/template_pool/silent_capital.json')
        self.assertEqual(pool['elements'][0]['element']['location'],'myfirstmod:silent_capital')
        setfile=load(DATA/'worldgen/structure_set/silent_capital.json')
        self.assertEqual(setfile['structures'][0]['structure'],'myfirstmod:silent_capital')
        java=(SRC/'kingdom/CapitalStructure.java').read_text()
        self.assertIn('CHUNK_X=0,CHUNK_Z=-22',java)
        self.assertIn('context.chunkPos().x!=CHUNK_X||context.chunkPos().z!=CHUNK_Z',java)
    def test_crown_gate_states_and_court_rules(self):
        gate=load(ASSETS/'blockstates/crown_gate.json')['variants']
        self.assertEqual(sorted(gate),['phase=0','phase=1','phase=2'])
        for variant in gate.values():
            self.assertTrue((ASSETS/('textures/'+variant['model'].split(':',1)[1]+'.png')).exists())
        court=(SRC/'kingdom/CourtOfSeals.java').read_text()
        self.assertIn('record.seals&7)!=7',court)
        self.assertIn('CrownGateBlock.PHASE,1',court)
        self.assertIn('CrownGateBlock.PHASE,2',court)
        self.assertIn('chests/crown_cache',court)
        rules=(SRC/'kingdom/CourtRules.java').read_text()
        self.assertIn('waves(){return 3;}',rules)
        self.assertIn('chestOffset(){return new int[]{0,2,37};}',rules)
    def test_court_state_persistence(self):
        state=(SRC/'realm/RealmState.java').read_text()
        for token in ['public long courtPos;','public int courtWave;','courtActors','nbt.putLong("CourtPos",courtPos);']:
            self.assertIn(token,state)
    def test_crown_reward_and_effects(self):
        items=(SRC/'item/ModItems.java').read_text()
        self.assertIn('CROWN_SEAL',items)
        self.assertIn('SILENT_CROWN',items)
        crown=(SRC/'item/SilentCrown.java').read_text()
        self.assertIn('DamageTypes.FALL',crown)
        self.assertIn('StatusEffects.NIGHT_VISION',crown)
        main=(SRC/'MyFirstMod.java').read_text()
        self.assertIn('SilentCrown.shields',main)
        self.assertIn('SilentCrown::tick',main)
        self.assertTrue((DATA/'loot_table/chests/crown_cache.json').exists())
        self.assertTrue((DATA/'recipe/silent_crown.json').exists())
        language=load(ASSETS/'lang/en_us.json')
        for key in ['item.myfirstmod.crown_seal.tooltip','item.myfirstmod.silent_crown.tooltip']:
            self.assertIn(key,language)
    def test_journal_capital_pages(self):
        journal=(SRC/'realm/ExpeditionJournal.java').read_text()
        self.assertIn('journal.myfirstmod.capital',journal)
        self.assertIn('journal.myfirstmod.coronation',journal)
        language=load(ASSETS/'lang/en_us.json')
        self.assertIn('(0, -352)',language['journal.myfirstmod.capital'])
        self.assertIn('CORONATION',language['journal.myfirstmod.coronation'])
if __name__=='__main__':unittest.main()

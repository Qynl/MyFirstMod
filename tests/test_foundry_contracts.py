"""Contract tests for the Ashen Foundry milestone: third dungeon, seal chain, plinth, charm."""
import json, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/myfirstmod'
ASSETS=ROOT/'src/main/resources/assets/myfirstmod'
SRC=ROOT/'src/main/java/dev/qynl/myfirstmod'
def load(p):return json.loads(Path(p).read_text())
class FoundryTests(unittest.TestCase):
    def test_foundry_only_in_cinder_steps(self):
        for biome in ['hushed_grove','veil_highlands','luminous_fen','drowned_stacks','prism_wastes','shard_spires','cinder_steps','ember_vents']:
            data=load(DATA/f'worldgen/biome/{biome}.json')
            features=[f for stage in data['features'] for f in stage]
            self.assertEqual('myfirstmod:ashen_foundry' in features,biome=='cinder_steps',biome)
            self.assertEqual('myfirstmod:region_signatures' in features,biome!='hushed_grove' and biome not in ('luminous_fen','prism_wastes'),biome)
    def test_crucible_and_plinth_states(self):
        bell=load(ASSETS/'blockstates/ember_crucible.json')['variants']
        self.assertEqual(sorted(bell),['quenched=false','quenched=true'])
        for variant in bell.values():
            texture=ASSETS/('textures/'+load(ASSETS/('models/'+variant['model'].split(':',1)[1]+'.json'))['textures']['all'].split(':',1)[1]+'.png')
            self.assertTrue(texture.exists(),str(texture))
        plinth=load(ASSETS/'blockstates/seal_plinth.json')['variants']
        self.assertEqual(sorted(plinth),['seals=0','seals=1','seals=2','seals=3'])
        for variant in plinth.values():
            self.assertTrue((ASSETS/('textures/'+variant['model'].split(':',1)[1]+'.png')).exists())
    def test_slagglass_is_a_full_block(self):
        language=load(ASSETS/'lang/en_us.json')
        for path in ['blockstates/slagglass.json','models/block/slagglass.json','models/item/slagglass.json','textures/block/slagglass.png']:
            self.assertTrue((ASSETS/path).exists(),path)
        self.assertTrue((DATA/'loot_table/blocks/slagglass.json').exists())
        tag=load(DATA.parents[0]/'minecraft/tags/block/mineable/pickaxe.json')
        self.assertIn('myfirstmod:slagglass',tag['values'])
        self.assertIn('block.myfirstmod.slagglass',language)
    def test_seal_chain_persistence(self):
        record=(SRC/'realm/ExpeditionRecord.java').read_text()
        self.assertIn('public int seals;',record)
        self.assertIn('record.seals=nbt.getInt("Seals")&7;',record)
        self.assertIn('nbt.putInt("Seals",seals);',record)
        self.assertIn('Math.min(8, biomes.size())',record)
        state=(SRC/'realm/RealmState.java').read_text()
        self.assertIn('public long plinthPos;',state)
        self.assertIn('public int worldSeals;',state)
        self.assertIn('nbt.putLong("PlinthPos",plinthPos);',state)
    def test_quench_is_gated_and_one_way(self):
        quench=(SRC/'kingdom/FoundryQuench.java').read_text()
        self.assertIn('CrucibleBlock.QUENCHED',quench)
        self.assertIn('foundry.myfirstmod.once',quench)
        self.assertIn('foundry.myfirstmod.unworthy',quench)
        self.assertIn('record.seals&SealChain.ROOT',quench)
        self.assertIn('record.seals&SealChain.DROWNED',quench)
        self.assertIn('Blocks.LAVA',quench)
        self.assertIn('ModBlocks.SLAGGLASS',quench)
        self.assertIn('isChunkLoaded',quench)
        chain=(SRC/'kingdom/SealChain.java').read_text()
        self.assertIn('public static final int ROOT=1,DROWNED=2,CINDER=4;',chain)
        self.assertIn('DamageTypes.HOT_FLOOR',chain)
        self.assertIn('setFireTicks(0)',chain)
        main=(SRC/'MyFirstMod.java').read_text()
        self.assertIn('ServerLivingEntityEvents.ALLOW_DAMAGE',main)
        self.assertIn('SealChain.useSeal',main)
    def test_foundry_rewards_change_play(self):
        items=(SRC/'item/ModItems.java').read_text()
        self.assertIn('CINDER_SEAL',items)
        self.assertIn('CINDERWALK_CHARM',items)
        self.assertTrue((DATA/'loot_table/chests/foundry_cache.json').exists())
        self.assertTrue((DATA/'recipe/cinderwalk_charm.json').exists())
        self.assertTrue((DATA/'advancement/recipes/cinderwalk_charm.json').exists())
        language=load(ASSETS/'lang/en_us.json')
        for key in ['item.myfirstmod.cinder_seal.tooltip','item.myfirstmod.cinderwalk_charm.tooltip']:
            self.assertIn(key,language)
    def test_journal_chain_pages(self):
        journal=(SRC/'realm/ExpeditionJournal.java').read_text()
        self.assertIn('journal.myfirstmod.foundry',journal)
        self.assertIn('journal.myfirstmod.chain',journal)
        language=load(ASSETS/'lang/en_us.json')
        self.assertIn('ASHEN FOUNDRY',language['journal.myfirstmod.foundry'])
        self.assertIn('THE CHAIN OF SEALS',language['journal.myfirstmod.chain'])
    def test_chimney_signature_for_cinder_steps(self):
        signatures=(SRC/'realm/RegionSignatures.java').read_text()
        self.assertIn('chimney(w,surface,random)',signatures)
        self.assertIn('ModBlocks.CINDERSTONE',signatures)
        self.assertIn('Blocks.CAMPFIRE',signatures)
if __name__=='__main__':unittest.main()

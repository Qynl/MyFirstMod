"""Contract tests for the eight-region kingdom: routed biomes, terrain shaping, authored
signatures, the Drowned Archive, the Tide Bell drain, progression rewards and the wisp."""
import json, re, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/myfirstmod'
ASSETS=ROOT/'src/main/resources/assets/myfirstmod'
SRC=ROOT/'src/main/java/dev/qynl/myfirstmod'
def load(p):return json.loads(Path(p).read_text())
BIOMES=['hushed_grove','veil_highlands','luminous_fen','drowned_stacks','prism_wastes','shard_spires','cinder_steps','ember_vents']
class RegionTests(unittest.TestCase):
    def test_climate_bands_tile_the_whole_axis(self):
        entries=load(DATA/'dimension/null_realm.json')['generator']['biome_source']['biomes']
        self.assertEqual([e['biome'] for e in entries],['myfirstmod:'+b for b in BIOMES])
        bands=[e['parameters']['temperature'] for e in entries]
        self.assertEqual(bands[0][0],-1)
        for i in range(1,len(bands)):self.assertEqual(bands[i][0],bands[i-1][1])
        self.assertGreaterEqual(bands[-1][1],1)
        for lo,hi in bands:self.assertLess(lo,hi)
    def test_all_biomes_exist_with_surface_and_fog(self):
        for biome in BIOMES:
            data=load(DATA/f'worldgen/biome/{biome}.json')
            self.assertEqual(data['effects']['fog_color']&0xFF000000,0)
            self.assertIn('ambient_sound',data['effects'])
            self.assertIn('particle',data['effects'])
            features=[f for stage in data['features'] for f in stage]
            self.assertIn('myfirstmod:realm_scenery',features)
    def test_terrain_shaping_is_gated_by_climate(self):
        router=load(DATA/'worldgen/noise_settings/null_realm.json')['noise_router']
        terrain=json.dumps(router['initial_density_without_jaggedness'])
        for token in ['range_choice','minecraft:steep','myfirstmod:climate','myfirstmod:basins']:
            self.assertIn(token,terrain)
        surface=json.dumps(load(DATA/'worldgen/noise_settings/null_realm.json')['surface_rule'])
        for top in ['brinesilt','veilstone','vent_basalt']:
            self.assertIn(top,surface)
    def test_signatures_only_in_new_biomes(self):
        for biome in BIOMES:
            data=load(DATA/f'worldgen/biome/{biome}.json')
            vegetation=[f for stage in data['features'] for f in stage if 'signatures' in f]
            self.assertEqual(bool(vegetation),biome in ('veil_highlands','drowned_stacks','shard_spires','ember_vents'),biome)
            structures=[f for stage in data['features'] for f in stage if 'archive' in f]
            self.assertEqual(bool(structures),biome=='drowned_stacks',biome)
    def test_region_blocks_are_complete_assets(self):
        language=load(ASSETS/'lang/en_us.json')
        for name in ['brinesilt','veilstone','vent_basalt','spire_crystal','oxidized_trim']:
            self.assertTrue((ASSETS/f'blockstates/{name}.json').exists(),name)
            self.assertTrue((ASSETS/f'textures/block/{name}.png').exists(),name)
            self.assertTrue((DATA/f'loot_table/blocks/{name}.json').exists(),name)
            self.assertIn('block.myfirstmod.'+name,language)
        bell=load(ASSETS/'blockstates/tide_bell.json')['variants']
        self.assertEqual(sorted(bell),['drained=false','drained=true'])
        for variant in bell.values():
            model=ASSETS/('models/'+variant['model'].split(':',1)[1]+'.json')
            texture=ASSETS/('textures/'+load(model)['textures']['all'].split(':',1)[1]+'.png')
            self.assertTrue(texture.exists(),str(texture))
        self.assertIn('block.myfirstmod.tide_bell',language)
    def test_archive_tide_contract(self):
        source=(SRC/'kingdom/ArchiveTides.java').read_text()
        self.assertIn('TideBellBlock.DRAINED',source)
        self.assertIn('archive.myfirstmod.once',source)
        self.assertIn('Blocks.WATER',source)
        self.assertIn('canPlayerModifyAt',source)
        self.assertIn('isChunkLoaded',source)
        feature=(SRC/'kingdom/ArchiveFeature.java').read_text()
        self.assertIn('RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/archive_cache"))',feature)
        self.assertIn('addPendingBlockEntityNbt',feature)
        self.assertIn('ModBlocks.TIDE_BELL',feature)
        self.assertIn('ModBlocks.WAYSTONE',feature)
        language=load(ASSETS/'lang/en_us.json')
        for key in ['archive.myfirstmod.drained','archive.myfirstmod.once','archive.myfirstmod.dry','archive.myfirstmod.unloaded']:
            self.assertIn(key,language)
        self.assertEqual(language['archive.myfirstmod.drained'].count('%s'),1)
        self.assertTrue((DATA/'loot_table/chests/archive_cache.json').exists())
    def test_archive_geometry_is_pure_and_bounded(self):
        rules=(SRC/'kingdom/ArchiveRules.java').read_text()
        self.assertIn('radius(){return 5;}',rules)
        self.assertIn('floorOffset(){return -5;}',rules)
        self.assertIn('ceilOffset(){return -2;}',rules)
    def test_progression_rewards_change_play(self):
        items=(SRC/'item/ModItems.java').read_text()
        self.assertIn('DROWNED_SEAL',items)
        self.assertIn('TIDE_LANTERN',items)
        lantern=(SRC/'item/TideLanternItem.java').read_text()
        self.assertIn('WATER_BREATHING,800',lantern)
        self.assertIn('NIGHT_VISION,400',lantern)
        self.assertIn('set(this,1800)',lantern)
        self.assertTrue((DATA/'recipe/tide_lantern.json').exists())
        self.assertTrue((DATA/'advancement/recipes/tide_lantern.json').exists())
        journal=(SRC/'realm/ExpeditionJournal.java').read_text()
        self.assertIn('journal.myfirstmod.regions',journal)
        self.assertIn('journal.myfirstmod.seals',journal)
        language=load(ASSETS/'lang/en_us.json')
        for key in ['journal.myfirstmod.regions','journal.myfirstmod.seals','item.myfirstmod.tide_lantern.tooltip']:
            self.assertIn(key,language)
    def test_wisp_is_passive_and_bound_to_young_stone(self):
        entity=(SRC/'mob/VeilWispEntity.java').read_text()
        self.assertIn('extends AnimalEntity',entity)
        self.assertIn('setNoGravity(true)',entity)
        self.assertIn('isBreedingItem(ItemStack stack){return false;}',entity)
        self.assertIn('createChild(ServerWorld world,PassiveEntity other){return null;}',entity)
        spawn=(SRC/'boss/ModEntities.java').read_text()
        self.assertIn('VEIL_WISP',spawn)
        self.assertIn('VEILSTONE)||world.getBlockState(pos.down()).isOf(dev.qynl.myfirstmod.block.ModBlocks.PRISMSTONE',spawn)
        self.assertTrue((ASSETS/'textures/entity/veil_wisp.png').exists())
        self.assertTrue((DATA/'loot_table/entities/veil_wisp.json').exists())
        model=(ROOT/'src/client/java/dev/qynl/myfirstmod/client/model/VeilWispModel.java').read_text()
        self.assertIn('TexturedModelData.of(d,32,32)',model)
    def test_journal_region_pages_are_written(self):
        language=load(ASSETS/'lang/en_us.json')
        self.assertIn('EIGHT REGIONS',language['journal.myfirstmod.regions'])
        self.assertIn('ROYAL SEALS',language['journal.myfirstmod.seals'])
if __name__=='__main__':unittest.main()

"""Reproducible 1.21.1 worldgen resources; no vanilla dimension preset dependencies.
2.0 alpha.2: eight routed biomes with per-region terrain shaping (terraced stacks,
jagged spires, lifted highlands, flattened vents) instead of recoloured flats.
"""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/myfirstmod'
def put(path, data):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, indent=2) + '\n')
def noise(name, xz, y):
    return {'type':'minecraft:noise','noise':'myfirstmod:'+name,'xz_scale':xz,'y_scale':y}
def add(a,b): return {'type':'minecraft:add','argument1':a,'argument2':b}
def mul(a,b): return {'type':'minecraft:mul','argument1':a,'argument2':b}
def block(name): return {'type':'minecraft:block','result_state':{'Name':name if ':' in name else 'minecraft:'+name}}
def mask(lo, hi):
    return {'type':'minecraft:range_choice','input':noise('climate',1,0),'min_inclusive':lo,'max_exclusive':hi,'when_in_range':1,'when_out_of_range':0}
for name, octave, amps in [('strata',-6,[1,1,.5]),('fractures',-4,[1,.5,.25]),('climate',-8,[1,1]),('basins',-8,[1,.5]),('ravines',-6,[1,.4])]:
    put('worldgen/noise/'+name+'.json', {'firstOctave':octave,'amplitudes':amps})
# Region masks key every landform term to one climate band, so each biome owns a silhouette.
M_HIGH, M_STACKS, M_SPIRES, M_VENTS = mask(-.55,-.25), mask(.05,.35), mask(.6,.8), mask(.92,1.01)
# Drowned Stacks: quantise the basin noise into four flooded terraces.
terrace = {'type':'minecraft:range_choice','input':noise('basins',1,0),'min_inclusive':-1000,'max_exclusive':-.4,'when_in_range':0,
           'when_out_of_range':{'type':'minecraft:range_choice','input':noise('basins',1,0),'min_inclusive':-.4,'max_exclusive':.1,'when_in_range':.5,
           'when_out_of_range':{'type':'minecraft:range_choice','input':noise('basins',1,0),'min_inclusive':.1,'max_exclusive':.5,'when_in_range':1,'when_out_of_range':1.5}}}
stacks = mul(M_STACKS, mul(.8, add(terrace, -.75)))
# Shard Spires: vanilla's steep ridge function, gated to the spire band.
spires = mul(M_SPIRES, mul(1.2, {'type':'minecraft:steep'}))
# Veil Highlands: a lifted plateau with its own broad swell.
high = mul(M_HIGH, add(.5, mul(.35, noise('basins',.6,0))))
basins_coef = add(.8, add(mul(-.5, M_STACKS), mul(-.55, M_VENTS)))
strata_coef = mul(-1.1, add(1, mul(-.6, M_VENTS)))
ridges = add(.55, mul(strata_coef, {'type':'minecraft:abs','argument':noise('strata',1,.12)}))
terrain = add({'type':'minecraft:y_clamped_gradient','from_y':8,'to_y':168,'from_value':1.8,'to_value':-1.8},
              add(mul(basins_coef, noise('basins',1,0)), add(ridges, add(mul(.35, noise('climate',1,0)),
              add(mul(.23, noise('fractures',1,.5)), add(add(stacks, spires), high))))))
ravines = {'type':'minecraft:max','argument1':add(mul(14,{'type':'minecraft:abs','argument':noise('ravines',.8,0)}),-.55),
           'argument2':{'type':'minecraft:y_clamped_gradient','from_y':42,'to_y':66,'from_value':1,'to_value':-1}}
terrain = {'type':'minecraft:min','argument1':terrain,'argument2':ravines}
router = {k:0 for k in ['barrier','fluid_level_floodedness','fluid_level_spread','lava','vegetation','continents','erosion','depth','ridges','vein_toggle','vein_ridged','vein_gap']}
caves = {'type':'minecraft:range_choice','input':{'type':'minecraft:y_clamped_gradient','from_y':-64,'to_y':320,'from_value':-64,'to_value':320},'min_inclusive':-40,'max_exclusive':48,'when_in_range':{'type':'minecraft:min','argument1':terrain,'argument2':add(mul(3,{'type':'minecraft:abs','argument':noise('fractures',.7,1.8)}),-.18)},'when_out_of_range':terrain}
router.update(temperature=noise('climate',1,0),initial_density_without_jaggedness=terrain,final_density={"type":"minecraft:squeeze","argument":{"type":"minecraft:interpolated","argument":caves}})
rules = [{'type':'minecraft:condition','if_true':{'type':'minecraft:vertical_gradient','random_name':'minecraft:bedrock_floor','true_at_and_below':{'above_bottom':0},'false_at_and_above':{'above_bottom':5}},'then_run':block('bedrock')}]
for biome, top in [('hushed_grove','myfirstmod:hushed_moss'),('prism_wastes','myfirstmod:prismstone'),('cinder_steps','myfirstmod:cinderstone'),('luminous_fen','myfirstmod:lumen_moss'),('drowned_stacks','myfirstmod:brinesilt'),('veil_highlands','myfirstmod:veilstone'),('shard_spires','myfirstmod:prismstone'),('ember_vents','myfirstmod:vent_basalt')]:
    rules.append({'type':'minecraft:condition','if_true':{'type':'minecraft:biome','biome_is':['myfirstmod:'+biome]},'then_run':{'type':'minecraft:condition','if_true':{'type':'minecraft:stone_depth','offset':0,'add_surface_depth':False,'secondary_depth_range':0,'surface_type':'floor'},'then_run':block(top)}})
rules.append({'type':'minecraft:condition','if_true':{'type':'minecraft:stone_depth','offset':3,'add_surface_depth':False,'secondary_depth_range':0,'surface_type':'floor'},'then_run':block('myfirstmod:polished_nullstone')})
put('worldgen/noise_settings/null_realm.json', {'sea_level':0,'disable_mob_generation':False,'aquifers_enabled':False,'ore_vertices_enabled' if False else 'ore_veins_enabled':False,'legacy_random_source':False,'default_block':{'Name':'myfirstmod:nullstone'},'default_fluid':{'Name':'minecraft:air'},'noise':{'min_y':-64,'height':384,'size_horizontal':1,'size_vertical':2},'noise_router':router,'surface_rule':{'type':'minecraft:sequence','sequence':rules},'spawn_target':[]})
biomes = []
REGIONS = [
 ('hushed_grove',[-1,-.55],0x102F38,0x132535,'sculk_soul','soul_sand_valley.loop',[]),
 ('veil_highlands',[-.55,-.25],0x274049,0x2C4A52,'cloud','warped_forest.loop',[('creature','myfirstmod:veil_wisp',12,1,2)]),
 ('luminous_fen',[-.25,.05],0x183F42,0x193E47,'spore_blossom_air','warped_forest.loop',[]),
 ('drowned_stacks',[.05,.35],0x14424A,0x173B44,'dripping_dripstone','underwater.loop',[('monster','minecraft:drowned',20,1,2)]),
 ('prism_wastes',[.35,.6],0x302646,0x271839,'end_rod','soul_sand_valley.loop',[]),
 ('shard_spires',[.6,.8],0x2A2A4E,0x232045,'glow','soul_sand_valley.loop',[('creature','myfirstmod:veil_wisp',12,1,2)]),
 ('cinder_steps',[.8,.92],0x352B38,0x281A30,'ash','basalt_deltas.loop',[]),
 ('ember_vents',[.92,1.01],0x3A2A26,0x2E1F1C,'white_ash','basalt_deltas.loop',[])]
for name,temp,fog,sky,particle,loop,extra in REGIONS:
    biomes.append({'biome':'myfirstmod:'+name,'parameters':{'temperature':temp,'humidity':[-1,1],'continentalness':[-1,1],'erosion':[-1,1],'depth':[-1,1],'weirdness':[-1,1],'offset':0}})
    spawners={'monster':[{'type':'myfirstmod:rift_sentinel','weight':25,'minCount':1,'maxCount':2},{'type':'myfirstmod:shardstalker','weight':35,'minCount':1,'maxCount':2}]}
    for category,actor,weight,lo,hi in extra:
        spawners.setdefault(category,[]).append({'type':actor,'weight':weight,'minCount':lo,'maxCount':hi})
    structures=['myfirstmod:realm_ruins','myfirstmod:waystone_shrine','myfirstmod:rift_observatory','myfirstmod:mourning_cathedral','myfirstmod:forgotten_memorial']
    vegetation=['myfirstmod:realm_flora','myfirstmod:realm_scenery']
    if name in ('veil_highlands','drowned_stacks','shard_spires','ember_vents'):
        vegetation.append('myfirstmod:region_signatures')
    if name=='drowned_stacks':
        structures.append('myfirstmod:drowned_archive')
    put('worldgen/biome/'+name+'.json',{'has_precipitation':False,'temperature':.4,'downfall':0,'effects':{'fog_color':fog,'sky_color':sky,'water_color':0x31667F,'water_fog_color':0x122333,'particle':{'options':{'type':'minecraft:'+particle},'probability':.004},'ambient_sound':'minecraft:ambient.'+loop,'mood_sound':{'sound':'minecraft:ambient.cave','tick_delay':8000,'block_search_extent':8,'offset':2}},'spawners':spawners,'spawn_costs':{},'carvers':{},'features':[[],[],[],[],structures,[],['myfirstmod:realm_resources'],[],[],vegetation,[]]})
put('dimension/null_realm.json',{'type':'myfirstmod:null_realm','generator':{'type':'minecraft:noise','settings':'myfirstmod:null_realm','biome_source':{'type':'minecraft:multi_noise','biomes':biomes}}})
p=ROOT/'dimension_type/null_realm.json'
d=json.loads(p.read_text());d.update(ambient_light=.16,effects='minecraft:overworld',fixed_time=18000,has_ceiling=False,infiniburn='#minecraft:infiniburn_overworld',monster_spawn_light_level={'type':'minecraft:uniform','min_inclusive':0,'max_inclusive':7})
p.write_text(json.dumps(d,indent=2)+'\n')
for name,chance in [('realm_ruins',12),('realm_flora',2),('realm_resources',1),('waystone_shrine',32),('rift_observatory',48),('mourning_cathedral',40),('forgotten_memorial',18),('realm_scenery',1)]:
    put('worldgen/configured_feature/'+name+'.json',{'type':'myfirstmod:'+name,'config':{}})
    put('worldgen/placed_feature/'+name+'.json',{'feature':'myfirstmod:'+name,'placement':[{'type':'minecraft:rarity_filter','chance':chance},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})
put('worldgen/configured_feature/realm_vault.json',{'type':'myfirstmod:realm_vault','config':{}})
# Region signatures run often: each chunk of a young biome should feel authored, not empty.
put('worldgen/configured_feature/region_signatures.json',{'type':'myfirstmod:region_signatures','config':{}})
put('worldgen/placed_feature/region_signatures.json',{'feature':'myfirstmod:region_signatures','placement':[{'type':'minecraft:rarity_filter','chance':2},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})
put('worldgen/configured_feature/drowned_archive.json',{'type':'myfirstmod:drowned_archive','config':{}})
put('worldgen/placed_feature/drowned_archive.json',{'feature':'myfirstmod:drowned_archive','placement':[{'type':'minecraft:rarity_filter','chance':34},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})

p=ROOT.parents[1]/'assets/myfirstmod/lang/en_us.json';lang=json.loads(p.read_text());lang.update({
 'journal.myfirstmod.gate':'ANCIENT THRESHOLD\n\nFind the great reinforced frame in an Overworld Ancient City.\n\nKeep its 22x8 outline intact and 20x6 opening clear. Use one Echo Shard on any frame block. Both orientations work.\n\nThe sanctuary gate returns you home.',
 'message.myfirstmod.city_gate_location':'The gateway must stand within an Ancient City in the Overworld.',
 'message.myfirstmod.city_gate_incomplete':'The great frame is incomplete or obstructed. Preserve its 22 by 8 outline and clear the 20 by 6 opening.',
 'message.myfirstmod.city_gate_open':'The ancient gateway is already awake.',
 'message.myfirstmod.city_gate_awakened':'The Ancient City answers. Cross the veil to enter the Null Realm.',
 'region.myfirstmod.veil_highlands':'Veil Highlands','biome.myfirstmod.veil_highlands':'Veil Highlands',
 'region.myfirstmod.drowned_stacks':'Drowned Stacks','biome.myfirstmod.drowned_stacks':'Drowned Stacks',
 'region.myfirstmod.shard_spires':'Shard Spires','biome.myfirstmod.shard_spires':'Shard Spires',
 'region.myfirstmod.ember_vents':'Ember Vents','biome.myfirstmod.ember_vents':'Ember Vents',
 'journal.myfirstmod.regions':'EIGHT REGIONS\n\nGrove, Highlands, Fen, Stacks, Wastes, Spires, Steps, Vents.\n\nHighlands float; Stacks drown; Spires cut; Vents smoulder. Each keeps its own stone, fog and silence.\n\nNew regions generate only in new chunks.',
 'journal.myfirstmod.seals':'ROYAL SEALS\n\nRootbound Seal: the monastery keeper.\nDrowned Seal: the flooded archive.\n\nTwo more seals wait in the kingdom. The ledger remembers each region you have walked.'
});p.write_text(json.dumps(lang,indent=2)+'\n')

"""Reproducible 1.21.1 worldgen resources; no vanilla dimension preset dependencies."""
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
for name, octave, amps in [('strata',-6,[1,1,.5]),('fractures',-4,[1,.5,.25]),('climate',-8,[1,1])]:
    put('worldgen/noise/'+name+'.json', {'firstOctave':octave,'amplitudes':amps})
terrain = add({'type':'minecraft:y_clamped_gradient','from_y':24,'to_y':128,'from_value':1.5,'to_value':-1.5}, add(mul(.8,noise('strata',1,.3)),mul(.22,noise('fractures',1,1))))
router = {k:0 for k in ['barrier','fluid_level_floodedness','fluid_level_spread','lava','vegetation','continents','erosion','depth','ridges','vein_toggle','vein_ridged','vein_gap']}
caves = {'type':'minecraft:range_choice','input':{'type':'minecraft:y_clamped_gradient','from_y':-64,'to_y':320,'from_value':-64,'to_value':320},'min_inclusive':-40,'max_exclusive':48,'when_in_range':{'type':'minecraft:min','argument1':terrain,'argument2':add(mul(3,{'type':'minecraft:abs','argument':noise('fractures',.7,1.8)}),-.18)},'when_out_of_range':terrain}
router.update(temperature=noise('climate',1,0),initial_density_without_jaggedness=terrain,final_density={"type":"minecraft:squeeze","argument":{"type":"minecraft:interpolated","argument":caves}})
rules = [{'type':'minecraft:condition','if_true':{'type':'minecraft:vertical_gradient','random_name':'minecraft:bedrock_floor','true_at_and_below':{'above_bottom':0},'false_at_and_above':{'above_bottom':5}},'then_run':block('bedrock')}]
for biome, top in [('hushed_grove','myfirstmod:hushed_moss'),('prism_wastes','myfirstmod:prismstone'),('cinder_steps','myfirstmod:cinderstone'),('luminous_fen','myfirstmod:lumen_moss')]:
    rules.append({'type':'minecraft:condition','if_true':{'type':'minecraft:biome','biome_is':['myfirstmod:'+biome]},'then_run':{'type':'minecraft:condition','if_true':{'type':'minecraft:stone_depth','offset':0,'add_surface_depth':False,'secondary_depth_range':0,'surface_type':'floor'},'then_run':block(top)}})
put('worldgen/noise_settings/null_realm.json', {'sea_level':0,'disable_mob_generation':False,'aquifers_enabled':False,'ore_veins_enabled':False,'legacy_random_source':False,'default_block':{'Name':'myfirstmod:nullstone'},'default_fluid':{'Name':'minecraft:air'},'noise':{'min_y':-64,'height':384,'size_horizontal':1,'size_vertical':2},'noise_router':router,'surface_rule':{'type':'minecraft:sequence','sequence':rules},'spawn_target':[]})
biomes = []
for name,temp,fog,sky,particle in [('hushed_grove',[-1,-.3],0x102F38,0x132535,'sculk_soul'),('luminous_fen',[-.3,.15],0x183F42,0x193E47,'spore_blossom_air'),('prism_wastes',[.15,.55],0x302646,0x271839,'end_rod'),('cinder_steps',[.55,1],0x352B38,0x281A30,'ash')]:
    biomes.append({'biome':'myfirstmod:'+name,'parameters':{'temperature':temp,'humidity':[-1,1],'continentalness':[-1,1],'erosion':[-1,1],'depth':[-1,1],'weirdness':[-1,1],'offset':0}})
    put('worldgen/biome/'+name+'.json',{'has_precipitation':False,'temperature':.4,'downfall':0,'effects':{'fog_color':fog,'sky_color':sky,'water_color':0x31667F,'water_fog_color':0x122333,'particle':{'options':{'type':'minecraft:'+particle},'probability':.004},'mood_sound':{'sound':'minecraft:ambient.cave','tick_delay':8000,'block_search_extent':8,'offset':2}},'spawners':{'monster':[{'type':'myfirstmod:rift_sentinel','weight':25,'minCount':1,'maxCount':2},{'type':'myfirstmod:shardstalker','weight':35,'minCount':1,'maxCount':2}]},'spawn_costs':{},'carvers':{},'features':[[],[],[],[],['myfirstmod:realm_ruins','myfirstmod:waystone_shrine'],[],['myfirstmod:realm_resources'],[],[],['myfirstmod:realm_flora'],[]]})
put('dimension/null_realm.json',{'type':'myfirstmod:null_realm','generator':{'type':'minecraft:noise','settings':'myfirstmod:null_realm','biome_source':{'type':'minecraft:multi_noise','biomes':biomes}}})
p=ROOT/'dimension_type/null_realm.json'
d=json.loads(p.read_text());d.update(ambient_light=.16,effects='minecraft:overworld',fixed_time=18000,has_ceiling=False,infiniburn='#minecraft:infiniburn_overworld',monster_spawn_light_level={'type':'minecraft:uniform','min_inclusive':0,'max_inclusive':7})
p.write_text(json.dumps(d,indent=2)+'\n')
for name,chance in [('realm_ruins',12),('realm_flora',2),('realm_resources',1),('waystone_shrine',32)]:
    put('worldgen/configured_feature/'+name+'.json',{'type':'myfirstmod:'+name,'config':{}})
    put('worldgen/placed_feature/'+name+'.json',{'feature':'myfirstmod:'+name,'placement':[{'type':'minecraft:rarity_filter','chance':chance},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})

put('worldgen/configured_feature/realm_vault.json',{'type':'myfirstmod:realm_vault','config':{}})

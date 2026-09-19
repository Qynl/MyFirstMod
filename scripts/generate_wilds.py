"""Wilds & Relics: original art, block loot, crafting, mining tags, and expedition caches."""
import json,random,runpy
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
art=runpy.run_path(str(BASE/'scripts/generate_art.py'))
png,sprite=art['png'],art['sprite']
ASSETS=BASE/'src/main/resources/assets/myfirstmod'
DATA=BASE/'src/main/resources/data/myfirstmod'
def put(root,path,data):
    p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def asset(path,data):put(ASSETS,path,data)
def data(path,value):put(DATA,path,value)
BLOCKS=['nullstone','polished_nullstone','nullstone_bricks','resonite_ore','prism_ore','cinder_ore','resonite_block','prism_lamp','prismstone','cinderstone','hushed_moss','lumen_moss','hushwood','hush_planks','hush_leaves']
for name in BLOCKS+['waystone']:
    rng=random.Random('wilds/'+name);pixels=[]
    base=(37,54,66)
    if name in ['prismstone','prism_ore']:base=(99,86,120)
    if name in ['cinderstone','cinder_ore']:base=(70,43,52)
    if 'moss' in name or name=='hush_leaves':base=(37,85,79)
    if name in ['hushwood','hush_planks']:base=(41,59,75)
    if name=='resonite_block':base=(58,113,124)
    for y in range(16):
        for x in range(16):
            shade=rng.randrange(-12,13);color=tuple(max(0,min(255,c+shade)) for c in base)
            if name=='nullstone_bricks' and (y%8==0 or (x+(8 if y//8 else 0))%16==0):color=(16,27,39)
            if name=='polished_nullstone' and (x in [0,15] or y in [0,15]):color=(66,83,103)
            if name=='hushwood' and (x+y//4)%4==0:color=(20,37,54)
            if name=='hush_planks' and (y%4==0 or (x+(y//4)*5)%16==0):color=(21,33,47)
            if name.endswith('_ore') and ((x*3+y*5)%23<3 or (x-6)**2+(y-9)**2<7):
                color={'resonite_ore':(100,231,213),'prism_ore':(208,157,250),'cinder_ore':(250,152,73)}[name]
            if name in ['prism_lamp','waystone']:
                color=(41,50,76)
                if abs(x-7.5)+abs(y-7.5)<7:color=(160,238,229) if name=='prism_lamp' else (198,158,247)
                if abs(x-7.5)+abs(y-7.5)<3:color=(236,255,233)
            if name=='lumen_moss' and (x*7+y)%17==0:color=(137,225,187)
            if name=='hush_leaves' and (x+y)%7==0:color=(100,173,157)
            pixels.append(color+(255,))
    png('block/'+name+'.png',16,16,pixels)
    asset('blockstates/'+name+'.json',{'variants':{'':{'model':'myfirstmod:block/'+name}}})
    asset('models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
    if name!='waystone':asset('models/item/'+name+'.json',{'parent':'myfirstmod:block/'+name})

DARK=(28,32,55);TEAL=(109,233,215);LIGHT=(210,255,240);PURPLE=(174,131,220);GOLD=(244,177,103)
shard=[(15,3),(25,12),(21,25),(11,29),(6,17)]
sprite('raw_resonite',[(shard,DARK), ([(14,6),(22,13),(18,24),(10,25),(9,17)],TEAL), ([(14,6),(16,15),(10,25),(9,17)],LIGHT)])
sprite('resonite_ingot',[([(3,12),(11,6),(27,8),(29,21),(22,27),(4,25)],DARK), ([(5,13),(12,9),(25,10),(26,19),(21,23),(6,22)],TEAL), ([(5,13),(12,9),(25,10),(19,15)],LIGHT)])
sprite('prism_dust',[([(3,24),(8,20),(12,11),(16,16),(21,9),(27,24),(19,29),(8,28)],DARK), ([(5,24),(12,14),(16,20),(21,12),(25,24),(17,27)],PURPLE), ([(11,5),(14,2),(17,7),(14,11)],LIGHT)])
sprite('cinder_pearl',[([(7,4),(23,4),(29,14),(25,26),(14,30),(4,22),(2,11)],DARK), ([(8,7),(22,7),(26,14),(23,24),(14,27),(7,20),(5,12)],GOLD), ([(9,10),(14,8),(17,10),(12,15),(8,15)],(255,241,175))])
sprite('dusk_fiber',[([(4,5),(9,3),(12,16),(25,25),(27,29),(19,28),(8,20)],DARK), ([(6,5),(8,5),(10,18),(24,27),(21,27),(8,19)],TEAL), ([(21,2),(24,3),(18,16),(4,29),(2,26),(16,14)],PURPLE)])
sprite('hushberry',[([(12,4),(16,1),(21,2),(18,8),(25,12),(26,22),(18,29),(8,26),(4,17),(7,9)],DARK), ([(8,12),(18,9),(23,14),(23,22),(17,26),(10,24),(7,18)],PURPLE), ([(10,13),(14,11),(16,13),(12,17)],LIGHT), ([(14,7),(16,3),(20,3),(17,8)],TEAL)])
sprite('expedition_stew',[([(2,14),(30,14),(26,27),(7,27)],DARK), ([(5,14),(27,14),(24,24),(9,24)],(105,67,94)), ([(4,11),(10,7),(24,8),(29,12),(26,17),(7,17)],TEAL), ([(10,10),(14,9),(16,13),(11,14)],PURPLE), ([(21,10),(24,11),(23,14),(20,13)],GOLD)])
sprite('ember_tonic',[([(11,2),(22,2),(21,10),(28,15),(26,29),(6,29),(4,15),(12,10)],DARK), ([(12,5),(20,5),(19,12),(25,16),(23,26),(9,26),(7,16),(14,12)],LIGHT), ([(8,18),(24,18),(23,26),(9,26)],GOLD), ([(12,2),(22,2),(22,6),(12,6)],(116,69,68))])
handle=[([(6,28),(3,26),(21,6),(25,10)],DARK), ([(6,25),(20,9),(22,11),(8,27)],(91,75,117))]
sprite('resonite_pickaxe',handle+[( [(8,3),(20,3),(29,11),(29,18),(25,18),(21,11),(8,8)],DARK), ([(10,4),(20,5),(27,11),(27,16),(25,14),(20,9),(10,7)],TEAL)])
sprite('resonite_axe',handle+[( [(13,1),(24,3),(30,10),(26,18),(20,17),(13,10)],DARK), ([(15,3),(23,5),(27,10),(24,15),(21,14),(16,9)],TEAL)])
sprite('resonite_shovel',handle+[( [(20,2),(29,4),(29,13),(22,18),(15,11)],DARK), ([(21,4),(27,6),(27,12),(22,15),(18,10)],TEAL)])
sprite('prism_staff',[([(4,29),(1,26),(21,4),(28,9),(26,15),(21,13)],DARK), ([(4,26),(21,7),(24,10),(6,28)],(113,85,148)), ([(22,1),(30,5),(28,12),(23,14),(18,8)],TEAL), ([(23,3),(28,6),(26,10),(22,11),(20,8)],LIGHT)])
sprite('cinder_maul',handle+[( [(9,3),(22,1),(31,10),(28,20),(16,16),(7,8)],DARK), ([(10,5),(21,4),(28,11),(25,17),(17,13)],GOLD), ([(18,5),(21,5),(27,11),(24,11)],LIGHT)])
sprite('survey_lens',[([(2,27),(12,17),(11,7),(18,1),(27,4),(31,13),(25,22),(17,23),(7,31)],DARK), ([(14,8),(19,4),(26,6),(28,13),(24,19),(17,20),(14,16)],TEAL), ([(17,8),(21,6),(24,8),(18,15),(16,13)],LIGHT), ([(5,27),(14,19),(17,22),(7,29)],GOLD)])
sprite('veil_charm',[([(15,2),(25,10),(27,23),(16,31),(4,23),(7,10)],DARK), ([(15,5),(22,11),(24,22),(16,27),(7,22),(10,11)],PURPLE), ([(16,10),(21,18),(16,24),(12,18)],TEAL)])
sprite('repair_kit',[([(3,9),(13,9),(13,4),(23,4),(24,9),(29,9),(29,28),(3,28)],DARK), ([(6,12),(26,12),(26,25),(6,25)],(92,73,120)), ([(15,13),(19,13),(19,17),(23,17),(23,21),(19,21),(19,24),(15,24),(15,21),(11,21),(11,17),(15,17)],TEAL)])
sprite('wayfarer_thread',[([(8,2),(22,2),(26,8),(21,16),(27,23),(22,30),(8,30),(4,23),(10,15),(4,8)],DARK), ([(9,5),(21,5),(23,8),(17,15),(23,23),(21,27),(9,27),(7,23),(14,15),(7,8)],PURPLE), ([(12,8),(19,8),(17,12),(14,12)],LIGHT), ([(12,22),(19,22),(20,25),(11,25)],TEAL)])
ITEMS=['raw_resonite','resonite_ingot','prism_dust','cinder_pearl','dusk_fiber','hushberry','expedition_stew','ember_tonic','resonite_pickaxe','resonite_axe','resonite_shovel','prism_staff','cinder_maul','survey_lens','veil_charm','repair_kit','wayfarer_thread']
for name in ITEMS:
    asset('models/item/'+name+'.json',{'parent':'minecraft:item/handheld' if name in ['resonite_pickaxe','resonite_axe','resonite_shovel','prism_staff','cinder_maul'] else 'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})

# Minecraft 1.21.1 uses singular loot_table / recipe / tags/block paths.
def drop(name):return {'type':'minecraft:item','name':name}
silk={'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}
for name in BLOCKS:
    entries=[drop('myfirstmod:'+name)]
    if name.endswith('_ore'):
        output={'resonite_ore':'raw_resonite','prism_ore':'prism_dust','cinder_ore':'cinder_pearl'}[name]
        normal=drop('myfirstmod:'+output)
        normal['functions']=[{'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'},{'function':'minecraft:explosion_decay'}]
        silk_drop=drop('myfirstmod:'+name);silk_drop['conditions']=[silk]
        entries=[{'type':'minecraft:alternatives','children':[silk_drop,normal]}]
    table={'type':'minecraft:block','pools':[{'rolls':1,'entries':entries,'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
    if name=='hush_leaves':
        table['pools']=[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[dict(drop('myfirstmod:hush_leaves'),conditions=[silk]),dict(drop('myfirstmod:dusk_fiber'),functions=[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':0,'max':2}}])]}]}, {'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':.35},{'condition':'minecraft:survives_explosion'}],'entries':[drop('myfirstmod:hushberry')]}]
    data('loot_table/blocks/'+name+'.json',table)
vanilla=BASE/'src/main/resources/data/minecraft'
for path,values in {
    'tags/block/mineable/pickaxe.json':[n for n in BLOCKS if n not in ['hushed_moss','lumen_moss','hushwood','hush_planks','hush_leaves']],
    'tags/block/mineable/axe.json':['hushwood','hush_planks'],
    'tags/block/mineable/hoe.json':['hushed_moss','lumen_moss','hush_leaves'],
    'tags/block/needs_iron_tool.json':['resonite_ore','prism_ore','cinder_ore','resonite_block'],
    'tags/item/planks.json':['hush_planks'],
    'tags/block/planks.json':['hush_planks'],
}.items():put(vanilla,path,{'replace':False,'values':['myfirstmod:'+v for v in values]})

def ingredient(name):return {'item':name if ':' in name else 'myfirstmod:'+name}
def shaped(name,pattern,key,result=None,count=1):
    data('recipe/'+name+'.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':{k:ingredient(v) for k,v in key.items()},'result':{'id':'myfirstmod:'+(result or name),'count':count}})
def shapeless(name,ingredients,result=None,count=1):
    data('recipe/'+name+'.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[ingredient(n) for n in ingredients],'result':{'id':'myfirstmod:'+(result or name),'count':count}})
for mode,time in [('smelting',200),('blasting',100)]:
    data('recipe/resonite_'+mode+'.json',{'type':'minecraft:'+mode,'category':'misc','ingredient':ingredient('raw_resonite'),'result':{'id':'myfirstmod:resonite_ingot'},'experience':.7,'cookingtime':time})
shapeless('hush_planks',['hushwood'],count=4)
shaped('polished_nullstone',['SS','SS'],{'S':'nullstone'},count=4)
shaped('nullstone_bricks',['SS','SS'],{'S':'polished_nullstone'},count=4)
shaped('resonite_block',['III','III','III'],{'I':'resonite_ingot'})
shapeless('resonite_unpack',['resonite_block'],'resonite_ingot',9)
shaped('prism_lamp',[' P ','PGP',' P '],{'P':'prism_dust','G':'minecraft:glass'},count=4)
shaped('resonite_pickaxe',['III',' S ',' S '],{'I':'resonite_ingot','S':'minecraft:stick'})
shaped('resonite_axe',['II','IS',' S'],{'I':'resonite_ingot','S':'minecraft:stick'})
shaped('resonite_shovel',['I','S','S'],{'I':'resonite_ingot','S':'minecraft:stick'})
shaped('prism_staff',[' PP',' IP','S  '],{'P':'prism_dust','I':'resonite_ingot','S':'minecraft:stick'})
shaped('cinder_maul',['ICI','ICI',' S '],{'I':'resonite_ingot','C':'cinder_pearl','S':'minecraft:stick'})
shaped('survey_lens',[' IP',' SI','S  '],{'I':'resonite_ingot','P':'prism_dust','S':'minecraft:stick'})
shaped('veil_charm',[' F ','FPF',' F '],{'F':'dusk_fiber','P':'minecraft:ender_pearl'})
shapeless('repair_kit',['resonite_ingot','resonite_ingot','dusk_fiber','resonant_shard'])
shaped('wayfarer_thread',[' F ','FPF',' R '],{'F':'dusk_fiber','P':'minecraft:ender_pearl','R':'resonant_shard'})
shapeless('expedition_stew',['hushberry','hushberry','minecraft:brown_mushroom','minecraft:bowl'])
shapeless('ember_tonic',['cinder_pearl','hushberry','minecraft:glass_bottle'])
# The wilds can sustain expeditions without an overworld mushroom supply.
shapeless('grove_stew',['hushberry','hushberry','hushberry','minecraft:bowl'],'expedition_stew')
for path in (DATA/'recipe').glob('*.json'):
    name=path.stem
    if name not in ['resonite_'+m for m in ['smelting','blasting']] and name not in ITEMS+['hush_planks','polished_nullstone','nullstone_bricks','resonite_block','resonite_unpack','prism_lamp','grove_stew']:continue
    data('advancement/recipes/'+name+'.json',{'criteria':{'entered':{'trigger':'minecraft:changed_dimension','conditions':{'to':'myfirstmod:null_realm'}},'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:raw_resonite','myfirstmod:resonite_ingot','myfirstmod:prism_dust','myfirstmod:hushwood','myfirstmod:hushberry']} ]}}},'requirements':[['entered','has_material']],'rewards':{'recipes':['myfirstmod:'+name]}})

def count_drop(name,low,high,weight=1):
    entry=drop('myfirstmod:'+name);entry['weight']=weight
    entry['functions']=[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':low,'max':high}}]
    return entry
cache={'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':[count_drop('resonite_ingot',2,5,4),count_drop('prism_dust',3,7,5),count_drop('dusk_fiber',2,5,5),count_drop('hushberry',4,8,5),count_drop('repair_kit',1,1,2),count_drop('survey_lens',1,1,1),count_drop('wayfarer_thread',1,1,1)]}]}
data('loot_table/chests/waystone_cache.json',cache)
# Add a small expedition-supply roll without duplicating it on regeneration.
for name in ['realm_cache','vault_cache']:
    p=DATA/('loot_table/chests/'+name+'.json');table=json.loads(p.read_text())
    marker='myfirstmod:resonite_ingot'
    if marker not in json.dumps(table):
        table['pools'].append({'rolls':1,'entries':[count_drop('resonite_ingot',2,4),count_drop('prism_dust',3,6),count_drop('repair_kit',1,1)]})
    data('loot_table/chests/'+name+'.json',table)

# A distinct, non-dyed armor material with original worn and inventory textures.
for layer in [1,2]:
    rng=random.Random('resonite-armor-'+str(layer));pixels=[]
    for y in range(32):
        for x in range(64):
            n=rng.randint(-7,7)
            color=(40+n,83+n,99+n)
            if y%8 in [0,7] or x%8 in [0,7]:color=(23,35,59)
            if (x+y*2)%19<2:color=(105,230,213)
            if y%16==4 and x%8 in [3,4]:color=(210,253,233)
            pixels.append(color+(255,))
    png(f'models/armor/resonite_layer_{layer}.png',64,32,pixels)
sprite('resonite_helmet',[([(4,5),(26,5),(30,12),(28,25),(21,25),(21,18),(11,18),(11,25),(4,25),(2,12)],DARK), ([(7,8),(24,8),(27,13),(26,22),(24,22),(24,15),(8,15),(8,22),(6,22),(5,13)],TEAL), ([(10,9),(21,9),(21,12),(10,12)],LIGHT)])
sprite('resonite_chestplate',[([(9,3),(13,8),(19,8),(23,3),(31,11),(26,16),(25,29),(7,29),(6,16),(1,11)],DARK), ([(9,7),(13,11),(19,11),(23,7),(27,11),(23,14),(22,26),(10,26),(9,14),(5,11)],TEAL), ([(14,13),(18,13),(20,18),(16,23),(12,18)],LIGHT)])
sprite('resonite_leggings',[([(7,3),(25,3),(26,29),(17,29),(16,16),(15,29),(6,29)],DARK), ([(10,6),(22,6),(22,12),(10,12)],TEAL), ([(9,14),(14,14),(12,26),(9,26)],TEAL), ([(18,14),(23,14),(23,26),(20,26)],TEAL)])
sprite('resonite_boots',[([(5,7),(14,7),(14,29),(2,29),(2,20),(5,18)],DARK), ([(19,7),(28,7),(28,18),(31,20),(31,29),(19,29)],DARK), ([(7,10),(12,10),(12,26),(5,26),(5,22),(8,21)],TEAL), ([(21,10),(26,10),(25,21),(28,22),(28,26),(21,26)],TEAL)])
armor={'resonite_helmet':['III','I I'],'resonite_chestplate':['I I','III','III'],'resonite_leggings':['III','I I','I I'],'resonite_boots':['I I','I I']}
for name,pattern in armor.items():
    asset('models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
    shaped(name,pattern,{'I':'resonite_ingot'})
    data('advancement/recipes/'+name+'.json',{'criteria':{'ingot':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:resonite_ingot']}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})
for tag in ['stone_crafting_materials','stone_tool_materials']:
    put(vanilla,'tags/item/'+tag+'.json',{'replace':False,'values':['myfirstmod:nullstone']})
data('recipe/prism_glass.json',{'type':'minecraft:smelting','category':'blocks','ingredient':ingredient('prismstone'),'result':{'id':'minecraft:glass'},'experience':.1,'cookingtime':200})
data('advancement/recipes/prism_glass.json',{'criteria':{'prismstone':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:prismstone']}]}}},'rewards':{'recipes':['myfirstmod:prism_glass']}})

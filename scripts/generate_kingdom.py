"""2.0 alpha: native jigsaw monastery, original materials, items and biome enrichment.
No libraries required. NBT is authored geometry, not a downloaded vanilla structure.
"""
from pathlib import Path
import json, gzip, struct, runpy, random
B=Path(__file__).resolve().parents[1];A=B/'src/main/resources/assets/myfirstmod';D=B/'src/main/resources/data/myfirstmod'
a=runpy.run_path(str(B/'scripts/generate_art.py'));png,sprite=a['png'],a['sprite']
def put(root,path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def s(text):
 raw=text.encode();return struct.pack('>H',len(raw))+raw
def tag(value):
 if isinstance(value,str):return 8,s(value)
 if isinstance(value,int):return 3,struct.pack('>i',value)
 if isinstance(value,dict):return 10,b''.join(bytes([t])+s(k)+v for k,val in value.items() for t,v in [tag(val)])+b'\0'
 if isinstance(value,list):
  entries=[tag(v) for v in value];kind=entries[0][0] if entries else 10
  assert all(t==kind for t,v in entries)
  return 9,bytes([kind])+struct.pack('>i',len(entries))+b''.join(v for t,v in entries)
 raise TypeError(value)
blocks={};palette=[];index={}
def block(x,y,z,name,properties=None,nbt=None):
 state={'Name':name if ':' in name else 'minecraft:'+name}
 if properties:state['Properties']=properties
 key=json.dumps(state,sort_keys=True)
 if key not in index:index[key]=len(palette);palette.append(state)
 entry={'pos':[x,y,z],'state':index[key]}
 if nbt:entry['nbt']=nbt
 blocks[x,y,z]=entry
# A real multi-chunk structure. Vanilla's structure pipeline handles chunk-clipped placement.
for x in range(47):
 for z in range(47):
  for y in range(18):
   name='air'
   if y==0:name='myfirstmod:polished_nullstone' if (x+z)%5 else 'mossy_stone_bricks'
   if (x in [0,46] or z in [0,46]) and 1<=y<=5:name='mossy_stone_bricks'
   if (x in [0,46] or z in [0,46]) and y==6 and (x+z)%4<2:name='stone_brick_wall'
   if z==46 and 21<=x<=25 and y<=4 and y>0:name='air'
   block(x,y,z,name)
def room(x1,z1,x2,z2,height):
 for x in range(x1,x2+1):
  for z in range(z1,z2+1):
   for y in range(1,height+1):
    edge=x in [x1,x2] or z in [z1,z2]
    if edge:block(x,y,z,'mossy_stone_bricks' if (x+y+z)%4 else 'cracked_stone_bricks')
   roof=height+1+(min(x-x1,x2-x)//3)
   block(x,roof,z,'myfirstmod:hush_planks' if z%4 else 'myfirstmod:hushwood')
# Northern chapterhouse, west ground-floor chapel, east two-storey library.
room(14,2,32,20,9);room(2,22,12,40,6);room(34,22,44,40,10)
for x1,z1 in [(23,20),(7,40),(39,40)]:
 for x in range(x1-1,x1+2):
  for y in range(1,5):block(x,y,z1,'air')
for x1 in [2,12,14,32,34,44]:
 for z in ([7,15] if x1 in [14,32] else [25,31,37]):
  for y in range(3,6):block(x1,y,z,'green_stained_glass')
# Library gallery and a two-wide, six-step stair. Leave headroom all the way up.
for x in range(35,44):
 for z in range(23,35):block(x,6,z,'myfirstmod:hush_planks')
for step in range(6):
 for x in [39,40]:
  for y in range(1+step,5+step):block(x,y,39-step,'air')
  block(x,1+step,39-step,'dark_oak_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
for x in [35,43]:
 for z in range(24,33,3):
  for y in [1,2,7,8]:block(x,y,z,'bookshelf')
# Cloister colonnade, a root garden and a victory shortcut through its south screen.
for x in [16,30]:
 for z in range(24,42,4):
  for y in range(1,6):block(x,y,z,'myfirstmod:hushwood')
  block(x,6,z,'myfirstmod:prism_lamp')
for x in range(18,29):
 for z in range(27,36):
  if x in [18,28] or z in [27,35]:
   for y in [1,2,3]:block(x,y,z,'myfirstmod:hush_leaves')
  if x!=23:block(x,0,z,'myfirstmod:hushed_moss')
for x in range(22,25):
 for y in [1,2,3]:block(x,y,27,'iron_bars');block(x,y,35,'air')
# Giant roots flank (never cover) the boss's movement lanes.
for x,z in [(16,4),(30,4),(16,18),(30,18)]:
 for y in range(1,12):block(x,y,z,'myfirstmod:hushwood')
 for dx in range(-2,3):block(x+dx,11,z,'myfirstmod:hush_leaves')
 block(x,5,z+(-1 if z==18 else 1),'myfirstmod:prism_lamp')
for x,z in [(7,23),(7,38),(39,23),(39,38),(4,44),(42,44)]:block(x,4,z,'myfirstmod:prism_lamp')
for x,z,y,name in [(23,10,1,'root_heart'),(7,26,1,'cloister_bell'),(39,26,7,'cloister_bell')]:
 block(x,y,z,'myfirstmod:'+name,{'facing':'north','bells':'0'})
block(23,1,43,'myfirstmod:waystone')
for x,y,z in [(5,1,35),(42,7,24)]:
 block(x,y,z,'spawner',nbt={'id':'minecraft:mob_spawner','SpawnData':{'entity':{'id':'myfirstmod:rift_sentinel' if y==1 else 'myfirstmod:shardstalker'}},'Delay':160,'MinSpawnDelay':400,'MaxSpawnDelay':800,'SpawnCount':1,'SpawnRange':2,'MaxNearbyEntities':2,'RequiredPlayerRange':12})
for x,y,z in [(9,1,24),(36,7,30)]:
 block(x,y,z,'chest',{'facing':'south','type':'single','waterlogged':'false'},{'id':'minecraft:chest','LootTable':'myfirstmod:chests/monastery_cache'})
root={'DataVersion':3955,'size':[47,18,47],'palette':palette,'blocks':list(blocks.values()),'entities':[]}
_,raw=tag(root);path=D/'structure/rootbound_monastery.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\0\0'+raw,mtime=0))
put(D,'tags/worldgen/biome/has_rootbound_monastery.json',{'values':['myfirstmod:hushed_grove']})
put(D,'worldgen/template_pool/rootbound_monastery.json',{'name':'myfirstmod:rootbound_monastery','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'myfirstmod:rootbound_monastery','processors':'minecraft:empty','projection':'rigid'}}]})
put(D,'worldgen/structure/rootbound_monastery.json',{'type':'myfirstmod:rootbound_monastery','biomes':'#myfirstmod:has_rootbound_monastery','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin'})
put(D,'worldgen/structure_set/rootbound_monastery.json',{'structures':[{'structure':'myfirstmod:rootbound_monastery','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':32,'separation':16,'salt':2074301}})
put(D,'loot_table/entities/rootbound_prior.json',{'type':'minecraft:entity','pools':[]})
put(D,'loot_table/chests/monastery_cache.json',{'type':'minecraft:chest','pools':[{'rolls':4,'entries':[{'type':'minecraft:item','name':'myfirstmod:'+n,'weight':w,'functions':[{'function':'minecraft:set_count','count':c}]} for n,w,c in [('hushberry',4,5),('resonite_ingot',3,2),('prism_dust',3,5),('repair_kit',1,1),('memory_shard',1,1)]]}]})
for name,bright in [('root_heart',(93,158,102)),('cloister_bell',(210,180,100)),('root_reliquary',(126,225,161))]:
 pixels=[]
 for y in range(16):
  for x in range(16):
   c=(35+(x+y)%9,47+(x*y)%11,36)
   if x in [1,14] or y in [1,14]:c=(84,102,63)
   if abs(x-7.5)+abs(y-7.5)<5 and (x+y)%3:c=bright
   pixels.append(c+(255,))
 png('block/'+name+'.png',16,16,pixels)
 put(A,'models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
 put(A,'blockstates/'+name+'.json',{'variants':{f'bells={b},facing={f}':{'model':'myfirstmod:block/'+name} for b in range(4) for f in ['north','east','south','west']}})
sprite('rootbound_seal',[([(16,1),(29,12),(26,25),(16,31),(6,25),(3,12)],(63,66,40)), ([(16,5),(25,14),(22,24),(16,27),(10,24),(7,14)],(152,184,100)), ([(14,9),(18,9),(18,16),(24,12),(25,16),(18,21),(18,25),(14,25),(14,20),(8,16),(10,12),(14,16)],(47,80,56))])
sprite('briarbrand',[([(24,1),(28,3),(23,19),(17,24),(11,19)],(38,62,47)), ([(24,3),(25,6),(20,19),(16,21),(14,18)],(169,231,155)), ([(11,17),(18,23),(14,26),(8,20)],(159,116,64)), ([(11,22),(14,25),(7,31),(4,28)],(83,59,45)), ([(20,9),(29,6),(25,12),(17,15)],(107,162,106))])
for name in ['rootbound_seal','briarbrand']:
 put(A,'models/item/'+name+'.json',{'parent':'minecraft:item/handheld' if name=='briarbrand' else 'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
put(D,'recipe/briarbrand.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':[' R ',' R ','FSD'],'key':{'R':{'item':'myfirstmod:resonite_ingot'},'F':{'item':'myfirstmod:dusk_fiber'},'S':{'item':'myfirstmod:rootbound_seal'},'D':{'item':'myfirstmod:hushwood'}},'result':{'id':'myfirstmod:briarbrand','count':1}})
put(D,'advancement/recipes/briarbrand.json',{'criteria':{'seal':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:rootbound_seal']}]}}},'rewards':{'recipes':['myfirstmod:briarbrand']}})
rng=random.Random('rootbound-prior');pixels=[]
for y in range(128):
 for x in range(128):
  grain=rng.randint(-6,6);c=(45+grain,66+grain,43+grain)
  if x%8 in [0,7]:c=(79,89,51)
  if (x*3+y)%23==0:c=(128,159,82)
  if y<24 and x<40:c=(115+grain,135+grain,89+grain)
  if 10<=y<=12 and 10<=x<=19:c=(213,255,155)
  pixels.append(c+(255,))
png('entity/rootbound_prior.png',128,128,pixels)
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({
 'entity.myfirstmod.rootbound_prior':'The Rootbound Prior', 'block.myfirstmod.root_heart':'Rootbound Heart', 'block.myfirstmod.cloister_bell':'Cloister Bell','block.myfirstmod.root_reliquary':'The Prior’s Reliquary',
 'item.myfirstmod.rootbound_seal':'Rootbound Seal','item.myfirstmod.rootbound_seal.tooltip':'One shared reward per monastery. Craft the Briarbrand with Resonite, Dusk Fiber and Hushwood.',
 'item.myfirstmod.briarbrand':'Briarbrand','item.myfirstmod.briarbrand.tooltip':'Hold one second, then release a narrow four-block thorn sweep: 9 damage, Slowness III for three seconds, and one heart healed when it connects. Ten-second cooldown, three durability, attunable at the forge.',
 'monastery.myfirstmod.riddle':'Two bells stir the roots: one in the west chapel, one above the east library. Ring both, then return to this heart.',
 'monastery.myfirstmod.bell':'The cloister answers. Return to the Rootbound Heart after both bells have rung.',
 'monastery.myfirstmod.spent':'The roots have fallen silent.', 'monastery.myfirstmod.active':'The Prior is already awake. A missing guardian resets after six seconds nearby.',
 'monastery.myfirstmod.blocked':'Clear the space above the heart before waking the Prior.', 'monastery.myfirstmod.peaceful':'The Prior cannot awaken in Peaceful difficulty.',
 'monastery.myfirstmod.begin':'The Rootbound Prior awakens. Leave the marked ground; strike during recovery.', 'monastery.myfirstmod.reward':'You claim the monastery’s shared Rootbound Seal. Its roots can be reforged into the Briarbrand.',
 'prior.myfirstmod.attack.1':'Root Lash — leave the marked lane', 'prior.myfirstmod.attack.2':'Thorn Crown — gaps or close center; watch the second pulse', 'prior.myfirstmod.attack.3':'Seedfall — leave the marked circles', 'prior.myfirstmod.recovery':'Rootbound Prior — recovery',
 'journal.myfirstmod.monastery':'ROOTBOUND MONASTERY\n\nSeek the great cloister in new Hushed Grove chunks. Bind its entrance waystone. Ring the west chapel bell and the upper east-library bell.\n\nWake the Prior in the north hall. Defeat it, then claim the shared seal from its heart.',
 'message.myfirstmod.gate_charging':'The frame remembers. Hold the Echo Shard nearby while the threshold awakens.',
 'message.myfirstmod.gate_cancelled':'The awakening fades. No Echo Shard was consumed.'
});put(A,'lang/en_us.json',lang)

# --- 2.0 alpha.2: region assets, archive rewards and the veil wisp -----------------
for name in ['brinesilt','veilstone','vent_basalt','spire_crystal','oxidized_trim']:
    put(A,'models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
    put(A,'blockstates/'+name+'.json',{'variants':{'':{'model':'myfirstmod:block/'+name}}})
    put(A,'models/item/'+name+'.json',{'parent':'myfirstmod:block/'+name})
    put(D,'loot_table/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'myfirstmod:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
tag=(D.parents[0]/'minecraft/tags/block/mineable/pickaxe.json')
values=sorted(set(json.loads(tag.read_text())['values'])|{'myfirstmod:brinesilt','myfirstmod:veilstone','myfirstmod:vent_basalt','myfirstmod:spire_crystal','myfirstmod:oxidized_trim'})
tag.write_text(json.dumps({'replace':False,'values':values},indent=2)+'\n')
for drained,tint in [(False,(214,196,120)),(True,(120,110,74))]:
    pixels=[]
    for y in range(16):
        for x in range(16):
            c=(58+(x+y)%7,74+(x*y)%9,80)
            if x in [2,13] or y in [1,14]:c=(38,52,58)
            if abs(x-7.5)+abs(y-7.5)<5:c=tint
            pixels.append(c+(255,))
    suffix='' if not drained else '_drained'
    png('block/tide_bell'+suffix+'.png',16,16,pixels)
    put(A,'models/block/tide_bell'+suffix+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/tide_bell'+suffix}})
put(A,'blockstates/tide_bell.json',{'variants':{'drained=false':{'model':'myfirstmod:block/tide_bell'},'drained=true':{'model':'myfirstmod:block/tide_bell_drained'}}})
sprite('drowned_seal',[([(16,1),(29,10),(27,24),(16,31),(5,24),(3,10)],(30,64,72)), ([(16,5),(25,12),(23,23),(16,27),(9,23),(7,12)],(96,190,196)), ([(10,14),(22,14),(20,18),(12,18)],(28,52,60)), ([(10,19),(22,19),(20,23),(12,23)],(28,52,60))])
sprite('tide_lantern',[([(12,2),(20,2),(21,5),(11,5)],(70,84,90)), ([(10,5),(22,5),(23,24),(9,24)],(44,58,64)), ([(13,8),(19,8),(20,21),(12,21)],(140,235,225)), ([(15,11),(17,11),(18,17),(14,17)],(235,255,250)), ([(11,24),(21,24),(21,28),(11,28)],(70,84,90)), ([(15,0),(17,0),(17,2),(15,2)],(120,140,150))])
for name in ['drowned_seal','tide_lantern']:
    put(A,'models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
put(D,'loot_table/chests/archive_cache.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'myfirstmod:drowned_seal'}]},{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':[{'type':'minecraft:item','name':'myfirstmod:resonite_ingot','weight':3,'functions':[{'function':'minecraft:set_count','count':1}]},{'type':'minecraft:item','name':'myfirstmod:prism_dust','weight':3,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}}]},{'type':'minecraft:item','name':'minecraft:book','weight':2},{'type':'minecraft:item','name':'myfirstmod:memory_shard','weight':1}]}]})
put(D,'loot_table/entities/veil_wisp.json',{'type':'minecraft:entity','pools':[]})
put(D,'recipe/tide_lantern.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[{'item':'myfirstmod:prism_lamp'},{'item':'myfirstmod:drowned_seal'},{'item':'myfirstmod:resonite_ingot'}],'result':{'id':'myfirstmod:tide_lantern','count':1}})
put(D,'advancement/recipes/tide_lantern.json',{'criteria':{'seal':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:drowned_seal']}]}}},'rewards':{'recipes':['myfirstmod:tide_lantern']}})
rng=random.Random('veil-wisp');pixels=[]
for y in range(32):
    for x in range(32):
        g=rng.randint(-8,8);c=(0,0,0,0)
        dx=abs(x-15.5);dy=abs(y-15.5)
        if dx+dy<5:c=(225+g//2,255,246)
        elif dx+dy<8:c=(140+g,226+g//2,214)
        elif (dx<14 and dy<5) or (dy<14 and dx<5):c=(110+g,190+g,200)
        pixels.append(c if len(c)==4 else c+(255,))
png('entity/veil_wisp.png',32,32,pixels)
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({
 'block.myfirstmod.brinesilt':'Brinesilt','block.myfirstmod.veilstone':'Veilstone','block.myfirstmod.vent_basalt':'Vent Basalt',
 'block.myfirstmod.spire_crystal':'Spire Crystal','block.myfirstmod.oxidized_trim':'Oxidized Trim','block.myfirstmod.tide_bell':'Tide Bell',
 'item.myfirstmod.drowned_seal':'Drowned Seal','item.myfirstmod.drowned_seal.tooltip':'Recovered from the drained archive vault. Craft the Tide Lantern with a Prism Lamp and a Resonite Ingot.',
 'item.myfirstmod.tide_lantern':'Tide Lantern','item.myfirstmod.tide_lantern.tooltip':'Use: forty seconds of water breathing and twenty seconds of night vision. Ninety-second cooldown. The drowned archive\'s answer to deep water.',
 'entity.myfirstmod.veil_wisp':'Veil Wisp',
 'archive.myfirstmod.drained':'The tide obeys. %s blocks of water drain from the hall.',
 'archive.myfirstmod.dry':'The bell rings over an already dry hall.',
 'archive.myfirstmod.once':'The tide obeys only once.',
 'archive.myfirstmod.unloaded':'The hall\'s waters lie beyond loaded chunks.'
});put(A,'lang/en_us.json',lang)
print('Generated Rootbound Monastery:',len(blocks),'blocks across a 47x47 footprint.')

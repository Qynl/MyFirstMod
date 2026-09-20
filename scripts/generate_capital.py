"""2.0 alpha.4: the Silent Capital — a unique fixed-position citadel template,
its crown gate assets, the crown seal chain end and the court reward.
NBT is authored geometry; no vanilla structure is copied."""
from pathlib import Path
import json, gzip, struct, runpy
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
W=63;H=10
def garden(x1,z1):
    for x in range(x1,x1+11):
        for z in range(z1,z1+11):
            block(x,0,z,'myfirstmod:hushed_moss')
            if x in (x1,x1+10) or z in (z1,z1+10):block(x,1,z,'myfirstmod:veilstone')
    for dx,dz in [(3,3),(7,7),(3,7),(7,3)]:block(x1+dx,1,z1+dz,'myfirstmod:hush_leaves')
    block(x1+1,2,z1+1,'myfirstmod:prism_lamp')
for x in range(W):
    for z in range(W):
        for y in range(H):
            name='air'
            if y==0:
                name='myfirstmod:prismstone' if z>=47 else 'myfirstmod:veilstone'
            if (x in (0,W-1) or z in (0,W-1)) and 1<=y<=5:name='myfirstmod:nullstone_bricks'
            if (x in (0,W-1) or z in (0,W-1)) and y==6 and (x+z)%4<2:name='myfirstmod:nullstone_bricks'
            if z<=2 and 29<=x<=33 and 1<=y<=5:name='air'
            block(x,y,z,name)
for x in range(16,47):
    for z in range(31,46):
        if x in (16,46) or z in (31,45):block(x,1,z,'myfirstmod:polished_nullstone')
for x in range(29,34):
    for z in range(36,41):
        block(x,1,z,'myfirstmod:polished_nullstone');block(x,2,z,'myfirstmod:polished_nullstone')
block(31,3,38,'myfirstmod:polished_nullstone')
block(31,4,38,'myfirstmod:nullstone_bricks');block(31,5,38,'myfirstmod:nullstone_bricks')
block(30,3,38,'myfirstmod:nullstone_bricks');block(32,3,38,'myfirstmod:nullstone_bricks')
block(31,1,1,'myfirstmod:crown_gate',{'phase':'0'})
block(31,1,3,'myfirstmod:waystone')
for z in range(6,29,6):
    for x in (27,35):
        block(x,1,z,'myfirstmod:polished_nullstone');block(x,2,z,'myfirstmod:polished_nullstone');block(x,3,z,'myfirstmod:prism_lamp')
for xr in range(18,45,4):
    for z in (48,60):
        for y in range(1,6):block(xr,y,z,'myfirstmod:nullstone_bricks')
for x in range(16,47):
    for z in range(47,62):block(x,6,z,'myfirstmod:polished_nullstone')
for z in range(31,46,4):
    for x in (6,10,52,56):block(x,1,z,'myfirstmod:polished_nullstone')
garden(3,3);garden(49,3)
block(31,1,55,'spawner',nbt={'id':'minecraft:mob_spawner','SpawnData':{'entity':{'id':'myfirstmod:rift_sentinel'}},'Delay':200,'MinSpawnDelay':400,'MaxSpawnDelay':900,'SpawnCount':1,'SpawnRange':3,'MaxNearbyEntities':2,'RequiredPlayerRange':12})
ordered=[blocks[k] for k in sorted(blocks)]
root={'size':[W,H,W],'palette':palette,'blocks':ordered,'entities':[]}
_,raw=tag(root);path=D/'structure/silent_capital.nbt';path.parent.mkdir(parents=True,exist_ok=True)
path.write_bytes(gzip.compress(b'\n\0\0'+raw,mtime=0))
put(D,'worldgen/template_pool/silent_capital.json',{'name':'myfirstmod:silent_capital','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'myfirstmod:silent_capital','processors':'minecraft:empty','projection':'rigid'}}]})
put(D,'worldgen/structure/silent_capital.json',{'type':'myfirstmod:silent_capital','biomes':'#myfirstmod:has_silent_capital','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin'})
put(D,'worldgen/structure_set/silent_capital.json',{'structures':[{'structure':'myfirstmod:silent_capital','weight':1}],'placement':{'type':'minecraft:random_spread','salt':47110023,'spacing':34,'separation':24}})
put(D,'tags/worldgen/biome/has_silent_capital.json',{'values':['myfirstmod:hushed_grove','myfirstmod:veil_highlands','myfirstmod:luminous_fen','myfirstmod:drowned_stacks','myfirstmod:prism_wastes','myfirstmod:shard_spires','myfirstmod:cinder_steps','myfirstmod:ember_vents']})
for phase,core in [(0,(46,40,52)),(1,(96,214,200)),(2,(240,190,110))]:
    pixels=[]
    for y in range(16):
        for x in range(16):
            c=(30,28,34)
            if x in (0,15) or y in (0,15):c=(58,52,64)
            if 3<=x<=12 and 3<=y<=12:c=core
            if 6<=x<=9 and 3<=y<=12 and phase==0:c=(20,18,24)
            if phase==2 and (x+y)%4==0 and 3<=x<=12 and 3<=y<=12:c=(255,232,170)
            pixels.append(c+(255,))
    png('block/crown_gate_%d.png'%phase,16,16,pixels)
    put(A,'models/block/crown_gate_%d.json'%phase,{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/crown_gate_%d'%phase}})
put(A,'blockstates/crown_gate.json',{'variants':{'phase=0':{'model':'myfirstmod:block/crown_gate_0'},'phase=1':{'model':'myfirstmod:block/crown_gate_1'},'phase=2':{'model':'myfirstmod:block/crown_gate_2'}}})
sprite('crown_seal',[([(16,1),(29,10),(27,24),(16,31),(5,24),(3,10)],(52,44,64)), ([(16,5),(25,12),(23,23),(16,27),(9,23),(7,12)],(240,190,110)), ([(12,9),(20,9),(22,14),(16,20),(10,14)],(120,96,150)), ([(15,12),(17,12),(17,17),(15,17)],(255,240,200))])
sprite('silent_crown',[([(6,14),(26,14),(27,20),(5,20)],(240,190,110)), ([(6,8),(9,8),(9,14),(6,14)],(240,190,110)), ([(14,5),(18,5),(18,14),(14,14)],(240,190,110)), ([(23,8),(26,8),(26,14),(23,14)],(240,190,110)), ([(15,9),(17,9),(17,13),(15,13)],(140,235,225)), ([(6,20),(26,20),(26,24),(5,24)],(190,150,80))])
for name in ['crown_seal','silent_crown']:
    put(A,'models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
put(D,'loot_table/chests/crown_cache.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'myfirstmod:crown_seal'}]},{'rolls':{'type':'minecraft:uniform','min':2,'max':4},'entries':[{'type':'minecraft:item','name':'myfirstmod:resonite_block','weight':2},{'type':'minecraft:item','name':'myfirstmod:memory_shard','weight':2},{'type':'minecraft:item','name':'minecraft:echo_shard','weight':1}]}]})
put(D,'recipe/silent_crown.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[{'item':'myfirstmod:crown_seal'},{'item':'myfirstmod:resonite_block'}],'result':{'id':'myfirstmod:silent_crown','count':1}})
put(D,'advancement/recipes/silent_crown.json',{'criteria':{'seal':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:crown_seal']}]}}},'rewards':{'recipes':['myfirstmod:silent_crown']}})
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({
 'block.myfirstmod.crown_gate':'Crown Gate',
 'item.myfirstmod.crown_seal':'Crown Seal','item.myfirstmod.crown_seal.tooltip':'The fourth seal, taken from the Silent Court. Craft the Crown of the Silent Court with a Resonite Block.',
 'item.myfirstmod.silent_crown':'Crown of the Silent Court','item.myfirstmod.silent_crown.tooltip':'Worn: the realm no longer breaks your fall, and its dark becomes sight. The kingdom knows its crowned.',
 'court.myfirstmod.unworthy':'The Crown Gate answers only to a bearer of three seals.',
 'court.myfirstmod.session':'The court is in session. Stand your ground.',
 'court.myfirstmod.ruled':'The court has ruled. Its treasury is open.',
 'court.myfirstmod.wave':'The court calls its %s. hold the plaza.',
 'court.myfirstmod.open':'The Crown Gate unfolds. The Silent Capital remembers its court.',
 'journal.myfirstmod.capital':'SILENT CAPITAL\n\nDue north of the sanctuary, past the last ridge: (0, -352).\n\nWalls, boulevard, gardens, colonnades and a throne under a pale roof. The Crown Gate sleeps at the south door.',
 'journal.myfirstmod.coronation':'CORONATION\n\nThree seals attuned, use the Crown Gate. The court convenes in three sessions.\n\nOutlive them and the throne treasury opens: the Crown Seal, last of the chain. Wear the crown and the realm softens.'
});put(A,'lang/en_us.json',lang)
print('Generated Silent Capital: %d blocks across a %dx%d footprint.'%(len(blocks),W,W))

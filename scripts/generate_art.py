"""Original deterministic pixel textures. Uses only Python's standard library."""
import math, struct, zlib, json, random
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/myfirstmod'
def png(path,w,h,pixels):
    def chunk(tag,data): return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
    raw=b''.join(b'\0'+bytes(sum((list(c) for c in pixels[y*w:(y+1)*w]),[])) for y in range(h))
    path=ROOT/'textures'/path;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))
def sprite(name,polygons):
    pixels=[(0,0,0,0)]*1024
    for vertices,color in polygons:
        for y in range(32):
            for x in range(32):
                inside=False;j=len(vertices)-1
                for i,(ax,ay) in enumerate(vertices):
                    bx,by=vertices[j]
                    if (ay>y+.5)!=(by>y+.5) and x+.5<(bx-ax)*(y+.5-ay)/(by-ay)+ax: inside=not inside
                    j=i
                if inside:pixels[y*32+x]=color+(255,)
    png('item/'+name+'.png',32,32,pixels)
for frame in range(32):
    p=[(0,0,0,0)]*1024
    for y in range(32):
        for x in range(32):
            dx=x-15.5;dy=y-15.5;r=math.hypot(dx,dy)
            if r<=14:
                color=(31,39,56) if r<11 else (86,75,117)
                if 12<r<13.5:color=(135,177,193)
                if r<10 and (x%5==0 or y%5==0):color=(43,54,70)
                if r>10 and (abs(dx)<1 or abs(dy)<1):color=(142,245,224)
                a=frame*math.tau/32
                u=dx*math.cos(a)+dy*math.sin(a);v=-dx*math.sin(a)+dy*math.cos(a)
                if -10<v<0 and abs(u)<(10+v)*.26+.5:color=(145,255,228)
                if 0<=v<8 and abs(u)<(8-v)*.25+.5:color=(186,111,236)
                if r<2:color=(237,244,232)
                p[y*32+x]=color+(255,)
    png(f'item/arena_compass_{frame:02}.png',32,32,p)
sprite('resonant_shard',[([(15,1),(25,10),(21,25),(10,31),(6,17)],(42,37,71)), ([(15,3),(22,11),(18,25),(11,28),(9,17)],(96,77,158)), ([(15,3),(16,15),(11,28),(9,17)],(112,230,220)), ([(16,15),(22,11),(18,25),(11,28)],(175,126,229)), ([(15,3),(17,9),(16,15),(12,12)],(211,255,238))])
sprite('null_relic',[([(16,1),(28,13),(27,24),(16,31),(5,24),(4,13)],(40,37,61)), ([(16,4),(25,14),(23,23),(16,27),(9,23),(7,14)],(117,83,169)), ([(16,7),(22,15),(19,22),(13,22),(10,15)],(45,112,121)), ([(16,9),(19,15),(16,22),(13,15)],(155,255,227))])
sprite('resonance_matrix',[([(16,1),(31,16),(16,31),(1,16)],(61,45,90)), ([(16,4),(28,16),(16,28),(4,16)],(163,123,204)), ([(16,7),(25,16),(16,25),(7,16)],(30,65,79)), ([(16,10),(22,16),(16,22),(10,16)],(162,255,227))])
for name,base,glow,w,h in [('rift_sentinel',(24,45,53),(91,244,214),64,64),('shardstalker',(43,30,61),(191,130,241),64,32),('nullsteel',(20,47,56),(92,210,214),16,16),('null_edge',(62,37,88),(173,113,229),16,16),('null_core',(37,91,104),(157,255,231),16,16),('ascended_core',(83,58,98),(255,218,133),16,16),('resonance_core',(33,43,61),(131,255,229),16,16)]:
    rng=random.Random(name);pixels=[]
    for y in range(h):
        for x in range(w):
            n=rng.randrange(-8,9);color=tuple(max(0,min(255,c+n)) for c in base)
            if (x*3+y)%17==0 or (x+y*2)%29==0:color=tuple(int(c*.7) for c in glow)
            if name=='resonance_core' and abs(x-7.5)+abs(y-7.5)<5:color=glow
            if name=='rift_sentinel' and 9<=y<=11 and 9<=x<=14:color=glow
            if name=='shardstalker' and 8<=y<=11 and 33<=x<=44:color=glow
            pixels.append(color+(255,))
    folder='entity' if name in ('rift_sentinel','shardstalker') else 'block'
    png(folder+'/'+name+'.png',w,h,pixels)
def put(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
for i in range(32):
    put(Path(f'models/item/arena_compass_{i:02}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'myfirstmod:item/arena_compass_{i:02}'}})
put(Path('models/item/arena_compass.json'),{'parent':'myfirstmod:item/arena_compass_00','overrides':[{'predicate':{'angle':i/32},'model':f'myfirstmod:item/arena_compass_{i:02}'} for i in range(32)]})
for name in ['null_relic','resonant_shard','resonance_matrix']:
    put(Path('models/item/'+name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
put(Path('blockstates/resonance_core.json'),{'variants':{'':{'model':'myfirstmod:block/resonance_core'}}})
put(Path('models/block/resonance_core.json'),{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/resonance_core'}})
p=ROOT/'models/item/nullblade.json';d=json.loads(p.read_text())
d['textures'].update(blade='myfirstmod:block/nullsteel',edge='myfirstmod:block/null_edge',core='myfirstmod:block/null_core')
put(Path('models/item/nullblade.json'),d)
d['textures']['core']='myfirstmod:block/ascended_core'
put(Path('models/item/ascended_nullblade.json'),d)

# Endgame artifacts: a ritual seal, fractured crown, and reflective aegis.
sprite('echo_sigil',[([(16,1),(30,16),(16,31),(2,16)],(48,39,76)), ([(16,4),(27,16),(16,28),(5,16)],(137,110,190)), ([(16,8),(23,16),(16,24),(9,16)],(31,47,68)), ([(15,6),(18,6),(18,26),(15,26)],(165,250,226)), ([(6,15),(26,15),(26,18),(6,18)],(165,250,226))])
sprite('warden_crest',[([(3,7),(10,13),(16,2),(22,13),(29,7),(25,26),(7,26)],(70,46,84)), ([(5,10),(11,17),(16,6),(21,17),(27,10),(23,23),(9,23)],(241,201,126)), ([(16,13),(20,20),(16,26),(12,20)],(115,252,220))])
sprite('rift_aegis',[([(4,4),(16,1),(28,4),(26,23),(16,31),(6,23)],(41,32,64)), ([(6,6),(16,4),(26,6),(24,22),(16,28),(8,22)],(149,119,202)), ([(9,8),(16,6),(23,8),(21,21),(16,25),(11,21)],(31,70,83)), ([(16,9),(20,16),(16,23),(12,16)],(164,255,229))])
for name in ['echo_sigil','warden_crest','rift_aegis']:
    put(Path('models/item/'+name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
put(Path('blockstates/echo_altar.json'),{'variants':{'':{'model':'myfirstmod:block/echo_altar'}}})
put(Path('models/block/echo_altar.json'),{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/ascended_core'}})

# Ancient City gateway: original animated teal/violet membrane, not a solid purple cube.
pixels=[]
for frame in range(16):
 for y in range(16):
  for x in range(16):
   wave=math.sin((x+y)*.6-frame*math.tau/16)+math.sin((x-y)*.4+frame*math.tau/16)
   pixels.append((int(34+15*wave),int(94+26*wave),int(122+30*wave),185))
png('block/void_portal.png',16,256,pixels)
put(Path('textures/block/void_portal.png.mcmeta'),{'animation':{'frametime':3,'interpolate':True}})
put(Path('blockstates/void_portal.json'),{'variants':{'axis=x':{'model':'myfirstmod:block/void_portal'},'axis=z':{'model':'myfirstmod:block/void_portal','y':90}}})
put(Path('models/block/void_portal.json'),{'textures':{'portal':'myfirstmod:block/void_portal','particle':'myfirstmod:block/void_portal'},'elements':[{'from':[0,0,6],'to':[16,16,10],'shade':False,'faces':{f:{'texture':'#portal','uv':[0,0,16,16]} for f in ['north','south','east','west','up','down']}}]})

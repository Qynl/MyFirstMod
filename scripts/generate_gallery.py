"""Dependency-free documentation plates: real pixel assets and source-derived cuboid silhouettes.
Models use simplified material colors, not Minecraft's UV textures/lighting. No screenshots.
Fail closed on unsupported numeric expressions so model changes cannot silently invent geometry.
"""
from pathlib import Path
import re, math, base64, html
B=Path(__file__).resolve().parents[1];OUT=B/'docs/images';OUT.mkdir(exist_ok=True)
MODELS=B/'src/client/java/dev/qynl/myfirstmod/client/model'
ASSETS=B/'src/main/resources/assets/myfirstmod/textures'
def svg(w,h,title,desc):
 return [f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img" aria-labelledby="title desc"><title id="title">{html.escape(title)}</title><desc id="desc">{html.escape(desc)}</desc><rect width="100%" height="100%" fill="#0b121d"/>',f'<text x="32" y="44" fill="#e2eee7" font-family="sans-serif" font-size="27" font-weight="bold">{html.escape(title)}</text>']
def text(x,y,value,size=14,color='#9aaebc'):
 return f'<text x="{x}" y="{y}" fill="{color}" font-family="sans-serif" font-size="{size}">{html.escape(value)}</text>'
def image(path,x,y,w,h):
 return f'<image x="{x}" y="{y}" width="{w}" height="{h}" style="image-rendering:pixelated" xlink:href="data:image/png;base64,{base64.b64encode(path.read_bytes()).decode()}"/>'
items=['briarbrand','rootbound_seal','requiem_glaive','prism_staff','cinder_maul','rift_aegis','ashen_flask','pilgrim_step','pilgrim_atlas','survey_lens','echo_sigil','null_relic','regent_crest','warden_crest','memory_shard','mourning_ember','astral_core','resonite_ingot','resonite_pickaxe','resonite_chestplate','iron_vow','ember_vow','mist_vow','vigor_rune','gale_rune','focus_rune']
s=svg(1080,920,'THE PILGRIM’S ARMORY','Twenty-six actual item textures from the mod, enlarged without smoothing.');s.append(text(32,70,'Original in-game pixel assets • Weapons, exploration, progression and rewards'))
for i,name in enumerate(items):
 x=20+i%6*176;y=94+i//6*163
 s += [f'<rect x="{x}" y="{y}" width="164" height="151" rx="8" fill="#121f2c" stroke="#243648"/>',image(ASSETS/'item'/f'{name}.png',x+34,y+8,96,96),text(x+10,y+130,name.replace('_',' ').title(),13,'#d3e4df')]
s.append('</svg>');(OUT/'item-gallery.svg').write_text('\n'.join(s)+'\n')

def extract(name):
 source=(MODELS/(name+'Model.java')).read_text().split('public static TexturedModelData',1)[1].split('return TexturedModelData',1)[0]
 # Expand the three small Java construction loops into their exact numeric geometry.
 if name=='GraveRegent':
  pat=r'for\(int i=-1;i<=1;i\+\+\)(h.addChild.*?ModelTransform.pivot\(i\*5,-13,-6\)\);)'
  source=re.sub(pat,lambda m:'\n'.join(m[1].replace('"crown"+i',f'"crown{i}"').replace('i*5',str(i*5)) for i in range(-1,2)),source,flags=re.S)
 if name=='RiftHerald':
  pat=r'for\(int i=0;i<4;i\+\+\) (root.addChild.*?ModelTransform.NONE\);)'
  source=re.sub(pat,lambda m:'\n'.join(m[1].replace('"shard"+i',f'"shard{i}"').replace('ModelTransform.NONE',f'ModelTransform.of({math.cos(i*math.pi/2)*10},{8+math.sin(i)*2},{math.sin(i*math.pi/2)*10},0,{-i*math.pi/2},{.2 if i%2==0 else -.2})') for i in range(4)),source,flags=re.S)
 if name=='Shardstalker':
  pat=r'for\(int i=0;i<6;i\+\+\) \{\s*int side=i<3\?-1:1;\s*(r.addChild.*?)\s*\}'
  def expand(m):
   out=[]
   for i in range(6):
    side=-1 if i<3 else 1
    out.append(m[1].replace('"leg"+i',f'"leg{i}"').replace('side<0?-10:0',str(-10 if side<0 else 0)).replace('side<0?-11:9',str(-11 if side<0 else 9)).replace('side*4',str(side*4)).replace('(i%3-1)*5',str((i%3-1)*5)))
   return '\n'.join(out)
  source=re.sub(pat,expand,source,flags=re.S)
 assert 'for(' not in source.replace('for (','for('),name
 nodes={'':{'parent':None,'transform':[0]*6,'boxes':[]}};aliases={'r':'','root':''}
 event=re.compile(r'(?:(?:var|ModelPartData)\s+(\w+)\s*=\s*)?(\w+)\.(addChild|getChild)\("([^"]+)"')
 for m in event.finditer(source):
  alias,parent,method,child=m.groups();parent=aliases[parent];key=parent+'/'+child
  if method=='getChild':aliases[alias]=key;continue
  start=source.index('(',m.start());depth=1;j=start+1
  while depth:
   depth+=(source[j]=='(')-(source[j]==')');j+=1
  args=source[start:j]
  transform=re.search(r'ModelTransform\.(pivot|of)\(([^)]+)\)',args)
  nums=lambda s:[float(v.strip().removesuffix('f')) for v in s.split(',')]
  pose=nums(transform[2]) if transform else [0]*6
  pose=pose+[0]*(6-len(pose))
  boxes=[]
  for cube in re.finditer(r'\.cuboid\(([^)]+)(?:\)\))?',args):
   values=cube[1];dilation=0
   if 'new Dilation(' in values:
    values,di=values.split(', new Dilation(');dilation=float(di.removesuffix('f'))
   v=nums(values);assert len(v)==6,(name,v)
   boxes.append([v[k]-dilation for k in range(3)]+[v[k+3]+2*dilation for k in range(3)])
  assert boxes,(name,child)
  nodes[key]={'parent':parent,'transform':pose,'boxes':boxes}
  if alias:aliases[alias]=key
 assert len(nodes)>4,name
 return nodes

def world(nodes,key,p):
 if key=='':return p
 n=nodes[key];px,py,pz,rx,ry,rz=n['transform'];x,y,z=p
 # ModelPart uses translation then Z/Y/X rotation (vectors therefore rotate X first).
 y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
 x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
 x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
 return world(nodes,n['parent'],(x+px,y+py,z+pz))
def project(p):
 # Quantization prevents coplanar painter-order drift across Python/libm versions.
 x,y,z=(round(v,6) for v in p)
 return tuple(round(v,6) for v in (.881*x+.472*z,.142*x+.951*y-.264*z,.45*x-.30*y-.84*z))
def model(name,x,y,w,h):
 nodes=extract(name);polys=[]
 for key,n in nodes.items():
  glow=any(word in key for word in ['core','eye','crown','horn','shard'])
  base=(119,94,141) if name=='NullWarden' else (142,104,75) if name=='GraveRegent' else (63,133,142) if name=='RiftSentinel' else (86,110,72) if name=='RootboundPrior' else (121,90,157)
  if name=='VeilWisp':base=(120,205,196)
  if glow:base=(145,216,202) if name not in ('GraveRegent','RootboundPrior') else (220,180,100) if name=='GraveRegent' else (213,255,155)
  for bx,by,bz,dx,dy,dz in n['boxes']:
   v=[project(world(nodes,key,(bx+(i&1)*dx,by+((i>>1)&1)*dy,bz+((i>>2)&1)*dz))) for i in range(8)]
   for indices,shade in [([0,2,3,1],.9),([4,5,7,6],.52),([0,4,6,2],.6),([1,3,7,5],.78),([0,1,5,4],1.1),([2,6,7,3],.4)]:
    pts=[v[i] for i in indices];color='#'+''.join(f'{min(255,int(c*shade)):02x}' for c in base)
    polys.append((sum(p[2] for p in pts)/4,pts,color))
 points=[p for _,ps,_ in polys for p in ps];minx=min(p[0] for p in points);maxx=max(p[0] for p in points);miny=min(p[1] for p in points);maxy=max(p[1] for p in points)
 scale=min(w/(maxx-minx),h/(maxy-miny));ox=x+(w-(maxx-minx)*scale)/2;oy=y+(h-(maxy-miny)*scale)/2
 out=[f'<g data-model="{name}" data-parts="{len(nodes)-1}">']
 for _,pts,color in sorted(polys,key=lambda p:round(p[0],6)):
  coords=' '.join(f'{ox+(p[0]-minx)*scale:.2f},{oy+(p[1]-miny)*scale:.2f}' for p in pts)
  out.append(f'<polygon points="{coords}" fill="{color}" stroke="#142330" stroke-width="0.55" stroke-linejoin="round"/>')
 out.append('</g>');return out
s=svg(1200,1120,'THOSE WHO WAIT IN THE DARK','Source-derived geometric previews of the Null Warden, Grave Regent, Rootbound Prior, Veil Wisp, Rift Herald, Rift Sentinel and Shardstalker. Simplified colors; not gameplay screenshots.')
s.append(text(32,73,'Actual Java-model cuboids and bone pivots • Simplified materials • Not gameplay screenshots'))
for name,title,role,x,y,w,h in [('NullWarden','NULL WARDEN','Realm boss / fractured colossus',25,105,370,545),('GraveRegent','GRAVE REGENT','Hollow Keep boss / crowned monarch',414,105,330,290),('RiftHerald','RIFT HERALD','Unstable rift guardian',784,104,185,225),('RiftSentinel','RIFT SENTINEL','Committed greatblade attacks',995,104,180,225),('Shardstalker','SHARDSTALKER','Six-legged ambush predator',786,416,380,220),('RootboundPrior','ROOTBOUND PRIOR','Monastery keeper / bark-robed abbot',25,720,560,300),('VeilWisp','VEIL WISP','Highland mote / harmless drifter',25,416,370,220)]:
 s.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h+53}" rx="10" fill="#101e2a" stroke="#243c49"/>')
 s+=model(name,x+18,y+14,w-36,h-25)
 s += [text(x+16,y+h+18,title,17,'#deebe5'),text(x+16,y+h+38,role,11)]
s += [f'<rect x="414" y="416" width="330" height="220" rx="10" fill="#0d1721" stroke="#22384a"/>',
 text(430,448,'EIGHT REGIONS',20,'#cfe7dc'),text(430,476,'Grove • Highlands • Fen • Stacks',13),text(430,498,'Wastes • Spires • Steps • Vents',13),
 text(430,530,'Highlands float; Stacks drown;',13),text(430,552,'Spires cut; Vents smoulder.',13),text(430,584,'Each keeps its own stone, fog,',13),text(430,606,'silence and signature landmark.',13),
 text(620,760,'ROOTBOUND MONASTERY',24,'#cfe7dc'),text(620,792,'New in 2.0: a 47 by 47 cloister generated by vanilla jigsaw rules',13),text(620,817,'in fresh Hushed Grove chunks, never over the sanctuary approach.',13),
 text(620,848,'Ring the west chapel bell and the upper east library bell,',13),text(620,873,'then wake the Prior at the Rootbound Heart in the north hall.',13),
 text(620,904,'Three telegraphed attacks: Root Lash lane, Thorn Crown ring,',13),text(620,929,'Seedfall circles. Strike only during the visible recovery pause.',13),
 text(620,960,'Reward: one shared Rootbound Seal per monastery, reforged',13),text(620,985,'into the Briarbrand: a short thorn sweep that slows and heals.',13)]
s.append(text(32,1090,'Generated from src/client/.../model/*.java. Live animation, UV textures, glow and renderer scaling are not reproduced.',12))
s.append('</svg>');(OUT/'creature-gallery.svg').write_text('\n'.join(s)+'\n')
# Biome materials: eight actual surface textures, one card per region. Not landscape screenshots.
s=svg(1080,600,'EIGHT REGIONS. ONE LONG NIGHT.','Actual surface textures for all eight Null Realm biomes. Material palette, not a landscape screenshot.')
for i,(title,tex,caption) in enumerate([('HUSHED GROVE','hushed_moss','Root arches, moss, fallen wood'),('VEIL HIGHLANDS','veilstone','Floating islands and hanging roots'),('LUMINOUS FEN','lumen_moss','Glowing pools and spores'),('DROWNED STACKS','brinesilt','Flooded terraces and colonnades'),('PRISM WASTES','prismstone','Crystal flats and amethyst'),('SHARD SPIRES','spire_crystal','Jagged ridges and crystal columns'),('CINDER STEPS','cinderstone','Ash terraces and basalt'),('EMBER VENTS','vent_basalt','Smoking flats and lava pockets')]):
    x=22+(i%4)*266;y=96+(i//4)*246
    s += [text(x,y,title,18,'#dce9e1'),image(ASSETS/'block'/f'{tex}.png',x,y+14,120,120),text(x,y+158,caption,12),
          f'<rect x="{x-6}" y="{y-22}" width="252" height="206" rx="10" fill="none" stroke="#22384a"/>']
s.append(text(22,572,'Original block textures • Material palette, not a gameplay screenshot',13));s.append('</svg>');(OUT/'biome-palettes.svg').write_text('\n'.join(s)+'\n')
# Dimension route diagram, to exact portal block proportions.
s=svg(1100,420,'AWAKEN THE ANCIENT CITY','22 by 8 reinforced-deepslate frame with a 20 by 6 opening. Three-second awakening, Echo Shard to the Null Realm sanctuary and its matching return gateway, then Hollow Keep after a Warden clear.')
s.append(text(30,76,'Find the central monument • Right-click any frame block with one Echo Shard • hold it still for three seconds'))
for y in range(8):
 for x in range(22):
  border=x in [0,21] or y in [0,7]
  s.append(f'<rect x="{30+x*20}" y="{106+y*20}" width="19" height="19" fill="{"#566669" if border else "#266376"}"/>')
s += [text(30,295,'22 × 8 outside / 20 × 6 opening',18,'#cfe7dc'),text(30,320,'Survival: generated Ancient City, Overworld',13),text(30,345,'Creative: matching replicas also work',13),text(30,370,'Step away or release the shard and the awakening fades',13),text(30,395,'No shard is consumed by a cancelled awakening',13)]
s += [text(508,180,'→',40,'#8cd7c9'),text(570,150,'NULL REALM',24,'#b0e4d0'),text(570,176,'Matching 22 × 8 return gateway at the sanctuary',13),text(570,201,'Sanctuary → wilds → Null Warden',14),text(570,232,'Two bells → Rootbound Prior → Briarbrand',13,'#a9d8b0'),text(570,266,'↓ First Warden victory',18,'#d3bd88'),text(570,311,'HOLLOW KEEP',24,'#d4b18c'),text(570,339,'Three wards → Grave Regent → Requiem Glaive',14)]
s.append('</svg>');(OUT/'ancient-city-gateway.svg').write_text('\n'.join(s)+'\n')
print('Generated four documentation plates from local assets/model sources.')

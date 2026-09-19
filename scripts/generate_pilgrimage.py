"""Deterministic original art and data for the Ashen Pilgrimage overhaul."""
import json,random,runpy
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
art=runpy.run_path(str(BASE/'scripts/generate_art.py'));png,sprite=art['png'],art['sprite']
A=BASE/'src/main/resources/assets/myfirstmod';D=BASE/'src/main/resources/data/myfirstmod'
def put(root,path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def asset(path,value):put(A,path,value)
def data(path,value):put(D,path,value)
DARK=(30,26,36);BRONZE=(154,119,76);GOLD=(234,191,114);LIGHT=(242,222,179)
sprite('ashen_flask',[
 ([(12,2),(20,2),(20,8),(26,14),(26,26),(21,30),(10,30),(5,25),(5,14),(12,8)],DARK),
 ([(13,4),(19,4),(19,9),(23,15),(23,25),(20,27),(11,27),(8,24),(8,15),(13,9)],BRONZE),
 ([(10,15),(21,15),(21,24),(18,26),(12,26),(10,23)],GOLD),
 ([(12,1),(20,1),(20,5),(12,5)],(67,56,58)),
 ([(12,17),(14,17),(14,23),(12,23)],LIGHT),
 ([(18,10),(21,13),(18,13)],LIGHT)])
sprite('pilgrim_step',[
 ([(10,2),(20,2),(20,19),(28,23),(28,29),(3,29),(3,23),(8,18)],DARK),
 ([(12,4),(18,4),(18,20),(25,24),(25,26),(6,26),(6,24),(11,20)],BRONZE),
 ([(10,9),(20,9),(20,12),(10,12)],LIGHT),
 ([(9,14),(20,14),(20,17),(9,17)],GOLD),
 ([(4,28),(28,28),(28,30),(4,30)],(84,72,75))])
sprite('mourning_ember',[
 ([(16,1),(20,10),(27,7),(25,19),(29,22),(23,30),(10,31),(3,24),(7,14),(11,17)],DARK),
 ([(16,6),(18,15),(23,12),(21,21),(25,23),(20,28),(12,28),(7,23),(10,18),(13,21)],(163,97,72)),
 ([(16,12),(17,22),(21,20),(19,26),(13,26),(10,23)],GOLD),
 ([(16,19),(18,25),(14,26)],LIGHT)])
for name in ['ashen_flask','pilgrim_step','mourning_ember']:
 asset('models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
for name,w,h in [('rift_sentinel',256,64),('shardstalker',128,64)]:
 rng=random.Random('pilgrimage-'+name);pixels=[]
 for y in range(h):
  for x in range(w):
   n=rng.randint(-5,5);c=(43+n,43+n,53+n)
   if x%8 in [0,7] or y%8 in [0,7]:c=(81,67,58)
   if (x*3+y)%31==0:c=(140,121,96)
   if name=='rift_sentinel':
    if 8<=x<=15 and 9<=y<=11:c=(231,183,111)
    if 102<=x<=126 and 3<=y<=20:c=(164+n,167+n,174+n)
    if 40<=x<60 and 20<=y<38:c=(67+n,34+n,42+n)
   else:
    if x>=48 and x<64:c=(154+n,114+n,185+n)
    if y in [30,31] and 8<=x<=15:c=(242,218,154)
    if x>=64 and (x+y)%11<2:c=(184,169,211)
   pixels.append(c+(255,))
 png('entity/'+name+'_pilgrimage.png',w,h,pixels)
for name in ['funerary_seal','mourning_reliquary']:
 for lit in [False,True]:
  suffix='_lit' if lit else '';pixels=[]
  for y in range(16):
   for x in range(16):
    c=(35,33,43)
    if x in [0,15] or y in [0,15]:c=(73,65,65)
    if x in [2,13] or y in [2,13]:c=BRONZE
    if name=='funerary_seal':glyph=(x in [7,8] and 4<=y<=12 or y==6 and 4<=x<=11 or abs(x-7.5)+abs(y-10)<3)
    else:glyph=(abs(x-7.5)+abs(y-7.5)<5 and (x+y)%3!=0)
    if glyph:c=LIGHT if lit else (119,85,75)
    pixels.append(c+(255,))
  png('block/'+name+suffix+'.png',16,16,pixels)
  asset('models/block/'+name+suffix+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name+suffix}})
 asset('blockstates/'+name+'.json',{'variants':{'rite='+str(i):{'model':'myfirstmod:block/'+name+('_lit' if i==3 else '')} for i in range(4)}})
for name,ingredients in [('ashen_flask',['minecraft:glass_bottle','myfirstmod:resonite_ingot','myfirstmod:hushberry']),('pilgrim_step',['minecraft:leather','myfirstmod:dusk_fiber','myfirstmod:resonite_ingot'])]:
 data('recipe/'+name+'.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[{'item':i} for i in ingredients],'result':{'id':'myfirstmod:'+name,'count':1}})
 data('advancement/recipes/'+name+'.json',{'criteria':{'ingot':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:resonite_ingot']}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})
def entry(name,low,high,weight):
 return {'type':'minecraft:item','name':'myfirstmod:'+name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':low,'max':high}}]}
data('loot_table/chests/pilgrim_cache.json',{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':[entry('resonite_ingot',2,4,4),entry('prism_dust',4,8,4),entry('expedition_stew',1,1,3),entry('repair_kit',1,2,2),entry('resonant_shard',1,3,2),entry('echo_sigil',1,1,1)]}]})
data('advancement/mourning_ember.json',{'parent':'myfirstmod:enter_realm','display':{'icon':{'id':'myfirstmod:mourning_ember'},'title':{'text':'A Light for the Nameless'},'description':{'text':'Complete a cathedral rite and recover a Mourning Ember.'},'frame':'challenge','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'ember':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:mourning_ember']}]}}}})

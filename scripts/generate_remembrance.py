"""Original deterministic inventory/block assets and Remembrance recipes."""
import json,runpy
from pathlib import Path
B=Path(__file__).resolve().parents[1];a=runpy.run_path(str(B/'scripts/generate_art.py'));png,sprite=a['png'],a['sprite']
A=B/'src/main/resources/assets/myfirstmod';D=B/'src/main/resources/data/myfirstmod'
def put(root,path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def asset(path,value):put(A,path,value)
def data(path,value):put(D,path,value)
for name,color,shape in [
 ('memory_shard',(185,208,224),[(16,3),(25,11),(21,26),(12,30),(7,20)]),
 ('iron_vow',(178,186,199),[(8,8),(24,8),(23,20),(16,28),(9,20)]),
 ('ember_vow',(237,159,97),[(16,4),(21,13),(24,12),(25,23),(16,28),(7,22),(11,13)]),
 ('mist_vow',(135,210,192),[(6,10),(24,7),(21,13),(26,16),(9,25),(13,18),(6,19)]),
 ('silent_vow',(169,147,191),[(8,8),(24,8),(24,24),(8,24)])]:
 sprite(name,[([(16,0),(30,12),(27,26),(16,32),(4,27),(1,12)],(40,32,46)), ([(16,3),(26,13),(24,24),(16,28),(7,24),(5,13)],(101,79,64)),(shape,color)])
 asset('models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
sprite('pilgrim_atlas',[([(3,4),(15,6),(18,3),(29,6),(29,29),(18,26),(14,29),(3,26)],(47,36,44)), ([(5,6),(14,8),(14,26),(5,24)],(198,175,137)), ([(17,7),(26,8),(26,26),(17,24)],(166,143,112)), ([(8,11),(12,13),(8,20)],(92,124,114)), ([(20,10),(23,16),(19,20)],(84,104,107))])
asset('models/item/pilgrim_atlas.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/pilgrim_atlas'}})
for name in ['memory_stele','pilgrim_ledger']:
 pixels=[]
 for y in range(16):
  for x in range(16):
   c=(38,36,49) if name=='memory_stele' else (65,45,43)
   if x in [1,14] or y in [1,14]:c=(152,118,79)
   if 4<=x<=11 and 4<=y<=11 and (y%3==1 or x==7):c=(177,223,210) if name=='memory_stele' else (217,191,141)
   pixels.append(c+(255,))
 png('block/'+name+'.png',16,16,pixels)
 asset('models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
 asset('blockstates/'+name+'.json',{'variants':{'':{'model':'myfirstmod:block/'+name}}})
asset('models/item/pilgrim_ledger.json',{'parent':'myfirstmod:block/pilgrim_ledger'})
data('loot_table/blocks/pilgrim_ledger.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'myfirstmod:pilgrim_ledger'}]}]})
recipes={'pilgrim_atlas':['minecraft:compass','minecraft:book','myfirstmod:prism_dust'],
 'pilgrim_ledger':['minecraft:book','myfirstmod:hush_planks','myfirstmod:resonite_ingot'],
 'iron_vow':['myfirstmod:memory_shard','myfirstmod:memory_shard','myfirstmod:resonite_ingot'],
 'ember_vow':['myfirstmod:memory_shard','myfirstmod:memory_shard','myfirstmod:cinder_pearl'],
 'mist_vow':['myfirstmod:memory_shard','myfirstmod:memory_shard','myfirstmod:dusk_fiber'],
 'silent_vow':['minecraft:paper','myfirstmod:hushberry']}
for name,ingredients in recipes.items():
 data('recipe/'+name+'.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':i} for i in ingredients],'result':{'id':'myfirstmod:'+name,'count':1}})
 data('advancement/recipes/'+name+'.json',{'criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[ingredients[-1]]}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})
data('loot_table/chests/memorial_cache.json',{'type':'minecraft:chest','pools':[{'rolls':3,'entries':[{'type':'minecraft:item','name':'myfirstmod:'+name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':count}]} for name,count,weight in [('hushberry',4,4),('prism_dust',4,3),('resonite_ingot',2,3),('repair_kit',1,1),('dusk_fiber',3,3)]]}]})
p=B/'src/main/resources/data/minecraft/tags/block/mineable/axe.json';d=json.loads(p.read_text());d['values']=sorted(set(d['values']+['myfirstmod:pilgrim_ledger']));p.write_text(json.dumps(d,indent=2)+'\n')
# Legacy handcrafted-frame entry is retired; old active gates remain functional.
for path in ['recipe/portal_frame.json','advancement/recipes/portal_frame.json']:
 (D/path).unlink(missing_ok=True)

"""The Hollow Keep: original assets and an isolated void dimension for reserved architecture."""
import json,random,runpy
from pathlib import Path
B=Path(__file__).resolve().parents[1];a=runpy.run_path(str(B/'scripts/generate_art.py'));png,sprite=a['png'],a['sprite']
A=B/'src/main/resources/assets/myfirstmod';D=B/'src/main/resources/data/myfirstmod'
def put(root,path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def asset(path,value):put(A,path,value)
def data(path,value):put(D,path,value)
data('dimension/hollow_keep.json',{'type':'myfirstmod:null_realm','generator':{'type':'minecraft:flat','settings':{'biome':'minecraft:the_void','features':False,'lakes':False,'layers':[{'block':'minecraft:air','height':1}],'structure_overrides':[]}}})
for name,tint in [('keep_gate',(181,221,222)),('keep_ward',(189,136,116)),('keep_heart',(210,182,119))]:
 pixels=[]
 for y in range(16):
  for x in range(16):
   c=(30,30,42)
   if x in [1,14] or y in [1,14]:c=(105,88,81)
   if abs(x-7.5)+abs(y-7.5) in [4,5,6] or x in [7,8] and 4<=y<=11:c=tint
   pixels.append(c+(255,))
 png('block/'+name+'.png',16,16,pixels)
 asset('models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
 asset('blockstates/'+name+'.json',{'variants':{'':{'model':'myfirstmod:block/'+name}}})
rng=random.Random('grave-regent');pixels=[]
for y in range(128):
 for x in range(128):
  n=rng.randint(-5,5);c=(43+n,37+n,51+n)
  if x%8 in [0,7] or y%12==0:c=(124,94,66)
  if 10<=x<20 and 11<=y<14:c=(210,233,213)
  if 48<=x<104 and y<17:c=(171+n,132+n,79+n)
  if y>=88 and 48<=x<100:c=(72+n,35+n,46+n) if (x+y)%13 else (179,138,83)
  if 96<=x<124 and 40<=y<54:c=(150+n,222+n,209+n)
  pixels.append(c+(255,))
png('entity/grave_regent.png',128,128,pixels)
sprite('regent_crest',[([(3,7),(9,12),(16,3),(23,12),(29,7),(26,26),(16,30),(6,26)],(50,37,48)), ([(6,12),(11,16),(16,8),(21,16),(26,12),(23,23),(16,27),(9,23)],(213,170,99)), ([(16,15),(20,20),(16,24),(12,20)],(137,228,209))])
sprite('requiem_glaive',[([(3,29),(25,2),(30,1),(28,8),(24,13),(22,11),(7,31)],(36,32,48)), ([(6,28),(24,6),(26,7),(8,30)],(164,128,86)), ([(23,4),(29,2),(26,9),(23,11),(22,8)],(176,230,222)), ([(17,15),(20,11),(25,14),(22,17)],(219,180,106))])
for name in ['regent_crest','requiem_glaive']:
 asset('models/item/'+name+'.json',{'parent':'minecraft:item/handheld' if name=='requiem_glaive' else 'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
data('loot_table/entities/grave_regent.json',{'type':'minecraft:entity','pools':[]})
data('recipe/requiem_glaive.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':['  C',' I ','S  '],'key':{'C':{'item':'myfirstmod:regent_crest'},'I':{'item':'myfirstmod:resonite_ingot'},'S':{'item':'minecraft:stick'}},'result':{'id':'myfirstmod:requiem_glaive','count':1}})
data('advancement/recipes/requiem_glaive.json',{'criteria':{'crest':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:regent_crest']}]}}},'rewards':{'recipes':['myfirstmod:requiem_glaive']}})
data('advancement/regent.json',{'parent':'myfirstmod:enter_realm','display':{'icon':{'id':'myfirstmod:regent_crest'},'title':{'text':'The Throne Remembers'},'description':{'text':'Defeat the Grave Regent and claim its crest.'},'frame':'challenge','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'crest':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:regent_crest']}]}}}})

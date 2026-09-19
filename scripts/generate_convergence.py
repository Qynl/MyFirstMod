"""Original Convergence art and resource contracts; no external dependencies."""
import json,random,runpy
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
art=runpy.run_path(str(BASE/'scripts/generate_art.py'))
png,sprite=art['png'],art['sprite']
ASSETS=BASE/'src/main/resources/assets/myfirstmod'
DATA=BASE/'src/main/resources/data/myfirstmod'
def put(root,path,value):
    p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def asset(path,value):put(ASSETS,path,value)
def data(path,value):put(DATA,path,value)
for name,tint in [('astral_core',(175,136,239)),('vigor_rune',(247,142,166)),('gale_rune',(110,239,200)),('focus_rune',(138,175,250))]:
    sprite(name,[([(16,1),(29,10),(27,25),(16,31),(5,25),(3,10)],(33,28,57)),
                 ([(16,4),(26,12),(23,23),(16,27),(9,23),(6,12)],tint),
                 ([(16,8),(23,14),(21,21),(16,24),(11,21),(9,14)],(40,45,73))])
    # Distinct inset glyphs as well as colors for accessibility.
    glyph={'astral_core':[(16,7),(21,15),(16,24),(11,15)],
           'vigor_rune':[(14,10),(18,10),(18,14),(22,14),(22,18),(18,18),(18,22),(14,22),(14,18),(10,18),(10,14),(14,14)],
           'gale_rune':[(12,11),(21,11),(17,15),(21,15),(12,22),(15,17),(10,17)],
           'focus_rune':[(12,10),(20,10),(23,16),(20,22),(12,22),(9,16)]}[name]
    # Draw all layers together; sprite() intentionally creates a fresh canvas.
    sprite(name,[([(16,1),(29,10),(27,25),(16,31),(5,25),(3,10)],(33,28,57)),
                 ([(16,4),(26,12),(23,23),(16,27),(9,23),(6,12)],tint),
                 ([(16,8),(23,14),(21,21),(16,24),(11,21),(9,14)],(40,45,73)),(glyph,(223,255,236))])
    asset('models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:item/'+name}})
for name,tint in [('rift_anchor',(183,125,245)),('attunement_forge',(106,239,211))]:
    pixels=[]
    for y in range(16):
        for x in range(16):
            distance=abs(x-7.5)+abs(y-7.5)
            color=(34,38,57)
            if 4<distance<7 or x in [1,14] and y in [1,14]:color=tint
            if name=='rift_anchor' and distance<2:color=(246,229,255)
            if name=='attunement_forge' and (y in [6,7] and 4<=x<=11 or 7<=x<=8 and 5<=y<=12):color=(230,255,238)
            pixels.append(color+(255,))
    png('block/'+name+'.png',16,16,pixels)
    asset('blockstates/'+name+'.json',{'variants':{'':{'model':'myfirstmod:block/'+name}}})
    asset('models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'myfirstmod:block/'+name}})
asset('models/item/attunement_forge.json',{'parent':'myfirstmod:block/attunement_forge'})
rng=random.Random('rift-herald');pixels=[]
for y in range(64):
    for x in range(64):
        n=rng.randint(-5,5);color=(43+n,40+n,71+n)
        if (x+y*3)%17<2:color=(140,107,197)
        if y<20 and 12<=x<=17:color=(178,252,228)
        if x>=36 and (y%8 in [0,1]):color=(214,160,251)
        pixels.append(color+(255,))
png('entity/rift_herald.png',64,64,pixels)
for age in range(4):
    pixels=[]
    for y in range(16):
        for x in range(16):
            color=(0,0,0,0)
            height=5+age*3
            if 7<=x<=8 and y>=16-height:color=(66,130,112,255)
            for side in [-1,1]:
                for leaf in range(age+1):
                    lx=8+side*(2+leaf%2);ly=13-leaf*3
                    if abs(x-lx)+abs(y-ly)<=2:color=(91,184,154,255)
            if age==3 and ((x-4)**2+(y-6)**2<=3 or (x-11)**2+(y-9)**2<=3):color=(210,143,239,255)
            pixels.append(color)
    png('block/hush_nursery_'+str(age)+'.png',16,16,pixels)
    asset('models/block/hush_nursery_'+str(age)+'.json',{'parent':'minecraft:block/cross','textures':{'cross':'myfirstmod:block/hush_nursery_'+str(age)}})
asset('blockstates/hush_nursery.json',{'variants':{'age='+str(age):{'model':'myfirstmod:block/hush_nursery_'+str(age)} for age in range(4)}})
asset('models/item/hush_nursery.json',{'parent':'minecraft:item/generated','textures':{'layer0':'myfirstmod:block/hush_nursery_3'}})
# The completeness check's canonical block model points to its mature stage.
asset('models/block/hush_nursery.json',{'parent':'myfirstmod:block/hush_nursery_3'})
for name in ['hush_nursery','attunement_forge']:
    data('loot_table/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'myfirstmod:'+name}]}]})
data('loot_table/entities/rift_herald.json',{'type':'minecraft:entity','pools':[]})
for rune,material in [('vigor_rune','minecraft:golden_apple'),('gale_rune','minecraft:feather'),('focus_rune','myfirstmod:prism_dust')]:
    data('recipe/'+rune+'.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'myfirstmod:astral_core'},{'item':material},{'item':'myfirstmod:resonant_shard'}],'result':{'id':'myfirstmod:'+rune,'count':1}})
data('recipe/attunement_forge.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['IRI','BBB'],'key':{'I':{'item':'myfirstmod:resonite_ingot'},'R':{'item':'myfirstmod:resonance_matrix'},'B':{'item':'myfirstmod:nullstone_bricks'}},'result':{'id':'myfirstmod:attunement_forge','count':1}})
data('recipe/hush_nursery.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'myfirstmod:hushberry'},{'item':'myfirstmod:hushberry'},{'item':'myfirstmod:dusk_fiber'},{'item':'myfirstmod:lumen_moss'}],'result':{'id':'myfirstmod:hush_nursery','count':2}})
for name,trigger in [('hush_nursery','myfirstmod:hushberry'),('attunement_forge','myfirstmod:resonite_ingot'),('vigor_rune','myfirstmod:astral_core'),('gale_rune','myfirstmod:astral_core'),('focus_rune','myfirstmod:astral_core')]:
    data('advancement/recipes/'+name+'.json',{'criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[trigger]}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})
data('advancement/convergence.json',{'parent':'myfirstmod:enter_realm','display':{'icon':{'id':'myfirstmod:astral_core'},'title':{'text':'A Star in the Silence'},'description':{'text':'Close a Convergence rift and claim an Astral Core.'},'frame':'challenge','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'core':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:astral_core']}]}}}})

path=BASE/"src/main/resources/data/minecraft/tags/block/mineable/axe.json"
tag=json.loads(path.read_text());tag["values"]=sorted(set(tag["values"]+["myfirstmod:attunement_forge"]))
path.write_text(json.dumps(tag,indent=2)+"\n")

import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources/data/myfirstmod'
def put(p,d):
    p=R/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
def item(name,count=1,weight=1):
    return {'type':'minecraft:item','name':name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':count}]}
put('loot_table/chests/realm_cache.json',{'type':'minecraft:chest','pools':[
    {'rolls':1,'entries':[item('myfirstmod:resonant_shard',{'type':'minecraft:uniform','min':1,'max':2})]},
    {'rolls':{'type':'minecraft:uniform','min':2,'max':4},'entries':[item('minecraft:echo_shard',1,2),item('minecraft:amethyst_shard',{'type':'minecraft:uniform','min':3,'max':8},5),item('minecraft:golden_carrot',{'type':'minecraft:uniform','min':2,'max':5},5),item('minecraft:diamond',1,1),item('minecraft:experience_bottle',{'type':'minecraft:uniform','min':2,'max':4},3)]}]})
for name in ['rift_sentinel','shardstalker']:
    put('loot_table/entities/'+name+'.json',{'type':'minecraft:entity','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:killed_by_player'},{'condition':'minecraft:random_chance','chance':.2}],'entries':[item('myfirstmod:resonant_shard')]},{'rolls':1,'entries':[item('minecraft:amethyst_shard',{'type':'minecraft:uniform','min':0,'max':2})]}]})
# Boss loot is distributed once per eligible player by the encounter, never as duplicate entity drops.
put('loot_table/entities/null_warden.json',{'type':'minecraft:entity','pools':[]})
put('recipe/arena_compass.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'minecraft:compass'},{'item':'minecraft:echo_shard'}],'result':{'id':'myfirstmod:arena_compass','count':1}})
put('recipe/resonance_matrix.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['SSS','SAS','SSS'],'key':{'S':{'item':'myfirstmod:resonant_shard'},'A':{'item':'minecraft:amethyst_shard'}},'result':{'id':'myfirstmod:resonance_matrix','count':1}})
put('recipe/ascended_nullblade.json',{'type':'minecraft:smithing_transform','template':{'item':'minecraft:echo_shard'},'base':{'item':'myfirstmod:nullblade'},'addition':{'item':'myfirstmod:resonance_matrix'},'result':{'id':'myfirstmod:ascended_nullblade','count':1}})
# A small advancement path makes the expedition and post-boss goal discoverable.
for name,parent,icon,title,description,criteria in [
    ('enter_realm',None,'myfirstmod:arena_compass','Beyond the Seal','Enter the Null Realm and follow the resonance.',{'enter':{'trigger':'minecraft:changed_dimension','conditions':{'to':'myfirstmod:null_realm'}}}),
    ('claim_heart','enter_realm','myfirstmod:null_relic','The Silence Breaks','Claim the Heart of the Null. Ascended courts now await.',{'heart':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:null_relic']}]}}}),
    ('gather_resonance','claim_heart','myfirstmod:resonance_matrix','What Remains','Gather eight Resonant Shards for a Resonance Matrix.',{'shards':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:resonant_shard'],'count':{'min':8}}]}}}),
    ('awaken_blade','gather_resonance','myfirstmod:ascended_nullblade','An Answer to the Void','Awaken the Nullblade at a smithing table.',{'blade':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:ascended_nullblade']}]}}})]:
    display={'icon':{'id':icon},'title':{'text':title},'description':{'text':description},'frame':'challenge' if name=='awaken_blade' else 'task','show_toast':True,'announce_to_chat':True,'hidden':False}
    if parent is None:display['background']='minecraft:textures/block/sculk.png'
    advancement={'display':display,'criteria':criteria}
    if parent:advancement['parent']='myfirstmod:'+parent
    put('advancement/'+name+'.json',advancement)
for name,ingredient in [('arena_compass','minecraft:echo_shard'),('resonance_matrix','myfirstmod:resonant_shard')]:
    put('advancement/recipes/'+name+'.json',{'criteria':{'has_item':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[ingredient]}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})

put('recipe/echo_sigil.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':[' S ','SES',' S '],'key':{'S':{'item':'myfirstmod:resonant_shard'},'E':{'item':'minecraft:echo_shard'}},'result':{'id':'myfirstmod:echo_sigil','count':1}})
put('recipe/rift_aegis.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[{'item':'minecraft:shield'},{'item':'myfirstmod:warden_crest'},{'item':'myfirstmod:resonance_matrix'}],'result':{'id':'myfirstmod:rift_aegis','count':1}})
put('recipe/expedition_journal.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'minecraft:book'},{'item':'minecraft:amethyst_shard'}],'result':{'id':'minecraft:written_book','count':1,'components':{'minecraft:custom_data':{'NullJournal':True},'minecraft:written_book_content':{'title':'Null Expedition','author':'The Threshold Archive','pages':['{"text":"Open this journal to read your expedition progress."}']}}}})
for name,ingredient in [('echo_sigil','myfirstmod:resonant_shard'),('rift_aegis','myfirstmod:warden_crest'),('expedition_journal','myfirstmod:arena_compass')]:
    put('advancement/recipes/'+name+'.json',{'criteria':{'has_item':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[ingredient]}]}}},'rewards':{'recipes':['myfirstmod:'+name]}})
put('advancement/echo_victory.json',{'parent':'myfirstmod:claim_heart','display':{'icon':{'id':'myfirstmod:warden_crest'},'title':{'text':'The Second Silence'},'description':{'text':'Defeat an echo of the Warden and claim its crest.'},'frame':'challenge','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'crest':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['myfirstmod:warden_crest']}]}}}})

put('loot_table/chests/vault_cache.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[item('myfirstmod:echo_sigil')]},{'rolls':{'type':'minecraft:uniform','min':2,'max':4},'entries':[item('myfirstmod:resonant_shard',{'type':'minecraft:uniform','min':2,'max':4},4),item('minecraft:diamond',{'type':'minecraft:uniform','min':1,'max':3},2),item('minecraft:echo_shard',2,3),item('minecraft:enchanted_golden_apple',1,1)]}]})

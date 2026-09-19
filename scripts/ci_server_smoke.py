#!/usr/bin/env python3
"""Launch a real Fabric dedicated server, exercise worldgen over local RCON, then stop.
Never use this on a production world: the isolated run/ci-smoke world is disposable.
"""
import os
import re
import secrets
import socket
import struct
import subprocess
import time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'run'
LOG=ROOT/'build/ci/server-smoke.log'

class Rcon:
    def __init__(self,password):
        self.socket=socket.create_connection(('127.0.0.1',25575),timeout=120)
        self.send(3,password)
        for _ in range(3):
            ident,kind,_=self.receive()
            if ident==-1:raise RuntimeError('Smoke-test RCON authentication failed')
            if kind==2:break
        else:raise RuntimeError('No RCON authentication response')
    def read(self,size):
        data=b''
        while len(data)<size:
            part=self.socket.recv(size-len(data))
            if not part:raise RuntimeError('RCON closed unexpectedly')
            data+=part
        return data
    def send(self,kind,text):
        body=struct.pack('<ii',1,kind)+text.encode()+b'\0\0'
        self.socket.sendall(struct.pack('<i',len(body))+body)
    def receive(self):
        size=struct.unpack('<i',self.read(4))[0]
        if not 10<=size<=1048576:raise RuntimeError('Invalid RCON response size')
        body=self.read(size)
        ident,kind=struct.unpack('<ii',body[:8])
        return ident,kind,body[8:-2].decode(errors='replace')
    def command(self,text,allow_failure=False):
        self.send(2,text)
        _,_,response=self.receive()
        print('>',text,'\n',response,flush=True)
        with (LOG.parent/'server-commands.log').open('a') as commands:
            commands.write('> '+text+'\n'+response+'\n')
        if re.search(r'unknown or incomplete|incorrect argument|does not exist',response,re.I) or (not allow_failure and re.search(r'failed|not found|not loaded',response,re.I)):
            raise RuntimeError('Smoke command failed: '+response)
        return response

def main():
    RUN.mkdir(exist_ok=True);LOG.parent.mkdir(parents=True,exist_ok=True)
    password=secrets.token_hex(24)
    (RUN/'eula.txt').write_text('eula=true\n')
    (RUN/'server.properties').write_text('\n'.join([
        'level-name=ci-smoke','level-seed=498271','online-mode=false',
        'enable-rcon=true','rcon.port=25575','rcon.password='+password,
        'server-ip=127.0.0.1','server-port=25565','spawn-protection=0',
        'view-distance=2','simulation-distance=2','max-players=2',
        'sync-chunk-writes=false','max-tick-time=120000','difficulty=normal',
        'generate-structures=true','enable-query=false','']) )
    with LOG.open('w') as log:
        proc=subprocess.Popen(['./gradlew','runServer','--args=nogui','--no-daemon','--console=plain'],
                              cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        connection=None
        try:
            deadline=time.monotonic()+420
            while time.monotonic()<deadline:
                if proc.poll() is not None:raise RuntimeError('Server exited during startup')
                if 'Done (' in LOG.read_text(errors='replace'):
                    try:connection=Rcon(password);break
                    except ConnectionRefusedError:pass
                time.sleep(1)
            if connection is None:raise TimeoutError('Dedicated server did not become ready')
            prefix='execute in myfirstmod:null_realm run '
            connection.command(prefix+'forceload add 0 0 0 160')
            connection.command(prefix+'forceload add 256 256 288 288')
            connection.command(prefix+'place feature myfirstmod:realm_ruins 264 100 264')
            connection.command(prefix+'place feature myfirstmod:realm_vault 280 100 280')
            connection.command(prefix+'setblock 8 81 30 myfirstmod:echo_altar')
            connection.command(prefix+'setblock 21 81 152 myfirstmod:resonance_core')
            connection.command(prefix+'summon myfirstmod:rift_sentinel 8 120 152')
            connection.command(prefix+'summon myfirstmod:shardstalker 10 120 154')
            # Deterministic shrine fixture, independent of random trees or steep terrain.
            connection.command(prefix+'forceload add 1024 1024 1056 1056')
            # Tickets load asynchronously. /place feature also needs the surrounding chunks.
            chunk_deadline=time.monotonic()+60
            while True:
                ready=all('passed' in connection.command(
                    prefix+f'execute if loaded {x} 80 {z}',allow_failure=True).lower()
                    for x in [1032,1048,1064] for z in [1032,1048,1064])
                if ready:break
                if time.monotonic()>chunk_deadline:raise TimeoutError('Shrine fixture chunks did not load')
                time.sleep(1)
            connection.command(prefix+'fill 1042 60 1042 1054 200 1054 minecraft:air')
            connection.command(prefix+'fill 1042 80 1042 1054 80 1054 myfirstmod:hushed_moss')
            connection.command(prefix+'place feature myfirstmod:waystone_shrine 1048 81 1048')
            connection.command(prefix+'execute if block 1048 81 1048 myfirstmod:waystone run say SMOKE_WAYSTONE_OK')
            connection.command(prefix+'fill 1042 81 1042 1054 200 1054 minecraft:air')
            connection.command(prefix+'fill 1042 80 1042 1054 80 1054 myfirstmod:hushed_moss')
            connection.command(prefix+'place feature myfirstmod:rift_observatory 1048 81 1048')
            connection.command(prefix+'execute if block 1048 81 1048 myfirstmod:rift_anchor run say SMOKE_RIFT_OK')
            connection.command(prefix+'summon myfirstmod:rift_herald 1048 83 1048')
            connection.command(prefix+'setblock 1044 81 1044 myfirstmod:attunement_forge')
            connection.command(prefix+'setblock 1045 80 1044 myfirstmod:hushed_moss')
            connection.command(prefix+'setblock 1045 81 1044 myfirstmod:hush_nursery[age=3]')
            connection.command(prefix+'loot spawn 1045 83 1044 mine 1045 81 1044 minecraft:iron_hoe')
            connection.command(prefix+'execute if entity @e[type=minecraft:item,nbt={Item:{id:"myfirstmod:hush_nursery"}}] run say SMOKE_NURSERY_OK')
            # Exercise the actual block-loot codecs, not just JSON parsing.
            for index,(ore,drop) in enumerate([('resonite_ore','raw_resonite'),('prism_ore','prism_dust'),('cinder_ore','cinder_pearl')]):
                x=8+index
                connection.command(prefix+f'setblock {x} 110 155 myfirstmod:{ore}')
                connection.command(prefix+f'loot spawn {x} 120 155 mine {x} 110 155 minecraft:iron_pickaxe')
                selector='@e[type=minecraft:item,nbt={Item:{id:"myfirstmod:'+drop+'"}}]'
                connection.command(prefix+'execute if entity '+selector+' run say SMOKE_ORE_OK')
            connection.command(prefix+'loot spawn 8 120 155 loot myfirstmod:chests/waystone_cache')
            # Full three-level cathedral fixture with actual custom spawner codecs.
            connection.command(prefix+'fill 1041 60 1041 1055 200 1055 minecraft:air')
            connection.command(prefix+'fill 1041 80 1041 1055 80 1055 myfirstmod:hushed_moss')
            connection.command(prefix+'place feature myfirstmod:mourning_cathedral 1048 81 1048')
            for x,y,z,block in [(1048,65,1048,'mourning_reliquary'),(1048,81,1044,'funerary_seal'),(1048,73,1052,'funerary_seal'),(1052,65,1048,'funerary_seal')]:
                connection.command(prefix+f'execute if block {x} {y} {z} myfirstmod:{block}[rite=0] run say SMOKE_CATHEDRAL_OK')
            connection.command(prefix+'execute if block 1048 73 1043 minecraft:spawner run say SMOKE_CRYPT_SPAWNER_OK')
            for y,expected in [(73,'rift_sentinel'),(65,'shardstalker')]:
                response=connection.command(prefix+f'data get block 1048 {y} 1043 SpawnData.entity.id')
                if 'myfirstmod:'+expected not in response:raise RuntimeError('Wrong cathedral spawner actor: '+response)
            connection.command(prefix+'loot spawn 1048 83 1048 loot myfirstmod:chests/pilgrim_cache')
            connection.command(prefix+'setblock 1048 65 1048 myfirstmod:mourning_reliquary[rite=3]')
            connection.command(prefix+'summon minecraft:item 1048 84 1048 {Item:{id:"myfirstmod:ashen_flask",count:1}}')
            connection.command(prefix+'summon minecraft:item 1048 84 1048 {Item:{id:"myfirstmod:pilgrim_step",count:1}}')
            # Repeat placements exercise memorial variants without relying on player interaction.
            for attempt in range(3):
                connection.command(prefix+'fill 1041 60 1041 1055 200 1055 minecraft:air')
                connection.command(prefix+'fill 1041 80 1041 1055 80 1055 myfirstmod:hushed_moss')
                connection.command(prefix+'place feature myfirstmod:forgotten_memorial 1048 81 1048')
                connection.command(prefix+'execute if block 1048 81 1048 myfirstmod:memory_stele run say SMOKE_MEMORY_OK')
            connection.command(prefix+'setblock 1049 81 1048 myfirstmod:pilgrim_ledger')
            connection.command(prefix+'loot spawn 1049 83 1048 mine 1049 81 1048 minecraft:iron_axe')
            connection.command(prefix+'execute if entity @e[type=minecraft:item,nbt={Item:{id:"myfirstmod:pilgrim_ledger"}}] run say SMOKE_LEDGER_OK')
            connection.command(prefix+'loot spawn 1048 83 1048 loot myfirstmod:chests/memorial_cache')
            for item in ['pilgrim_atlas','memory_shard','iron_vow','ember_vow','mist_vow','silent_vow']:
                connection.command(prefix+'summon minecraft:item 1048 84 1048 {Item:{id:"myfirstmod:'+item+'",count:1}}')
            # Exercise the actual incremental fortress builder, not a substitute fixture.
            build_deadline=time.monotonic()+120
            while 'KEEP_READY' not in connection.command('hollowkeep prepare'):
                if time.monotonic()>build_deadline:raise TimeoutError('Keep construction did not finish')
                time.sleep(1)
            keep='execute in myfirstmod:hollow_keep run '
            connection.command(keep+'forceload add -32 -32 31 31')
            for x,z,block in [(-20,0,'keep_ward'),(20,0,'keep_ward'),(0,-20,'keep_ward'),(0,22,'keep_heart'),(0,28,'keep_gate')]:
                connection.command(keep+f'execute if block {x} 65 {z} myfirstmod:{block} run say SMOKE_KEEP_ROOM_OK')
            connection.command(keep+'execute if block 28 85 28 myfirstmod:prism_lamp run say SMOKE_KEEP_TOWER_OK')
            connection.command(keep+'summon myfirstmod:grave_regent 0 65 0')
            connection.command(keep+'summon minecraft:item 0 66 22 {Item:{id:"myfirstmod:requiem_glaive",count:1}}')
            connection.command(keep+'loot spawn 0 66 22 loot myfirstmod:entities/grave_regent')
            # Flat natural-ground fixtures exercise the new scenery path for all four materials.
            for material in ['hushed_moss','lumen_moss','prismstone','cinderstone']:
                connection.command(prefix+'fill 1041 60 1041 1055 200 1055 minecraft:air')
                connection.command(prefix+f'fill 1041 78 1041 1055 80 1055 myfirstmod:{material}')
                connection.command(prefix+'place feature myfirstmod:realm_scenery 1048 81 1048')
                connection.command(prefix+'execute unless blocks 1042 81 1042 1053 81 1053 1042 100 1042 all run say SMOKE_SCENERY_OK')
            # Actual vanilla city-center templates, including their top-center jigsaw final state.
            city='execute in minecraft:overworld run '
            connection.command(city+'forceload add 2000 2000 2095 2095')
            deadline=time.monotonic()+90
            while not all('passed' in connection.command(city+f'execute if loaded {x} 80 {z}',allow_failure=True).lower()
                          for x in range(2008,2096,16) for z in range(2008,2096,16)):
                if time.monotonic()>deadline:raise TimeoutError('Ancient City fixture chunks did not load')
                time.sleep(1)
            def rotated(x,y,z,turn):
                for _ in range(turn):x,z=-z,x
                return f'{2048+x} {80+y} {2048+z}'
            for template in range(1,4):
                for turn,rotation in enumerate(['none','clockwise_90','180','counterclockwise_90']):
                    corners=[tuple(map(int,rotated(x,0,z,turn).split())) for x in [0,17] for z in [0,40]]
                    xmin,xmax=min(p[0] for p in corners),max(p[0] for p in corners)
                    zmin,zmax=min(p[2] for p in corners),max(p[2] for p in corners)
                    connection.command(city+f'fill {xmin} 80 {zmin} {xmax} 110 {zmax} minecraft:air')
                    connection.command(city+f'place template minecraft:ancient_city/city_center/city_center_{template} 2048 80 2048 {rotation}')
                    # /place template retains jigsaws; natural jigsaw generation replaces this one.
                    connection.command(city+f'setblock {rotated(13,24,20,turn)} minecraft:reinforced_deepslate',allow_failure=True)
                    if turn==0:
                        connection.command(city+f'setblock {rotated(13,20,18,turn)} minecraft:chest')
                        response=connection.command(city+f'nullgate {rotated(13,17,10,turn)}',allow_failure=True)
                        if 'GATE_INVALID' not in response:raise RuntimeError('Obstructed city gate was accepted: '+response)
                        connection.command(city+f'execute if block {rotated(13,18,11,turn)} minecraft:air run say SMOKE_GATE_NO_PARTIAL_WRITE')
                        connection.command(city+f'setblock {rotated(13,20,18,turn)} minecraft:air')
                    response=connection.command(city+f'nullgate {rotated(13,17,10,turn)}')
                    if 'GATE_OPEN' not in response:raise RuntimeError('Vanilla city frame did not open: '+response)
                    axis='z' if turn%2==0 else 'x'
                    for y,z in [(18,11),(18,30),(23,11),(23,30),(20,20)]:
                        connection.command(city+f'execute if block {rotated(13,y,z,turn)} myfirstmod:void_portal[axis={axis}] run say SMOKE_CITY_GATE_OK')
                    connection.command(city+f'execute if block {rotated(13,24,20,turn)} minecraft:reinforced_deepslate run say SMOKE_CITY_OUTLINE_OK')
            connection.command(city+'forceload remove 2000 2000 2095 2095')
            connection.command('save-all flush')
            connection.command('stop')
            code=proc.wait(timeout=120)
            if code:raise RuntimeError(f'Dedicated server task failed with exit code {code}')
            text=LOG.read_text(errors='replace')
            bad=[line for line in text.splitlines() if re.search(
                r'Failed to (?:parse|load)|Couldn.t (?:parse|load)|Error loading|Exception in server tick|Unbound values|Missing referenced',line,re.I)]
            if bad:raise RuntimeError('Resource/runtime errors:\n'+'\n'.join(bad))
            print('PASS: server startup, realm chunks, ruins/vault/shrine/observatory/cathedral, spawner IDs, nursery/ore drops, caches, mobs, save and shutdown.',flush=True)
        finally:
            if connection:connection.socket.close()
            if proc.poll() is None:
                os.killpg(proc.pid,15)
                try:proc.wait(timeout=20)
                except subprocess.TimeoutExpired:os.killpg(proc.pid,9);proc.wait()

if __name__=='__main__':
    try:main()
    except Exception as error:
        message=str(error).replace('%','%25').replace('\n','%0A').replace('\r','%0D')
        print('::error title=Server smoke failure::'+message,flush=True)
        raise

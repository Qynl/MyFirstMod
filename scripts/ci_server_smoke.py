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
            # Exercise the actual block-loot codecs, not just JSON parsing.
            for index,(ore,drop) in enumerate([('resonite_ore','raw_resonite'),('prism_ore','prism_dust'),('cinder_ore','cinder_pearl')]):
                x=8+index
                connection.command(prefix+f'setblock {x} 110 155 myfirstmod:{ore}')
                connection.command(prefix+f'loot spawn {x} 120 155 mine {x} 110 155 minecraft:iron_pickaxe')
                selector='@e[type=minecraft:item,nbt={Item:{id:"myfirstmod:'+drop+'"}}]'
                connection.command(prefix+'execute if entity '+selector+' run say SMOKE_ORE_OK')
            connection.command(prefix+'loot spawn 8 120 155 loot myfirstmod:chests/waystone_cache')
            connection.command('save-all flush')
            connection.command('stop')
            code=proc.wait(timeout=120)
            if code:raise RuntimeError(f'Dedicated server task failed with exit code {code}')
            text=LOG.read_text(errors='replace')
            bad=[line for line in text.splitlines() if re.search(
                r'Failed to (?:parse|load)|Couldn.t (?:parse|load)|Error loading|Exception in server tick|Unbound values|Missing referenced',line,re.I)]
            if bad:raise RuntimeError('Resource/runtime errors:\n'+'\n'.join(bad))
            print('PASS: server startup, realm chunks, ruins/vault/shrine, ore drops, caches, mobs, save and shutdown.',flush=True)
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

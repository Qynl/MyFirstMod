#!/usr/bin/env python3
"""2.0.0-alpha.6 refinement pass: the realm build set, remembrance advancements
and the wayfarer bounty. Emits blockstates, block/item models, shaped + stonecutting
recipes, block loot tables, tag additions, lang names and nine advancements."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/myfirstmod'
TAGS = ROOT / 'src/main/resources/data/minecraft/tags'
ASSETS = ROOT / 'src/main/resources/assets/myfirstmod'

PREFIX = {'nullstone': 'nullstone', 'polished_nullstone': 'polished_nullstone',
          'nullstone_bricks': 'nullstone_brick', 'veilstone': 'veilstone',
          'prismstone': 'prismstone', 'cinderstone': 'cinderstone', 'hush_planks': 'hush_plank'}
STONES = ['nullstone', 'polished_nullstone', 'nullstone_bricks', 'veilstone', 'prismstone', 'cinderstone']
PIECES = {'stairs': 'Stairs', 'slab': 'Slab', 'wall': 'Wall'}
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
BOTTOM_OFFSET = {'straight': 270, 'inner_left': 180, 'inner_right': 270, 'outer_left': 180, 'outer_right': 270}
TOP_OFFSET = {'straight': 270, 'inner_left': 270, 'inner_right': 0, 'outer_left': 270, 'outer_right': 0}
SHAPE_MODEL = {'straight': '', 'inner_left': '_inner', 'inner_right': '_inner',
               'outer_left': '_outer', 'outer_right': '_outer'}
LANG_NAMES = {'nullstone': 'Nullstone', 'polished_nullstone': 'Polished Nullstone',
              'nullstone_bricks': 'Nullstone Bricks', 'veilstone': 'Veilstone',
              'prismstone': 'Prismstone', 'cinderstone': 'Cinderstone', 'hush_planks': 'Hush Planks'}

LANDMARK_META = [
    ('veil_watchtower', 'Veil Watchtower', 'myfirstmod:prism_lamp', 'Climb a Veil Watchtower and open the cache aloft.'),
    ('brine_chapel', 'Brine Chapel', 'myfirstmod:tide_lantern', 'Wade the flooded nave of a Brine Chapel and claim its cache.'),
    ('geode_garden', 'Geode Garden', 'minecraft:amethyst_shard', 'Break open a Geode Garden and take the half-buried cache.'),
    ('slag_camp', 'Slag Camp', 'myfirstmod:hushberry', 'Rest a moment at a Slag Camp and loot its supply barrels.'),
    ('caravan_wreck', 'Caravan Wreck', 'myfirstmod:dusk_fiber', 'Pick through a Caravan Wreck and its spilled caches.'),
    ('echo_fissure', 'Echo Fissure', 'minecraft:echo_shard', 'Descend an Echo Fissure past the Warden and seize the echo cache.'),
    ('heartwood_circle', 'Heartwood Circle', 'myfirstmod:hush_leaves', 'Stand in a Heartwood Circle and uncover the covered cache.'),
    ('fen_shrine', 'Fen Shrine', 'myfirstmod:lumen_moss', 'Kneel at a Fen Shrine and take the cache at its steps.'),
]


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n')


def stairs_blockstate(name):
    variants = {}
    for facing in ['east', 'north', 'south', 'west']:
        for half, offsets in (('bottom', BOTTOM_OFFSET), ('top', TOP_OFFSET)):
            for shape in ['inner_left', 'inner_right', 'outer_left', 'outer_right', 'straight']:
                apply = {'model': f'myfirstmod:block/{name}{SHAPE_MODEL[shape]}', 'uvlock': True}
                y = (ROT[facing] + offsets[shape]) % 360
                if half == 'top':
                    apply['x'] = 180
                if y:
                    apply['y'] = y
                variants[f'facing={facing},half={half},shape={shape}'] = apply
    return {'variants': variants}


def wall_blockstate(name):
    parts = [{'when': {'up': 'true'}, 'apply': {'model': f'myfirstmod:block/{name}_post'}}]
    for i, direction in enumerate(['north', 'east', 'south', 'west']):
        parts.append({'when': {direction: 'low'},
                      'apply': {'model': f'myfirstmod:block/{name}_side', 'uvlock': True, **({'y': i * 90} if i else {})}})
        parts.append({'when': {direction: 'tall'},
                      'apply': {'model': f'myfirstmod:block/{name}_side_tall', 'uvlock': True, **({'y': i * 90} if i else {})}})
    return {'multipart': parts}


def textures(base):
    t = f'myfirstmod:block/{base}'
    return {'bottom': t, 'side': t, 'top': t}


def main():
    lang = json.loads((ASSETS / 'lang/en_us.json').read_text())
    pickaxe = json.loads((TAGS / 'block/mineable/pickaxe.json').read_text())
    axe = json.loads((TAGS / 'block/mineable/axe.json').read_text())
    planks_block = json.loads((TAGS / 'block/planks.json').read_text())
    planks_item = json.loads((TAGS / 'item/planks.json').read_text())
    # Idempotence: drop anything this generator owns (including retired names)
    # before re-adding, so repeated runs never duplicate or strand entries.
    owned = set()
    for base, stone in [(b, True) for b in STONES] + [('hush_planks', False)]:
        for piece in (['stairs', 'slab', 'wall'] if stone else ['stairs', 'slab']):
            owned.add(f'myfirstmod:{PREFIX[base]}_{piece}')
    owned |= {'myfirstmod:nullstone_bricks_stairs', 'myfirstmod:nullstone_bricks_slab', 'myfirstmod:nullstone_bricks_wall'}
    for key in [k for k in lang if k.startswith('block.myfirstmod.') and k.split('.', 2)[2] in
                {o.split(':')[1] for o in owned}]:
        del lang[key]
    for tag in (pickaxe, axe, planks_block, planks_item):
        tag['values'] = [v for v in tag['values'] if v not in owned]

    sets = [(base, True) for base in STONES] + [('hush_planks', False)]
    for base, stone in sets:
        for piece in (['stairs', 'slab', 'wall'] if stone else ['stairs', 'slab']):
            name = f'{PREFIX[base]}_{piece}'
            block_id = f'myfirstmod:{name}'
            lang[f'block.myfirstmod.{name}'] = f'{LANG_NAMES[base]} {PIECES[piece]}'
            # Blockstate + models
            if piece == 'stairs':
                dump(ASSETS / f'blockstates/{name}.json', stairs_blockstate(name))
                for suffix, parent in (('', 'minecraft:block/stairs'), ('_inner', 'minecraft:block/inner_stairs'),
                                       ('_outer', 'minecraft:block/outer_stairs')):
                    dump(ASSETS / f'models/block/{name}{suffix}.json', {'parent': parent, 'textures': textures(base)})
                dump(ASSETS / f'models/item/{name}.json', {'parent': f'myfirstmod:block/{name}'})
            elif piece == 'slab':
                dump(ASSETS / f'blockstates/{name}.json', {'variants': {
                    'type=bottom': {'model': f'myfirstmod:block/{name}'},
                    'type=top': {'model': f'myfirstmod:block/{name}_top'},
                    'type=double': {'model': f'myfirstmod:block/{base}'}}})
                dump(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/slab', 'textures': textures(base)})
                dump(ASSETS / f'models/block/{name}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': textures(base)})
                dump(ASSETS / f'models/item/{name}.json', {'parent': f'myfirstmod:block/{name}'})
            else:
                dump(ASSETS / f'blockstates/{name}.json', wall_blockstate(name))
                for suffix, parent in (('_post', 'minecraft:block/template_wall_post'),
                                       ('_side', 'minecraft:block/template_wall_side'),
                                       ('_side_tall', 'minecraft:block/template_wall_side_tall')):
                    dump(ASSETS / f'models/block/{name}{suffix}.json', {'parent': parent, 'textures': {'wall': f'myfirstmod:block/{base}'}})
                dump(ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:block/wall_inventory',
                                                           'textures': {'wall': f'myfirstmod:block/{base}'}})
            # Recipes: shaped for every piece, stonecutting for the stone families
            if piece == 'stairs':
                pattern, count = ['#  ', '## ', '###'], 4
            elif piece == 'slab':
                pattern, count = ['###'], 6
            else:
                pattern, count = ['###', '###'], 6
            dump(DATA / f'recipe/{name}.json', {
                'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': pattern,
                'key': {'#': {'item': f'myfirstmod:{base}'}}, 'result': {'id': block_id, 'count': count}})
            if stone:
                dump(DATA / f'recipe/{name}_from_stonecutting.json', {
                    'type': 'minecraft:stonecutting', 'ingredient': {'item': f'myfirstmod:{base}'},
                    'result': {'count': 1, 'id': block_id}})
            # Loot: slabs double up when placed as a full block
            if piece == 'slab':
                dump(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
                    'type': 'minecraft:item', 'name': block_id, 'functions': [
                        {'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [
                            {'condition': 'minecraft:block_state_property', 'block': block_id, 'properties': {'type': 'double'}}]},
                        {'function': 'minecraft:explosion_decay'}]}]}]})
            else:
                dump(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1,
                    'entries': [{'type': 'minecraft:item', 'name': block_id}],
                    'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
            # Tags
            if stone:
                pickaxe['values'].append(block_id)
            else:
                axe['values'].append(block_id)
                planks_block['values'].append(block_id)
                planks_item['values'].append(block_id)

    # Remembrance: eight discovery advancements plus the survey bounty.
    for kind, title, icon, description in LANDMARK_META:
        dump(DATA / f'advancement/landmarks/{kind}.json', {
            'parent': 'myfirstmod:enter_realm',
            'display': {'icon': {'id': icon}, 'title': {'text': title}, 'description': {'text': description},
                        'frame': 'task', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
            'criteria': {'discover': {'trigger': 'minecraft:impossible'}},
            'rewards': {'experience': 50}})
    dump(DATA / 'advancement/landmarks/cartographer_of_the_null.json', {
        'parent': 'myfirstmod:enter_realm',
        'display': {'icon': {'id': 'myfirstmod:pilgrim_atlas'}, 'title': {'text': 'Cartographer of the Null'},
                    'description': {'text': 'Open a cache of every landmark family in the Null Realm.'},
                    'frame': 'goal', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
        'criteria': {'complete': {'trigger': 'minecraft:impossible'}},
        'rewards': {'experience': 200, 'loot': ['myfirstmod:chests/wayfarer_bounty']}})
    dump(DATA / 'loot_table/chests/wayfarer_bounty.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'myfirstmod:memory_shard',
                                  'functions': [{'function': 'minecraft:set_count', 'count': 3, 'add': False}]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:echo_shard',
                                  'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False}]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'myfirstmod:resonite_ingot',
                                  'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False}]}]}]})

    (TAGS / 'block/mineable/pickaxe.json').write_text(json.dumps(pickaxe, indent=2) + '\n')
    (TAGS / 'block/mineable/axe.json').write_text(json.dumps(axe, indent=2) + '\n')
    (TAGS / 'block/planks.json').write_text(json.dumps(planks_block, indent=2) + '\n')
    (TAGS / 'item/planks.json').write_text(json.dumps(planks_item, indent=2) + '\n')
    (ASSETS / 'lang/en_us.json').write_text(json.dumps(lang, indent=2) + '\n')
    print(f'Build set: {sum(3 if s else 2 for _, s in sets)} pieces across {len(sets)} families, '
          f'{len(LANDMARK_META) + 1} remembrance advancements.')


if __name__ == '__main__':
    main()

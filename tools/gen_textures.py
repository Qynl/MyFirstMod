#!/usr/bin/env python3
"""
FabricRacers - procedural texture generator.

Generates every PNG the mod needs (no binary blobs checked in by hand), so the
art can be regenerated or tweaked at any time:

    python3 tools/gen_textures.py

It only uses the Python standard library (zlib + struct) so it runs anywhere.

CAR TEXTURE LAYOUT (64x64) - mirrored in
src/client/java/dev/qynl/racers/client/CarTexture.java :

    BODY_TOP    ( 0,  0, 32, 16)   hood / roof / trunk top surfaces
    BODY_SIDE   ( 0, 16, 32, 16)   side panels (mirrored on both sides)
    BODY_FRONT  ( 0, 32, 16, 12)   front fascia (lights, grille)
    BODY_BACK   (16, 32, 16, 12)   rear fascia (tail lights)
    GLASS       (32, 32, 16, 16)   windows (semi transparent)
    DETAIL      (48, 32, 16, 16)   racing numbers / decals / accents
    WHEEL_SIDE  (32,  0, 16, 16)   tyre sidewall + rim (left/right faces)
    WHEEL_TREAD (32, 16, 16, 16)   tread band (wraps around the tyre)
    WHEEL_HUB   (48,  0, 16, 16)   inner hub + brake disc
    EXTRA       (48, 16, 16, 16)   spoilers, mirrors, exhausts
    ICON        ( 0, 48, 32, 16)   free area: badge / decal art
    TIRE_ICON   (32, 48, 16, 16)   tyre item icon
"""

import json
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fabricracers")
TEXTURES = os.path.join(ASSETS, "textures")


# ---------------------------------------------------------------------------
# tiny PNG canvas
# ---------------------------------------------------------------------------
class Canvas:
    def __init__(self, w, h, bg=(0, 0, 0, 0)):
        self.w = w
        self.h = h
        self.px = [list(bg) for _ in range(w * h)]

    def set(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            if len(color) == 3:
                color = (color[0], color[1], color[2], 255)
            self.px[y * self.w + x] = list(color)

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return tuple(self.px[y * self.w + x])
        return (0, 0, 0, 0)

    def rect(self, x, y, w, h, color):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, color)

    def border(self, x, y, w, h, color):
        for xx in range(x, x + w):
            self.set(xx, y, color)
            self.set(xx, y + h - 1, color)
        for yy in range(y, y + h):
            self.set(x, yy, color)
            self.set(x + w - 1, yy, color)

    def hline(self, x0, x1, y, color):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            self.set(x, y, color)

    def vline(self, x, y0, y1, color):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            self.set(x, y, color)

    def line(self, x0, y0, x1, y1, color):
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx - dy
        while True:
            self.set(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x0 += sx
            if e2 < dx:
                err += dx
                y0 += sy

    def disc(self, cx, cy, r, color, fill=True):
        for y in range(int(cy - r), int(cy + r) + 1):
            for x in range(int(cx - r), int(cx + r) + 1):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= r - 0.25:
                    self.set(x, y, color)
                elif fill and d <= r + 0.25:
                    self.set(x, y, color)

    def ring(self, cx, cy, r, color, thickness=1.1):
        for y in range(int(cy - r - 2), int(cy + r) + 3):
            for x in range(int(cx - r - 2), int(cx + r) + 3):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if r - thickness <= d <= r:
                    self.set(x, y, color)

    def blit(self, other, dx, dy, alpha=True):
        for y in range(other.h):
            for x in range(other.w):
                c = other.get(x, y)
                if c[3] == 0 and alpha:
                    continue
                self.set(dx + x, dy + y, c)

    def scale(self, factor):
        out = Canvas(self.w * factor, self.h * factor)
        for y in range(out.h):
            for x in range(out.w):
                out.set(x, y, self.get(x // factor, y // factor))
        return out

    def save(self, path, mcmeta=None):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        raw = b""
        for y in range(self.h):
            raw += b"\x00"
            for x in range(self.w):
                r, g, b, a = self.px[y * self.w + x]
                raw += bytes((r & 255, g & 255, b & 255, a & 255))
        png = b"\x89PNG\r\n\x1a\n"
        png += _chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        png += _chunk(b"IDAT", zlib.compress(raw, 9))
        png += _chunk(b"IEND", b"")
        with open(path, "wb") as fh:
            fh.write(png)
        if mcmeta is not None:
            with open(path + ".mcmeta", "w") as fh:
                json.dump(mcmeta, fh, indent=2)


def _chunk(tag, data):
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


# ---------------------------------------------------------------------------
# color helpers
# ---------------------------------------------------------------------------
def shade(color, amount):
    """amount > 0 lightens, < 0 darkens (keeps alpha)."""
    r, g, b = color[0], color[1], color[2]
    a = color[3] if len(color) > 3 else 255
    if amount >= 0:
        r = int(r + (255 - r) * amount)
        g = int(g + (255 - g) * amount)
        b = int(b + (255 - b) * amount)
    else:
        r = int(r * (1 + amount))
        g = int(g * (1 + amount))
        b = int(b * (1 + amount))
    return (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), a)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def noise_rect(canvas, x, y, w, h, base, spread=10, rng=None, speckle=0.0, speckle_color=None):
    rng = rng or random.Random(1234)
    speckle_color = speckle_color or shade(base, -0.35)
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            v = rng.randint(-spread, spread)
            c = (base[0] + v, base[1] + v, base[2] + v, base[3] if len(base) > 3 else 255)
            if speckle and rng.random() < speckle:
                c = speckle_color
            canvas.set(xx, yy, c)


# ---------------------------------------------------------------------------
# palettes
# ---------------------------------------------------------------------------
PAINTS = {
    "red": (196, 44, 40),
    "blue": (44, 92, 196),
    "yellow": (232, 190, 44),
    "green": (56, 168, 78),
    "orange": (232, 122, 40),
    "purple": (140, 62, 190),
    "black": (40, 40, 44),
    "white": (232, 232, 232),
}

CAR_COLORS = {
    "kart": ((228, 88, 42), (255, 226, 120)),
    "rally": ((62, 142, 214), (245, 245, 245)),
    "sport": ((214, 46, 46), (40, 40, 44)),
    "hyper": ((252, 196, 22), (36, 36, 40)),
    "f1": ((24, 148, 92), (240, 240, 240)),
    "truck": ((176, 96, 48), (120, 120, 126)),
    "drift": ((236, 108, 196), (52, 46, 60)),
    "hotrod": ((144, 44, 60), (206, 206, 198)),
}


# ---------------------------------------------------------------------------
# car skins (64x64) - layout mirrors CarTexture.java
# ---------------------------------------------------------------------------
def car_skin(name, primary, secondary, style):
    c = Canvas(64, 64, (0, 0, 0, 0))
    rng = random.Random(hash(name) & 0xFFFF)
    dark = shade(primary, -0.45)
    light = shade(primary, 0.35)

    # --- BODY_TOP (0,0,32,16): hood stripes, roof, trunk -------------------
    noise_rect(c, 0, 0, 32, 16, primary, spread=6, rng=rng)
    c.rect(0, 2, 32, 1, light)
    c.rect(0, 13, 32, 1, shade(primary, -0.2))
    for i in range(0, 32, 6):
        c.rect(i, 0, 2, 16, secondary)
    if style == "f1":
        c.rect(14, 4, 4, 8, (26, 26, 30))          # airbox / cockpit opening
    if style in ("sport", "hyper"):
        c.rect(2, 5, 28, 1, shade(secondary, -0.2))
    if style == "truck":
        c.rect(0, 0, 32, 3, shade(primary, -0.15))  # ribbed roof
    if style == "rally":
        c.rect(12, 6, 8, 4, (250, 250, 250))
        c.rect(13, 7, 2, 2, (28, 28, 32))
        c.rect(17, 7, 2, 2, (28, 28, 32))
    if style == "drift":
        c.rect(0, 0, 32, 2, secondary)
    if style == "hotrod":
        c.rect(10, 4, 12, 3, shade(primary, -0.3))

    # --- BODY_SIDE (0,16,32,16) -------------------------------------------
    noise_rect(c, 0, 16, 32, 16, primary, spread=5, rng=rng)
    c.rect(0, 16, 32, 2, light)
    c.rect(0, 29, 32, 3, dark)
    # window band
    c.rect(6, 19, 20, 5, (26, 30, 40))
    c.rect(6, 19, 20, 1, (70, 80, 100))
    if style in ("sport", "hyper", "f1", "drift"):
        c.rect(0, 24, 32, 1, secondary)
    if style == "hyper":
        c.rect(3, 25, 26, 2, shade(primary, -0.35))   # side scoop
    if style == "truck":
        c.rect(0, 22, 32, 1, dark)
    c.rect(26, 25, 3, 3, (250, 236, 140))             # side marker / number roundel
    c.rect(27, 26, 1, 1, (40, 40, 40))
    if style == "rally":
        c.rect(2, 25, 4, 3, (250, 250, 250))
    if style == "kart":
        c.rect(0, 27, 32, 2, (60, 60, 66))

    # --- BODY_FRONT (0,32,16,12) -----------------------------------------
    noise_rect(c, 0, 32, 16, 12, shade(primary, -0.1), spread=5, rng=rng)
    c.rect(1, 34, 14, 3, (36, 40, 48))                # grille
    for i in range(2, 15, 3):
        c.rect(i, 34, 1, 3, (90, 96, 108))
    head = (252, 244, 196)
    c.rect(1, 38, 4, 3, head)                          # headlights
    c.rect(11, 38, 4, 3, head)
    c.rect(1, 38, 4, 1, (255, 255, 240))
    c.rect(4, 42, 8, 2, (30, 30, 34))                  # splitter
    if style == "f1":
        c.rect(6, 32, 4, 4, (30, 30, 34))
    if style == "truck":
        c.rect(0, 32, 16, 2, shade(primary, -0.35))

    # --- BODY_BACK (16,32,16,12) -----------------------------------------
    noise_rect(c, 16, 32, 16, 12, shade(primary, -0.12), spread=5, rng=rng)
    tail = (226, 54, 54)
    c.rect(17, 34, 5, 2, tail)
    c.rect(26, 34, 5, 2, tail)
    c.rect(17, 34, 5, 1, (255, 120, 120))
    c.rect(19, 38, 10, 3, (40, 40, 46))                # diffuser
    c.rect(22, 41, 4, 1, (70, 70, 78))
    if style in ("sport", "hyper", "f1", "drift", "hotrod"):
        c.rect(16, 44, 16, 1, secondary)

    # --- GLASS (32,32,16,16) ---------------------------------------------
    for y in range(16):
        for x in range(16):
            t = y / 15.0
            base = mix((46, 58, 78, 210), (18, 22, 32, 235), t)
            c.set(32 + x, 32 + y, base)
    for i in range(16):
        c.set(32 + i, 32 + i % 3, (120, 150, 190, 200))
    c.rect(32, 32, 16, 1, (150, 175, 210, 220))
    c.rect(40, 32, 1, 16, (90, 110, 140, 180))

    # --- DETAIL (48,32,16,16): plates, lights, decals --------------------
    noise_rect(c, 48, 32, 16, 16, (60, 62, 70), spread=6, rng=rng)
    c.rect(49, 33, 6, 5, (240, 240, 236))              # blank number plate
    c.rect(50, 34, 4, 3, (60, 60, 66))
    c.rect(56, 33, 7, 3, (240, 236, 228))              # white sticker area
    c.rect(56, 37, 7, 2, secondary)
    c.rect(49, 41, 14, 1, light)
    c.rect(49, 42, 14, 1, dark)
    for y in range(44, 48):
        for x in range(48, 64):
            c.set(x, y, (34, 36, 42) if (x + y) % 2 == 0 else (46, 48, 56))
    if style == "f1":
        c.rect(48, 32, 16, 1, secondary)

    # --- WHEEL_SIDE (32,0,16,16) -----------------------------------------
    rubber = (28, 28, 32)
    c.rect(32, 0, 16, 16, rubber)
    c.disc(40, 8, 7.4, (36, 36, 40))
    c.disc(40, 8, 6.4, (22, 22, 26))
    rim_color = (206, 206, 212) if style != "kart" else (170, 120, 60)
    c.disc(40, 8, 4.2, rim_color)
    c.disc(40, 8, 2.2, shade(rim_color, -0.5))
    c.rect(40, 3, 1, 3, shade(rim_color, -0.3))
    c.rect(40, 10, 1, 3, shade(rim_color, -0.3))
    c.rect(35, 8, 3, 1, shade(rim_color, -0.3))
    c.rect(43, 8, 3, 1, shade(rim_color, -0.3))
    c.rect(39, 7, 2, 2, (40, 40, 46))
    for i in range(16):
        c.set(32 + i, 0, (18, 18, 22))
        c.set(32 + i, 15, (18, 18, 22))
        c.set(32, i, (18, 18, 22))
        c.set(47, i, (18, 18, 22))

    # --- WHEEL_TREAD (32,16,16,16) ---------------------------------------
    for y in range(16):
        for x in range(16):
            v = 18 if (x // 2) % 2 == 0 else 40
            c.set(32 + x, 16 + y, (v, v, v + 2))
    for i in range(16):
        c.set(32 + i, 16, (12, 12, 14))
        c.set(32 + i, 31, (12, 12, 14))

    # --- WHEEL_HUB (48,0,16,16) ------------------------------------------
    c.rect(48, 0, 16, 16, (30, 30, 34))
    c.disc(56, 8, 6.0, (150, 150, 156))
    c.disc(56, 8, 4.4, (188, 188, 196))
    c.disc(56, 8, 1.6, (60, 60, 66))
    for a in range(0, 360, 90):
        import math

        x = 56 + int(4.6 * math.cos(math.radians(a)))
        y = 8 + int(4.6 * math.sin(math.radians(a)))
        c.rect(x - 1, y - 1, 2, 2, (70, 70, 76))

    # --- EXTRA (48,16,16,16): spoilers, mirrors, exhausts ----------------
    c.rect(48, 16, 16, 16, dark)
    c.rect(48, 16, 16, 1, light)
    c.rect(49, 19, 14, 2, (52, 54, 60))                # spoiler blade
    c.rect(50, 21, 2, 4, (44, 46, 52))
    c.rect(60, 21, 2, 4, (44, 46, 52))
    c.rect(50, 27, 3, 2, (200, 200, 208))              # exhaust tip
    c.rect(57, 27, 3, 2, (200, 200, 208))
    c.rect(53, 27, 4, 1, (90, 90, 96))
    c.rect(51, 30, 2, 3, (60, 60, 68))                 # mirror stalks
    c.rect(60, 30, 2, 3, (60, 60, 68))

    # --- ICON (0,48,32,16): big badge / livery accent --------------------
    c.rect(0, 48, 32, 16, shade(primary, -0.25))
    for i in range(0, 32, 4):
        c.rect(i, 48, 2, 16, secondary if (i // 4) % 2 == 0 else shade(secondary, -0.3))
    c.rect(8, 51, 16, 10, (30, 30, 36))
    c.rect(9, 52, 14, 8, (240, 240, 236))
    c.rect(11, 54, 10, 4, shade(primary, -0.2))
    c.rect(12, 55, 3, 2, (30, 30, 36))
    c.rect(17, 55, 3, 2, (30, 30, 36))

    # --- TIRE_ICON (32,48,16,16) -----------------------------------------
    c.rect(32, 48, 16, 16, rubber)
    c.disc(40, 56, 7.2, (34, 34, 38))
    c.ring(40, 56, 6.6, (90, 90, 96), 1.4)
    c.disc(40, 56, 3.6, (170, 170, 178))
    c.disc(40, 56, 1.4, (70, 70, 76))
    return c


# ---------------------------------------------------------------------------
# item icons (16x16)
# ---------------------------------------------------------------------------
def item_car(color_a, color_b, style):
    """Side view car icon used by the car item + ghost/spawn items."""
    c = Canvas(16, 16)
    body = color_a
    dark = shade(body, -0.4)
    light = shade(body, 0.3)
    if style == "f1":
        c.rect(2, 9, 12, 3, body)                       # tub
        c.rect(1, 8, 4, 1, body)                        # front wing
        c.rect(11, 7, 4, 1, body)                       # rear wing
        c.rect(5, 6, 4, 3, body)                        # cockpit
        c.rect(6, 7, 3, 2, (30, 30, 36))
        c.rect(12, 9, 2, 2, (40, 40, 46))
        c.set(13, 8, (226, 60, 60))
    elif style == "kart":
        c.rect(2, 10, 11, 2, body)
        c.rect(4, 8, 5, 2, body)
        c.rect(5, 8, 3, 1, (30, 30, 36))
        c.rect(1, 12, 3, 1, (40, 40, 46))
        c.rect(11, 12, 3, 1, (40, 40, 46))
    elif style == "truck":
        c.rect(1, 7, 14, 6, body)
        c.rect(3, 5, 5, 3, body)
        c.rect(4, 6, 3, 2, (40, 46, 58))
        c.rect(10, 7, 5, 4, shade(body, -0.15))
        c.rect(1, 12, 14, 1, dark)
    else:
        c.rect(1, 8, 14, 5, body)
        c.rect(3, 5, 8, 3, body)
        c.rect(4, 6, 6, 2, (40, 46, 58))
        c.rect(1, 12, 14, 1, dark)
        if style in ("sport", "hyper", "drift", "hotrod"):
            c.rect(9, 4, 5, 1, dark)                    # spoiler
            c.rect(9, 5, 1, 1, dark)
        if style == "hyper":
            c.rect(1, 9, 3, 2, shade(body, -0.25))
    c.rect(1, 8, 14, 1, light)
    c.rect(2, 9, 3, 2, (250, 240, 200))                 # lights
    c.rect(12, 9, 2, 2, (230, 70, 70))
    c.rect(2, 13, 3, 2, (26, 26, 30))                   # wheels
    c.rect(11, 13, 3, 2, (26, 26, 30))
    c.rect(2, 13, 3, 1, (60, 60, 66))
    c.rect(11, 13, 3, 1, (60, 60, 66))
    c.rect(6, 11, 4, 1, color_b)
    return c


def item_carrot_key():
    c = Canvas(16, 16)
    gold = (238, 190, 68)
    dark_gold = (168, 126, 32)
    c.rect(2, 9, 8, 2, gold)                             # key shaft
    c.rect(3, 8, 6, 1, shade(gold, 0.25))
    c.rect(3, 11, 6, 1, dark_gold)
    c.rect(3, 12, 1, 1, gold)                            # teeth
    c.rect(5, 12, 1, 1, gold)
    c.ring(11, 5, 3.2, gold, 1.6)                        # bow of the key
    c.disc(12, 10, 3.4, (232, 122, 40))                  # carrot head
    c.disc(12, 10, 2.4, (244, 152, 60))
    c.rect(9, 7, 1, 2, (74, 160, 62))                    # leaves
    c.rect(10, 6, 1, 2, (86, 176, 70))
    c.rect(11, 6, 1, 2, (74, 160, 62))
    c.set(13, 11, (216, 104, 32))
    return c


def item_wrench():
    c = Canvas(16, 16)
    steel = (176, 178, 188)
    dark = (110, 112, 122)
    c.line(4, 12, 10, 5, steel)
    c.line(5, 12, 11, 5, dark)
    c.line(4, 13, 10, 6, dark)
    c.rect(9, 2, 4, 4, steel)
    c.rect(10, 3, 2, 2, (0, 0, 0, 0))
    c.rect(2, 11, 4, 4, steel)
    c.rect(3, 12, 2, 2, (0, 0, 0, 0))
    c.rect(4, 13, 2, 2, steel)
    return c


def item_track_wand():
    c = Canvas(16, 16)
    c.line(3, 13, 11, 3, (120, 84, 52))
    c.line(4, 13, 12, 3, (150, 108, 66))
    c.disc(12, 3, 3.0, (86, 220, 240))
    c.disc(12, 3, 1.8, (200, 250, 255))
    c.set(9, 6, (150, 245, 255))
    c.set(14, 0, (150, 245, 255))
    return c


def item_stopwatch():
    c = Canvas(16, 16)
    c.disc(8, 9, 6.2, (198, 160, 64))
    c.disc(8, 9, 4.8, (236, 236, 240))
    c.vline(8, 5, 9, (40, 40, 46))
    c.line(8, 9, 11, 7, (206, 62, 62))
    c.rect(7, 1, 3, 2, (198, 160, 64))
    c.rect(11, 3, 2, 2, (150, 150, 158))
    return c


def item_paint_can(color):
    c = Canvas(16, 16)
    metal = (156, 158, 166)
    c.rect(3, 4, 10, 10, metal)
    c.rect(3, 4, 10, 1, shade(metal, 0.3))
    c.rect(3, 13, 10, 1, shade(metal, -0.35))
    c.rect(4, 6, 8, 6, color)                            # paint band
    c.rect(4, 6, 8, 1, shade(color, 0.3))
    c.rect(2, 3, 12, 2, (120, 122, 130))                 # lid
    c.rect(6, 1, 4, 2, (90, 92, 100))                    # handle
    for x in range(4, 12, 2):
        c.set(x, 12, shade(color, -0.3))
    return c


def item_trophy(metal, label_colors):
    c = Canvas(16, 16)
    c.rect(5, 12, 6, 2, shade(metal, -0.25))
    c.rect(6, 9, 4, 3, metal)
    c.rect(3, 2, 10, 7, metal)
    c.rect(4, 3, 8, 5, shade(metal, 0.2))
    c.rect(4, 3, 1, 5, shade(metal, -0.2))
    c.rect(12, 3, 1, 5, shade(metal, -0.2))
    c.rect(3, 4, 2, 3, (0, 0, 0, 0))
    c.rect(11, 4, 2, 3, (0, 0, 0, 0))
    c.rect(6, 5, 4, 2, label_colors)
    return c


def item_generic_part(name, c):
    if name == "engine_turbo":
        c.disc(8, 9, 5.0, (188, 190, 198))
        c.disc(8, 9, 3.2, (120, 122, 130))
        c.rect(2, 3, 4, 3, (150, 152, 160))
        c.rect(11, 2, 3, 4, (110, 112, 120))
        c.disc(8, 9, 1.4, (60, 62, 70))
        c.ring(8, 9, 4.4, (240, 120, 60), 1.0)
    elif name == "engine_v8":
        c.rect(1, 5, 14, 7, (86, 88, 96))
        c.rect(1, 5, 14, 1, (140, 142, 150))
        for x in range(2, 14, 3):
            c.rect(x, 2, 2, 3, (150, 152, 160))
            c.rect(x, 12, 2, 2, (60, 62, 70))
        c.rect(6, 7, 4, 3, (200, 160, 60))
    elif name == "gearbox_dct":
        c.rect(2, 6, 12, 6, (96, 98, 106))
        c.rect(2, 6, 12, 1, (150, 152, 160))
        c.disc(11, 9, 3.0, (150, 152, 160))
        c.disc(11, 9, 1.2, (70, 72, 80))
        c.rect(3, 3, 3, 3, (200, 160, 60))
        c.rect(7, 3, 3, 3, (200, 160, 60))
    elif name == "gearbox_sequential":
        c.rect(2, 7, 12, 5, (70, 72, 80))
        c.rect(2, 7, 12, 1, (130, 132, 140))
        c.rect(7, 3, 2, 5, (176, 178, 188))
        c.disc(7, 3, 2.2, (210, 60, 60))
        for x in range(3, 12, 2):
            c.rect(x, 9, 1, 2, (150, 152, 160))
    elif name == "brakes_carbon":
        c.disc(8, 8, 6.0, (60, 62, 70))
        c.disc(8, 8, 4.6, (34, 34, 40))
        c.ring(8, 8, 5.4, (120, 122, 130), 1.0)
        c.rect(2, 10, 4, 4, (200, 120, 40))
        c.rect(3, 11, 2, 2, (240, 160, 60))
    elif name.startswith("tires_"):
        compound = {"tires_soft": (206, 62, 62), "tires_medium": (226, 190, 70), "tires_hard": (232, 232, 232), "tires_street": (120, 160, 220)}[name]
        c.rect(1, 3, 14, 10, (32, 32, 36))
        c.rect(1, 5, 14, 2, (56, 56, 60))
        c.rect(1, 10, 14, 2, (56, 56, 60))
        c.rect(4, 1, 8, 14, (32, 32, 36))
        c.rect(5, 2, 6, 12, (44, 44, 48))
        c.rect(5, 5, 6, 6, compound)
        c.rect(6, 6, 4, 4, shade(compound, -0.25))
    elif name == "aero_wing":
        c.rect(1, 4, 14, 3, (56, 58, 66))
        c.rect(1, 4, 14, 1, (110, 112, 120))
        c.rect(3, 7, 2, 6, (70, 72, 80))
        c.rect(11, 7, 2, 6, (70, 72, 80))
        c.rect(2, 13, 12, 2, (40, 42, 48))
        c.rect(2, 9, 12, 2, (90, 92, 100))
    elif name == "ground_effect":
        c.rect(1, 9, 14, 4, (60, 62, 70))
        c.rect(1, 9, 14, 1, (120, 122, 130))
        for x in range(2, 14, 3):
            c.rect(x, 10, 1, 3, (36, 36, 42))
        c.rect(6, 4, 4, 5, (86, 88, 96))
        c.rect(6, 4, 4, 1, (140, 142, 150))
    elif name == "roll_cage":
        c.line(3, 14, 8, 3, (170, 172, 180))
        c.line(12, 14, 8, 3, (170, 172, 180))
        c.line(8, 3, 8, 14, (140, 142, 150))
        c.rect(3, 13, 10, 2, (120, 122, 130))
        c.rect(6, 2, 5, 2, (200, 60, 60))
    elif name == "chassis_plate":
        c.rect(2, 5, 12, 7, (140, 142, 150))
        c.rect(2, 5, 12, 1, (190, 192, 200))
        for x in range(3, 13, 2):
            c.set(x, 7, (90, 92, 100))
            c.set(x, 9, (90, 92, 100))
        c.rect(6, 3, 4, 2, (100, 102, 110))
        c.rect(6, 12, 4, 2, (100, 102, 110))
    elif name == "boost_nitro":
        c.rect(4, 3, 8, 12, (72, 74, 82))
        c.rect(5, 4, 6, 10, (46, 48, 56))
        c.rect(5, 8, 6, 4, (90, 200, 240))
        c.rect(6, 9, 4, 2, (180, 240, 255))
        c.rect(6, 1, 4, 2, (170, 172, 180))
        c.rect(7, 0, 2, 1, (200, 202, 210))
    elif name == "carbon_weave":
        for y in range(16):
            for x in range(16):
                d = ((x // 2) + (y // 2)) % 2
                v = 34 if d == 0 else 52
                c.set(x, y, (v, v, v + 2))
        c.rect(0, 0, 16, 1, (90, 90, 96))
        c.rect(0, 15, 16, 1, (90, 90, 96))
    elif name == "race_guide":
        c.rect(2, 2, 12, 13, (150, 60, 52))
        c.rect(2, 2, 12, 1, (190, 90, 80))
        c.rect(3, 3, 10, 11, (236, 228, 210))
        c.rect(5, 5, 6, 1, (90, 90, 96))
        c.rect(5, 7, 6, 1, (140, 140, 146))
        c.rect(5, 9, 6, 1, (140, 140, 146))
        c.rect(5, 11, 4, 1, (140, 140, 146))
        c.rect(1, 2, 2, 13, (120, 46, 40))
    return c


# ---------------------------------------------------------------------------
# blocks
# ---------------------------------------------------------------------------
def tex_asphalt():
    c = Canvas(16, 16)
    rng = random.Random(7)
    noise_rect(c, 0, 0, 16, 16, (52, 52, 56), spread=9, rng=rng, speckle=0.10,
               speckle_color=(74, 74, 80))
    for _ in range(26):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        c.rect(x, y, rng.choice((1, 1, 2)), 1, (86, 86, 92))
    for _ in range(10):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        c.set(x, y, (34, 34, 38))
    return c


def tex_asphalt_cracked():
    c = tex_asphalt()
    rng = random.Random(21)
    for _ in range(3):
        x, y = rng.randint(0, 12), rng.randint(0, 12)
        c.line(x, y, x + rng.randint(2, 5), y + rng.randint(-3, 4), (30, 30, 34))
    return c


def tex_road_block():
    c = tex_asphalt()
    for y in range(16):
        if y % 4 < 2:
            c.rect(7, y, 2, 1, (216, 210, 190))
            c.rect(7, y, 1, 1, (240, 236, 220))
    return c


def tex_safety_barrier():
    c = Canvas(16, 16)
    for x in range(16):
        color = (222, 66, 58) if (x // 4) % 2 == 0 else (238, 238, 234)
        for y in range(16):
            v = 0 if y % 5 else -18
            c.set(x, y, shade(color, v / 100.0))
    c.rect(0, 0, 16, 1, (250, 250, 246))
    c.rect(0, 15, 16, 1, (150, 40, 36))
    return c


def tex_start_gate():
    """Animated: 4 frames of a checkered gantry with a pulsing light bar."""
    frames = 4
    c = Canvas(16, 16 * frames)
    for f in range(frames):
        off = f * 16
        for y in range(16):
            for x in range(16):
                on = ((x // 2) + (y // 2)) % 2 == 0
                c.set(x, off + y, (34, 34, 38) if on else (226, 224, 216))
        pulse = (f + 0.5) / frames
        bar = (int(120 + 135 * pulse), int(200 - 40 * pulse), int(60 + 40 * pulse))
        c.rect(0, off + 6, 16, 4, bar)
        c.rect(0, off + 6, 16, 1, shade(bar, 0.45))
        c.rect(0, off + 9, 16, 1, shade(bar, -0.4))
        for x in range(0, 16, 4):
            c.set(x + (f % 2), off + 7, (255, 255, 230))
            c.set(x + (f % 2), off + 8, (255, 255, 230))
    return c


def tex_checkpoint_marker():
    c = Canvas(16, 16, (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = abs(x - 8)
            a = 0
            if d <= 1:
                a = 200
            elif d <= 3:
                a = 120
            if a:
                c.set(x, y, (86, 232, 246, a))
    rng = random.Random(5)
    for _ in range(24):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        if c.get(x, y)[3]:
            c.set(x, y, (200, 252, 255, 220))
    return c


def tex_pit_lane():
    c = Canvas(16, 16)
    rng = random.Random(11)
    noise_rect(c, 0, 0, 16, 16, (150, 148, 142), spread=7, rng=rng, speckle=0.08,
               speckle_color=(120, 118, 112))
    c.rect(0, 3, 16, 1, (86, 84, 80))
    c.rect(0, 12, 16, 1, (86, 84, 80))
    c.rect(0, 0, 16, 1, (186, 184, 178))
    return c


def tex_fake_ice():
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = 190 + ((x * 7 + y * 13) % 24)
            c.set(x, y, (v - 40, v - 12, 255, 190))
    for i in range(0, 16, 5):
        c.line(i, 0, i + 5, 15, (232, 248, 255, 220))
    return c


def tex_fake_slime():
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = (x * 3 + y * 5) % 16
            c.set(x, y, (110 + v, 220 - v, 120 + v // 2, 170))
    c.rect(0, 0, 16, 1, (190, 255, 190, 200))
    return c


def tex_kerb(color):
    c = Canvas(16, 16)
    base = (204, 58, 52) if color == "red" else (232, 228, 218)
    for y in range(16):
        for x in range(16):
            v = ((x + y) % 4) - 2
            c.set(x, y, shade(base, v / 24.0))
    c.rect(0, 0, 16, 2, shade(base, 0.3))
    c.rect(0, 14, 16, 2, shade(base, -0.35))
    return c


def tex_finish_line():
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            on = ((x // 4) + (y // 4)) % 2 == 0
            base = (240, 238, 230) if on else (38, 38, 44)
            v = ((x * 3 + y * 5) % 5) - 2
            c.set(x, y, shade(base, v / 30.0))
    return c


def tex_station():
    c = Canvas(16, 16)
    noise_rect(c, 0, 0, 16, 16, (58, 60, 70), spread=6, rng=random.Random(3))
    c.rect(1, 1, 14, 14, (32, 34, 42))
    c.rect(2, 2, 12, 12, (44, 46, 58))
    # big "P"
    for y in range(3, 13):
        c.set(5, y, (238, 236, 228))
        c.set(6, y, (238, 236, 228) if y > 7 else (238, 236, 228))
    for x in range(5, 10):
        c.set(x, 3, (238, 236, 228))
        c.set(x, 7, (238, 236, 228))
    for y in range(3, 8):
        c.set(9, y, (238, 236, 228))
    for y in range(3, 13):
        c.set(11, y, (250, 210, 70) if y % 2 == 0 else (200, 160, 40))
    c.rect(5, 3, 5, 1, (255, 255, 245))
    return c


def tex_boost_pad():
    """Animated 4 frames of scrolling chevrons."""
    frames = 4
    c = Canvas(16, 16 * frames)
    for f in range(frames):
        off = f * 16
        for y in range(16):
            for x in range(16):
                phase = (y + f * 4) % 16
                band = phase < 8
                if band:
                    v = 200 - phase * 6
                    c.set(x, off + y, (60, 200, 240, max(120, v)))
                else:
                    c.set(x, off + y, (18, 20, 30, 200))
        for i in range(4):
            top = off + ((f * 4 + i * 8) % 16)
            for x in range(16):
                for dy in range(0, 5):
                    y = top + dy
                    if y >= off + 16:
                        continue
                    d = abs(x - 8)
                    if abs(dy - 2) <= (2 - min(2, d // 3)):
                        c.set(x, y, (250, 250, 255))
    return c


def tex_mod_icon():
    c = Canvas(64, 64)
    c.rect(0, 0, 64, 64, (26, 28, 34))
    for y in range(64):
        for x in range(64):
            if (x + y) % 16 < 8 and y > 40:
                c.set(x, y, (40, 42, 50))
    # checkered flag top-left
    for by in range(0, 24, 4):
        for bx in range(0, 24, 4):
            if ((bx + by) // 4) % 2 == 0:
                c.rect(bx, by, 4, 4, (238, 236, 228))
    c.rect(22, 2, 2, 22, (150, 150, 156))
    # car silhouette
    body = (222, 62, 52)
    c.rect(14, 34, 38, 12, body)
    c.rect(14, 34, 38, 3, shade(body, 0.3))
    c.rect(22, 26, 20, 9, body)
    c.rect(24, 28, 16, 5, (40, 48, 62))
    c.rect(10, 30, 34, 2, shade(body, -0.3))
    c.disc(22, 48, 6, (30, 30, 34))
    c.disc(22, 48, 3, (190, 190, 198))
    c.disc(45, 48, 6, (30, 30, 34))
    c.disc(45, 48, 3, (190, 190, 198))
    c.rect(46, 36, 6, 3, (250, 240, 200))
    c.rect(12, 38, 4, 4, (250, 240, 200))
    return c


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------
# ---------------------------------------------------------------------------
# extras: race pass, boost flame, the secret car
# ---------------------------------------------------------------------------
def item_race_pass():
    """A race entry pass: lanyard + ticket with a chequered edge."""
    c = Canvas(16, 16, (0, 0, 0, 0))
    # lanyard
    c.line(5, 1, 5, 5, (70, 74, 88, 255))
    c.line(10, 1, 10, 5, (70, 74, 88, 255))
    # ticket
    c.rect(2, 5, 12, 8, (238, 238, 242))
    c.border(2, 5, 12, 8, (150, 150, 158))
    c.rect(3, 6, 10, 2, (200, 44, 40))
    for x in range(3, 13, 2):
        c.set(x, 9, (40, 40, 46))
        c.set(x + 1, 9, (232, 232, 236))
    c.rect(3, 11, 10, 1, (150, 150, 158))
    return c


def tex_flame():
    """Boost flame strip: 16x16 of layered orange/yellow with an alpha edge."""
    c = Canvas(16, 16, (0, 0, 0, 0))
    rng = random.Random(77)
    for x in range(16):
        for y in range(16):
            dx = x / 15.0
            dy = abs(y - 7.5) / 7.5
            if dx + dy * 1.15 > 1.15:
                continue
            heat = max(0.0, 1.0 - (dx * 0.75 + dy * 0.9)) + rng.uniform(-0.06, 0.06)
            if heat > 0.72:
                color = (255, 246, 200, 235)
            elif heat > 0.45:
                color = (255, 196, 82, 225)
            elif heat > 0.22:
                color = (240, 122, 32, 205)
            else:
                color = (188, 62, 22, 170)
            c.set(x, y, color)
    return c


def main():
    random.seed(404)
    tex = lambda *p: os.path.join(TEXTURES, *p)

    # --- blocks ----------------------------------------------------------
    asphalt = tex_asphalt()
    asphalt.save(tex("block", "asphalt.png"))
    tex_asphalt_cracked().save(tex("block", "asphalt_cracked.png"))
    tex_road_block().save(tex("block", "road_block.png"))
    tex_safety_barrier().save(tex("block", "safety_barrier.png"))
    tex_pit_lane().save(tex("block", "pit_lane.png"))
    tex_fake_ice().save(tex("block", "fake_ice.png"))
    tex_fake_slime().save(tex("block", "fake_slime.png"))
    tex_start_gate().save(
        tex("block", "start_gate.png"),
        mcmeta={"animation": {"frametime": 4, "interpolate": True}},
    )
    tex_checkpoint_marker().save(tex("block", "checkpoint_marker.png"))
    tex_kerb("red").save(tex("block", "kerb_red.png"))
    tex_kerb("white").save(tex("block", "kerb_white.png"))
    tex_finish_line().save(tex("block", "finish_line.png"))
    tex_station().save(tex("block", "station.png"))
    tex_boost_pad().save(
        tex("block", "boost_pad.png"),
        mcmeta={"animation": {"frametime": 2, "interpolate": True}},
    )

    # --- cars (entity skins) --------------------------------------------
    for name, (primary, secondary) in CAR_COLORS.items():
        car_skin(name, primary, secondary, name).save(tex("entity", "car", "%s.png" % name))
    car_skin("wraith", (38, 36, 56), (128, 56, 200), "hyper").save(
        tex("entity", "car", "wraith.png")
    )
    car_skin("phantom", (208, 210, 216), (70, 70, 80), "sport").save(
        tex("entity", "car", "phantom.png")
    )
    car_skin("shade", (44, 66, 58), (90, 200, 160), "f1").save(
        tex("entity", "car", "shade.png")
    )
    # the secret car: near black with violet trim
    car_skin("shadow", (26, 26, 34), (138, 62, 190), "hyper").save(
        tex("entity", "car", "shadow.png")
    )
    tex_flame().save(tex("entity", "car", "flame.png"))
    # white "skin template" so players can hand-paint their own liveries
    car_skin("template", (232, 232, 236), (170, 172, 180), "sport").save(
        tex("entity", "car", "template.png")
    )

    # --- items -----------------------------------------------------------
    item_carrot_key().save(tex("item", "carrot_key.png"))
    for name, (primary, secondary) in CAR_COLORS.items():
        item_car(primary, secondary, name).save(tex("item", "car_%s.png" % name))
    item_car(*CAR_COLORS["hyper"], "hyper").save(tex("item", "car_shadow.png"))
    item_wrench().save(tex("item", "wrench.png"))
    item_track_wand().save(tex("item", "track_wand.png"))
    item_race_pass().save(tex("item", "race_pass.png"))
    item_stopwatch().save(tex("item", "stopwatch.png"))
    for color_name, rgb in PAINTS.items():
        item_paint_can(rgb).save(tex("item", "paint_%s.png" % color_name))
    item_trophy((238, 200, 72), (150, 110, 20)).save(tex("item", "trophy_gold.png"))
    item_trophy((206, 210, 218), (110, 114, 124)).save(tex("item", "trophy_silver.png"))
    item_trophy((196, 130, 78), (110, 66, 34)).save(tex("item", "trophy_bronze.png"))
    for part in (
        "engine_turbo", "engine_v8", "gearbox_dct", "gearbox_sequential",
        "brakes_carbon", "tires_soft", "tires_medium", "tires_hard", "tires_street",
        "aero_wing", "ground_effect", "roll_cage", "chassis_plate", "boost_nitro",
        "carbon_weave", "race_guide",
    ):
        item_generic_part(part, Canvas(16, 16)).save(tex("item", "%s.png" % part))

    # --- mod icon --------------------------------------------------------
    icon_dir = os.path.join(ROOT, "src", "main", "resources")
    tex_mod_icon().save(os.path.join(icon_dir, "icon.png"))

    # --- contact sheet for human review (4x nearest neighbour) -----------
    review = Canvas(16 * 4 * 10, 16 * 4 * 6)
    tiles = [
        "item/carrot_key.png", "item/car_kart.png", "item/car_f1.png", "item/car_sport.png",
        "item/car_hyper.png", "item/car_rally.png", "item/car_drift.png", "item/car_hotrod.png",
        "item/car_truck.png", "item/car_shadow.png",
        "item/wrench.png", "item/track_wand.png", "item/stopwatch.png", "item/paint_red.png",
        "item/paint_blue.png", "item/paint_green.png", "item/trophy_gold.png", "item/trophy_silver.png",
        "item/trophy_bronze.png", "item/boost_nitro.png",
        "item/engine_turbo.png", "item/engine_v8.png", "item/gearbox_dct.png",
        "item/gearbox_sequential.png", "item/brakes_carbon.png", "item/tires_soft.png",
        "item/tires_medium.png", "item/tires_hard.png", "item/aero_wing.png", "item/roll_cage.png",
        "block/asphalt.png", "block/road_block.png", "block/safety_barrier.png",
        "block/pit_lane.png", "block/fake_ice.png", "block/fake_slime.png",
        "block/checkpoint_marker.png", "entity/car/sport.png", "entity/car/f1.png",
            "entity/car/hyper.png",
        "entity/car/kart.png", "entity/car/rally.png", "entity/car/wraith.png",
        "entity/car/template.png", "item/carbon_weave.png", "item/race_guide.png",
        "item/tires_street.png", "item/chassis_plate.png", "item/ground_effect.png",
        "item/paint_yellow.png",
    ]
    for i, rel in enumerate(tiles):
        path = os.path.join(TEXTURES, rel)
        if not os.path.exists(path):
            continue
        img = read_png(path)
        review.blit(img.scale(4) if max(img.w, img.h) <= 16 else img.scale(1),
                    (i % 10) * 64, (i // 10) * 64)
    review.save(os.path.join(ROOT, "tools", "preview_items.png"))

    count = sum(len(f) for _, _, f in os.walk(TEXTURES))
    print("generated %d texture files under %s" % (count, TEXTURES))


def read_png(path):
    """Minimal PNG reader (RGBA, filter types 0-4) used for previews."""
    with open(path, "rb") as fh:
        data = fh.read()
    pos = 8
    w = h = 0
    idat = b""
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            w, h = struct.unpack(">II", chunk[:8])
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
    raw = zlib.decompress(idat)
    canvas = Canvas(w, h)
    stride = w * 4
    prev = bytearray(stride)
    off = 0
    for y in range(h):
        ftype = raw[off]
        off += 1
        line = bytearray(raw[off:off + stride])
        off += stride
        if ftype == 1:
            for i in range(4, stride):
                line[i] = (line[i] + line[i - 4]) & 255
        elif ftype == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif ftype == 3:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif ftype == 4:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                b = prev[i]
                c = prev[i - 4] if i >= 4 else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        for x in range(w):
            canvas.set(x, y, tuple(line[x * 4:x * 4 + 4]))
        prev = line
    return canvas


if __name__ == "__main__":
    main()

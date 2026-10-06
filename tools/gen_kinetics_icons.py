#!/usr/bin/env python3
"""
Generates the Null Kinetics HUD icon textures (16x16 RGBA PNGs) using a tiny
dependency-free rasterizer: shapes are drawn into an 8x supersampled float
coverage buffer and box-filtered down to the final resolution for clean
anti-aliasing.

Output: src/main/resources/assets/myfirstmod/textures/gui/kinetics/*.png
"""
import math
import struct
import zlib
import os

SIZE = 16          # final texture size
SS = 8             # supersample factor
N = SIZE * SS      # working buffer size (128)

# ---------------------------------------------------------------- PNG writer

def write_png(path, size, pixels):
    """pixels: list of rows, each a list of (r, g, b, a) tuples."""
    def chunk(tag, data):
        raw = tag + data
        return struct.pack(">I", len(data)) + raw + struct.pack(">I", zlib.crc32(raw) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    scan = b""
    for row in pixels:
        scan += b"\x00" + b"".join(struct.pack("BBBB", *px) for px in row)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", zlib.compress(scan, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)

# ---------------------------------------------------------------- rasterizer

class Canvas:
    def __init__(self):
        self.buf = [[0.0] * N for _ in range(N)]  # coverage in [0,1]

    def add_coverage(self, x, y, v):
        if 0 <= x < N and 0 <= y < N:
            self.buf[y][x] = min(1.0, self.buf[y][x] + v)

    def line(self, x1, y1, x2, y2, width):
        """Thick anti-aliased line segment; coordinates in 16-unit space."""
        s = SS
        x1, y1, x2, y2, w = x1 * s, y1 * s, x2 * s, y2 * s, width * s
        r = w / 2 + 1.5
        minx = max(0, int(min(x1, x2) - r))
        maxx = min(N - 1, int(max(x1, x2) + r))
        miny = max(0, int(min(y1, y2) - r))
        maxy = min(N - 1, int(max(y1, y2) + r))
        dx, dy = x2 - x1, y2 - y1
        len2 = dx * dx + dy * dy
        for py in range(miny, maxy + 1):
            for px in range(minx, maxx + 1):
                if len2 == 0:
                    d = math.hypot(px + 0.5 - x1, py + 0.5 - y1)
                else:
                    t = ((px + 0.5 - x1) * dx + (py + 0.5 - y1) * dy) / len2
                    t = max(0.0, min(1.0, t))
                    d = math.hypot(px + 0.5 - (x1 + t * dx), py + 0.5 - (y1 + t * dy))
                half = w / 2
                if d < half:
                    self.add_coverage(px, py, 1.0)
                elif d < half + 1.0:
                    self.add_coverage(px, py, half + 1.0 - d)

    def circle(self, cx, cy, radius, ring=False, width=1.2):
        """Filled circle, or ring (annulus) when ring=True."""
        s = SS
        cx, cy, rad = cx * s, cy * s, radius * s
        for py in range(N):
            for px in range(N):
                d = math.hypot(px + 0.5 - cx, py + 0.5 - cy)
                if ring:
                    half = width * s / 2
                    if abs(d - rad) < half:
                        self.add_coverage(px, py, 1.0)
                    elif abs(d - rad) < half + 1.0:
                        self.add_coverage(px, py, half + 1.0 - abs(d - rad))
                else:
                    if d < rad:
                        self.add_coverage(px, py, 1.0)
                    elif d < rad + 1.0:
                        self.add_coverage(px, py, rad + 1.0 - d)

    def arc(self, cx, cy, radius, a0, a1, width=1.4):
        """Arc from angle a0 to a1 (degrees, clockwise from +x axis, y down)."""
        s = SS
        cx, cy, rad = cx * s, cy * s, radius * s
        for py in range(N):
            for px in range(N):
                dx, dy = px + 0.5 - cx, py + 0.5 - cy
                d = math.hypot(dx, dy)
                half = width * s / 2
                if abs(d - rad) >= half + 1.0:
                    continue
                ang = math.degrees(math.atan2(dy, dx)) % 360.0
                # angular span check
                span = (a1 - a0) % 360.0
                off = (ang - a0) % 360.0
                if off > span:
                    continue
                if abs(d - rad) < half:
                    self.add_coverage(px, py, 1.0)
                elif abs(d - rad) < half + 1.0:
                    self.add_coverage(px, py, half + 1.0 - abs(d - rad))

    def triangle(self, p1, p2, p3):
        """Filled triangle via barycentric test with 1px AA edge."""
        (x1, y1), (x2, y2), (x3, y3) = p1, p2, p3
        s = SS
        x1, y1 = x1 * s, y1 * s
        x2, y2 = x2 * s, y2 * s
        x3, y3 = x3 * s, y3 * s
        minx = max(0, int(min(x1, x2, x3) - 1))
        maxx = min(N - 1, int(max(x1, x2, x3) + 1))
        miny = max(0, int(min(y1, y2, y3) - 1))
        maxy = min(N - 1, int(max(y1, y2, y3) + 1))
        den = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        if den == 0:
            return
        for py in range(miny, maxy + 1):
            for px in range(minx, maxx + 1):
                fx, fy = px + 0.5, py + 0.5
                l1 = ((y2 - y3) * (fx - x3) + (x3 - x2) * (fy - y3)) / den
                l2 = ((y3 - y1) * (fx - x3) + (x1 - x3) * (fy - y3)) / den
                l3 = 1.0 - l1 - l2
                m = min(l1, l2, l3)
                if m >= 0:
                    self.add_coverage(px, py, 1.0)
                elif m > -0.06:
                    self.add_coverage(px, py, 1.0 + m / 0.06)

    def downsample(self, color, glow=0.0):
        """Box-filter the coverage buffer to SIZE x SIZE and colorize."""
        r, g, b = color
        gr, gg, gb = (16, 42, 39)  # dark teal shadow
        rows = []
        for ty in range(SIZE):
            row = []
            for tx in range(SIZE):
                cov = 0.0
                for sy in range(SS):
                    for sx in range(SS):
                        cov += self.buf[ty * SS + sy][tx * SS + sx]
                cov /= SS * SS
                cov = min(1.0, cov)
                # Slight dark-teal tint at partial coverage edges
                if cov > 0:
                    if glow:
                        cr = int(round(gr * (1 - cov) + r * cov))
                        cg = int(round(gg * (1 - cov) + g * cov))
                        cb = int(round(gb * (1 - cov) + b * cov))
                    else:
                        cr, cg, cb = r, g, b
                    row.append((min(255, cr), min(255, cg), min(255, cb), int(round(cov * 255))))
                else:
                    row.append((0, 0, 0, 0))
            rows.append(row)
        return rows

# ---------------------------------------------------------------- palettes

LIGHT = (0x9F, 0xF7, 0xEC)   # bright cyan
TEAL = (0x3F, 0xD9, 0xC6)    # primary teal
DEEP = (0x1E, 0x8F, 0x84)    # deeper teal

# ---------------------------------------------------------------- icons

def icon_dash():
    c = Canvas()
    # Two forward chevrons: "> >"
    c.line(2.5, 3.0, 8.0, 8.0, 2.2)
    c.line(8.0, 8.0, 2.5, 13.0, 2.2)
    c.line(8.0, 3.0, 13.5, 8.0, 2.2)
    c.line(13.5, 8.0, 8.0, 13.0, 2.2)
    # speed streaks behind
    c.line(1.5, 6.0, 4.0, 6.0, 1.0)
    c.line(0.5, 8.0, 3.0, 8.0, 1.0)
    c.line(1.5, 10.0, 4.0, 10.0, 1.0)
    return c.downsample(TEAL, glow=True)

def icon_jump():
    c = Canvas()
    # Upward arrow
    c.line(8.0, 14.5, 8.0, 5.0, 2.2)              # shaft
    c.triangle((8.0, 1.5), (4.2, 6.8), (11.8, 6.8))  # head
    # Double chevrons beneath (multi-jump)
    c.line(3.5, 10.0, 5.5, 8.0, 1.2)
    c.line(5.5, 8.0, 7.5, 10.0, 1.2)
    c.line(8.5, 10.0, 10.5, 8.0, 1.2)
    c.line(10.5, 8.0, 12.5, 10.0, 1.2)
    c.line(3.5, 13.5, 5.5, 11.5, 1.2)
    c.line(5.5, 11.5, 7.5, 13.5, 1.2)
    c.line(8.5, 13.5, 10.5, 11.5, 1.2)
    c.line(10.5, 11.5, 12.5, 13.5, 1.2)
    return c.downsample(TEAL, glow=True)

def icon_wallrun():
    c = Canvas()
    # Wall: thick vertical slab on the left with brick joints
    c.line(3.0, 1.5, 3.0, 14.5, 3.4)
    c.line(1.6, 5.5, 4.4, 5.5, 0.7)
    c.line(1.6, 10.5, 4.4, 10.5, 0.7)
    # Runner: diagonal body + head
    c.line(6.5, 6.0, 10.0, 10.0, 1.8)
    c.circle(11.3, 11.2, 1.5)
    c.line(10.0, 10.0, 12.5, 12.5, 1.4)   # leading leg
    # Motion streaks trailing
    c.line(7.0, 3.5, 10.5, 3.5, 1.0)
    c.line(8.5, 1.8, 12.5, 1.8, 0.9)
    return c.downsample(TEAL, glow=True)

def icon_glide():
    c = Canvas()
    # Swept wings forming a shallow 'V' glide silhouette
    c.line(1.5, 4.5, 8.0, 8.5, 1.8)
    c.line(8.0, 8.5, 14.5, 4.5, 1.8)
    # Wing tips flare
    c.line(1.5, 4.5, 2.8, 6.8, 1.4)
    c.line(14.5, 4.5, 13.2, 6.8, 1.4)
    # Body diamond
    c.line(8.0, 7.0, 9.6, 9.0, 1.3)
    c.line(9.6, 9.0, 8.0, 11.0, 1.3)
    c.line(8.0, 11.0, 6.4, 9.0, 1.3)
    c.line(6.4, 9.0, 8.0, 7.0, 1.3)
    # Air current dashes below
    c.line(4.5, 13.0, 8.0, 13.0, 0.9)
    c.line(9.5, 14.0, 12.5, 14.0, 0.9)
    return c.downsample(TEAL, glow=True)

def icon_grapple():
    c = Canvas()
    # Rope from top
    c.line(11.0, 0.5, 11.0, 3.5, 1.2)
    # Hook: shaft then J-curve
    c.line(11.0, 3.0, 11.0, 8.5, 2.0)
    c.arc(8.0, 8.5, 3.0, 90, 270, 2.0)          # left-facing curve
    c.line(5.0, 8.5, 5.0, 6.5, 2.0)             # hook tip rising
    c.triangle((5.0, 4.2), (3.4, 7.0), (6.6, 7.0))  # barbed tip
    # Anchor sparkle
    c.line(13.0, 5.5, 13.0, 11.5, 0.9)
    return c.downsample(TEAL, glow=True)

def icon_slide():
    c = Canvas()
    # Low sliding figure: head + tucked body
    c.circle(12.2, 5.2, 1.7)
    c.line(4.0, 8.5, 10.8, 6.2, 2.2)   # body
    c.line(6.5, 8.0, 4.0, 10.5, 1.7)   # front leg bent
    c.line(10.8, 6.2, 8.5, 10.5, 1.5)  # trailing leg
    # Ground + speed streaks behind
    c.line(1.0, 12.5, 15.0, 12.5, 1.2)
    c.line(1.0, 3.5, 4.0, 3.5, 1.0)
    c.line(0.5, 6.0, 3.0, 6.0, 0.9)
    c.line(1.0, 8.8, 3.0, 8.8, 0.9)
    return c.downsample(TEAL, glow=True)

# ---------------------------------------------------------------- main

def main():
    out_dir = os.path.join(os.path.dirname(__file__),
                           "..", "src", "main", "resources", "assets",
                           "myfirstmod", "textures", "gui", "kinetics")
    out_dir = os.path.normpath(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    icons = {
        "dash": icon_dash,
        "jump": icon_jump,
        "wallrun": icon_wallrun,
        "glide": icon_glide,
        "grapple": icon_grapple,
        "slide": icon_slide,
    }
    for name, fn in icons.items():
        pixels = fn()
        path = os.path.join(out_dir, name + ".png")
        write_png(path, SIZE, pixels)
        print("wrote", path)

    # Preview sheet (4x scale, all icons in a row) for visual inspection.
    names = list(icons)
    scale = 4
    sheet_w = SIZE * len(names) * scale + (len(names) + 1) * scale
    sheet_h = SIZE * scale + 2 * scale
    sheet = [[(20, 26, 34, 255)] * sheet_w for _ in range(sheet_h)]
    for i, name in enumerate(names):
        px = icons[name]()
        ox = scale + i * (SIZE * scale + scale)
        for y in range(SIZE):
            for x in range(SIZE):
                r, g, b, a = px[y][x]
                for sy in range(scale):
                    for sx in range(scale):
                        yy = scale + y * scale + sy
                        xx = ox + x * scale + sx
                        if 0 <= yy < sheet_h and 0 <= xx < sheet_w:
                            base = sheet[yy][xx]
                            alpha = a / 255.0
                            sheet[yy][xx] = (
                                int(base[0] * (1 - alpha) + r * alpha),
                                int(base[1] * (1 - alpha) + g * alpha),
                                int(base[2] * (1 - alpha) + b * alpha),
                                255,
                            )
    write_png(os.path.join(out_dir, "_preview.png"), sheet_w, sheet)
    print("wrote preview (inspect and delete before release build)")

if __name__ == "__main__":
    main()

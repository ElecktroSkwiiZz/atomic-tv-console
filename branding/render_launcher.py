#!/usr/bin/env python3
"""Generate Android TV launcher branding for JB Atomic Industries.
Requires Pillow. Reproducible, no network access at render time.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

OUT = Path(__file__).resolve().parents[1] / "app/src/main/res/drawable-nodpi"
OUT.mkdir(parents=True, exist_ok=True)
S = 2
CYAN = (0, 226, 250)
WHITE = (248, 253, 255)
TITLE = "TV Console"
KIND = "console"

def font(size, bold=True):
    paths = ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
             "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf") if bold else (
             "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",)
    for p in paths:
        if Path(p).is_file():
            return ImageFont.truetype(p, size)
    raise RuntimeError("DejaVu fonts required to reproduce TV branding")

def canvas(w,h):
    im = Image.new("RGB", (w,h))
    pix = im.load()
    for y in range(h):
        for x in range(w):
            v = x/w * .50 + (1-y/h)*.50
            pix[x,y] = (int(2+4*v),int(18+43*v),int(70+100*v))
    return im

def logo(draw, cx, cy, r, width):
    pts=[(cx+math.cos(math.pi/6+i*math.pi/3)*r,
          cy+math.sin(math.pi/6+i*math.pi/3)*r) for i in range(6)]
    draw.line(pts+[pts[0]],fill=CYAN,width=width,joint="curve")
    x0,y0=cx-r*.54,cy-r*.31
    x1,y1=cx+r*.54,cy+r*.26
    draw.rounded_rectangle((x0,y0,x1,y1),radius=r*.08,outline=WHITE,width=width)
    if KIND=="capture":
        for sx in (-1,1):
            for sy in (-1,1):
                x=cx+sx*r*.34;y=cy+sy*r*.16
                draw.line([(x-sx*r*.09,y),(x,y),(x,y-sy*r*.09)],fill=CYAN,width=width)
        draw.ellipse((cx-r*.055,cy-r*.055,cx+r*.055,cy+r*.055),fill=CYAN)
    else:
        draw.line([(cx-r*.28,cy-r*.10),(cx-r*.13,cy),(cx-r*.28,cy+r*.10)],
                  fill=CYAN,width=width,joint="curve")
        draw.line((cx-r*.09,cy+r*.10,cx+r*.20,cy+r*.10),fill=WHITE,width=width)
    draw.line((cx-r*.12,cy+r*.32,cx+r*.12,cy+r*.32),fill=CYAN,width=width)

w,h=640*S,360*S
im=canvas(w,h)
d=ImageDraw.Draw(im)
d.polygon([(0,0),(360*S,0),(490*S,360*S),(130*S,360*S)],fill=(5,39,127))
d.polygon([(300*S,0),(490*S,0),(640*S,185*S),(640*S,270*S)],fill=(7,44,136))
logo(d,148*S,176*S,122*S,7*S)
d.text((295*S,95*S),"ATOMIC",font=font(64*S),fill=WHITE,stroke_width=0)
d.text((295*S,165*S),TITLE,font=font(36*S),fill=CYAN,stroke_width=0)
d.rounded_rectangle((297*S,229*S,609*S,233*S),radius=2*S,fill=CYAN)
d.text((298*S,246*S),"JB ATOMIC INDUSTRIES",font=font(18*S),fill=WHITE)
d.rounded_rectangle((5*S,5*S,635*S,355*S),radius=27*S,outline=CYAN,width=5*S)
im.resize((640,360),Image.Resampling.LANCZOS).save(OUT/"console_banner_official.webp",format="WEBP",quality=92,method=6)

icon_img=canvas(512,512)
di=ImageDraw.Draw(icon_img)
logo(di,256,256,218,15)
di.rounded_rectangle((5,5,507,507),radius=70,outline=CYAN,width=10)
icon_img.save(OUT/"console_logo_official.webp",format="WEBP",quality=92,method=6)
print("Rendered launcher banner and icon:", "console_banner_official.webp", "console_logo_official.webp")

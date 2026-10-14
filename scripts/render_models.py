#!/usr/bin/env python3
"""Geometry-derived contact sheet (software preview, not a Minecraft screenshot)."""
import json
import math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font=ImageFont.truetype(FONT,15)
small=ImageFont.truetype(FONT,11)
SIZE=300
out=Image.new('RGB',(SIZE*3,SIZE*3),(16,21,29));draw=ImageDraw.Draw(out)
faces=[('north',[0,1,3,2],.8),('east',[1,5,7,3],.7),('south',[5,4,6,7],.9),('west',[4,0,2,6],.68),('up',[2,3,7,6],1.15),('down',[4,5,1,0],.55)]
def project(v):
 x,y,z=v;x-=8;y-=8;z-=8
 a=math.radians(-30)
 xx=x*math.cos(a)-z*math.sin(a);zz=x*math.sin(a)+z*math.cos(a)
 b=math.radians(18)
 yy=y*math.cos(b)-zz*math.sin(b);depth=y*math.sin(b)+zz*math.cos(b)
 return xx,yy,depth
for idx,entry in enumerate(json.loads((ROOT/'assets/catalog.json').read_text())):
 ox=(idx%3)*SIZE;oy=(idx//3)*SIZE
 model=json.loads((ROOT/'assets/blockbench'/(entry['id']+'.bbmodel')).read_text())
 tex=Image.open(ROOT/'assets/resourcepack/assets/millionblocks/textures/item'/entry['texture']).convert('RGBA')
 polygons=[]
 for cube in model['elements']:
  lo,hi=cube['from'],cube['to']
  points=[project((x,y,z)) for z in [lo[2],hi[2]] for y in [lo[1],hi[1]] for x in [lo[0],hi[0]]]
  for name,ids,shade in faces:
   uv=cube['faces'][name]['uv'];color=tex.getpixel((min(63,int((uv[0]+uv[2])/2)),min(63,int((uv[1]+uv[3])/2))))
   if color[3]==0:continue
   color=tuple(min(255,int(c*shade)) for c in color[:3])
   polygons.append((sum(points[i][2] for i in ids)/4,[(ox+150+points[i][0]*11,oy+148-points[i][1]*11) for i in ids],color))
 for _,poly,color in sorted(polygons,reverse=True):draw.polygon(poly,fill=color,outline=tuple(max(0,c-25) for c in color))
 draw.text((ox+14,oy+12),entry['title'],font=font,fill=entry['accent'])
 draw.text((ox+14,oy+269),f"{entry['cubes']} cubes • editable / static export",font=small,fill='#a3adb9')
 draw.rectangle((ox,oy,ox+SIZE-1,oy+SIZE-1),outline='#384354')
path=ROOT/'assets/previews/v06_geometry_contact_sheet.png';out.save(path);print(path)

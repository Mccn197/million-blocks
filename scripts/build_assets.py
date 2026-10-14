#!/usr/bin/env python3
"""Deterministic V0.6 geometry/atlases from preserved handoff prototypes."""
import base64
import copy
import hashlib
import io
import json
import math
from pathlib import Path
import random
import uuid
from PIL import Image, ImageDraw
import yaml

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'assets/resourcepack'
NS = PACK / 'assets/millionblocks'
BH = ROOT / 'assets/betterhud-plugin/plugins/BetterHud'
NAMES = {
 'prismbreaker': ('Prismbreaker', '#36e8e6'), 'quakehammer': ('Quakehammer', '#ffbd35'),
 'detonation_buddy': ('Detonation Buddy', '#66ee42'), 'blastcaster': ('Blastcaster', '#ff543c'),
 'bore_x': ('Bore-X', '#ffc23b'), 'redline_mk1': ('REDLINE MK-I', '#ff323c'),
 'starfall_beacon': ('Starfall Beacon', '#59c9ff'), 'void_engine': ('VOID ENGINE', '#a958ff'),
 'shop_portal': ('Industrial KEEP OUT / DANGER', '#60ff56')}


def uid(name): return str(uuid.uuid5(uuid.NAMESPACE_URL, 'millionblocks:v06:' + name))
def dump(path, value):
 path.parent.mkdir(parents=True, exist_ok=True)
 path.write_text(json.dumps(value, indent=2) + '\n')

def atlas(name, accent):
 image = Image.new('RGBA', (64, 64))
 draw = ImageDraw.Draw(image)
 colors = ['#29313d', '#677888', accent, '#171c23', '#bb8c35', '#e0d8bf', '#586351', '#cf3e34']
 rng = random.Random(name)
 for tile, color in enumerate(colors):
  x, y = (tile % 4) * 16, (tile // 4) * 16
  draw.rectangle((x,y,x+15,y+15), fill=color)
  rgb = tuple(int(color[i:i+2],16) for i in (1,3,5))
  for px in range(x,x+16):
   for py in range(y,y+16):
    delta=rng.choice([-7,-3,0,0,2,5])
    image.putpixel((px,py),tuple(max(0,min(255,c+delta)) for c in rgb)+(255,))
  draw.line((x,y,x+15,y),fill=tuple(min(255,c+30) for c in rgb))
  draw.line((x,y,x,y+15),fill=tuple(min(255,c+15) for c in rgb))
  draw.line((x,y+15,x+15,y+15),fill=tuple(max(0,c-25) for c in rgb))
 # Dedicated hazard tape, stamped labels, grille and luminous emitter tiles.
 draw.rectangle((0,32,15,47),fill='#ffbf28')
 for start in range(-16,24,8):
  draw.polygon([(start,32),(start+4,32),(start+20,48),(start+16,48)],fill='#10151c')
 draw.rectangle((16,32,47,47),fill='#242a33')
 draw.text((17,33),'KEEP',fill='#ffce52');draw.text((17,40),'OUT',fill='#fff0ba')
 draw.text((16,49),'DANGER',fill='#ff664d')
 for y in range(34,47,3):draw.line((50,y,62,y),fill='#10131a',width=2)
 # Cutout portal energy lattice; transparent holes keep the frame open.
 for y in range(49,64,5):draw.line((0,y,15,y),fill=accent,width=1)
 for x in range(1,16,5):draw.line((x,48,x,63),fill=accent,width=1)
 draw.rectangle((48,48,63,63),fill=accent)
 draw.rectangle((51,51,60,60),outline='#fff4da',width=2)
 return image


def cube(model, name, low, high, tile=0, uv=None):
 faces={face:{'uv': uv or [(tile%4)*16,(tile//4)*16,(tile%4)*16+16,(tile//4)*16+16], 'texture':0}
        for face in ['north','east','south','west','up','down']}
 element={'name':name,'from':list(low),'to':list(high),'uuid':uid(model['model_identifier']+name),'faces':faces,'autouv':0}
 model['elements'].append(element)
 return element['uuid']


def rim(model, name, center, radius, z, tile=1):
 # Faceted annulus built from eight stepped segments; no unsupported Java model rotations.
 cx,cy=center
 for i in range(8):
  a=math.pi*2*i/8
  x,y=cx+radius*math.cos(a),cy+radius*math.sin(a)
  cube(model,name+str(i),(x-.65,y-.65,z),(x+.65,y+.65,z+.6),tile)


def improve(name, title, accent):
 baseline=ROOT/'references/prototypes/blockbench'/f'{name}.bbmodel'
 model=json.loads(baseline.read_text())
 model['name']=title+' V0.6'
 model['model_identifier']=name
 model['credit']='MillionBlocks original geometry V0.6; based on supplied editable V0.5.1 prototype'
 model['resolution']={'width':64,'height':64}
 for e in model['elements']:
  label=e['name'].lower();tile=0
  if any(k in label for k in ['acier','traverse','tête','plat ','verin','rivet','collier','axe','spire','pointe','anneau','pilier','fronton','ancrage']):tile=1
  if any(k in label for k in ['lame','cyan','énerg','cristal','noyau','diode','lentille','rouge','lampe','ioniseur']):tile=2
  if any(k in label for k in ['poignée','sangle','chenille','museau','bouche','oeil','pare-flamme']):tile=3
  if name=='detonation_buddy' and any(k in label for k in ['capsule','dos','patte']):tile=6
  if 'charge tnt' in label:tile=7
  if any(k in label for k in ['danger','avertissement']):tile=8
  if 'membrane' in label:tile=12
  for face in e['faces'].values():
   face['texture']=0;face['uv']=[(tile%4)*16,(tile//4)*16,(tile%4)*16+16,(tile//4)*16+16]
 # Every asset gets physically recessed panels, riveted shell and a distinct functional silhouette.
 if name=='prismbreaker':
  for x in [2,4,6,9,11,13]:
   cube(model,'diamond cutting facet '+str(x),(x,11.8,5.0),(x+1.2,13.1,5.45),2)
  for y in [2,4,6,8]:cube(model,'leather grip '+str(y),(7.0,y,6.0),(8.8,y+.65,8.0),3)
  cube(model,'power cell',(6,9,6),(10,11,9),0);cube(model,'prism inset',(7,9.4,5.6),(9,10.6,6),2)
 elif name=='quakehammer':
  for x in [1.7,12.2]:
   for y in [10.2,12.8]:cube(model,'impact piston '+str((x,y)),(x,y,4.45),(x+1.7,y+1.0,5.0),1)
  cube(model,'hazard impact rail',(1.8,11.2,4.4),(14.2,12.5,4.8),8)
  for y in [2,4,6,8]:cube(model,'shock grip '+str(y),(7,y,6.2),(9,y+.6,8.7),3)
 elif name=='detonation_buddy':
  for x in [3.2,11.5]:
   cube(model,'armored wheel '+str(x),(x,2,5),(x+1.3,5,11),3)
   for z in [5.8,9.5]:cube(model,'wheel hub '+str((x,z)),(x-.15,2.8,z),(x+1.45,4.2,z+1),1)
  cube(model,'hazard belt',(4,6.2,3.2),(12,7.1,3.6),8)
  cube(model,'mining backpack',(5,6,12.2),(11,11,14),0)
  for x in [5.2,10.3]:cube(model,'status eye '+str(x),(x,10,3.1),(x+1,11,3.5),2)
 elif name=='blastcaster':
  rim(model,'segmented muzzle',(8,9),2.6,.2,1)
  cube(model,'top aiming rail',(6.5,12,4),(9.5,12.7,13),0)
  for z in [4,6,8,10]:cube(model,'barrel cooling vent '+str(z),(11.0,8.0,z),(11.7,10.7,z+.6),3)
  cube(model,'hazard cartridge',(4.1,6.2,8),(4.55,10.8,12),8)
  cube(model,'trigger guard',(7,3.8,7),(10,4.5,10),1)
 elif name=='bore_x':
  for step in range(6):
   half=2.7-step*.38;z=4.1-step*.64
   cube(model,'stepped tungsten drill '+str(step),(8-half,8-half,z),(8+half,8+half,z+.55),1 if step%2==0 else 4)
  for x in [4.3,10.8]:
   for z in [7,9,11,13]:cube(model,'motor heat sink '+str((x,z)),(x,7,z),(x+.6,10.8,z+.65),0)
  cube(model,'warning panel',(4.2,5.5,8),(4.6,6.7,12),8)
 elif name=='redline_mk1':
  rim(model,'octagonal focusing lens',(8,8.8),2.1,.1,1)
  cube(model,'red emitter',(7,7.8,0),(9,9.8,.3),15)
  for x in [2.0,12.0]:
   for z in [6,7.5,9,10.5]:cube(model,'radiator rib '+str((x,z)),(x,5,z),(x+1.2,8,z+.45),0)
  cube(model,'redline safety bar',(5.4,10.4,5),(10.6,11,13),8)
  cube(model,'optic sight',(7.2,12,8),(8.8,13.3,10.5),1)
 elif name=='starfall_beacon':
  for x in [3.0,11.0]:
   cube(model,'stabilizer foot '+str(x),(x,.6,4.2),(x+2,1.6,12),0)
   cube(model,'gold support '+str(x),(x,3,7),(x+1,11,8.2),4)
  rim(model,'orbital segmented ring',(8,11),3.9,5.2,1)
  cube(model,'starfall core',(7,10,4.8),(9,12,5.2),15)
 elif name=='void_engine':
  for y in [4,7,10,13]:
   for x in [3.0,12.0]:cube(model,'reactor cooling fin '+str((x,y)),(x,y,4),(x+1,y+.55,12),0)
  rim(model,'containment ring',(8,9),3.0,2.7,1)
  cube(model,'void singularity',(6.7,7.7,2.3),(9.3,10.3,2.8),15)
  for x in [4,10]:cube(model,'warning lower casing '+str(x),(x,1,3.5),(x+2,3,4),8)
 elif name=='shop_portal':
  for x in [1,13.1]:
   cube(model,'hydraulic piston '+str(x),(x,1,5.2),(x+1.6,13.0,6.4),1)
   for y in [2,6,10]:cube(model,'hazard joint '+str((x,y)),(x-.25,y,3.5),(x+1.85,y+1.1,4),8)
  cube(model,'KEEP OUT sign',(4,13,1.8),(12,15,2.2),uv=[16,32,40,47])
  cube(model,'DANGER sign',(5,1.0,1.8),(11,2.2,2.2),uv=[16,48,48,57])
  for x in [2,12.5]:cube(model,'green beacon '+str(x),(x,14.5,3),(x+1.4,15.8,4.4),15)
  cube(model,'industrial sill',(0,0,1.5),(16,.7,8),0)
 # Original bolt details at physical hull corners; mapped to brushed metal.
 shell=model['elements'][0]
 for i,(x,y,z) in enumerate([(shell['from'][0],shell['from'][1],shell['from'][2]-.2),
                            (shell['to'][0]-.4,shell['to'][1]-.4,shell['from'][2]-.2)]):
  cube(model,'fastener '+str(i),(x,y,z),(x+.35,y+.35,z+.25),1)
 image=atlas(name,accent);buf=io.BytesIO();image.save(buf,format='PNG');png=buf.getvalue()
 texture=f'{name}_v06.png'
 (NS/'textures/item'/texture).write_bytes(png)
 model['textures']=[{'path':texture,'name':texture,'id':'0','namespace':'millionblocks','folder':'item',
                     'particle':True,'render_mode':'default','visible':True,'saved':True,'uuid':uid(name+'texture'),
                     'source':'data:image/png;base64,'+base64.b64encode(png).decode()}]
 root_group={'name':title,'uuid':uid(name+'root'),'origin':[8,8,8],'rotation':[0,0,0],'children':[e['uuid'] for e in model['elements']]}
 model['outliner']=[root_group]
 dump(ROOT/'assets/blockbench'/f'{name}.bbmodel',model)
 elements=[]
 for e in model['elements']:
  elements.append({'from':e['from'],'to':e['to'],'faces':{f:{'uv':[v/4 for v in face['uv']],'texture':'#0'} for f,face in e['faces'].items()},**({'rotation':e['rotation']} if 'rotation' in e else {})})
 exported={'credit':model['credit'],'textures':{'0':f'millionblocks:item/{name}_v06','particle':f'millionblocks:item/{name}_v06'},'elements':elements,'display':model.get('display',{})}
 dump(NS/'models/item'/f'{name}.json',exported)
 dump(NS/'items'/f'{name}.json',{'model':{'type':'minecraft:model','model':f'millionblocks:item/{name}'}})
 if name in ['bore_x','redline_mk1']:
  if name=='bore_x':
   part='bore_x_rotor'
   selected=[i for i,e in enumerate(model['elements']) if any(k in e['name'] for k in ['axe','pointe','spire','dent foret','stepped tungsten'])]
  else:
   part='redline_portable'
   selected=[i for i,e in enumerate(model['elements']) if any(k in e['name'] for k in ['canon','lentille','bobine','pare-flamme','réacteur','octagonal','emitter','optic','safety'])]
  partial=copy.deepcopy(exported);partial['elements']=[exported['elements'][i] for i in selected]
  if name=='redline_mk1':
   chassis=copy.deepcopy(exported);chassis['elements']=[e for i,e in enumerate(exported['elements']) if i not in selected]
   dump(NS/'models/item/redline_chassis.json',chassis)
   dump(NS/'items/redline_chassis.json',{'model':{'type':'minecraft:model','model':'millionblocks:item/redline_chassis'}})
  dump(NS/'models/item'/f'{part}.json',partial)
  dump(NS/'items'/f'{part}.json',{'model':{'type':'minecraft:model','model':f'millionblocks:item/{part}'}})
 if name in ['shop_portal','bore_x','blastcaster','void_engine']:

  rig=copy.deepcopy(model);rig['meta']['model_format']='free'
  animation={'name':{'shop_portal':'emerge','bore_x':'drill_spin','blastcaster':'recoil','void_engine':'charge'}[name],
             'uuid':uid(name+'animation'),'loop':'loop' if name=='bore_x' else 'once','length':1.0,'anim_time_update':'','blend_weight':'','animators':{}}
  channel='rotation' if name=='bore_x' else 'position'
  frames=[(0,[0,0,0]),(.5,[0,0,180]),(1,[0,0,360])] if name=='bore_x' else (
      [(0,[0,-16,0]),(1,[0,0,0])] if name=='shop_portal' else (
      [(0,[0,0,0]),(.1,[0,0,2]),(.4,[0,0,0]),(1,[0,0,0])] if name=='blastcaster' else [(0,[0,0,0]),(.5,[0,2,0]),(1,[0,0,0])]))
  animated_bone=root_group['uuid']
  if name=='bore_x':
   rotor_ids=[model['elements'][i]['uuid'] for i in selected]
   rotor={'name':'Tungsten rotor','uuid':uid(name+'rotor'),'origin':[8,8,4],'rotation':[0,0,0],'children':rotor_ids}
   housing={'name':'Fixed housing','uuid':uid(name+'housing'),'origin':[8,8,8],'rotation':[0,0,0],
            'children':[e['uuid'] for e in model['elements'] if e['uuid'] not in rotor_ids]}
   rig['outliner'][0]['children']=[rotor,housing];animated_bone=rotor['uuid']
  animation['animators'][animated_bone]={'name':title,'type':'bone','keyframes':[
      {'channel':channel,'time':t,'data_points':[dict(zip(['x','y','z'],map(str,v)))], 'uuid':uid(name+str(t)),'interpolation':'linear'} for t,v in frames]}
  rig['animations']=[animation];dump(ROOT/'assets/blockbench/animated'/f'{name}_rig.bbmodel',rig)
 return {'id':name,'title':title,'cubes':len(elements),'accent':accent,'texture':texture}


def betterhud():
 def cond(first,second,op='=='):return {'1':{'first':first,'second':str(second).lower(),'operation':op}}
 images={n:{'type':'single','file':f'{n}.png'} for n in ['mb_amber_panel','mb_amber_pickaxe','mb_amber_block']}
 for i in range(5):images[f'mb_crack_{i}']={'type':'single','file':f'mb_stone_crack_{i}.png'}
 # Original images have mb_panel_amber / mb_pickaxe / mb_block filenames.
 for k,v in {'mb_amber_panel':'mb_panel_amber.png','mb_amber_pickaxe':'mb_pickaxe.png','mb_amber_block':'mb_block.png'}.items():images[k]['file']=v
 layout={'images':{'1':{'name':'mb_amber_panel','x':52,'y':0},
                    '2':{'name':'mb_amber_pickaxe','x':0,'y':-16,'conditions':cond('millionblocks_icon',True)},
                    '3':{'name':'mb_amber_block','x':20,'y':0,'conditions':cond('millionblocks_icon',True)}},
          'texts':{'1':{'name':'mb_font','pattern':'<#fff2d6>[millionblocks_count]','x':70,'y':15,'align':'left','scale':1.6},
                   '2':{'name':'mb_font','pattern':'<#ffca31>[millionblocks_multiplier]','x':35,'y':-12,'align':'left','scale':1},
                   '3':{'name':'mb_font','pattern':'<#b9c4c4>[millionblocks_goal]  [millionblocks_percent]','x':70,'y':48,'align':'left','scale':.85}}}
 for i in range(5):
  layout['images'][str(4+i)]={'name':f'mb_crack_{i}','x':28,'y':8,'conditions':{
   '1':{'first':'millionblocks_crack','second':str(i),'operation':'=='},
   '2':{'first':'millionblocks_icon','second':'true','operation':'=='}}}
 hud={'mb_amber':{'default':False,'tick':1,'conditions':cond('millionblocks_visible',True),
       'layouts':{'1':{'name':'mb_amber_layout','gui':{'x':0,'y':0},'pixel':{'x':20,'y':80}}}}}
 for folder,value in [('images',images),('layouts',{'mb_amber_layout':layout}),('huds',hud),('texts',{'mb_font':{'merge-default-bitmap':True,'use-unifont':False,'include':[]}})]:
  (BH/folder/'millionblocks-amber.yml').write_text(yaml.safe_dump(value,sort_keys=False,allow_unicode=True))


def main():
 prototypes=ROOT/'references/prototypes/blockbench'
 if not prototypes.exists():
  import shutil
  shutil.copytree(ROOT/'assets/blockbench',prototypes)
 catalog=[improve(n,*v) for n,v in NAMES.items()]
 dump(ROOT/'assets/catalog.json',catalog)
 betterhud()
 print('Built',len(catalog),'editable models, Minecraft item definitions, atlases and BetterHud config')

if __name__=='__main__':main()

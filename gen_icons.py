from PIL import Image, ImageDraw, ImageFont
import os

def draw_rr(draw, xy, r, fill):
    x0,y0,x1,y1 = xy
    draw.rectangle([x0+r,y0,x1-r,y1],fill=fill)
    draw.rectangle([x0,y0+r,x1,y1-r],fill=fill)
    for ex,ey in [(x0,y0),(x1-2*r,y0),(x0,y1-2*r),(x1-2*r,y1-2*r)]:
        draw.ellipse([ex,ey,ex+2*r,ey+2*r],fill=fill)

def icon(size):
    img = Image.new("RGBA",(size,size),(0,0,0,0))
    d = ImageDraw.Draw(img)
    r = int(size*0.18)
    draw_rr(d,[0,0,size-1,size-1],r,(183,28,28))
    pad=int(size*0.18); pw=size-2*pad; ph=int(pw*1.28)
    px=pad; py=(size-ph)//2; fold=int(pw*0.22)
    d.polygon([(px,py+fold),(px+pw-fold,py),(px+pw,py+fold),(px+pw,py+ph),(px,py+ph)],fill=(255,255,255,255))
    d.polygon([(px+pw-fold,py),(px+pw,py+fold),(px+pw-fold,py+fold)],fill=(220,220,220,255))
    fs=int(pw*0.38); font=None
    for fp in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf","/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"]:
        try: font=ImageFont.truetype(fp,fs); break
        except: pass
    if not font: font=ImageFont.load_default()
    bb=d.textbbox((0,0),"PDF",font=font); tw=bb[2]-bb[0]; th=bb[3]-bb[1]
    tx=px+(pw-tw)//2; ty=py+fold+(ph-fold-th)//2-int(size*0.03)
    d.text((tx,ty),"PDF",fill=(183,28,28,255),font=font)
    lx0=px+int(pw*0.15); lx1=px+int(pw*0.85); ly=ty+th+int(size*0.04)
    lh=max(2,int(size*0.025)); lg=int(size*0.05)
    for i in range(3):
        y=ly+i*(lh+lg)
        if y+lh<py+ph-int(size*0.05):
            d.rectangle([lx0,y,lx0+int((lx1-lx0)*(0.6 if i==2 else 1)),y+lh],fill=(200,200,200,200))
    return img

for folder,size in {"mipmap-mdpi":48,"mipmap-hdpi":72,"mipmap-xhdpi":96,"mipmap-xxhdpi":144,"mipmap-xxxhdpi":192}.items():
    p=f"app/src/main/res/{folder}"; os.makedirs(p,exist_ok=True)
    ic=icon(size); ic.save(f"{p}/ic_launcher.png","PNG")
    ri=Image.new("RGBA",(size,size),(0,0,0,0))
    mk=Image.new("L",(size,size),0); ImageDraw.Draw(mk).ellipse([0,0,size-1,size-1],fill=255)
    ri.paste(ic,(0,0),mk); ri.save(f"{p}/ic_launcher_round.png","PNG")
    print(f"Icono {folder} generado")

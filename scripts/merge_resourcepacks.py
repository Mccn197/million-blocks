#!/usr/bin/env python3
"""Merge a generated BetterHud 26.2 pack with MillionBlocks, refusing conflicting resources."""
import argparse
import json
from pathlib import Path, PurePosixPath
import stat
import sys
import zipfile


def read_pack(path):
    result={}
    with zipfile.ZipFile(path) as z:
        if sum(i.file_size for i in z.infolist())>512*1024*1024:raise ValueError('Uncompressed pack exceeds 512 MiB')
        for entry in z.infolist():
            if entry.is_dir():continue
            name=entry.filename
            p=PurePosixPath(name)
            if p.is_absolute() or '..' in p.parts or '\\' in name or stat.S_ISLNK(entry.external_attr>>16):
                raise ValueError('Unsafe ZIP entry: '+name)
            if name in result:raise ValueError('Duplicate ZIP entry: '+name)
            result[name]=z.read(entry)
    if 'pack.mcmeta' not in result:raise ValueError('pack.mcmeta must be at ZIP root')
    return result


def supports_88(meta):
    pack=meta['pack']
    if 'min_format' in pack and 'max_format' in pack:
        def major(v):return v[0] if isinstance(v,list) else v
        return major(pack['min_format'])<=88<=major(pack['max_format'])
    formats=pack.get('supported_formats',pack.get('pack_format'))
    if isinstance(formats,int):return formats==88
    if isinstance(formats,list):return formats[0]<=88<=formats[-1]
    if isinstance(formats,dict):return formats['min_inclusive']<=88<=formats['max_inclusive']
    return False


def merge(betterhud,items,output):
    if output.exists():raise ValueError('Output already exists; refusing overwrite')
    main=read_pack(betterhud);extra=read_pack(items)
    for resources in [main,extra]:
        if not supports_88(json.loads(resources['pack.mcmeta'])):raise ValueError('Both packs must support Minecraft 26.2 / format 88')
    for name,data in extra.items():
        if name=='pack.mcmeta':continue # Preserve BetterHud's metadata and shader overlays.
        if name in main and main[name]!=data:raise ValueError('Resource conflict: '+name)
        main[name]=data
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'x',zipfile.ZIP_DEFLATED) as z:
        for name,data in sorted(main.items()):z.writestr(name,data)
    return len(main)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--betterhud',required=True,type=Path)
    p.add_argument('--items',required=True,type=Path)
    p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();print('Merged',merge(a.betterhud,a.items,a.output),'resources:',a.output)

if __name__=='__main__':
    try:main()
    except (ValueError,OSError,KeyError,zipfile.BadZipFile,json.JSONDecodeError) as e:sys.exit('REFUSED: '+str(e))

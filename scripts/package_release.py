#!/usr/bin/env python3
"""Package the compiled candidate and validated asset/config sources, without deployment."""
import hashlib
import json
import re
import xml.etree.ElementTree as E
from pathlib import Path
import shutil
import struct
import subprocess
import zipfile
ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'


def archive(path, entries):
    with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
        for source,name in sorted(entries,key=lambda pair:pair[1]):
            z.write(source,name)
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None, 'Corrupt archive: '+str(path)


def tree(path,prefix=''):
    return [(p,prefix+p.relative_to(path).as_posix()) for p in path.rglob('*') if p.is_file() and '__pycache__' not in p.parts]


def main():
    jar=ROOT/'target/MillionBlocks-0.6.0.jar'
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        manifest=z.read('META-INF/MANIFEST.MF').decode()
        line=next(l for l in manifest.splitlines() if l.startswith('Paper-API-Validation:'))
        api=line.split(':',1)[1].strip()
        assert api in ['26.2-source-58e6e45','26.2.build.121-stable'],api
        bytecode=z.read('eu/reverie/millionblocks/MillionBlocksPlugin.class')
        assert struct.unpack('>H',bytecode[6:8])[0]==69,'Expected Java 25 class version'
        assert not any(name.startswith(('org/bukkit/','io/papermc/paper/')) for name in z.namelist()),'API must not be bundled'
        assert not any(name.endswith('Test.class') for name in z.namelist())
    DIST.mkdir(exist_ok=True)
    suffix='-source-api' if api.startswith('26.2-source') else ''
    shipped=DIST/f'MillionBlocks-0.6.0{suffix}.jar';shutil.copyfile(jar,shipped)
    itemzip=DIST/'MillionBlocks-items-0.6.0.zip';archive(itemzip,tree(ROOT/'assets/resourcepack'))
    bhzip=DIST/'MillionBlocks-BetterHud-0.6.0.zip';archive(bhzip,tree(ROOT/'assets/betterhud-plugin'))
    models=DIST/'MillionBlocks-models-0.6.0.zip'
    archive(models,tree(ROOT/'assets/blockbench','blockbench/')+tree(ROOT/'assets/resourcepack/assets/millionblocks/models','minecraft/models/')+
            tree(ROOT/'assets/resourcepack/assets/millionblocks/textures/item','textures/')+[(ROOT/'assets/catalog.json','catalog.json')])
    sourcezip=DIST/'MillionBlocks-sources-0.6.0.zip'
    paths=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0')
    archive(sourcezip,[(ROOT/p,p) for p in paths if p])
    reports=list((ROOT/'target/surefire-reports').glob('TEST-*.xml'))
    assert reports,'Missing current Java test reports'
    suites=[E.parse(p).getroot() for p in reports]
    java_tests=sum(int(s.attrib['tests']) for s in suites)
    failures=sum(int(s.attrib.get(k,0)) for s in suites for k in ['failures','errors'])
    skipped=sum(int(s.attrib.get('skipped',0)) for s in suites)
    assert java_tests>0 and failures==0 and skipped==0,'Java tests not fully passed'
    python_log=(ROOT/'docs/validation/python-tests.log').read_text()
    match=re.search(r'Ran (\d+) tests',python_log)
    assert match and re.search(r'\nOK\s*$',python_log),'Python test completion missing'
    python_tests=int(match.group(1))
    provenance={'version':'0.6.0','git_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip(),
        'java':'Temurin 25.0.2+10','maven':'3.9.11','paper_api':api,
        'official_paper_source_commit':'58e6e45d67c998909581f9079189ef51eb6f1267' if suffix else None,
        'exact_paper_build_121_validated':not bool(suffix),'server_started':False,'minecraft_client_tested':False,
        'nitroserv_accessed':False,'reveriemc_accessed':False,'remote_pipeline_triggered':False,
        'java_tests':java_tests,'python_tests':python_tests,'tests_failed':failures,'tests_skipped':skipped,
        'betterhud_validation':'2.1.0 public source API/config contract inspected; installed server version and generated shader pack not validated',
        'jar_sha256':hashlib.sha256(shipped.read_bytes()).hexdigest()}
    (DIST/'build-provenance.json').write_text(json.dumps(provenance,indent=2)+'\n')
    artifacts=[shipped,itemzip,bhzip,models,sourcezip,DIST/'build-provenance.json']
    sums=DIST/'SHA256SUMS'
    def checksums(files):sums.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in files))
    checksums(artifacts)
    bundle=DIST/'MillionBlocks-V06-CANDIDATE.zip'
    archive(bundle,[(p,p.name) for p in artifacts+[sums]]+tree(ROOT/'docs','docs/'))
    checksums(artifacts+[bundle])
    for path in artifacts+[bundle,sums]:print(path.name,path.stat().st_size,'bytes')

if __name__=='__main__':main()

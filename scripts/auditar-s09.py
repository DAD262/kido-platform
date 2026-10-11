"""Chequeos estáticos offline del repositorio (no reemplazan Maven ni Docker)."""
from pathlib import Path
import xml.etree.ElementTree as ET
import yaml
import sys

root=Path(__file__).resolve().parents[1]
errors=[]
java=list(root.rglob('*.java'))
poms=list(root.rglob('pom.xml'))
yamls=[x for x in root.rglob('*') if x.suffix.lower() in ('.yaml','.yml') and '.github' not in x.parts]

for path in poms:
    try: ET.parse(path)
    except Exception as exc: errors.append(f'POM inválido: {path}: {exc}')
for path in yamls:
    try:
        with path.open(encoding='utf-8') as stream: yaml.safe_load(stream)
    except Exception as exc: errors.append(f'YAML inválido: {path}: {exc}')

# Conservative lexical brace check: strips strings, chars, // and /* */ comments.
def balanced(path):
    s=path.read_text(encoding='utf-8')
    state='code'; depth=0; line=1
    i=0
    while i<len(s):
        c=s[i]; nex=s[i+1] if i+1<len(s) else ''
        if c=='\n': line+=1
        if state=='code':
            if c=='/' and nex=='/': state='line';i+=2;continue
            if c=='/' and nex=='*': state='block';i+=2;continue
            if c=='"': state='string';i+=1;continue
            if c=="'": state='char';i+=1;continue
            if c=='{':depth+=1
            if c=='}':
                depth-=1
                if depth<0:return f'llave extra en línea {line}'
        elif state=='line':
            if c=='\n':state='code'
        elif state=='block':
            if c=='*' and nex=='/':state='code';i+=2;continue
        elif state in ('string','char'):
            if c=='\\':i+=2;continue
            if c==('"' if state=='string' else "'"):state='code'
        i+=1
    if depth!=0:return f'paréntesis de bloque desequilibrados: {depth}'
    return None
for path in java:
    b=balanced(path)
    if b:errors.append(f'Java sospechoso: {path}: {b}')

# Control key business guardrails, including demo opt-in and duplicate handling.
required={
    'Pago retry instead of HTTP sync':'services/kido-pago-ms/src/main/java/pe/edu/upeu/pago/service/PagoService.java',
    'Kafka payment idempotency':'services/kido-inscripcion-ms/src/main/java/pe/edu/upeu/inscripcion/messaging/PagoCompraEventosConsumer.java',
    'S09 demo':'scripts/demo-s9-saga.ps1',
    'Postgres purchase unique':'services/kido-inscripcion-ms/src/main/resources/db/migration/V4__saga_orden_compra.sql',
    'Postgres notification unique':'services/kido-notificacion-ms/src/main/resources/db/migration/V2__idempotencia_notificaciones_eventos.sql',
    'S09 docs':'docs/s09-implementacion-validacion.md',
}
for label,path in required.items():
    if not (root/path).is_file():errors.append(f'Falta {label}: {path}')
if (root/'.git').exists():errors.append('No incluir .git en un ZIP distribuible')
if (root/'.env').exists():errors.append('No incluir credenciales privadas .env en ZIP')
if errors:
    print('AUDITORÍA ESTÁTICA: ERRORES')
    print('\n'.join(errors))
    sys.exit(1)
print(f'AUDITORÍA ESTÁTICA OK: {len(poms)} POM, {len(yamls)} YAML, {len(java)} Java.')
print('NOTA: sin red/Maven/Docker, no se ha validado compilación ni ejecución real de microservicios.')

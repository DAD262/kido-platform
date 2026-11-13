const {test}=require('node:test');const assert=require('node:assert/strict');
const {configuracion,origen,ruta}=require('./compartir-config.cjs');
const config=configuracion({KIDO_PUBLIC_ORIGIN:'https://tunel.example',KIDO_PUBLIC_UNTIL:'2026-10-30T20:00:00Z'});
test('permite solo el origen autorizado durante el plazo',()=>{assert.equal(origen({headers:{host:'tunel.example'}},config,Date.parse('2026-10-30T19:00:00Z')).origin,'https://tunel.example');assert.equal(origen({headers:{host:'otro.example'}},config,0),null);});
test('vence incluso si el tunel sigue abierto y conserva localhost',()=>{assert.equal(origen({headers:{host:'tunel.example'}},config,config.hasta),null);assert.equal(origen({headers:{host:'localhost:4300','x-forwarded-host':'tunel.example'}},config,config.hasta),null);assert.equal(origen({headers:{host:'localhost:4300'}},config,config.hasta).origin,'http://localhost:4300');});
test('sin fecha no habilita acceso publico',()=>{const cerrado=configuracion({KIDO_PUBLIC_ORIGIN:'https://tunel.example'});assert.equal(origen({headers:{host:'tunel.example'}},cerrado),null);});
test('normaliza rutas y no permite atravesar el filtro con codificacion',()=>{assert.equal(ruta({url:'/auth/realms/kido/../master'}),'/auth/realms/master');assert.equal(ruta({url:'/auth/realms/kido/%252e%252e/master'}),'');});

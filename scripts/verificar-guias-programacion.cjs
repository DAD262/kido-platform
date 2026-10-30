const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const dir=path.resolve(__dirname,'../frontend/public/learning');
const archivos=fs.readdirSync(dir).filter(f=>f.endsWith('.html'));
assert.equal(archivos.length,72);
let enlaces=0;
for(const archivo of archivos){
 const html=fs.readFileSync(path.join(dir,archivo),'utf8');
 for(const seccion of ['Qué aprenderás','Ejemplo guiado','Tu práctica','Comprueba tu solución','documentación oficial']){
  assert.ok(html.includes(seccion),`${archivo}: falta ${seccion}`);
 }
 assert.ok(!html.includes('example.invalid'),archivo);
 for(const match of html.matchAll(/href="([a-z0-9-]+\.html)"/g)){
  assert.ok(fs.existsSync(path.join(dir,match[1])),`${archivo}: enlace roto ${match[1]}`);enlaces++;
 }
}
console.log(`${archivos.length} guías completas; ${enlaces} enlaces entre lecciones verificados.`);

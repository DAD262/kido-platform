// Puerto exclusivo de KIDO. No aceptar el puerto alternativo que proponga Angular.
const net = require('node:net');
const path = require('node:path');
const { spawn } = require('node:child_process');
const PORT = 4300;
const fs = require('node:fs');
const envPath = path.resolve(__dirname, '../../.env');
const settings = {};
if(fs.existsSync(envPath))for(const line of fs.readFileSync(envPath,'utf8').split(/\r?\n/)){const i=line.indexOf('=');const key=line.slice(0,i);if(['KIDO_PUBLIC_ORIGIN','KIDO_PUBLIC_UNTIL'].includes(key))settings[key]=line.slice(i+1).trim();}
const shared = {...settings,...Object.fromEntries(Object.entries(process.env).filter(([key])=>['KIDO_PUBLIC_ORIGIN','KIDO_PUBLIC_UNTIL'].includes(key)))};
require('./compartir-config.cjs').configuracion(shared);
async function comprobar(host) {
  await new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', error => {
      if (error.code === 'EADDRNOTAVAIL' || error.code === 'EAFNOSUPPORT') resolve();
      else reject(error);
    });
    server.listen({port:PORT,host,exclusive:true}, () => server.close(resolve));
  });
}
(async () => {
  const args=process.argv.slice(2);
  if(args.some(a=>a==='--port'||a.startsWith('--port=')||a==='--host'||a.startsWith('--host='))) {
    throw new Error('KIDO utiliza localhost:4300. Cambia su configuraciÃ³n explÃ­cita para usar otro puerto.');
  }
  await comprobar('127.0.0.1');
  await comprobar('::1');
  const cli=path.join(path.dirname(require.resolve('@angular/cli/package.json')),'bin','ng.js');
  console.log('KIDO â†’ http://localhost:4300 Â· ContaCore conserva su puerto 4200.');
  const child=spawn(process.execPath,[cli,'serve','--port',String(PORT),'--host','localhost','--proxy-config','proxy.conf.cjs',...args],{cwd:path.resolve(__dirname,'..'),stdio:'inherit',env:{...process.env,...shared,CI:'true'}});
  child.on('error',error=>{console.error(error.message);process.exitCode=1;});
  child.on('exit',code=>{process.exitCode=code??1;});
  process.on('SIGINT',()=>child.kill('SIGINT'));
  process.on('SIGTERM',()=>child.kill('SIGTERM'));
})().catch(error=>{
  console.error(error.code==='EADDRINUSE'?'No se inicia KIDO: el puerto 4300 estÃ¡ ocupado. Revisa el proceso; no se cambiarÃ¡ al puerto de otra aplicaciÃ³n.':error.message);
  process.exitCode=1;
});


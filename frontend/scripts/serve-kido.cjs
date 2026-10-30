// Puerto exclusivo de KIDO. No aceptar el puerto alternativo que proponga Angular.
const net = require('node:net');
const path = require('node:path');
const { spawn } = require('node:child_process');
const PORT = 4300;
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
    throw new Error('KIDO utiliza localhost:4300. Cambia su configuración explícita para usar otro puerto.');
  }
  await comprobar('127.0.0.1');
  await comprobar('::1');
  const cli=path.join(path.dirname(require.resolve('@angular/cli/package.json')),'bin','ng.js');
  console.log('KIDO → http://localhost:4300 · ContaCore conserva su puerto 4200.');
  const child=spawn(process.execPath,[cli,'serve','--port',String(PORT),'--host','localhost','--proxy-config','proxy.conf.json',...args],{cwd:path.resolve(__dirname,'..'),stdio:'inherit',env:{...process.env,CI:'true'}});
  child.on('error',error=>{console.error(error.message);process.exitCode=1;});
  child.on('exit',code=>{process.exitCode=code??1;});
  process.on('SIGINT',()=>child.kill('SIGINT'));
  process.on('SIGTERM',()=>child.kill('SIGTERM'));
})().catch(error=>{
  console.error(error.code==='EADDRINUSE'?'No se inicia KIDO: el puerto 4300 está ocupado. Revisa el proceso; no se cambiará al puerto de otra aplicación.':error.message);
  process.exitCode=1;
});

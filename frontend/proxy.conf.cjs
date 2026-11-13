const {configuracion,origen,ruta}=require('./scripts/compartir-config.cjs');
const config=configuracion();
function cabeceras(proxyReq,req){
  const externo=origen(req,config);
  for(const key of ['Forwarded','X-Forwarded-For','X-Forwarded-Host','X-Forwarded-Proto','X-Forwarded-Port','X-Forwarded-Prefix'])proxyReq.removeHeader(key);
  if(externo){proxyReq.setHeader('X-Forwarded-Host',externo.host);proxyReq.setHeader('X-Forwarded-Proto',externo.protocol.slice(0,-1));proxyReq.setHeader('X-Forwarded-Port',externo.port||(externo.protocol==='https:'?'443':'80'));}
}
module.exports={
 '^/auth(?:/|\\?|$)':{
  target:'http://127.0.0.1:19090',changeOrigin:true,secure:false,
  rewrite:value=>value.replace(/^\/auth(?=\/|\?|$)/,''),
  bypass:req=>{if(!origen(req,config)||!/^\/auth\/(?:resources\/|realms\/kido(?:\/(?:\.well-known\/|protocol\/openid-connect(?:\/|$)|login-actions(?:\/|$)|account(?:\/|$)|broker(?:\/|$))|$))/.test(ruta(req)))return false;},
  configure:proxy=>proxy.on('proxyReq',(proxyReq,req)=>{cabeceras(proxyReq,req);proxyReq.setHeader('X-Forwarded-Prefix','/auth');}),
 },
 '^/api(?:/|\\?|$)':{
  target:'http://127.0.0.1:18080',changeOrigin:true,secure:false,
  bypass:req=>{const externo=origen(req,config),url=ruta(req);if(!externo||!url)return false;if(config.publico&&externo.origin===config.publico.origin&&/^\/api\/v1\/(?:usuarios\/admin|pagos\/admin)(?:\/|$)/.test(url))return false;},
  configure:proxy=>proxy.on('proxyReq',cabeceras),
 },
 '^/':{
  target:'http://127.0.0.1:4300',
  // Devolver la ruta pasa al servidor Angular sin reenviarla. Al vencer, devuelve 404.
  bypass:req=>origen(req,config)?(req.url||'/'):false,
 },
};

const locales=new Set(['localhost:4300','127.0.0.1:4300','[::1]:4300']);
function configuracion(env=process.env){
  if(!env.KIDO_PUBLIC_ORIGIN)return {publico:null,hasta:0};
  const publico=new URL(env.KIDO_PUBLIC_ORIGIN);
  if(publico.protocol!=='https:'||publico.username||publico.password||publico.search||publico.hash||publico.pathname!=='/')throw new Error('KIDO_PUBLIC_ORIGIN debe ser un origen HTTPS sin ruta ni credenciales.');
  const hasta=Date.parse(env.KIDO_PUBLIC_UNTIL||'');
  return {publico,hasta:Number.isFinite(hasta)?hasta:0};
}
function origen(req,config,ahora=Date.now()){
  const host=String(req.headers.host||'').toLowerCase();
  const forwarded=String(req.headers['x-forwarded-host']||'').split(',').at(-1).trim().toLowerCase();
  if(config.publico&&(host===config.publico.host||forwarded===config.publico.host))return ahora<config.hasta?config.publico:null;
  if(forwarded&&!locales.has(forwarded))return null;
  if(String(req.headers['x-forwarded-proto']||'').split(',').at(-1).trim()==='https')return null;
  return locales.has(host)?new URL('http://'+host):null;
}
function ruta(req){try{const value=decodeURIComponent((req.url||'/').split('?')[0]);if(value.includes('%')||value.includes('\\'))return '';return new URL(value,'http://localhost').pathname;}catch{return '';}}
module.exports={configuracion,origen,ruta};

/** Guías locales conocidas o recursos HTTP(S); nunca ejecutar esquemas de código. */
export function enlaceClase(url:string,origen:string):string|null {
 if(/^\/learning\/[a-z0-9-]+\.html$/.test(url))return new URL(url,origen).href;
 if(!/^https?:\/\//i.test(url))return null;
 try {const destino=new URL(url);return ['http:','https:'].includes(destino.protocol)?destino.href:null;}catch{return null;}
}

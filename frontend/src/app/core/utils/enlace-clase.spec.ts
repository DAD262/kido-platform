import {enlaceClase} from './enlace-clase';
describe('Enlaces del aula',()=>{
 it('abre las guías en el origen de Kido y no en 4200',()=>{expect(enlaceClase('/learning/java-inicio-1.html','http://localhost:4300')).toBe('http://localhost:4300/learning/java-inicio-1.html');});
 it('conserva las referencias oficiales HTTPS',()=>{expect(enlaceClase('https://dev.java/learn/','http://localhost:4300')).toBe('https://dev.java/learn/');});
 it('rechaza código, rutas relativas y navegación local arbitraria',()=>{for(const url of ['javascript:alert(1)','data:text/html,test','//otro-sitio.com','/learning/../admin','/admin','texto sin enlace','https://'])expect(enlaceClase(url,'http://localhost:4300')).toBeNull();});
});

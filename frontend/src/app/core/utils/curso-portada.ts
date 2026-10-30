export function portadaCurso(titulo:string|undefined|null):string|null {
 const t=(titulo??'').toLocaleLowerCase('es');
 if(t.includes('javascript')||t.includes('typescript'))return '/course-covers/spring.jpg?v=photo-1';
 if(t.includes('angular')||t.includes('html'))return '/course-covers/python-2.jpg?v=photo-1';
 if(t.includes('sql')||t.includes('git'))return '/course-covers/java.jpg?v=photo-1';
 if(t.includes('python'))return t.includes('2.0')?'/course-covers/python-2.jpg?v=photo-1':'/course-covers/python.jpg?v=photo-1';
 if(t.includes('spring'))return '/course-covers/spring.jpg?v=photo-1';
 if(t.includes('java'))return '/course-covers/java.jpg?v=photo-1';
 return null;
}

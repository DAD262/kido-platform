INSERT INTO categorias(nombre, descripcion) VALUES
('Tecnología','Programación y herramientas digitales'),
('Educación','Estrategias para enseñar y aprender');
INSERT INTO cursos(titulo,descripcion,tipo,precio,docente_id,estado,categoria_id) VALUES
('Introducción a Java','Curso gratuito de fundamentos de Java','GRATUITO',0,1,'PUBLICADO',1),
('Desarrollo web con Spring','Curso de pago para crear APIs REST','PAGO',79.90,1,'PUBLICADO',1);
INSERT INTO modulos(curso_id,titulo,orden_modulo) VALUES (1,'Primeros pasos',1),(2,'API REST',1);
INSERT INTO lecciones(modulo_id,titulo,orden_leccion,url_contenido) VALUES
(1,'Instalación de Java',1,'https://example.invalid/java'),(2,'Crear un controlador',1,'https://example.invalid/spring');

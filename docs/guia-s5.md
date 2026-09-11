# Guía de sustentación S5 para Adan

## Antes de iniciar

- Presenta un pitch breve de 1 a 3 minutos: problema, solución Kido y avance construido.
- Usa una presentación ordenada y cuida la puntualidad y presentación personal.
- Abre previamente Docker Desktop, Eureka, Config Server, GitHub y una terminal.
- Ensaya el flujo completo y explica qué código desarrollaste tú.

## Orden de la demostración

1. Explica el problema: Kido centraliza cursos gratuitos y pagados.
2. Indica tu aporte: Curso e Inscripción.
3. Muestra las dos bases de datos independientes y las migraciones Flyway.
4. Abre Config Server y compara DEV con PROD.
5. Abre Eureka y muestra las dos instancias de Curso.
6. Ejecuta el CRUD de Curso mediante Gateway.
7. Envía un curso gratuito con precio mayor a cero para demostrar el error 400.
8. Ejecuta varias consultas y muestra `X-Instance-ID`.
9. Detén una instancia de Curso y demuestra que la otra responde.
10. Enseña el repositorio y la documentación.

## Respuestas sencillas a preguntas probables

**¿Por qué hay dos bases de datos?** Cada microservicio es dueño de sus datos. Inscripciones guarda el identificador del curso, pero no crea una llave foránea hacia la base de Cursos.

**¿Qué hace Config Server?** Guarda la configuración fuera del código. El mismo JAR usa puertos, base de datos y nivel de logs diferentes según DEV o PROD.

**¿Qué hace Eureka?** Mantiene una lista dinámica de instancias disponibles. Los clientes internos buscan el nombre lógico y no un puerto escrito a mano.

**¿Qué significa lb://?** Es una dirección lógica. Eureka encuentra las instancias y Spring Cloud LoadBalancer elige una de ellas.

**¿Qué balanceo se usa?** Round-robin. Alterna las peticiones porque las dos instancias son equivalentes y no guardan sesión local.

**¿Por qué Eureka no detecta una caída al instante?** Usa renovaciones periódicas. Espera un intervalo antes de retirar una instancia para evitar eliminarla por una falla breve.

**¿Dónde está Tomcat?** Está embebido en cada aplicación Spring Web MVC. Los servicios se empaquetan como JAR.

**¿Cuál es la parte transaccional?** Inscripcion es la cabecera y ProgresoLeccion es el detalle. Se guardan juntos dentro de una transacción.

**¿Qué cambia respecto de Pagatu?** El patrón distribuido es el mismo, pero el dominio es propio: catálogo de cursos e inscripciones con progreso.

## Repartición sugerida del tiempo

- 1 a 3 minutos: pitch breve del equipo.
- 8 minutos: explicación técnica del integrante.
- 5 minutos: demo CRUD, error, registro y balanceo.
- 5 minutos: preguntas individuales.

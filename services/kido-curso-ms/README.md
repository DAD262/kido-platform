# kido-curso-ms

Microservicio de Adan para administrar el catálogo de cursos gratuitos y pagados.

## Recursos

- Categoria.
- Curso.
- Modulo.
- Leccion.

## CRUD evaluable

| Método | Ruta |
|---|---|
| GET | `/api/v1/cursos` |
| GET | `/api/v1/cursos/{id}` |
| POST | `/api/v1/cursos` |
| PUT | `/api/v1/cursos/{id}` |
| DELETE | `/api/v1/cursos/{id}` |

La configuración proviene de Kido Config Server. La base DEV se inicia con `docker compose -f compose-dev.yml up -d`.

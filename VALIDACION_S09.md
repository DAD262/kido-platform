# KIDO — Validación de la actualización S09

Revisión del ZIP KIDO_REVISION_S09_20261014-211747. Se conserva el frontend existente y el avance de ambos integrantes.

## Verificado en este entorno

- Inspección del código Saga y contraste con el manual adjunto.
- Auditoría estática: 7 POM, 29 YAML y 146 archivos Java; sin errores detectados por el auditor existente. No sustituye la compilación.
- Cambio de comportamiento acotado a AcademicoResultadoConsumer: filtra tipos ajenos a la Saga antes de validar ordenId.
- Cinco pruebas adicionales escritas para el filtro/enrutamiento y la reversión única del movimiento de venta.
- Script de demo ampliado con pausas, consultas PostgreSQL y búsqueda de logs por ventana temporal.
- Matriz de rúbrica, guía de capturas e informe base. Diagrama ajustado a las transiciones implementadas.
- Paquete incremental con hashes de origen/destino y respaldo previo a la copia.
- Sintaxis PowerShell comprobada con su parser para el instalador y la demo.
- Instalador ejecutado con PowerShell en Linux sobre copias temporales: instalación completa, segunda ejecución sin cambios y rechazo de un archivo editado después del ZIP sin modificar ningún archivo. Falta ejecutarlo en Windows del estudiante.
- Comparación por hashes: frontend, infraestructura y migraciones idénticos a los recibidos.

## Pendiente de ejecución real

Se intentó `mvnw -B -ntp test` con JDK 21. Maven se detuvo antes de compilar porque no pudo resolver repo.maven.apache.org al descargar el parent Spring Boot 4.0.7. Esto no demuestra un error de compilación ni prueba que los tests pasen.

No hay Docker disponible en este entorno. Ejecutar las pruebas con los contenedores locales del estudiante. Las consultas y resultados esperados del guion no son resultados ya obtenidos.

```powershell
Set-Location 'D:\KIDO 2\kido-platform\services\kido-pago-ms'
.\mvnw.cmd -B -ntp test
```

Si Maven termina correctamente, seguir docs/s09-guia-capturas.md para reconstruir únicamente Pago y ejecutar la demo con pausas. Si la configuración de fallo de Inscripción cambió, recrear también ese servicio.

## Condiciones para cerrar la entrega

- Evidencias reales KIDO: duplicado ignorado, fallo compensado, JSON de compensación repetido, misma orden/movimiento e índices UNIQUE reales.
- Evidencias PAGATU obligatorias según el manual, que no está incluido en este ZIP.
- Número de equipo, GitHub/commit y aporte individual verificable.
- Reflexión revisada y feedback personal.

El PDF no se presenta como terminado antes de incorporar esas evidencias. No se alteran migraciones aplicadas, .env, datos, dependencias ni frontend. Kafka UI es la herramienta ya presente; Axon no se ha integrado.

## Validación real posterior — 14/10/2026

- Maven: BUILD SUCCESS; 8 pruebas, 0 fallos, 0 errores.
- Orden 15: APROBADA, inscripción sincronizada y una sola inscripción tras repetir el evento.
- Orden 16: REEMBOLSADA, sin inscripción y venta 16 REVERTIDO por S/ 71.91.
- Reenvío desde Kafka UI: partición 2, offset 2; consumidor status=ignored.
- PostgreSQL confirmó que el reenvío conservó la misma orden y un único movimiento de venta.
- Estas comprobaciones completan las pruebas principales de KIDO; el informe final y los requisitos restantes de la rúbrica siguen pendientes.

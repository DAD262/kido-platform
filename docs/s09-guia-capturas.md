# S09 — Pruebas reales y capturas con KIDO

## 1. Instalar y compilar

Cerrar los archivos de código con cambios sin guardar en VS Code (guardar primero).
Desde PowerShell, ejecutar el instalador del ZIP con -Raiz 'D:\KIDO 2\kido-platform'. El instalador compara hashes antes de modificar, respalda los archivos afectados y conserva .env, frontend, migraciones, volúmenes e historial Git.

```powershell
Set-Location 'D:\KIDO 2\kido-platform'
Push-Location .\services\kido-pago-ms
try {
    .\mvnw.cmd -B -ntp test
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de Pago' }
} finally { Pop-Location }
```

No se exige recompilar todos los módulos para probar este cambio. Si faltan dependencias, Maven necesita descargarlas. Conservar el resultado real de Surefire; un test escrito no equivale a un test aprobado.

En el .env existente comprobar estas dos variables; no reemplazarlo por .env.example:

```dotenv
KIDO_PAGOS_PERMITIR_SIMULACION=true
KIDO_SAGA_ESTUDIANTE_FALLO_DEMO=999999
```

Si ya están así, reconstruir solamente Pago:

```powershell
docker compose up -d --build --no-deps kido-pago-ms
```

Si cambió la variable de fallo, recrear también Inscripción con `docker compose up -d --no-deps kido-inscripcion-ms`.
Verificar salud/arranque en los logs antes de iniciar la demo.

## 2. Prueba con pausas

Usar un curso existente de tipo PAGO, estado PUBLICADO y precio mayor a cero. El valor 2 es el valor predeterminado; si tu curso tiene otro ID, cambiar -CursoId.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo-s9-saga.ps1 -CursoId 2 -PausarCapturas
```

La cuenta admin del realm de demostración es la usada por el script existente. Si cambiaste la contraseña, establecer KIDO_DEMO_ADMIN_PASSWORD solo en esa sesión o introducirla sin imprimirla. No enviar tokens ni contraseñas en capturas.

Cada pausa permite capturar pantalla con Win+Impr Pant o la herramienta de capturas en modo pantalla completa. Mantener visible la barra de tareas con fecha/hora y el perfil de Windows, VS Code o navegador; el nombre impreso en la consola sirve de apoyo, no reemplaza el perfil exigido. Si hace falta, poner VS Code y navegador lado a lado y usar el perfil del navegador.

| Captura | Resultado que debe verse |
|---|---|
| 01 | Servicios y definición de índices UNIQUE reales de inscripciones |
| 02 | Orden APROBADA, sincronizada, una inscripción y una venta PENDIENTE |
| 03 | Nuevo log ignored para la orden normal; una inscripción y misma venta |
| 04 | inscripcion.fallida publicada, compensated consumido, orden REEMBOLSADA, cero inscripciones por esa orden y venta REVERTIDO |
| 05 | Nueva compensación ignored; venta con mismo ID/importe, sin segundo movimiento |
| 06 | Grupos consumidores y lag por partición; si no es cero, esperar y volver a consultar |

El script crea dos compras simuladas y conserva sus registros para trazabilidad. No ejecuta pagos externos ni borra datos. La etapa 4 repite el evento de pago y provoca otra solicitud de compensación sobre la misma orden: esto comprueba idempotencia de negocio. El siguiente paso demuestra adicionalmente la reentrega del JSON exacto del resultado original.

## 3. Repetir exactamente inscripcion.fallida en Kafka UI

1. Abrir http://localhost:18085 y seleccionar kido-dev.
2. Topics → kido-academico-eventos → Messages. Buscar el ordenId compensado que imprimió la demo.
3. Abrir el mensaje cuyo tipoEvento es inscripcion.fallida, origen kido-inscripcion-ms. Capturar key, topic, partition, offset y JSON completo. Copiar el JSON original, incluyendo timestamp y motivo; anotar partition/offset.
4. Antes del reenvío, en otra consola situada en la raíz ejecutar:

   ```powershell
   $desde = (Get-Date).ToUniversalTime().ToString('o')
   ```

5. Usar Produce Message en el mismo topic. Key: el mismo ordenId como texto. Value: el JSON íntegro copiado. Conservar el formato JSON/String que permita la UI; si se ofrecen headers, mantener los originales. Enviar una sola vez.
6. Volver a Messages, comprobar que hay otra entrada con el mismo key/value y distinto offset. Capturar ambas entradas y sus detalles.
7. En la consola ejecutar:

   ```powershell
   docker compose logs --no-color --since $desde kido-pago-ms
   ```

   Debe aparecer `eventType=inscripcion.fallida ordenId=TU_ID status=ignored`. Capturar el log reciente.
8. Comprobar la misma orden y su movimiento (reemplazar 123 por el ID real):

   ```powershell
   $id = 123
   "SELECT id,estado,inscripcion_sincronizada FROM ordenes_compra WHERE id=$id; SELECT id,orden_id,tipo,estado,monto FROM movimientos_saldo WHERE orden_id=$id ORDER BY id;" |
       docker compose exec -T postgres-pago sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
   ```

   La orden debe seguir REEMBOLSADA y debe existir una única VENTA REVERTIDO con el mismo ID y monto de la captura anterior. No interpretar una segunda línea JSON como una segunda compensación.

## 4. Regresión académica y aporte

La corrección del consumidor tiene pruebas para curso.completado/certificado.disponible sin ordenId, resultado inválido de Saga, éxito y fallo válidos. Para evidencia de integración, completar una lección/curso mediante los flujos existentes y comprobar que Pago continúa consumiendo eventos posteriores de Saga. No publicar estados académicos falsos para simular un curso terminado.

Revisar y registrar los cambios reales en Git, sin añadir contraseñas:

```powershell
git status --short
git diff --stat
git config user.name
git log -3 --date=iso --format='%h | %ad | %an | %s'
```

El instalador no crea commits ni hace push. Añadir selectivamente los archivos tras validar y usar la cuenta del autor real. El manual exige trazabilidad de commits y coherencia con las capturas, no una firma criptográfica obligatoria.

## 5. Lo que falta para cerrar el PDF

- Las evidencias reales anteriores y las de PAGATU exigidas por la rúbrica.
- Número de equipo y enlace GitHub.
- Hallazgo técnico verificado y resultado de las pruebas.
- Respuestas personales del anexo.

No se modifica una base activa para provocar el bug original de PAGATU. Si no existe la captura anterior al fix, reproducirlo en una copia de laboratorio de PAGATU. Un error de índice generado con SQL o un test con Mockito no sustituye la excepción real pedida al consumidor.

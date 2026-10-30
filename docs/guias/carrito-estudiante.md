# Carrito del estudiante: una compra conjunta

El carrito conserva hasta 20 cursos de pago por perfil en PostgreSQL. Cada curso tiene cantidad uno. El backend resuelve el perfil autenticado y valida que el conjunto solicitado coincida exactamente con el carrito. Obtiene precios y condiciones del catálogo; el navegador únicamente confirma el total revisado.

`POST /api/v1/pagos/carrito/checkout` crea una compra principal con referencia `CARRITO-{id}` y una preferencia de Mercado Pago con todos los cursos. Repetir la solicitud con el mismo carrito, precios, cuenta y ambiente reutiliza la compra y la preferencia pendientes. Los cursos gratuitos, retirados, no publicados o ya comprados bloquean la compra; no se omiten silenciosamente para cobrar otro total.

El total es la suma de las líneas. Cada acceso mantiene una orden por curso, su docente y la instantánea de lecciones, pero comparte una sola operación de pago con la compra principal. La base permite compartir un ID de operación únicamente mediante la relación con esa compra; conserva la unicidad de los pagos individuales y no reclasifica órdenes históricas.

La confirmación y el webhook consultan el pago en Mercado Pago y validan estado, referencia, receptor, ambiente, moneda, importe y suma del detalle. Una aprobación transaccional publica un evento por curso después del commit. Los eventos y sus resultados académicos conservan compra conjunta, operación, ambiente y receptor; la inscripción mantiene su idempotencia por orden.

PRUEBAS y DESCONOCIDO no generan movimientos ni liquidaciones reales. En PRODUCCION, costos y neto se distribuyen en céntimos entre los accesos; el último recibe el residuo, de modo que la suma siempre coincide con el proveedor. Los reembolsos por curso usan la misma operación con el importe de esa línea e idempotencia propia. Una devolución parcial bloquea los fondos afectados para revisión conforme a las reglas existentes.

La constancia conjunta se consulta desde cualquiera de sus accesos, siempre comprobando al comprador o administrador autorizado. Contiene una operación, un total y el detalle de los cursos, docentes e importes. El PDF agrega páginas de detalle cuando es necesario. Las constancias individuales anteriores siguen funcionando.

El frontend muestra un resumen compacto, el total y un único botón Pagar. No tiene selector de curso ni botones para pagar cada fila. Fuera del carrito, la compra individual del detalle del curso conserva su flujo.

La excepción comercial local sigue desactivada por defecto y requiere cuenta de prueba verificada. Producción conserva sus condiciones comerciales y de seguridad. No se publican datos comerciales ni se ejecutan cobros para comprobar la integración.
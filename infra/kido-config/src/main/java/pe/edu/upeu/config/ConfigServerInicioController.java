package pe.edu.upeu.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigServerInicioController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String inicio() {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>KIDO Config Server</title>
                  <style>
                    body{margin:0;background:#f4f7fc;color:#111827;font-family:Segoe UI,Arial,sans-serif}
                    main{max-width:820px;margin:8vh auto;padding:42px;background:#fff;border:1px solid #dfe5ef;border-radius:20px;box-shadow:0 18px 45px #23387618}
                    .mark{display:inline-grid;place-items:center;width:46px;height:46px;border-radius:13px;background:linear-gradient(135deg,#12b7ef,#2448ed);color:#fff;font-weight:900;font-size:25px}
                    h1{font-size:38px;margin:18px 0 6px}p{color:#5d6676;line-height:1.65}.ok{color:#087f5b;font-weight:800}
                    .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:13px;margin-top:28px}
                    a{display:block;padding:16px;border:1px solid #dfe5ef;border-radius:12px;color:#2448ed;text-decoration:none;font-weight:700}a:hover{background:#eef3ff}
                    code{display:block;margin-top:5px;color:#687386;font-size:12px;font-weight:500;overflow-wrap:anywhere}@media(max-width:650px){main{margin:0;padding:25px;min-height:100vh;border-radius:0}.grid{grid-template-columns:1fr}}
                  </style>
                </head>
                <body><main>
                  <div class="mark">K</div>
                  <h1>KIDO Config Server</h1>
                  <p class="ok">● Servicio activo</p>
                  <p>Distribuye la configuración centralizada para la infraestructura y los microservicios de KIDO.</p>
                  <div class="grid">
                    <a href="/actuator/health">Estado del servicio<code>/actuator/health</code></a>
                    <a href="/kido-gateway/prod">Gateway PROD<code>/kido-gateway/prod</code></a>
                    <a href="/kido-pago-ms/prod">Pagos PROD<code>/kido-pago-ms/prod</code></a>
                    <a href="/kido-curso-ms/prod">Cursos PROD<code>/kido-curso-ms/prod</code></a>
                  </div>
                </main></body></html>
                """;
    }
}

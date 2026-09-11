package pe.edu.upeu.curso.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String trace = req.getHeader("X-Trace-ID");
        if (trace == null || trace.isBlank()) trace = UUID.randomUUID().toString();
        String instance = System.getenv().getOrDefault("HOSTNAME", "local") + ":" + req.getLocalPort();
        try {
            MDC.put("traceId", trace);
            res.setHeader("X-Trace-ID", trace);
            res.setHeader("X-Instance-ID", "kido-curso-ms:" + instance);
            chain.doFilter(req, res);
            LoggerFactory.getLogger(getClass()).info("instance={} {} {} status={}", instance, req.getMethod(), req.getRequestURI(), res.getStatus());
        } finally { MDC.clear(); }
    }
}

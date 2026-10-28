package pe.edu.upeu.usuario.support;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana deslizante en memoria para frenar registros automatizados desde una misma IP.
 * Basta para una instancia; con varias réplicas conviene moverlo a Redis o al Gateway.
 */
public class LimitadorIntentos {

    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();

    public LimitadorIntentos(int maximo, Duration ventana, Clock reloj) {
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /** Registra un intento y devuelve false si la clave ya superó el máximo dentro de la ventana. */
    public boolean permitir(String clave) {
        long ahora = reloj.millis();
        long limite = ahora - ventana.toMillis();
        Deque<Long> cola = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst() <= limite) {
                cola.pollFirst();
            }
            if (cola.size() >= maximo) {
                return false;
            }
            cola.addLast(ahora);
        }
        if (intentos.size() > 10_000) {
            intentos.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || e.getValue().peekLast() <= limite;
                }
            });
        }
        return true;
    }
}

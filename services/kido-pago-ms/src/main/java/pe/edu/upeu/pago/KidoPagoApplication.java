package pe.edu.upeu.pago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidoPagoApplication {
 public static void main(String[] args){ SpringApplication.run(KidoPagoApplication.class,args); }
}

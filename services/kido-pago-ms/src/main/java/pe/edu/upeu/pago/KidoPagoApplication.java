package pe.edu.upeu.pago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "pe.edu.upeu.pago.client")
public class KidoPagoApplication {
 public static void main(String[] args){ SpringApplication.run(KidoPagoApplication.class,args); }
}

package pe.edu.upeu.pago.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;
public record PagoLiquidacionRequest(@NotBlank @Size(max=120) String banco,@NotBlank @Size(max=120) String codigoOperacion,@NotNull LocalDate fechaOperacion,@NotBlank @Size(max=160) String destino,@NotBlank @Size(max=180) String titular,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal monto,@AssertTrue boolean verificada) {}

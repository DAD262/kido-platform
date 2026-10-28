package pe.edu.upeu.pago.service;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import pe.edu.upeu.pago.repository.BloqueoSaldoRepository;
@Service @RequiredArgsConstructor public class BloqueoSaldoService {private final BloqueoSaldoRepository repo;@Transactional(propagation=Propagation.MANDATORY) public void bloquear(Long id){repo.asegurar(id);repo.bloquear(id).orElseThrow();}}

package br.edu.exemplo.controleeventos.repository;

import br.edu.exemplo.controleeventos.model.Evento;
import br.edu.exemplo.controleeventos.model.StatusEvento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Parte 07: eventos com o status informado a partir de uma data, em ordem cronológica. */
    List<Evento> findByStatusAndDataGreaterThanEqualOrderByDataAscHoraAsc(
            StatusEvento status, LocalDate data);
}

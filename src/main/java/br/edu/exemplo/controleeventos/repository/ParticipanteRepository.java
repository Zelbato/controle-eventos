package br.edu.exemplo.controleeventos.repository;

import br.edu.exemplo.controleeventos.model.Participante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    /** Parte 11: evita criar um segundo participante com o mesmo e-mail. */
    Optional<Participante> findByEmailIgnoreCase(String email);
}

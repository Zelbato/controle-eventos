package br.edu.exemplo.controleeventos.repository;

import br.edu.exemplo.controleeventos.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Parte 09: usado pelo login (UsuarioDetailsService). */
    Optional<Usuario> findByEmail(String email);

    /** Parte 10: verificação de e-mail duplicado no cadastro público. */
    boolean existsByEmailIgnoreCase(String email);

    /** Usado para impedir a exclusão de um participante ligado a uma conta. */
    boolean existsByParticipanteId(Long participanteId);
}

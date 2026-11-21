package br.edu.exemplo.controleeventos.repository;

import br.edu.exemplo.controleeventos.model.Inscricao;
import br.edu.exemplo.controleeventos.model.StatusInscricao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    /** Total histórico de inscrições do evento (ativas + canceladas). */
    long countByEventoId(Long eventoId);

    /** Parte 12: somente inscrições ATIVAS ocupam vaga. */
    long countByEventoIdAndStatus(Long eventoId, StatusInscricao status);

    /** Parte 12: duplicidade considera apenas inscrições ATIVAS. */
    boolean existsByEventoIdAndParticipanteIdAndStatus(
            Long eventoId, Long participanteId, StatusInscricao status);

    /** Parte 12: indicador de inscrições ativas no relatório. */
    long countByStatus(StatusInscricao status);

    List<Inscricao> findByEventoId(Long eventoId);

    /** Parte 11: inscrições da própria conta, mais recentes primeiro. */
    List<Inscricao> findByParticipanteIdOrderByDataInscricaoDesc(Long participanteId);

    boolean existsByEventoId(Long eventoId);

    boolean existsByParticipanteId(Long participanteId);

    /** Parte 12 (item 4): registros antigos criados antes da coluna status. */
    @Transactional
    @Modifying
    @Query("UPDATE Inscricao i SET i.status = br.edu.exemplo.controleeventos.model.StatusInscricao.ATIVA "
            + "WHERE i.status IS NULL")
    int preencherStatusNulo();
}

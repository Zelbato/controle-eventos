package br.edu.exemplo.controleeventos.service;

import br.edu.exemplo.controleeventos.model.Evento;
import br.edu.exemplo.controleeventos.model.StatusEvento;
import br.edu.exemplo.controleeventos.repository.EventoRepository;
import br.edu.exemplo.controleeventos.repository.InscricaoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private final EventoRepository repository;
    private final InscricaoRepository inscricaoRepository;

    public EventoService(EventoRepository repository, InscricaoRepository inscricaoRepository) {
        this.repository = repository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public Evento salvar(Evento evento) {
        return repository.save(evento);
    }

    public List<Evento> listarTodos() {
        return repository.findAll();
    }

    public Evento buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * Parte 07: eventos ABERTOS que ainda não começaram.
     * Usado pelo catálogo do participante (Parte 11).
     */
    @Transactional(readOnly = true)
    public List<Evento> listarAbertosFuturos() {
        LocalDateTime agora = LocalDateTime.now();
        return repository
                .findByStatusAndDataGreaterThanEqualOrderByDataAscHoraAsc(
                        StatusEvento.ABERTO, LocalDate.now())
                .stream()
                .filter(e -> LocalDateTime.of(e.getData(), e.getHora()).isAfter(agora))
                .toList();
    }

    /**
     * Um evento com inscrições (ativas ou canceladas) não é apagado,
     * para preservar o histórico. Nesse caso, altere o status para CANCELADO.
     */
    public void excluir(Long id) {
        if (inscricaoRepository.existsByEventoId(id)) {
            throw new IllegalStateException(
                    "Este evento possui inscrições e não pode ser excluído. "
                    + "Altere o status para CANCELADO.");
        }
        repository.deleteById(id);
    }
}

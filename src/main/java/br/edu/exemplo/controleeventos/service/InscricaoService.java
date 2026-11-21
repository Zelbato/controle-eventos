package br.edu.exemplo.controleeventos.service;

import br.edu.exemplo.controleeventos.model.*;
import br.edu.exemplo.controleeventos.repository.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public InscricaoService(InscricaoRepository inscricaoRepository,
            EventoRepository eventoRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Parte 05: regras de inscrição (evento aberto, duplicidade e vagas).
     * Parte 12: duplicidade e vagas consideram somente inscrições ATIVAS.
     */
    @Transactional
    public Inscricao salvar(Inscricao inscricao) {
        if (inscricao.getEvento() == null || inscricao.getEvento().getId() == null) {
            throw new IllegalStateException("Selecione um evento.");
        }
        if (inscricao.getParticipante() == null || inscricao.getParticipante().getId() == null) {
            throw new IllegalStateException("Selecione um participante.");
        }
        Evento evento = eventoRepository.findById(inscricao.getEvento().getId())
                .orElseThrow(() -> new IllegalStateException("Evento não encontrado."));
        Participante participante = participanteRepository.findById(inscricao.getParticipante().getId())
                .orElseThrow(() -> new IllegalStateException("Participante não encontrado."));

        if (evento.getStatus() != StatusEvento.ABERTO) {
            throw new IllegalStateException("Este evento não está aberto para inscrições.");
        }

        boolean jaInscrito = inscricaoRepository.existsByEventoIdAndParticipanteIdAndStatus(
                evento.getId(), participante.getId(), StatusInscricao.ATIVA);
        if (jaInscrito) {
            throw new IllegalStateException("Já existe inscrição ativa deste participante neste evento.");
        }

        long totalInscritos = inscricaoRepository.countByEventoIdAndStatus(
                evento.getId(), StatusInscricao.ATIVA);
        if (totalInscritos >= evento.getQuantidadeVagas()) {
            throw new IllegalStateException("Não há vagas disponíveis neste evento.");
        }

        inscricao.setEvento(evento);
        inscricao.setParticipante(participante);
        inscricao.setStatus(StatusInscricao.ATIVA);
        inscricao.setDataCancelamento(null);
        inscricao.setDataInscricao(LocalDateTime.now());
        return inscricaoRepository.save(inscricao);
    }

    /**
     * Parte 11: inscrição feita pelo próprio usuário autenticado.
     * O participante vem da conta logada, nunca do formulário.
     */
    @Transactional
    public Inscricao inscreverUsuario(Long eventoId, String email) {
        Participante participante = participanteDaConta(email);

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new IllegalStateException("Evento não encontrado."));

        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setParticipante(participante);
        return salvar(inscricao); // reutiliza as regras da Parte 05
    }

    /** Parte 11: inscrições (ativas e canceladas) apenas da conta autenticada. */
    @Transactional(readOnly = true)
    public List<Inscricao> listarDoUsuario(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
        if (usuario.getParticipante() == null) {
            return List.of();
        }
        return inscricaoRepository.findByParticipanteIdOrderByDataInscricaoDesc(
                usuario.getParticipante().getId());
    }

    /**
     * Parte 12: cancela somente a inscrição da própria conta, antes do início
     * do evento. O registro permanece no banco com status CANCELADA.
     */
    @Transactional
    public void cancelarMinhaInscricao(Long inscricaoId, String email) {
        Participante participante = participanteDaConta(email);

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new IllegalStateException("Inscrição não encontrada."));

        if (!participante.getId().equals(inscricao.getParticipante().getId())) {
            throw new IllegalStateException("Você não pode cancelar outra inscrição.");
        }
        if (inscricao.getStatus() != StatusInscricao.ATIVA) {
            throw new IllegalStateException("Esta inscrição já está cancelada.");
        }

        Evento evento = inscricao.getEvento();
        LocalDateTime inicio = LocalDateTime.of(evento.getData(), evento.getHora());
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Não é possível cancelar após o início do evento.");
        }

        inscricao.setStatus(StatusInscricao.CANCELADA);
        inscricao.setDataCancelamento(LocalDateTime.now());
        // @Transactional grava a alteração ao final; não há deleteById().
    }

    /**
     * Mapa eventoId -> id da inscrição ATIVA da conta autenticada.
     * Usado no catálogo e no detalhe para mostrar "Inscrito" e o botão Cancelar.
     * Retorna vazio se a conta não existir ou não tiver participante vinculado.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> inscricoesAtivasPorEvento(String email) {
        Map<Long, Long> mapa = new HashMap<>();
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            if (usuario.getParticipante() == null) {
                return;
            }
            for (Inscricao i : inscricaoRepository.findByParticipanteIdOrderByDataInscricaoDesc(
                    usuario.getParticipante().getId())) {
                if (i.getStatus() == StatusInscricao.ATIVA) {
                    mapa.put(i.getEvento().getId(), i.getId());
                }
            }
        });
        return mapa;
    }

    public List<Inscricao> listarTodas() {
        return inscricaoRepository.findAll();
    }

    public List<Inscricao> listarPorEvento(Long eventoId) {
        return inscricaoRepository.findByEventoId(eventoId);
    }

    /** Parte 12: inscritos que ocupam vaga (somente ATIVAS). */
    public long contarPorEvento(Long eventoId) {
        return inscricaoRepository.countByEventoIdAndStatus(eventoId, StatusInscricao.ATIVA);
    }

    /** Parte 12: card "Inscrições ativas". */
    public long contarAtivas() {
        return inscricaoRepository.countByStatus(StatusInscricao.ATIVA);
    }

    /** Total histórico (ativas + canceladas). */
    public long contarTodas() {
        return inscricaoRepository.count();
    }

    /** Exclusão física: operação administrativa (ADMIN). */
    public void excluir(Long id) {
        inscricaoRepository.deleteById(id);
    }

    private Participante participanteDaConta(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
        if (usuario.getParticipante() == null) {
            throw new IllegalStateException("Esta conta não possui participante vinculado.");
        }
        return usuario.getParticipante();
    }
}

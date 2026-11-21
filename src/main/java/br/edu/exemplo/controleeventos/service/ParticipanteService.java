package br.edu.exemplo.controleeventos.service;

import br.edu.exemplo.controleeventos.model.Participante;
import br.edu.exemplo.controleeventos.repository.InscricaoRepository;
import br.edu.exemplo.controleeventos.repository.ParticipanteRepository;
import br.edu.exemplo.controleeventos.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParticipanteService {

    private final ParticipanteRepository repository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ParticipanteService(ParticipanteRepository repository,
            InscricaoRepository inscricaoRepository,
            UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Participante salvar(Participante participante) {
        return repository.save(participante);
    }

    public List<Participante> listarTodos() {
        return repository.findAll();
    }

    public Participante buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    /** Não apaga quem possui inscrições ou conta de acesso vinculada. */
    public void excluir(Long id) {
        if (inscricaoRepository.existsByParticipanteId(id)) {
            throw new IllegalStateException(
                    "Este participante possui inscrições e não pode ser excluído.");
        }
        if (usuarioRepository.existsByParticipanteId(id)) {
            throw new IllegalStateException(
                    "Este participante está vinculado a uma conta de acesso e não pode ser excluído.");
        }
        repository.deleteById(id);
    }
}

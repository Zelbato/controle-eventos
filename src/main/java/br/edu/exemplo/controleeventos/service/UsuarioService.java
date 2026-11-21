package br.edu.exemplo.controleeventos.service;

import br.edu.exemplo.controleeventos.dto.CadastroUsuarioForm;
import br.edu.exemplo.controleeventos.model.Participante;
import br.edu.exemplo.controleeventos.model.PerfilUsuario;
import br.edu.exemplo.controleeventos.model.Usuario;
import br.edu.exemplo.controleeventos.repository.ParticipanteRepository;
import br.edu.exemplo.controleeventos.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parte 10: cadastro público de contas, sempre com perfil PARTICIPANTE.
 * Parte 11: cria o Participante junto e faz o vínculo com o Usuario.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            ParticipanteRepository participanteRepository,
            PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.participanteRepository = participanteRepository;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario cadastrar(CadastroUsuarioForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        // Parte 11: não vincula automaticamente a um participante antigo só pelo e-mail.
        if (participanteRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException(
                    "Já existe um participante com este e-mail. Procure a organização do evento.");
        }

        Participante participante = new Participante();
        participante.setNome(form.getNome().trim());
        participante.setEmail(email);
        participante.setTelefone(form.getTelefone().trim());
        participante.setDocumento(form.getDocumento().trim());
        participante = participanteRepository.save(participante);

        Usuario usuario = new Usuario();
        usuario.setNome(form.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode(form.getSenha()));
        usuario.setPerfil(PerfilUsuario.PARTICIPANTE); // o visitante nunca escolhe o perfil
        usuario.setParticipante(participante);
        return usuarioRepository.save(usuario);
    }
}

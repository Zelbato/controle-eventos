package br.edu.exemplo.controleeventos.config;

import br.edu.exemplo.controleeventos.model.Participante;
import br.edu.exemplo.controleeventos.model.PerfilUsuario;
import br.edu.exemplo.controleeventos.model.Usuario;
import br.edu.exemplo.controleeventos.repository.InscricaoRepository;
import br.edu.exemplo.controleeventos.repository.ParticipanteRepository;
import br.edu.exemplo.controleeventos.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Parte 09: cria o primeiro administrador (e uma conta de teste) ao iniciar.
 * Parte 12: preenche status ATIVA em inscrições antigas sem status.
 *
 * As senhas padrão servem apenas para a aula. Em um sistema real, use
 * senhas fortes definidas por variáveis de ambiente.
 */
@Configuration
public class CargaInicial {

    @Bean
    CommandLineRunner criarDadosIniciais(
            UsuarioRepository usuarioRepository,
            ParticipanteRepository participanteRepository,
            InscricaoRepository inscricaoRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminSenha,
            @Value("${app.carga.participante-teste:true}") boolean criarParticipanteTeste) {

        return args -> {
            String emailAdmin = adminEmail.trim().toLowerCase(Locale.ROOT);
            if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setEmail(emailAdmin);
                admin.setSenha(passwordEncoder.encode(adminSenha));
                admin.setPerfil(PerfilUsuario.ADMIN);
                usuarioRepository.save(admin);
            }

            String emailTeste = "participante@eventos.com";
            if (criarParticipanteTeste && usuarioRepository.findByEmail(emailTeste).isEmpty()) {
                Participante pessoa = participanteRepository.findByEmailIgnoreCase(emailTeste)
                        .orElseGet(() -> {
                            Participante p = new Participante();
                            p.setNome("Participante Teste");
                            p.setEmail(emailTeste);
                            p.setTelefone("(00) 00000-0000");
                            p.setDocumento("000.000.000-00");
                            return participanteRepository.save(p);
                        });

                Usuario participante = new Usuario();
                participante.setNome("Participante Teste");
                participante.setEmail(emailTeste);
                participante.setSenha(passwordEncoder.encode("teste123"));
                participante.setPerfil(PerfilUsuario.PARTICIPANTE);
                participante.setParticipante(pessoa);
                usuarioRepository.save(participante);
            }

            inscricaoRepository.preencherStatusNulo();
        };
    }
}

package br.edu.exemplo.controleeventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.exemplo.controleeventos.dto.CadastroUsuarioForm;
import br.edu.exemplo.controleeventos.model.*;
import br.edu.exemplo.controleeventos.repository.*;
import br.edu.exemplo.controleeventos.service.InscricaoService;
import br.edu.exemplo.controleeventos.service.UsuarioService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes baseados nos checklists dos Tutoriais 08 a 12.
 * Executar: mvn test (usa banco H2 em memória, não precisa do PostgreSQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ControleEventosTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @Autowired InscricaoService inscricaoService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EventoRepository eventoRepository;
    @Autowired InscricaoRepository inscricaoRepository;
    @Autowired PasswordEncoder encoder;

    // ---------------- Parte 08: login e proteção de rotas ----------------

    @Test
    void rotasAdministrativasExigemLogin() throws Exception {
        for (String rota : new String[]{"/", "/eventos", "/participantes", "/inscricoes", "/relatorios"}) {
            mvc.perform(get(rota)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void paginasPublicasAbremSemLogin() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/cadastro")).andExpect(status().isOk());
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
    }

    @Test
    void loginDoAdministradorAbreDashboard() throws Exception {
        mvc.perform(formLogin("/login").user("admin@eventos.com").password("admin123"))
                .andExpect(redirectedUrl("/"));
        // Parte 10: e-mail em maiúsculas também funciona
        mvc.perform(formLogin("/login").user("ADMIN@EVENTOS.COM").password("admin123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void senhaIncorretaVoltaParaLoginComErro() throws Exception {
        mvc.perform(formLogin("/login").user("admin@eventos.com").password("errada"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutPorPostEncerraSessao() throws Exception {
        mvc.perform(post("/logout").with(user("admin@eventos.com").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void exclusaoNaoFuncionaPorGetNemSemCsrf() throws Exception {
        var admin = user("admin@eventos.com").roles("ADMIN");
        mvc.perform(get("/eventos/excluir/999").with(admin)).andExpect(status().is4xxClientError());
        mvc.perform(post("/eventos/excluir/999").with(admin)).andExpect(status().isForbidden());
    }

    // ---------------- Parte 09: usuários no banco e perfis ----------------

    @Test
    void administradorFoiCriadoComSenhaBcrypt() {
        Usuario admin = usuarioRepository.findByEmail("admin@eventos.com").orElseThrow();
        assertThat(admin.getPerfil()).isEqualTo(PerfilUsuario.ADMIN);
        assertThat(admin.getSenha()).isNotEqualTo("admin123").startsWith("$2");
        assertThat(encoder.matches("admin123", admin.getSenha())).isTrue();
    }

    @Test
    void participanteNaoAcessaRotasAdministrativas() throws Exception {
        var participante = user("participante@eventos.com").roles("PARTICIPANTE");
        for (String rota : new String[]{"/eventos/novo", "/eventos", "/participantes", "/inscricoes", "/relatorios"}) {
            mvc.perform(get(rota).with(participante)).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminNaoUsaAreaDoParticipante() throws Exception {
        mvc.perform(get("/catalogo").with(user("admin@eventos.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // ---------------- Parte 10: cadastro público ----------------

    @Test
    void cadastroValidaCamposESenhas() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "").param("email", "invalido")
                        .param("telefone", "").param("documento", "")
                        .param("senha", "123").param("confirmacaoSenha", "456"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro-usuario"))
                .andExpect(model().attributeHasFieldErrors("cadastro",
                        "nome", "email", "telefone", "documento", "senha", "confirmacaoSenha"));
    }

    @Test
    void cadastroCriaParticipanteVinculadoEBloqueiaDuplicado() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Ana Souza").param("email", "Ana@Exemplo.com")
                        .param("telefone", "(11) 99999-0000").param("documento", "123")
                        .param("senha", "senhaSegura1").param("confirmacaoSenha", "senhaSegura1"))
                .andExpect(redirectedUrl("/login?cadastro"));

        Usuario ana = usuarioRepository.findByEmail("ana@exemplo.com").orElseThrow();
        assertThat(ana.getPerfil()).isEqualTo(PerfilUsuario.PARTICIPANTE);
        assertThat(ana.getSenha()).isNotEqualTo("senhaSegura1");
        assertThat(ana.getParticipante()).isNotNull();          // Parte 11
        assertThat(ana.getParticipante().getEmail()).isEqualTo("ana@exemplo.com");

        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Outra Ana").param("email", "ana@exemplo.com")
                        .param("telefone", "1").param("documento", "2")
                        .param("senha", "senhaSegura1").param("confirmacaoSenha", "senhaSegura1"))
                .andExpect(view().name("cadastro-usuario"))
                .andExpect(model().attributeHasFieldErrors("cadastro", "email"));

        mvc.perform(formLogin("/login").user("ana@exemplo.com").password("senhaSegura1"))
                .andExpect(redirectedUrl("/"));
    }

    // ---------------- Partes 11 e 12: inscrição, vagas e cancelamento ----------------

    @Test
    void fluxoCompletoDeInscricaoECancelamento() throws Exception {
        criarConta("conta.a@teste.com");
        criarConta("conta.b@teste.com");
        Evento evento = criarEvento(1, LocalDate.now().plusDays(10));

        // A se inscreve pelo catálogo (sem enviar participante)
        mvc.perform(post("/inscricoes/minha").param("eventoId", evento.getId().toString())
                        .with(user("conta.a@teste.com").roles("PARTICIPANTE")).with(csrf()))
                .andExpect(redirectedUrl("/meus-eventos"))
                .andExpect(flash().attribute("sucesso", "Inscrição realizada!"));

        // Repetir inscrição: bloqueado (Parte 05)
        assertThatThrownBy(() -> inscricaoService.inscreverUsuario(evento.getId(), "conta.a@teste.com"))
                .hasMessageContaining("inscrição ativa");

        // B não ocupa a vaga já usada
        mvc.perform(post("/inscricoes/minha").param("eventoId", evento.getId().toString())
                        .with(user("conta.b@teste.com").roles("PARTICIPANTE")).with(csrf()))
                .andExpect(redirectedUrl("/catalogo"))
                .andExpect(flash().attribute("erro", "Não há vagas disponíveis neste evento."));

        // B não vê as inscrições de A
        assertThat(inscricaoService.listarDoUsuario("conta.b@teste.com")).isEmpty();
        Inscricao deA = inscricaoService.listarDoUsuario("conta.a@teste.com").get(0);

        // B não pode cancelar a inscrição de A (POST forjado)
        mvc.perform(post("/minhas-inscricoes/" + deA.getId() + "/cancelar")
                        .with(user("conta.b@teste.com").roles("PARTICIPANTE")).with(csrf()))
                .andExpect(flash().attribute("erro", "Você não pode cancelar outra inscrição."));

        // A cancela: registro continua no banco como CANCELADA
        mvc.perform(post("/minhas-inscricoes/" + deA.getId() + "/cancelar")
                        .with(user("conta.a@teste.com").roles("PARTICIPANTE")).with(csrf()))
                .andExpect(redirectedUrl("/meus-eventos"))
                .andExpect(flash().attribute("sucesso", "Inscrição cancelada com sucesso."));
        Inscricao cancelada = inscricaoRepository.findById(deA.getId()).orElseThrow();
        assertThat(cancelada.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(cancelada.getDataCancelamento()).isNotNull();

        // Cancelar de novo: bloqueado
        assertThatThrownBy(() -> inscricaoService.cancelarMinhaInscricao(deA.getId(), "conta.a@teste.com"))
                .hasMessageContaining("já está cancelada");

        // A vaga foi liberada para B
        inscricaoService.inscreverUsuario(evento.getId(), "conta.b@teste.com");
        assertThat(inscricaoService.contarPorEvento(evento.getId())).isEqualTo(1);
        assertThat(inscricaoRepository.countByEventoId(evento.getId())).isEqualTo(2); // histórico
    }

    @Test
    void naoCancelaDepoisDoInicioDoEvento() {
        criarConta("conta.c@teste.com");
        Evento evento = criarEvento(5, LocalDate.now().plusDays(3));
        Inscricao inscricao = inscricaoService.inscreverUsuario(evento.getId(), "conta.c@teste.com");

        evento.setData(LocalDate.now().minusDays(1)); // evento já começou
        eventoRepository.save(evento);

        assertThatThrownBy(() -> inscricaoService.cancelarMinhaInscricao(inscricao.getId(), "conta.c@teste.com"))
                .hasMessageContaining("após o início");
    }

    @Test
    void naoInscreveEmEventoEncerrado() {
        criarConta("conta.d@teste.com");
        Evento evento = criarEvento(5, LocalDate.now().plusDays(3));
        evento.setStatus(StatusEvento.ENCERRADO);
        eventoRepository.save(evento);
        assertThatThrownBy(() -> inscricaoService.inscreverUsuario(evento.getId(), "conta.d@teste.com"))
                .hasMessageContaining("não está aberto");
    }

    // ---------------- Todas as telas renderizam sem erro ----------------

    @Test
    void todasAsTelasDoAdminRenderizam() throws Exception {
        Evento evento = criarEvento(3, LocalDate.now().plusDays(5));
        criarConta("conta.e@teste.com");
        inscricaoService.inscreverUsuario(evento.getId(), "conta.e@teste.com");
        var admin = user("admin@eventos.com").roles("ADMIN");
        String[] rotas = {"/", "/eventos", "/eventos/novo", "/eventos/editar/" + evento.getId(),
            "/eventos/" + evento.getId() + "/inscritos", "/participantes", "/participantes/novo",
            "/inscricoes", "/inscricoes/nova", "/relatorios"};
        for (String rota : rotas) {
            mvc.perform(get(rota).with(admin)).andExpect(status().isOk());
        }
        mvc.perform(get("/acesso-negado")).andExpect(status().isOk());
    }

    @Test
    void todasAsTelasDoParticipanteRenderizam() throws Exception {
        Evento evento = criarEvento(3, LocalDate.now().plusDays(5));
        criarConta("conta.f@teste.com");
        Inscricao i = inscricaoService.inscreverUsuario(evento.getId(), "conta.f@teste.com");
        inscricaoService.cancelarMinhaInscricao(i.getId(), "conta.f@teste.com");
        inscricaoService.inscreverUsuario(evento.getId(), "conta.f@teste.com");
        var participante = user("conta.f@teste.com").roles("PARTICIPANTE");
        for (String rota : new String[]{"/", "/catalogo", "/meus-eventos"}) {
            mvc.perform(get(rota).with(participante)).andExpect(status().isOk());
        }
    }

    @Test
    void eventoComInscricoesNaoEExcluido() throws Exception {
        Evento evento = criarEvento(3, LocalDate.now().plusDays(5));
        criarConta("conta.g@teste.com");
        inscricaoService.inscreverUsuario(evento.getId(), "conta.g@teste.com");
        mvc.perform(post("/eventos/excluir/" + evento.getId())
                        .with(user("admin@eventos.com").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/eventos"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(eventoRepository.existsById(evento.getId())).isTrue();
    }

    // ---------------- Melhorias de fluxo: catálogo e detalhe ----------------

    @Test
    void catalogoEDetalheMostramInscricaoECancelamNaMesmaTela() throws Exception {
        criarConta("conta.h@teste.com");
        Evento evento = criarEvento(5, LocalDate.now().plusDays(7));
        var participante = user("conta.h@teste.com").roles("PARTICIPANTE");

        // Inscrição a partir do catálogo volta para o catálogo
        mvc.perform(post("/inscricoes/minha").param("eventoId", evento.getId().toString())
                        .param("origem", "catalogo").with(participante).with(csrf()))
                .andExpect(redirectedUrl("/catalogo"))
                .andExpect(flash().attribute("sucesso", "Inscrição realizada!"));

        Long inscricaoId = inscricaoService.inscricoesAtivasPorEvento("conta.h@teste.com").get(evento.getId());
        assertThat(inscricaoId).isNotNull();

        // Catálogo e detalhe sabem que a conta está inscrita
        mvc.perform(get("/catalogo").with(participante))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("minhasInscricoes"));
        mvc.perform(get("/catalogo/" + evento.getId()).with(participante))
                .andExpect(status().isOk())
                .andExpect(view().name("detalhe-evento"))
                .andExpect(model().attribute("minhaInscricaoId", inscricaoId))
                .andExpect(model().attribute("podeInscrever", false));

        // Cancelar a partir do detalhe volta para o detalhe
        mvc.perform(post("/minhas-inscricoes/" + inscricaoId + "/cancelar")
                        .param("origem", "detalhe").param("eventoId", evento.getId().toString())
                        .with(participante).with(csrf()))
                .andExpect(redirectedUrl("/catalogo/" + evento.getId()));
        assertThat(inscricaoService.inscricoesAtivasPorEvento("conta.h@teste.com")).isEmpty();

        // Origem desconhecida não é usada como endereço de redirecionamento
        mvc.perform(post("/inscricoes/minha").param("eventoId", evento.getId().toString())
                        .param("origem", "https://site-externo.com").with(participante).with(csrf()))
                .andExpect(redirectedUrl("/meus-eventos"));
    }

    @Test
    void detalheDeEventoInexistenteVoltaParaCatalogo() throws Exception {
        mvc.perform(get("/catalogo/999999").with(user("participante@eventos.com").roles("PARTICIPANTE")))
                .andExpect(redirectedUrl("/catalogo"));
    }

    // ---------------- Relatório em PDF ----------------

    @Test
    void relatorioPdfEhGeradoParaAdminEBloqueadoParaParticipante() throws Exception {
        criarEvento(10, LocalDate.now().plusDays(4));
        var resposta = mvc.perform(get("/relatorios/pdf").with(user("admin@eventos.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("relatorio-eventos-")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(resposta, 0, 4)).isEqualTo("%PDF");

        mvc.perform(get("/relatorios/pdf").with(user("participante@eventos.com").roles("PARTICIPANTE")))
                .andExpect(status().isForbidden());
    }

    // ---------------- auxiliares ----------------

    private void criarConta(String email) {
        CadastroUsuarioForm form = new CadastroUsuarioForm();
        form.setNome("Pessoa " + email);
        form.setEmail(email);
        form.setTelefone("(11) 90000-0000");
        form.setDocumento("000");
        form.setSenha("senha12345");
        form.setConfirmacaoSenha("senha12345");
        usuarioService.cadastrar(form);
    }

    private Evento criarEvento(int vagas, LocalDate data) {
        Evento e = new Evento();
        e.setTitulo("Evento de teste");
        e.setDescricao("Descrição");
        e.setData(data);
        e.setHora(LocalTime.of(19, 0));
        e.setLocal("Auditório");
        e.setQuantidadeVagas(vagas);
        e.setStatus(StatusEvento.ABERTO);
        return eventoRepository.save(e);
    }
}

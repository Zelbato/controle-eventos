package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.model.Evento;
import br.edu.exemplo.controleeventos.model.StatusInscricao;
import br.edu.exemplo.controleeventos.repository.UsuarioRepository;
import br.edu.exemplo.controleeventos.service.EventoService;
import br.edu.exemplo.controleeventos.service.InscricaoService;
import br.edu.exemplo.controleeventos.service.ParticipanteService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página inicial (dashboard). O conteúdo muda conforme o perfil:
 * ADMIN vê indicadores e os próximos eventos; PARTICIPANTE vê seus números
 * e os eventos com inscrições abertas.
 */
@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;
    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final InscricaoService inscricaoService;

    public InicioController(UsuarioRepository usuarioRepository, EventoService eventoService,
            ParticipanteService participanteService, InscricaoService inscricaoService) {
        this.usuarioRepository = usuarioRepository;
        this.eventoService = eventoService;
        this.participanteService = participanteService;
        this.inscricaoService = inscricaoService;
    }

    @GetMapping("/")
    public String inicio(Authentication auth, Model model) {
        usuarioRepository.findByEmail(auth.getName())
                .ifPresent(u -> model.addAttribute("nomeUsuario", u.getNome()));

        // Eventos abertos e futuros, com quantidade de inscritos ativos de cada um
        List<Evento> abertos = eventoService.listarAbertosFuturos();
        Map<Long, Long> inscritosPorEvento = new HashMap<>();
        for (Evento e : abertos) {
            inscritosPorEvento.put(e.getId(), inscricaoService.contarPorEvento(e.getId()));
        }
        model.addAttribute("inscritosPorEvento", inscritosPorEvento);
        model.addAttribute("eventosDisponiveis", abertos.size());

        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) {
            model.addAttribute("totalEventos", eventoService.listarTodos().size());
            model.addAttribute("totalParticipantes", participanteService.listarTodos().size());
            model.addAttribute("inscricoesAtivas", inscricaoService.contarAtivas());
            model.addAttribute("inscricoesTotal", inscricaoService.contarTodas());
            model.addAttribute("proximosEventos", abertos.stream().limit(5).toList());
        } else {
            long minhasAtivas = inscricaoService.listarDoUsuario(auth.getName()).stream()
                    .filter(i -> i.getStatus() == StatusInscricao.ATIVA)
                    .count();
            model.addAttribute("minhasAtivas", minhasAtivas);
            model.addAttribute("proximosEventos", abertos.stream().limit(3).toList());
        }
        return "inicio";
    }
}

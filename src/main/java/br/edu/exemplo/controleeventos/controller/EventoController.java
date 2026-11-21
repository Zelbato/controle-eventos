package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.model.Evento;
import br.edu.exemplo.controleeventos.model.StatusEvento;
import br.edu.exemplo.controleeventos.service.EventoService;
import br.edu.exemplo.controleeventos.service.InscricaoService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventoController {

    private final EventoService eventoService;
    private final InscricaoService inscricaoService;

    public EventoController(EventoService eventoService, InscricaoService inscricaoService) {
        this.eventoService = eventoService;
        this.inscricaoService = inscricaoService;
    }

    @GetMapping("/eventos")
    public String listarEventos(Model model) {
        List<Evento> eventos = eventoService.listarTodos();
        Map<Long, Long> inscritosPorEvento = new HashMap<>();
        for (Evento e : eventos) {
            inscritosPorEvento.put(e.getId(), inscricaoService.contarPorEvento(e.getId()));
        }
        model.addAttribute("eventos", eventos);
        model.addAttribute("inscritosPorEvento", inscritosPorEvento);
        return "lista-eventos";
    }

    @GetMapping("/eventos/novo")
    public String novoEvento(Model model) {
        model.addAttribute("evento", new Evento());
        return "form-evento";
    }

    @PostMapping("/eventos/salvar")
    public String salvarEvento(@Valid Evento evento, BindingResult resultado) {
        if (resultado.hasErrors()) {
            return "form-evento";
        }
        eventoService.salvar(evento);
        return "redirect:/eventos";
    }

    @GetMapping("/eventos/editar/{id}")
    public String editarEvento(@PathVariable Long id, Model model) {
        Evento evento = eventoService.buscarPorId(id);
        if (evento == null) {
            return "redirect:/eventos";
        }
        model.addAttribute("evento", evento);
        return "form-evento";
    }

    /** Parte 08: exclusão somente por POST (com token CSRF). */
    @PostMapping("/eventos/excluir/{id}")
    public String excluirEvento(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            eventoService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Evento excluído.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/eventos";
    }

    /**
     * Parte 11: catálogo de eventos abertos e futuros para o participante.
     * Também informa em quais eventos a conta já está inscrita.
     */
    @GetMapping("/catalogo")
    public String catalogo(Authentication auth, Model model) {
        List<Evento> eventos = eventoService.listarAbertosFuturos();
        Map<Long, Long> vagasDisponiveis = new HashMap<>();
        for (Evento e : eventos) {
            vagasDisponiveis.put(e.getId(),
                    e.getQuantidadeVagas() - inscricaoService.contarPorEvento(e.getId()));
        }
        model.addAttribute("eventos", eventos);
        model.addAttribute("vagasDisponiveis", vagasDisponiveis);
        model.addAttribute("minhasInscricoes", inscricaoService.inscricoesAtivasPorEvento(auth.getName()));
        return "catalogo-eventos";
    }

    /** Página de detalhe de um evento, com inscrição ou cancelamento. */
    @GetMapping("/catalogo/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model) {
        Evento evento = eventoService.buscarPorId(id);
        if (evento == null) {
            return "redirect:/catalogo";
        }
        long inscritos = inscricaoService.contarPorEvento(id);
        long livres = evento.getQuantidadeVagas() - inscritos;
        Long minhaInscricaoId = inscricaoService.inscricoesAtivasPorEvento(auth.getName()).get(id);
        boolean podeInscrever = evento.getStatus() == StatusEvento.ABERTO
                && !evento.isIniciado()
                && livres > 0
                && minhaInscricaoId == null;

        model.addAttribute("evento", evento);
        model.addAttribute("inscritos", inscritos);
        model.addAttribute("livres", livres);
        model.addAttribute("minhaInscricaoId", minhaInscricaoId);
        model.addAttribute("podeInscrever", podeInscrever);
        return "detalhe-evento";
    }
}

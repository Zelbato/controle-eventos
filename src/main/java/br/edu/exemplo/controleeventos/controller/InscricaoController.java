package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.model.Evento;
import br.edu.exemplo.controleeventos.model.Inscricao;
import br.edu.exemplo.controleeventos.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InscricaoController {

    private final InscricaoService inscricaoService;
    private final EventoService eventoService;
    private final ParticipanteService participanteService;

    public InscricaoController(InscricaoService inscricaoService, EventoService eventoService,
            ParticipanteService participanteService) {
        this.inscricaoService = inscricaoService;
        this.eventoService = eventoService;
        this.participanteService = participanteService;
    }

    // ---------- Área administrativa (ADMIN) ----------

    @GetMapping("/inscricoes")
    public String listar(Model model) {
        model.addAttribute("inscricoes", inscricaoService.listarTodas());
        return "lista-inscricoes";
    }

    @GetMapping("/inscricoes/nova")
    public String nova(Model model) {
        model.addAttribute("inscricao", new Inscricao());
        carregarListas(model);
        return "form-inscricao";
    }

    /** Inscrição feita pelo ADMIN escolhendo o participante (Parte 04/05). */
    @PostMapping("/inscricoes/salvar")
    public String salvar(Inscricao inscricao, Model model) {
        try {
            inscricaoService.salvar(inscricao);
            return "redirect:/inscricoes";
        } catch (IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            carregarListas(model);
            return "form-inscricao";
        }
    }

    /** Parte 08: exclusão somente por POST (com token CSRF). */
    @PostMapping("/inscricoes/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        inscricaoService.excluir(id);
        return "redirect:/inscricoes";
    }

    @GetMapping("/eventos/{id}/inscritos")
    public String inscritosDoEvento(@PathVariable Long id, Model model) {
        Evento evento = eventoService.buscarPorId(id);
        if (evento == null) {
            return "redirect:/eventos";
        }
        model.addAttribute("evento", evento);
        model.addAttribute("inscricoes", inscricaoService.listarPorEvento(id));
        model.addAttribute("totalInscritos", inscricaoService.contarPorEvento(id));
        return "inscritos-evento";
    }

    private void carregarListas(Model model) {
        model.addAttribute("eventos", eventoService.listarTodos());
        model.addAttribute("participantes", participanteService.listarTodos());
    }

    // ---------- Área do participante (PARTICIPANTE) ----------

    /**
     * Parte 11: o participante vem da conta autenticada, nunca do formulário.
     * "origem" (opcional) indica para qual tela voltar: catalogo ou detalhe.
     */
    @PostMapping("/inscricoes/minha")
    public String inscreverMinhaConta(@RequestParam Long eventoId,
            @RequestParam(required = false) String origem,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            inscricaoService.inscreverUsuario(eventoId, authentication.getName());
            redirect.addFlashAttribute("sucesso", "Inscrição realizada!");
            return destino(origem, eventoId, "redirect:/meus-eventos");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return destino(origem, eventoId, "redirect:/catalogo");
        }
    }

    /** Parte 11: somente as inscrições da própria conta. */
    @GetMapping("/meus-eventos")
    public String meusEventos(Authentication auth, Model model) {
        model.addAttribute("inscricoes", inscricaoService.listarDoUsuario(auth.getName()));
        return "meus-eventos";
    }

    /** Parte 12: cancelamento da própria inscrição, por POST. */
    @PostMapping("/minhas-inscricoes/{id}/cancelar")
    public String cancelarMinhaInscricao(@PathVariable Long id,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) Long eventoId,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            inscricaoService.cancelarMinhaInscricao(id, authentication.getName());
            redirect.addFlashAttribute("sucesso", "Inscrição cancelada com sucesso.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return destino(origem, eventoId, "redirect:/meus-eventos");
    }

    /**
     * Escolhe a tela de retorno. Só aceita valores conhecidos, para não
     * permitir redirecionamento para endereços arbitrários.
     */
    private String destino(String origem, Long eventoId, String padrao) {
        if ("catalogo".equals(origem)) {
            return "redirect:/catalogo";
        }
        if ("detalhe".equals(origem) && eventoId != null) {
            return "redirect:/catalogo/" + eventoId;
        }
        return padrao;
    }
}

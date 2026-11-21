package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.model.Participante;
import br.edu.exemplo.controleeventos.service.ParticipanteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping("/participantes")
    public String listar(Model model) {
        model.addAttribute("participantes", participanteService.listarTodos());
        return "lista-participantes";
    }

    @GetMapping("/participantes/novo")
    public String novo(Model model) {
        model.addAttribute("participante", new Participante());
        return "form-participante";
    }

    @PostMapping("/participantes/salvar")
    public String salvar(@Valid Participante participante, BindingResult resultado) {
        if (resultado.hasErrors()) {
            return "form-participante";
        }
        participanteService.salvar(participante);
        return "redirect:/participantes";
    }

    @GetMapping("/participantes/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Participante participante = participanteService.buscarPorId(id);
        if (participante == null) {
            return "redirect:/participantes";
        }
        model.addAttribute("participante", participante);
        return "form-participante";
    }

    /** Parte 08: exclusão somente por POST (com token CSRF). */
    @PostMapping("/participantes/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            participanteService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Participante excluído.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/participantes";
    }
}

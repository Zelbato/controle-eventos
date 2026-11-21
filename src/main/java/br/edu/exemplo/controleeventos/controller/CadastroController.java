package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.dto.CadastroUsuarioForm;
import br.edu.exemplo.controleeventos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Parte 10: cadastro público de contas PARTICIPANTE. */
@Controller
public class CadastroController {

    private final UsuarioService usuarioService;

    public CadastroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cadastro")
    public String novo(Model model) {
        model.addAttribute("cadastro", new CadastroUsuarioForm());
        return "cadastro-usuario";
    }

    @PostMapping("/cadastro")
    public String salvar(
            @Valid @ModelAttribute("cadastro") CadastroUsuarioForm form,
            BindingResult resultado) {

        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmacaoSenha())) {
            resultado.rejectValue("confirmacaoSenha", "", "As senhas informadas não coincidem.");
        }
        if (resultado.hasErrors()) {
            return "cadastro-usuario";
        }
        try {
            usuarioService.cadastrar(form);
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("email", "", e.getMessage());
            return "cadastro-usuario";
        }
        return "redirect:/login?cadastro";
    }
}

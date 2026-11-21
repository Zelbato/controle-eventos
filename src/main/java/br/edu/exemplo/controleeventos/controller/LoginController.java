package br.edu.exemplo.controleeventos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Parte 08: apenas exibe a página de login.
 * Quem valida usuário e senha é o Spring Security, no POST /login.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Página exibida quando o perfil não tem permissão (HTTP 403).
     * Aceita qualquer método: um POST recusado (ex.: token CSRF vencido)
     * também é encaminhado para cá.
     */
    @RequestMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}

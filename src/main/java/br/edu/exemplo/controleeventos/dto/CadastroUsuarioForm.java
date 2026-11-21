package br.edu.exemplo.controleeventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Parte 10: dados do formulário público de cadastro.
 * Não possui o campo perfil: o servidor define PARTICIPANTE.
 * Parte 11: telefone e documento para criar o Participante vinculado.
 */
public class CadastroUsuarioForm {

    @NotBlank(message = "Informe seu nome")
    @Size(min = 3, message = "O nome deve possuir pelo menos 3 caracteres")
    private String nome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Informe um e-mail válido")
    private String email;

    @NotBlank(message = "Informe o telefone")
    private String telefone;

    @NotBlank(message = "Informe o documento")
    private String documento;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 8, message = "Use pelo menos 8 caracteres")
    private String senha;

    @NotBlank(message = "Confirme sua senha")
    private String confirmacaoSenha;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}

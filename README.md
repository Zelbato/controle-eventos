# Controle de Eventos

**Aluno:** Heitor André Zelbato Araujo

## Descrição

Sistema web para controle de eventos, com login e perfis de acesso (administrador e participante). O administrador cadastra, lista, edita e exclui eventos, participantes e inscrições e acompanha os relatórios, que podem ser impressos ou baixados em PDF. O participante cria a própria conta, consulta o catálogo, se inscreve nos eventos e cancela suas inscrições, mantendo o histórico.

## Órgãos transversais

Java, Spring Boot, Spring MVC, Spring Security, Thymeleaf, Bean Validation, Spring Data JPA, Hibernate, PostgreSQL, OpenPDF e Maven.

## Banco de dados

`controle_eventos`

## Como executar

1. Criar o banco PostgreSQL `controle_eventos`.
2. Configure usuário e senha do PostgreSQL em `src/main/resources/application.properties`.
3. Abrir o projeto no NetBeans.
4. Executar a classe `ControleEventos`.
5. Acessar `http://localhost:8081` e entrar com `admin@eventos.com` / `admin123`.

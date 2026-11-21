# Sistema Web de Controle de Eventos

Spring Boot 3.5.16 · Java 21 · Thymeleaf · Spring Data JPA · Spring Security · PostgreSQL · BCrypt

Projeto completo com as **Partes 01 a 12** do curso: CRUD de eventos, participantes e inscrições;
regras de vagas; relatórios; login; usuários e perfis no banco; cadastro público;
inscrição pelo próprio participante; cancelamento com histórico.

---

## 1. Como executar

### Requisitos
- JDK 21
- NetBeans (ou outra IDE com Maven)
- PostgreSQL. Duas opções:
  - **Docker:** na pasta do projeto, rode `docker compose up -d`.
  - **PostgreSQL instalado:** execute `sql/01-criar-banco.sql` como usuário `postgres`.

### Conexão (application.properties)
| Propriedade | Padrão | Variável para alterar |
|---|---|---|
| URL | `jdbc:postgresql://localhost:5432/controle_eventos` | `DB_URL` |
| Usuário | `eventos_app` | `DB_USERNAME` |
| Senha | `eventos_local_2026` | `DB_PASSWORD` |

> O banco `controle_eventos` e o usuário `eventos_app` foram criados no PostgreSQL local (porta 5432). O docker-compose.yml é opcional e usa a porta 5434.

### No NetBeans
1. **File → Open Project** e selecione a pasta `controle-eventos`.
2. Aguarde o Maven baixar as dependências.
3. Clique em **Run Project** (F6).
4. Acesse **http://localhost:8081**.

As tabelas são criadas pelo Hibernate (`ddl-auto=update`).

### Contas criadas automaticamente (CargaInicial)
| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | `admin@eventos.com` | `admin123` |
| PARTICIPANTE | `participante@eventos.com` | `teste123` |

> Senhas apenas para a aula. Para trocar, use as variáveis `APP_ADMIN_EMAIL` e
> `APP_ADMIN_PASSWORD`. Para não criar a conta de teste, use `APP_PARTICIPANTE_TESTE=false`.
> Não versione senhas reais no Git.

---

## 2. Rotas e permissões

| Rota | Acesso |
|---|---|
| `/login`, `/cadastro`, `/css/**`, `/acesso-negado` | Público |
| `/` (início/dashboard) | Qualquer usuário logado. O conteúdo muda conforme o perfil |
| `/catalogo`, `/catalogo/{id}`, `/meus-eventos`, `POST /inscricoes/minha`, `POST /minhas-inscricoes/{id}/cancelar` | PARTICIPANTE |
| `/eventos/**`, `/participantes/**`, `/inscricoes/**`, `/relatorios` | ADMIN |

- Todas as alterações de dados usam **POST com token CSRF** (o CSRF fica ativado).
- Exclusões e logout **não** funcionam por GET.
- Acesso sem permissão abre a página **Acesso negado** (HTTP 403).

---

## 3. O que cada tutorial acrescentou

| Parte | Conteúdo | Arquivos principais |
|---|---|---|
| 01–07 | CRUD de Evento, Participante e Inscrição; regras de vagas e duplicidade; inscritos por evento; relatório geral; `listarAbertosFuturos()` | `model/*`, `repository/*`, `service/*`, `controller/*`, templates `lista-*`, `form-*`, `inscritos-evento`, `relatorio-geral` |
| 08 | Spring Security; página de login; logout por POST; exclusões trocadas de GET para POST | `pom.xml`, `config/SecurityConfig`, `controller/LoginController`, `login.html`, `fragments/layout.html` |
| 09 | `Usuario` e `PerfilUsuario` no PostgreSQL; `UsuarioDetailsService`; BCrypt; regras por perfil; administrador inicial | `model/Usuario`, `model/PerfilUsuario`, `repository/UsuarioRepository`, `service/UsuarioDetailsService`, `config/CargaInicial` |
| 10 | Cadastro público (sempre PARTICIPANTE); DTO com validação; e-mail único e normalizado | `dto/CadastroUsuarioForm`, `service/UsuarioService`, `controller/CadastroController`, `cadastro-usuario.html` |
| 11 | Vínculo `Usuario` ↔ `Participante`; inscrição pela conta logada; catálogo; Meus eventos | `UsuarioService`, `InscricaoService.inscreverUsuario/listarDoUsuario`, `catalogo-eventos.html`, `meus-eventos.html` |
| 12 | `StatusInscricao` (ATIVA/CANCELADA); cancelamento sem excluir; vagas e duplicidade só com ATIVAS; indicadores ativas × histórico | `model/StatusInscricao`, `InscricaoService.cancelarMinhaInscricao`, `InscricaoRepository`, `meus-eventos.html`, `inicio.html` |

### Melhorias de fluxo (além dos tutoriais)
- **Catálogo:** mostra "Inscrito" nos eventos em que a conta já tem vaga e permite cancelar ali mesmo.
  Depois de inscrever ou cancelar, o usuário continua na mesma tela (parâmetro `origem`, que só aceita
  `catalogo` ou `detalhe`).
- **Detalhe do evento** (`/catalogo/{id}`): descrição completa, informações, ocupação e o botão de
  inscrever ou cancelar.
- **Busca e filtros instantâneos** nas listas de eventos, participantes e inscrições do admin e no catálogo
  (script em `fragments/layout.html :: filtros-script`).
- **Pré-visualização ao vivo** nos formulários de evento, participante e inscrição.
- **Relatório:** botão **Imprimir** (layout próprio para papel, sem menus) e **Baixar PDF**
  (`/relatorios/pdf`, gerado no servidor com a biblioteca OpenPDF).

### Ajustes em relação ao projeto de exemplo (.zip)
- `Inscricao.status` passou de `String` para o enum `StatusInscricao`, como pede a Parte 12.
  Os valores `'ATIVA'` que já estão no banco continuam válidos.
- Foi criado `EventoService.listarAbertosFuturos()`, usado pela Parte 11 mas ausente no .zip.
- Visual refeito com um design system único (`static/css/style.css`): menu lateral por perfil,
  painel com indicadores, cartões de evento no catálogo e telas de login e cadastro em duas colunas.
  As partes repetidas ficam em `templates/fragments/layout.html` (head, menu lateral, barra
  superior com **Sair**, mensagens), e os ícones SVG ficam em `templates/fragments/icones.html`.
- Um evento ou participante com inscrições não é apagado. Aparece uma mensagem de aviso no
  lugar de um erro de banco. Para tirar um evento de circulação, mude o status para CANCELADO.
- A dependência `thymeleaf-extras-springsecurity6` foi adicionada para mostrar o menu conforme
  o perfil (`sec:authorize`).

---

## 4. Testes automatizados

`src/test/java/.../ControleEventosTest.java` cobre os checklists dos Tutoriais 08 a 12:
proteção de rotas, login e erro de senha, logout, exclusão só por POST com CSRF, perfis,
cadastro (validações, duplicidade, BCrypt, vínculo), inscrição, vagas, cancelamento, histórico,
tentativa de cancelar a inscrição de outra conta e renderização de todas as telas.

- NetBeans: botão direito no projeto → **Test**.
- Terminal: `mvn test`.

Os testes usam um banco **H2 em memória**, então não precisam do PostgreSQL.

---

## 5. Roteiro de teste manual (para apresentar)
1. Abra `/eventos` sem login → vai para `/login`.
2. Entre como ADMIN → cadastre um evento futuro, ABERTO, com **1 vaga**.
3. Saia. Em `/cadastro`, crie a **conta A** → entre → **Catálogo** → *Inscrever-me*.
4. Crie a **conta B** → tente se inscrever → "Não há vagas disponíveis".
5. Entre com A → **Meus eventos** → *Cancelar inscrição* → status **CANCELADA**.
6. Entre com B → inscrição aceita (a vaga foi liberada).
7. Como PARTICIPANTE, abra `/eventos/novo` → **Acesso negado**.
8. No banco, rode `sql/02-consultas-conferencia.sql`: a inscrição cancelada continua na tabela.

---

## 6. Estrutura
```
src/main/java/br/edu/exemplo/controleeventos/
├── ControleEventos.java
├── config/       SecurityConfig, CargaInicial
├── controller/   Inicio, Login, Cadastro, Evento, Participante, Inscricao, Relatorio
├── dto/          CadastroUsuarioForm, OcupacaoEvento
├── model/        Evento, StatusEvento, Participante, Inscricao, StatusInscricao, Usuario, PerfilUsuario
├── repository/   Evento, Participante, Inscricao, Usuario
└── service/      EventoService, ParticipanteService, InscricaoService, UsuarioService, UsuarioDetailsService
src/main/resources/
├── application.properties
├── static/css/   style.css (design system único)
└── templates/    login, cadastro-usuario, inicio, catalogo-eventos, meus-eventos, acesso-negado,
                  lista-*/form-* (eventos, participantes, inscrições), inscritos-evento,
                  relatorio-geral, fragments/layout, fragments/icones
sql/              criação do banco e consultas de conferência
docker-compose.yml
```

package br.edu.exemplo.controleeventos.controller;

import br.edu.exemplo.controleeventos.dto.OcupacaoEvento;
import br.edu.exemplo.controleeventos.service.EventoService;
import br.edu.exemplo.controleeventos.service.InscricaoService;
import br.edu.exemplo.controleeventos.service.ParticipanteService;
import br.edu.exemplo.controleeventos.service.RelatorioPdfService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RelatorioController {

    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final InscricaoService inscricaoService;
    private final RelatorioPdfService relatorioPdfService;

    public RelatorioController(EventoService eventoService,
            ParticipanteService participanteService, InscricaoService inscricaoService,
            RelatorioPdfService relatorioPdfService) {
        this.eventoService = eventoService;
        this.participanteService = participanteService;
        this.inscricaoService = inscricaoService;
        this.relatorioPdfService = relatorioPdfService;
    }

    @GetMapping("/relatorios")
    public String relatorioGeral(Model model) {
        var eventos = eventoService.listarTodos();
        List<OcupacaoEvento> ocupacoes = eventos.stream()
                .map(evento -> {
                    long inscritos = inscricaoService.contarPorEvento(evento.getId());
                    long disponiveis = evento.getQuantidadeVagas() - inscritos;

                    return new OcupacaoEvento(
                            evento.getTitulo(),
                            evento.getQuantidadeVagas(),
                            inscritos,
                            disponiveis
                    );
                })
                .toList();

        model.addAttribute("totalEventos", eventos.size());
        model.addAttribute("totalParticipantes",
                participanteService.listarTodos().size());
        // Parte 12: separa inscrições ativas do total histórico
        model.addAttribute("totalInscricoes", inscricaoService.contarAtivas());
        model.addAttribute("totalHistorico", inscricaoService.contarTodas());
        model.addAttribute("ocupacoes", ocupacoes);
        return "relatorio-geral";
    }

    /** Baixa o relatório geral em PDF. */
    @GetMapping("/relatorios/pdf")
    public ResponseEntity<byte[]> baixarPdf() {
        byte[] pdf = relatorioPdfService.gerar();
        String nomeArquivo = "relatorio-eventos-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

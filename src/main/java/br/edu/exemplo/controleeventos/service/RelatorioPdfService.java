package br.edu.exemplo.controleeventos.service;

import br.edu.exemplo.controleeventos.model.Evento;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Gera o relatório geral em PDF (biblioteca OpenPDF).
 * Mesmos números da tela /relatorios: totais e ocupação por evento.
 */
@Service
public class RelatorioPdfService {

    private static final Color PRIMARIA = new Color(79, 70, 229);
    private static final Color TEXTO = new Color(15, 23, 42);
    private static final Color SUAVE = new Color(100, 116, 139);
    private static final Color BORDA = new Color(226, 232, 240);
    private static final Color FUNDO_CLARO = new Color(238, 242, 255);
    private static final Color LINHA_ALTERNADA = new Color(248, 250, 252);

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final InscricaoService inscricaoService;

    public RelatorioPdfService(EventoService eventoService,
            ParticipanteService participanteService,
            InscricaoService inscricaoService) {
        this.eventoService = eventoService;
        this.participanteService = participanteService;
        this.inscricaoService = inscricaoService;
    }

    public byte[] gerar() {
        List<Evento> eventos = eventoService.listarTodos();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 44, 44);

        try {
            PdfWriter.getInstance(documento, saida);
            documento.addTitle("Relatório geral - Controle de Eventos");
            documento.open();

            // Cabeçalho
            Paragraph marca = new Paragraph("CONTROLE DE EVENTOS", fonte(9, Font.BOLD, PRIMARIA));
            documento.add(marca);
            Paragraph titulo = new Paragraph("Relatório geral", fonte(22, Font.BOLD, TEXTO));
            titulo.setSpacingBefore(4);
            documento.add(titulo);
            Paragraph gerado = new Paragraph("Gerado em " + LocalDateTime.now().format(DATA_HORA),
                    fonte(10, Font.NORMAL, SUAVE));
            gerado.setSpacingAfter(18);
            documento.add(gerado);

            // Indicadores
            PdfPTable indicadores = new PdfPTable(4);
            indicadores.setWidthPercentage(100);
            indicadores.setSpacingAfter(22);
            indicadores.addCell(indicador("Eventos", eventos.size()));
            indicadores.addCell(indicador("Participantes", participanteService.listarTodos().size()));
            indicadores.addCell(indicador("Inscrições ativas", inscricaoService.contarAtivas()));
            indicadores.addCell(indicador("Histórico de inscrições", inscricaoService.contarTodas()));
            documento.add(indicadores);

            // Tabela de ocupação
            Paragraph secao = new Paragraph("Ocupação por evento", fonte(13, Font.BOLD, TEXTO));
            secao.setSpacingAfter(4);
            documento.add(secao);
            Paragraph nota = new Paragraph("Considera apenas inscrições ativas.", fonte(9, Font.NORMAL, SUAVE));
            nota.setSpacingAfter(10);
            documento.add(nota);

            PdfPTable tabela = new PdfPTable(new float[]{3.2f, 1.6f, 2.2f, 1.4f, 1f, 1.1f, 1.2f, 1.2f});
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            for (String coluna : new String[]{"Evento", "Data", "Local", "Status", "Vagas", "Inscritos", "Livres", "Ocupação"}) {
                tabela.addCell(cabecalho(coluna));
            }

            if (eventos.isEmpty()) {
                PdfPCell vazio = celula("Nenhum evento cadastrado.", Element.ALIGN_CENTER, false);
                vazio.setColspan(8);
                vazio.setPadding(14);
                tabela.addCell(vazio);
            }

            boolean alternar = false;
            for (Evento evento : eventos) {
                long inscritos = inscricaoService.contarPorEvento(evento.getId());
                int vagas = evento.getQuantidadeVagas();
                long livres = Math.max(0, vagas - inscritos);
                long ocupacao = vagas > 0 ? inscritos * 100 / vagas : 0;
                String quando = (evento.getData() != null ? evento.getData().format(DATA) : "-")
                        + (evento.getHora() != null ? " " + evento.getHora().format(HORA) : "");

                tabela.addCell(celula(evento.getTitulo(), Element.ALIGN_LEFT, alternar));
                tabela.addCell(celula(quando, Element.ALIGN_LEFT, alternar));
                tabela.addCell(celula(evento.getLocal(), Element.ALIGN_LEFT, alternar));
                tabela.addCell(celula(String.valueOf(evento.getStatus()), Element.ALIGN_LEFT, alternar));
                tabela.addCell(celula(String.valueOf(vagas), Element.ALIGN_RIGHT, alternar));
                tabela.addCell(celula(String.valueOf(inscritos), Element.ALIGN_RIGHT, alternar));
                tabela.addCell(celula(String.valueOf(livres), Element.ALIGN_RIGHT, alternar));
                tabela.addCell(celula(ocupacao + "%", Element.ALIGN_RIGHT, alternar));
                alternar = !alternar;
            }
            documento.add(tabela);

            Paragraph rodape = new Paragraph("Sistema de Controle de Eventos · Spring Boot", fonte(8, Font.NORMAL, SUAVE));
            rodape.setSpacingBefore(20);
            rodape.setAlignment(Element.ALIGN_CENTER);
            documento.add(rodape);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do relatório.", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return saida.toByteArray();
    }

    private static Font fonte(float tamanho, int estilo, Color cor) {
        return new Font(Font.HELVETICA, tamanho, estilo, cor);
    }

    private static PdfPCell indicador(String rotulo, long valor) {
        PdfPCell celula = new PdfPCell();
        celula.setBackgroundColor(FUNDO_CLARO);
        celula.setBorderColor(Color.WHITE);
        celula.setBorderWidth(3);
        celula.setPadding(10);
        celula.addElement(new Paragraph(rotulo, fonte(8.5f, Font.NORMAL, SUAVE)));
        celula.addElement(new Paragraph(String.valueOf(valor), fonte(20, Font.BOLD, PRIMARIA)));
        return celula;
    }

    private static PdfPCell cabecalho(String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte(8.5f, Font.BOLD, Color.WHITE)));
        celula.setBackgroundColor(PRIMARIA);
        celula.setBorder(Rectangle.NO_BORDER);
        celula.setPaddingTop(7);
        celula.setPaddingBottom(7);
        celula.setPaddingLeft(6);
        return celula;
    }

    private static PdfPCell celula(String texto, int alinhamento, boolean alternada) {
        PdfPCell celula = new PdfPCell(new Phrase(texto == null ? "" : texto, fonte(9, Font.NORMAL, TEXTO)));
        celula.setHorizontalAlignment(alinhamento);
        celula.setBorder(Rectangle.BOTTOM);
        celula.setBorderColor(BORDA);
        celula.setPaddingTop(6);
        celula.setPaddingBottom(6);
        celula.setPaddingLeft(6);
        celula.setPaddingRight(6);
        if (alternada) {
            celula.setBackgroundColor(LINHA_ALTERNADA);
        }
        return celula;
    }
}

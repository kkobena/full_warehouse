package com.kobe.warehouse.service.remise.impl;

import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import com.kobe.warehouse.service.remise.impl.TableauxJournal.Colonne;
import com.kobe.warehouse.service.remise.impl.TableauxJournal.Tableau;
import com.kobe.warehouse.service.remise.impl.TableauxJournal.TypeCellule;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.excel.CsvExportService.SectionCsv;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService.SectionExcel;
import com.kobe.warehouse.service.report.pdf.AbstractStatistiqueReportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Écrit le journal en PDF, en classeur et en CSV, à partir des mêmes {@link TableauxJournal}.
 *
 * <p>Le PDF passe par le socle des rapports ({@link #getCommonParameters()},
 * {@link #exportReportToPdf()}), le classeur et le CSV par leurs services communs
 * ({@link ReportExcelExportService}, {@link CsvExportService}) : même en-tête, mêmes styles que les
 * autres états.
 *
 * <p>Le socle lit ses variables dans {@link #getParameters()}, état d'un service partagé : elles
 * sont
 * donc propres au fil d'exécution, et effacées après chaque PDF. Deux journaux peuvent s'exporter
 * en même temps.
 */
@Component
public class JournalRemisesExports extends AbstractStatistiqueReportService {

    private static final String GABARIT = "reports/journal-remises/main";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final SpringTemplateEngine templateEngine;
    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final ThreadLocal<Map<String, Object>> parametres = ThreadLocal.withInitial(HashMap::new);

    JournalRemisesExports(
            SpringTemplateEngine templateEngine,
            StorageService storageService,
            ReportExcelExportService excelExportService,
            CsvExportService csvExportService) {
        super(storageService);
        this.templateEngine = templateEngine;
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
    }

    static String construireTitre(
            JournalRemisesFiltre filtre,
            SyntheseJournalDTO synthese,
            Map<NatureJournal, List<LigneJournalDTO>> journaux) {
        String objet = synthese != null || journaux.size() != 1
                ? "Journal des remises, promotions et UG"
                : "Journal — " + journaux.keySet().iterator().next().getLibelle();
        return objet + " du " + filtre.du().format(JOUR) + " au " + filtre.au().format(JOUR);
    }

    private static TableauPdf convertirPourPdf(Tableau t) {
        List<TypeCellule> types = t.colonnes().stream().map(Colonne::type).toList();
        return new TableauPdf(
                t.titre(),
                t.titre().startsWith("Journal"),
                t.colonnes().stream().map(Colonne::libelle).toList(),
                types.stream().map(TypeCellule::estNumerique).toList(),
                t.lignes().stream().map(l -> formaterLigne(l, types, true)).toList(),
                t.total() == null ? null : formaterLigne(t.total(), types, true));
    }

    // ===== PDF =====

    private static Object[] preparerCellules(Object[] valeurs, List<TypeCellule> types) {
        Object[] cellules = new Object[valeurs.length];
        for (int i = 0; i < valeurs.length; i++) {
            Object v = valeurs[i];
            cellules[i] = v == null || (v instanceof Number && types.get(i).estNumerique())
                    ? v
                    : formaterTexte(v, types.get(i), false);
        }
        return cellules;
    }

    private static String[] lireEntetes(Tableau t) {
        return t.colonnes().stream().map(Colonne::libelle).toArray(String[]::new);
    }

    private static List<String> formaterLigne(Object[] valeurs, List<TypeCellule> types, boolean groupe) {
        List<String> textes = new ArrayList<>(valeurs.length);
        for (int i = 0; i < valeurs.length; i++) {
            textes.add(formaterTexte(valeurs[i], types.get(i), groupe));
        }
        return textes;
    }

    // ===== classeur =====

    /**
     * @param groupe sépare les milliers (PDF) ; le CSV garde des nombres que le tableur relit
     */
    static String formaterTexte(Object v, TypeCellule type, boolean groupe) {
        if (v == null) {
            return "";
        }
        if (v instanceof LocalDate d) {
            return d.format(JOUR);
        }
        if (v instanceof LocalDateTime t) {
            return t.format(type == TypeCellule.DATE ? JOUR : HEURE);
        }
        if (v instanceof Number n && type.estNumerique()) {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
            nf.setGroupingUsed(groupe);
            nf.setMaximumFractionDigits(type == TypeCellule.TAUX ? 2 : 0);
            // L'espace insécable du format français se lit mal hors d'un navigateur.
            return nf.format(n).replace(' ', ' ').replace(' ', ' ');
        }
        return v.toString();
    }

    byte[] exporter(
            FormatExport format,
            JournalRemisesFiltre filtre,
            SyntheseJournalDTO synthese,
            Map<NatureJournal, List<LigneJournalDTO>> journaux) {
        List<Tableau> tableaux = TableauxJournal.construireTableaux(synthese, journaux);
        String titre = construireTitre(filtre, synthese, journaux);
        try {
            return switch (format) {
                case PDF -> produirePdf(titre, tableaux);
                case XLSX -> produireClasseur(titre, tableaux);
                case CSV -> produireCsv(titre, tableaux);
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ===== CSV =====

    private byte[] produirePdf(String titre, List<Tableau> tableaux) {
        try {
            super.getCommonParameters();
            getParameters().put(Constant.REPORT_TITLE, titre);
            getParameters()
                    .put(
                            "tableaux",
                            tableaux.stream()
                                    .map(JournalRemisesExports::convertirPourPdf)
                                    .toList());
            return super.exportReportToPdf();
        } finally {
            parametres.remove();
        }
    }

    // ===== mise en texte =====

    /**
     * Une feuille par partie du journal ; les nombres restent des nombres, les dates passent en
     * texte.
     */
    private byte[] produireClasseur(String titre, List<Tableau> tableaux) throws IOException {
        Map<String, List<SectionExcel>> feuilles = new LinkedHashMap<>();
        for (Tableau t : tableaux) {
            List<TypeCellule> types = t.colonnes().stream().map(Colonne::type).toList();
            feuilles.computeIfAbsent(t.feuille(), f -> new ArrayList<>())
                    .add(new SectionExcel(
                            t.titre(),
                            lireEntetes(t),
                            t.lignes().stream()
                                    .map(l -> preparerCellules(l, types))
                                    .toList(),
                            t.total() == null ? null : preparerCellules(t.total(), types)));
        }
        return excelExportService.createExcelReport(titre, feuilles);
    }

    /**
     * Une section par tableau ; BOM UTF-8 pour qu'Excel lise les accents.
     */
    private byte[] produireCsv(String titre, List<Tableau> tableaux) throws IOException {
        List<SectionCsv> sections = new ArrayList<>(tableaux.size());
        for (Tableau t : tableaux) {
            List<TypeCellule> types = t.colonnes().stream().map(Colonne::type).toList();
            List<String[]> lignes = new ArrayList<>(t.lignes().size() + 1);
            t.lignes().forEach(l -> lignes.add(formaterLigne(l, types, false).toArray(String[]::new)));
            if (t.total() != null) {
                lignes.add(formaterLigne(t.total(), types, false).toArray(String[]::new));
            }
            sections.add(new SectionCsv(t.titre(), lireEntetes(t), lignes));
        }
        return csvExportService.addUtf8Bom(csvExportService.createCsvReport(titre, sections));
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(GABARIT, super.getContextVariables());
    }

    // ===== socle des rapports =====

    @Override
    protected String getTemplateAsHtml(Context context) {
        getParameters().forEach(context::setVariable);
        return templateEngine.process(GABARIT, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return parametres.get();
    }

    @Override
    protected String getGenerateFileName() {
        return "journal-remises";
    }

    /**
     * Un tableau mis en texte pour le gabarit ; {@code numerique} aligne la colonne à droite.
     */
    record TableauPdf(
            String titre,
            boolean nouvellePage,
            List<String> entetes,
            List<Boolean> numerique,
            List<List<String>> lignes,
            List<String> total) {}
}

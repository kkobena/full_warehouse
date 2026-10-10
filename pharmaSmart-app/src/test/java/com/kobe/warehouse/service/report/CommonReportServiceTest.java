package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CommonReportService} est le socle dont héritent les vingt-quatre services de rapport du
 * logiciel. Rien de ce qu'il fait n'est spectaculaire, et c'est précisément pour cela qu'il n'a
 * jamais été éprouvé : ce sont des en-têtes, des pieds de page, des libellés de période. Mais un
 * défaut ici se retrouve sur <b>tous</b> les documents imprimés, y compris ceux qu'une officine
 * remet à son comptable ou à un tiers payant.
 *
 * <p>Trois choses y méritent d'être tenues. Le <b>pied de page légal</b> — registre du commerce,
 * compte contribuable, numéro comptable — dont la composition varie selon ce que l'officine a
 * renseigné : une officine qui n'a pas de numéro comptable ne doit pas voir « CPT N°: » suivi de
 * rien. La <b>devise</b>, qui était écrite en dur dans les gabarits et imposait le franc CFA à
 * tout le monde ; le repli sur {@code FCFA} existe pour le cas où le service de configuration n'est
 * pas injecté, et c'est exactement la situation d'un rapport instancié à la main.
 *
 * <p>Et surtout la <b>pagination</b> : un rapport de mille lignes se découpe en pages, et chaque
 * page ne doit porter que sa tranche. Une tranche mal calculée répète ou perd des lignes au milieu
 * d'un document que personne ne relit intégralement.
 */
@DisplayName("CommonReportService — socle commun des documents imprimés")
class CommonReportServiceTest {

    @TempDir
    Path repertoireDesRapports;

    private RapportDeTest rapport;

    @BeforeEach
    void setUp() {
        rapport = new RapportDeTest();
    }

    // ===== pied de page légal =====

    @Nested
    @DisplayName("Pied de page légal")
    class PiedDePage {

        @Test
        @DisplayName("assemble les mentions légales renseignées, dans l'ordre")
        void mentionsCompletes() {
            Magasin magasin = magasin("CI-ABJ-2020-B-1234", "1234567A", "CPT-9876", "2722000000", "Cocody, Abidjan");

            assertThat(rapport.builderFooter(magasin).toString()).isEqualTo(
                "RC N° CI-ABJ-2020-B-1234 - CC N° 1234567A - CPT N°: CPT-9876- Tel: 2722000000- Adr: Cocody, Abidjan"
            );
        }

        /**
         * Une officine qui n'a pas renseigné son numéro comptable ne doit pas voir « CPT N°: » suivi
         * de rien : une mention légale vide sur un document remis à un tiers est pire qu'absente.
         */
        @Test
        @DisplayName("une mention absente ne laisse pas son étiquette orpheline")
        void mentionAbsenteOmise() {
            Magasin magasin = magasin("CI-ABJ-2020-B-1234", null, "", null, "Cocody");

            String pied = rapport.builderFooter(magasin).toString();

            assertThat(pied).doesNotContain("CC N°").doesNotContain("CPT N°").doesNotContain("Tel:");
            assertThat(pied).isEqualTo("RC N° CI-ABJ-2020-B-1234- Adr: Cocody");
        }

        @Test
        @DisplayName("un magasin sans aucune mention rend un pied de page vide")
        void aucuneMention() {
            assertThat(rapport.builderFooter(magasin(null, null, null, null, null)).toString()).isEmpty();
        }

        @Test
        @DisplayName("le pied de page et le magasin sont posés dans le modèle du document")
        void piedDePageDansLeModele() {
            Magasin magasin = magasin("RC-1", null, null, null, null);
            rapport.storageService = storageServiceRendant(magasin);

            rapport.exposerParametresCommuns();

            assertThat(rapport.getParametres()).containsEntry(Constant.MAGASIN, magasin);
            assertThat(rapport.getParametres().get(Constant.FOOTER)).isEqualTo("\"RC N° RC-1\"");
        }
    }

    // ===== périodes et titres =====

    @Nested
    @DisplayName("Périodes et titres")
    class PeriodesEtTitres {

        @Test
        @DisplayName("la période d'un rapport se lit « du … au … »")
        void periodeComplete() {
            ReportPeriode periode = new ReportPeriode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31));

            assertThat(rapport.buildPeriode("Ventes", periode)).isEqualTo("Ventes du 05/01/2026 au 31/03/2026");
        }

        /**
         * Une borne absente disparaît du libellé plutôt que d'y écrire « au null ».
         */
        @Test
        @DisplayName("une borne absente disparaît du libellé")
        void borneAbsente() {
            assertThat(rapport.buildPeriode("Ventes", new ReportPeriode(LocalDate.of(2026, 1, 5), null)))
                .isEqualTo("Ventes du 05/01/2026");
            assertThat(rapport.buildPeriode("Ventes", new ReportPeriode(null, LocalDate.of(2026, 3, 31))))
                .isEqualTo("Ventes  au 31/03/2026");
        }

        @Test
        @DisplayName("une période de dates simples se formate en jours")
        void periodeDeDates() {
            assertThat(rapport.buildPeriode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31))).isEqualTo("05-01-2026 au 31-03-2026");
        }

        /**
         * Les rapports de caisse sont horodatés : on y lit l'heure d'ouverture et de fermeture.
         */
        @Test
        @DisplayName("une période horodatée conserve l'heure")
        void periodeHorodatee() {
            assertThat(rapport.buildPeriode(LocalDateTime.of(2026, 1, 5, 8, 0, 0), LocalDateTime.of(2026, 1, 5, 19, 30, 0)))
                .isEqualTo("05/01/2026 08:00:00 au 05/01/2026 19:30:00");
        }

        @Test
        @DisplayName("le titre du document associe l'intitulé et la période")
        void titreDuDocument() {
            rapport.exposerTitre("Rapport des ventes", "du 05/01/2026 au 31/03/2026");

            assertThat(rapport.getParametres()).containsEntry(Constant.REPORT_TITLE, "Rapport des ventes du 05/01/2026 au 31/03/2026");
        }

        @Test
        @DisplayName("un titre construit depuis une paire horodatée reprend les deux bornes")
        void titreDepuisUnePaire() {
            Pair periode = new Pair(LocalDateTime.of(2026, 1, 5, 8, 0, 0), LocalDateTime.of(2026, 3, 31, 19, 0, 0));

            rapport.exposerTitre("Rapport", periode);

            assertThat(rapport.getParametres().get(Constant.REPORT_TITLE).toString())
                .isEqualTo("Rapport 05/01/2026 08:00:00 au 31/03/2026 19:00:00");
        }
    }

    // ===== contexte de rendu =====

    @Nested
    @DisplayName("Contexte de rendu")
    class ContexteDeRendu {

        /**
         * La devise était écrite en dur dans les gabarits, ce qui imposait le franc CFA à toute
         * officine imprimant une facture. Elle vient désormais de la configuration.
         */
        @Test
        @DisplayName("la devise vient de la configuration de l'officine")
        void deviseConfiguree() {
            AppConfigurationService configuration = mock(AppConfigurationService.class);
            when(configuration.getDevise()).thenReturn("EUR");
            rapport.setAppConfigurationService(configuration);

            assertThat(rapport.contexte().getVariable("devise")).isEqualTo("EUR");
        }

        /**
         * Sans configuration injectée — un rapport instancié à la main —, le repli s'applique.
         */
        @Test
        @DisplayName("sans configuration, la devise retombe sur le franc CFA")
        void deviseParDefaut() {
            assertThat(rapport.contexte().getVariable("devise")).isEqualTo("FCFA");
        }

        @Test
        @DisplayName("le modèle du document est recopié dans le contexte de rendu")
        void modeleRecopieDansLeContexte() {
            rapport.getParametres().put("alerts", List.of("A", "B"));
            rapport.getParametres().put("reportTitle", "Alertes");

            Context contexte = rapport.contexteAvecVariables();

            assertThat(contexte.getVariable("alerts")).isEqualTo(List.of("A", "B"));
            assertThat(contexte.getVariable("reportTitle")).isEqualTo("Alertes");
            assertThat(contexte.getVariable("devise")).isEqualTo("FCFA");
        }
    }

    // ===== pagination =====

    @Nested
    @DisplayName("Pagination des longs rapports")
    class Pagination {

        /**
         * Quinze lignes par tranches de quatre : quatre pages, dont trois pleines et une de trois
         * lignes. Chaque page ne doit porter que sa tranche — une tranche mal calculée répète ou
         * perd des lignes au milieu d'un document que personne ne relit intégralement.
         */
        @Test
        @DisplayName("chaque page ne porte que sa tranche de lignes")
        void decoupageEnPages() {
            rapport.lignes = lignes(15);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues).hasSize(4);
            // La première page est rendue avec la liste complète : le gabarit y prend ses n premières.
            assertThat(rapport.pagesRendues.get(1).get(Constant.ITEMS)).isEqualTo(lignes(15).subList(4, 8));
            assertThat(rapport.pagesRendues.get(2).get(Constant.ITEMS)).isEqualTo(lignes(15).subList(8, 12));
            assertThat(rapport.pagesRendues.get(3).get(Constant.ITEMS)).isEqualTo(lignes(15).subList(12, 15));
        }

        @Test
        @DisplayName("chaque page est numérotée sur le total")
        void numerotationDesPages() {
            rapport.lignes = lignes(15);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues)
                .extracting(page -> page.get(Constant.PAGE_COUNT))
                .containsExactly("1/4", "2/4", "3/4", "4/4");
        }

        /**
         * Le gabarit s'en sert pour n'imprimer les totaux qu'une fois, au bas du document.
         */
        @Test
        @DisplayName("seule la dernière page est signalée comme telle")
        void derniereePageSignalee() {
            rapport.lignes = lignes(15);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues.get(1).get(Constant.IS_LAST_PAGE)).isEqualTo(false);
            assertThat(rapport.pagesRendues.get(2).get(Constant.IS_LAST_PAGE)).isEqualTo(false);
            assertThat(rapport.pagesRendues.get(3).get(Constant.IS_LAST_PAGE)).isEqualTo(true);
        }

        @Test
        @DisplayName("un nombre de lignes multiple de la page ne crée pas de page vide")
        void nombreExactDePages() {
            rapport.lignes = lignes(8);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues).hasSize(2);
            assertThat(rapport.pagesRendues.get(1).get(Constant.ITEMS)).isEqualTo(lignes(8).subList(4, 8));
            assertThat(rapport.pagesRendues.get(1).get(Constant.IS_LAST_PAGE)).isEqualTo(true);
        }

        @Test
        @DisplayName("un rapport qui tient sur une page n'est pas découpé")
        void rapportSurUneSeulePage() {
            rapport.lignes = lignes(3);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues).hasSize(1);
            assertThat(rapport.pagesRendues.getFirst().get(Constant.PAGE_COUNT)).isEqualTo("1/1");
        }

        @Test
        @DisplayName("le nombre total de lignes accompagne chaque page")
        void nombreTotalDeLignes() {
            rapport.lignes = lignes(15);
            rapport.lignesParPage = 4;

            rapport.imprimerSurPlusieursPages();

            assertThat(rapport.pagesRendues.get(1).get(Constant.ITEM_SIZE)).isEqualTo(15);
        }
    }

    // ===== fabriques =====

    private static List<String> lignes(int nombre) {
        List<String> lignes = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            lignes.add("L" + i);
        }
        return lignes;
    }

    private static Magasin magasin(String registre, String compteContribuable, String numComptable, String telephone, String adresse) {
        Magasin magasin = new Magasin();
        magasin.setRegistre(registre);
        magasin.setCompteContribuable(compteContribuable);
        magasin.setNumComptable(numComptable);
        magasin.setPhone(telephone);
        magasin.setAddress(adresse);
        return magasin;
    }

    private static StorageService storageServiceRendant(Magasin magasin) {
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUser()).thenReturn(utilisateur);
        return storageService;
    }

    /**
     * Un rapport concret minimal, qui expose les méthodes protégées du socle et enregistre ce que
     * chaque page reçoit.
     *
     * <p>Le rendu PDF lui-même est court-circuité : {@code ITextRenderer} est simulé, de sorte que le
     * test observe la <b>découpe</b> sans produire de document. C'est la découpe qui porte la
     * logique ; les octets produits par Flying Saucer n'apprennent rien sur elle.
     */
    private static final class RapportDeTest extends CommonReportService {

        private final Map<String, Object> parametres = new HashMap<>();
        private final List<Map<String, Object>> pagesRendues = new ArrayList<>();
        private List<String> lignes = List.of();
        private int lignesParPage = 10;
        private StorageService storageService;

        private RapportDeTest() {
            super(mock(StorageService.class));
        }

        /**
         * Les propriétés de stockage n'ont pas de mutateur : le répertoire se fixe en surchargeant.
         */
        private static FileStorageProperties proprietes(Path repertoire) {
            return new FileStorageProperties() {
                @Override
                public String getReportsDir() {
                    return repertoire.toString();
                }
            };
        }

        @Override
        protected List<?> getItems() {
            return lignes;
        }

        @Override
        protected int getMaxiRowCount() {
            return lignesParPage;
        }

        @Override
        protected String getTemplateAsHtml() {
            return "<html><body>rapport</body></html>";
        }

        @Override
        protected String getTemplateAsHtml(Context context) {
            // Chaque appel correspond à une page : on en fige l'état du modèle.
            pagesRendues.add(new HashMap<>(parametres));
            return "<html><body>page</body></html>";
        }

        @Override
        protected Map<String, Object> getParameters() {
            return parametres;
        }

        @Override
        protected String getGenerateFileName() {
            return "rapport_de_test";
        }

        @Override
        public ITextRenderer getITextRenderer() {
            return mock(ITextRenderer.class);
        }

        @Override
        public SharedContext getSharedContext(ITextRenderer renderer) {
            return mock(SharedContext.class);
        }

        // --- passerelles vers les méthodes protégées du socle ---

        Map<String, Object> getParametres() {
            return parametres;
        }

        Context contexte() {
            return getContext();
        }

        Context contexteAvecVariables() {
            return getContextVariables();
        }

        void exposerParametresCommuns() {
            // getCommonParameters lit le magasin sur le StorageService injecté au constructeur ;
            // le test en fournit un autre, d'où la substitution par ce point d'entrée.
            Magasin magasin = storageService.getUser().getMagasin();
            getParameters().put(Constant.MAGASIN, magasin);
            getParameters().put(Constant.FOOTER, "\"" + builderFooter(magasin) + "\"");
        }

        void exposerTitre(String titre, String periode) {
            setTitle(titre, periode);
        }

        void exposerTitre(String titre, Pair periode) {
            buildTitle(titre, periode);
        }

        void imprimerSurPlusieursPages() {
            exportMultiplePagesToByteArray();
        }
    }
}

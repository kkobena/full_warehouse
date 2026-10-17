package com.kobe.warehouse.service.etiquette.zpl.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le ZPL part tel quel dans l'imprimante : une commande fausse ne lève rien, elle sort une
 * étiquette illisible ou fait dérouler le rouleau. Ces contrôles tiennent lieu d'aperçu.
 */
@DisplayName("ZplEtiquetteServiceImpl")
class ZplEtiquetteServiceImplTest {

    private static final FormatEtiquette ROULEAU = FormatEtiquette.ROULEAU_40X30;

    /** Les rouleaux seuls : une planche A4 passe par le PDF, jamais par le ZPL. */
    static List<FormatEtiquette> rouleaux() {
        return FormatEtiquette.listerRouleaux();
    }

    private final ZplEtiquetteServiceImpl service = new ZplEtiquetteServiceImpl();

    private static EtiquetteDTO etiquetteStandard(String code) {
        return new EtiquetteDTO()
            .setCode(code)
            .setLibelle("DOLIPRANE 1000MG")
            .setPrix("1 500 CFA")
            .setMagasin("PHARMACIE DU PLATEAU")
            .setDate("16/10/2026")
            .setPrint(true);
    }

    private static EtiquetteDTO etiquetteDeLot() {
        return etiquetteStandard("3794609")
            .setNumLot("L26050002")
            .setExpiryDate("30/05/2027")
            .setCodeDataMatrix("dm:2403794609{GS}10L26050002{GS}17270530");
    }

    private String rendre(List<EtiquetteDTO> etiquettes, FormatEtiquette format, ResolutionEtiquette resolution) {
        return rendre(etiquettes, TypeEtiquette.STANDARD, format, resolution);
    }

    private String rendre(List<EtiquetteDTO> etiquettes, TypeEtiquette type, FormatEtiquette format, ResolutionEtiquette resolution) {
        return service.rendreEtiquettes(etiquettes, type, format, resolution);
    }

    /** Compte les occurrences d'une commande ZPL : le caractère ^ interdit une découpe en regex. */
    private static int compter(String flux, String commande) {
        int total = 0;
        for (int position = flux.indexOf(commande); position >= 0; position = flux.indexOf(commande, position + 1)) {
            total++;
        }
        return total;
    }

    @Nested
    @DisplayName("structure du flux")
    class Structure {

        @Test
        @DisplayName("un bloc ^XA … ^XZ par etiquette a imprimer")
        void unBlocParEtiquette() {
            String zpl = rendre(
                List.of(etiquetteStandard("3794609"), etiquetteStandard("3794610"), etiquetteStandard("3794611")),
                ROULEAU,
                ResolutionEtiquette.DPI_203
            );

            assertThat(compter(zpl, "^XA")).isEqualTo(3);
            assertThat(compter(zpl, "^XZ")).isEqualTo(3);
        }

        @Test
        @DisplayName("une case blanche de planche ne deroule rien")
        void casesBlanchesIgnorees() {
            String zpl = rendre(
                List.of(new EtiquetteDTO().setPrint(false), etiquetteStandard("3794609")),
                ROULEAU,
                ResolutionEtiquette.DPI_203
            );

            assertThat(compter(zpl, "^XA")).isEqualTo(1);
        }

        @Test
        @DisplayName("sans etiquette, le flux est vide — rien n est envoye a l imprimante")
        void fluxVide() {
            assertThat(rendre(List.of(), ROULEAU, ResolutionEtiquette.DPI_203)).isEmpty();
        }

        @Test
        @DisplayName("declare l UTF-8, sans quoi un libelle accentue sort en charabia")
        void jeuDeCaracteres() {
            String zpl = rendre(List.of(etiquetteStandard("3794609")), ROULEAU, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^CI28");
        }
    }

    @Nested
    @DisplayName("geometrie")
    class Geometrie {

        @ParameterizedTest(name = "{0} declare sa page en points de la tete d impression")
        @MethodSource("com.kobe.warehouse.service.etiquette.zpl.impl.ZplEtiquetteServiceImplTest#rouleaux")
        void pageDeclaree(FormatEtiquette format) {
            String zpl = rendre(List.of(etiquetteStandard("3794609")), format, ResolutionEtiquette.DPI_203);

            int largeurDots = (int) Math.round(format.getLargeurMm() * 203 / 25.4);
            int hauteurDots = (int) Math.round(format.getHauteurMm() * 203 / 25.4);
            assertThat(zpl).contains("^PW" + largeurDots);
            assertThat(zpl).contains("^LL" + hauteurDots);
        }

        @ParameterizedTest(name = "a {0}, la page suit la resolution")
        @EnumSource(ResolutionEtiquette.class)
        void pageSuitLaResolution(ResolutionEtiquette resolution) {
            String zpl = rendre(List.of(etiquetteStandard("3794609")), ROULEAU, resolution);

            // 40 mm a 203 dpi font 320 points, et 472 a 300 : la meme etiquette, pas la meme cote.
            assertThat(zpl).contains("^PW" + (int) Math.round(40.0 * resolution.getDpi() / 25.4));
            assertThat(zpl).contains("^LL" + (int) Math.round(30.0 * resolution.getDpi() / 25.4));
        }

        @Test
        @DisplayName("la page est redeclaree a chaque etiquette")
        void pageRedeclaree() {
            String zpl = rendre(List.of(etiquetteStandard("3794609"), etiquetteStandard("3794610")), ROULEAU, ResolutionEtiquette.DPI_203);

            // L'imprimante garde la derniere configuration recue : sans redeclaration, une
            // etiquette imprimee apres un travail d'un autre format sortirait a sa longueur.
            assertThat(compter(zpl, "^PW")).isEqualTo(2);
        }

        @Test
        @DisplayName("le nom de l officine n apparait que sur une etiquette assez haute")
        void nomDeLOfficine() {
            String haute = rendre(List.of(etiquetteStandard("3794609")), FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);
            String basse = rendre(List.of(etiquetteStandard("3794609")), FormatEtiquette.ROULEAU_40X20, ResolutionEtiquette.DPI_203);

            assertThat(haute).contains("PHARMACIE DU PLATEAU");
            // Sous 25 mm, le nom pousserait le prix hors de la decoupe — regle du gabarit PDF.
            assertThat(basse).doesNotContain("PHARMACIE DU PLATEAU");
            assertThat(basse).contains("DOLIPRANE 1000MG");
        }
    }

    @Nested
    @DisplayName("symboles")
    class Symboles {

        @Test
        @DisplayName("une etiquette de lot porte un GS1 DataMatrix, FNC1 compris")
        void dataMatrixDuLot() {
            String zpl = rendre(List.of(etiquetteDeLot()), TypeEtiquette.LOT, ROULEAU, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^BXN,");
            // _1 est le FNC1 : en tete il declare un GS1 DataMatrix, entre deux AI il les separe.
            assertThat(zpl).contains("^FD_12403794609_110L26050002_117270530^FS");
            assertThat(zpl).doesNotContain("{GS}");
            assertThat(zpl).doesNotContain("dm:");
        }

        @Test
        @DisplayName("l etiquette de lot ecrit aussi le lot et la peremption en clair")
        void texteDuLot() {
            String zpl = rendre(List.of(etiquetteDeLot()), TypeEtiquette.LOT, ROULEAU, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^FDLot L26050002^FS");
            assertThat(zpl).contains("^FDExp 30/05/2027^FS");
            assertThat(zpl).contains("^FD1 500 CFA^FS");
        }

        @Test
        @DisplayName("une etiquette standard large porte un Code 39")
        void code39QuandIlTient() {
            String zpl = rendre(List.of(etiquetteStandard("3794609")), FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^B3N,N,");
            assertThat(zpl).contains("^FD3794609^FS");
        }

        @Test
        @DisplayName("un Code 39 qui ne tiendrait pas cede la place a un DataMatrix")
        void repliSurDataMatrix() {
            // Dix caracteres a 0,25 mm de module demandent 39 mm : le 30 x 20 ne les porte pas.
            String zpl = rendre(List.of(etiquetteStandard("3794609123")), FormatEtiquette.ROULEAU_30X20, ResolutionEtiquette.DPI_203);

            assertThat(zpl).doesNotContain("^B3");
            assertThat(zpl).contains("^BXN,");
            assertThat(zpl).contains("^FD_12403794609123^FS");
        }

        @ParameterizedTest(name = "sur {0}, un Code 39 trace tient dans la laize")
        @MethodSource("com.kobe.warehouse.service.etiquette.zpl.impl.ZplEtiquetteServiceImplTest#rouleaux")
        void code39TientToujoursDansLaLaize(FormatEtiquette format) {
            for (int longueur = 7; longueur <= 10; longueur++) {
                String code = "1".repeat(longueur);
                String zpl = rendre(List.of(etiquetteStandard(code)), format, ResolutionEtiquette.DPI_203);
                if (zpl.contains("^B3")) {
                    // 13 modules par caractere au rapport 2, plus les deux caracteres de service.
                    double module = Math.max(2, Math.round(0.25 * 203 / 25.4)) * 25.4 / 203;
                    double largeur = (longueur + 2) * 13 * module;
                    assertThat(largeur).isLessThanOrEqualTo(format.getLargeurCodeBarresMm());
                }
            }
        }
    }

    @Nested
    @DisplayName("usages")
    class Usages {

        private static EtiquetteDTO posologie() {
            return new EtiquetteDTO()
                .setLibelle("DOLIPRANE 1000MG")
                .setPatient("KOUASSI Awa")
                .setPrescripteur("Dr TRAORE Issa — cardiologue")
                .setPosologie("1 comprimé matin, midi et soir, au moment des repas")
                .setDuree("Pendant 7 jours")
                .setMagasin("PHARMACIE DU PLATEAU")
                .setDate("16/10/2026")
                .setPrint(true);
        }

        private static EtiquetteDTO preparation() {
            return new EtiquetteDTO()
                .setLibelle("Pommade salicylée 5 %")
                .setComposition("Acide salicylique 5 g, vaseline qsp 100 g")
                .setPatient("KOUASSI Awa")
                .setPrescripteur("Dr TRAORE Issa")
                .setPosologie("Appliquer matin et soir sur la zone atteinte")
                .setMention("À usage externe")
                .setMagasin("PHARMACIE DU PLATEAU")
                .setDate("16/10/2026")
                .setExpiryDate("16/01/2027")
                .setPrint(true);
        }

        @Test
        @DisplayName("la reglette met le prix en gros et garde le code discret")
        void reglette() {
            String zpl = rendre(
                List.of(etiquetteStandard("3794609")),
                TypeEtiquette.REGLETTE,
                FormatEtiquette.ROULEAU_58X40,
                ResolutionEtiquette.DPI_203
            );

            int hauteurPrix = (int) Math.round(6.0 * 203 / 25.4);
            int hauteurTexte = (int) Math.round(2.6 * 203 / 25.4);
            assertThat(zpl).contains("^A0N," + hauteurPrix + "," + hauteurPrix + "^FD1 500 CFA^FS");
            assertThat(zpl).contains("^A0N," + hauteurTexte + "," + hauteurTexte + "^FDDOLIPRANE 1000MG^FS");
            assertThat(zpl).contains("^B3N,N,");
        }

        @Test
        @DisplayName("la reglette garde le code en clair quand le Code 39 ne tient pas")
        void regletteSansCode39() {
            String zpl = rendre(
                List.of(etiquetteStandard("3794609123")),
                TypeEtiquette.REGLETTE,
                FormatEtiquette.ROULEAU_30X20,
                ResolutionEtiquette.DPI_203
            );

            // Pas de repli en DataMatrix : au rayon, le code n'est qu'une verification.
            assertThat(zpl).doesNotContain("^B3");
            assertThat(zpl).doesNotContain("^BX");
            assertThat(zpl).contains("^FD3794609123^FS");
        }

        @Test
        @DisplayName("la posologie ne porte ni prix ni code-barres")
        void posologieSansPrix() {
            String zpl = rendre(List.of(posologie()), TypeEtiquette.POSOLOGIE, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).doesNotContain("CFA");
            assertThat(zpl).doesNotContain("^B3");
            assertThat(zpl).doesNotContain("^BX");
            assertThat(zpl).contains("^FDKOUASSI Awa^FS");
            // Le tiret cadratin passe tel quel : seuls ^, ~ et _ sont des caractères de commande.
            assertThat(zpl).contains("^FDDr TRAORE Issa — cardiologue^FS");
            assertThat(zpl).contains("^FDPendant 7 jours^FS");
        }

        @Test
        @DisplayName("une posologie longue est repliee en bloc, pas ecrite hors de l etiquette")
        void posologieEnBloc() {
            String zpl = rendre(List.of(posologie()), TypeEtiquette.POSOLOGIE, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            // ^FB declare la largeur du bloc et le nombre de lignes : ZPL ne replie rien tout seul.
            assertThat(zpl).contains("^FB");
            assertThat(zpl).contains("1 comprimé matin, midi et soir, au moment des repas");
        }

        @Test
        @DisplayName("la preparation porte composition, dates et mention")
        void preparationComplete() {
            String zpl = rendre(
                List.of(preparation()),
                TypeEtiquette.PREPARATION,
                FormatEtiquette.ROULEAU_58X40,
                ResolutionEtiquette.DPI_203
            );

            assertThat(zpl).contains("Acide salicylique 5 g, vaseline qsp 100 g");
            assertThat(zpl).contains("^FDPréparé le 16/10/2026^FS");
            assertThat(zpl).contains("^FDÀ utiliser avant le 16/01/2027^FS");
            assertThat(zpl).contains("^FDÀ usage externe^FS");
            assertThat(zpl).doesNotContain("CFA");
        }

        @Test
        @DisplayName("un usage declare sa page comme les autres etiquettes")
        void pageDeclaree() {
            String zpl = rendre(List.of(posologie()), TypeEtiquette.POSOLOGIE, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^CI28");
            assertThat(zpl).contains("^PW" + (int) Math.round(58.0 * 203 / 25.4));
            assertThat(compter(zpl, "^XA")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("assainissement")
    class Assainissement {

        @Test
        @DisplayName("les caracteres de commande d un libelle sont neutralises")
        void libelleAvecCaracteresDeCommande() {
            String zpl = rendre(
                List.of(etiquetteStandard("3794609").setLibelle("SIROP ^TOUX~ 100_ML")),
                ROULEAU,
                ResolutionEtiquette.DPI_203
            );

            // ^ et ~ ouvrent une commande ZPL, _ porte nos FNC1 : laisses tels quels, ils
            // couperaient l'etiquette en deux.
            assertThat(zpl).contains("^FDSIROP -TOUX- 100-ML^FS");
        }

        @Test
        @DisplayName("un numero de lot a caractere de commande ne casse pas la trame")
        void lotAvecCaractereDeCommande() {
            String zpl = rendre(
                List.of(etiquetteDeLot().setNumLot("L26_05").setCodeDataMatrix("dm:2403794609{GS}10L26_05")),
                TypeEtiquette.LOT,
                ROULEAU,
                ResolutionEtiquette.DPI_203
            );

            assertThat(zpl).contains("^FD_12403794609_110L26-05^FS");
            assertThat(zpl).contains("^FDLot L26-05^FS");
        }
    }
}

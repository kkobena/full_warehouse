package com.kobe.warehouse.domain.enumeration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Une cote fausse dans le catalogue ne se voit pas à la relecture du code : elle se voit sur la
 * planche imprimée, après avoir gâché le consommable. Ces contrôles tiennent lieu de garde-fou.
 */
@DisplayName("FormatEtiquette")
class FormatEtiquetteTest {

    /** Tolérance d'un dixième de millimètre : les cotes des planches sont données à 0,1 mm. */
    private static final double TOLERANCE_MM = 0.1;

    /** Les contrôles de grille ne valent que pour une planche : un rouleau n'a pas de page A4. */
    static List<FormatEtiquette> planches() {
        return FormatEtiquette.listerPlanches();
    }

    static List<FormatEtiquette> rouleaux() {
        return FormatEtiquette.listerRouleaux();
    }

    @Nested
    @DisplayName("geometrie")
    class Geometrie {

        @ParameterizedTest(name = "{0} tient dans la largeur d une page A4")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#planches")
        void tientDansLaLargeur(FormatEtiquette format) {
            // La dernière colonne ne porte pas de gouttière : elle sortirait de la page.
            double occupee =
                2 * format.getMargeCoteMm() + format.getColonnes() * format.getPasHorizontalMm() - format.getGouttiereHorizontaleMm();

            assertThat(occupee).isLessThanOrEqualTo(FormatEtiquette.LARGEUR_A4_MM + TOLERANCE_MM);
        }

        @ParameterizedTest(name = "{0} tient dans la hauteur d une page A4")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#planches")
        void tientDansLaHauteur(FormatEtiquette format) {
            double occupee =
                2 * format.getMargeHautMm() + format.getRangees() * format.getPasVerticalMm() - format.getGouttiereVerticaleMm();

            assertThat(occupee).isLessThanOrEqualTo(FormatEtiquette.HAUTEUR_A4_MM + TOLERANCE_MM);
        }

        @ParameterizedTest(name = "{0} ne laisse pas plus d une etiquette de marge perdue")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#planches")
        void margesPlausibles(FormatEtiquette format) {
            // Une marge supérieure à une étiquette entière trahit une grille mal transcrite :
            // le format tiendrait une rangée ou une colonne de plus.
            double resteLargeur =
                FormatEtiquette.LARGEUR_A4_MM - (2 * format.getMargeCoteMm() + format.getColonnes() * format.getPasHorizontalMm());
            double resteHauteur =
                FormatEtiquette.HAUTEUR_A4_MM - (2 * format.getMargeHautMm() + format.getRangees() * format.getPasVerticalMm());

            assertThat(resteLargeur).isLessThan(format.getLargeurMm());
            assertThat(resteHauteur).isLessThan(format.getHauteurMm());
        }

        @ParameterizedTest(name = "{0} a des cotes strictement positives")
        @EnumSource(FormatEtiquette.class)
        void cotesPositives(FormatEtiquette format) {
            assertThat(format.getLargeurMm()).isPositive();
            assertThat(format.getHauteurMm()).isPositive();
            assertThat(format.getColonnes()).isPositive();
            assertThat(format.getRangees()).isPositive();
            assertThat(format.getMargeCoteMm()).isNotNegative();
            assertThat(format.getMargeHautMm()).isNotNegative();
            assertThat(format.getGouttiereHorizontaleMm()).isNotNegative();
            assertThat(format.getGouttiereVerticaleMm()).isNotNegative();
        }

        @ParameterizedTest(name = "{0} annonce une capacite egale a sa grille")
        @EnumSource(FormatEtiquette.class)
        void capaciteEgaleLaGrille(FormatEtiquette format) {
            assertThat(format.getCapacite()).isEqualTo(format.getColonnes() * format.getRangees());
        }

        @ParameterizedTest(name = "{0} porte un libelle et une reference")
        @EnumSource(FormatEtiquette.class)
        void libelleEtReference(FormatEtiquette format) {
            assertThat(format.getLibelle()).isNotBlank();
            assertThat(format.getReference()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("format par defaut")
    class ParDefaut {

        @Test
        @DisplayName("reprend le calage mesure sur le PDF d avant le catalogue")
        void geometrieHistorique() {
            FormatEtiquette format = FormatEtiquette.parDefaut();

            assertThat(format).isEqualTo(FormatEtiquette.A4_65_HISTORIQUE);
            assertThat(format.getLargeurMm()).isEqualTo(38.0);
            assertThat(format.getHauteurMm()).isEqualTo(21.2);
            assertThat(format.getColonnes()).isEqualTo(5);
            assertThat(format.getRangees()).isEqualTo(13);
            assertThat(format.getMargeCoteMm()).isEqualTo(5.0);
            assertThat(format.getMargeHautMm()).isEqualTo(5.0);
            // Pas de 40 mm : c'est ce que l'ancien gabarit imprimait, mesure faite.
            assertThat(format.getPasHorizontalMm()).isEqualTo(40.0);
            assertThat(format.getPasVerticalMm()).isEqualTo(21.2);
            // 5 x 40 = 200 mm : la gouttiere de la derniere colonne tombe pile sur la marge.
            assertThat(format.getLargeurGrilleMm()).isEqualTo(200.0);
            assertThat(format.isDerniereColonneRognee()).isFalse();
        }

        @Test
        @DisplayName("compte 65 etiquettes — c est la borne de la case de depart")
        void soixanteCinqEtiquettes() {
            assertThat(FormatEtiquette.parDefaut().getCapacite()).isEqualTo(65);
        }

        @Test
        @DisplayName("sans gouttiere, le pas egale l etiquette")
        void pasSansGouttiere() {
            assertThat(FormatEtiquette.A4_65_L7651.getPasHorizontalMm()).isEqualTo(38.1);
            assertThat(FormatEtiquette.A4_65_L7651.getPasVerticalMm()).isEqualTo(21.2);
        }
    }

    @Nested
    @DisplayName("capacites annoncees")
    class Capacites {

        @Test
        @DisplayName("chaque format porte le nombre d etiquettes de son nom")
        void capacitesConformesAuNom() {
            assertThat(FormatEtiquette.A4_65_HISTORIQUE.getCapacite()).isEqualTo(65);
            assertThat(FormatEtiquette.A4_65_L7651.getCapacite()).isEqualTo(65);
            assertThat(FormatEtiquette.A4_48_45X21.getCapacite()).isEqualTo(48);
            assertThat(FormatEtiquette.A4_40_45X25.getCapacite()).isEqualTo(40);
            assertThat(FormatEtiquette.A4_24_63X34.getCapacite()).isEqualTo(24);
            assertThat(FormatEtiquette.A4_21_63X38.getCapacite()).isEqualTo(21);
            assertThat(FormatEtiquette.A4_14_99X38.getCapacite()).isEqualTo(14);
        }

        @ParameterizedTest(name = "la grille de {0} tient entre les marges")
        @EnumSource(FormatEtiquette.class)
        void grilleEntreLesMarges(FormatEtiquette format) {
            assertThat(format.getMargeCoteMm() + format.getLargeurGrilleMm())
                .isLessThanOrEqualTo(FormatEtiquette.LARGEUR_A4_MM - format.getMargeCoteMm() + TOLERANCE_MM);
        }

        @Test
        @DisplayName("aucun doublon de geometrie dans le catalogue")
        void pasDeDoublon() {
            assertThat(FormatEtiquette.values())
                .extracting(f -> f.getLargeurMm() + "x" + f.getHauteurMm() + "/" + f.getColonnes() + "x" + f.getRangees())
                .doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("rouleaux")
    class Rouleaux {

        /** Au-dela, ce n'est plus un rouleau d'etiquettes mais une planche : la laize est bornee. */
        private static final double LAIZE_MAXI_MM = 110.0;

        @Test
        @DisplayName("le catalogue en propose, et aucun n est le format par defaut")
        void catalogueNonVide() {
            assertThat(FormatEtiquette.listerRouleaux()).isNotEmpty();
            assertThat(FormatEtiquette.listerPlanches()).isNotEmpty();
            assertThat(FormatEtiquette.parDefaut().isRouleau()).isFalse();
            // Les deux familles couvrent le catalogue, sans recouvrement.
            assertThat(FormatEtiquette.listerPlanches().size() + FormatEtiquette.listerRouleaux().size())
                .isEqualTo(FormatEtiquette.values().length);
        }

        @ParameterizedTest(name = "{0} n a qu une seule case")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#rouleaux")
        void uneSeuleCase(FormatEtiquette format) {
            assertThat(format.isRouleau()).isTrue();
            assertThat(format.getSupport()).isEqualTo(SupportEtiquette.ROULEAU);
            assertThat(format.getColonnes()).isEqualTo(1);
            assertThat(format.getRangees()).isEqualTo(1);
            assertThat(format.getCapacite()).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0} ne porte ni marge de planche ni gouttiere")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#rouleaux")
        void sansMargeDePlanche(FormatEtiquette format) {
            // La page EST l'etiquette : une marge de planche decalerait le contenu de la decoupe.
            assertThat(format.getMargeCoteMm()).isZero();
            assertThat(format.getMargeHautMm()).isZero();
            assertThat(format.getGouttiereHorizontaleMm()).isZero();
            assertThat(format.getGouttiereVerticaleMm()).isZero();
        }

        @ParameterizedTest(name = "{0} tient dans une laize d imprimante")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#rouleaux")
        void cotesPlausibles(FormatEtiquette format) {
            assertThat(format.getLargeurMm()).isBetween(20.0, LAIZE_MAXI_MM);
            assertThat(format.getHauteurMm()).isBetween(15.0, LAIZE_MAXI_MM);
            // Le DataMatrix fait 11 mm : en dessous, l etiquette de lot ne tient plus.
            assertThat(format.getHauteurMm()).isGreaterThanOrEqualTo(13.0);
        }

        @ParameterizedTest(name = "le code-barres de {0} laisse la place au texte")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#rouleaux")
        void hauteurDuCodeBarres(FormatEtiquette format) {
            // Sans hauteur declaree, le symbole prend toute la page et chasse le prix dehors.
            assertThat(format.getHauteurCodeBarresMm()).isGreaterThanOrEqualTo(8.0);
            assertThat(format.getHauteurCodeBarresMm()).isLessThan(format.getHauteurMm());
            assertThat(format.getLargeurCodeBarresMm()).isLessThan(format.getLargeurMm());
            assertThat(format.getLargeurCodeBarresMm()).isPositive();
        }

        @ParameterizedTest(name = "{0} ignore la case de depart")
        @MethodSource("com.kobe.warehouse.domain.enumeration.FormatEtiquetteTest#rouleaux")
        void caseDeDepartIgnoree(FormatEtiquette format) {
            // Une case de depart y ferait derouler des etiquettes blanches : du consommable perdu.
            assertThat(format.normaliserCaseDeDepart(1)).isEqualTo(1);
            assertThat(format.normaliserCaseDeDepart(40)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("case de depart")
    class CaseDeDepart {

        @Test
        @DisplayName("une case valide est gardee telle quelle")
        void caseValide() {
            assertThat(FormatEtiquette.A4_65_HISTORIQUE.normaliserCaseDeDepart(12)).isEqualTo(12);
            assertThat(FormatEtiquette.A4_65_HISTORIQUE.normaliserCaseDeDepart(65)).isEqualTo(65);
        }

        @Test
        @DisplayName("une case hors planche est ramenee dans les bornes")
        void caseHorsBornes() {
            assertThat(FormatEtiquette.A4_21_63X38.normaliserCaseDeDepart(40)).isEqualTo(21);
            assertThat(FormatEtiquette.A4_65_HISTORIQUE.normaliserCaseDeDepart(0)).isEqualTo(1);
            assertThat(FormatEtiquette.A4_65_HISTORIQUE.normaliserCaseDeDepart(-3)).isEqualTo(1);
        }
    }
}

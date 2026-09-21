package com.kobe.warehouse.service.mobile.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.service.mobile.util.PaginationHelper.PaginationMetadata;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Toute la pagination de l'API mobile passe par cet utilitaire. Ce qui s'y joue n'est pas le
 * découpage — un {@code subList} ne se trompe pas — mais la <b>défense contre ce que le client
 * envoie</b>.
 *
 * <p>Une application mobile envoie ce qu'elle veut : une taille de page de dix mille pour « tout
 * charger d'un coup », un numéro de page négatif après une erreur de calcul, un numéro au-delà de la
 * dernière page après qu'un article a été supprimé entre deux défilements. Chacun de ces cas doit se
 * ramener à quelque chose de servable : une taille plafonnée pour que le serveur ne s'effondre pas,
 * une page ramenée dans les bornes pour que l'utilisateur voie des résultats plutôt qu'une liste
 * vide inexpliquée.
 *
 * <p>Les <b>en-têtes</b> comptent autant que le corps : c'est sur eux que le client décide d'afficher
 * ou non le bouton « page suivante », et ils doivent traverser CORS pour lui parvenir.
 */
@DisplayName("PaginationHelper — pagination de l'API mobile")
class PaginationHelperTest {

    // ===== taille de page =====

    @Nested
    @DisplayName("Taille de page")
    class TailleDePage {

        /** Un client qui demande dix mille éléments ferait tomber le serveur : la taille est plafonnée. */
        @Test
        @DisplayName("une taille excessive est ramenée au plafond")
        void taillePlafonnee() {
            assertThat(PaginationHelper.validateSize(10_000)).isEqualTo(PaginationHelper.MAX_SIZE);
            assertThat(PaginationHelper.validateSize(101)).isEqualTo(100);
        }

        @Test
        @DisplayName("une taille absurde retombe sur la taille par défaut")
        void tailleAbsurde() {
            assertThat(PaginationHelper.validateSize(0)).isEqualTo(PaginationHelper.DEFAULT_SIZE);
            assertThat(PaginationHelper.validateSize(-5)).isEqualTo(PaginationHelper.DEFAULT_SIZE);
        }

        @Test
        @DisplayName("une taille raisonnable est conservée")
        void tailleConservee() {
            assertThat(PaginationHelper.validateSize(50)).isEqualTo(50);
            assertThat(PaginationHelper.validateSize(100)).isEqualTo(100);
        }
    }

    // ===== nombre de pages =====

    @Nested
    @DisplayName("Nombre de pages")
    class NombreDePages {

        @Test
        @DisplayName("un reste occupe une page de plus")
        void resteOccupeUnePage() {
            assertThat(PaginationHelper.calculateTotalPages(100, 20)).isEqualTo(5);
            assertThat(PaginationHelper.calculateTotalPages(101, 20)).isEqualTo(6);
            assertThat(PaginationHelper.calculateTotalPages(1, 20)).isEqualTo(1);
        }

        @Test
        @DisplayName("sans élément, il n'y a aucune page")
        void aucunElement() {
            assertThat(PaginationHelper.calculateTotalPages(0, 20)).isZero();
            assertThat(PaginationHelper.calculateTotalPages(-3, 20)).isZero();
        }

        /** Diviser par zéro n'a pas de sens : le calcul répond zéro plutôt que de lever une erreur. */
        @Test
        @DisplayName("une taille nulle ne provoque pas de division par zéro")
        void tailleNulle() {
            assertThat(PaginationHelper.calculateTotalPages(100, 0)).isZero();
        }
    }

    // ===== numéro de page =====

    @Nested
    @DisplayName("Numéro de page")
    class NumeroDePage {

        @Test
        @DisplayName("un numéro négatif est ramené à la première page")
        void numeroNegatif() {
            assertThat(PaginationHelper.validatePage(-1, 5)).isZero();
            assertThat(PaginationHelper.validatePage(-100, 5)).isZero();
        }

        /**
         * Un article supprimé entre deux défilements peut faire disparaître la dernière page sous les
         * pieds du client : le ramener sur la dernière page existante lui montre des résultats plutôt
         * qu'une liste vide qu'il prendrait pour une panne.
         */
        @Test
        @DisplayName("un numéro au-delà de la dernière page y est ramené")
        void numeroAuDela() {
            assertThat(PaginationHelper.validatePage(10, 5)).isEqualTo(4);
            assertThat(PaginationHelper.validatePage(5, 5)).isEqualTo(4);
        }

        @Test
        @DisplayName("un numéro dans les bornes est conservé")
        void numeroValide() {
            assertThat(PaginationHelper.validatePage(0, 5)).isZero();
            assertThat(PaginationHelper.validatePage(3, 5)).isEqualTo(3);
        }
    }

    // ===== découpage =====

    @Nested
    @DisplayName("Découpage de la liste")
    class Decoupage {

        @Test
        @DisplayName("chaque page porte sa tranche d'éléments")
        void tranchesSuccessives() {
            List<Integer> elements = elements(25);

            assertThat(PaginationHelper.paginateList(elements, 0, 10)).containsExactlyElementsOf(elements.subList(0, 10));
            assertThat(PaginationHelper.paginateList(elements, 1, 10)).containsExactlyElementsOf(elements.subList(10, 20));
        }

        @Test
        @DisplayName("la dernière page peut être partielle")
        void dernierePagePartielle() {
            assertThat(PaginationHelper.paginateList(elements(25), 2, 10)).hasSize(5);
        }

        @Test
        @DisplayName("une page au-delà des données rend une liste vide")
        void pageAuDelaDesDonnees() {
            assertThat(PaginationHelper.paginateList(elements(25), 10, 10)).isEmpty();
        }

        @Test
        @DisplayName("une liste absente ou vide rend une liste vide, sans erreur")
        void listeAbsenteOuVide() {
            assertThat(PaginationHelper.paginateList(null, 0, 10)).isEmpty();
            assertThat(PaginationHelper.paginateList(List.of(), 0, 10)).isEmpty();
        }

        /**
         * {@code paginateList} ne valide pas le numéro de page — c'est {@code createPaginatedResponse}
         * qui le fait avant de l'appeler. Appelée directement avec un numéro négatif, elle échoue.
         * Le test fige ce partage des responsabilités : la méthode est publique, et un futur appelant
         * doit savoir qu'elle attend un numéro déjà validé.
         */
        @Test
        @DisplayName("appelée directement, elle attend un numéro de page déjà validé")
        void numeroNonValide() {
            assertThatThrownBy(() -> PaginationHelper.paginateList(elements(25), -1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("la tranche rendue est indépendante de la liste d'origine")
        void trancheIndependante() {
            List<Integer> elements = new ArrayList<>(elements(25));

            List<Integer> tranche = PaginationHelper.paginateList(elements, 0, 10);
            elements.clear();

            assertThat(tranche).hasSize(10);
        }
    }

    // ===== réponse paginée =====

    @Nested
    @DisplayName("Réponse paginée")
    class ReponsePaginee {

        @Test
        @DisplayName("le corps ne porte que la page demandée")
        void corpsDeLaReponse() {
            ResponseEntity<List<Integer>> reponse = PaginationHelper.createPaginatedResponse(elements(25), 1, 10);

            assertThat(reponse.getBody()).hasSize(10).containsExactlyElementsOf(elements(25).subList(10, 20));
        }

        /** C'est sur ces en-têtes que le client décide d'afficher ou non « page suivante ». */
        @Test
        @DisplayName("les en-têtes décrivent la position dans l'ensemble")
        void entetesDePagination() {
            HttpHeaders entetes = PaginationHelper.createPaginatedResponse(elements(25), 1, 10).getHeaders();

            assertThat(entetes.getFirst(PaginationHelper.HEADER_TOTAL_COUNT)).isEqualTo("25");
            assertThat(entetes.getFirst(PaginationHelper.HEADER_TOTAL_PAGES)).isEqualTo("3");
            assertThat(entetes.getFirst(PaginationHelper.HEADER_CURRENT_PAGE)).isEqualTo("1");
            assertThat(entetes.getFirst(PaginationHelper.HEADER_PAGE_SIZE)).isEqualTo("10");
            assertThat(entetes.getFirst(PaginationHelper.HEADER_HAS_NEXT)).isEqualTo("true");
            assertThat(entetes.getFirst(PaginationHelper.HEADER_HAS_PREVIOUS)).isEqualTo("true");
        }

        @Test
        @DisplayName("la première page n'a pas de précédente, la dernière pas de suivante")
        void bornesDeLEnsemble() {
            HttpHeaders premiere = PaginationHelper.createPaginatedResponse(elements(25), 0, 10).getHeaders();
            HttpHeaders derniere = PaginationHelper.createPaginatedResponse(elements(25), 2, 10).getHeaders();

            assertThat(premiere.getFirst(PaginationHelper.HEADER_HAS_PREVIOUS)).isEqualTo("false");
            assertThat(premiere.getFirst(PaginationHelper.HEADER_HAS_NEXT)).isEqualTo("true");
            assertThat(derniere.getFirst(PaginationHelper.HEADER_HAS_PREVIOUS)).isEqualTo("true");
            assertThat(derniere.getFirst(PaginationHelper.HEADER_HAS_NEXT)).isEqualTo("false");
        }

        /**
         * Sans exposition explicite, un navigateur cache ces en-têtes au code JavaScript : le client
         * recevrait la page sans jamais savoir combien il y en a.
         */
        @Test
        @DisplayName("les en-têtes traversent CORS")
        void entetesExposesAuClient() {
            HttpHeaders entetes = PaginationHelper.createPaginatedResponse(elements(25), 0, 10).getHeaders();

            assertThat(entetes.getAccessControlExposeHeaders())
                .contains(PaginationHelper.HEADER_TOTAL_COUNT, PaginationHelper.HEADER_TOTAL_PAGES, PaginationHelper.HEADER_HAS_NEXT);
        }

        @Test
        @DisplayName("une demande hors bornes est ramenée sur la dernière page servable")
        void demandeHorsBornes() {
            ResponseEntity<List<Integer>> reponse = PaginationHelper.createPaginatedResponse(elements(25), 99, 10);

            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_CURRENT_PAGE)).isEqualTo("2");
            assertThat(reponse.getBody()).hasSize(5);
        }

        @Test
        @DisplayName("une taille excessive est plafonnée, en-tête compris")
        void taillePlafonnee() {
            ResponseEntity<List<Integer>> reponse = PaginationHelper.createPaginatedResponse(elements(500), 0, 10_000);

            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_PAGE_SIZE)).isEqualTo("100");
            assertThat(reponse.getBody()).hasSize(100);
        }

        @Test
        @DisplayName("un ensemble vide rend une réponse vide et cohérente")
        void ensembleVide() {
            ResponseEntity<List<Integer>> reponse = PaginationHelper.createPaginatedResponse(List.<Integer>of(), 0, 10);

            assertThat(reponse.getBody()).isEmpty();
            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_TOTAL_COUNT)).isEqualTo("0");
            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_TOTAL_PAGES)).isEqualTo("0");
            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_HAS_NEXT)).isEqualTo("false");
        }

        /**
         * La seconde forme laisse l'appelant chercher lui-même sa page en base — le décompte total
         * arrive séparément, sans qu'on ait à tout charger pour le connaître.
         */
        @Test
        @DisplayName("la forme paresseuse ne charge que ce qu'on lui donne")
        void formeParesseuse() {
            ResponseEntity<List<Integer>> reponse = PaginationHelper.createPaginatedResponse(
                () -> elements(10),
                () -> 250L,
                1,
                10
            );

            assertThat(reponse.getBody()).hasSize(10);
            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_TOTAL_COUNT)).isEqualTo("250");
            assertThat(reponse.getHeaders().getFirst(PaginationHelper.HEADER_TOTAL_PAGES)).isEqualTo("25");
        }
    }

    // ===== métadonnées =====

    @Nested
    @DisplayName("Métadonnées")
    class Metadonnees {

        @Test
        @DisplayName("les métadonnées décrivent la position dans l'ensemble")
        void positionDansLEnsemble() {
            PaginationMetadata metadonnees = PaginationHelper.createMetadata(25, 1, 10);

            assertThat(metadonnees.totalCount()).isEqualTo(25);
            assertThat(metadonnees.totalPages()).isEqualTo(3);
            assertThat(metadonnees.currentPage()).isEqualTo(1);
            assertThat(metadonnees.pageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("la première et la dernière page se reconnaissent")
        void premiereEtDernierePage() {
            assertThat(PaginationHelper.createMetadata(25, 0, 10).isFirst()).isTrue();
            assertThat(PaginationHelper.createMetadata(25, 0, 10).isLast()).isFalse();
            assertThat(PaginationHelper.createMetadata(25, 2, 10).isFirst()).isFalse();
            assertThat(PaginationHelper.createMetadata(25, 2, 10).isLast()).isTrue();
        }

        @Test
        @DisplayName("un ensemble vide se reconnaît comme tel")
        void ensembleVide() {
            assertThat(PaginationHelper.createMetadata(0, 0, 10).isEmpty()).isTrue();
            assertThat(PaginationHelper.createMetadata(25, 0, 10).isEmpty()).isFalse();
        }
    }

    // ===== décalage en base =====

    @Nested
    @DisplayName("Décalage en base")
    class DecalageEnBase {

        @Test
        @DisplayName("le décalage suit la taille de page validée")
        void decalage() {
            assertThat(PaginationHelper.calculateOffset(0, 20)).isZero();
            assertThat(PaginationHelper.calculateOffset(3, 20)).isEqualTo(60);
        }

        /** Une taille aberrante ne doit pas contaminer le décalage envoyé à la base. */
        @Test
        @DisplayName("une taille aberrante est validée avant le calcul")
        void tailleAberrante() {
            assertThat(PaginationHelper.calculateOffset(2, 0)).isEqualTo(40); // taille par défaut : 20
            assertThat(PaginationHelper.calculateOffset(1, 10_000)).isEqualTo(100); // plafond : 100
        }
    }

    // ===== fabrique =====

    private static List<Integer> elements(int nombre) {
        List<Integer> elements = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            elements.add(i);
        }
        return elements;
    }
}

package com.kobe.warehouse.service.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("EtiquettesPromotion")
class EtiquettesPromotionTest {

    private static final PromotionProduitDTO ACIDUM =
            new PromotionProduitDTO(973, "ACIDUM PHOSPHORICUM", "3020611", 4495, null, 3596);

    private static PromotionDTO promotion(EtatPromotion etat, int quantiteMin, List<PromotionProduitDTO> produits) {
        return promotion(TypePromotion.PROMO_POURCENTAGE, 20, etat, quantiteMin, produits);
    }

    private static PromotionDTO promotion(
            TypePromotion type,
            Integer valeur,
            EtatPromotion etat,
            int quantiteMin,
            List<PromotionProduitDTO> produits) {
        return new PromotionDTO(
                1L,
                "PROMO-00001",
                "Opération test",
                type,
                valeur,
                LocalDate.of(2026, 11, 26),
                LocalDate.of(2026, 12, 31),
                StatutPromotion.VALIDEE,
                etat,
                0,
                quantiteMin,
                null,
                null,
                0,
                produits.size(),
                produits);
    }

    @Test
    @DisplayName("un pack s'étiquette sur son emballage : son libellé, son prix et son code-barres")
    void pack() {
        PromotionDTO base = promotion(TypePromotion.PROMO_PACK, 8_000, EtatPromotion.EN_COURS, 1, List.of(ACIDUM));
        PromotionDTO pack = new PromotionDTO(
                base.id(),
                base.code(),
                "Duo cheveux",
                base.type(),
                base.valeur(),
                base.dateDebut(),
                base.dateFin(),
                base.statut(),
                base.etat(),
                0,
                1,
                null,
                null,
                0,
                1,
                base.produits(),
                "PK00000001");

        List<EtiquetteDTO> etiquettes = EtiquettesPromotion.construire(pack, 2);

        assertThat(etiquettes).hasSize(2);
        assertThat(etiquettes.getFirst().getCode()).isEqualTo("PK00000001");
        assertThat(etiquettes.getFirst().getLibelle()).isEqualTo("Duo cheveux");
        assertThat(etiquettes.getFirst().getPrix()).contains("000").endsWith("CFA");
        assertThatThrownBy(() ->
                        EtiquettesPromotion.construirePack(promotion(EtatPromotion.EN_COURS, 1, List.of(ACIDUM)), 1))
                .isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    @DisplayName("une offre N+M porte le prix normal et l'offre en toutes lettres, sans prix barré")
    void offreNM() {
        List<EtiquetteDTO> etiquettes = EtiquettesPromotion.construire(
                promotion(TypePromotion.PROMO_OFFRE_NM, 1, EtatPromotion.EN_COURS, 2, List.of(ACIDUM)), 1);

        assertThat(etiquettes).hasSize(1);
        EtiquetteDTO etiquette = etiquettes.getFirst();
        assertThat(etiquette.getPrixAvant()).isNull();
        assertThat(etiquette.getPrix()).contains("495").endsWith("CFA");
        assertThat(etiquette.getMention()).isEqualTo("2 achetés + 1 offert · Jusqu'au 31/12/2026");
        assertThat(EtiquettesPromotion.offre(TypePromotion.PROMO_OFFRE_NM, 1, 2))
                .isEqualTo("1 acheté + 2 offerts");
        assertThat(EtiquettesPromotion.offre(TypePromotion.PROMO_POURCENTAGE, 2, 1))
                .isNull();
    }

    @Test
    @DisplayName("une remise de panier se dit en toutes lettres : son prix unitaire dépend du reste du panier")
    void seuilMontant() {
        String offre = EtiquettesPromotion.offre(TypePromotion.PROMO_SEUIL_MONTANT, 1, 10, 20_000);

        assertThat(offre).startsWith("-10 % dès 20").endsWith("000 CFA d'achat");
        assertThat(EtiquettesPromotion.offre(TypePromotion.PROMO_SEUIL_MONTANT, 1, 10, null))
                .isNull();
    }

    @Test
    @DisplayName("une etiquette par article, repetee qty fois, avec prix barre, prix remise et date de fin")
    void construit() {
        List<EtiquetteDTO> etiquettes =
                EtiquettesPromotion.construire(promotion(EtatPromotion.EN_COURS, 1, List.of(ACIDUM)), 3);

        assertThat(etiquettes).hasSize(3);
        EtiquetteDTO etiquette = etiquettes.getFirst();
        assertThat(etiquette.getLibelle()).isEqualTo("ACIDUM PHOSPHORICUM");
        assertThat(etiquette.getCode()).isEqualTo("3020611");
        assertThat(etiquette.getPrixAvant()).contains("495").doesNotContain("CFA");
        assertThat(etiquette.getPrix()).contains("596").endsWith("CFA");
        assertThat(etiquette.getMention()).isEqualTo("Jusqu'au 31/12/2026");
    }

    @Test
    @DisplayName(
            "le seuil de quantite figure sur l'etiquette : sans lui, elle annoncerait un prix que la caisse ne fait pas")
    void seuil() {
        List<EtiquetteDTO> etiquettes =
                EtiquettesPromotion.construire(promotion(EtatPromotion.A_VENIR, 3, List.of(ACIDUM)), 1);

        assertThat(etiquettes.getFirst().getMention()).isEqualTo("Jusqu'au 31/12/2026 · dès 3 achetés");
    }

    @Test
    @DisplayName("un article que la promotion ne remise pas n'a pas d'etiquette promo")
    void articleSansRemise() {
        PromotionProduitDTO prixFixeAuDessus = new PromotionProduitDTO(1, "SAVON", "111", 1000, 1200, 1000);

        List<EtiquetteDTO> etiquettes = EtiquettesPromotion.construire(
                promotion(EtatPromotion.EN_COURS, 1, List.of(ACIDUM, prixFixeAuDessus)), 1);

        assertThat(etiquettes).extracting(EtiquetteDTO::getLibelle).containsExactly("ACIDUM PHOSPHORICUM");
    }

    @ParameterizedTest
    @EnumSource(
            value = EtatPromotion.class,
            names = {"BROUILLON", "EPUISEE", "SUSPENDUE", "TERMINEE", "ANNULEE"})
    @DisplayName("une promotion qui ne s'applique pas au comptoir ne s'etiquette pas")
    void refuse(EtatPromotion etat) {
        assertThatThrownBy(() -> EtiquettesPromotion.construire(promotion(etat, 1, List.of(ACIDUM)), 1))
                .isInstanceOf(BadRequestAlertException.class);
    }
}

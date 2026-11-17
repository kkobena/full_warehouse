package com.kobe.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Commande — retard de livraison et delai annonce")
class CommandeRetardTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);

    private static Commande commande(OrderStatut statut, LocalDateTime dateEnvoi) {
        Commande commande = new Commande();
        commande.setOrderStatus(statut);
        commande.setDateEnvoi(dateEnvoi);
        return commande;
    }

    @Test
    @DisplayName("compte les jours au-dela du delai annonce")
    void compterLesJoursDeRetard() {
        Commande commande = commande(OrderStatut.REQUESTED, LocalDateTime.of(2026, 11, 1, 8, 0));

        // Partie le 1er pour un delai de 5 jours : attendue le 6, nous sommes le 13.
        assertThat(commande.joursDeRetard(AUJOURDHUI, 5)).isEqualTo(7L);
    }

    @Test
    @DisplayName("ne signale rien tant qu'on est dans les temps")
    void dansLesTemps() {
        Commande commande = commande(OrderStatut.REQUESTED, LocalDateTime.of(2026, 11, 12, 8, 0));

        assertThat(commande.joursDeRetard(AUJOURDHUI, 5)).isZero();
    }

    @Test
    @DisplayName("le jour meme de l'echeance n'est pas un retard")
    void jourDeLEcheance() {
        Commande commande = commande(OrderStatut.REQUESTED, LocalDateTime.of(2026, 11, 8, 8, 0));

        assertThat(commande.joursDeRetard(AUJOURDHUI, 5)).isZero();
    }

    @Test
    @DisplayName("un brouillon n'attend rien, si vieux soit-il")
    void brouillonJamaisEnRetard() {
        Commande commande = commande(OrderStatut.REQUESTED, null);

        assertThat(commande.estEnvoyee()).isFalse();
        assertThat(commande.joursDeRetard(AUJOURDHUI, 1)).isZero();
    }

    @Test
    @DisplayName("une commande recue sort des alertes du jour")
    void commandeRecue() {
        // Le retard appartient alors a l'historique, pas aux relances a passer.
        Commande commande = commande(OrderStatut.RECEIVED, LocalDateTime.of(2026, 1, 1, 8, 0));

        assertThat(commande.joursDeRetard(AUJOURDHUI, 5)).isZero();
    }

    @Test
    @DisplayName("une agence sans delai herite de celui de son grossiste")
    void delaiHeriteDuGrossiste() {
        Fournisseur grossiste = new Fournisseur();
        grossiste.setDelaiLivraisonJours(3);
        Fournisseur agence = new Fournisseur();
        agence.setParent(grossiste);

        assertThat(agence.delaiLivraisonEffectif()).isEqualTo(3);
    }

    @Test
    @DisplayName("sans delai nulle part, on retombe sur une semaine")
    void delaiParDefaut() {
        assertThat(new Fournisseur().delaiLivraisonEffectif()).isEqualTo(Fournisseur.DELAI_LIVRAISON_PAR_DEFAUT);
    }

    @Test
    @DisplayName("le delai propre a l'agence prime sur celui du grossiste")
    void delaiPropreAgence() {
        Fournisseur grossiste = new Fournisseur();
        grossiste.setDelaiLivraisonJours(7);
        Fournisseur agence = new Fournisseur();
        agence.setParent(grossiste);
        agence.setDelaiLivraisonJours(2);

        assertThat(agence.delaiLivraisonEffectif()).isEqualTo(2);
    }
}

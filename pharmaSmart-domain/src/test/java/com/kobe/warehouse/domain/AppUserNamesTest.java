package com.kobe.warehouse.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ces libellés sont calculés à l'affichage, jamais à la saisie : une exception ici fait échouer
 * une liste entière — plannings, mouvements de stock, tickets — pour un seul utilisateur mal
 * renseigné. Or ni le nom ni le prénom ne sont obligatoires en base.
 */
@DisplayName("AppUserNames — libellés d'utilisateur")
class AppUserNamesTest {

    @Test
    @DisplayName("Le nom est abrégé à son initiale, suivi du prénom")
    void abreviationNominale() {
        assertEquals("K.Awa", AppUserNames.shortName(utilisateur("Kone", "Awa")));
        assertEquals("K.Awa", AppUserNames.fullName(utilisateur("Kone", "Awa")));
    }

    @Test
    @DisplayName("Un utilisateur sans nom de famille se réduit à son prénom")
    void sansNomDeFamille() {
        assertEquals("Awa", AppUserNames.shortName(utilisateur(null, "Awa")));
        assertEquals("Awa", AppUserNames.shortName(utilisateur("   ", "Awa")));
    }

    @Test
    @DisplayName("Un utilisateur sans aucun nom donne un libellé vide, pas une erreur")
    void sansAucunNom() {
        assertEquals("", AppUserNames.shortName(utilisateur(null, null)));
    }

    @Test
    @DisplayName("Un utilisateur absent n'a pas de libellé")
    void utilisateurAbsent() {
        assertNull(AppUserNames.shortName((AppUser) null));
        assertNull(AppUserNames.fullName((AppUser) null));
        assertNull(AppUserNames.LastNameFirstName(null));
    }

    @Test
    @DisplayName("Le libellé long garde le nom entier, puis le prénom")
    void libelleLong() {
        assertEquals("Kone Awa", AppUserNames.LastNameFirstName(utilisateur("Kone", "Awa")));
        assertEquals("Kone ", AppUserNames.LastNameFirstName(utilisateur("Kone", null)));
    }

    private AppUser utilisateur(String nom, String prenom) {
        AppUser user = new AppUser();
        user.setLastName(nom);
        user.setFirstName(prenom);
        return user;
    }
}

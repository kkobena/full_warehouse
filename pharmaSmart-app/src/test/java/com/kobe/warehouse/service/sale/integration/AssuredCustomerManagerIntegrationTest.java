package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.AssuredCustomerManager} sur un vrai PostgreSQL.
 *
 * <p>Le service est mince, et ses règles — validation du téléphone, comparaison d'identifiants —
 * relèvent du test unitaire. Ce qui reste à voir en base est ailleurs : la mise à jour touche une
 * table dont les colonnes sont bornées en longueur et non nulles, et un rejet de téléphone doit
 * laisser la fiche intacte plutôt qu'à demi modifiée.
 */
@DisplayName("AssuredCustomerManager — fiche assuré sur PostgreSQL")
class AssuredCustomerManagerIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La mise à jour du nom et du téléphone est écrite en base")
    void miseAJourDeLaFiche() {
        AssuredCustomer assure = assure("KONATE", "Sali", "ASS-MGR-1");
        viderLeCache();

        AssuredCustomerDTO modifications = new AssuredCustomerDTO();
        modifications.setFirstName("Salimata");
        modifications.setLastName("KONATE-DIARRA");
        modifications.setPhone("0709090909");
        modifications.setNumAyantDroit("AD-42");

        services.assuredCustomerManager.updateAssuredCustomer(em.find(AssuredCustomer.class, assure.getId()), modifications);
        viderLeCache();

        AssuredCustomer relu = em.find(AssuredCustomer.class, assure.getId());
        assertEquals("Salimata", relu.getFirstName());
        assertEquals("KONATE-DIARRA", relu.getLastName());
        assertEquals("0709090909", relu.getPhone());
        assertEquals("AD-42", relu.getNumAyantDroit());
    }

    @Test
    @DisplayName("Un téléphone invalide interrompt la mise à jour avant l'écriture")
    void telephoneInvalide() {
        AssuredCustomer assure = assure("COULIBALY", "Adama", "ASS-MGR-2");
        viderLeCache();

        AssuredCustomerDTO modifications = new AssuredCustomerDTO();
        modifications.setFirstName("Adamou");
        modifications.setLastName("COULIBALY");
        modifications.setPhone("pas-un-numero");

        AssuredCustomer managed = em.find(AssuredCustomer.class, assure.getId());
        assertThrows(InvalidPhoneNumberException.class, () -> services.assuredCustomerManager.updateAssuredCustomer(managed, modifications));

        em.clear();
        assertEquals("Adama", em.find(AssuredCustomer.class, assure.getId()).getFirstName(), "rien n'est parti en base");
    }

    @Test
    @DisplayName("Un DTO nul ne touche à rien")
    void miseAJourSansDonnees() {
        AssuredCustomer assure = assure("TRAORE", "Awa", "ASS-MGR-3");
        viderLeCache();

        AssuredCustomer managed = em.find(AssuredCustomer.class, assure.getId());
        services.assuredCustomerManager.updateAssuredCustomer(managed, null);
        viderLeCache();

        assertEquals("Awa", em.find(AssuredCustomer.class, assure.getId()).getFirstName());
    }

    @Test
    @DisplayName("L'ayant droit est rendu par référence, sans aller le chercher en base")
    void ayantDroitParReference() {
        AssuredCustomer assure = assure("SANGARE", "Moussa", "ASS-MGR-4");
        viderLeCache();

        var trouve = services.assuredCustomerManager.getAyantDroitFromId(assure.getId());

        assertTrue(trouve.isPresent());
        assertEquals(assure.getId(), trouve.orElseThrow().getId());
        assertTrue(services.assuredCustomerManager.getAyantDroitFromId(null).isEmpty());
    }

    @Test
    @DisplayName("Deux fiches se comparent sur leur identifiant")
    void comparaisonDeFiches() {
        AssuredCustomer assure = assure("DIALLO", "Ibrahim", "ASS-MGR-5");
        viderLeCache();

        AssuredCustomerDTO memeFiche = new AssuredCustomerDTO();
        memeFiche.setId(assure.getId());
        AssuredCustomerDTO autreFiche = new AssuredCustomerDTO();
        autreFiche.setId(assure.getId() + 1);

        AssuredCustomer managed = em.find(AssuredCustomer.class, assure.getId());
        assertTrue(services.assuredCustomerManager.isSameCustomer(managed, memeFiche));
        assertFalse(services.assuredCustomerManager.isSameCustomer(managed, autreFiche));
    }
}

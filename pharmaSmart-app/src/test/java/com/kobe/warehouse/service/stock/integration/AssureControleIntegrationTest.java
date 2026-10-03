package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.customer.HistoriqueClientService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ControleAssureDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.impl.AssuredCustomerServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Saisie d'un assuré : numéro de carte déjà utilisé (message nommant le dossier en cause, contrôle
 * anticipé) et homonymes (avertissement), sur PostgreSQL.
 */
@DisplayName("Assuré : numéro de carte déjà utilisé et homonymes")
class AssureControleIntegrationTest extends AbstractStockIntegrationTest {

    private AssuredCustomerServiceImpl service;
    private TiersPayant organisme;

    @BeforeEach
    void preparer() {
        service = new AssuredCustomerServiceImpl(
            IntegrationPostgresDatabase.bean(AssuredCustomerRepository.class),
            IntegrationPostgresDatabase.bean(ClientTiersPayantRepository.class),
            IntegrationPostgresDatabase.bean(ThirdPartySaleLineRepository.class),
            mock(CustomerDataService.class),
            mock(HistoriqueClientService.class)
        );
        organisme = new TiersPayant();
        organisme.setName(unique("ORG"));
        organisme.setFullName("ORGANISME " + organisme.getName());
        organisme.setCategorie(TiersPayantCategorie.ASSURANCE);
        organisme.setNbreBordereaux(1);
        organisme.setCreated(java.time.LocalDateTime.now());
        organisme.setUser(utilisateur);
        em.persist(organisme);
        em.flush();
    }

    @Test
    @DisplayName("Un numéro de carte déjà porté par un autre dossier est refusé, avec le nom du dossier")
    void numeroDejaUtilise() {
        service.createFromDto(assure("KOUASSI", "Jean", "CARTE1"));
        viderLeCache();

        GenericError erreur = assertThrows(GenericError.class, () -> service.createFromDto(assure("TRAORE", "Awa", "CARTE1")));

        assertTrue(erreur.getMessage().contains("CARTE1"));
        assertTrue(erreur.getMessage().contains("KOUASSI Jean"), "le message nomme le dossier en cause");
    }

    @Test
    @DisplayName("Le contrôle anticipé signale le numéro utilisé, et son titulaire, avant tout enregistrement")
    void controleAnticipe() {
        service.createFromDto(assure("KOUASSI", "Jean", "CARTE2"));
        viderLeCache();

        ControleAssureDTO controle = service.controlerAssure(organisme.getId(), "CARTE2", null, null, null);
        ControleAssureDTO libre = service.controlerAssure(organisme.getId(), "AUTRE", null, null, null);

        assertTrue(controle.numeroDejaUtilise());
        assertEquals("KOUASSI Jean", controle.titulaireDuNumero());
        assertFalse(libre.numeroDejaUtilise());
        assertNull(libre.titulaireDuNumero());
    }

    @Test
    @DisplayName("Un dossier peut garder son propre numéro : il n'est pas en conflit avec lui-même")
    void memeDossier() {
        AssuredCustomer cree = service.createFromDto(assure("KOUASSI", "Jean", "CARTE3"));
        viderLeCache();

        assertFalse(service.controlerAssure(organisme.getId(), "CARTE3", null, null, cree.getId()).numeroDejaUtilise());
    }

    @Test
    @DisplayName("Les homonymes sont signalés sans bloquer la création")
    void homonymes() {
        service.createFromDto(assure("KOUASSI", "Jean", "CARTE4"));
        viderLeCache();

        assertEquals(List.of("KOUASSI Jean"), service.controlerAssure(null, null, "kouassi", "JEAN", null).homonymes());
        service.createFromDto(assure("KOUASSI", "Jean", "CARTE5"));
    }

    @Test
    @DisplayName("Mêmes nom, prénom ET matricule : bloqué, même auprès d'un autre organisme")
    void dossierIdentique() {
        service.createFromDto(assure("KOUASSI", "Jean", "MAT1"));
        TiersPayant autre = autreOrganisme();
        viderLeCache();

        AssuredCustomerDTO doublon = assure("kouassi", "JEAN", "MAT1").setTiersPayantId(autre.getId());
        GenericError erreur = assertThrows(GenericError.class, () -> service.createFromDto(doublon));

        assertTrue(erreur.getMessage().contains("nom, prénom et numéro de matricule"));
        assertTrue(erreur.getMessage().contains("KOUASSI Jean"));
        ControleAssureDTO controle = service.controlerAssure(autre.getId(), "MAT1", "Kouassi", "Jean", null);
        assertTrue(controle.dossierExistant());
        assertEquals("KOUASSI Jean", controle.titulaireDuDossier());
    }

    @Test
    @DisplayName("Un autre matricule, ou un autre nom, n'est pas bloqué")
    void dossierDifferent() {
        service.createFromDto(assure("KOUASSI", "Jean", "MAT2"));
        viderLeCache();

        service.createFromDto(assure("KOUASSI", "Jean", "MAT3"));
        service.createFromDto(assure("TRAORE", "Awa", "MAT4"));
        assertFalse(service.controlerAssure(organisme.getId(), "MAT9", "Kouassi", "Jean", null).dossierExistant());
    }

    @Test
    @DisplayName("Modifier un dossier sans changer son identité n'est pas bloqué par lui-même")
    void modificationSansChangerLIdentite() {
        AssuredCustomer cree = service.createFromDto(assure("KOUASSI", "Jean", "MAT5"));
        viderLeCache();

        assertFalse(service.controlerAssure(organisme.getId(), "MAT5", "KOUASSI", "Jean", cree.getId()).dossierExistant());
    }

    @Test
    @DisplayName("Un ayant droit de mêmes nom, prénom et numéro assuré est bloqué")
    void ayantDroitIdentique() {
        AssuredCustomerDTO premier = assure("KOUASSI", "Jean", "MAT6");
        premier.setAyantDroits(List.of(ayantDroit("KOUASSI", "Awa", "AD1")));
        service.createFromDto(premier);
        viderLeCache();

        AssuredCustomerDTO second = assure("DIABATE", "Moussa", "MAT7");
        second.setAyantDroits(List.of(ayantDroit("kouassi", "awa", "AD1")));
        GenericError erreur = assertThrows(GenericError.class, () -> service.createFromDto(second));

        assertTrue(erreur.getMessage().contains("ayant droit"));
    }

    private TiersPayant autreOrganisme() {
        TiersPayant autre = new TiersPayant();
        autre.setName(unique("AUTRE"));
        autre.setFullName("ORGANISME " + autre.getName());
        autre.setCategorie(TiersPayantCategorie.ASSURANCE);
        autre.setNbreBordereaux(1);
        autre.setCreated(java.time.LocalDateTime.now());
        autre.setUser(utilisateur);
        em.persist(autre);
        em.flush();
        return autre;
    }

    private AssuredCustomerDTO ayantDroit(String nom, String prenom, String numeroAssure) {
        AssuredCustomerDTO dto = new AssuredCustomerDTO();
        dto.setFirstName(nom);
        dto.setLastName(prenom);
        dto.setNumAyantDroit(numeroAssure);
        return dto;
    }

    private AssuredCustomerDTO assure(String nom, String prenom, String numeroCarte) {
        AssuredCustomerDTO dto = new AssuredCustomerDTO();
        dto.setFirstName(nom);
        dto.setLastName(prenom);
        dto.setTiersPayantId(organisme.getId());
        dto.setNum(numeroCarte);
        dto.setTaux(80);
        dto.setAyantDroits(List.of());
        dto.setTiersPayants(List.of());
        return dto;
    }
}

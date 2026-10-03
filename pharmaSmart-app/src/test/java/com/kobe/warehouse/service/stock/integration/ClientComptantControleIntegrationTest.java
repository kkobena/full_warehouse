package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.customer.HistoriqueClientService;
import com.kobe.warehouse.service.dto.ControleClientDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.errors.CustomerAlreadyExistException;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Création d'un client comptant : doublon exact, voisins, téléphone facultatif ou étranger, email. */
@DisplayName("Client comptant : contrôle anticipé et validations")
class ClientComptantControleIntegrationTest extends AbstractStockIntegrationTest {

    private UninsuredCustomerService service;

    @BeforeEach
    void creerLeService() {
        service = new UninsuredCustomerService(
            IntegrationPostgresDatabase.bean(UninsuredCustomerRepository.class),
            mock(HistoriqueClientService.class)
        );
    }

    @Test
    @DisplayName("Le client identique (nom, prénom, téléphone) est signalé avant l'enregistrement, avec de quoi le sélectionner")
    void clientExistant() {
        UninsuredCustomerDTO cree = service.create(client("KOUASSI", "Jean", "0701020304"));
        viderLeCache();

        ControleClientDTO controle = service.controler("0701020304", "kouassi", "JEAN", null);

        assertNotNull(controle.clientExistant());
        assertEquals(cree.getId(), controle.clientExistant().getId());
        assertNull(service.controler("0701020304", "kouassi", "JEAN", cree.getId()).clientExistant(), "un dossier ne se bloque pas lui-même");
    }

    @Test
    @DisplayName("Le message de doublon nomme le client en cause")
    void messagePrecis() {
        service.create(client("KOUASSI", "Jean", "0701020305"));
        viderLeCache();

        CustomerAlreadyExistException erreur = assertThrows(
            CustomerAlreadyExistException.class,
            () -> service.create(client("KOUASSI", "Jean", "0701020305"))
        );

        assertTrue(erreur.getMessage().contains("KOUASSI Jean"));
    }

    @Test
    @DisplayName("Un même téléphone avec un autre nom, ou un même nom avec un autre téléphone, est un voisin à signaler")
    void voisins() {
        service.create(client("KOUASSI", "Jean", "0701020306"));
        viderLeCache();

        List<ControleClientDTO.ClientProcheDTO> memeTelephone = service.controler("0701020306", "TRAORE", "Awa", null).proches();
        List<ControleClientDTO.ClientProcheDTO> memeNom = service.controler("0799999999", "Kouassi", "Jean", null).proches();

        assertEquals("même téléphone", memeTelephone.getFirst().motif());
        assertEquals("même nom", memeNom.getFirst().motif());
    }

    @Test
    @DisplayName("Une faute de frappe sur le nom ne passe plus inaperçue")
    void nomProche() {
        service.create(client("KOUASSI", "Konan", "0701020307"));
        viderLeCache();

        List<ControleClientDTO.ClientProcheDTO> proches = service.controler(null, "KOUASI", "Konan", null).proches();

        assertFalse(proches.isEmpty(), "« KOUASI Konan » ressemble à « KOUASSI Konan »");
        assertEquals("nom proche", proches.getFirst().motif());
    }

    @Test
    @DisplayName("Le téléphone est facultatif : deux clients de même nom sans téléphone peuvent coexister")
    void telephoneFacultatif() {
        service.create(client("DIABATE", "Moussa", null));
        service.create(client("DIABATE", "Moussa", null));
    }

    @Test
    @DisplayName("Un numéro étranger saisi avec son indicatif est accepté ; un numéro invalide est refusé avec un message qui l'explique")
    void telephoneEtranger() {
        service.create(client("TRAORE", "Awa", "+22370123456"));

        InvalidPhoneNumberException erreur = assertThrows(InvalidPhoneNumberException.class, () -> service.create(client("TRAORE", "Bintou", "1234")));

        assertTrue(erreur.getMessage().contains("indicatif"));
    }

    @Test
    @DisplayName("Un email au format invalide est refusé par le DTO ; vide, il reste permis")
    void email() {
        try (var fabrique = Validation.buildDefaultValidatorFactory()) {
            var validateur = fabrique.getValidator();
            assertFalse(validateur.validate(client("KOUASSI", "Jean", "0701020308").setEmailEtRetourne("azer")).isEmpty());
            assertTrue(validateur.validate(client("KOUASSI", "Jean", "0701020308").setEmailEtRetourne("jean@exemple.ci")).isEmpty());
            assertTrue(validateur.validate(client("KOUASSI", "Jean", "0701020308").setEmailEtRetourne("")).isEmpty());
            assertTrue(validateur.validate(client("KOUASSI", "Jean", "0701020308").setEmailEtRetourne(null)).isEmpty());
        }
    }

    @Test
    @DisplayName("La date de naissance et le sexe d'un client comptant sont enregistrés, relus et modifiables")
    void naissanceEtSexe() {
        UninsuredCustomerDTO dto = client("KOUASSI", "Jean", "0701020309");
        dto.setDatNaiss(java.time.LocalDate.of(1990, 5, 17));
        dto.setSexe("M");

        UninsuredCustomerDTO cree = service.create(dto);
        viderLeCache();
        var relu = em.find(com.kobe.warehouse.domain.UninsuredCustomer.class, cree.getId());

        assertEquals(java.time.LocalDate.of(1990, 5, 17), relu.getDatNaiss());
        assertEquals("M", relu.getSexe());
        assertEquals(java.time.LocalDate.of(1990, 5, 17), cree.getDatNaiss());

        UninsuredCustomerDTO modif = client("KOUASSI", "Jean", "0701020309");
        modif.setId(cree.getId());
        modif.setDatNaiss(java.time.LocalDate.of(1991, 1, 2));
        modif.setSexe("");
        service.update(modif);
        viderLeCache();

        var apres = em.find(com.kobe.warehouse.domain.UninsuredCustomer.class, cree.getId());
        assertEquals(java.time.LocalDate.of(1991, 1, 2), apres.getDatNaiss());
        assertNull(apres.getSexe(), "un sexe vide est enregistré comme absent");
    }

    private static Client client(String nom, String prenom, String telephone) {
        Client dto = new Client();
        dto.setFirstName(nom);
        dto.setLastName(prenom);
        dto.setPhone(telephone);
        return dto;
    }

    /** DTO d'essai : ajoute un setter chaînable pour l'email, sans toucher au DTO de production. */
    private static class Client extends UninsuredCustomerDTO {

        Client setEmailEtRetourne(String email) {
            setEmail(email);
            return this;
        }
    }
}

package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.impl.TiersPayantServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Création de tiers payant : contrôle de doublon (nom, code organisme, identifiant contribuable) et
 * taux de couverture par défaut, sur PostgreSQL — migration V2.1.21 comprise.
 */
@DisplayName("Tiers payant : doublons et taux de couverture par défaut")
class TiersPayantDoublonIntegrationTest extends AbstractStockIntegrationTest {

    private TiersPayantServiceImpl service;

    @BeforeEach
    void creerLeService() {
        service = new TiersPayantServiceImpl(
            IntegrationPostgresDatabase.bean(TiersPayantRepository.class),
            services.storageService,
            IntegrationPostgresDatabase.bean(ClientTiersPayantRepository.class),
            IntegrationPostgresDatabase.bean(ThirdPartySaleLineRepository.class)
        );
    }

    @Test
    @DisplayName("Le taux de couverture par défaut est enregistré et relu ; il est facultatif")
    void tauxDeCouvertureParDefaut() {
        TiersPayantDto avec = service.createFromDto(organisme("CNPSX").setTauxCouvertureDefaut(80));
        TiersPayantDto sans = service.createFromDto(organisme("DEPOTX"));
        viderLeCache();

        assertEquals(80, em.find(com.kobe.warehouse.domain.TiersPayant.class, avec.getId()).getTauxCouvertureDefaut());
        assertNull(em.find(com.kobe.warehouse.domain.TiersPayant.class, sans.getId()).getTauxCouvertureDefaut(), "jamais 100 % supposé en silence");
    }

    @Test
    @DisplayName("La base refuse un taux hors de 0 à 100")
    void tauxHorsBornes() {
        TiersPayantDto cree = service.createFromDto(organisme("BORNES"));
        em.flush();

        assertThrows(
            PersistenceException.class,
            () -> {
                em.createNativeQuery("UPDATE tiers_payant SET taux_couverture_defaut = 150 WHERE id = :id").setParameter("id", cree.getId()).executeUpdate();
                em.flush();
            }
        );
    }

    @Test
    @DisplayName("Un identifiant contribuable déjà connu est refusé, quelle que soit la casse et le nom")
    void doublonDeNcc() {
        service.createFromDto(organisme("PREMIER").setNcc("abc1234"));

        GenericError erreur = assertThrows(GenericError.class, () -> service.createFromDto(organisme("TOUT AUTRE NOM").setNcc("ABC1234")));

        assertTrue(erreur.getMessage().contains("identifiant contribuable"));
    }

    @Test
    @DisplayName("Modifier un organisme en gardant son propre NCC n'est pas un doublon ; lui donner celui d'un autre l'est")
    void modificationEtNcc() {
        TiersPayantDto premier = service.createFromDto(organisme("UN").setNcc("NCC1"));
        TiersPayantDto second = service.createFromDto(organisme("DEUX").setNcc("NCC2"));

        service.updateFromDto(premier.setTelephone("0700000000"));
        assertThrows(GenericError.class, () -> service.updateFromDto(second.setNcc("ncc1")));
    }

    @Test
    @DisplayName("Un NCC absent ne déclenche aucun contrôle : plusieurs organismes peuvent ne pas en avoir")
    void nccAbsent() {
        service.createFromDto(organisme("SANS NCC UN"));
        service.createFromDto(organisme("SANS NCC DEUX").setNcc(""));
    }

    @Test
    @DisplayName("Le doublon sur le nom reste refusé, avec un message correctement orthographié")
    void doublonDeNom() {
        TiersPayantDto existant = service.createFromDto(organisme("MUTUELLE"));

        GenericError erreur = assertThrows(
            GenericError.class,
            () -> service.createFromDto(organisme("AUTRE").setName(existant.getName()))
        );

        assertFalse(erreur.getMessage().contains("orgasisme"));
        assertTrue(erreur.getMessage().contains("même nom"));
    }

    private TiersPayantDto organisme(String nom) {
        String unique = unique(nom);
        return new TiersPayantDto()
            .setName(unique)
            .setFullName("ORGANISME " + unique)
            .setTelephone("0102030405")
            .setCategorie(TiersPayantCategorie.ASSURANCE)
            .setNbreBordereaux(1)
            .setDelaiReglement(30);
    }
}

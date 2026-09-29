package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import com.kobe.warehouse.repository.CustomizedProductRepository;
import com.kobe.warehouse.repository.DciRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.dci.dto.ModeRattachementDci;
import com.kobe.warehouse.service.dci.service.DciServiceImpl;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relation n-n produit ↔ DCI sur PostgreSQL — PLAN-PRODUIT-DCI-N-N. */
@DisplayName("Produit ↔ DCI (n-n) sur PostgreSQL")
class DciProduitIntegrationTest extends AbstractStockIntegrationTest {

    private DciServiceImpl dciService;
    private CustomizedProductRepository catalogue;

    @BeforeEach
    void services() {
        dciService = new DciServiceImpl(IntegrationPostgresDatabase.bean(DciRepository.class), services.produitRepository);
        catalogue = new CustomizedProductRepository(
            services.stockProduitRepository,
            services.produitRepository,
            services.storageService,
            mock(EtatProduitService.class),
            IntegrationPostgresDatabase.bean(SalesLineRepository.class),
            IntegrationPostgresDatabase.bean(OrderLineRepository.class),
            em,
            IntegrationPostgresDatabase.bean(MagasinRepository.class)
        );
    }

    @Test
    @DisplayName("La migration marque les DCI écrites « A/B » ou « A+B » comme composées")
    void dciComposeesMarquees() {
        assertEquals(0, compter("SELECT COUNT(*) FROM dci WHERE libelle ~ '[/+]' AND statut <> 'COMPOSEE'"));
        assertEquals(0, compter("SELECT COUNT(*) FROM dci WHERE libelle !~ '[/+]' AND statut <> 'ACTIVE'"));
    }

    @Test
    @DisplayName("Une molécule d'association compte parmi les produits qui la portent, et ne se supprime pas")
    void comptageEtSuppression() {
        Dci codeine = dci("CODEINE");
        Produit association = produit(unique("ASSOC CODEINE"));
        association.remplacerDcis(List.of(dci("PARACETAMOL"), codeine));
        em.flush();

        assertEquals(1, services.produitRepository.countByDciId(codeine.getId()), "portée en second rang, elle compte quand même");
        assertEquals(List.of(association.getId()), services.produitRepository.findAllByDciIdForDetail(codeine.getId()).stream().map(Produit::getId).toList());
        assertThrows(GenericError.class, () -> dciService.delete(codeine.getId()));
    }

    @Test
    @DisplayName("Affecter en masse ajoute la molécule par défaut, et la remplace sur demande")
    void affectationEnMasse() {
        Dci existante = dci("EXISTANTE");
        Dci ajoutee = dci("AJOUTEE");
        Produit produit = produit(unique("MASSE"));
        produit.remplacerDcis(List.of(existante));
        em.flush();

        dciService.rattacherProduits(ajoutee.getId(), List.of(produit.getId()), ModeRattachementDci.AJOUTER);
        em.flush();
        assertEquals(List.of(existante.getId(), ajoutee.getId()), molecules(produit));

        dciService.rattacherProduits(ajoutee.getId(), List.of(produit.getId()), ModeRattachementDci.REMPLACER);
        em.flush();
        assertEquals(List.of(ajoutee.getId()), molecules(produit));
        assertEquals(ajoutee.getId().longValue(), compter("SELECT dci_id FROM produit WHERE id = " + produit.getId()));
    }

    @Test
    @DisplayName("Le catalogue trouve une association par l'une de ses molécules, sans la dupliquer")
    void rechercheCatalogue() {
        Dci premiere = dci("RECHERCHE UN");
        Dci seconde = dci("RECHERCHE DEUX");
        Produit association = produit(unique("CATALOGUE ASSOC"));
        association.remplacerDcis(List.of(premiere, seconde));
        em.flush();
        viderLeCache();

        List<Integer> parFiltre = catalogue.lite(new ProduitCriteria().setDciId(seconde.getId())).stream().map(ProduitDTO::getId).toList();
        List<Integer> parTexte = catalogue.lite(new ProduitCriteria().setSearch(premiere.getLibelle())).stream().map(ProduitDTO::getId).toList();

        assertEquals(List.of(association.getId()), parFiltre);
        assertEquals(List.of(association.getId()), parTexte, "une seule ligne malgré les deux molécules jointes");
    }

    @Test
    @DisplayName("Une DCI créée est active")
    void nouvelleDciActive() {
        assertEquals(StatutDci.ACTIVE, dci("NOUVELLE").getStatut());
    }

    private Dci dci(String libelle) {
        Dci dci = new Dci();
        dci.setLibelle(unique(libelle));
        dci.setCode(unique("D"));
        em.persist(dci);
        em.flush();
        return dci;
    }

    private List<Integer> molecules(Produit produit) {
        return em.createNativeQuery("SELECT dci_id FROM produit_dci WHERE produit_id = :id ORDER BY rang", Integer.class)
            .setParameter("id", produit.getId())
            .getResultList();
    }
}

package com.kobe.warehouse.service.stock.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import com.kobe.warehouse.repository.ProduitFavoriRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.FavoriSuggere;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import com.kobe.warehouse.service.stock.dto.SubstitutPropose;
import com.kobe.warehouse.service.stock.impl.ProduitFavoriServiceImpl;
import com.kobe.warehouse.service.stock.impl.SubstitutionComptoirServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Grille des produits favoris et substituts disponibles au comptoir
 * (docs/PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3 et §4).
 *
 * <p>Le plus fragile est le SQL : {@code search_produits_by_ids_json} reprend la recherche pour des identifiants, et la
 * table {@code substitut} s'écrit dans un sens mais se lit dans les deux. Les deux services tournent donc sur les vrais
 * repositories ; seul {@link ProduitService} est reconstitué, par l'appel natif lui-même, faute de contexte Spring complet.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Favoris et substituts du comptoir")
class SubstitutionEtFavorisIntegrationTest extends AbstractDashboardIntegrationTest {

    private ProduitFavoriServiceImpl favoris;
    private SubstitutionComptoirServiceImpl substitutions;
    private SubstitutRepository substitutRepository;

    @BeforeEach
    void preparer() {
        ProduitRepository produitRepository = IntegrationPostgresDatabase.bean(ProduitRepository.class);
        substitutRepository = IntegrationPostgresDatabase.bean(SubstitutRepository.class);
        ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ProduitService produitService = mock(ProduitService.class);
        when(produitService.findSearchByIds(any(), any())).thenAnswer(i -> {
            List<Integer> ids = i.getArgument(0);
            String tableau = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "{", "}"));
            String resultat = produitRepository.searchProduitsByIdsJson(tableau, MAGASIN_ID);
            return resultat == null ? List.<ProduitSearch>of() : json.readValue(resultat, new TypeReference<List<ProduitSearch>>() {});
        });
        StorageService storageService = mock(StorageService.class);
        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);

        favoris = new ProduitFavoriServiceImpl(
            IntegrationPostgresDatabase.bean(ProduitFavoriRepository.class),
            produitRepository,
            produitService,
            storageService
        );
        substitutions = new SubstitutionComptoirServiceImpl(substitutRepository, produitService, storageService);
    }

    // ===== favoris =====

    @Test
    @DisplayName("un favori s'épingle une seule fois et la grille suit l'ordre d'épinglage, stock compris")
    void epinglerEtLister() {
        Produit premier = produit(unique("FAV-A"), 0);
        Produit second = produit(unique("FAV-B"), 0);
        stock(premier, 12);
        stock(second, 0);

        favoris.ajouter(premier.getId());
        favoris.ajouter(second.getId());
        favoris.ajouter(premier.getId());
        em.flush();

        List<ProduitSearch> grille = favoris.lister();
        assertThat(grille).extracting(ProduitSearch::id).containsExactly(premier.getId(), second.getId());
        assertThat(grille.get(0).totalQuantity()).isEqualTo(12);
        assertThat(grille.get(1).totalQuantity()).isZero();
        assertThat(grille.get(0).regularunitprice()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("retirer un favori le sort de la grille, et retirer un produit qui n'en est pas un ne fait rien")
    void retirer() {
        Produit un = produit(unique("FAV-C"), 0);
        Produit deux = produit(unique("FAV-D"), 0);
        favoris.ajouter(un.getId());
        favoris.ajouter(deux.getId());
        em.flush();

        favoris.retirer(un.getId());
        favoris.retirer(un.getId());
        em.flush();

        assertThat(favoris.lister()).extracting(ProduitSearch::id).containsExactly(deux.getId());
    }

    @Test
    @DisplayName("réordonner place les produits cités d'abord, dans l'ordre cité, puis les oubliés ; un inconnu est ignoré")
    void reordonner() {
        Produit a = produit(unique("FAV-E"), 0);
        Produit b = produit(unique("FAV-F"), 0);
        Produit c = produit(unique("FAV-G"), 0);
        favoris.ajouter(a.getId());
        favoris.ajouter(b.getId());
        favoris.ajouter(c.getId());
        em.flush();

        favoris.reordonner(List.of(c.getId(), 987_654, a.getId()));
        em.flush();
        em.clear();

        assertThat(favoris.lister()).extracting(ProduitSearch::id).containsExactly(c.getId(), a.getId(), b.getId());
    }

    @Test
    @DisplayName("les suggestions sont les produits sans ordonnance les plus vendus au comptoir, hors favoris, hors ordonnance et hors carnet")
    void suggestions() {
        Produit frequent = produit(unique("SUG-FREQUENT"), 0);
        Produit occasionnel = produit(unique("SUG-OCCASIONNEL"), 0);
        Produit dejaEpingle = produit(unique("SUG-EPINGLE"), 0);
        Produit surOrdonnance = produit(unique("SUG-ORDONNANCE"), 0);
        surOrdonnance.setStatutLegal(StatutLegal.LISTE_I);
        Produit auCarnet = produit(unique("SUG-CARNET"), 0);
        CashRegister caisse = caisseOuverte(50_000);

        for (int i = 0; i < 3; i++) {
            ligneDeVente(venteComptant(caisse, 10_000), frequent, 1);
        }
        // Une grosse quantité sur un seul ticket ne fait pas un produit fréquent : il passe après.
        ligneDeVente(venteComptant(caisse, 50_000), occasionnel, 20);
        for (int i = 0; i < 5; i++) {
            ligneDeVente(venteComptant(caisse, 10_000), dejaEpingle, 1);
            ligneDeVente(venteComptant(caisse, 10_000), surOrdonnance, 1);
            ligneDeVente(venteCarnet(caisse, 10_000), auCarnet, 1);
        }
        favoris.ajouter(dejaEpingle.getId());
        em.flush();
        em.createNativeQuery("REFRESH MATERIALIZED VIEW mv_produits_frequents_comptoir").executeUpdate();

        List<FavoriSuggere> suggestions = favoris.suggerer(10);

        assertThat(suggestions).extracting(s -> s.produit().id()).containsExactly(frequent.getId(), occasionnel.getId());
        assertThat(suggestions.get(0).nbVentes()).isEqualTo(3);
        assertThat(suggestions.get(1).nbVentes()).isEqualTo(1);
        assertThat(suggestions.get(1).qteVendue()).isEqualTo(20);
    }

    @Test
    @DisplayName("la grille est plafonnée : le treizième favori est refusé, et en retirer un libère la place")
    void grillePleine() {
        java.util.List<Produit> produits = new java.util.ArrayList<>();
        for (int i = 0; i < 13; i++) {
            produits.add(produit(unique("FAV-MAX-" + i), 0));
        }
        for (int i = 0; i < 12; i++) {
            favoris.ajouter(produits.get(i).getId());
        }
        em.flush();

        assertThatThrownBy(() -> favoris.ajouter(produits.get(12).getId())).isInstanceOf(BadRequestAlertException.class);
        assertThat(favoris.lister()).hasSize(12);

        favoris.retirer(produits.get(0).getId());
        favoris.ajouter(produits.get(12).getId());
        em.flush();
        assertThat(favoris.lister()).hasSize(12);
    }

    @Test
    @DisplayName("épingler deux fois le même produit ne compte qu'une fois dans le plafond")
    void doublonHorsPlafond() {
        Produit un = produit(unique("FAV-DBL"), 0);
        favoris.ajouter(un.getId());
        favoris.ajouter(un.getId());
        em.flush();

        assertThat(favoris.lister()).hasSize(1);
    }

    @Test
    @DisplayName("épingler un produit inconnu est refusé")
    void produitInconnu() {
        assertThatThrownBy(() -> favoris.ajouter(987_654)).isInstanceOf(BadRequestAlertException.class);
    }

    // ===== substituts =====

    @Test
    @DisplayName("les substituts proposés sont ceux en stock, lus dans les deux sens, génériques avant thérapeutiques puis du moins cher au plus cher")
    void substitutsDisponibles() {
        Produit manquant = produit(unique("SUB-ORIGINE"), 0);
        Produit genericCher = produit(unique("SUB-GENERIQUE-CHER"), 0);
        Produit genericPasCher = produit(unique("SUB-GENERIQUE-PAS-CHER"), 0);
        Produit genericEnRupture = produit(unique("SUB-GENERIQUE-RUPTURE"), 0);
        Produit therapeutique = produit(unique("SUB-THERAPEUTIQUE"), 0);
        genericCher.setRegularUnitPrice(9_000);
        genericPasCher.setRegularUnitPrice(6_000);
        therapeutique.setRegularUnitPrice(1_000);
        stock(genericCher, 5);
        stock(genericPasCher, 2);
        stock(genericEnRupture, 0);
        stock(therapeutique, 8);

        // Écrits dans un sens ou dans l'autre : la table se lit dans les deux.
        lier(manquant, genericCher, TypeSubstitut.GENERIQUE);
        lier(genericPasCher, manquant, TypeSubstitut.GENERIQUE);
        lier(manquant, genericEnRupture, TypeSubstitut.GENERIQUE);
        lier(therapeutique, manquant, TypeSubstitut.THERAPEUTIQUE);
        em.flush();

        List<SubstitutPropose> propositions = substitutions.lireSubstitutsDisponibles(manquant.getId());

        assertThat(propositions).extracting(p -> p.produit().id()).containsExactly(genericPasCher.getId(), genericCher.getId(), therapeutique.getId());
        assertThat(propositions).extracting(SubstitutPropose::typeSubstitut).containsExactly("GENERIQUE", "GENERIQUE", "THERAPEUTIQUE");
        assertThat(propositions.get(0).typeSubstitutLibelle()).isEqualTo("Générique");
        assertThat(propositions.get(0).produit().totalQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("un produit sans substitut, ou dont tous les substituts sont en rupture, ne propose rien")
    void aucunSubstitut() {
        Produit seul = produit(unique("SUB-SEUL"), 0);
        Produit autre = produit(unique("SUB-RUPTURE"), 0);
        Produit sansStock = produit(unique("SUB-SANS-STOCK"), 0);
        stock(sansStock, 0);
        lier(autre, sansStock, TypeSubstitut.GENERIQUE);
        em.flush();

        assertThat(substitutions.lireSubstitutsDisponibles(seul.getId())).isEmpty();
        assertThat(substitutions.lireSubstitutsDisponibles(autre.getId())).isEmpty();
    }

    private void lier(Produit produit, Produit substitut, TypeSubstitut type) {
        Substitut lien = new Substitut();
        lien.setProduit(produit);
        lien.setSubstitut(substitut);
        lien.setType(type);
        substitutRepository.save(lien);
    }
}

package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Une suggestion de commande regroupe, par fournisseur, ce qu'il faudrait réapprovisionner. Trois
 * choses ne se vérifient qu'en base.
 *
 * <p>La <b>fusion</b> de plusieurs suggestions du même fournisseur d'abord : {@code suggestion_line}
 * porte une contrainte d'unicité sur {@code (suggestion, fournisseur_produit)}. Déplacer les lignes
 * d'une suggestion vers une autre qui référence déjà le même couple viole la contrainte — c'est
 * pour cela que les lignes en double sont supprimées plutôt que repointées, et cela ne se démontre
 * pas sur des doublures.
 *
 * <p>L'<b>assainissement</b> ensuite : il relit le stock réel de chaque produit et retire les
 * lignes devenues inutiles. Il lit pour cela le stock de l'emplacement de vente, ce qui suppose des
 * {@code stock_produit} réellement rattachés.
 *
 * <p>Les <b>lectures</b> enfin : la liste paginée passe par une projection filtrée, l'agrégat par
 * fournisseur par une requête de regroupement, et le budget de commande par deux requêtes natives.
 */
@DisplayName("SuggestionProduitService — suggestions de commande sur PostgreSQL")
class SuggestionProduitServiceIntegrationTest extends AbstractStockIntegrationTest {

    @BeforeEach
    void reglagesDeSuggestion() {
        lenient().when(services.appConfigurationService.findSuggestionRetention()).thenReturn(90);
        lenient().when(services.appConfigurationService.getNombreJourRetentionCommande()).thenReturn(90);
        lenient().when(services.appConfigurationService.getNthMoisConsommation()).thenReturn(3);
        lenient().when(services.storageService.getDefaultMagasinMainStorage()).thenReturn(rayon);
    }

    // ===== lectures =====

    @Test
    @DisplayName("La liste filtre par type, statut et fournisseur")
    void listeFiltree() {
        Fournisseur fournisseur = fournisseurPartage();
        Fournisseur autreFournisseur = fournisseurPartage();
        Suggestion automatique = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        Suggestion manuelle = suggestion(autreFournisseur, TypeSuggession.MANUELLE, StatutSuggession.VALIDEE);
        viderLeCache();

        assertEquals(2, services.suggestionProduitService.getAllSuggestion(null, null, null, null, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(
            List.of(automatique.getId()),
            services.suggestionProduitService
                .getAllSuggestion(null, null, TypeSuggession.AUTO, null, PageRequest.of(0, 20))
                .map(SuggestionProjection::id)
                .getContent()
        );
        assertEquals(
            1,
            services.suggestionProduitService
                .getAllSuggestion(null, null, null, Set.of(StatutSuggession.VALIDEE), PageRequest.of(0, 20))
                .getTotalElements()
        );
        assertEquals(
            List.of(manuelle.getId()),
            services.suggestionProduitService
                .getAllSuggestion(null, Set.of(autreFournisseur.getId()), null, null, PageRequest.of(0, 20))
                .map(SuggestionProjection::id)
                .getContent()
        );
    }

    @Test
    @DisplayName("Le compteur par statut suit la validation d'une suggestion")
    void compteurParStatut() {
        Suggestion suggestion = suggestion(fournisseurPartage(), TypeSuggession.AUTO, StatutSuggession.GENEREE);
        viderLeCache();

        long genereesAuDepart = services.suggestionProduitService.countByStatut(StatutSuggession.GENEREE);
        long valideesAuDepart = services.suggestionProduitService.countByStatut(StatutSuggession.VALIDEE);

        services.suggestionProduitService.validerSuggestion(suggestion.getId());
        viderLeCache();

        assertEquals(genereesAuDepart - 1, services.suggestionProduitService.countByStatut(StatutSuggession.GENEREE));
        assertEquals(valideesAuDepart + 1, services.suggestionProduitService.countByStatut(StatutSuggession.VALIDEE));

        Suggestion relue = em.find(Suggestion.class, suggestion.getId());
        assertEquals(StatutSuggession.VALIDEE, relue.getStatut());
        assertEquals(utilisateur.getId(), relue.getValidePar().getId());
        assertEquals(java.time.LocalDate.now(), relue.getDateValidation().toLocalDate());
    }

    @Test
    @DisplayName("Une suggestion se relit avec sa référence et l'agrégat de ses lignes")
    void relectureDuneSuggestion() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ligne(suggestion, produitChez(fournisseur, "DOLIPRANE", 6_000), 10);
        ligne(suggestion, produitChez(fournisseur, "EFFERALGAN", 5_000), 4);
        viderLeCache();

        assertEquals(
            suggestion.getSuggessionReference(),
            services.suggestionProduitService.getSuggestionById(suggestion.getId()).orElseThrow().getSuggessionReference()
        );
        assertTrue(services.suggestionProduitService.getSuggestionById(999_999).isEmpty());
    }

    @Test
    @DisplayName("Les lignes d'une suggestion remontent avec le stock courant du rayon")
    void lignesAvecStockCourant() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        Produit produit = produitChez(fournisseur, "AVEC STOCK", 6_000);
        stock(produit, rayon, 7);
        ligne(suggestion, produit, 10);
        viderLeCache();

        List<SuggestionLineDTO> lignes = services.suggestionProduitService.getAllSuggestionLines(suggestion.getId(), null, null);

        assertEquals(1, lignes.size());
        SuggestionLineDTO ligne = lignes.getFirst();
        assertEquals(produit.getId(), ligne.produitId());
        assertEquals(10, ligne.quantity());
        assertEquals(7, ligne.currentStock());
        assertFalse(ligne.quantiteModifieeManuel());
    }

    @Test
    @DisplayName("L'agrégat par fournisseur regroupe les suggestions ouvertes")
    void agregatParFournisseur() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ligne(suggestion, produitChez(fournisseur, "REGROUPE", 6_000), 10);
        viderLeCache();

        assertTrue(
            services.suggestionProduitService
                .getSuggestionsParFournisseur(Set.of(StatutSuggession.GENEREE), Set.of(fournisseur.getId()), null)
                .stream()
                .anyMatch(f -> f.fournisseurId().equals(fournisseur.getId())),
            "le fournisseur de la suggestion apparaît dans l'agrégat"
        );
    }

    // ===== lignes =====

    @Test
    @DisplayName("Ajouter un produit déjà suggéré cumule la quantité au lieu d'ajouter une ligne")
    void ajoutCumuleSurLaLigneExistante() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        Produit produit = produitChez(fournisseur, "A CUMULER", 6_000);
        ligne(suggestion, produit, 10);
        viderLeCache();

        services.suggestionProduitService.addSuggestionLine(suggestion.getId(), demandeDeLigne(produit, 5));
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT COUNT(*) FROM suggestion_line WHERE suggestion_id = " + suggestion.getId()),
            "la contrainte (suggestion, fournisseur_produit) tient"
        );
        assertEquals(15, compter("SELECT quantity FROM suggestion_line WHERE suggestion_id = " + suggestion.getId()));
    }

    @Test
    @DisplayName("Ajouter un produit non suggéré ouvre une nouvelle ligne")
    void ajoutDuneNouvelleLigne() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ligne(suggestion, produitChez(fournisseur, "DEJA LA", 6_000), 10);
        Produit nouveau = produitChez(fournisseur, "NOUVEAU", 5_000);
        viderLeCache();

        services.suggestionProduitService.addSuggestionLine(suggestion.getId(), demandeDeLigne(nouveau, 7));
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM suggestion_line WHERE suggestion_id = " + suggestion.getId()));
        assertEquals(
            7,
            compter(
                "SELECT sl.quantity FROM suggestion_line sl JOIN fournisseur_produit fp ON sl.fournisseur_produit_id = fp.id WHERE fp.produit_id = " +
                nouveau.getId()
            )
        );
    }

    @Test
    @DisplayName("Corriger une quantité marque la ligne comme saisie à la main, la réinitialiser l'annule")
    void correctionManuelleDeLaQuantite() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        SuggestionLine ligne = ligne(suggestion, produitChez(fournisseur, "A CORRIGER", 6_000), 10);
        viderLeCache();

        services.suggestionProduitService.updateSuggestionLinQuantity(new SuggestionLineDTO(
            ligne.getId(), 25, null, null, null, null, null, null, null, 0, null, 0, 0, null, null, null, null, false, null, null
        ));
        viderLeCache();

        assertEquals(25, compter("SELECT quantity FROM suggestion_line WHERE id = " + ligne.getId()));
        assertTrue(em.find(SuggestionLine.class, ligne.getId()).isQuantiteModifieeManuel());

        services.suggestionProduitService.resetQuantiteManuelle(ligne.getId());
        viderLeCache();

        assertFalse(em.find(SuggestionLine.class, ligne.getId()).isQuantiteModifieeManuel(), "le batch peut de nouveau recalculer la ligne");
    }

    @Test
    @DisplayName("Supprimer une ligne l'efface sans toucher à sa suggestion")
    void suppressionDuneLigne() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        SuggestionLine ligne = ligne(suggestion, produitChez(fournisseur, "A JETER", 6_000), 10);
        ligne(suggestion, produitChez(fournisseur, "A GARDER", 6_000), 4);
        viderLeCache();

        services.suggestionProduitService.deleteSuggestionLine(Set.of(ligne.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligne.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM suggestion_line WHERE suggestion_id = " + suggestion.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + suggestion.getId()));
    }

    @Test
    @DisplayName("Supprimer une suggestion l'efface avec ses lignes")
    void suppressionDuneSuggestion() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion aGarder = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ligne(aGarder, produitChez(fournisseur, "A GARDER", 6_000), 4);
        Suggestion aSupprimer = suggestion(fournisseur, TypeSuggession.MANUELLE, StatutSuggession.GENEREE);
        SuggestionLine ligneSupprimee = ligne(aSupprimer, produitChez(fournisseur, "A JETER", 6_000), 10);
        viderLeCache();

        services.suggestionProduitService.deleteSuggestion(Set.of(aSupprimer.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + aSupprimer.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligneSupprimee.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + aGarder.getId()));
    }

    @Test
    @DisplayName("Rejeter une suggestion la supprime avec ses lignes")
    void rejetDuneSuggestion() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        SuggestionLine ligne = ligne(suggestion, produitChez(fournisseur, "A REJETER", 6_000), 10);
        viderLeCache();

        services.suggestionProduitService.rejeterSuggestion(suggestion.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + suggestion.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligne.getId()));
    }

    // ===== fusion =====

    @Test
    @DisplayName("Fusionner deux suggestions du même fournisseur regroupe leurs lignes")
    void fusionDeDeuxSuggestions() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion ancienne = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ancienne.setUpdatedAt(LocalDateTime.now().minusDays(1));
        SuggestionLine ligneAncienne = ligne(ancienne, produitChez(fournisseur, "ANCIEN", 6_000), 4);

        Suggestion recente = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);
        ligne(recente, produitChez(fournisseur, "RECENT", 5_000), 10);
        viderLeCache();

        services.suggestionProduitService.fusionnerSuggestion(Set.of(ancienne.getId(), recente.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + ancienne.getId()), "la plus ancienne disparaît");
        assertEquals(1, compter("SELECT COUNT(*) FROM suggestion WHERE id = " + recente.getId()));
        assertEquals(2, compter("SELECT COUNT(*) FROM suggestion_line WHERE suggestion_id = " + recente.getId()));
        assertEquals(recente.getId().intValue(), compter("SELECT suggestion_id FROM suggestion_line WHERE id = " + ligneAncienne.getId()));
    }

    @Test
    @DisplayName("Fusionner des suggestions de fournisseurs différents est refusé")
    void fusionDeFournisseursDifferents() {
        Suggestion premiere = suggestion(fournisseurPartage(), TypeSuggession.AUTO, StatutSuggession.GENEREE);
        Suggestion seconde = suggestion(fournisseurPartage(), TypeSuggession.AUTO, StatutSuggession.GENEREE);
        viderLeCache();

        Set<Integer> ids = Set.of(premiere.getId(), seconde.getId());

        assertTrue(
            assertThrows(GenericError.class, () -> services.suggestionProduitService.fusionnerSuggestion(ids))
                .getMessage()
                .contains("fournisseurs differents")
        );
    }

    // ===== assainissement =====

    @Test
    @DisplayName("L'assainissement retire les lignes des produits désactivés ou déjà bien approvisionnés")
    void assainissement() {
        Fournisseur fournisseur = fournisseurPartage();
        Suggestion suggestion = suggestion(fournisseur, TypeSuggession.AUTO, StatutSuggession.GENEREE);

        Produit aGarder = produitChez(fournisseur, "MANQUE", 6_000);
        aGarder.setQtySeuilMini(20);
        stock(aGarder, rayon, 3);
        SuggestionLine ligneGardee = ligne(suggestion, aGarder, 10);

        Produit bienFourni = produitChez(fournisseur, "BIEN FOURNI", 6_000);
        bienFourni.setQtySeuilMini(5);
        stock(bienFourni, rayon, 40);
        SuggestionLine ligneBienFournie = ligne(suggestion, bienFourni, 10);

        Produit desactive = produitChez(fournisseur, "DESACTIVE", 6_000);
        desactive.setQtySeuilMini(20);
        desactive.setStatus(Status.DISABLE);
        stock(desactive, rayon, 1);
        SuggestionLine ligneDesactivee = ligne(suggestion, desactive, 10);
        viderLeCache();

        services.suggestionProduitService.sanitize(suggestion.getId());
        viderLeCache();

        assertEquals(1, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligneGardee.getId()), "le produit qui manque reste suggéré");
        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligneBienFournie.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_line WHERE id = " + ligneDesactivee.getId()));
    }

    // ===== fabriques du jeu d'essai propres aux suggestions =====

    private Fournisseur fournisseurPartage() {
        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);
        em.flush();
        return fournisseur;
    }

    /** Un produit référencé chez un fournisseur imposé : c'est ce couple que la suggestion vise. */
    private Produit produitChez(Fournisseur fournisseur, String libelle, int prixAchat) {
        Produit produit = produit(unique(libelle), 10_000, prixAchat);
        FournisseurProduit reference = new FournisseurProduit();
        reference.setProduit(produit);
        reference.setFournisseur(fournisseur);
        reference.setCodeCip(unique("CIP"));
        reference.setPrixAchat(prixAchat);
        reference.setPrixUni(produit.getRegularUnitPrice());
        em.persist(reference);
        produit.setFournisseurProduitPrincipal(reference);
        produit.getFournisseurProduits().add(reference);
        em.flush();
        return produit;
    }

    private Suggestion suggestion(Fournisseur fournisseur, TypeSuggession type, StatutSuggession statut) {
        Suggestion suggestion = new Suggestion();
        suggestion.setSuggessionReference(unique("SUG"));
        suggestion.setMagasin(magasin);
        suggestion.setFournisseur(fournisseur);
        suggestion.setTypeSuggession(type);
        suggestion.setStatut(statut);
        suggestion.setLastUserEdit(utilisateur);
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setUpdatedAt(LocalDateTime.now());
        em.persist(suggestion);
        em.flush();
        return suggestion;
    }

    private SuggestionLine ligne(Suggestion suggestion, Produit produit, int quantite) {
        SuggestionLine ligne = new SuggestionLine();
        ligne.setSuggestion(suggestion);
        ligne.setFournisseurProduit(produit.getFournisseurProduitPrincipal());
        ligne.setQuantity(quantite);
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        suggestion.getSuggestionLines().add(ligne);
        em.flush();
        return ligne;
    }

    private SuggestionLineDTO demandeDeLigne(Produit produit, int quantite) {
        return new SuggestionLineDTO(
            null, quantite, null, null, null, null, null, produit.getId(), null, 0, null, 0, 0, null, null, null, null, false, null, null
        );
    }
}

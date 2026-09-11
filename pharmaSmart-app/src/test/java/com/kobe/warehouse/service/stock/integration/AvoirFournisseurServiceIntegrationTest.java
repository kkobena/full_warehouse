package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Un avoir fournisseur naît d'un retour de marchandise et fige ce que le fournisseur accepte de
 * reprendre. Trois choses ne s'observent que contre une vraie base.
 *
 * <p>D'abord la <b>numérotation</b> : la référence se calcule à partir de l'identifiant généré, ce
 * qui impose deux écritures successives sur la même ligne — impossible à éprouver sans séquence
 * réelle, et la colonne est unique.
 *
 * <p>Ensuite le <b>sort du retour</b> : accepter l'intégralité des quantités le clôt, en accepter
 * une partie le laisse ouvert. Cette bascule et le report de {@code accepted_qty} sur chaque ligne
 * de retour sont deux écritures dans deux tables qui doivent rester d'accord.
 *
 * <p>Enfin les <b>lectures</b> : l'encours par fournisseur est une agrégation SQL rendue en
 * {@code Object[]}, et la recherche multi-critères une {@code Specification} — ni l'une ni l'autre
 * ne prouve quoi que ce soit sur des doublures.
 */
@DisplayName("AvoirFournisseurService — avoirs fournisseur sur PostgreSQL")
class AvoirFournisseurServiceIntegrationTest extends AbstractStockIntegrationTest {

    // ===== création =====

    @Test
    @DisplayName("Accepter l'intégralité du retour crée un avoir numéroté et clôt le retour")
    void avoirTotalClotLeRetour() {
        Produit produit = produitReference("DOLIPRANE", 6_000);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        RetourBonItem ligne = ligneDeRetour(retour, produit, 10);
        viderLeCache();

        AvoirFournisseurDTO avoir = services.avoirFournisseurService.create(
            new AvoirFournisseurCommand(retour.getId(), "casse transport", List.of(acceptation(ligne, 10)))
        );
        viderLeCache();

        assertNotNull(avoir.getId());
        assertEquals("AV-" + Year.now().getValue() + "-" + String.format("%04d", avoir.getId()), avoir.getReference());
        assertEquals(60_000, avoir.getMontant(), "10 unités au prix d'achat de 6 000");
        assertEquals(AvoirFournisseurStatut.EN_ATTENTE, avoir.getStatut());

        assertEquals(1, compter("SELECT COUNT(*) FROM avoir_fournisseur_line WHERE avoir_fournisseur_id = " + avoir.getId()));
        assertEquals(10, compter("SELECT accepted_qty FROM retour_bon_item WHERE id = " + ligne.getId()));
        assertEquals(RetourStatut.CLOSED, em.find(RetourBon.class, retour.getId()).getStatut());
    }

    @Test
    @DisplayName("Une acceptation partielle laisse le retour ouvert en acceptation partielle")
    void avoirPartielLaisseLeRetourOuvert() {
        Produit produit = produitReference("EFFERALGAN", 5_000);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        RetourBonItem ligne = ligneDeRetour(retour, produit, 10);
        viderLeCache();

        AvoirFournisseurDTO avoir = services.avoirFournisseurService.create(
            new AvoirFournisseurCommand(retour.getId(), null, List.of(acceptation(ligne, 4)))
        );
        viderLeCache();

        assertEquals(20_000, avoir.getMontant());
        assertEquals(4, compter("SELECT accepted_qty FROM retour_bon_item WHERE id = " + ligne.getId()));
        assertEquals(RetourStatut.PARTIALLY_ACCEPTED, em.find(RetourBon.class, retour.getId()).getStatut());
    }

    @Test
    @DisplayName("Un prix d'achat imposé sur la ligne prime sur celui du retour")
    void prixImposeSurLaLigne() {
        Produit produit = produitReference("PRIX REVU", 6_000);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        RetourBonItem ligne = ligneDeRetour(retour, produit, 5);
        viderLeCache();

        AvoirFournisseurDTO avoir = services.avoirFournisseurService.create(
            new AvoirFournisseurCommand(retour.getId(), null, List.of(new AvoirFournisseurCommand.AvoirLigneCommand(ligne.getId(), 5, 4_000)))
        );
        viderLeCache();

        assertEquals(20_000, avoir.getMontant(), "5 × 4 000, et non le prix d'achat du retour");
    }

    @Test
    @DisplayName("Un avoir sans ligne acceptée vaut zéro et clôt tout de même le retour")
    void avoirSansLigne() {
        Produit produit = produitReference("REFUS TOTAL", 6_000);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        ligneDeRetour(retour, produit, 10);
        viderLeCache();

        AvoirFournisseurDTO avoir = services.avoirFournisseurService.create(
            new AvoirFournisseurCommand(retour.getId(), "rien de repris", List.of())
        );
        viderLeCache();

        assertEquals(0, avoir.getMontant());
        assertEquals(0, compter("SELECT COUNT(*) FROM avoir_fournisseur_line WHERE avoir_fournisseur_id = " + avoir.getId()));
        assertEquals(RetourStatut.CLOSED, em.find(RetourBon.class, retour.getId()).getStatut());
    }

    @Test
    @DisplayName("Un retour introuvable ou déjà traité n'ouvre pas d'avoir")
    void retourInexploitable() {
        Produit produit = produitReference("DEJA TRAITE", 6_000);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        retour.setStatut(RetourStatut.CLOSED);
        viderLeCache();

        AvoirFournisseurCommand surRetourClos = new AvoirFournisseurCommand(retour.getId(), null, List.of());
        assertTrue(
            assertThrows(GenericError.class, () -> services.avoirFournisseurService.create(surRetourClos))
                .getMessage()
                .contains("déjà traité")
        );

        AvoirFournisseurCommand surRetourInconnu = new AvoirFournisseurCommand(999_999, null, List.of());
        assertTrue(
            assertThrows(GenericError.class, () -> services.avoirFournisseurService.create(surRetourInconnu))
                .getMessage()
                .contains("RetourBon non trouvé")
        );
    }

    // ===== cycle de vie =====

    @Test
    @DisplayName("Le statut d'un avoir suit son traitement, et un avoir annulé se fige")
    void cycleDeVieDuStatut() {
        AvoirFournisseurDTO avoir = avoirEnAttente("A SUIVRE", 6_000, 10);
        viderLeCache();

        assertEquals(
            AvoirFournisseurStatut.IMPUTE,
            services.avoirFournisseurService.updateStatut(avoir.getId(), AvoirFournisseurStatut.IMPUTE).getStatut()
        );
        viderLeCache();
        assertEquals(AvoirFournisseurStatut.IMPUTE, em.find(AvoirFournisseur.class, avoir.getId()).getStatut());

        assertTrue(
            assertThrows(GenericError.class, () -> services.avoirFournisseurService.annuler(avoir.getId(), "trop tard"))
                .getMessage()
                .contains("déjà imputé")
        );
    }

    @Test
    @DisplayName("Annuler un avoir enregistre le motif, et on ne l'annule pas deux fois")
    void annulationAvecMotif() {
        AvoirFournisseurDTO avoir = avoirEnAttente("A ANNULER", 6_000, 10);
        viderLeCache();

        AvoirFournisseurDTO annule = services.avoirFournisseurService.annuler(avoir.getId(), "erreur de saisie");
        viderLeCache();

        assertEquals(AvoirFournisseurStatut.ANNULE, annule.getStatut());
        assertEquals("erreur de saisie", em.find(AvoirFournisseur.class, avoir.getId()).getCommentaire());

        assertTrue(
            assertThrows(GenericError.class, () -> services.avoirFournisseurService.annuler(avoir.getId(), "encore"))
                .getMessage()
                .contains("déjà annulé")
        );
        assertTrue(
            assertThrows(
                GenericError.class,
                () -> services.avoirFournisseurService.updateStatut(avoir.getId(), AvoirFournisseurStatut.IMPUTE)
            )
                .getMessage()
                .contains("avoir annulé")
        );
    }

    @Test
    @DisplayName("Un avoir introuvable ne se modifie ni ne s'annule")
    void avoirIntrouvable() {
        assertThrows(GenericError.class, () -> services.avoirFournisseurService.updateStatut(999_999, AvoirFournisseurStatut.IMPUTE));
        assertThrows(GenericError.class, () -> services.avoirFournisseurService.annuler(999_999, "motif"));
    }

    // ===== lectures =====

    @Test
    @DisplayName("L'encours par fournisseur additionne les seuls avoirs en attente")
    void encoursParFournisseur() {
        AvoirFournisseurDTO premier = avoirEnAttente("PREMIER", 6_000, 10);
        AvoirFournisseurDTO second = avoirEnAttente("SECOND", 5_000, 2);
        AvoirFournisseurDTO impute = avoirEnAttente("IMPUTE", 9_000, 3);
        services.avoirFournisseurService.updateStatut(impute.getId(), AvoirFournisseurStatut.IMPUTE);
        viderLeCache();

        List<AvoirEncoursFournisseurDTO> encours = services.avoirFournisseurService.getEncoursParFournisseur();

        assertEquals(60_000L, montantPour(encours, premier));
        assertEquals(10_000L, montantPour(encours, second));
        assertTrue(
            encours.stream().noneMatch(e -> e.getFournisseurId().equals(impute.getFournisseurId())),
            "un avoir imputé ne pèse plus sur l'encours"
        );
        assertEquals(2, services.avoirFournisseurService.countEnAttente());
    }

    @Test
    @DisplayName("La recherche filtre par statut, fournisseur, référence et période")
    void rechercheMultiCritere() {
        AvoirFournisseurDTO avoir = avoirEnAttente("A CHERCHER", 6_000, 10);
        AvoirFournisseurDTO autre = avoirEnAttente("AUTRE", 5_000, 1);
        services.avoirFournisseurService.updateStatut(autre.getId(), AvoirFournisseurStatut.IMPUTE);
        viderLeCache();

        Integer fournisseurId = avoir.getFournisseurId();
        LocalDate veille = LocalDate.now().minusDays(1);
        LocalDate lendemain = LocalDate.now().plusDays(1);

        assertEquals(
            List.of(avoir.getId()),
            services.avoirFournisseurService
                .findAll(null, null, fournisseurId, null, null, PageRequest.of(0, 20))
                .map(AvoirFournisseurDTO::getId)
                .getContent()
        );
        assertEquals(
            1,
            services.avoirFournisseurService
                .findAll(avoir.getReference(), null, null, null, null, PageRequest.of(0, 20))
                .getTotalElements()
        );
        assertEquals(
            1,
            services.avoirFournisseurService
                .findAll(null, AvoirFournisseurStatut.IMPUTE, null, veille, lendemain, PageRequest.of(0, 20))
                .getTotalElements()
        );
        assertTrue(
            services.avoirFournisseurService
                .findAll(null, null, null, veille.minusDays(30), veille.minusDays(10), PageRequest.of(0, 20))
                .isEmpty(),
            "hors période, rien ne remonte"
        );
        assertTrue(
            services.avoirFournisseurService.findAll(null, null, null, null, null, PageRequest.of(0, 20)).getTotalElements() >= 2,
            "sans filtre, la recherche rend tout le registre"
        );
    }

    // ===== fabriques du jeu d'essai propres aux avoirs =====

    private long montantPour(List<AvoirEncoursFournisseurDTO> encours, AvoirFournisseurDTO avoir) {
        return encours
            .stream()
            .filter(e -> e.getFournisseurId().equals(avoir.getFournisseurId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("aucun encours pour le fournisseur " + avoir.getFournisseurId()))
            .getMontantEncours();
    }

    /** Un produit et son fournisseur principal, dont le prix d'achat fait le montant de l'avoir. */
    private Produit produitReference(String libelle, int prixAchat) {
        Produit produit = produit(unique(libelle), 10_000, prixAchat);
        fournisseurProduit(produit);
        em.flush();
        return produit;
    }

    private RetourBon retourBon(Fournisseur fournisseur) {
        RetourBon retour = new RetourBon();
        retour.setUser(utilisateur);
        retour.setDateMtv(LocalDateTime.now());
        retour.setStatut(RetourStatut.VALIDATED);
        retour.setFournisseur(fournisseur);
        retour.setReference(unique("RET"));
        em.persist(retour);
        em.flush();
        return retour;
    }

    private RetourBonItem ligneDeRetour(RetourBon retour, Produit produit, int quantite) {
        FournisseurProduit reference = produit.getFournisseurProduitPrincipal();
        RetourBonItem ligne = new RetourBonItem();
        ligne.setRetourBon(retour);
        ligne.setMotifRetour(premierMotif());
        ligne.setQtyMvt(quantite);
        ligne.setInitStock(quantite);
        ligne.setAfterStock(0);
        ligne.setPrixAchat(reference.getPrixAchat());
        ligne.setDateMtv(LocalDateTime.now());
        em.persist(ligne);
        retour.getRetourBonItems().add(ligne);
        em.flush();
        return ligne;
    }

    private AvoirFournisseurCommand.AvoirLigneCommand acceptation(RetourBonItem ligne, int quantite) {
        return new AvoirFournisseurCommand.AvoirLigneCommand(ligne.getId(), quantite, null);
    }

    /** Un avoir déjà créé, en attente : le point de départ des tests de statut et de lecture. */
    private AvoirFournisseurDTO avoirEnAttente(String libelle, int prixAchat, int quantite) {
        Produit produit = produitReference(libelle, prixAchat);
        RetourBon retour = retourBon(produit.getFournisseurProduitPrincipal().getFournisseur());
        RetourBonItem ligne = ligneDeRetour(retour, produit, quantite);
        return services.avoirFournisseurService.create(
            new AvoirFournisseurCommand(retour.getId(), null, List.of(acceptation(ligne, quantite)))
        );
    }

    private MotifRetourProduit premierMotif() {
        return em
            .createQuery("SELECT m FROM MotifRetourProduit m ORDER BY m.id", MotifRetourProduit.class)
            .setMaxResults(1)
            .getSingleResult();
    }
}

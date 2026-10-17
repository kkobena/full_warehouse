package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.ProductResolveResult;
import com.kobe.warehouse.service.dto.ProductResolveResult.MatchedBy;
import com.kobe.warehouse.service.dto.ProduitCodeAliasDTO;
import com.kobe.warehouse.service.dto.ReceptionScanResultDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Résolution d'un code scanné sur un vrai PostgreSQL.
 *
 * <p>C'est le cœur de l'adaptation au marché ivoirien : le GTIN encodé par le fabricant n'est
 * jamais celui que le grossiste a saisi, donc le scan d'une boîte ne résout rien tant que
 * personne n'a fait le lien. Une fois le lien mémorisé, les scans suivants doivent passer seuls.
 *
 * <p>Rien de tout cela ne se vérifie sans base : la recherche de la ligne de commande repose sur
 * un {@code EXISTS} JPQL sur une entité jointe, et l'unicité des alias est posée par une
 * contrainte de table — deux choses qu'un mock ne peut pas contredire.
 */
@DisplayName("Résolution d'un code scanné et codes alternatifs sur PostgreSQL")
class ProductCodeAliasIntegrationTest extends AbstractStockIntegrationTest {

    /** GTIN-14 du fabricant, absent du catalogue du grossiste. */
    private static final String GTIN_FABRICANT = "07353163006031";

    /** EAN-13 que le parser déduit du GTIN ci-dessus, son chiffre indicateur étant zéro. */
    private static final String EAN_DU_GTIN = "7353163006031";

    private record Ligne(com.kobe.warehouse.domain.CommandeId commandeId, Integer produitId, String codeCip) {}

    private Ligne ligneACommander() {
        Produit produit = produit(unique("PRODUIT"));
        stock(produit, rayon, 10);
        FournisseurProduit fournisseurProduit = fournisseurProduit(produit);
        Commande commande = commande(fournisseurProduit.getFournisseur(), OrderStatut.RECEIVED);
        OrderLine ligne = ligneDeCommande(commande, fournisseurProduit, 10, 0);
        ligne.setUpdated(true);
        em.flush();
        return new Ligne(commande.getId(), produit.getId(), fournisseurProduit.getCodeCip());
    }

    @Test
    @DisplayName("Un GTIN inconnu ne résout rien, puis résout la ligne une fois mémorisé")
    void aliasMemoriseResoutLesScansSuivants() {
        Ligne ligne = ligneACommander();
        viderLeCache();

        ReceptionScanResultDTO avant = services.stockEntryService.processScanReception(
            ligne.commandeId().getId(),
            GTIN_FABRICANT
        );
        assertFalse(avant.found(), "le GTIN du fabricant n'est pas dans le catalogue");
        assertNotNull(avant.dataMatrixHint(), "les données lues sur la boîte sont remontées");
        // getProductCode() préfère l'EAN-13 déduit du GTIN à chiffre indicateur zéro : c'est le
        // code sous lequel le pharmacien cherchera le produit.
        assertEquals(EAN_DU_GTIN, avant.dataMatrixHint().scannedCode());

        services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        ReceptionScanResultDTO apres = services.stockEntryService.processScanReception(
            ligne.commandeId().getId(),
            GTIN_FABRICANT
        );
        assertTrue(apres.found(), "le même scan passe désormais sans intervention");
        assertNull(apres.dataMatrixHint(), "plus rien à faire saisir");
        assertEquals(1, apres.scannedQty());
    }

    @Test
    @DisplayName("Le CIP interne du grossiste, non numérique, résout la ligne")
    void codeCipInterneTrouveLaLigne() {
        Ligne ligne = ligneACommander();
        viderLeCache();

        // Un code comme « CIP-12 » n'est ni un EAN ni un CIP-13 : il n'est reconnu par aucune
        // nomenclature et ne remonte que par le code brut.
        ReceptionScanResultDTO resultat = services.stockEntryService.processScanReception(
            ligne.commandeId().getId(),
            ligne.codeCip()
        );

        assertTrue(resultat.found(), "le code du catalogue passe avant les alias");
    }

    @Test
    @DisplayName("Un code interne mémorisé comme alias résout la ligne")
    void aliasSurUnCodeInterne() {
        Ligne ligne = ligneACommander();
        String codeGrossiste = "GROS/A-7788";
        services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            codeGrossiste,
            CodeAliasType.CUSTOM,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        ReceptionScanResultDTO resultat = services.stockEntryService.processScanReception(
            ligne.commandeId().getId(),
            codeGrossiste
        );

        assertTrue(resultat.found(), "un alias CUSTOM doit être atteignable par le scan");
    }

    @Test
    @DisplayName("resoudreCode distingue la source de la correspondance")
    void sourceDeLaCorrespondance() {
        Ligne ligne = ligneACommander();
        services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        List<ProductResolveResult> parCip = services.productCodeResolverService.resoudreCode(ligne.codeCip());
        assertEquals(1, parCip.size());
        assertEquals(MatchedBy.CODE_CIP, parCip.getFirst().matchedBy());
        assertEquals(ligne.produitId(), parCip.getFirst().produitId());

        List<ProductResolveResult> parAlias = services.productCodeResolverService.resoudreCode(GTIN_FABRICANT);
        assertEquals(1, parAlias.size());
        assertEquals(MatchedBy.ALIAS, parAlias.getFirst().matchedBy());
        assertEquals(ligne.produitId(), parAlias.getFirst().produitId());
    }

    @Test
    @DisplayName("Mémoriser deux fois le même code ne crée qu'un alias")
    void memorisationIdempotente() {
        Ligne ligne = ligneACommander();

        ProduitCodeAliasDTO premier = services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        ProduitCodeAliasDTO second = services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        assertEquals(premier.id(), second.id(), "la contrainte d'unicité n'est pas violée, l'existant est renvoyé");
        assertEquals(1, services.productCodeResolverService.listerAlias(ligne.produitId()).size());
    }

    @Test
    @DisplayName("Un même code peut viser deux produits, les deux sont proposés")
    void codePartageParDeuxProduits() {
        Ligne premier = ligneACommander();
        Ligne second = ligneACommander();
        services.productCodeResolverService.enregistrerAlias(
            premier.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        services.productCodeResolverService.enregistrerAlias(
            second.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        List<ProductResolveResult> resultats = services.productCodeResolverService.resoudreCode(GTIN_FABRICANT);

        assertEquals(2, resultats.size(), "aucun produit n'est choisi d'office");
    }

    @Test
    @DisplayName("Un alias supprimé ne résout plus rien")
    void aliasSupprime() {
        Ligne ligne = ligneACommander();
        ProduitCodeAliasDTO alias = services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        services.productCodeResolverService.supprimerAlias(alias.id());
        viderLeCache();

        assertTrue(services.productCodeResolverService.resoudreCode(GTIN_FABRICANT).isEmpty());
        assertTrue(services.productCodeResolverService.listerAlias(ligne.produitId()).isEmpty());
    }

    @Test
    @DisplayName("La recherche produit du comptoir trouve un produit par son code alternatif")
    void rechercheComptoirParAlias() {
        Ligne ligne = ligneACommander();
        services.productCodeResolverService.enregistrerAlias(
            ligne.produitId(),
            GTIN_FABRICANT,
            CodeAliasType.GTIN_14,
            CodeAliasSource.MANUAL
        );
        viderLeCache();

        // search_produits_json est une fonction PL/pgSQL (V2.1.46) : seule la base peut la juger.
        String json = em
            .createNativeQuery("SELECT search_produits_json(:code, :magasin, 10)::text", String.class)
            .setParameter("code", GTIN_FABRICANT)
            .setParameter("magasin", magasin.getId())
            .getSingleResult()
            .toString();

        assertTrue(json.contains("\"id\": " + ligne.produitId()) || json.contains("\"id\":" + ligne.produitId()), json);
    }
}

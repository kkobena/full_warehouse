package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.records.ImportLineErrorRecord;
import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import com.kobe.warehouse.service.inventaire.impl.InventaireImportServiceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * L'import CSV est la reprise d'un comptage fait ailleurs — douchette autonome, tableur du
 * pharmacien. Le fichier n'est jamais propre : colonnes inversées, quantités vides, codes d'un
 * autre inventaire. Le service doit compter ce qu'il peut et rendre compte du reste ligne par
 * ligne, sans jamais compter une ligne à moitié — {@code quantity_init} et la valorisation
 * omises faisaient échouer la clôture bien plus tard.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventaireImportService — reprise d'un comptage par fichier CSV")
class InventaireImportServiceImplTest {

    private static final long INVENTAIRE = 42L;

    @Mock
    private StoreInventoryLineRepository storeInventoryLineRepository;

    @Mock
    private InventoryStockService inventoryStockService;

    @Mock
    private UserService userService;

    private InventaireImportService service;
    private AppUser operateur;

    @BeforeEach
    void init() {
        service = new InventaireImportServiceImpl(storeInventoryLineRepository, inventoryStockService, userService);
        operateur = new AppUser();
        operateur.setId(9);
    }

    @Test
    @DisplayName("Un fichier propre compte les lignes avec leur stock théorique et leur valorisation")
    void importNominal() {
        StoreInventoryLine doliprane = ligne("CIP-1", 600, 1_000);
        StoreInventoryLine efferalgan = ligne("CIP-2", 800, 1_400);
        when(storeInventoryLineRepository.findAllByStoreInventoryIdAndCodeCipIn(eq(INVENTAIRE), anySet()))
            .thenReturn(List.of(doliprane, efferalgan));
        when(inventoryStockService.buildStockMapForInventory(any(StoreInventory.class), anySet()))
            .thenReturn(Map.of(1, 100, 2, 40));
        when(userService.getUser()).thenReturn(operateur);

        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier("CIP-1;97\nCIP-2;40"));

        assertEquals(2, resultat.imported());
        assertEquals(0, resultat.ignored());
        assertEquals(0, resultat.rejected());

        assertEquals(100, doliprane.getQuantityInit(), "la quantité initiale vient du stock théorique");
        assertEquals(97, doliprane.getQuantityOnHand());
        assertEquals(-3, doliprane.getGap());
        assertEquals(600, doliprane.getInventoryValueCost(), "la valorisation est figée au comptage");
        assertEquals(1_000, doliprane.getLastUnitPrice());
        assertTrue(doliprane.getUpdated());
        assertSame(operateur, doliprane.getCountedBy());
        assertEquals(0, efferalgan.getGap());
        verify(storeInventoryLineRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Une quantité non numérique est rejetée, les autres lignes passent")
    void quantiteNonNumerique() {
        StoreInventoryLine ligne = ligne("CIP-1", 600, 1_000);
        when(storeInventoryLineRepository.findAllByStoreInventoryIdAndCodeCipIn(eq(INVENTAIRE), anySet()))
            .thenReturn(List.of(ligne));
        when(inventoryStockService.buildStockMapForInventory(any(StoreInventory.class), anySet()))
            .thenReturn(Map.of(1, 10));
        when(userService.getUser()).thenReturn(operateur);

        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier("CIP-1;5\nCIP-9;douze"));

        assertEquals(1, resultat.imported());
        assertEquals(1, resultat.rejected());
        ImportLineErrorRecord erreur = resultat.errors().getFirst();
        assertEquals(2, erreur.lineNumber(), "le numéro de ligne du fichier est reporté tel quel");
        assertEquals("CIP-9", erreur.rawCode());
        assertTrue(erreur.reason().contains("douze"), erreur.reason());
    }

    @Test
    @DisplayName("Une ligne à une seule colonne est rejetée comme incomplète")
    void ligneIncomplete() {
        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier("CIP-1"));

        assertEquals(0, resultat.imported());
        assertEquals(1, resultat.rejected());
        assertTrue(resultat.errors().getFirst().reason().startsWith("Ligne incomplète"));
        verify(storeInventoryLineRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Un code CIP vide est rejeté sans être confondu avec une quantité invalide")
    void codeVide() {
        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier(";12"));

        assertEquals(1, resultat.rejected());
        assertEquals("Code CIP vide", resultat.errors().getFirst().reason());
    }

    @Test
    @DisplayName("Un fichier dont aucun code n'appartient à l'inventaire est tout entier ignoré")
    void aucunCodeReconnu() {
        when(storeInventoryLineRepository.findAllByStoreInventoryIdAndCodeCipIn(eq(INVENTAIRE), anySet()))
            .thenReturn(List.of());

        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier("CIP-X;3\nCIP-Y;4"));

        assertEquals(0, resultat.imported());
        assertEquals(2, resultat.ignored(), "les deux codes sont comptés comme ignorés, pas en erreur");
        assertEquals(0, resultat.rejected());
        verify(storeInventoryLineRepository, never()).saveAll(anyList());
        verify(userService, never()).getUser();
    }

    @Test
    @DisplayName("Un code absent de l'inventaire est ignoré, le reste du fichier est compté")
    void codeInconnuIgnore() {
        StoreInventoryLine ligne = ligne("CIP-1", 600, 1_000);
        when(storeInventoryLineRepository.findAllByStoreInventoryIdAndCodeCipIn(eq(INVENTAIRE), anySet()))
            .thenReturn(List.of(ligne));
        when(inventoryStockService.buildStockMapForInventory(any(StoreInventory.class), anySet()))
            .thenReturn(Map.of(1, 10));
        when(userService.getUser()).thenReturn(operateur);

        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier("CIP-1;8\nCIP-AUTRE;3"));

        assertEquals(1, resultat.imported());
        assertEquals(1, resultat.ignored());
        assertEquals(8, ligne.getQuantityOnHand());
    }

    @Test
    @DisplayName("Un produit sans stock théorique connu part de zéro, pas d'une NPE")
    void produitSansStockTheorique() {
        StoreInventoryLine ligne = ligne("CIP-1", 600, 1_000);
        when(storeInventoryLineRepository.findAllByStoreInventoryIdAndCodeCipIn(eq(INVENTAIRE), anySet()))
            .thenReturn(List.of(ligne));
        when(inventoryStockService.buildStockMapForInventory(any(StoreInventory.class), anySet()))
            .thenReturn(Map.of());
        when(userService.getUser()).thenReturn(operateur);

        service.importDetail(INVENTAIRE, fichier("CIP-1;6"));

        assertEquals(0, ligne.getQuantityInit());
        assertEquals(6, ligne.getGap(), "tout le comptage est en écart positif");
    }

    @Test
    @DisplayName("Un fichier vide ne compte ni n'ignore rien")
    void fichierVide() {
        ImportResultRecord resultat = service.importDetail(INVENTAIRE, fichier(""));

        assertEquals(0, resultat.imported());
        assertEquals(0, resultat.ignored());
        assertEquals(0, resultat.rejected());
        verify(storeInventoryLineRepository, never()).findAllByStoreInventoryIdAndCodeCipIn(any(Long.class), anySet());
    }

    @Test
    @DisplayName("Un fichier illisible est rapporté comme tel plutôt que de faire échouer l'appel")
    void fichierIllisible() throws IOException {
        MultipartFile illisible = new MockMultipartFile("f", "f.csv", "text/csv", new byte[0]) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("disque plein");
            }
        };

        ImportResultRecord resultat = service.importDetail(INVENTAIRE, illisible);

        assertEquals(0, resultat.imported());
        assertEquals(1, resultat.errors().size());
        assertTrue(resultat.errors().getFirst().reason().contains("disque plein"));
    }

    // ===== jeu d'essai =====

    private MultipartFile fichier(String contenu) {
        return new MockMultipartFile("file", "inventaire.csv", "text/csv",
            contenu.getBytes(StandardCharsets.UTF_8));
    }

    /** Une ligne d'inventaire dont le produit porte un seul code CIP, celui de son fournisseur principal. */
    private StoreInventoryLine ligne(String codeCip, int prixAchat, int prixVente) {
        Produit produit = new Produit();
        produit.setId(Integer.parseInt(codeCip.substring(codeCip.length() - 1)));
        produit.setCostAmount(prixAchat);
        produit.setRegularUnitPrice(prixVente);

        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setCodeCip(codeCip);
        fournisseurProduit.setPrixAchat(prixAchat);
        fournisseurProduit.setPrixUni(prixVente);
        fournisseurProduit.setProduit(produit);
        produit.setFournisseurProduits(Set.of(fournisseurProduit));
        produit.setFournisseurProduitPrincipal(fournisseurProduit);

        StoreInventory inventaire = new StoreInventory();
        inventaire.setId(INVENTAIRE);
        inventaire.setInventoryCategory(InventoryCategory.MAGASIN);

        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setId((long) produit.getId());
        ligne.setProduit(produit);
        ligne.setStoreInventory(inventaire);
        return ligne;
    }
}

package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.inventaire.impl.InventaireQueryServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Les requêtes de comptage et de pagination des lignes d'inventaire ravalaient toute exception et
 * rendaient un résultat vide. Sur un écran de comptage, « 0 ligne » ne se lit pas comme une panne :
 * ça se lit « il n'y a rien à compter ». Le décompte et la liste échouant de la même façon,
 * l'écran restait cohérent — et faux, l'erreur ne vivant que dans les journaux.
 *
 * <p>Une requête en échec doit échouer visiblement.
 */
@DisplayName("Lignes d'inventaire — une requête en échec ne se déguise pas en liste vide")
class InventaireQueryErreurTest {

    private final EntityManager em = mock(EntityManager.class);
    private final StoreInventoryRepository storeInventoryRepository = mock(StoreInventoryRepository.class);

    private final InventaireQueryService service = new InventaireQueryServiceImpl(
        storeInventoryRepository,
        mock(InventoryStockService.class),
        mock(AppConfigurationService.class),
        em
    );

    @Test
    @DisplayName("l'échec de la requête remonte à l'appelant")
    void echecDeRequeteRemonte() {
        StoreInventory inventaire = new StoreInventory();
        inventaire.setStatut(InventoryStatut.PROCESSING);
        when(storeInventoryRepository.getReferenceById(1L)).thenReturn(inventaire);
        when(em.createNativeQuery(anyString())).thenThrow(new PersistenceException("colonne inconnue"));

        StoreInventoryLineFilterRecord filtre =
            new StoreInventoryLineFilterRecord(1L, null, null, null, null);

        assertThrows(PersistenceException.class,
            () -> service.getInventoryPage(filtre, PageRequest.of(0, 20), false));
    }
}

package com.kobe.warehouse.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Rafraîchir une vue matérialisée sans vider le cache de son rapport laissait servir l'ancien
 * résultat jusqu'à expiration — une heure. Au premier démarrage, c'était un résumé VIDE, calculé
 * sur la vue que Flyway venait de créer : la valorisation du stock affichait « 0 produits, marge
 * 0,00 % » une heure durant, alors que la vue était remplie depuis longtemps.
 */
@DisplayName("Vues matérialisées — le cache suit le rafraîchissement")
class MaterializedViewRefreshServiceCacheTest {

    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager("stockValuation", "topProducts");
    private MaterializedViewRefreshService service;

    @BeforeEach
    void preparer() {
        @SuppressWarnings("unchecked")
        ObjectProvider<CacheManager> fournisseur = mock(ObjectProvider.class);
        when(fournisseur.getIfAvailable()).thenReturn(caches);
        service = new MaterializedViewRefreshService(fournisseur);

        EntityManager em = mock(EntityManager.class);
        when(em.createNativeQuery(anyString())).thenReturn(mock(Query.class));
        ReflectionTestUtils.setField(service, "entityManager", em);

        caches.getCache("stockValuation").put("summary_1", "résumé périmé");
        caches.getCache("topProducts").put("top_1", "palmarès");
    }

    @AfterEach
    void nettoyer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("rafraîchir la valorisation vide son cache, et seulement le sien")
    void videLeCacheDeLaVue() {
        service.refreshView("mv_stock_valuation");

        assertThat(caches.getCache("stockValuation").get("summary_1")).isNull();
        assertThat(caches.getCache("topProducts").get("top_1")).isNotNull();
    }

    /**
     * Le nouveau contenu d'un REFRESH n'est visible qu'après le commit : vider avant laisserait
     * une lecture concurrente remettre l'ancien contenu en cache.
     */
    @Test
    @DisplayName("dans une transaction, le cache n'est vidé qu'au commit")
    void videApresLeCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.refreshView("mv_stock_valuation_by_rayon");
        assertThat(caches.getCache("stockValuation").get("summary_1")).isNotNull();

        List<TransactionSynchronization> synchronisations = TransactionSynchronizationManager.getSynchronizations();
        synchronisations.forEach(TransactionSynchronization::afterCommit);

        assertThat(caches.getCache("stockValuation").get("summary_1")).isNull();
    }

    @Test
    @DisplayName("une vue sans rapport en cache ne touche à rien")
    void vueSansCache() {
        service.refreshView("mv_produits_frequents_comptoir");

        assertThat(caches.getCache("stockValuation").get("summary_1")).isNotNull();
        assertThat(caches.getCache("topProducts").get("top_1")).isNotNull();
    }

    /**
     * Une vue mal orthographiée dans la correspondance ne lèverait aucune erreur : elle ne serait
     * jamais rafraîchie sous ce nom, et son cache ne serait donc jamais vidé.
     */
    @Test
    @DisplayName("chaque vue gérée qui a un cache pointe vers une vue réellement rafraîchie")
    void correspondanceCoherente() {
        assertThat(service.getAllManagedViews()).containsAll(MaterializedViewRefreshService.CACHES_PAR_VUE.keySet());
    }
}

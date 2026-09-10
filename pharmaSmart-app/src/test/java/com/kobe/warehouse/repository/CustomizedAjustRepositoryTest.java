package com.kobe.warehouse.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import com.kobe.warehouse.service.report.AjustementReportReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Couvre l'implementation de {@link AjustService} : l'interface elle-meme ne porte aucun code.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomizedAjustRepository (AjustService)")
class CustomizedAjustRepositoryTest {

    private static final int AJUST_ID = 12;

    @Mock
    private AjustementReportReportService ajustementReportService;

    @Mock
    private EntityManager em;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    @SuppressWarnings("rawtypes")
    private CriteriaQuery criteriaQuery;

    @Mock
    @SuppressWarnings("rawtypes")
    private Root root;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path path;

    @Mock
    @SuppressWarnings("rawtypes")
    private Join join;

    @Mock
    @SuppressWarnings("rawtypes")
    private SetJoin setJoin;

    @Mock
    private Predicate predicate;

    @Mock
    @SuppressWarnings("rawtypes")
    private TypedQuery typedQuery;

    private CustomizedAjustRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = new CustomizedAjustRepository(ajustementReportService);
        ReflectionTestUtils.setField(repository, "em", em);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(any(Class.class))).thenReturn(criteriaQuery);
        when(criteriaQuery.from(any(Class.class))).thenReturn(root);
        when(criteriaQuery.select(any(Selection.class))).thenReturn(criteriaQuery);
        when(criteriaQuery.distinct(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(criteriaQuery);
        when(criteriaQuery.orderBy(any(jakarta.persistence.criteria.Order[].class))).thenReturn(criteriaQuery);
        when(criteriaQuery.where(nullable(Predicate.class))).thenReturn(criteriaQuery);
        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(root.join(nullable(SingularAttribute.class))).thenReturn(join);
        when(path.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(join.join(nullable(SingularAttribute.class))).thenReturn(join);
        when(join.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(join.joinSet(nullable(String.class), any(JoinType.class))).thenReturn(setJoin);
        when(setJoin.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.equal(nullable(Expression.class), any(Object.class))).thenReturn(predicate);
        when(cb.like(nullable(Expression.class), anyString())).thenReturn(predicate);
        when(cb.upper(nullable(Expression.class))).thenReturn(path);
        when(cb.between(nullable(Expression.class), any(Comparable.class), any(Comparable.class))).thenReturn(predicate);
        when(cb.countDistinct(nullable(Expression.class))).thenReturn(path);
        when(cb.desc(nullable(Expression.class))).thenReturn(org.mockito.Mockito.mock(jakarta.persistence.criteria.Order.class));
        when(cb.function(anyString(), any(Class.class), any(Expression[].class))).thenReturn(path);
        when(em.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(em.createQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyInt(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(typedQuery.getSingleResult()).thenReturn(0L);
    }

    private static AppUser user() {
        AppUser user = new AppUser();
        user.setId(9);
        user.setFirstName("Awa");
        user.setLastName("KOUAME");
        return user;
    }

    private static Ajust ajust(AjustementStatut statut, Ajustement... ajustements) {
        Ajust ajust = new Ajust();
        ajust.setId(AJUST_ID);
        ajust.setStatut(statut);
        ajust.setDateMtv(LocalDateTime.of(2026, 4, 18, 10, 0));
        ajust.setCommentaire("casse en rayon");
        ajust.setUser(user());
        ajust.setAjustements(new ArrayList<>(List.of(ajustements)));
        return ajust;
    }

    private static Ajustement ajustement(Ajust ajust, String libelle, String codeCip) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip(codeCip);
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle(libelle);
        produit.setFournisseurProduitPrincipal(fp);
        Storage storage = new Storage();
        storage.setId(2);
        storage.setStorageType(StorageType.PRINCIPAL);
        StockProduit stockProduit = new StockProduit();
        stockProduit.setId(60);
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        Ajustement ajustement = new Ajustement();
        ajustement.setId(70);
        ajustement.setAjust(ajust);
        ajustement.setStockProduit(stockProduit);
        ajustement.setQtyMvt(-4);
        ajustement.setStockBefore(20);
        ajustement.setStockAfter(16);
        ajustement.setDateMtv(LocalDateTime.of(2026, 4, 18, 10, 0));
        return ajustement;
    }

    private static AjustementFilterRecord filtre(String search, Long userId, AjustType type) {
        return new AjustementFilterRecord(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30),
            userId,
            search,
            AjustementStatut.PENDING,
            type
        );
    }

    @Nested
    @DisplayName("loadAll")
    class LoadAll {

        @Test
        @DisplayName("renvoie une page vide sans interroger la liste quand rien ne correspond")
        void aucunResultat() {
            when(typedQuery.getSingleResult()).thenReturn(0L);

            Page<AjustDTO> page = repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            assertThat(page).isEmpty();
            assertThat(page.getTotalElements()).isZero();
            verify(typedQuery, never()).setFirstResult(anyInt());
        }

        @Test
        @DisplayName("un comptage nul en base est traite comme zero")
        void comptageNul() {
            when(typedQuery.getSingleResult()).thenReturn(null);

            assertThat(repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20)).getTotalElements()).isZero();
        }

        @Test
        @DisplayName("pagine la requete et mappe chaque ajustement avec son detail")
        void avecResultats() {
            Ajust ajust = ajust(AjustementStatut.PENDING);
            when(typedQuery.getSingleResult()).thenReturn(1L);
            when(typedQuery.getResultList()).thenReturn(List.of(ajust));

            Page<AjustDTO> page = repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            assertThat(page.getTotalElements()).isEqualTo(1L);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().getId()).isEqualTo(AJUST_ID);
            assertThat(page.getContent().getFirst().getCommentaire()).isEqualTo("casse en rayon");
            assertThat(page.getContent().getFirst().getUserFullName()).isNotBlank();
        }

        @Test
        @DisplayName("reporte l offset et la taille de page sur la requete")
        void pagination() {
            when(typedQuery.getSingleResult()).thenReturn(100L);
            when(typedQuery.getResultList()).thenReturn(List.of(ajust(AjustementStatut.PENDING)));

            repository.loadAll(filtre(null, null, null), PageRequest.of(2, 15));

            verify(typedQuery).setFirstResult(30);
            verify(typedQuery).setMaxResults(15);
        }

        @Test
        @DisplayName("le detail des lignes est charge par une requete dediee et mappe")
        void detailDesLignes() {
            Ajust ajust = ajust(AjustementStatut.PENDING);
            when(typedQuery.getSingleResult()).thenReturn(1L);
            when(typedQuery.getResultList())
                .thenReturn(List.of(ajust))
                .thenReturn(List.of(ajustement(ajust, "DOLIPRANE 1000MG", "1234567")));

            Page<AjustDTO> page = repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            List<AjustementDTO> lignes = page.getContent().getFirst().getAjustements();
            assertThat(lignes).hasSize(1);
            assertThat(lignes.getFirst().getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(lignes.getFirst().getCodeCip()).isEqualTo("1234567");
        }

        @Test
        @DisplayName("une erreur sur le chargement du detail laisse une liste vide")
        void detailEnErreur() {
            Ajust ajust = ajust(AjustementStatut.PENDING);
            when(typedQuery.getSingleResult()).thenReturn(1L);
            when(typedQuery.getResultList()).thenReturn(List.of(ajust)).thenThrow(new IllegalStateException("session fermee"));

            Page<AjustDTO> page = repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            assertThat(page.getContent().getFirst().getAjustements()).isEmpty();
        }
    }

    @Nested
    @DisplayName("predicats de filtrage")
    class PredicatsDeFiltrage {

        @ParameterizedTest(name = "une recherche [{0}] n ajoute aucun predicat de texte")
        @NullAndEmptySource
        void rechercheVide(String search) {
            repository.loadAll(filtre(search, null, null), PageRequest.of(0, 20));

            verify(cb, never()).like(nullable(Expression.class), anyString());
        }

        @Test
        @DisplayName("cherche sur code CIP, libelle, EAN laboratoire et EAN fournisseur")
        void rechercheLibre() {
            repository.loadAll(filtre("DOLI", null, null), PageRequest.of(0, 20));

            verify(cb, org.mockito.Mockito.times(4)).like(nullable(Expression.class), org.mockito.ArgumentMatchers.eq("DOLI%"));
            verify(cb).or(any(Predicate[].class));
        }

        @Test
        @DisplayName("filtre sur l utilisateur")
        void filtreSurUtilisateur() {
            repository.loadAll(filtre(null, 9L, null), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(9L));
        }

        @Test
        @DisplayName("aucun filtre d utilisateur quand il n est pas transmis")
        void sansUtilisateur() {
            repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            verify(cb, never()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(9L));
        }

        @Test
        @DisplayName("filtre toujours sur le statut demande")
        void filtreSurLeStatut() {
            repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(AjustementStatut.PENDING));
        }

        @Test
        @DisplayName("filtre sur le type de mouvement")
        void filtreSurLeType() {
            repository.loadAll(filtre(null, null, AjustType.AJUSTEMENT_OUT), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(AjustType.AJUSTEMENT_OUT));
        }

        @Test
        @DisplayName("aucun filtre de type quand il n est pas transmis")
        void sansType() {
            repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            verify(cb, never()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(AjustType.AJUSTEMENT_OUT));
        }

        @Test
        @DisplayName("borne toujours la periode sur la date du mouvement")
        void bornesDePeriode() {
            repository.loadAll(filtre(null, null, null), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).between(
                nullable(Expression.class),
                org.mockito.ArgumentMatchers.eq(java.sql.Date.valueOf(LocalDate.of(2026, 4, 1))),
                org.mockito.ArgumentMatchers.eq(java.sql.Date.valueOf(LocalDate.of(2026, 4, 30)))
            );
        }
    }

    @Nested
    @DisplayName("exportToPdf")
    class ExportToPdf {

        @Test
        @DisplayName("charge l ajustement et delegue au service de rapport")
        void exporte() {
            Ajust ajust = ajust(AjustementStatut.CLOSED);
            when(em.find(Ajust.class, AJUST_ID)).thenReturn(ajust);
            when(ajustementReportService.export(ajust)).thenReturn(new byte[] { 7 });

            assertThat(repository.exportToPdf(AJUST_ID)).containsExactly(7);
        }
    }

    @Nested
    @DisplayName("getOneById")
    class GetOneById {

        @Test
        @DisplayName("un ajustement deja cloture n est plus modifiable")
        void ajustementCloture() {
            when(em.find(Ajust.class, AJUST_ID)).thenReturn(ajust(AjustementStatut.CLOSED));

            assertThat(repository.getOneById(AJUST_ID)).isEmpty();

            verifyNoInteractions(ajustementReportService);
        }

        @Test
        @DisplayName("un ajustement en brouillon remonte avec ses lignes triees par code CIP")
        void ajustementEnBrouillon() {
            Ajust ajust = ajust(AjustementStatut.PENDING);
            ajust.getAjustements().add(ajustement(ajust, "EFFERALGAN", "7654321"));
            ajust.getAjustements().add(ajustement(ajust, "DOLIPRANE 1000MG", "1234567"));
            when(em.find(Ajust.class, AJUST_ID)).thenReturn(ajust);

            Optional<AjustDTO> resultat = repository.getOneById(AJUST_ID);

            assertThat(resultat).isPresent();
            assertThat(resultat.orElseThrow().getId()).isEqualTo(AJUST_ID);
            assertThat(resultat.orElseThrow().getAjustements())
                .extracting(AjustementDTO::getCodeCip)
                .containsExactly("1234567", "7654321");
        }

        @Test
        @DisplayName("un ajustement en brouillon sans ligne remonte une liste vide")
        void ajustementSansLigne() {
            when(em.find(Ajust.class, AJUST_ID)).thenReturn(ajust(AjustementStatut.PENDING));

            assertThat(repository.getOneById(AJUST_ID)).get().extracting(AjustDTO::getAjustements).asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.LIST
            ).isEmpty();
        }
    }
}

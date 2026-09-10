package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockReapproServiceImpl")
class StockReapproServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    @SuppressWarnings("rawtypes")
    private CriteriaUpdate criteriaUpdate;

    @Mock
    @SuppressWarnings("rawtypes")
    private Root root;

    @Mock
    private Query query;

    @Mock
    private Path<Object> idPath;

    @Mock
    private Predicate predicate;

    private ObjectMapper objectMapper;
    private StockReapproServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new StockReapproServiceImpl(entityManager, appConfigurationService, transactionTemplate, salesRepository, objectMapper);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createCriteriaUpdate(Produit.class)).thenReturn(criteriaUpdate);
        when(criteriaUpdate.from(Produit.class)).thenReturn(root);
        when(criteriaUpdate.set(nullable(SingularAttribute.class), any())).thenReturn(criteriaUpdate);
        when(root.get(nullable(SingularAttribute.class))).thenReturn(idPath);
        when(criteriaBuilder.equal(nullable(jakarta.persistence.criteria.Expression.class), any(Object.class))).thenReturn(predicate);
        when(criteriaUpdate.where(nullable(Predicate.class))).thenReturn(criteriaUpdate);
        when(entityManager.createQuery(any(CriteriaUpdate.class))).thenReturn(query);

        // Le template execute reellement le callback pour que la mise a jour soit observable.
        when(transactionTemplate.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    private static AppConfiguration configuration(String value) {
        AppConfiguration c = new AppConfiguration();
        c.setValue(value);
        return c;
    }

    private void stubModel(String value) {
        when(appConfigurationService.findOneById(EntityConstant.APP_MODEL_REAPPRO))
            .thenReturn(value == null ? Optional.empty() : Optional.of(configuration(value)));
    }

    private void stubLastReapproDate(String value) {
        when(appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO)).thenReturn(Optional.of(configuration(value)));
    }

    private void stubReapproParameters(String dayStock, String nbLimit, String denominateur) {
        when(appConfigurationService.findOneById(EntityConstant.APP_DAY_STOCK))
            .thenReturn(dayStock == null ? Optional.empty() : Optional.of(configuration(dayStock)));
        when(appConfigurationService.findOneById(EntityConstant.APP_LIMIT_NBR_DAY_REAPPRO))
            .thenReturn(nbLimit == null ? Optional.empty() : Optional.of(configuration(nbLimit)));
        when(appConfigurationService.findOneById(EntityConstant.APP_DENOMINATEUR_REAPPRO))
            .thenReturn(denominateur == null ? Optional.empty() : Optional.of(configuration(denominateur)));
    }

    private void stubVentes(String json) {
        when(salesRepository.fetchProductQuantitySold(any(LocalDate.class), any(LocalDate.class))).thenReturn(json);
    }

    /** Relit les valeurs poussees dans le CriteriaUpdate : [qtyAppro, qtySeuilMini]. */
    @SuppressWarnings("unchecked")
    private int[] capturerMiseAJour() {
        ArgumentCaptor<Object> valeurs = ArgumentCaptor.forClass(Object.class);
        verify(criteriaUpdate, times(3)).set(nullable(SingularAttribute.class), valeurs.capture());
        return new int[] { (int) valeurs.getAllValues().get(0), (int) valeurs.getAllValues().get(1) };
    }

    @Nested
    @DisplayName("choix du modele")
    class ChoixDuModele {

        @Test
        @DisplayName("le modele SEMOIS n active pas le calcul classique")
        void modeleSemois() {
            stubModel("SEMOIS");

            service.computeStockReapprovisionnement();

            verify(appConfigurationService, never()).findOneById(EntityConstant.APP_LAST_DAY_REAPPRO);
            verifyNoInteractions(salesRepository);
        }

        @Test
        @DisplayName("le modele CLASSIQUE active le calcul")
        void modeleClassique() {
            stubModel("CLASSIQUE");
            stubLastReapproDate(null);
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(salesRepository).fetchProductQuantitySold(any(), any());
        }

        @ParameterizedTest(name = "une valeur [{0}] retombe sur le modele CLASSIQUE")
        @ValueSource(strings = { "", "PAS_UN_MODELE" })
        void valeurInvalideRetombeSurClassique(String value) {
            stubModel(value);
            stubLastReapproDate(null);
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(salesRepository).fetchProductQuantitySold(any(), any());
        }

        @ParameterizedTest(name = "l absence de configuration retombe sur le modele CLASSIQUE")
        @NullSource
        void absenceDeConfigurationRetombeSurClassique(String value) {
            stubModel(value);
            stubLastReapproDate(null);
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(salesRepository).fetchProductQuantitySold(any(), any());
        }
    }

    @Nested
    @DisplayName("declenchement mensuel")
    class DeclenchementMensuel {

        @BeforeEach
        void modeleClassique() {
            stubModel("CLASSIQUE");
        }

        @Test
        @DisplayName("sans date de dernier calcul, le calcul est lance")
        void sansDateDeDernierCalcul() {
            stubLastReapproDate(null);
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(salesRepository).fetchProductQuantitySold(any(), any());
        }

        @Test
        @DisplayName("un calcul deja fait ce mois-ci n est pas rejoue")
        void dejaCalculeCeMois() {
            stubLastReapproDate(LocalDate.now().withDayOfMonth(1).toString());

            service.computeStockReapprovisionnement();

            verifyNoInteractions(salesRepository);
            verify(appConfigurationService, never()).update(any(AppConfiguration.class));
        }

        @Test
        @DisplayName("un calcul datant d un autre mois est rejoue")
        void calculeUnAutreMois() {
            stubLastReapproDate(LocalDate.now().minusMonths(1).withDayOfMonth(1).toString());
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(salesRepository).fetchProductQuantitySold(any(), any());
        }

        @Test
        @DisplayName("aucun calcul quand le parametre de derniere execution n existe pas")
        void parametreAbsent() {
            when(appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO)).thenReturn(Optional.empty());

            service.computeStockReapprovisionnement();

            verifyNoInteractions(salesRepository);
        }

        @Test
        @DisplayName("horodate et enregistre la date de calcul apres coup")
        void horodateApresCalcul() {
            AppConfiguration lastRun = configuration(null);
            when(appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO)).thenReturn(Optional.of(lastRun));
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            assertThat(lastRun.getValue()).isEqualTo(LocalDate.now().toString());
            assertThat(lastRun.getUpdated()).isNotNull();
            verify(appConfigurationService).update(lastRun);
        }
    }

    @Nested
    @DisplayName("calcul des seuils")
    class CalculDesSeuils {

        @BeforeEach
        void modeleClassiqueSansDate() {
            stubModel("CLASSIQUE");
            stubLastReapproDate(null);
        }

        @Test
        @DisplayName("applique les parametres par defaut quand rien n est configure")
        void parametresParDefaut() {
            stubReapproParameters(null, null, null);
            // 840 vendus / 84 = 10 par jour -> seuil 10*10 = 100, reappro 10*8 = 80
            stubVentes("[{\"id\":1,\"itemQty\":1,\"itemQtySold\":0,\"qtySold\":840}]");

            service.computeStockReapprovisionnement();

            int[] maj = capturerMiseAJour();
            assertThat(maj[0]).isEqualTo(80);
            assertThat(maj[1]).isEqualTo(100);
        }

        @Test
        @DisplayName("applique les parametres configures")
        void parametresConfigures() {
            stubReapproParameters("5", "2", "10");
            // 100 vendus / 10 = 10 par jour -> seuil 10*5 = 50, reappro 10*2 = 20
            stubVentes("[{\"id\":1,\"itemQty\":1,\"itemQtySold\":0,\"qtySold\":100}]");

            service.computeStockReapprovisionnement();

            int[] maj = capturerMiseAJour();
            assertThat(maj[0]).isEqualTo(20);
            assertThat(maj[1]).isEqualTo(50);
        }

        @Test
        @DisplayName("integre les ventes au detail arrondies a l unite superieure")
        void integreLesVentesAuDetail() {
            stubReapproParameters("1", "1", "1");
            // 10 + ceil(5/2) = 13 par jour
            stubVentes("[{\"id\":1,\"itemQty\":2,\"itemQtySold\":5,\"qtySold\":10}]");

            service.computeStockReapprovisionnement();

            int[] maj = capturerMiseAJour();
            assertThat(maj[0]).isEqualTo(13);
            assertThat(maj[1]).isEqualTo(13);
        }

        @Test
        @DisplayName("ignore le detail quand aucune unite n a ete vendue au detail")
        void sansVenteAuDetail() {
            stubReapproParameters("1", "1", "1");
            stubVentes("[{\"id\":1,\"itemQty\":2,\"itemQtySold\":0,\"qtySold\":10}]");

            service.computeStockReapprovisionnement();

            assertThat(capturerMiseAJour()[0]).isEqualTo(10);
        }

        @Test
        @DisplayName("arrondit les moyennes fractionnaires a l unite superieure")
        void arrondiSuperieur() {
            stubReapproParameters("1", "1", "4");
            // 10 / 4 = 2.5 -> ceil = 3
            stubVentes("[{\"id\":1,\"itemQty\":1,\"itemQtySold\":0,\"qtySold\":10}]");

            service.computeStockReapprovisionnement();

            assertThat(capturerMiseAJour()[0]).isEqualTo(3);
        }

        @Test
        @DisplayName("met a jour chaque produit vendu")
        void metAJourChaqueProduit() {
            stubReapproParameters("1", "1", "1");
            stubVentes("[{\"id\":1,\"itemQty\":1,\"itemQtySold\":0,\"qtySold\":10}," +
                "{\"id\":2,\"itemQty\":1,\"itemQtySold\":0,\"qtySold\":20}]");

            service.computeStockReapprovisionnement();

            verify(query, times(2)).executeUpdate();
        }

        @Test
        @DisplayName("aucune vente : aucune mise a jour")
        void aucuneVente() {
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            verify(query, never()).executeUpdate();
        }

        @Test
        @DisplayName("une charge utile illisible n interrompt pas le traitement")
        void chargeUtileIllisible() {
            stubReapproParameters(null, null, null);
            stubVentes("ceci n est pas du json");

            service.computeStockReapprovisionnement();

            verify(query, never()).executeUpdate();
            verify(appConfigurationService).update(any(AppConfiguration.class));
        }

        @Test
        @DisplayName("une charge utile nulle n interrompt pas le traitement")
        void chargeUtileNulle() {
            stubReapproParameters(null, null, null);
            stubVentes(null);

            service.computeStockReapprovisionnement();

            verify(query, never()).executeUpdate();
        }

        @Test
        @DisplayName("interroge les ventes sur la fenetre glissante des trois derniers mois")
        void fenetreDeTroisMois() {
            stubReapproParameters(null, null, null);
            stubVentes("[]");

            service.computeStockReapprovisionnement();

            ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
            verify(salesRepository).fetchProductQuantitySold(from.capture(), to.capture());
            assertThat(from.getValue()).isBefore(to.getValue());
        }
    }
}

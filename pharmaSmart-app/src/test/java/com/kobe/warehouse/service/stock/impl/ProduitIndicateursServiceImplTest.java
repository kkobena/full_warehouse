package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.repository.ParetoAnalysisRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.dto.ProduitIndicateursDTO;
import com.kobe.warehouse.service.dto.VenteMoisDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProduitIndicateursServiceImpl")
class ProduitIndicateursServiceImplTest {

    private static final Integer PRODUIT_ID = 4242;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ParetoAnalysisRepository paretoAnalysisRepository;

    @Mock
    private VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository;

    @Mock(name = "produitQuery")
    private Query produitQuery;

    @Mock(name = "rotationQuery")
    private Query rotationQuery;

    private ProduitIndicateursServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProduitIndicateursServiceImpl(paretoAnalysisRepository, ventesMensuellesAgregeesRepository);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        when(entityManager.createNativeQuery(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            return sql.contains("v_stock_rotation") ? rotationQuery : produitQuery;
        });
        when(produitQuery.setParameter(eq("produitId"), org.mockito.ArgumentMatchers.any())).thenReturn(produitQuery);
        when(rotationQuery.setParameter(eq("produitId"), org.mockito.ArgumentMatchers.any())).thenReturn(rotationQuery);
        when(paretoAnalysisRepository.findByProduitId(PRODUIT_ID)).thenReturn(Optional.empty());
        produitRow(new Object[] { "B", Boolean.FALSE, Boolean.FALSE, null, null });
        rotationRows(List.of());
    }

    private void produitRow(Object... row) {
        when(produitQuery.getResultList()).thenReturn(List.of((Object) row));
    }

    private void rotationRows(List<Object[]> rows) {
        when(rotationQuery.getResultList()).thenReturn(rows);
    }

    private static VentesMensuellesAgregees vente(String anneeMois, int qte, int ca, int nb) {
        VentesMensuellesAgregees v = new VentesMensuellesAgregees();
        v.setAnneeMois(anneeMois);
        v.setQuantiteVendue(qte);
        v.setMontantCa(ca);
        v.setNombreVentes(nb);
        return v;
    }

    @Nested
    @DisplayName("getIndicateurs")
    class GetIndicateurs {

        @Test
        @DisplayName("renvoie vide quand le produit n existe pas")
        void produitInexistant() {
            when(produitQuery.getResultList()).thenReturn(List.of());

            assertThat(service.getIndicateurs(PRODUIT_ID)).isEmpty();
        }

        @ParameterizedTest(name = "classe_criticite=[{0}] devient {1}")
        @CsvSource({ "A+, A_PLUS", "A_PLUS, A_PLUS", "APLUS, A_PLUS", "a, A", "B, B", "c, C", "D, D" })
        void mappeLaClasseDeCriticite(String stocke, ClasseCriticite attendue) {
            produitRow(new Object[] { stocke, null, null, null, null });

            assertThat(service.getIndicateurs(PRODUIT_ID))
                .get()
                .extracting(ProduitIndicateursDTO::classeCriticite)
                .isEqualTo(attendue);
        }

        @ParameterizedTest(name = "classe_criticite=[{0}] retombe sur B")
        @ValueSource(strings = { "Z", "inconnue", "" })
        void classeInvalideRetombeSurB(String stocke) {
            produitRow(new Object[] { stocke, null, null, null, null });

            assertThat(service.getIndicateurs(PRODUIT_ID))
                .get()
                .extracting(ProduitIndicateursDTO::classeCriticite)
                .isEqualTo(ClasseCriticite.B);
        }

        @ParameterizedTest(name = "classe_criticite nulle retombe sur B")
        @NullSource
        void classeNulleRetombeSurB(String stocke) {
            produitRow(new Object[] { stocke, null, null, null, null });

            assertThat(service.getIndicateurs(PRODUIT_ID))
                .get()
                .extracting(ProduitIndicateursDTO::classeCriticite)
                .isEqualTo(ClasseCriticite.B);
        }

        @Test
        @DisplayName("remonte les badges reglementaires")
        void badgesReglementaires() {
            produitRow(new Object[] { "A", Boolean.TRUE, Boolean.TRUE, null, null });

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.produitId()).isEqualTo(PRODUIT_ID);
            assertThat(dto.estMedicamentEssentiel()).isTrue();
            assertThat(dto.estProduitGarde()).isTrue();
        }

        @Test
        @DisplayName("des badges nuls valent faux")
        void badgesNuls() {
            produitRow(new Object[] { "A", null, null, null, null });

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.estMedicamentEssentiel()).isFalse();
            assertThat(dto.estProduitGarde()).isFalse();
        }

        @Test
        @DisplayName("calcule le taux de marge en pourcentage arrondi au centieme")
        void tauxDeMarge() {
            produitRow(new Object[] { "A", null, null, 1500, 1000 });

            assertThat(service.getIndicateurs(PRODUIT_ID).orElseThrow().tauxMarge())
                .isEqualByComparingTo(new BigDecimal("33.33"));
        }

        @Test
        @DisplayName("taux de marge negatif quand le produit est vendu a perte")
        void margeNegative() {
            produitRow(new Object[] { "A", null, null, 800, 1000 });

            assertThat(service.getIndicateurs(PRODUIT_ID).orElseThrow().tauxMarge())
                .isEqualByComparingTo(new BigDecimal("-25.00"));
        }

        @Test
        @DisplayName("taux de marge nul quand le prix de vente est absent")
        void margeSansPrixDeVente() {
            produitRow(new Object[] { "A", null, null, null, 1000 });

            assertThat(service.getIndicateurs(PRODUIT_ID).orElseThrow().tauxMarge()).isNull();
        }

        @Test
        @DisplayName("taux de marge nul quand le prix d achat est absent")
        void margeSansPrixDAchat() {
            produitRow(new Object[] { "A", null, null, 1500, null });

            assertThat(service.getIndicateurs(PRODUIT_ID).orElseThrow().tauxMarge()).isNull();
        }

        @Test
        @DisplayName("taux de marge nul quand le prix de vente est zero")
        void margeAvecPrixDeVenteZero() {
            produitRow(new Object[] { "A", null, null, 0, 1000 });

            assertThat(service.getIndicateurs(PRODUIT_ID).orElseThrow().tauxMarge()).isNull();
        }

        @Test
        @DisplayName("valeurs de rotation par defaut quand la vue ne renvoie rien")
        void rotationAbsente() {
            rotationRows(List.of());

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.cmm()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.rotationAnnuelleQte()).isNull();
            assertThat(dto.couvertureStockJours()).isNull();
            assertThat(dto.ca30Jours()).isZero();
            assertThat(dto.ca12Mois()).isZero();
            assertThat(dto.qteVendue12Mois()).isZero();
        }

        @Test
        @DisplayName("remonte les indicateurs de rotation")
        void rotationPresente() {
            rotationRows(List.<Object[]>of(new Object[] { new BigDecimal("12.5"), 150, 45, 30000, 360000, 180 }));

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.cmm()).isEqualByComparingTo(new BigDecimal("12.5"));
            assertThat(dto.rotationAnnuelleQte()).isEqualByComparingTo(new BigDecimal("150"));
            assertThat(dto.couvertureStockJours()).isEqualTo(45);
            assertThat(dto.ca30Jours()).isEqualTo(30000);
            assertThat(dto.ca12Mois()).isEqualTo(360000);
            assertThat(dto.qteVendue12Mois()).isEqualTo(180);
        }

        @Test
        @DisplayName("une ligne de rotation entierement nulle retombe sur les defauts")
        void rotationColonnesNulles() {
            rotationRows(List.<Object[]>of(new Object[] { null, null, null, null, null, null }));

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.cmm()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.rotationAnnuelleQte()).isNull();
            assertThat(dto.couvertureStockJours()).isNull();
            assertThat(dto.ca30Jours()).isZero();
            assertThat(dto.ca12Mois()).isZero();
            assertThat(dto.qteVendue12Mois()).isZero();
        }

        @Test
        @DisplayName("pas de score Pareto quand le produit n est pas classe")
        void paretoAbsent() {
            when(paretoAnalysisRepository.findByProduitId(PRODUIT_ID)).thenReturn(Optional.empty());

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.rang()).isNull();
            assertThat(dto.caCumulePct()).isNull();
            assertThat(dto.frequenceMois()).isNull();
        }

        @Test
        @DisplayName("remonte le score Pareto")
        void paretoPresent() {
            when(paretoAnalysisRepository.findByProduitId(PRODUIT_ID))
                .thenReturn(Optional.of(new Object[] { PRODUIT_ID, new BigDecimal("18.40"), 7, "A", 11 }));

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.caCumulePct()).isEqualByComparingTo(new BigDecimal("18.40"));
            assertThat(dto.rang()).isEqualTo(7);
            assertThat(dto.frequenceMois()).isEqualTo(11);
        }

        @Test
        @DisplayName("une ligne Pareto aux colonnes nulles ne remplit rien")
        void paretoColonnesNulles() {
            when(paretoAnalysisRepository.findByProduitId(PRODUIT_ID))
                .thenReturn(Optional.of(new Object[] { PRODUIT_ID, null, null, null, null }));

            ProduitIndicateursDTO dto = service.getIndicateurs(PRODUIT_ID).orElseThrow();

            assertThat(dto.caCumulePct()).isNull();
            assertThat(dto.rang()).isNull();
            assertThat(dto.frequenceMois()).isNull();
        }
    }

    @Nested
    @DisplayName("getVentesMensuelles")
    class GetVentesMensuelles {

        @Test
        @DisplayName("renvoie une liste vide quand il n y a pas d historique")
        void aucunHistorique() {
            when(ventesMensuellesAgregeesRepository.findLastNMonthsByProduit(PRODUIT_ID, 12)).thenReturn(List.of());

            assertThat(service.getVentesMensuelles(PRODUIT_ID, 12)).isEmpty();
        }

        @Test
        @DisplayName("reordonne du plus ancien au plus recent pour Chart.js")
        void ordreCroissant() {
            when(ventesMensuellesAgregeesRepository.findLastNMonthsByProduit(PRODUIT_ID, 3))
                .thenReturn(List.of(vente("2026-03", 3, 300, 3), vente("2026-01", 1, 100, 1), vente("2026-02", 2, 200, 2)));

            List<VenteMoisDTO> result = service.getVentesMensuelles(PRODUIT_ID, 3);

            assertThat(result).extracting(VenteMoisDTO::anneeMois).containsExactly("2026-01", "2026-02", "2026-03");
        }

        @Test
        @DisplayName("mappe chaque colonne de l agregat")
        void mappeLesColonnes() {
            when(ventesMensuellesAgregeesRepository.findLastNMonthsByProduit(PRODUIT_ID, 1))
                .thenReturn(List.of(vente("2026-05", 42, 84000, 17)));

            VenteMoisDTO dto = service.getVentesMensuelles(PRODUIT_ID, 1).getFirst();

            assertThat(dto.anneeMois()).isEqualTo("2026-05");
            assertThat(dto.quantiteVendue()).isEqualTo(42);
            assertThat(dto.montantCa()).isEqualTo(84000);
            assertThat(dto.nombreVentes()).isEqualTo(17);
        }
    }
}

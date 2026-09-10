package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.InventoryGapAnalysis;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.CauseEcart;
import com.kobe.warehouse.repository.InventoryGapAnalysisRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GapAnalysisServiceImpl")
class GapAnalysisServiceImplTest {

    private static final Long INVENTORY_ID = 55L;

    @Mock
    private InventoryGapAnalysisRepository gapRepo;

    @Mock
    private StoreInventoryLineRepository lineRepo;

    private GapAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GapAnalysisServiceImpl(gapRepo, lineRepo);
    }

    private static StoreInventoryLine line(long id, Integer gap) {
        StoreInventoryLine line = new StoreInventoryLine();
        line.setId(id);
        line.setGap(gap);
        return line;
    }

    private static InventoryGapAnalysis analysis(StoreInventoryLine line) {
        InventoryGapAnalysis ga = new InventoryGapAnalysis();
        ga.setId(900L);
        ga.setStoreInventoryLine(line);
        return ga;
    }

    @Nested
    @DisplayName("getLinesWithGap")
    class GetLinesWithGap {

        @Test
        @DisplayName("delegue au repository et renvoie sa page")
        void delegue() {
            Page<GapLineRecord> expected = new PageImpl<>(
                List.of(new GapLineRecord(1L, "DOLIPRANE", 10, 8, -2, -200, CauseEcart.CASSE, "carton ecrase"))
            );
            when(lineRepo.findLinesWithGap(any(), any(Pageable.class))).thenReturn(expected);

            Page<GapLineRecord> result = service.getLinesWithGap(INVENTORY_ID, PageRequest.of(1, 25));

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("neutralise le tri client pour ne pas casser la pagination")
        void neutraliseLeTriClient() {
            when(lineRepo.findLinesWithGap(any(), any(Pageable.class))).thenReturn(Page.empty());

            service.getLinesWithGap(INVENTORY_ID, PageRequest.of(2, 30, Sort.by("produitLibelle")));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(lineRepo).findLinesWithGap(any(), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(30);
            assertThat(captor.getValue().getSort().isSorted()).isFalse();
        }
    }

    @Nested
    @DisplayName("saveAnalysis")
    class SaveAnalysis {

        @ParameterizedTest(name = "entries={0} : sort sans toucher au depot")
        @NullAndEmptySource
        void entreesVidesOuNulles(List<GapEntryRecord> entries) {
            service.saveAnalysis(INVENTORY_ID, entries);

            verifyNoInteractions(lineRepo, gapRepo);
        }

        @Test
        @DisplayName("sort quand aucune ligne soumise n appartient a l inventaire")
        void aucuneLigneRechargee() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "CASSE", null)));

            verifyNoInteractions(gapRepo);
        }

        @Test
        @DisplayName("filtre les identifiants de ligne nuls avant le rechargement")
        void filtreLesIdentifiantsNuls() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, -3)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(
                INVENTORY_ID,
                List.of(new GapEntryRecord(null, "CASSE", null), new GapEntryRecord(1L, "CASSE", null))
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(lineRepo).findAllByIdInAndInventoryId(captor.capture(), any());
            assertThat(captor.getValue()).containsExactly(1L);
        }

        @Test
        @DisplayName("cree une qualification et normalise la quantite en valeur absolue")
        void creeUneQualification() {
            StoreInventoryLine l1 = line(1L, -7);
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(l1));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "VOL", "coup de chaleur")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            InventoryGapAnalysis saved = captor.getValue().getFirst();
            assertThat(saved.getId()).isNull();
            assertThat(saved.getStoreInventoryLine()).isSameAs(l1);
            assertThat(saved.getCause()).isEqualTo(CauseEcart.VOL);
            assertThat(saved.getQuantity()).isEqualTo(7);
            assertThat(saved.getCommentaire()).isEqualTo("coup de chaleur");
            verify(gapRepo, never()).deleteAllByStoreInventoryLineIdIn(anyCollection());
        }

        @Test
        @DisplayName("reutilise la qualification existante au lieu de la recreer")
        void reutiliseLaQualificationExistante() {
            StoreInventoryLine l1 = line(1L, 4);
            InventoryGapAnalysis existing = analysis(l1);
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(l1));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of(existing));

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "PEREMPTION", "perimes")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).containsExactly(existing);
            assertThat(existing.getId()).isEqualTo(900L);
            assertThat(existing.getCause()).isEqualTo(CauseEcart.PEREMPTION);
        }

        @Test
        @DisplayName("ne garde qu une qualification par ligne en cas de doublon en base")
        void doublonEnBase() {
            StoreInventoryLine l1 = line(1L, 4);
            InventoryGapAnalysis first = analysis(l1);
            InventoryGapAnalysis second = analysis(l1);
            second.setId(901L);
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(l1));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of(first, second));

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "CASSE", null)));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).containsExactly(first);
        }

        @Test
        @DisplayName("un gap nul sur la ligne se qualifie a zero")
        void gapNul() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, null)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "INCONNU", null)));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            assertThat(captor.getValue().getFirst().getQuantity()).isZero();
        }

        @ParameterizedTest(name = "cause=[{0}] efface la qualification")
        @ValueSource(strings = { "", "   " })
        void causeBlancheEfface(String cause) {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, -2)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, cause, null)));

            verify(gapRepo).deleteAllByStoreInventoryLineIdIn(List.of(1L));
            verify(gapRepo, never()).saveAll(any());
        }

        @Test
        @DisplayName("cause nulle efface la qualification")
        void causeNulleEfface() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, -2)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, null, null)));

            verify(gapRepo).deleteAllByStoreInventoryLineIdIn(List.of(1L));
            verify(gapRepo, never()).saveAll(any());
        }

        @Test
        @DisplayName("ignore une entree dont la ligne n a pas ete rechargee")
        void ignoreLigneHorsInventaire() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, -2)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(
                INVENTORY_ID,
                List.of(new GapEntryRecord(1L, "CASSE", null), new GapEntryRecord(999L, "VOL", null))
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("melange suppression et enregistrement dans le meme lot")
        void melangeEffacementEtEnregistrement() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong()))
                .thenReturn(List.of(line(1L, -2), line(2L, 5)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            service.saveAnalysis(
                INVENTORY_ID,
                List.of(new GapEntryRecord(1L, null, null), new GapEntryRecord(2L, "ERREUR_SAISIE", null))
            );

            verify(gapRepo).deleteAllByStoreInventoryLineIdIn(List.of(1L));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InventoryGapAnalysis>> captor = ArgumentCaptor.forClass(List.class);
            verify(gapRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().getFirst().getCause()).isEqualTo(CauseEcart.ERREUR_SAISIE);
        }

        @Test
        @DisplayName("une cause inconnue de l enumeration remonte en erreur")
        void causeInvalide() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of(line(1L, -2)));
            when(gapRepo.findAllByStoreInventoryLineIdIn(anyCollection())).thenReturn(List.of());

            List<GapEntryRecord> entries = List.of(new GapEntryRecord(1L, "PAS_UNE_CAUSE", null));

            assertThatThrownBy(() -> service.saveAnalysis(INVENTORY_ID, entries))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("restreint le rechargement a l inventaire cible")
        void restreintAlInventaire() {
            when(lineRepo.findAllByIdInAndInventoryId(anyCollection(), anyLong())).thenReturn(List.of());

            service.saveAnalysis(INVENTORY_ID, List.of(new GapEntryRecord(1L, "CASSE", null)));

            verify(lineRepo).findAllByIdInAndInventoryId(Set.of(1L), INVENTORY_ID);
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @ParameterizedTest(name = "{0} est libelle {1}")
        @CsvSource(
            {
                "CASSE, Casse / dommage",
                "VOL, Vol",
                "ERREUR_RECEPTION, Erreur de réception",
                "ERREUR_SAISIE, Erreur de saisie",
                "PEREMPTION, Péremption",
                "INCONNU, Cause inconnue",
            }
        )
        void libelleChaqueCause(String cause, String libelle) {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[] { CauseEcart.valueOf(cause), 3L, 12L });
            when(gapRepo.aggregateByInventoryId(INVENTORY_ID)).thenReturn(rows);

            List<GapSummaryRecord> summary = service.getSummary(INVENTORY_ID);

            assertThat(summary).hasSize(1);
            assertThat(summary.getFirst().cause()).isEqualTo(cause);
            assertThat(summary.getFirst().causeLabel()).isEqualTo(libelle);
            assertThat(summary.getFirst().nbProduits()).isEqualTo(3L);
            assertThat(summary.getFirst().quantiteTotale()).isEqualTo(12L);
        }

        @Test
        @DisplayName("renvoie une liste vide quand rien n est qualifie")
        void aucuneQualification() {
            when(gapRepo.aggregateByInventoryId(INVENTORY_ID)).thenReturn(List.of());

            assertThat(service.getSummary(INVENTORY_ID)).isEmpty();
        }

        @Test
        @DisplayName("agrege plusieurs causes")
        void plusieursCauses() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[] { CauseEcart.CASSE, 1L, 2L });
            rows.add(new Object[] { CauseEcart.VOL, 4L, 9L });
            when(gapRepo.aggregateByInventoryId(INVENTORY_ID)).thenReturn(rows);

            assertThat(service.getSummary(INVENTORY_ID))
                .extracting(GapSummaryRecord::cause)
                .containsExactly("CASSE", "VOL");
        }
    }

    @Nested
    @DisplayName("hasAnalysis")
    class HasAnalysis {

        @Test
        @DisplayName("vrai quand au moins une ligne est qualifiee")
        void vrai() {
            when(gapRepo.existsByStoreInventoryLineStoreInventoryId(INVENTORY_ID)).thenReturn(true);

            assertThat(service.hasAnalysis(INVENTORY_ID)).isTrue();
        }

        @Test
        @DisplayName("faux quand rien n est qualifie")
        void faux() {
            when(gapRepo.existsByStoreInventoryLineStoreInventoryId(INVENTORY_ID)).thenReturn(false);

            assertThat(service.hasAnalysis(INVENTORY_ID)).isFalse();
        }
    }
}

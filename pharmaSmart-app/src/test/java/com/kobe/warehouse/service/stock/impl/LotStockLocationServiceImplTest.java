package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LotStockLocationServiceImpl")
class LotStockLocationServiceImplTest {

    @Mock
    private LotStockLocationRepository lotStockLocationRepository;

    @Mock
    private LotRepository lotRepository;

    private LotStockLocationServiceImpl service;

    private Lot lot;
    private Storage storage;
    private Produit produit;

    @BeforeEach
    void setUp() {
        service = new LotStockLocationServiceImpl(lotStockLocationRepository, lotRepository);
        lot = lot(10);
        storage = storage(1);
        produit = produit(100);
    }

    private static Lot lot(int id) {
        return new Lot().setId(id);
    }

    private static Storage storage(int id) {
        Storage s = new Storage();
        s.setId(id);
        return s;
    }

    private static Produit produit(int id) {
        Produit p = new Produit();
        p.setId(id);
        return p;
    }

    private static LotStockLocation location(Lot lot, Storage storage, int qty) {
        return new LotStockLocation(lot, storage, qty);
    }

    @Nested
    @DisplayName("credit")
    class Credit {

        @ParameterizedTest(name = "qtyDelta={0} ne touche pas au depot")
        @ValueSource(ints = { 0, -1, -50 })
        void ignoreLesQuantitesNonPositives(int qtyDelta) {
            service.credit(lot, storage, qtyDelta);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("incremente une entree existante")
        void incrementeEntreeExistante() {
            LotStockLocation existing = location(lot, storage, 5);
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.of(existing));

            service.credit(lot, storage, 7);

            assertThat(existing.getQty()).isEqualTo(12);
            verify(lotStockLocationRepository).save(existing);
        }

        @Test
        @DisplayName("cree une entree quand elle n existe pas")
        void creeEntreeAbsente() {
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.empty());

            service.credit(lot, storage, 4);

            ArgumentCaptor<LotStockLocation> captor = ArgumentCaptor.forClass(LotStockLocation.class);
            verify(lotStockLocationRepository).save(captor.capture());
            assertThat(captor.getValue().getLot()).isSameAs(lot);
            assertThat(captor.getValue().getStorage()).isSameAs(storage);
            assertThat(captor.getValue().getQty()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("debit")
    class Debit {

        @ParameterizedTest(name = "qtyDelta={0} ne touche pas au depot")
        @ValueSource(ints = { 0, -3 })
        void ignoreLesQuantitesNonPositives(int qtyDelta) {
            service.debit(lot, storage, qtyDelta);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("ne fait rien si aucune entree")
        void aucuneEntree() {
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.empty());

            service.debit(lot, storage, 3);

            verify(lotStockLocationRepository, never()).save(any());
            verify(lotStockLocationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("decremente quand il reste du stock")
        void decremente() {
            LotStockLocation existing = location(lot, storage, 10);
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.of(existing));

            service.debit(lot, storage, 4);

            assertThat(existing.getQty()).isEqualTo(6);
            verify(lotStockLocationRepository).save(existing);
            verify(lotStockLocationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("supprime l entree quand elle tombe a zero")
        void supprimeQuandZero() {
            LotStockLocation existing = location(lot, storage, 4);
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.of(existing));

            service.debit(lot, storage, 4);

            verify(lotStockLocationRepository).delete(existing);
            verify(lotStockLocationRepository, never()).save(any());
        }

        @Test
        @DisplayName("supprime l entree quand le debit depasse le stock")
        void supprimeQuandNegatif() {
            LotStockLocation existing = location(lot, storage, 2);
            when(lotStockLocationRepository.findByLotAndStorage(lot, storage)).thenReturn(Optional.of(existing));

            service.debit(lot, storage, 9);

            verify(lotStockLocationRepository).delete(existing);
        }
    }

    @Nested
    @DisplayName("creditFromSold")
    class CreditFromSold {

        @Test
        @DisplayName("liste nulle : aucun effet")
        void listeNulle() {
            service.creditFromSold(null, storage);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("liste vide : aucun effet")
        void listeVide() {
            service.creditFromSold(List.of(), storage);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("ignore les lots de quantite non positive")
        void ignoreQuantiteNonPositive() {
            service.creditFromSold(List.of(new LotSold(10, "L1", 0, LocalDate.now())), storage);

            verify(lotStockLocationRepository, never()).findByLotIdAndStorageId(anyInt(), anyInt());
            verify(lotStockLocationRepository, never()).save(any());
        }

        @Test
        @DisplayName("incremente une entree existante")
        void incrementeExistante() {
            LotStockLocation existing = location(lot, storage, 3);
            when(lotStockLocationRepository.findByLotIdAndStorageId(10, 1)).thenReturn(Optional.of(existing));

            service.creditFromSold(List.of(new LotSold(10, "L1", 5, LocalDate.now())), storage);

            assertThat(existing.getQty()).isEqualTo(8);
            verify(lotStockLocationRepository).save(existing);
        }

        @Test
        @DisplayName("recree l entree via un proxy de lot quand elle a disparu")
        void recreeEntreeViaProxy() {
            when(lotStockLocationRepository.findByLotIdAndStorageId(10, 1)).thenReturn(Optional.empty());

            service.creditFromSold(List.of(new LotSold(10, "L1", 6, LocalDate.now())), storage);

            ArgumentCaptor<LotStockLocation> captor = ArgumentCaptor.forClass(LotStockLocation.class);
            verify(lotStockLocationRepository).save(captor.capture());
            assertThat(captor.getValue().getLot().getId()).isEqualTo(10);
            assertThat(captor.getValue().getStorage()).isSameAs(storage);
            assertThat(captor.getValue().getQty()).isEqualTo(6);
        }

        @Test
        @DisplayName("traite tous les lots de la liste")
        void traiteToutLaListe() {
            when(lotStockLocationRepository.findByLotIdAndStorageId(10, 1)).thenReturn(Optional.of(location(lot, storage, 1)));
            when(lotStockLocationRepository.findByLotIdAndStorageId(11, 1)).thenReturn(Optional.empty());

            service.creditFromSold(
                List.of(new LotSold(10, "L1", 2, LocalDate.now()), new LotSold(11, "L2", 3, LocalDate.now())),
                storage
            );

            verify(lotStockLocationRepository, times(2)).save(any(LotStockLocation.class));
        }
    }

    @Nested
    @DisplayName("creditLastLot")
    class CreditLastLot {

        @Test
        @DisplayName("quantite non positive : aucun effet")
        void quantiteNonPositive() {
            service.creditLastLot(produit, storage, 0);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("aucun lot recu : aucun effet")
        void aucunLot() {
            when(lotStockLocationRepository.findLastReceivedByStorageAndProduit(1, 100)).thenReturn(Optional.empty());

            service.creditLastLot(produit, storage, 5);

            verify(lotStockLocationRepository, never()).save(any());
        }

        @Test
        @DisplayName("incremente le dernier lot recu")
        void incremente() {
            LotStockLocation last = location(lot, storage, 2);
            when(lotStockLocationRepository.findLastReceivedByStorageAndProduit(1, 100)).thenReturn(Optional.of(last));

            service.creditLastLot(produit, storage, 5);

            assertThat(last.getQty()).isEqualTo(7);
            verify(lotStockLocationRepository).save(last);
        }
    }

    @Nested
    @DisplayName("debitFefo")
    class DebitFefo {

        @Test
        @DisplayName("quantite non positive : aucun effet")
        void quantiteNonPositive() {
            service.debitFefo(produit, storage, -1);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("consomme les lots dans l ordre FEFO et purge ceux vides")
        void consommeEnFefo() {
            LotStockLocation l1 = location(lot(1), storage, 3);
            LotStockLocation l2 = location(lot(2), storage, 10);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(l1, l2));

            service.debitFefo(produit, storage, 8);

            assertThat(l1.getQty()).isZero();
            assertThat(l2.getQty()).isEqualTo(5);
            verify(lotStockLocationRepository).save(l1);
            verify(lotStockLocationRepository).save(l2);
            verify(lotStockLocationRepository).deleteZeroQtyByLot(1);
            verify(lotStockLocationRepository, never()).deleteZeroQtyByLot(2);
        }

        @Test
        @DisplayName("s arrete des que la quantite demandee est servie")
        void sArreteQuandServie() {
            LotStockLocation l1 = location(lot(1), storage, 10);
            LotStockLocation l2 = location(lot(2), storage, 10);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(l1, l2));

            service.debitFefo(produit, storage, 4);

            assertThat(l1.getQty()).isEqualTo(6);
            assertThat(l2.getQty()).isEqualTo(10);
            verify(lotStockLocationRepository).save(l1);
            verify(lotStockLocationRepository, never()).save(l2);
            verify(lotStockLocationRepository, never()).deleteZeroQtyByLot(anyInt());
        }

        @Test
        @DisplayName("stock insuffisant : vide tout ce qui est disponible")
        void stockInsuffisant() {
            LotStockLocation l1 = location(lot(1), storage, 2);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(l1));

            service.debitFefo(produit, storage, 50);

            assertThat(l1.getQty()).isZero();
            verify(lotStockLocationRepository).deleteZeroQtyByLot(1);
        }

        @Test
        @DisplayName("ne purge qu une fois par lot en cas de doublon")
        void purgeDistincte() {
            Lot shared = lot(7);
            LotStockLocation l1 = location(shared, storage, 1);
            LotStockLocation l2 = location(shared, storage, 1);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(l1, l2));

            service.debitFefo(produit, storage, 2);

            verify(lotStockLocationRepository, times(1)).deleteZeroQtyByLot(7);
        }

        @Test
        @DisplayName("aucun lot disponible : aucune ecriture")
        void aucunLot() {
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of());

            service.debitFefo(produit, storage, 5);

            verify(lotStockLocationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("transferFefo")
    class TransferFefo {

        private final Storage dest = storage(2);

        @Test
        @DisplayName("quantite non positive : aucun effet")
        void quantiteNonPositive() {
            service.transferFefo(produit, storage, dest, 0);

            verifyNoInteractions(lotStockLocationRepository);
        }

        @Test
        @DisplayName("deplace en FEFO vers la destination et purge la source videe")
        void transfere() {
            Lot l1Lot = lot(1);
            Lot l2Lot = lot(2);
            LotStockLocation src1 = location(l1Lot, storage, 3);
            LotStockLocation src2 = location(l2Lot, storage, 10);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(src1, src2));
            when(lotStockLocationRepository.findByLotAndStorage(l1Lot, dest)).thenReturn(Optional.empty());
            LotStockLocation destExisting = location(l2Lot, dest, 1);
            when(lotStockLocationRepository.findByLotAndStorage(l2Lot, dest)).thenReturn(Optional.of(destExisting));

            service.transferFefo(produit, storage, dest, 8);

            assertThat(src1.getQty()).isZero();
            assertThat(src2.getQty()).isEqualTo(5);
            assertThat(destExisting.getQty()).isEqualTo(6);
            verify(lotStockLocationRepository).deleteZeroQtyByLot(1);
            verify(lotStockLocationRepository, never()).deleteZeroQtyByLot(2);
        }

        @Test
        @DisplayName("s arrete des que la quantite demandee est deplacee")
        void sArreteQuandServie() {
            Lot l1Lot = lot(1);
            LotStockLocation src1 = location(l1Lot, storage, 10);
            LotStockLocation src2 = location(lot(2), storage, 10);
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of(src1, src2));
            when(lotStockLocationRepository.findByLotAndStorage(l1Lot, dest)).thenReturn(Optional.empty());

            service.transferFefo(produit, storage, dest, 4);

            assertThat(src1.getQty()).isEqualTo(6);
            assertThat(src2.getQty()).isEqualTo(10);
            verify(lotStockLocationRepository, never()).deleteZeroQtyByLot(anyInt());
        }

        @Test
        @DisplayName("source vide : aucune ecriture")
        void sourceVide() {
            when(lotStockLocationRepository.findFefoByStorageAndProduit(1, 100)).thenReturn(List.of());

            service.transferFefo(produit, storage, dest, 5);

            verify(lotStockLocationRepository, never()).save(any());
        }
    }
}

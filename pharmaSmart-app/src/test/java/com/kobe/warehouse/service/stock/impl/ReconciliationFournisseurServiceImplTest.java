package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.ReconciliationFactureFournisseur;
import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.dto.ReconciliationFactureDTO;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService.ReconciliationCommand;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationFournisseurServiceImpl")
class ReconciliationFournisseurServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 3, 12);

    @Mock
    private CommandeRepository commandeRepository;

    private ReconciliationFournisseurServiceImpl service;

    private CommandeId commandeId;

    @BeforeEach
    void setUp() {
        service = new ReconciliationFournisseurServiceImpl(commandeRepository);
        commandeId = new CommandeId(42, ORDER_DATE);
    }

    private static Commande commande(Integer grossAmount, Integer taxAmount) {
        Commande commande = new Commande();
        commande.setGrossAmount(grossAmount);
        commande.setTaxAmount(taxAmount);
        return commande;
    }

    private static ReconciliationCommand command(Integer ht, Integer tva) {
        return new ReconciliationCommand("F-2026-001", LocalDate.of(2026, 3, 15), ht, tva);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("leve EntityNotFoundException quand la commande est introuvable")
        void commandeIntrouvable() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.save(commandeId, command(100, 10)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Commande introuvable");

            verify(commandeRepository, never()).save(any());
        }

        @Test
        @DisplayName("cree la reconciliation quand la commande n en a pas encore")
        void creeReconciliation() {
            Commande commande = commande(1000, 100);
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande));

            ReconciliationFactureDTO dto = service.save(commandeId, command(1000, 100));

            assertThat(commande.getReconciliation()).isNotNull();
            assertThat(commande.getReconciliation().getUpdatedAt()).isNull();
            assertThat(dto.getFactureReference()).isEqualTo("F-2026-001");
            assertThat(dto.getFactureDate()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(dto.getBlMontantHT()).isEqualTo(1000);
            assertThat(dto.getBlTVA()).isEqualTo(100);
            verify(commandeRepository).save(commande);
        }

        @Test
        @DisplayName("horodate la reconciliation existante au lieu d en creer une")
        void metAJourReconciliationExistante() {
            ReconciliationFactureFournisseur existing = new ReconciliationFactureFournisseur().setId(7);
            Commande commande = commande(1000, 100);
            commande.setReconciliation(existing);
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande));

            LocalDateTime before = LocalDateTime.now();
            ReconciliationFactureDTO dto = service.save(commandeId, command(1000, 100));

            assertThat(commande.getReconciliation()).isSameAs(existing);
            assertThat(existing.getUpdatedAt()).isNotNull().isAfterOrEqualTo(before);
            assertThat(dto.getId()).isEqualTo(7);
        }

        @Test
        @DisplayName("statut RECONCILIEE quand les deux ecarts sont nuls")
        void statutReconciliee() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(1000, 100)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(1000, 100));

            assertThat(dto.getEcartHT()).isZero();
            assertThat(dto.getEcartTVA()).isZero();
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.RECONCILIEE);
        }

        @Test
        @DisplayName("statut ECART sur un ecart HT seul")
        void statutEcartSurHt() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(1000, 100)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(1200, 100));

            assertThat(dto.getEcartHT()).isEqualTo(200);
            assertThat(dto.getEcartTVA()).isZero();
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.ECART);
        }

        @Test
        @DisplayName("statut ECART sur un ecart TVA seul")
        void statutEcartSurTva() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(1000, 100)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(1000, 130));

            assertThat(dto.getEcartHT()).isZero();
            assertThat(dto.getEcartTVA()).isEqualTo(30);
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.ECART);
        }

        @Test
        @DisplayName("ecart negatif quand la facture est inferieure au bon de livraison")
        void ecartNegatif() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(1000, 100)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(800, 80));

            assertThat(dto.getEcartHT()).isEqualTo(-200);
            assertThat(dto.getEcartTVA()).isEqualTo(-20);
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.ECART);
        }

        @Test
        @DisplayName("un montant brut nul sur la commande vaut zero")
        void grossAmountNul() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(null, 0)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(0, 0));

            assertThat(dto.getBlMontantHT()).isZero();
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.RECONCILIEE);
        }

        @Test
        @DisplayName("des montants de facture nuls valent zero")
        void montantsFactureNuls() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(0, 0)));

            ReconciliationFactureDTO dto = service.save(commandeId, command(null, null));

            assertThat(dto.getFactureMontantHT()).isZero();
            assertThat(dto.getFactureTVA()).isZero();
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.RECONCILIEE);
        }
    }

    @Nested
    @DisplayName("findByCommandeId")
    class FindByCommandeId {

        @Test
        @DisplayName("leve EntityNotFoundException quand la commande est introuvable")
        void commandeIntrouvable() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByCommandeId(commandeId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Commande introuvable");
        }

        @Test
        @DisplayName("renvoie null quand la commande n a pas de reconciliation")
        void sansReconciliation() {
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande(0, 0)));

            assertThat(service.findByCommandeId(commandeId)).isNull();
        }

        @Test
        @DisplayName("mappe la reconciliation existante")
        void avecReconciliation() {
            Commande commande = commande(0, 0);
            commande.setReconciliation(
                new ReconciliationFactureFournisseur()
                    .setId(9)
                    .setFactureReference("F-2026-042")
                    .setStatut(ReconciliationStatut.ECART)
                    .setEcartHT(150)
            );
            when(commandeRepository.findById(commandeId)).thenReturn(Optional.of(commande));

            ReconciliationFactureDTO dto = service.findByCommandeId(commandeId);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(9);
            assertThat(dto.getFactureReference()).isEqualTo("F-2026-042");
            assertThat(dto.getStatut()).isEqualTo(ReconciliationStatut.ECART);
            assertThat(dto.getEcartHT()).isEqualTo(150);
        }
    }
}

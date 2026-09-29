package com.kobe.warehouse.service.reglement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import java.time.LocalDate;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.service.ReglementFactureModeAllService;
import com.kobe.warehouse.service.reglement.service.ReglementFactureSelectionneesService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeSelectionFactureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReglementRegistry")
class ReglementRegistryTest {

    @Mock
    private ReglementGroupeSelectionFactureService reglementGroupeSelectionFactureService;

    @Mock
    private ReglementGroupeFactureService reglementGroupeFactureService;

    @Mock
    private ReglementFactureModeAllService reglementFactureModeAllService;

    @Mock
    private ReglementFactureSelectionneesService reglementFactureSelectionneesService;

    @Mock
    private FacturationRepository facturationRepository;

    private ReglementRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ReglementRegistry(
            reglementGroupeSelectionFactureService,
            reglementGroupeFactureService,
            reglementFactureModeAllService,
            reglementFactureSelectionneesService,
            facturationRepository
        );
    }

    @Test
    @DisplayName("GROUPE_TOTAL est servi par le reglement de groupe complet")
    void groupeTotal() {
        assertThat(registry.getService(ModeEditionReglement.GROUPE_TOTAL)).isSameAs(reglementGroupeFactureService);
    }

    @Test
    @DisplayName("GROUPE_PARTIEL est servi par le reglement de groupe sur selection")
    void groupePartiel() {
        assertThat(registry.getService(ModeEditionReglement.GROUPE_PARTIEL)).isSameAs(reglementGroupeSelectionFactureService);
    }

    @Test
    @DisplayName("FACTURE_TOTAL est servi par le reglement de facture complete")
    void factureTotal() {
        assertThat(registry.getService(ModeEditionReglement.FACTURE_TOTAL)).isSameAs(reglementFactureModeAllService);
    }

    @Test
    @DisplayName("FACTURE_PARTIEL est servi par le reglement sur bons selectionnes")
    void facturePartiel() {
        assertThat(registry.getService(ModeEditionReglement.FACTURE_PARTIEL)).isSameAs(reglementFactureSelectionneesService);
    }

    @ParameterizedTest(name = "le mode {0} n est pas pris en charge")
    @EnumSource(
        value = ModeEditionReglement.class,
        names = { "GROUPE_TOTAL", "GROUPE_PARTIEL", "FACTURE_TOTAL", "FACTURE_PARTIEL" },
        mode = EnumSource.Mode.EXCLUDE
    )
    void modeNonPrisEnCharge(ModeEditionReglement mode) {
        assertThatThrownBy(() -> registry.getService(mode))
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("Ce mode de facturation n'est pas pris en charge");
    }

    /** FAC-48 : le rapprochement envoie FACTURE_TOTAL même pour une facture de groupe. */
    @Test
    @DisplayName("FACTURE_TOTAL sur une facture de groupe est servi par le reglement de groupe")
    void factureTotaleDUnGroupe() {
        FactureItemId id = new FactureItemId(61L, LocalDate.of(2026, 9, 1));
        when(facturationRepository.existsByIdAndInvoiceDateAndGroupeTiersPayantIsNotNull(id.getId(), id.getInvoiceDate())).thenReturn(true);

        ReglementParam param = new ReglementParam().setId(id).setMode(ModeEditionReglement.FACTURE_TOTAL);

        assertThat(registry.getService(param)).isSameAs(reglementGroupeFactureService);
    }

    @Test
    @DisplayName("FACTURE_TOTAL sur une facture individuelle reste au reglement de facture complete")
    void factureTotaleIndividuelle() {
        FactureItemId id = new FactureItemId(12L, LocalDate.of(2026, 9, 1));
        when(facturationRepository.existsByIdAndInvoiceDateAndGroupeTiersPayantIsNotNull(id.getId(), id.getInvoiceDate())).thenReturn(false);

        ReglementParam param = new ReglementParam().setId(id).setMode(ModeEditionReglement.FACTURE_TOTAL);

        assertThat(registry.getService(param)).isSameAs(reglementFactureModeAllService);
    }

    @ParameterizedTest(name = "le mode {0} est respecte tel quel")
    @EnumSource(value = ModeEditionReglement.class, names = { "GROUPE_TOTAL", "GROUPE_PARTIEL", "FACTURE_PARTIEL" })
    void autresModesSansConsulterLaFacture(ModeEditionReglement mode) {
        ReglementParam param = new ReglementParam().setId(new FactureItemId(61L, LocalDate.of(2026, 9, 1))).setMode(mode);

        assertThat(registry.getService(param)).isSameAs(registry.getService(mode));
        verifyNoInteractions(facturationRepository);
    }

    @Test
    @DisplayName("un mode nul n est pas pris en charge")
    void modeNul() {
        assertThatThrownBy(() -> registry.getService((ModeEditionReglement) null)).isInstanceOf(GenericError.class);
    }
}

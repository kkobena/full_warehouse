package com.kobe.warehouse.service.exports.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.service.ap.AccountsPayableService;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import com.kobe.warehouse.service.facturation.service.RapprochementService;
import com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelService;
import com.kobe.warehouse.service.report.pdf.SupplierPerformancePdfReportService;
import com.kobe.warehouse.service.report.pdf.TiersPayantPdfReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exports — éditions fournisseurs et tiers payants")
class ProducteurRapportsPartenairesTest {

    private static final LocalDate DU = LocalDate.of(2026, 10, 1);
    private static final LocalDate AU = LocalDate.of(2026, 10, 31);

    @Mock
    private AccountsPayableService accountsPayableService;

    @Mock
    private SupplierPerformancePdfReportService supplierPerformancePdfReportService;

    @Mock
    private TiersPayantPdfReportService tiersPayantPdfReportService;

    @Mock
    private RecapitulatifMensuelService recapitulatifMensuelService;

    @Mock
    private RapprochementService rapprochementService;

    @Captor
    private ArgumentCaptor<RecapitulatifMensuelParams> recapitulatif;

    @Captor
    private ArgumentCaptor<RapprochementParams> rapprochement;

    @InjectMocks
    private ProducteurRapportsPartenaires partenaires;

    @Test
    @DisplayName("comptes fournisseurs : la période demandée")
    void comptesFournisseurs() {
        partenaires.produire(RapportExport.COMPTES_FOURNISSEURS, FormatExport.PDF, DU, AU, Map.of());

        verify(accountsPayableService).exportComptesAsPdf(DU, AU);
    }

    @Test
    @DisplayName("performance et créances : état du jour, la période ne les concerne pas")
    void etatsDuJour() {
        partenaires.produire(RapportExport.PERFORMANCE_FOURNISSEURS, FormatExport.PDF, null, null, Map.of());
        partenaires.produire(RapportExport.CREANCES_TIERS_PAYANTS, FormatExport.PDF, null, null, Map.of());

        verify(supplierPerformancePdfReportService).export();
        verify(tiersPayantPdfReportService).export();
    }

    @Test
    @DisplayName("récapitulatif des factures : le mois du début de la période, et le type retenu")
    void recapitulatifDesFactures() {
        partenaires.produire(
            RapportExport.RECAPITULATIF_FACTURES,
            FormatExport.XLSX,
            DU,
            AU,
            Map.of(FiltreExport.TYPE_FACTURE, List.of("GROUPED"))
        );

        verify(recapitulatifMensuelService).exportExcel(recapitulatif.capture());
        RecapitulatifMensuelParams params = recapitulatif.getValue();
        assertThat(params.annee()).isEqualTo(2026);
        assertThat(params.mois()).isEqualTo(10);
        assertThat(params.typeFacture()).isEqualTo(TypeFacture.GROUPED);
    }

    @Test
    @DisplayName("rapprochement : la période, tous tiers payants et tous statuts")
    void rapprochementDeLaPeriode() {
        partenaires.produire(RapportExport.RAPPROCHEMENT_TIERS_PAYANTS, FormatExport.PDF, DU, AU, Map.of());

        verify(rapprochementService).exportPdf(rapprochement.capture());
        RapprochementParams params = rapprochement.getValue();
        assertThat(params.startDate()).isEqualTo(DU);
        assertThat(params.endDate()).isEqualTo(AU);
        assertThat(params.tiersPayantId()).isNull();
        assertThat(params.statuts()).isNull();
    }
}

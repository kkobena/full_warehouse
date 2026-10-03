package com.kobe.warehouse.service.receipt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketNonRembourseBuilderTest {

    @Test
    void ticketNonRembourse_regroupeLesSeulesLignesExcluesEtLeurTotal() {
        ThirdPartySaleDTO vente = new ThirdPartySaleDTO();
        vente.setSalesLines(List.of(ligne(1000, false), ligne(500, true), ligne(300, true)));

        var ticket = TicketNonRembourseBuilder.construire(vente).orElseThrow();

        assertEquals(2, ticket.getSalesLines().size());
        assertEquals(800, ticket.getSalesAmount());
        assertEquals(800, ticket.getAmountToBePaid());
        assertTrue(ticket.getPayments().isEmpty());
    }

    @Test
    void aucunTicketQuandToutEstRembourse() {
        ThirdPartySaleDTO vente = new ThirdPartySaleDTO();
        vente.setSalesLines(List.of(ligne(1000, false)));

        assertTrue(TicketNonRembourseBuilder.construire(vente).isEmpty());
    }

    private SaleLineDTO ligne(int montant, boolean nonRembourse) {
        SaleLineDTO l = new SaleLineDTO();
        l.setSalesAmount(montant);
        l.setNonRembourse(nonRembourse);
        return l;
    }

    @Test
    void reglements_leTicketNonRembourseEstRegleAvecLesModesAutresQuEspecesDAbord() {
        List<PaymentDTO> reglements = List.of(reglement("CASH", 1000, 2000), reglement("CB", 300, 300));

        var repartition = TicketNonRembourseBuilder.repartirReglements(reglements, 500);

        // 300 en carte + 200 en espèces pour les 500 non remboursés ; le reste (800, 1800 versés) reste sur le ticket assurance
        assertEquals(List.of(200, 300), repartition.ticketNonRembourse().stream().map(PaymentDTO::getPaidAmount).toList());
        assertEquals(1, repartition.ticketAssurance().size());
        assertEquals(800, repartition.ticketAssurance().getFirst().getPaidAmount());
        assertEquals(1800, repartition.ticketAssurance().getFirst().getMontantVerse());
    }

    @Test
    void reglements_aucunReglementPourLeTicketNonRembourseQuandToutEstRembourse() {
        var repartition = TicketNonRembourseBuilder.repartirReglements(List.of(reglement("CB", 300, 300)), 0);

        assertTrue(repartition.ticketNonRembourse().isEmpty());
        assertEquals(300, repartition.ticketAssurance().getFirst().getPaidAmount());
    }

    @Test
    void tva_calculeeParTauxSurLesLignesDonnees() {
        SaleLineDTO a = ligne(1180, false);
        a.setTaxValue(18);
        SaleLineDTO b = ligne(500, false);
        b.setTaxValue(0);

        var tva = TicketNonRembourseBuilder.calculerTva(List.of(a, b));

        assertEquals(1, tva.size());
        assertEquals(18, tva.getFirst().getTva());
        assertEquals(180, tva.getFirst().getAmount());
    }

    private PaymentDTO reglement(String code, int paye, int verse) {
        PaymentDTO p = new PaymentDTO();
        p.setPaymentMode(new PaymentModeDTO().setCode(code));
        p.setPaidAmount(paye);
        p.setMontantVerse(verse);
        return p;
    }
}

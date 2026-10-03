package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.List;
import java.util.Optional;

/**
 * Ticket des lignes non remboursées d'une vente assurance : une vente comptant fictive, réglée par le
 * patient avec le reste de sa part — on n'y met donc ni règlement ni TVA, seulement les lignes et leur total.
 */
public final class TicketNonRembourseBuilder {

    public static final String TITRE = "PRODUITS NON REMBOURSES";

    private TicketNonRembourseBuilder() {}

    /** Règlements de la vente répartis entre le ticket assurance et le ticket des non remboursés. */
    public record Reglements(List<PaymentDTO> ticketAssurance, List<PaymentDTO> ticketNonRembourse) {}

    /**
     * Le ticket des non remboursés est réglé d'abord avec les modes autres qu'espèces, puis avec les espèces :
     * la monnaie rendue reste ainsi sur le ticket assurance, qui porte l'essentiel du règlement.
     */
    public static Reglements repartirReglements(List<PaymentDTO> reglements, int montantNonRembourse) {
        List<PaymentDTO> assurance = new ArrayList<>();
        List<PaymentDTO> nonRembourse = new ArrayList<>();
        if (reglements == null) {
            return new Reglements(assurance, nonRembourse);
        }
        int restant = montantNonRembourse;
        List<PaymentDTO> ordre = new ArrayList<>(reglements);
        ordre.sort(Comparator.comparing(TicketNonRembourseBuilder::estEspeces));
        java.util.Map<PaymentDTO, PaymentDTO> partAssurance = new java.util.IdentityHashMap<>();
        java.util.Map<PaymentDTO, PaymentDTO> partNonRembourse = new java.util.IdentityHashMap<>();
        for (PaymentDTO reglement : ordre) {
            int paye = reglement.getPaidAmount() == null ? 0 : reglement.getPaidAmount();
            int part = Math.min(paye, restant);
            restant -= part;
            if (part > 0) {
                partNonRembourse.put(reglement, copier(reglement, part, part));
            }
            if (paye - part > 0 || paye == 0) {
                int verse = reglement.getMontantVerse() == null ? 0 : reglement.getMontantVerse();
                partAssurance.put(reglement, copier(reglement, paye - part, Math.max(verse - part, 0)));
            }
        }
        for (PaymentDTO reglement : reglements) {
            if (partAssurance.containsKey(reglement)) {
                assurance.add(partAssurance.get(reglement));
            }
            if (partNonRembourse.containsKey(reglement)) {
                nonRembourse.add(partNonRembourse.get(reglement));
            }
        }
        return new Reglements(assurance, nonRembourse);
    }

    /** TVA par taux, calculée comme à l'enregistrement de la vente. */
    public static List<TvaEmbeded> calculerTva(List<SaleLineDTO> lignes) {
        TreeMap<Integer, Integer> parTaux = new TreeMap<>();
        for (SaleLineDTO ligne : lignes) {
            Integer taux = ligne.getTaxValue();
            if (taux == null || taux <= 0) {
                continue;
            }
            int ht = (int) Math.ceil(ligne.getSalesAmount() / (1 + taux / 100.0));
            parTaux.merge(taux, ligne.getSalesAmount() - ht, Integer::sum);
        }
        return parTaux.entrySet().stream().map(e -> new TvaEmbeded().setTva(e.getKey()).setAmount(e.getValue())).toList();
    }

    private static boolean estEspeces(PaymentDTO reglement) {
        return reglement.getPaymentMode() != null && ModePaimentCode.CASH.name().equals(reglement.getPaymentMode().getCode());
    }

    private static PaymentDTO copier(PaymentDTO source, int paye, int verse) {
        PaymentDTO copie = new PaymentDTO();
        copie.setPaymentMode(source.getPaymentMode());
        copie.setPaidAmount(paye);
        copie.setMontantVerse(verse);
        return copie;
    }

    static List<SaleLineDTO> listerLignesNonRemboursees(ThirdPartySaleDTO vente) {
        return vente.getSalesLines().stream().filter(SaleLineDTO::isNonRembourse).toList();
    }

    public static Optional<CashSaleDTO> construire(ThirdPartySaleDTO vente) {
        List<SaleLineDTO> lignes = listerLignesNonRemboursees(vente);
        if (lignes.isEmpty()) {
            return Optional.empty();
        }
        int total = lignes.stream().mapToInt(SaleLineDTO::getSalesAmount).sum();
        CashSaleDTO ticket = new CashSaleDTO();
        ticket.setNumberTransaction(vente.getNumberTransaction());
        ticket.setCassier(vente.getCassier());
        ticket.setCassierId(vente.getCassierId());
        ticket.setSeller(vente.getSeller());
        ticket.setSellerId(vente.getSellerId());
        ticket.setCreatedAt(vente.getCreatedAt());
        ticket.setUpdatedAt(vente.getUpdatedAt());
        ticket.setSalesLines(new ArrayList<>(lignes));
        ticket.setSalesAmount(total);
        ticket.setNetAmount(total);
        ticket.setAmountToBePaid(total);
        ticket.setDiscountAmount(0);
        ticket.setRestToPay(0);
        ticket.setPayments(repartirReglements(vente.getPayments(), total).ticketNonRembourse());
        ticket.setTvaEmbededs(calculerTva(lignes));
        return Optional.of(ticket);
    }
}

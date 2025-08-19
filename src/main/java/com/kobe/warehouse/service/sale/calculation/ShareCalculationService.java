package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.TiersPayantPrix;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ShareCalculationService {

    public CalculationResult calculate(SaleContext context) {
        CalculationResult finalResult = new CalculationResult();

        for (SalesLine item : context.getItems()) {
            CalculatedShare itemShare = new CalculatedShare();
            itemShare.setProductId(item.getProduit().getId());
            itemShare.setPharmacyPrice(item.getRegularUnitPrice());
            Set<ClientTiersPayant> applicablePolicies = context.getClientTiersPayants(); // In a real scenario, you might filter policies per item
            List<TiersPayantPrix> prixAssurances = item.getPrixAssurances();
            int calculationBase = determineCalculationBase(item, prixAssurances);
            itemShare.setCalculationBasePrice(calculationBase);

            double remainingAmount = calculationBase;
            double totalReimbursed = 0;

            // RO Share
            double roShare = calculateRoShare(calculationBase, prixAssurances, applicablePolicies);
            totalReimbursed += roShare;
            remainingAmount -= roShare;
            itemShare.setRoShare(roShare);

            // Complementary Shares
            Map<Long, Double> complementaryShares = calculateComplementaryShares(remainingAmount, prixAssurances, applicablePolicies);
            for (double share : complementaryShares.values()) {
                totalReimbursed += share;
            }
            itemShare.setComplementaryShares(complementaryShares);
            itemShare.setTotalReimbursedAmount(totalReimbursed);

            // Patient Share
            double patientShare = item.getSalesAmount() - totalReimbursed;
            itemShare.setPatientShare(patientShare);

            finalResult.add(itemShare);
        }

        finalResult.aggregateTotals();
        return finalResult;
    }

    private int determineCalculationBase(SalesLine item, List<TiersPayantPrix> prixAssurances) {

        if (CollectionUtils.isEmpty(prixAssurances)) return item.getSalesAmount();
        return prixAssurances.stream()
            .mapToInt(TiersPayantPrix::getMontant)
            .min()
            .orElse(item.getSalesAmount());
    }

    private double calculateRoShare(double calculationBase, List<TiersPayantPrix> prixAssurances, Set<ClientTiersPayant> applicablePolicies) {
        if (CollectionUtils.isEmpty(prixAssurances)) {
            return applicablePolicies.stream()
                .filter(p -> p.getPriorite() == PrioriteTiersPayant.R0)
                .findFirst()
                .map(p -> calculationBase * p.getTaux() / 100.0)
                .orElse(0.0);
        }
        return prixAssurances.stream()
            .filter(p -> p.getClientTiersPayant().getPriorite() == PrioriteTiersPayant.R0)
            .findFirst()
            .map(p -> calculatePrixOption(calculationBase))
            .orElse(0.0);
    }

    private double calculatePrixOption(double calculationBase) {
        return calculationBase;
        // return calculationBase * p.getTaux() / 100.0;
    }


    private Map<Long, Double> calculateComplementaryShares(double remainingAmount, List<TiersPayantPrix> prixAssurances, Set<ClientTiersPayant> clientTiersPayants) {
        Map<Long, Double> shares = new HashMap<>();
        double currentRemaining = remainingAmount;
        if (!CollectionUtils.isEmpty(prixAssurances)) {
            List<TiersPayantPrix> complementaryPolicies =
                prixAssurances.stream()
                    .filter(p -> p.getClientTiersPayant().getPriorite() != PrioriteTiersPayant.R0)
                    .sorted(Comparator.comparingInt(c -> c.getClientTiersPayant().getPriorite().getValue()))
                    .collect(Collectors.toList());

            for (TiersPayantPrix tiersPayantPrix : complementaryPolicies) {
                if (currentRemaining <= 0) break;

                ClientTiersPayant clientTiersPayant = tiersPayantPrix.getClientTiersPayant();
                // This is a simplified logic for CAS-6. A real implementation would need more details on the plans.
                //double rate = "CONFORT".equalsIgnoreCase(policy.getPolicyPlan()) ? 100.0 : policy.getReimbursementRate();

                //  double potentialShare = remainingAmount * rate / 100.0;
                double potentialShare = tiersPayantPrix.getMontant();


                double actualShare = Math.min(currentRemaining, potentialShare);
                shares.put(clientTiersPayant.getId(), actualShare);
                currentRemaining -= actualShare;
            }
        } else {
            for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
                if (currentRemaining <= 0) break;

                // This is a simplified logic for CAS-6. A real implementation would need more details on the plans.
                double rate = clientTiersPayant.getTaux() * 1.0;
                double potentialShare = remainingAmount * rate / 100.0; // The share is on the remaining part, not the total base
                double actualShare = Math.min(currentRemaining, potentialShare);
                shares.put(clientTiersPayant.getId(), actualShare);
                currentRemaining -= actualShare;
            }
        }


        return shares;
    }
}

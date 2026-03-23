package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.repository.ParetoAnalysisRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.dto.ProduitIndicateursDTO;
import com.kobe.warehouse.service.dto.VenteMoisDTO;
import com.kobe.warehouse.service.stock.ProduitIndicateursService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProduitIndicateursServiceImpl implements ProduitIndicateursService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ParetoAnalysisRepository paretoAnalysisRepository;
    private final VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository;

    public ProduitIndicateursServiceImpl(
        ParetoAnalysisRepository paretoAnalysisRepository,
        VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository
    ) {
        this.paretoAnalysisRepository = paretoAnalysisRepository;
        this.ventesMensuellesAgregeesRepository = ventesMensuellesAgregeesRepository;
    }

    @Override
    public Optional<ProduitIndicateursDTO> getIndicateurs(Integer produitId) {
        // 1. Données de base produit (classeCriticite, prix, badges réglementaires)
        List<Object[]> produitRows = entityManager.createNativeQuery("""
            SELECT p.classe_criticite, p.est_medicament_essentiel, p.est_produit_garde,
                   p.regular_unit_price, p.cost_amount
            FROM produit p
            WHERE p.id = :produitId
            """)
            .setParameter("produitId", produitId)
            .getResultList();

        if (produitRows.isEmpty()) {
            return Optional.empty();
        }
        Object[] produitRow = produitRows.getFirst();
        ClasseCriticite classeCriticite = toClasseCriticite((String) produitRow[0]);
        boolean estMedicamentEssentiel = Boolean.TRUE.equals(produitRow[1]);
        boolean estProduitGarde = Boolean.TRUE.equals(produitRow[2]);
        BigDecimal tauxMarge = calculerTauxMarge(produitRow[3], produitRow[4]);

        // 2. Indicateurs de rotation depuis v_stock_rotation
        List<Object[]> rotationRows = entityManager.createNativeQuery("""
            SELECT vsr.cmm,
                   vsr.rotation_annuelle_qte,
                   vsr.couverture_stock_jours,
                   vsr.ca_30_jours,
                   vsr.ca_12_mois,
                   vsr.qte_vendue_12_mois
            FROM v_stock_rotation vsr
            WHERE vsr.produit_id = :produitId
            """)
            .setParameter("produitId", produitId)
            .getResultList();

        BigDecimal cmm = BigDecimal.ZERO;
        BigDecimal rotationAnnuelleQte = null;
        Integer couvertureStockJours = null;
        Integer ca30Jours = 0;
        Integer ca12Mois = 0;
        Integer qteVendue12Mois = 0;

        if (!rotationRows.isEmpty()) {
            Object[] r = rotationRows.getFirst();
            cmm = toBigDecimal(r[0]);
            rotationAnnuelleQte = r[1] != null ? toBigDecimal(r[1]) : null;
            couvertureStockJours = r[2] != null ? ((Number) r[2]).intValue() : null;
            ca30Jours = r[3] != null ? ((Number) r[3]).intValue() : 0;
            ca12Mois = r[4] != null ? ((Number) r[4]).intValue() : 0;
            qteVendue12Mois = r[5] != null ? ((Number) r[5]).intValue() : 0;
        }

        // 3. Score Pareto depuis v_abc_pareto_analysis
        Integer rang = null;
        BigDecimal caCumulePct = null;
        Integer frequenceMois = null;

        Optional<Object[]> paretoOpt = paretoAnalysisRepository.findByProduitId(produitId);
        if (paretoOpt.isPresent()) {
            Object[] p = paretoOpt.get();
            caCumulePct = p[1] != null ? toBigDecimal(p[1]) : null;
            rang = p[2] != null ? ((Number) p[2]).intValue() : null;
            frequenceMois = p[4] != null ? ((Number) p[4]).intValue() : null;
        }

        return Optional.of(new ProduitIndicateursDTO(
            produitId,
            classeCriticite,
            estMedicamentEssentiel,
            estProduitGarde,
            cmm,
            rotationAnnuelleQte,
            couvertureStockJours,
            ca30Jours,
            ca12Mois,
            qteVendue12Mois,
            tauxMarge,
            rang,
            caCumulePct,
            frequenceMois
        ));
    }

    @Override
    public List<VenteMoisDTO> getVentesMensuelles(Integer produitId, int nbMois) {
        return ventesMensuellesAgregeesRepository
            .findLastNMonthsByProduit(produitId, nbMois)
            .stream()
            .map(vma -> new VenteMoisDTO(
                vma.getAnneeMois(),
                vma.getQuantiteVendue(),
                vma.getMontantCa(),
                vma.getNombreVentes()
            ))
            // Retourner en ordre croissant (passé → présent) pour Chart.js
            .sorted(Comparator.comparing(VenteMoisDTO::anneeMois))
            .toList();
    }

    // ── helpers ──

    private static ClasseCriticite toClasseCriticite(String value) {
        if (value == null) return ClasseCriticite.B;
        try {
            return ClasseCriticite.fromString(value);
        } catch (Exception e) {
            return ClasseCriticite.B;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private static BigDecimal calculerTauxMarge(Object prixVente, Object prixAchat) {
        if (prixVente == null || prixAchat == null) return null;
        BigDecimal pv = new BigDecimal(prixVente.toString());
        BigDecimal pa = new BigDecimal(prixAchat.toString());
        if (pv.compareTo(BigDecimal.ZERO) == 0) return null;
        return pv.subtract(pa)
            .multiply(BigDecimal.valueOf(100))
            .divide(pv, 2, RoundingMode.HALF_UP);
    }
}

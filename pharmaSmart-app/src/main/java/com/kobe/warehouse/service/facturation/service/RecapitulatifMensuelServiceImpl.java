package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.FactureTiersPayantRepository;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelDto;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelRow;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class RecapitulatifMensuelServiceImpl implements RecapitulatifMensuelService {

    private final AppConfigurationService appConfigurationService;

    @PersistenceContext
    private EntityManager entityManager;

    public RecapitulatifMensuelServiceImpl(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public Page<RecapitulatifMensuelDto> getRecapitulatif(RecapitulatifMensuelParams params, Pageable pageable) {
        YearMonth yearMonth = YearMonth.of(params.annee(), params.mois());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate precedentEndDate = startDate.minusDays(1);

        List<FactureTiersPayant> factures = queryFactures(params, startDate, endDate);

        // Group by tiersPayant (use tiersPayant.id as key; fallback to groupeTiersPayant)
        Map<Integer, List<FactureTiersPayant>> grouped = factures.stream()
            .collect(Collectors.groupingBy(f -> {
                if (f.getTiersPayant() != null) {
                    return f.getTiersPayant().getId();
                }
                return f.getGroupeTiersPayant() != null ? f.getGroupeTiersPayant().getId() : 0;
            }));

        List<RecapitulatifMensuelDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<FactureTiersPayant>> entry : grouped.entrySet()) {
            List<FactureTiersPayant> tpFactures = entry.getValue();
            FactureTiersPayant first = tpFactures.getFirst();
            TiersPayant tp = first.getTiersPayant();
            GroupeTiersPayant groupeTiersPayant=tp==null?first.getGroupeTiersPayant():null;
            String tpName = tp != null ? tp.getFullName() : (groupeTiersPayant != null ? groupeTiersPayant.getName() : "");
            String tpCode = tp != null ? (tp.getCodeOrganisme() != null ? tp.getCodeOrganisme() : "") : "";
            int idTp =tp != null ?tp.getId() :groupeTiersPayant.getId();
            int delai = tp != null && tp.getDelaiReglement() != null ? tp.getDelaiReglement() : appConfigurationService.getDelaiReglement();

            BigDecimal totalFacture = BigDecimal.ZERO;
            BigDecimal totalRegle = BigDecimal.ZERO;
            int nombreFactures = tpFactures.size();
            int nombreImpayees = 0;
            List<RecapitulatifMensuelRow> lignes = new ArrayList<>();

            for (FactureTiersPayant f : tpFactures) {
                BigDecimal montantNet = f.getMontantNet() != null ? f.getMontantNet() : BigDecimal.ZERO;
                BigDecimal montantRegle = BigDecimal.valueOf(f.getMontantRegle());
                BigDecimal restantDu = montantNet.subtract(montantRegle);
                LocalDate echeance = f.getInvoiceDate().plusDays(delai);

                totalFacture = totalFacture.add(montantNet);
                totalRegle = totalRegle.add(montantRegle);

                if (f.getStatut() != InvoiceStatut.PAID) {
                    nombreImpayees++;
                }

                lignes.add(new RecapitulatifMensuelRow(
                    f.getNumFacture(),
                    f.getInvoiceDate(),
                    echeance,
                    montantNet,
                    montantRegle,
                    restantDu,
                    f.getStatut()
                ));
            }

            BigDecimal soldePrecedent = computeSoldePrecedent(entry.getKey(), precedentEndDate, tp);
            BigDecimal soldeActuel = totalFacture.subtract(totalRegle);
            BigDecimal soldeCumule = soldePrecedent.add(soldeActuel);

            result.add(new RecapitulatifMensuelDto(idTp,
                tpName,
                tpCode,
                yearMonth,
                soldePrecedent,
                totalFacture,
                totalRegle,
                soldeActuel,
                soldeCumule,
                nombreFactures,
                nombreImpayees,
                lignes
            ));
        }

        // Apply pagination in-memory
        int total = result.size();
        int fromIndex = (int) pageable.getOffset();
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<RecapitulatifMensuelDto> pageContent = fromIndex >= total ? List.of() : result.subList(fromIndex, toIndex);
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public byte[] exportPdf(RecapitulatifMensuelParams params) {
        return new byte[0];
    }

    @Override
    public byte[] exportExcel(RecapitulatifMensuelParams params) {
        return new byte[0];
    }

    private List<FactureTiersPayant> queryFactures(RecapitulatifMensuelParams params, LocalDate startDate, LocalDate endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactureTiersPayant> cq = cb.createQuery(FactureTiersPayant.class);
        Root<FactureTiersPayant> root = cq.from(FactureTiersPayant.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.between(root.get("invoiceDate"), startDate, endDate));

        if (!CollectionUtils.isEmpty(params.tiersPayantIds())) {
            predicates.add(root.get("tiersPayant").get("id").in(params.tiersPayantIds()));
        }

        if (!CollectionUtils.isEmpty(params.groupIds())) {
            predicates.add(root.get("groupeTiersPayant").get("id").in(params.groupIds()));
        }

        TypeFacture typeFacture = params.typeFacture();
        if (typeFacture != null) {
            if (typeFacture == TypeFacture.INDIVIDUAL) {
                predicates.add(cb.isNull(root.get("groupeTiersPayant")));
            } else if (typeFacture == TypeFacture.GROUPED) {
                predicates.add(cb.isNotNull(root.get("groupeTiersPayant")));
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("invoiceDate")));

        TypedQuery<FactureTiersPayant> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    private BigDecimal computeSoldePrecedent(Integer tiersPayantKey, LocalDate endDate, TiersPayant tp) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactureTiersPayant> cq = cb.createQuery(FactureTiersPayant.class);
        Root<FactureTiersPayant> root = cq.from(FactureTiersPayant.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), endDate));
        predicates.add(root.get("statut").in(List.of(InvoiceStatut.NOT_PAID, InvoiceStatut.PARTIALLY_PAID)));

        if (tp != null) {
            predicates.add(cb.equal(root.get("tiersPayant").get("id"), tiersPayantKey));
        } else {
            predicates.add(cb.equal(root.get("groupeTiersPayant").get("id"), tiersPayantKey));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        try {
            List<FactureTiersPayant> previousFactures = entityManager.createQuery(cq).getResultList();
            BigDecimal totalNet = previousFactures.stream()
                .map(f -> f.getMontantNet() != null ? f.getMontantNet() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalRegle = previousFactures.stream()
                .map(f -> BigDecimal.valueOf(f.getMontantRegle()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return totalNet.subtract(totalRegle);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

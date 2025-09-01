package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajust_;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Ajustement_;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.service.FileResourceService;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import com.kobe.warehouse.service.report.AjustementReportReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@Transactional(readOnly = true)
public class CustomizedAjustRepository extends FileResourceService implements AjustService {

    private final AjustementReportReportService ajustementReportService;

    @PersistenceContext
    private EntityManager em;

    public CustomizedAjustRepository(AjustementReportReportService ajustementReportService) {
        this.ajustementReportService = ajustementReportService;
    }

    @Override
    public Page<AjustDTO> loadAll(AjustementFilterRecord ajustementFilterRecord, Pageable pageable) {
        long total = findAllCount(ajustementFilterRecord);
        List<AjustDTO> list = new ArrayList<>();
        if (total > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Ajust> cq = cb.createQuery(Ajust.class);
            Root<Ajustement> root = cq.from(Ajustement.class);
            cq.select(root.get(Ajustement_.ajust)).distinct(true).orderBy(cb.desc(root.get(Ajustement_.ajust).get(Ajust_.dateMtv)));
            List<Predicate> predicates = ajustPredicates(ajustementFilterRecord, cb, root);
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
            TypedQuery<Ajust> q = em.createQuery(cq);
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
            list = q.getResultList().stream().map(e -> new AjustDTO(e).setAjustements(items(e.getId()))).collect(Collectors.toList());
        }
        return new PageImpl<>(list, pageable, total);
    }

    private List<AjustementDTO> items(Long id) {
        try {
            TypedQuery<Ajustement> q = em.createQuery(
                "SELECT o FROM Ajustement o WHERE o.ajust.id=?1 ORDER BY o.produit.fournisseurProduitPrincipal.codeCip",
                Ajustement.class
            );
            q.setParameter(1, id);
            return q.getResultList().stream().map(AjustementDTO::new).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private long findAllCount(AjustementFilterRecord ajustementFilterRecord) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ajustement> root = cq.from(Ajustement.class);
        cq.select(cb.countDistinct(root.get(Ajustement_.ajust)));
        List<Predicate> predicates = ajustPredicates(ajustementFilterRecord, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private List<Predicate> ajustPredicates(AjustementFilterRecord ajustementFilterRecord, CriteriaBuilder cb, Root<Ajustement> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.hasLength(ajustementFilterRecord.search())) {
            String search = ajustementFilterRecord.search() + "%";
            Join<Ajustement, Produit> produitJoin = root.join(Ajustement_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(
                cb.or(
                    cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), search),
                    cb.like(cb.upper(produitJoin.get(Produit_.libelle)), search),
                    cb.like(cb.upper(produitJoin.get(Produit_.codeEanLaboratoire)), search),
                    cb.like(cb.upper(fp.get(FournisseurProduit_.codeEan)), search)
                )
            );
        }
        if (Objects.nonNull(ajustementFilterRecord.userId())) {
            predicates.add(cb.equal(root.get(Ajustement_.ajust).get(Ajust_.user).get(AppUser_.id), ajustementFilterRecord.userId()));
        }
        predicates.add(cb.equal(root.get(Ajustement_.ajust).get(Ajust_.statut), ajustementFilterRecord.statut()));
        predicates.add(
            cb.between(
                cb.function("DATE", Date.class, root.get(Ajustement_.ajust).get(Ajust_.dateMtv)),
                java.sql.Date.valueOf(ajustementFilterRecord.fromDate()),
                java.sql.Date.valueOf(ajustementFilterRecord.toDate())
            )
        );

        return predicates;
    }

    private Ajust findbyId(Long id) {
        return this.em.find(Ajust.class, id);
    }

    public Resource exportToPdf(Long id) throws IOException {
        return this.getResource(this.ajustementReportService.print(findbyId(id)));
    }

    public Optional<AjustDTO> getOneById(Long id) {
        Ajust ajust = findbyId(id);
        if (ajust.getStatut() == AjustementStatut.CLOSED) {
            return Optional.empty();
        }
        return Optional.of(
            new AjustDTO(ajust).setAjustements(
                ajust
                    .getAjustements()
                    .stream()
                    .map(AjustementDTO::new)
                    .sorted(Comparator.comparing(AjustementDTO::getCodeCip))
                    .collect(Collectors.toList())
            )
        );
    }
}

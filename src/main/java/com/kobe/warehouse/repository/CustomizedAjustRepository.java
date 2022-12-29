package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajust_;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Ajustement_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Repository
@Transactional(readOnly = true)
public class CustomizedAjustRepository implements AjustService {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<AjustDTO> loadAll(String search, LocalDate dtStart, LocalDate dtEnd, Pageable pageable) {
        long total = findAllCount(search, dtStart, dtEnd);
        List<AjustDTO> list = new ArrayList<>();
        if (total > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Ajust> cq = cb.createQuery(Ajust.class);
            Root<Ajustement> root = cq.from(Ajustement.class);
            cq.select(root.get(Ajustement_.ajust)).distinct(true).orderBy(cb.desc(root.get(Ajustement_.ajust).get(Ajust_.dateMtv)));
            List<Predicate> predicates = ajustPredicates(search, dtStart, dtEnd, cb, root);
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
            TypedQuery<Ajustement> q = em.createQuery("SELECT o FROM Ajustement o WHERE o.ajust.id=?1", Ajustement.class);
            q.setParameter(1, id);
            return q.getResultList().stream().map(AjustementDTO::new).collect(Collectors.toList());
        } catch (Exception e) {

            return Collections.emptyList();
        }
    }

    private long findAllCount(String search, LocalDate dtStart, LocalDate dtEnd) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ajustement> root = cq.from(Ajustement.class);
        cq.select(cb.countDistinct(root.get(Ajustement_.ajust)));
        List<Predicate> predicates = ajustPredicates(search, dtStart, dtEnd, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;

    }

    private List<Predicate> ajustPredicates(String search, LocalDate dtStart, LocalDate dtEnd, CriteriaBuilder cb, Root<Ajustement> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(search)) {
            search = search + "%";
            Join<Ajustement, Produit> produitJoin = root.join(Ajustement_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(produitJoin.get(Produit_.libelle)), search),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), search),
                cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), search)));
        }
        predicates.add(cb.equal(root.get(Ajustement_.ajust).get(Ajust_.statut), SalesStatut.CLOSED));
        predicates.add(cb.between(cb.function("DATE", Date.class, root.get(Ajustement_.ajust).get(Ajust_.dateMtv)),
            java.sql.Date.valueOf(dtStart), java.sql.Date.valueOf(dtEnd)));

        return predicates;
    }
}

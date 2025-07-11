package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
@Transactional(readOnly = true)
public class CustomizedCommandeRepository implements CustomizedCommandeService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Commande> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
        if (!StringUtils.hasText(commandeFilterDTO.getSearch())) {
            return fetchCommandesWithoutOrderLine(commandeFilterDTO, pageable);
        }
        return fetchCommandesWithOrderLine(commandeFilterDTO, pageable);
    }

    @Override
    public long countfetchCommandes(CommandeFilterDTO commandeFilterDTO) {
        if (!StringUtils.hasText(commandeFilterDTO.getSearch())) {
            return countfetchCommandesWithoutOrderLine(commandeFilterDTO);
        }
        return countFetchCommandesWithOrderLineCriteria(commandeFilterDTO);
    }

    public long countfetchCommandesWithoutOrderLine(CommandeFilterDTO commandeFilterDTO) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Commande> root = cq.from(Commande.class);
        cq.select(cb.count(root));
        List<Predicate> predicates = predicatesFetchCommandes(commandeFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private long countFetchCommandesWithOrderLineCriteria(CommandeFilterDTO commandeFilterDTO) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<OrderLine> root = cq.from(OrderLine.class);
        cq.select(cb.countDistinct(root.get(OrderLine_.commande)));
        List<Predicate> predicates = predicatesFetchCommandesWithOrderLineCriteria(commandeFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private List<Commande> fetchCommandesWithOrderLine(CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Commande> cq = cb.createQuery(Commande.class);
        Root<OrderLine> root = cq.from(OrderLine.class);
        cq.select(root.get(OrderLine_.commande)).distinct(true).orderBy(cb.desc(root.get(OrderLine_.commande).get(Commande_.updatedAt)));
        List<Predicate> predicates = predicatesFetchCommandesWithOrderLineCriteria(commandeFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Commande> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return q.getResultList();
    }

    private List<Commande> fetchCommandesWithoutOrderLine(CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Commande> cq = cb.createQuery(Commande.class);
        Root<Commande> root = cq.from(Commande.class);
        cq.select(root).orderBy(cb.desc(root.get(Commande_.updatedAt)));
        List<Predicate> predicates = predicatesFetchCommandes(commandeFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Commande> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return q.getResultList();
    }

    private List<Predicate> predicatesFetchCommandesWithOrderLineCriteria(
        CommandeFilterDTO commandeFilterDTO,
        CriteriaBuilder cb,
        Root<OrderLine> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (!CollectionUtils.isEmpty(commandeFilterDTO.getOrderStatuts())) {
            predicates.add(root.get(OrderLine_.commande).get(Commande_.orderStatus).in(commandeFilterDTO.getOrderStatuts()));
        }

        if (StringUtils.hasText(commandeFilterDTO.getSearchCommande())) {
            String searchCommande = commandeFilterDTO.getSearchCommande().toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(OrderLine_.commande).get(Commande_.orderReference)), searchCommande),
                    cb.like(cb.upper(root.get(OrderLine_.commande).get(Commande_.fournisseur).get(Fournisseur_.libelle)), searchCommande)
                )
            );
        }
        if (StringUtils.hasText(commandeFilterDTO.getSearch())) {
            String search = commandeFilterDTO.getSearch().toUpperCase() + "%";

            predicates.add(
                cb.or(
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)),
                        search
                    ),
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEan)),
                        search
                    ),
                    cb.like(cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.codeCip)), search)
                )
            );
        }
        return predicates;
    }

    private List<Predicate> predicatesFetchCommandes(CommandeFilterDTO commandeFilterDTO, CriteriaBuilder cb, Root<Commande> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (!CollectionUtils.isEmpty(commandeFilterDTO.getOrderStatuts())) {
            predicates.add(root.get(Commande_.orderStatus).in(commandeFilterDTO.getOrderStatuts()));
        }

        if (StringUtils.hasText(commandeFilterDTO.getSearchCommande())) {
            String searchCommande = commandeFilterDTO.getSearchCommande().toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(Commande_.orderReference)), searchCommande),
                    cb.like(cb.upper(root.get(Commande_.fournisseur).get(Fournisseur_.libelle)), searchCommande)
                )
            );
        }

        return predicates;
    }
}

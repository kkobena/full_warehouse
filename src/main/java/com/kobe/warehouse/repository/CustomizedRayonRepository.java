package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.Storage_;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.RayonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Transactional
public class CustomizedRayonRepository implements CustomizedRayonService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private StorageService storageService;

    @Autowired
    private RayonRepository rayonRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RayonDTO> listRayonsByStorageId(Long storageId, String query, Pageable pageable) {
        long total = findAllCount(storageId, query);
        List<RayonDTO> list = new ArrayList<>();
        if (total > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Rayon> cq = cb.createQuery(Rayon.class);
            Root<Rayon> root = cq.from(Rayon.class);
            cq.select(root);
            List<Predicate> predicates = produitPredicate(cb, root, storageId, query);
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
            TypedQuery<Rayon> q = em.createQuery(cq);
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
            list = q.getResultList().stream().map(o -> buildRayonDTOFromRayon(o)).collect(Collectors.toList());
        }
        return new PageImpl<>(list, pageable, total);
    }

    private long findAllCount(Long storageId, String query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Rayon> root = cq.from(Rayon.class);
        cq.select(cb.count(root));
        List<Predicate> predicates = produitPredicate(cb, root, storageId, query);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private List<Predicate> produitPredicate(CriteriaBuilder cb, Root<Rayon> root, Long storageId, String query) {
        List<Predicate> predicates = new ArrayList<>();

        if (!StringUtils.isEmpty(query)) {
            String search = query.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get(Rayon_.libelle)), search), cb.like(cb.upper(root.get(Rayon_.code)), search)));
        }
        predicates.add(cb.equal(root.get(Rayon_.storage).get(Storage_.id), storageId));

        return predicates;
    }

    @Override
    public RayonDTO save(RayonDTO dto) {
        return new RayonDTO(em.merge(buildRayonFromRayonDTO(dto, storageService.getDefaultConnectedUserMainStorage())));
    }

    @Override
    public RayonDTO update(RayonDTO dto) {
        Rayon rayon = rayonRepository.getReferenceById(dto.getId());
        return new RayonDTO(em.merge(buildRayonFromRayonDTO(dto, rayon)));
    }
}

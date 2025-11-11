package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.AssuredCustomer_;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ClientTiersPayant_;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CustomerDataService {

    private final Logger LOG = LoggerFactory.getLogger(CustomerDataService.class);
    private final CustomerRepository customerRepository;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final EntityManager entityManager;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;

    public CustomerDataService(
        CustomerRepository customerRepository,
        AssuredCustomerRepository assuredCustomerRepository,
        EntityManager entityManager,
        ClientTiersPayantRepository clientTiersPayantRepository,
        UninsuredCustomerRepository uninsuredCustomerRepository
    ) {
        this.customerRepository = customerRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.entityManager = entityManager;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
    }

    public Page<CustomerDTO> fetchAllCustomers(String categorie, String search, Status status, Pageable pageable) {
        if (!StringUtils.hasLength(categorie) || categorie.equalsIgnoreCase(EntityConstant.TOUT)) {
            return loadAll(search, status, pageable);
        }
        if (categorie.equalsIgnoreCase(EntityConstant.ASSURE) || EntityConstant.CARNET.equalsIgnoreCase(categorie)  ) {
            return loadAllAsuredCustomers(search, status, pageable);
        }
        return loadAllUninsuredCustomers(search, status, pageable);
    }

    public Page<CustomerDTO> loadAllUninsuredCustomers(String search, Status status, Pageable pageable) {
        Pageable page = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, "firstName", "lastName")
        );
        Specification<UninsuredCustomer> specification =uninsuredCustomerRepository.specialisation(status);
        if (StringUtils.hasLength(search)) {
            specification = uninsuredCustomerRepository.specialisationQueryString(search.toUpperCase() + "%");
        }
        return uninsuredCustomerRepository.findAll(specification, page).map(UninsuredCustomerDTO::new);
    }

    public Page<CustomerDTO> loadAll(String search, Status status, Pageable pageable) {
        long count = countAllCustomer(search, status);
        if (count == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        predicatsAll(search, status, predicates, cb, root);
        cq.select(root).distinct(true).orderBy(cb.asc(root.get(Customer_.firstName)), cb.asc(root.get(Customer_.lastName)));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = entityManager.createQuery(cq);
        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }
        List<Customer> customers = q.getResultList();
        return new PageImpl<>(customers.stream().map(this::mapFromEntity).toList(), pageable, count);
    }

    public Page<CustomerDTO> loadAllAsuredCustomers(String search, Status status, Pageable pageable) {
        long count = countAssuredCustomer(search, status);
        if (count == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, status, predicates, cb, assuredCustomerRoot);
        cq
            .select(root)
            .orderBy(
                cb.asc(assuredCustomerRoot.get(AssuredCustomer_.firstName)),
                cb.asc(assuredCustomerRoot.get(AssuredCustomer_.lastName))
            ).groupBy(root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = entityManager.createQuery(cq);
        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }
        List<Customer> assuredCustomers = q.getResultList();
        return new PageImpl<>(assuredCustomers.stream().map(this::mapFromEntity).collect(Collectors.toList()), pageable, count);
    }

    private long countAssuredCustomer(String search, Status status) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, status, predicates, cb, assuredCustomerRoot);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = entityManager.createQuery(cq);
        return q.getSingleResult();
    }

    private long countAllCustomer(String search, Status status) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Customer> root = cq.from(Customer.class);
        predicatsAll(search, status, predicates, cb, root);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = entityManager.createQuery(cq);
        return q.getSingleResult();
    }

    private void predicatsAll(String search, Status status, List<Predicate> predicates, CriteriaBuilder cb, Root<Customer> root) {
        predicates.add(cb.equal(root.get(Customer_.status), status));
        predicates.add(cb.equal(root.get(Customer_.typeAssure), TypeAssure.PRINCIPAL));
        if (StringUtils.hasLength(search)) {
            Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
            SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = assuredCustomerRoot.joinSet(
                AssuredCustomer_.CLIENT_TIERS_PAYANTS,
                JoinType.LEFT
            );
            String queryValue = search.toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(Customer_.firstName)), queryValue),
                    cb.like(cb.upper(root.get(Customer_.lastName)), queryValue),
                    cb.like(cb.upper(root.get(Customer_.code)), queryValue),
                    cb.like(cb.upper(cb.concat(cb.concat(root.get(Customer_.firstName), " "), root.get(Customer_.lastName))), queryValue),
                    cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                    cb.like(cb.upper(root.get(Customer_.phone)), queryValue)
                )
            );
        }
    }

    private void predicatsAssuredCustomer(
        String search,
        Status status,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<AssuredCustomer> root
    ) {
        predicates.add(cb.equal(root.get(AssuredCustomer_.status), status));
        predicates.add(cb.isNull(root.get(AssuredCustomer_.assurePrincipal)));
        if (StringUtils.hasLength(search)) {
            SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = root.joinSet(AssuredCustomer_.CLIENT_TIERS_PAYANTS);
            String queryValue = search.toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(AssuredCustomer_.firstName)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.lastName)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.code)), queryValue),
                    cb.like(
                        cb.upper(cb.concat(cb.concat(root.get(AssuredCustomer_.firstName), " "), root.get(AssuredCustomer_.lastName))),
                        queryValue
                    ),
                    cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.phone)), queryValue)
                )
            );
        }
    }

    public AssuredCustomerDTO mapAssuredFromEntity(Customer customer) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = clientTiersPayantRepository
            .findAllByAssuredCustomerId(customer.getId())
            .stream()
            .map(ClientTiersPayantDTO::new)
            .toList();
        List<AssuredCustomerDTO> ayantDroits = assuredCustomerRepository
            .findAllByAssurePrincipalId(customer.getId())
            .stream()
            .map(assuredCustomer -> new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList()))
            .toList();
        return new AssuredCustomerDTO((AssuredCustomer) customer, clientTiersPayantDTOS, ayantDroits);
    }

    public CustomerDTO mapFromEntity(Customer customer) {
        if (customer instanceof AssuredCustomer assuredCust) {
            List<ClientTiersPayantDTO> clientTiersPayantDTOS = clientTiersPayantRepository
                .findAllByAssuredCustomerId(customer.getId())
                .stream()
                .map(ClientTiersPayantDTO::new)
                .toList();
            List<AssuredCustomerDTO> ayantDroits = assuredCustomerRepository
                .findAllByAssurePrincipalId(customer.getId())
                .stream()
                .map(assuredCustomer -> new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList()))
                .toList();
            return new AssuredCustomerDTO(assuredCust, clientTiersPayantDTOS, ayantDroits);
        }
        return new UninsuredCustomerDTO((UninsuredCustomer) customer);
    }

    public Optional<CustomerDTO> getOneCustomer(Integer id) {
        return customerRepository.findById(id).map(this::mapFromEntity);
    }

    public Page<AssuredCustomerDTO> loadAllAsuredCustomers(String search, TiersPayantCategorie tiersPayantCategorie, Pageable pageable) {
        long count = count(search, tiersPayantCategorie);
        if (count == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, tiersPayantCategorie, predicates, cb, assuredCustomerRoot);
        cq
            .select(root)
            .orderBy(
                cb.asc(assuredCustomerRoot.get(AssuredCustomer_.firstName)),
                cb.asc(assuredCustomerRoot.get(AssuredCustomer_.lastName))
            ).groupBy(root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = entityManager.createQuery(cq);
        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }
        List<Customer> assuredCustomers = q.getResultList();
        if (Objects.nonNull(tiersPayantCategorie)) {
            return new PageImpl<>(
                assuredCustomers.stream().map(e -> mapAssuredFromEntity(e, tiersPayantCategorie)).collect(Collectors.toList()),
                pageable,
                count
            );
        }
        return new PageImpl<>(assuredCustomers.stream().map(this::mapAssuredFromEntity).collect(Collectors.toList()), pageable, count);
    }

    private void predicatsAssuredCustomer(
        String search,
        TiersPayantCategorie typeTiersPayant,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<AssuredCustomer> root
    ) {
        predicates.add(cb.equal(root.get(AssuredCustomer_.status), Status.ENABLE));
        predicates.add(cb.isNull(root.get(AssuredCustomer_.assurePrincipal)));
        SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = root.joinSet(AssuredCustomer_.CLIENT_TIERS_PAYANTS);
        if (StringUtils.hasLength(search)) {
            String queryValue = search.toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(AssuredCustomer_.firstName)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.lastName)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.code)), queryValue),
                    cb.like(
                        cb.upper(cb.concat(cb.concat(root.get(AssuredCustomer_.firstName), " "), root.get(AssuredCustomer_.lastName))),
                        queryValue
                    ),
                    cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                    cb.like(cb.upper(root.get(AssuredCustomer_.phone)), queryValue)
                )
            );
        }
        if (typeTiersPayant != null) {
            predicates.add(cb.equal(tiersPayantSetJoin.get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.categorie), typeTiersPayant));
        }
    }

    public List<ClientTiersPayantDTO> fetchCustomersTiersPayant(Integer id) {
        return clientTiersPayantRepository
            .findAllByAssuredCustomerId(id)
            .stream()
            .map(ClientTiersPayantDTO::new)
            .sorted(Comparator.comparing(ClientTiersPayantDTO::getCategorie))
            .collect(Collectors.toList());
    }

    public List<AssuredCustomerDTO> fetchAyantDroit(Integer id) {
        return assuredCustomerRepository
            .findAllByAssurePrincipalId(id)
            .stream()
            .map(AssuredCustomerDTO::new)
            .sorted(Comparator.comparing(AssuredCustomerDTO::getFullName))
            .collect(Collectors.toList());
    }

    private long count(String search, TiersPayantCategorie typeTiersPayant) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, typeTiersPayant, predicates, cb, assuredCustomerRoot);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = entityManager.createQuery(cq);
        return q.getSingleResult();
    }

    public AssuredCustomerDTO mapAssuredFromEntity(Customer customer, TiersPayantCategorie tiersPayantCategorie) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = clientTiersPayantRepository
            .findAllByAssuredCustomerIdAndTiersPayantCategorie(customer.getId(), tiersPayantCategorie)
            .stream()
            .map(ClientTiersPayantDTO::new)
            .toList();
        List<AssuredCustomerDTO> ayantDroits = assuredCustomerRepository
            .findAllByAssurePrincipalId(customer.getId())
            .stream()
            .map(assuredCustomer -> new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList()))
            .toList();
        return new AssuredCustomerDTO((AssuredCustomer) customer, clientTiersPayantDTOS, ayantDroits);
    }
}

package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerDataService {
    private final CustomerRepository customerRepository;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final EntityManager entityManager;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;

    public CustomerDataService(CustomerRepository customerRepository, AssuredCustomerRepository assuredCustomerRepository, EntityManager entityManager, ClientTiersPayantRepository clientTiersPayantRepository, UninsuredCustomerRepository uninsuredCustomerRepository) {
        this.customerRepository = customerRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.entityManager = entityManager;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
    }


    public Page<CustomerDTO> fetchAllCustomers(String categorie, String search, Status status, Pageable pageable) {
        if (StringUtils.isEmpty(categorie) || categorie.equalsIgnoreCase(EntityConstant.TOUT)) {
            return loadAll(search, status, pageable);
        }
        if (categorie.equalsIgnoreCase(EntityConstant.ASSURE)) {
            return loadAllAsuredCustomers(search, status, pageable);
        }
        return loadAllUninsuredCustomers(search, status, pageable);
    }


    public Page<CustomerDTO> loadAllUninsuredCustomers(String search, Status status, Pageable pageable) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, "firstName", "lastName"));
        Specification<UninsuredCustomer> specification = Specification.where(this.uninsuredCustomerRepository.specialisation(status));
        if (!StringUtils.isEmpty(search)) {
            specification = this.uninsuredCustomerRepository.specialisationQueryString(search.toUpperCase() + "%");

        }
        return this.uninsuredCustomerRepository.findAll(specification, page).map(CustomerDTO::new);
    }

    public Page<CustomerDTO> loadAll(String search, Status status, Pageable pageable) {
        long count = countAllCustomer(search, status);
        if (count == 0) return new PageImpl<>(Collections.emptyList(), pageable, count);
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        predicatsAll(search, status, predicates, cb, root);
        cq.select(root).distinct(true).orderBy(cb.asc(root.get(Customer_.firstName)), cb.asc(root.get(Customer_.lastName)));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = this.entityManager.createQuery(cq);
        if (pageable != null) {
            q.setFirstResult(pageable.getPageNumber());
            q.setMaxResults(pageable.getPageSize());
        }
        List<Customer> customers = q.getResultList();
        return new PageImpl<>(customers.stream().map(this::mapFromEntity).collect(Collectors.toList()), pageable, count);
    }

    public Page<CustomerDTO> loadAllAsuredCustomers(String search, Status status, Pageable pageable) {
        long count = countAssuredCustomer(search, status);
        if (count == 0) return new PageImpl<>(Collections.emptyList(), pageable, count);
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, status, predicates, cb, assuredCustomerRoot);
        cq.select(root).distinct(true).orderBy(cb.asc(assuredCustomerRoot.get(AssuredCustomer_.firstName)), cb.asc(assuredCustomerRoot.get(AssuredCustomer_.lastName)));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = this.entityManager.createQuery(cq);
        if (pageable != null) {
            q.setFirstResult(pageable.getPageNumber());
            q.setMaxResults(pageable.getPageSize());
        }
        List<Customer> assuredCustomers = q.getResultList();
        return new PageImpl<>(assuredCustomers.stream().map(this::mapFromEntity).collect(Collectors.toList()), pageable, count);
    }

    private long countAssuredCustomer(String search, Status status) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, status, predicates, cb, assuredCustomerRoot);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = this.entityManager.createQuery(cq);
        return q.getSingleResult();
    }

    private long countAllCustomer(String search, Status status) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Customer> root = cq.from(Customer.class);
        predicatsAll(search, status, predicates, cb, root);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = this.entityManager.createQuery(cq);
        return q.getSingleResult();
    }

    private void predicatsAll(String search, Status status, List<Predicate> predicates, CriteriaBuilder cb, Root<Customer> root) {
        predicates.add(cb.equal(root.get(Customer_.status), status));
        predicates.add(cb.equal(root.get(Customer_.typeAssure), TypeAssure.PRINCIPAL));
        if (!StringUtils.isEmpty(search)) {
            Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
            SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = assuredCustomerRoot.joinSet(AssuredCustomer_.CLIENT_TIERS_PAYANTS, JoinType.LEFT);
            String queryValue = search.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get(Customer_.firstName)), queryValue), cb.like(cb.upper(root.get(Customer_.lastName)), queryValue), cb.like(cb.upper(root.get(Customer_.code)), queryValue),
                cb.like(cb.upper(cb.concat(cb.concat(root.get(Customer_.firstName), " "), root.get(Customer_.lastName))), queryValue),
                cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                cb.like(cb.upper(root.get(Customer_.phone)), queryValue)
            ));
        }
    }


    private void predicatsAssuredCustomer(String search, Status status, List<Predicate> predicates, CriteriaBuilder cb, Root<AssuredCustomer> root) {
        predicates.add(cb.equal(root.get(AssuredCustomer_.status), status));
        predicates.add(cb.isNull(root.get(AssuredCustomer_.assurePrincipal)));
        if (!StringUtils.isEmpty(search)) {
            SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = root.joinSet(AssuredCustomer_.CLIENT_TIERS_PAYANTS);
            String queryValue = search.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get(AssuredCustomer_.firstName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.lastName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.code)), queryValue),
                cb.like(cb.upper(cb.concat(cb.concat(root.get(AssuredCustomer_.firstName), " "), root.get(AssuredCustomer_.lastName))), queryValue),
                cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                cb.like(cb.upper(root.get(AssuredCustomer_.phone)), queryValue)
            ));
        }
    }

    public AssuredCustomerDTO mapAssuredFromEntity(Customer customer) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = this.clientTiersPayantRepository.
            findAllByAssuredCustomerId(customer.getId()).stream().map(ClientTiersPayantDTO::new).collect(Collectors.toList());
        List<AssuredCustomerDTO> ayantDroits = this.assuredCustomerRepository.findAllByAssurePrincipalId(customer.getId()).stream().map(assuredCustomer -> new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList())).collect(Collectors.toList());
        return new AssuredCustomerDTO((AssuredCustomer) customer, clientTiersPayantDTOS, ayantDroits);


    }

    public CustomerDTO mapFromEntity(Customer customer) {
        if (customer instanceof AssuredCustomer) {
            List<ClientTiersPayantDTO> clientTiersPayantDTOS = this.clientTiersPayantRepository.
                findAllByAssuredCustomerId(customer.getId()).stream().map(ClientTiersPayantDTO::new).collect(Collectors.toList());
            List<AssuredCustomerDTO> ayantDroits = this.assuredCustomerRepository.findAllByAssurePrincipalId(customer.getId()).stream().map(assuredCustomer -> new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList())).collect(Collectors.toList());
            return new AssuredCustomerDTO((AssuredCustomer) customer, clientTiersPayantDTOS, ayantDroits);
        }
        return new CustomerDTO(customer);
    }

    public Optional<CustomerDTO> getOneCustomer(Long id) {
        return customerRepository.findById(id).map(this::mapFromEntity);
    }

    public List<AssuredCustomerDTO> loadAllAsuredCustomers(String search) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        Root<AssuredCustomer> assuredCustomerRoot = cb.treat(root, AssuredCustomer.class);
        predicatsAssuredCustomer(search, predicates, cb, assuredCustomerRoot);
        cq.select(root).distinct(true).orderBy(cb.asc(assuredCustomerRoot.get(AssuredCustomer_.firstName)), cb.asc(assuredCustomerRoot.get(AssuredCustomer_.lastName)));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Customer> q = this.entityManager.createQuery(cq);
        List<Customer> assuredCustomers = q.getResultList();
        return assuredCustomers.stream().map(this::mapAssuredFromEntity).collect(Collectors.toList());
    }

    private void predicatsAssuredCustomer(String search, List<Predicate> predicates, CriteriaBuilder cb, Root<AssuredCustomer> root) {
        predicates.add(cb.equal(root.get(AssuredCustomer_.status), Status.ENABLE));
        predicates.add(cb.isNull(root.get(AssuredCustomer_.assurePrincipal)));
        SetJoin<AssuredCustomer, ClientTiersPayant> tiersPayantSetJoin = root.joinSet(AssuredCustomer_.CLIENT_TIERS_PAYANTS);
        if (!StringUtils.isEmpty(search)) {
            String queryValue = search.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get(AssuredCustomer_.firstName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.lastName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.code)), queryValue),
                cb.like(cb.upper(cb.concat(cb.concat(root.get(AssuredCustomer_.firstName), " "), root.get(AssuredCustomer_.lastName))), queryValue),
                cb.like(cb.upper(tiersPayantSetJoin.get(ClientTiersPayant_.num)), queryValue),
                cb.like(cb.upper(root.get(AssuredCustomer_.phone)), queryValue)
            ));
        }
    }

    public List<ClientTiersPayantDTO> fetchCustomersTiersPayant(Long id) {
        return this.clientTiersPayantRepository.findAllByAssuredCustomerId(id).stream().map(ClientTiersPayantDTO::new)
            .sorted(Comparator.comparing(ClientTiersPayantDTO::getCategorie)).collect(Collectors.toList());
    }

    public List<AssuredCustomerDTO> fetchAyantDroit(Long id) {
        return this.assuredCustomerRepository.findAllByAssurePrincipalId(id).stream().map(AssuredCustomerDTO::new)
            .sorted(Comparator.comparing(AssuredCustomerDTO::getFullName)).collect(Collectors.toList());
    }
}

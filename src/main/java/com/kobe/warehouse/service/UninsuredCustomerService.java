package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.web.rest.errors.CustomerAlreadyExistException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UninsuredCustomerService {

    private final UninsuredCustomerRepository uninsuredCustomerRepository;

    public UninsuredCustomerService(UninsuredCustomerRepository uninsuredCustomerRepository) {
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
    }

    public UninsuredCustomerDTO create(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        Optional<UninsuredCustomer> uninsuredCustomerOptional = findOne(dto);
        if (uninsuredCustomerOptional.isPresent()) throw new CustomerAlreadyExistException();
        var uninsuredCustomer = new UninsuredCustomer();
        uninsuredCustomer.setCreatedAt(Instant.now());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        uninsuredCustomer.setCode(RandomStringUtils.randomNumeric(6));
        var cust = this.uninsuredCustomerRepository.save(uninsuredCustomer);
        return uninsuredCustomerFromEntity(cust);

    }

    public UninsuredCustomerDTO update(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        List<UninsuredCustomer> uninsuredCustomers = fetch(dto);
        if (uninsuredCustomers.size() > 1) throw new CustomerAlreadyExistException();
        var uninsuredCustomer = this.uninsuredCustomerRepository.getOne(dto.getId());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        var cust = this.uninsuredCustomerRepository.save(uninsuredCustomer);
        return uninsuredCustomerFromEntity(cust);

    }

    private UninsuredCustomerDTO uninsuredCustomerFromEntity(UninsuredCustomer uninsuredCustomer) {
        var customerDTO = new UninsuredCustomerDTO();
        customerDTO.setEmail(uninsuredCustomer.getEmail());
        customerDTO.setPhone(uninsuredCustomer.getPhone());
        customerDTO.setFirstName(uninsuredCustomer.getFirstName());
        customerDTO.setLastName(uninsuredCustomer.getLastName());
        customerDTO.setCode(uninsuredCustomer.getCode());
        customerDTO.setId(uninsuredCustomer.getId());
        customerDTO.setFullName(uninsuredCustomer.getFirstName() + " " + uninsuredCustomer.getLastName());
        return customerDTO;
    }

    public List<UninsuredCustomerDTO> fetch(String query) {
        Specification<UninsuredCustomer> specification = Specification.where(this.uninsuredCustomerRepository.specialisation());
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            specification = specification.and(this.uninsuredCustomerRepository.specialisationQueryString(query));

        }
        return this.uninsuredCustomerRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "firstName", "lastName")).stream().map(UninsuredCustomerDTO::new).collect(Collectors.toList());
    }

    public Optional<UninsuredCustomer> findOne(UninsuredCustomerDTO dto) {
        Specification<UninsuredCustomer> specification = Specification.where(this.uninsuredCustomerRepository.specialisationCheckExist(dto.getFirstName(), dto.getLastName(), dto.getPhone()));
        return this.uninsuredCustomerRepository.findOne(specification);
    }

    public List<UninsuredCustomer> fetch(UninsuredCustomerDTO dto) {
        Specification<UninsuredCustomer> specification = Specification.where(this.uninsuredCustomerRepository.specialisationCheckExist(dto.getFirstName(), dto.getLastName(), dto.getPhone()));
        return this.uninsuredCustomerRepository.findAll(specification);
    }
}

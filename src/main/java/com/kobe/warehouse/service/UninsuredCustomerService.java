package com.kobe.warehouse.service;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.errors.CustomerAlreadyExistException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UninsuredCustomerService {

    private final UninsuredCustomerRepository uninsuredCustomerRepository;

    public UninsuredCustomerService(UninsuredCustomerRepository uninsuredCustomerRepository) {
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
    }

    public UninsuredCustomerDTO create(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        Optional<UninsuredCustomer> uninsuredCustomerOptional = findOne(dto);
        if (uninsuredCustomerOptional.isPresent()) {
            throw new CustomerAlreadyExistException();
        }
        if (org.springframework.util.StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        var uninsuredCustomer = new UninsuredCustomer();
        uninsuredCustomer.setCreatedAt(LocalDateTime.now());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        uninsuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        uninsuredCustomer.setCode(RandomStringUtils.randomNumeric(6));
        var cust = uninsuredCustomerRepository.save(uninsuredCustomer);
        return uninsuredCustomerFromEntity(cust);
    }

    public UninsuredCustomerDTO update(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        Optional<UninsuredCustomer> uninsuredCustomerOptional = findOne(dto);
        if (uninsuredCustomerOptional.isPresent() && !Objects.equals(uninsuredCustomerOptional.get().getId(), dto.getId())) {
            throw new CustomerAlreadyExistException();
        }
        if (org.springframework.util.StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        var uninsuredCustomer = uninsuredCustomerRepository.getReferenceById(dto.getId());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        var cust = uninsuredCustomerRepository.save(uninsuredCustomer);
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
        Specification<UninsuredCustomer> specification = uninsuredCustomerRepository.specialisation(Status.ENABLE);
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            specification = specification.and(uninsuredCustomerRepository.specialisationQueryString(query));
        }
        return uninsuredCustomerRepository
            .findAll(specification, Sort.by(Sort.Direction.ASC, "firstName", "lastName"))
            .stream()
            .map(UninsuredCustomerDTO::new)
            .collect(Collectors.toList());
    }

    public Optional<UninsuredCustomer> findOne(UninsuredCustomerDTO dto) {
        Specification<UninsuredCustomer> specification =
            uninsuredCustomerRepository.specialisationCheckExist(dto.getFirstName(), dto.getLastName(), dto.getPhone())
        ;
        return uninsuredCustomerRepository.findOne(specification);
    }

    public void deleteCustomerById(Long id) throws GenericError {
        try {
            uninsuredCustomerRepository.deleteById(id);
        } catch (Exception e) {
            throw new GenericError("Impossible de supprimer ce client, Il existe des ventes qui lui sont ratachées ", "deleteCustomer");
        }
    }
}

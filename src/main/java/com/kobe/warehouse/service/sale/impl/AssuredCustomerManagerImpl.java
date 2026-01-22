package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import com.kobe.warehouse.service.sale.AssuredCustomerManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Implémentation du service de gestion des clients assurés.
 * Ce service gère toutes les opérations liées aux clients assurés.
 */
@Service
@Transactional
public class AssuredCustomerManagerImpl implements AssuredCustomerManager {

    private final AssuredCustomerRepository assuredCustomerRepository;

    public AssuredCustomerManagerImpl(AssuredCustomerRepository assuredCustomerRepository) {
        this.assuredCustomerRepository = assuredCustomerRepository;
    }

    @Override
    public Optional<AssuredCustomer> getAyantDroitFromId(Integer ayantDroitId) {
        if (ayantDroitId == null) {
            return Optional.empty();
        }
        AssuredCustomer ayantDroit = new AssuredCustomer();
        ayantDroit.setId(ayantDroitId);
        return Optional.of(ayantDroit);
    }

    @Override
    public void updateAssuredCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer)
        throws InvalidPhoneNumberException {
        if (isNull(customer)) {
            return;
        }

        assuredCustomer.setFirstName(customer.getFirstName());
        assuredCustomer.setLastName(customer.getLastName());

        if (StringUtils.hasText(customer.getPhone())) {
            if (!Util.isValidPhoneNumber(customer.getPhone())) {
                throw new InvalidPhoneNumberException();
            }
            assuredCustomer.setPhone(customer.getPhone());
        }

        if (StringUtils.hasLength(customer.getNumAyantDroit())) {
            assuredCustomer.setNumAyantDroit(customer.getNumAyantDroit());
        }

        this.assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public boolean isSameCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer) {
        return assuredCustomer.getId().compareTo(customer.getId()) == 0;
    }
}

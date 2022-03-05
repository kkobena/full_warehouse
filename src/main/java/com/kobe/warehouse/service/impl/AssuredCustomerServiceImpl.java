package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssuredCustomerServiceImpl implements AssuredCustomerService {
    private final AssuredCustomerRepository assuredCustomerRepository;

    public AssuredCustomerServiceImpl(AssuredCustomerRepository assuredCustomerRepository) {
        this.assuredCustomerRepository = assuredCustomerRepository;
    }

    @Override
    public void createFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = this.fromDto(dto);
        this.clientTiersPayantFromDto(dto.getTiersPayants(), assuredCustomer);
        this.ayantDroitsFromDto(dto.getAyantDroits(), assuredCustomer);
        this.assuredCustomerRepository.save(assuredCustomer);

    }

    @Override
    public void updateFromDto(AssuredCustomerDTO dto) {

    }

    @Override
    public void delete(Long id) {
        this.assuredCustomerRepository.deleteById(id);
    }

    @Override
    public void desable(Long id) {
        AssuredCustomer assuredCustomer = this.assuredCustomerRepository.getOne(id);
        assuredCustomer.setStatus(Status.DISABLE);
        this.assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public void createAyantDroitFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = this.assuredCustomerRepository.getOne(dto.getAssureId());
        AssuredCustomer ayantDroit = this.fromDto(dto);
        ayantDroit.setAssurePrincipal(assuredCustomer);
        this.assuredCustomerRepository.save(ayantDroit);
    }

    @Override
    public void updateAyantDroitFromDto(AssuredCustomerDTO dto) {

    }
}

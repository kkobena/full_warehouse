package com.kobe.warehouse.service.dci.service;

import com.kobe.warehouse.repository.DciRepository;
import com.kobe.warehouse.service.dci.dto.DciDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DciServiceImpl implements DciService {

    private final DciRepository dciRepository;

    public DciServiceImpl(DciRepository dciRepository) {
        this.dciRepository = dciRepository;
    }

    @Override
    public Page<DciDTO> findAll(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return this.dciRepository.findAllByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCaseOrderByLibelleAsc(
                    search,
                    search,
                    pageable
                ).map(DciDTO::new);
        }
        return this.dciRepository.findAllByOrderByLibelleAsc(pageable).map(DciDTO::new);
    }
}

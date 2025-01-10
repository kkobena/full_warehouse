package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.repository.RemiseClientRepository;
import com.kobe.warehouse.repository.RemiseProduitRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.service.dto.RemiseClientDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.RemiseProduitDTO;
import com.kobe.warehouse.service.dto.TypeRemise;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemiseServiceImpl implements RemiseService {

    private final RemiseRepository remiseRepository;
    private final RemiseProduitRepository remiseProduitRepository;
    private final RemiseClientRepository remiseClientRepository;

    public RemiseServiceImpl(
        RemiseRepository remiseRepository,
        RemiseProduitRepository remiseProduitRepository,
        RemiseClientRepository remiseClientRepository
    ) {
        this.remiseRepository = remiseRepository;
        this.remiseProduitRepository = remiseProduitRepository;
        this.remiseClientRepository = remiseClientRepository;
    }

    @Override
    public RemiseDTO save(RemiseDTO remiseDTO) {
        return toDTO(this.remiseRepository.save(toEntity(remiseDTO)));
    }

    @Override
    public RemiseDTO changeStatus(RemiseDTO remiseDTO) {
        Remise remise = toEntity(remiseDTO);
        remise.setEnable(remiseDTO.isEnable());
        return toDTO(this.remiseRepository.save(remise));
    }

    @Override
    public Optional<RemiseDTO> findOne(Long id) {
        return Optional.of(toDTO(this.remiseRepository.findById(id).orElse(null)));
    }

    @Override
    public void delete(Long id) {
        this.remiseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RemiseDTO> findAll(TypeRemise typeRemise) {
        if (typeRemise == TypeRemise.PRODUIT) {
            return this.remiseProduitRepository.findAll().stream().map(this::toDTO).toList();
        } else if (typeRemise == TypeRemise.CLIENT) {
            return this.remiseClientRepository.findAll().stream().map(this::toDTO).toList();
        }
        return this.remiseRepository.findAll().stream().map(this::toDTO).toList();
    }

    private Remise toEntity(RemiseDTO remiseDTO) {
        if (remiseDTO instanceof RemiseProduitDTO) {
            return toEntity(remiseDTO, new RemiseProduit());
        }
        return toEntity(remiseDTO, new RemiseClient());
    }

    private Remise toEntity(RemiseDTO remiseDTO, RemiseProduit remise) {
        remise.setValeur(remiseDTO.getValeur());
        if (remiseDTO.getId() != null) {
            remise.setId(remiseDTO.getId());
        }
        return remise;
    }

    private Remise toEntity(RemiseDTO remiseDTO, RemiseClient remise) {
        remise.setRemiseValue(remiseDTO.getRemiseValue());
        remise.setValeur(remiseDTO.getValeur());
        if (remiseDTO.getId() != null) {
            remise.setId(remiseDTO.getId());
        }
        return remise;
    }

    private RemiseDTO toDTO(Remise remise) {
        if (remise instanceof RemiseProduit) {
            return new RemiseProduitDTO((RemiseProduit) remise);
        } else {
            return new RemiseClientDTO((RemiseClient) remise);
        }
    }
}

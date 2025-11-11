package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.GrilleRemiseRepository;
import com.kobe.warehouse.repository.RemiseClientRepository;
import com.kobe.warehouse.repository.RemiseProduitRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.service.dto.GrilleRemiseDTO;
import com.kobe.warehouse.service.dto.CodeRemiseDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.RemiseClientDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.RemiseProduitDTO;
import com.kobe.warehouse.service.dto.RemiseProduitsDTO;
import com.kobe.warehouse.service.dto.TypeRemise;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class RemiseServiceImpl implements RemiseService {

    private final RemiseRepository remiseRepository;
    private final RemiseProduitRepository remiseProduitRepository;
    private final RemiseClientRepository remiseClientRepository;
    private final GrilleRemiseRepository grilleRemiseRepository;
    private final CustomizedProductService customizedProductService;

    public RemiseServiceImpl(
        RemiseRepository remiseRepository,
        RemiseProduitRepository remiseProduitRepository,
        RemiseClientRepository remiseClientRepository,
        GrilleRemiseRepository grilleRemiseRepository,
        CustomizedProductService customizedProductService
    ) {
        this.remiseRepository = remiseRepository;
        this.remiseProduitRepository = remiseProduitRepository;
        this.remiseClientRepository = remiseClientRepository;
        this.grilleRemiseRepository = grilleRemiseRepository;

        this.customizedProductService = customizedProductService;
    }

    @Override
    public RemiseDTO save(RemiseDTO remiseDTO) {
        if (remiseDTO.getId() != null) {
            this.grilleRemiseRepository.deleteByRemiseProduitId(remiseDTO.getId());
        }

        return toDTO(this.remiseRepository.save(toEntity(remiseDTO)));
    }

    @Override
    public RemiseDTO changeStatus(RemiseDTO remiseDTO) {
        Remise remise = toEntity(remiseDTO);

        return toDTO(this.remiseRepository.save(remise));
    }

    @Override
    public Optional<RemiseDTO> findOne(Integer id) {
        return Optional.of(toDTO(this.remiseRepository.findById(id).orElse(null)));
    }

    @Override
    public void delete(Integer id) {
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

    @Override
    public List<CodeRemiseDTO> findAllCodeRemise() {
        return CodeRemise.toListDTO();
    }

    @Override
    public List<CodeRemiseDTO> queryFullCodes() {
        return Arrays.stream(CodeRemise.values())
            .map(codeRemise -> {
                CodeRemiseDTO codeRemiseDTO = CodeRemise.toDTO(codeRemise);
                Map<RemiseProduit, List<GrilleRemise>> remises = grilleRemiseRepository
                    .findAllByCodeIn(List.of(codeRemise.getCodeVno(), codeRemise.getCodeVo()))
                    .stream()
                    .collect(Collectors.groupingBy(GrilleRemise::getRemiseProduit, Collectors.toList()));

                remises.forEach((remiseProduit, grilleRemises) ->
                    codeRemiseDTO.setRemise(new RemiseProduitDTO(remiseProduit, grilleRemises))
                );
                return codeRemiseDTO;
            })
            .toList();
    }

    @Override
    public List<GrilleRemiseDTO> findAllGrilles() {
        return this.grilleRemiseRepository.findAll().stream().map(GrilleRemiseDTO::new).toList();
    }

    @Override
    public void assosier(RemiseProduitsDTO remiseProduits) {
        CodeRemise codeRemise = CodeRemise.fromValue(remiseProduits.codeRemise());
        List<Produit> produits;

        if (remiseProduits.all()) {
            produits = this.customizedProductService.find(
                    new ProduitCriteria().setSearch(remiseProduits.search()).setRayonId(remiseProduits.rayonId())
                );
        } else {
            produits = this.customizedProductService.findByIds(remiseProduits.produitIds());
        }
        produits.forEach(produit -> {
            produit.setCodeRemise(codeRemise);
            try {
                this.customizedProductService.save(produit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Remise toEntity(RemiseDTO remiseDTO) {
        if (remiseDTO instanceof RemiseProduitDTO) {
            return toEntity(remiseDTO, new RemiseProduit());
        }
        return toEntity((RemiseClientDTO) remiseDTO, new RemiseClient());
    }

    private Remise toEntity(RemiseDTO remiseDTO, RemiseProduit remise) {
        remise.setValeur(remiseDTO.getValeur());
        remise.setEnable(remiseDTO.isEnable());
        if (remiseDTO.getId() != null) {
            remise.setId(remiseDTO.getId());
        }
        remise.setGrilles(toGrilleRemiseEntities(((RemiseProduitDTO) remiseDTO).getGrilles(), remise));
        return remise;
    }

    private List<GrilleRemise> toGrilleRemiseEntities(List<GrilleRemiseDTO> grilleRemises, RemiseProduit remise) {
        if (CollectionUtils.isEmpty(grilleRemises)) {
            return List.of();
        }
        return grilleRemises
            .stream()
            .map(grilleRemiseDTO ->
                new GrilleRemise()
                    .setEnable(remise.isEnable())
                    .setId(grilleRemiseDTO.getId())
                    .setRemiseValue(grilleRemiseDTO.getRemiseValue())
                    .setRemiseProduit(remise)
                    .setCode(CodeGrilleRemise.fromValue(grilleRemiseDTO.getCode()))
            )
            .toList();
    }

    private Remise toEntity(RemiseClientDTO remiseDTO, RemiseClient remise) {
        remise.setRemiseValue(remiseDTO.getRemiseValue());
        remise.setValeur(remiseDTO.getValeur());
        remise.setEnable(remiseDTO.isEnable());
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

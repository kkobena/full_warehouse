package com.kobe.warehouse.service.impl;


import com.kobe.warehouse.domain.Categorie;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.repository.CategorieRepository;
import com.kobe.warehouse.repository.FamilleProduitRepository;
import com.kobe.warehouse.service.dto.FamilleProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.referential.FamilleProduitService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FamilleProduit}.
 */
@Service
@Transactional
public class FamilleProduitServiceImpl implements FamilleProduitService {

    private final Logger log = LoggerFactory.getLogger(FamilleProduitServiceImpl.class);

    private final FamilleProduitRepository familleProduitRepository;
    private final CategorieRepository categorieRepository;

    public FamilleProduitServiceImpl(FamilleProduitRepository familleProduitRepository,
                                     CategorieRepository categorieRepository) {
        this.familleProduitRepository = familleProduitRepository;
        this.categorieRepository=categorieRepository;
    }

    /**
     * Save a familleProduit.
     *
     * @param familleProduitDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public FamilleProduitDTO save(FamilleProduitDTO familleProduitDTO) {
        log.debug("Request to save FamilleProduit : {}", familleProduitDTO);
        FamilleProduit familleProduit = new FamilleProduit(familleProduitDTO);
        familleProduit.setCategorie(fromId(familleProduitDTO.getCategorieId()));
        familleProduit = familleProduitRepository.save(familleProduit);
        return new FamilleProduitDTO(familleProduit);
    }

    /**
     * Get all the familleProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FamilleProduitDTO> findAll(String search,Pageable pageable) {

        return   familleProduitRepository.findAllByCodeOrLibelleContainingAllIgnoreCase(search,search,pageable)
            .map(FamilleProduitDTO::new);
    }


    /**
     * Get one familleProduit by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<FamilleProduitDTO> findOne(Long id) {
        log.debug("Request to get FamilleProduit : {}", id);
        return familleProduitRepository.findById(id)
            .map(FamilleProduitDTO::new);
    }

    /**
     * Delete the familleProduit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete FamilleProduit : {}", id);

        familleProduitRepository.deleteById(id);
    }

	@Override
	public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger(0);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(br);
			records.forEach(record -> {
				FamilleProduit familleProduit = new FamilleProduit();
				familleProduit.setCode(record.get(1));
				familleProduit.setLibelle(record.get(0));
				if (!StringUtils.isEmpty(record.get(2))) {
					Optional<Categorie> op = categorieRepository.findOneByLibelle(record.get(2));
					if (op.isPresent()) {
						familleProduit.setCategorie(op.get());

                    }

				}

				familleProduitRepository.save(familleProduit);
                count.incrementAndGet();
			});
		} catch (IOException e) {
			log.debug("importation : {}", e);
		}
        return new ResponseDTO().size(count.get());

	}

	private  Categorie fromId(Long categorieId){
        return  new Categorie().id(categorieId);
    }
}

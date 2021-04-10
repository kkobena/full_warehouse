package com.kobe.warehouse.web.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Produit}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ProduitResource {

    private final Logger log = LoggerFactory.getLogger(ProduitResource.class);

    private static final String ENTITY_NAME = "produit";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    private final UserRepository userRepository;
    private final ProduitRepository produitRepository;

    public ProduitResource(ProduitRepository produitRepository,
                           UserRepository userRepository
    ) {
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;

    }

    /**
     * {@code POST  /produits} : Create a new produit.
     *
     * @param produitDTO the produit to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     * body the new produit, or with status {@code 400 (Bad Request)} if the
     * produit has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/produits")
    public ResponseEntity<Produit> createProduit(@Valid @RequestBody ProduitDTO produitDTO) throws URISyntaxException {
        log.debug("REST request to save Produit : {}", produitDTO);
        if (produitDTO.getId() != null) {
            throw new BadRequestAlertException("A new produit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Produit produit = ProduitDTO.fromDTO(produitDTO);
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        if (user.isPresent()) {
            produit.setUser(user.get());
        }
        Produit result = produitRepository.save(produit);
        return ResponseEntity
            .created(new URI("/api/produits/" + result.getId())).headers(HeaderUtil
                .createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /produits} : Updates an existing produit.
     *
     * @param produit the produit to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     * the updated produit, or with status {@code 400 (Bad Request)} if the
     * produit is not valid, or with status
     * {@code 500 (Internal Server Error)} if the produit couldn't be
     * updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/produits")
    public ResponseEntity<Produit> updateProduit(@Valid @RequestBody Produit produit) throws URISyntaxException {
        log.debug("REST request to update Produit : {}", produit);
        if (produit.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Produit produit2 = produitRepository.getOne(produit.getId());
        produit2.setCostAmount(produit.getCostAmount());
        produit2.setRegularUnitPrice(produit.getRegularUnitPrice());
        produit2.setItemCostAmount(produit.getItemCostAmount());
        produit2.setItemQty(produit.getItemQty());
        produit2.setItemRegularUnitPrice(produit.getItemRegularUnitPrice());
        produit2.setLibelle(produit.getLibelle().trim().toUpperCase());
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        if (user.isPresent()) {
            produit2.setUser(user.get());
        }
        if (produit2.getTypeProduit() == TypeProduit.PACKAGE) {
            Produit detailProduit = produit2.getProduits().get(0);
            produitRepository.save(Produit.detailFromParent(produit2, detailProduit));
        }
        Produit result = produitRepository.save(produit2);
        return ResponseEntity.ok().headers(
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, produit.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /produits} : get all the produits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     * of produits in body.
     */
    @Transactional(readOnly = true)
    @GetMapping("/produits")
    public ResponseEntity<List<ProduitDTO>> getAllProduits(
        @RequestParam(name = "search", required = false, defaultValue = "") String search,
        @RequestParam(name = "withdetail", required = false, defaultValue = "false") boolean withdetail,
        @RequestParam(name = "typeProduit", required = false, defaultValue = "PACKAGE") TypeProduit typeProduit,
        Pageable pageable) {
        Page<Produit> page;
        if (StringUtils.isNotBlank(search)) {
            if (withdetail) {
                page = produitRepository.findAll(
                    produitRepository.specialisationCritereRecherche(String.format("%%%s%%", search)), pageable);
            } else {
                page = produitRepository
                    .findAll(produitRepository.specialisationCritereRecherche(String.format("%%%s%%", search))
                        .and(produitRepository.specialisationTypeProduit(typeProduit)), pageable);
            }
        } else {
            if (withdetail) {
                page = produitRepository.findAll(pageable);
            } else {
                page = produitRepository.findAll(produitRepository.specialisationTypeProduit(typeProduit), pageable);
            }
        }

        HttpHeaders headers = PaginationUtil
            .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent().stream().map(ProduitDTO::new).collect(Collectors.toList()));
    }

    /**
     * {@code GET  /produits/:id} : get the "id" produit.
     *
     * @param id the id of the produit to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     * the produit, or with status {@code 404 (Not Found)}.
     */
    @Transactional(readOnly = true)
    @GetMapping("/produits/{id}")
    public ResponseEntity<Produit> getProduit(@PathVariable Long id) {
        log.debug("REST request to get Produit : {}", id);
        Optional<Produit> produit = produitRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(produit);
    }

    /**
     * {@code DELETE  /produits/:id} : delete the "id" produit.
     *
     * @param id the id of the produit to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/produits/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        log.debug("REST request to delete Produit : {}", id);
        produitRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/produits/{id}/img")
    public ResponseEntity<Produit> uploadFile(@RequestPart("productimg") MultipartFile file,
                                              @PathVariable(name = "id", required = true) Long id) throws URISyntaxException, IOException {
        Optional<Produit> produit = produitRepository.findById(id);
        Produit result = null;
        if (produit.isPresent()) {
            result = produit.get();
            result.setImageUrl(file.getOriginalFilename());
            result.setData(Base64.getEncoder().encodeToString(file.getBytes()));
            result.setImageType(FilenameUtils.getExtension(file.getOriginalFilename()));
            produitRepository.save(result);

        }

        return ResponseEntity.ok().headers(
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

}

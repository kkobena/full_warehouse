package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.CommandeService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Commande}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class CommandeResource {

	private final Logger log = LoggerFactory.getLogger(CommandeResource.class);

	private static final String ENTITY_NAME = "commande";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;
	private final CommandeService commandeService;

	public CommandeResource(CommandeService commandeService) {
		this.commandeService = commandeService;
	}

	@GetMapping("/commandes")
	public ResponseEntity<List<Commande>> getAllCommandes(Pageable pageable) {
		log.debug("REST request to get a page of Commandes");
		Page<Commande> page =commandeService.getAllCommandes(pageable);
		HttpHeaders headers = PaginationUtil
				.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
		return ResponseEntity.ok().headers(headers).body(page.getContent());
	}

	@GetMapping("/commandes/{id}")
	public ResponseEntity<Commande> getCommande(@PathVariable Long id) {
		log.debug("REST request to get Commande : {}", id);
		Optional<Commande> commande = commandeService.getCommande(id);
		return ResponseUtil.wrapOrNotFound(commande);
	}

	@PostMapping("/commandes/set")
	public ResponseEntity<Commande> createCommande(@Valid @RequestBody Set<ProduitDTO> produitDTO)
			throws URISyntaxException {

		Commande result = commandeService.createCommande(produitDTO);
		return ResponseEntity
				.created(new URI("/api/commandes/" + result.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
				.body(result);
	}

}

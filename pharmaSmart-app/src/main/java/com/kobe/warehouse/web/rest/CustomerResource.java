package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.ImportationCustomer;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.customer.AlerteSanteDTO;
import com.kobe.warehouse.domain.enumeration.CanalConsentement;
import com.kobe.warehouse.service.customer.ConsentementDTO;
import com.kobe.warehouse.service.customer.ConsentementService;
import com.kobe.warehouse.service.customer.TraitementARenouvelerDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueSaisieDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueService;
import com.kobe.warehouse.service.customer.CustomerDocumentService;
import com.kobe.warehouse.service.customer.CustomerFicheService;
import com.kobe.warehouse.service.customer.DonneesClientDTO;
import com.kobe.warehouse.service.customer.DonneesPersonnellesClientService;
import com.kobe.warehouse.service.customer.DoublonClientGroupeDTO;
import com.kobe.warehouse.service.customer.FusionClientApercuDTO;
import com.kobe.warehouse.service.customer.FusionClientRequestDTO;
import com.kobe.warehouse.service.customer.FusionClientResultDTO;
import com.kobe.warehouse.service.customer.FusionClientService;
import org.springframework.http.MediaType;
import com.kobe.warehouse.service.customer.DerogationAlerteSanteDTO;
import com.kobe.warehouse.service.customer.DossierSanteDTO;
import com.kobe.warehouse.service.customer.DossierSanteService;
import com.kobe.warehouse.service.customer.DerogationLimiteCreditDTO;
import com.kobe.warehouse.service.customer.LimiteCreditService;
import com.kobe.warehouse.service.customer.RelanceDiffereDTO;
import com.kobe.warehouse.service.customer.RelanceDiffereService;
import com.kobe.warehouse.service.customer.SituationCreditDTO;
import com.kobe.warehouse.service.customer.CustomerSyntheseDTO;
import com.kobe.warehouse.service.customer.ProduitDelivreDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.security.navaccess.NavAccessExempt;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Customer}.
 */
@RestController
@RequestMapping("/api")
@RequiresNavAccess({ "customer", "ventes", "nouvelle-vente", "nouvelle-prevente" })
public class CustomerResource {
    private static final String ENTITY_NAME = "customer";

    private final CustomerDataService customerDataService;
    private final SaleDataService saleService;
    private final UninsuredCustomerService uninsuredCustomerService;
    private final ImportationCustomer importationCustomer;
    private final CustomerFicheService customerFicheService;
    private final DossierSanteService dossierSanteService;
    private final LimiteCreditService limiteCreditService;
    private final RelanceDiffereService relanceDiffereService;
    private final CustomerDocumentService customerDocumentService;
    private final FusionClientService fusionClientService;
    private final DonneesPersonnellesClientService donneesPersonnellesClientService;
    private final TraitementChroniqueService traitementChroniqueService;
    private final ConsentementService consentementService;


    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public CustomerResource(
        CustomerDataService customerDataService,
        SaleDataService saleService,
        UninsuredCustomerService uninsuredCustomerService,
        ImportationCustomer importationCustomer,
        CustomerFicheService customerFicheService,
        DossierSanteService dossierSanteService,
        LimiteCreditService limiteCreditService,
        RelanceDiffereService relanceDiffereService,
        CustomerDocumentService customerDocumentService,
        FusionClientService fusionClientService,
        DonneesPersonnellesClientService donneesPersonnellesClientService,
        TraitementChroniqueService traitementChroniqueService,
        ConsentementService consentementService
    ) {
        this.traitementChroniqueService = traitementChroniqueService;
        this.consentementService = consentementService;
        this.customerDocumentService = customerDocumentService;
        this.fusionClientService = fusionClientService;
        this.donneesPersonnellesClientService = donneesPersonnellesClientService;
        this.limiteCreditService = limiteCreditService;
        this.relanceDiffereService = relanceDiffereService;
        this.customerFicheService = customerFicheService;
        this.dossierSanteService = dossierSanteService;
        this.customerDataService = customerDataService;
        this.saleService = saleService;
        this.uninsuredCustomerService = uninsuredCustomerService;
        this.importationCustomer = importationCustomer;

    }

    @GetMapping("/customers")
    @NavAccessExempt("lecture du fichier clients, nécessaire à la vente pour tous les rôles de comptoir")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(
        Pageable pageable,
        @RequestParam(required = false, defaultValue = "ENABLE", name = "status") Status status,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "type", defaultValue = "TOUT") String type
    ) {
        Page<CustomerDTO> page = customerDataService.fetchAllCustomers(type, search, status, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/customers/{id}")
    @NavAccessExempt("lecture du fichier clients, nécessaire à la vente pour tous les rôles de comptoir")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Integer id) {

        return ResponseUtil.wrapOrNotFound(customerDataService.getOneCustomer(id));
    }

    @GetMapping("/customers/purchases")
    @NavAccessExempt("lecture du fichier clients, nécessaire à la vente pour tous les rôles de comptoir")
    public ResponseEntity<List<SaleDTO>> customerPurchases(
        @RequestParam(value = "customerId") Integer id,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        Pageable pageable
    ) {
        Page<SaleDTO> page = saleService.customerPurchases(id, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/customers/uninsured")
    public ResponseEntity<UninsuredCustomerDTO> createUninsuredCustomer(@Valid @RequestBody UninsuredCustomerDTO customer)
        throws URISyntaxException {

        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UninsuredCustomerDTO result = uninsuredCustomerService.create(customer);
        return ResponseEntity.created(new URI("/api/customers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/uninsured")
    public ResponseEntity<UninsuredCustomerDTO> updateUninsuredCustomer(@Valid @RequestBody UninsuredCustomerDTO uninsuredCustomerDTO) {
        if (uninsuredCustomerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        UninsuredCustomerDTO result = uninsuredCustomerService.update(uninsuredCustomerDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping("/customers/uninsured")
    @NavAccessExempt("lecture du fichier clients, nécessaire à la vente pour tous les rôles de comptoir")
    public ResponseEntity<List<UninsuredCustomerDTO>> getAllUninsuredCustomers(
        @RequestParam(value = "search", required = false) String search
    ) {

        return ResponseEntity.ok().body(uninsuredCustomerService.fetch(search));
    }

    // ── Fiche client 360° (docs/PLAN-FICHE-CLIENT.md, lot 1) : lectures bornées au client ──

    @GetMapping("/customers/{id}/synthese")
    public ResponseEntity<CustomerSyntheseDTO> getSynthese(@PathVariable Integer id) {
        return ResponseEntity.ok(customerFicheService.synthese(id));
    }

    @GetMapping("/customers/{id}/produits-delivres")
    public ResponseEntity<List<ProduitDelivreDTO>> getProduitsDelivres(
        @PathVariable Integer id,
        @RequestParam(value = "fromDate") LocalDate fromDate,
        @RequestParam(value = "toDate") LocalDate toDate,
        @RequestParam(value = "search", required = false) String search,
        Pageable pageable
    ) {
        Page<ProduitDelivreDTO> page = customerFicheService.produitsDelivres(id, fromDate, toDate, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /** Différés non soldés du client ; 204 s'il n'en a aucun. */
    @GetMapping("/customers/{id}/differes")
    public ResponseEntity<DiffereDTO> getDifferes(@PathVariable Integer id) {
        return customerFicheService.differes(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/customers/{id}/reglements-differes")
    public ResponseEntity<List<ReglementDiffereWrapperDTO>> getReglementsDifferes(
        @PathVariable Integer id,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        Pageable pageable
    ) {
        Page<ReglementDiffereWrapperDTO> page = customerFicheService.reglementsDifferes(id, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    // ── Sécurité du patient (docs/PLAN-FICHE-CLIENT.md, lot 2) ──

    @GetMapping("/customers/{id}/dossier-sante")
    public ResponseEntity<DossierSanteDTO> getDossierSante(@PathVariable Integer id) {
        return ResponseEntity.ok(dossierSanteService.dossier(id));
    }

    @PutMapping("/customers/{id}/dossier-sante")
    public ResponseEntity<DossierSanteDTO> updateDossierSante(@PathVariable Integer id, @RequestBody DossierSanteDTO dossier) {
        return ResponseEntity.ok(dossierSanteService.enregistrer(id, dossier));
    }

    /** Alertes levées par l'ajout du produit à une vente de ce client. */
    @GetMapping("/customers/{id}/alertes-sante")
    public ResponseEntity<List<AlerteSanteDTO>> getAlertesSante(@PathVariable Integer id, @RequestParam Integer produitId) {
        return ResponseEntity.ok(dossierSanteService.alertes(id, produitId));
    }

    /** Délivrance malgré une alerte bloquante : droit {@code pr-forcer-alerte-sante} ou clé d'un collègue qui le détient. */
    @PostMapping("/customers/{id}/alertes-sante/derogations")
    public ResponseEntity<Void> derogerAlerteSante(@PathVariable Integer id, @Valid @RequestBody DerogationAlerteSanteDTO derogation) {
        dossierSanteService.deroger(id, derogation);
        return ResponseEntity.noContent().build();
    }

    // ── Crédit (docs/PLAN-FICHE-CLIENT.md, lot 3) ──

    @GetMapping("/customers/{id}/limite-credit")
    public ResponseEntity<SituationCreditDTO> getSituationCredit(@PathVariable Integer id) {
        return ResponseEntity.ok(limiteCreditService.situation(id));
    }

    /** Vente à crédit au-delà de la limite : droit {@code pr-depasser-limite-credit} ou clé d'un collègue qui le détient. */
    @PostMapping("/customers/{id}/limite-credit/derogations")
    public ResponseEntity<Void> derogerLimiteCredit(@PathVariable Integer id, @Valid @RequestBody DerogationLimiteCreditDTO derogation) {
        limiteCreditService.deroger(id, derogation);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customers/{id}/relances-differes")
    @RequiresNavAccess({ "customer", "differes", "ventes", "nouvelle-vente", "nouvelle-prevente" })
    public ResponseEntity<List<RelanceDiffereDTO>> getRelancesDifferes(@PathVariable Integer id) {
        return ResponseEntity.ok(relanceDiffereService.historique(id));
    }

    /** Relance SMS du client pour ses différés ; ouverte à qui peut consulter sa fiche. */
    @PostMapping("/customers/{id}/relances-differes")
    @RequiresNavAccess(value = { "customer", "differes", "ventes", "nouvelle-vente", "nouvelle-prevente" }, action = NavAction.ACCESS)
    public ResponseEntity<RelanceDiffereDTO> relancerDifferes(@PathVariable Integer id) {
        return ResponseEntity.ok(relanceDiffereService.relancer(id));
    }

    // ── Suivi et communication (docs/PLAN-FICHE-CLIENT.md, lot 4) ──

    @GetMapping("/customers/{id}/traitements-chroniques")
    public ResponseEntity<List<TraitementChroniqueDTO>> getTraitementsChroniques(@PathVariable Integer id) {
        return ResponseEntity.ok(traitementChroniqueService.traitements(id));
    }

    @PostMapping("/customers/{id}/traitements-chroniques")
    @RequiresNavAccess(value = { "customer", "ventes", "nouvelle-vente", "nouvelle-prevente" }, action = NavAction.EDIT)
    public ResponseEntity<TraitementChroniqueDTO> declarerTraitement(@PathVariable Integer id, @Valid @RequestBody TraitementChroniqueSaisieDTO saisie) {
        return ResponseEntity.ok(traitementChroniqueService.declarer(id, saisie));
    }

    /** Modifie ou arrête ({@code actif = false}) un traitement : il n'est jamais effacé. */
    @PutMapping("/customers/{id}/traitements-chroniques/{traitementId}")
    public ResponseEntity<TraitementChroniqueDTO> modifierTraitement(
        @PathVariable Integer id,
        @PathVariable Integer traitementId,
        @Valid @RequestBody TraitementChroniqueSaisieDTO saisie
    ) {
        return ResponseEntity.ok(traitementChroniqueService.modifier(id, traitementId, saisie));
    }

    /** Patients dont un traitement chronique arrive à échéance, est en retard ou interrompu. */
    @GetMapping("/customers/traitements-a-renouveler")
    public ResponseEntity<List<TraitementARenouvelerDTO>> getTraitementsARenouveler() {
        return ResponseEntity.ok(traitementChroniqueService.aRenouveler());
    }

    @GetMapping("/customers/{id}/consentements")
    public ResponseEntity<List<ConsentementDTO>> getConsentements(@PathVariable Integer id) {
        return ResponseEntity.ok(consentementService.etat(id));
    }

    @GetMapping("/customers/{id}/consentements/historique")
    public ResponseEntity<List<ConsentementDTO>> getHistoriqueConsentements(@PathVariable Integer id) {
        return ResponseEntity.ok(consentementService.historique(id));
    }

    @PutMapping("/customers/{id}/consentements/{canal}")
    public ResponseEntity<ConsentementDTO> enregistrerConsentement(
        @PathVariable Integer id,
        @PathVariable CanalConsentement canal,
        @RequestParam boolean accorde
    ) {
        return ResponseEntity.ok(consentementService.enregistrer(id, canal, accorde));
    }

    // ── Qualité du fichier et documents (docs/PLAN-FICHE-CLIENT.md, lot 5) ──

    @GetMapping(value = "/customers/{id}/releve/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> relevePdf(@PathVariable Integer id, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return pdf(customerDocumentService.relevePdf(id, fromDate, toDate), "releve-compte-" + id);
    }

    @GetMapping(value = "/customers/{id}/attestation-depenses/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> attestationPdf(@PathVariable Integer id, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return pdf(customerDocumentService.attestationPdf(id, fromDate, toDate), "attestation-depenses-" + id);
    }

    private static ResponseEntity<byte[]> pdf(byte[] contenu, String nom) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nom + ".pdf").body(contenu);
    }

    @GetMapping("/customers/doublons")
    @RequiresNavAccess(value = "pr-fusion-client", action = NavAction.EXECUTE)
    public ResponseEntity<List<DoublonClientGroupeDTO>> getDoublons() {
        return ResponseEntity.ok(fusionClientService.doublons());
    }

    @PostMapping("/customers/fusion/apercu")
    @RequiresNavAccess(value = "pr-fusion-client", action = NavAction.EXECUTE)
    public ResponseEntity<FusionClientApercuDTO> apercuFusion(@Valid @RequestBody FusionClientRequestDTO request) {
        return ResponseEntity.ok(fusionClientService.apercu(request.targetId(), request.sourceIds()));
    }

    @PostMapping("/customers/fusion")
    @RequiresNavAccess(value = "pr-fusion-client", action = NavAction.EXECUTE)
    public ResponseEntity<FusionClientResultDTO> fusionner(@Valid @RequestBody FusionClientRequestDTO request) {
        return ResponseEntity.ok(fusionClientService.fusionner(request));
    }

    /** Droit d'accès : tout ce que l'officine détient sur le client, en JSON. */
    @GetMapping("/customers/{id}/donnees-personnelles")
    @RequiresNavAccess(value = "pr-donnees-personnelles-client", action = NavAction.EXECUTE)
    public ResponseEntity<DonneesClientDTO> exporterDonnees(@PathVariable Integer id) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donnees-client-" + id + ".json")
            .body(donneesPersonnellesClientService.exporter(id));
    }

    /** Droit à l'effacement : anonymise la fiche, l'historique des délivrances reste tracé. */
    @PostMapping("/customers/{id}/anonymisation")
    @RequiresNavAccess(value = "pr-donnees-personnelles-client", action = NavAction.EXECUTE)
    public ResponseEntity<Void> anonymiser(@PathVariable Integer id) {
        donneesPersonnellesClientService.anonymiser(id);
        return ResponseEntity.noContent().build();
    }

    /** Désactive ({@code DISABLE}) ou réactive ({@code ENABLE}) un client, assuré comme standard. */
    @PutMapping("/customers/{id}/status")
    @RequiresNavAccess(value = "customer", action = NavAction.EDIT)
    public ResponseEntity<Void> changeStatus(@PathVariable Integer id, @RequestParam Status status) {
        customerDataService.changeStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/customers/{id}")
    @RequiresNavAccess("customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {

        uninsuredCustomerService.deleteCustomerById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/customers/importjson")
    @RequiresNavAccess("customer")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importjson") MultipartFile file) throws IOException {
        ResponseDTO responseDTO = importationCustomer.updateStocFromJSON(file.getInputStream());
        return ResponseEntity.ok(responseDTO);
    }



}

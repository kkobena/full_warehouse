package com.kobe.warehouse.service.fne.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.RepartitionTiersPayantParTva;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.fne.model.FneInvoice;
import com.kobe.warehouse.service.fne.model.FneInvoiceItem;
import com.kobe.warehouse.service.fne.model.FneResponse;
import com.kobe.warehouse.service.fne.model.TaxeEnum;
import com.kobe.warehouse.service.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FneServiceImpl implements FneService {

    private static final Logger log = LoggerFactory.getLogger(FneServiceImpl.class);
    private final FacturationRepository facturationRepository;
    private final HttpClient httpClient;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    @Value("${fne-url}")
    private String fneUrl;

    public FneServiceImpl(FacturationRepository facturationRepository, HttpClient httpClient,
                          StorageService storageService, ObjectMapper objectMapper) {
        this.facturationRepository = facturationRepository;
        this.httpClient = httpClient;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public FneResponse create(FactureItemId factureItemId) throws GenericError {
        FactureTiersPayant factureTiersPayant = facturationRepository.findById(factureItemId)
            .orElseThrow(() -> new GenericError("Aucune facture trouvée"));
        return createInvoice(factureTiersPayant);
    }

    @Override
    public void certifyGroupInvoice(FactureItemId factureItemId) throws GenericError {
        List<FactureTiersPayant> factureTiersPayants = facturationRepository.findById(factureItemId)
            .orElseThrow(() -> new GenericError("Aucune facture trouvée")).getFactureTiersPayants();
        for (FactureTiersPayant factureTiersPayant : factureTiersPayants) {
            createInvoice(factureTiersPayant);
        }
    }

    private FneInvoice buildFromFacture(FactureTiersPayant factureTiersPayant, Magasin magasin) {

        TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
        FneInvoice fneInvoice = new FneInvoice();
        fneInvoice.setEstablishment(magasin.getName());
        fneInvoice.setClientCompanyName(tiersPayant.getFullName());
        fneInvoice.setClientEmail("test@gmail.com");
        fneInvoice.setClientPhone("0757467789");
        // fneInvoice.setClientEmail(tiersPayant.getEmail());
        //
        fneInvoice.setPointOfSale(magasin.getFnePointOfSale());
        fneInvoice.setClientNcc(tiersPayant.getNcc());
        fneInvoice.setItems(buildFromProduitCodeTva(factureTiersPayant));
        return fneInvoice;
    }


    private FneResponse createInvoice(FactureTiersPayant factureTiersPayant) {
        try {
            Magasin magasin = storageService.getConnectedUserMagasin();
            FneInvoice fneInvoice = buildFromFacture(factureTiersPayant, magasin);
            log.info("fneInvoice --- {}", fneInvoice);


            String jsonPayload = objectMapper.writeValueAsString(fneInvoice);
            log.info("JSON payload: {}", jsonPayload);


            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fneUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + magasin.getFneSecretKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("FNE Response status: {}", response.statusCode());
            log.debug("FNE Response body: {}", response.body());

            // Check if request was successful
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Deserialize response to FneResponse
                FneResponse fneResponse = objectMapper.readValue(response.body(), FneResponse.class);
                log.info("FNE Response: {}", fneResponse);

                // Save response to database
                saveResponse(fneResponse, factureTiersPayant);
                return fneResponse;
            } else {
                log.error("FNE API returned error status: {} - {}", response.statusCode(), response.body());
                throw new GenericError("L'opération a échoué lors de l'envoi de la facture à la FNE. Veuillez réessayer ou contacter l'administrateur.");
            }

        } catch (Exception e) {
            log.error("Error creating FNE invoice", e);
            throw new GenericError("Failed to create FNE invoice: " + e.getMessage());
        }
    }


    private void saveResponse(FneResponse fneResponse, FactureTiersPayant factureTiersPayant) {
        factureTiersPayant.setFneResponse(fneResponse);
        facturationRepository.save(factureTiersPayant);
    }


    private List<FneInvoiceItem> buildFromProduitCodeTva(FactureTiersPayant factureTiersPayant) {
        List<RepartitionTiersPayantParTva> repartitions = factureTiersPayant.getRepartitions();
        repartitions.sort(Comparator.comparing(RepartitionTiersPayantParTva::tva));
        List<FneInvoiceItem> fneInvoiceItems = new ArrayList<>();

        String reference = factureTiersPayant.getNumFacture();
        String description = "FACTURATION DU " + DateUtil.formatFr(factureTiersPayant.getDebutPeriode()) + " AU "
            + DateUtil.formatFr(factureTiersPayant.getFinPeriode());
        repartitions.forEach(repartition -> fneInvoiceItems.add(new FneInvoiceItem(1, new String[]{TaxeEnum.getByValue(repartition.tva()).name()}, reference, description, repartition.montantHt(), 0.0)));
        return fneInvoiceItems;
    }


    // 1428351F


}

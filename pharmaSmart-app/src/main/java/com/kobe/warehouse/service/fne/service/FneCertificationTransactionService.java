package com.kobe.warehouse.service.fne.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.fne.model.FneInvoice;
import com.kobe.warehouse.service.fne.model.FneResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean séparé pour certifier une facture individuelle dans sa propre transaction.
 * Nécessaire pour contourner la limitation Spring AOP (self-invocation).
 */
@Service
public class FneCertificationTransactionService {

    private static final Logger log = LoggerFactory.getLogger(FneCertificationTransactionService.class);

    private final FacturationRepository facturationRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${fne-url}")
    private String fneUrl;

    public FneCertificationTransactionService(
        FacturationRepository facturationRepository,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.facturationRepository = facturationRepository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Envoie une facture à l'API FNE et sauvegarde la réponse.
     * Chaque appel est dans sa propre transaction : une erreur n'impacte pas les autres.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void certifier(FactureTiersPayant facture, FneInvoice fneInvoice, Magasin magasin) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(fneInvoice);
            log.debug("FNE payload facture {}: {}", facture.getNumFacture(), jsonPayload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fneUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + magasin.getFneSecretKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("FNE response [{}] facture {}: HTTP {}", magasin.getName(), facture.getNumFacture(), response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                FneResponse fneResponse = objectMapper.readValue(response.body(), FneResponse.class);
                facture.setFneResponse(fneResponse);
                facturationRepository.save(facture);
            } else {
                throw new GenericError("FNE HTTP " + response.statusCode() + " — " + response.body());
            }
        } catch (GenericError e) {
            throw e;
        } catch (Exception e) {
            throw new GenericError("Erreur technique FNE : " + e.getMessage());
        }
    }
}

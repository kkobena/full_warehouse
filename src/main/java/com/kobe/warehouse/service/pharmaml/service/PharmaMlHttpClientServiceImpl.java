package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.domain.SubstitutionProposee;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.PharmaMlEnvoiRepository;
import com.kobe.warehouse.repository.SubstitutionProposeeRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.InfoProduitDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.CorpsRepartiteur;
import com.kobe.warehouse.service.pharmaml.dto.response.CorpsResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.CsrpEnveloppeResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.IndisponibiliteN;
import com.kobe.warehouse.service.pharmaml.dto.response.LigneInfoReponse;
import com.kobe.warehouse.service.pharmaml.dto.response.LigneNReponse;
import com.kobe.warehouse.service.pharmaml.dto.response.MessageRepartiteur;
import com.kobe.warehouse.service.pharmaml.dto.response.NormaleReponse;
import com.kobe.warehouse.service.pharmaml.dto.response.PrixN;
import com.kobe.warehouse.service.pharmaml.dto.response.ProduitRemplacant;
import com.kobe.warehouse.service.pharmaml.dto.response.RepCommande;
import com.kobe.warehouse.service.pharmaml.dto.response.RepInfos;
import com.kobe.warehouse.service.pharmaml.dto.response.enumeration.TypePrix;
import com.kobe.warehouse.service.pharmaml.dto.response.enumeration.TypeRemplacement;
import com.kobe.warehouse.service.rupture.service.RuptureService;
import com.kobe.warehouse.service.settings.FileStorageService;
import com.kobe.warehouse.service.utils.NumberUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Implémentation du service de communication HTTP avec les serveurs PharmaML.
 * Gère la sérialisation XML, les envois HTTP avec retry, et le parsing des réponses.
 */
@Service
@Transactional(readOnly = true)
public class PharmaMlHttpClientServiceImpl implements PharmaMlHttpClientService {

    private static final Logger LOG = LoggerFactory.getLogger(PharmaMlHttpClientServiceImpl.class);
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 2_000L;

    private static final JAXBContext JAXB_REQUEST_CONTEXT;
    private static final JAXBContext JAXB_RESPONSE_CONTEXT;

    static {
        try {
            JAXB_REQUEST_CONTEXT = JAXBContext.newInstance(CsrpEnveloppe.class);
            JAXB_RESPONSE_CONTEXT = JAXBContext.newInstance(CsrpEnveloppeResponse.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final HttpClient httpClient;
    private final FileStorageService fileStorageService;
    private final PharmaMlEnvoiRepository pharmaMlEnvoiRepository;
    private final CommandeRepository commandeRepository;
    private final OrderLineService orderLineService;
    private final RuptureService ruptureService;
    private final FournisseurProduitService fournisseurProduitService;
    private final SubstitutionProposeeRepository substitutionProposeeRepository;
    private final CommandeIdGeneratorService commandeIdGeneratorService;
    private final ReferenceService referenceService;
    private final StorageService storageService;

    public PharmaMlHttpClientServiceImpl(
        HttpClient httpClient,
        FileStorageService fileStorageService,
        PharmaMlEnvoiRepository pharmaMlEnvoiRepository,
        CommandeRepository commandeRepository,
        OrderLineService orderLineService,
        RuptureService ruptureService,
        FournisseurProduitService fournisseurProduitService,
        SubstitutionProposeeRepository substitutionProposeeRepository,
        CommandeIdGeneratorService commandeIdGeneratorService,
        ReferenceService referenceService,
        StorageService storageService
    ) {
        this.httpClient = httpClient;
        this.fileStorageService = fileStorageService;
        this.pharmaMlEnvoiRepository = pharmaMlEnvoiRepository;
        this.commandeRepository = commandeRepository;
        this.orderLineService = orderLineService;
        this.ruptureService = ruptureService;
        this.fournisseurProduitService = fournisseurProduitService;
        this.substitutionProposeeRepository = substitutionProposeeRepository;
        this.commandeIdGeneratorService = commandeIdGeneratorService;
        this.referenceService = referenceService;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public PharmamlCommandeResponse sendCommandeWithRetry(
        CsrpEnveloppe payload,
        Commande commande,
        Fournisseur fournisseur,
        PharmaMlEnvoi envoi,
        String fileName
    ) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        String xmlPayload = serializePayload(payload);
        saveXmlFile(payload, "C", fileName);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(groupeFournisseur.getUrlPharmaMl()))
            .header("Content-Type", "text/xml; charset=UTF-8")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(xmlPayload))
            .build();

        long delayMs = INITIAL_RETRY_DELAY_MS;
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                envoi.setTentatives(attempt).setDerniereTentative(LocalDateTime.now());
                pharmaMlEnvoiRepository.save(envoi);
                LOG.info("PharmaML retry {}/{} pour commande {}", attempt, MAX_RETRY_ATTEMPTS, commande.getOrderReference());
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new GenericError("Interruption lors du retry PharmaML", "pharmaMlError");
                }
                delayMs *= 2;
            }
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return processCommandeResponse(response, fileName, commande, fournisseur);
            } catch (IOException e) {
                lastException = e;
                LOG.warn("PharmaML tentative {}/{} échouée: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenericError("Interruption lors de l'envoi PharmaML", "pharmaMlError");
            }
        }
        throw new GenericError("Échec de l'envoi après " + MAX_RETRY_ATTEMPTS + " tentatives: " + lastException.getMessage(), "pharmaMlError");
    }

    @Override
    public void sendSimpleMessage(CsrpEnveloppe payload, Fournisseur fournisseur, String fileName, String actionName) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        String xmlPayload = serializePayload(payload);

        if (fileName != null) {
            saveXmlFile(payload, actionName.substring(0, 3), fileName);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(groupeFournisseur.getUrlPharmaMl()))
            .header("Content-Type", "text/xml; charset=UTF-8")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(xmlPayload))
            .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                LOG.warn("{}: HTTP {} pour fichier {}", actionName, httpResponse.statusCode(), fileName);
                throw new GenericError("Le grossiste a retourné une erreur lors de " + actionName, "pharmaMlError");
            }
            LOG.info("{} envoyé avec succès pour fichier {}", actionName, fileName);
        } catch (IOException | InterruptedException e) {
            LOG.error("Erreur lors de l'envoi {}", actionName, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GenericError("Erreur lors de l'envoi de " + actionName, "pharmaMlError");
        }
    }

    @Override
    public List<InfoProduitDTO> sendInfoRequest(CsrpEnveloppe payload, Fournisseur fournisseur) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        String xmlPayload = serializePayload(payload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(groupeFournisseur.getUrlPharmaMl()))
            .header("Content-Type", "text/xml; charset=UTF-8")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(xmlPayload))
            .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return parseInfosResponse(httpResponse);
        } catch (IOException | InterruptedException e) {
            LOG.error("Erreur lors de la demande de disponibilité PharmaML", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GenericError("Erreur lors de la demande de disponibilité", "pharmaMlError");
        }
    }

    @Override
    public String serializePayload(CsrpEnveloppe payload) {
        try {
            Marshaller marshaller = JAXB_REQUEST_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(payload, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOG.error("Erreur de sérialisation XML", e);
            throw new GenericError("Erreur lors de la construction du message XML", "pharmaMlError");
        }
    }

    @Override
    public void saveXmlFile(Object payload, String prefix, String fileName) {
        Path path = fileStorageService.getFilePharmamlStorageLocation()
            .resolve(prefix.toUpperCase() + "_" + fileName + ".xml");

        try (OutputStream os = Files.newOutputStream(path)) {
            Marshaller marshaller;
            if (payload instanceof CsrpEnveloppe) {
                marshaller = JAXB_REQUEST_CONTEXT.createMarshaller();
            } else {
                marshaller = JAXB_RESPONSE_CONTEXT.createMarshaller();
            }
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(payload, os);
        } catch (IOException | JAXBException e) {
            LOG.error("Erreur lors de la sauvegarde du fichier XML: {}", e.getMessage());
        }
    }

    @Override
    public String generateFileName(String orderReference, String fournisseurLibelle) {
        return orderReference + "_" + StringUtils.replace(fournisseurLibelle, SPACE, EMPTY);
    }

    private PharmamlCommandeResponse processCommandeResponse(
        HttpResponse<String> httpResponse,
        String fileName,
        Commande commande,
        Fournisseur fournisseur
    ) {
        int httpCode = httpResponse.statusCode();

        if (httpCode == 200) {
            try {
                Unmarshaller unmarshaller = JAXB_RESPONSE_CONTEXT.createUnmarshaller();
                CsrpEnveloppeResponse response = (CsrpEnveloppeResponse) unmarshaller.unmarshal(new StringReader(httpResponse.body()));
                saveXmlFile(response, "R", fileName);
                return traiterCommandeRepondue(commande, response, fournisseur);
            } catch (JAXBException ex) {
                LOG.error("Erreur de parsing de la réponse XML", ex);
            }
        } else {
            saveLogResponse(httpResponse.body(), "LOG_" + fileName);
        }
        return new PharmamlCommandeResponse(false, 0, 0, 0, null);
    }

    private void saveLogResponse(String response, String fileName) {
        try {
            Files.writeString(fileStorageService.getFilePharmamlStorageLocation().resolve(fileName + ".xml"), response);
        } catch (IOException ex) {
            LOG.error("Erreur lors de la sauvegarde du log de réponse", ex);
        }
    }

    private List<LigneNReponse> getLigneNReponses(CsrpEnveloppeResponse response) {
        if (nonNull(response)) {
            CorpsResponse corps = response.getCorps();
            if (nonNull(corps)) {
                MessageRepartiteur messageRepartiteur = corps.getMessageRepartiteur();
                if (nonNull(messageRepartiteur)) {
                    CorpsRepartiteur corpsR = messageRepartiteur.getCorps();
                    if (nonNull(corpsR)) {
                        RepCommande repCommande = corpsR.getRepCommande();
                        if (nonNull(repCommande)) {
                            NormaleReponse normale = repCommande.getNormale();
                            if (nonNull(normale)) {
                                return normale.getLignes();
                            }
                        }
                    }
                }
            }
        }
        return List.of();
    }

    private PharmamlCommandeResponse traiterCommandeRepondue(
        Commande commande,
        CsrpEnveloppeResponse response,
        Fournisseur fournisseur
    ) {
        AtomicInteger countRupture = new AtomicInteger(0);
        AtomicInteger ruptureComplet = new AtomicInteger(0);
        AtomicInteger prisEncompte = new AtomicInteger(0);
        Map<OrderLine, Pair<FournisseurProduit, LigneNReponse>> lignesRupture = new HashMap<>();
        List<LigneNReponse> lignes = getLigneNReponses(response);
        List<OrderLine> items = commande.getOrderLines();
        List<OrderLine> itemsToRemove = new ArrayList<>();

        int itemSize = items.size();
        AtomicInteger montantCommande = new AtomicInteger();

        for (LigneNReponse ligneNReponse : lignes) {
            items.stream()
                .filter(ord -> ligneNReponse.getCodeProduit().equals(ord.getFournisseurProduit().getCodeCip()))
                .findFirst()
                .ifPresentOrElse(
                    orderLine -> {
                        int qteLivre = ligneNReponse.getQuantiteLivree();
                        int qteCommandee = orderLine.getQuantityRequested();
                        if (qteLivre >= qteCommandee) {
                            prisEncompte.incrementAndGet();
                            processOrderDetailResponse(orderLine, ligneNReponse);
                            montantCommande.addAndGet(computeOrderAmount(ligneNReponse));
                        } else {
                            if (qteLivre > 0) {
                                prisEncompte.incrementAndGet();
                                processOrderDetailResponse(orderLine, ligneNReponse);
                                montantCommande.addAndGet(computeOrderAmount(ligneNReponse));
                            } else {
                                ruptureComplet.incrementAndGet();
                            }
                            countRupture.incrementAndGet();
                            lignesRupture.put(orderLine, Pair.of(orderLine.getFournisseurProduit(), ligneNReponse));
                        }
                        itemsToRemove.add(orderLine);
                    },
                    () -> LOG.error("Produit {} non trouvé dans la commande {}", ligneNReponse.getCodeProduit(), commande.getOrderReference())
                );
        }
        items.removeAll(itemsToRemove);

        Integer reliquatCommandeId = null;
        if (countRupture.get() > 0) {
            createRupture(lignesRupture, commande, fournisseur);
            reliquatCommandeId = creerCommandeReliquat(commande, lignesRupture);
        }
        if (prisEncompte.get() > 0) {
            commande.setFinalAmount(montantCommande.get());
            commande.setHtAmount(montantCommande.get());
            commande.setUpdatedAt(LocalDateTime.now());
            commande.setHasBeenSubmittedToPharmaML(true);
            commandeRepository.save(commande);
        }
        if (itemSize == ruptureComplet.get()) {
            commande.setOrderStatus(OrderStatut.ARCHIVED);
            commande.setUpdatedAt(LocalDateTime.now());
            commandeRepository.save(commande);
        }
        return new PharmamlCommandeResponse(true, itemSize, prisEncompte.get(), countRupture.get(), reliquatCommandeId);
    }

    private void processOrderDetailResponse(OrderLine o, LigneNReponse ligneNReponse) {
        com.kobe.warehouse.service.dto.Pair prix = getPrixAchatPrixUni(ligneNReponse.getPrix());

        o.setQuantityReceived(ligneNReponse.getQuantiteLivree());

        int prixAchat = Integer.parseInt(prix.key() + "");
        int prixUnit = Integer.parseInt(prix.value() + "");
        if (prixAchat > 0) {
            o.setOrderCostAmount(prixAchat);
        }
        if (prixUnit > 0) {
            o.setOrderUnitPrice(prixUnit);
        }
        o.setUpdatedAt(LocalDateTime.now());
        o.setUpdated(true);
        orderLineService.save(o);
    }

    private com.kobe.warehouse.service.dto.Pair getPrixAchatPrixUni(List<PrixN> prixs) {
        if (CollectionUtils.isEmpty(prixs)) {
            return new com.kobe.warehouse.service.dto.Pair(0, 0);
        }
        Integer prixAchat = NumberUtil.intFromString(
            prixs.stream().filter(p -> p.getNature().equals(TypePrix.PHAHT.name())).findAny().map(PrixN::getValeur).orElse("")
        );
        Integer prixUnitt = NumberUtil.intFromString(
            prixs.stream().filter(p -> p.getNature().equals(TypePrix.PUBTC.name())).findAny().map(PrixN::getValeur).orElse("")
        );
        return new com.kobe.warehouse.service.dto.Pair(prixAchat, prixUnitt);
    }

    private int computeOrderAmount(LigneNReponse ligneNReponse) {
        return ligneNReponse.getQuantiteLivree() * Integer.parseInt(getPrixAchatPrixUni(ligneNReponse.getPrix()).key() + "");
    }

    private void createRupture(
        Map<OrderLine, Pair<FournisseurProduit, LigneNReponse>> lignesRupture,
        Commande commande,
        Fournisseur fournisseur
    ) {
        lignesRupture.forEach((orderLine, coupleProduitResponse) -> {
            LigneNReponse ligneNReponse = coupleProduitResponse.getSecond();
            this.ruptureService.createRupture(
                orderLine.getFournisseurProduit().getProduit(),
                fournisseur,
                orderLine.getQuantityRequested() - ligneNReponse.getQuantiteLivree()
            );
            processRemplacement(ligneNReponse, orderLine, commande, fournisseur);
            if (ligneNReponse.getQuantiteLivree() == 0) {
                this.orderLineService.delete(orderLine);
            }
        });
    }

    private void processRemplacement(LigneNReponse ligneNReponse, OrderLine origin, Commande commande, Fournisseur fournisseur) {
        IndisponibiliteN indisponibilite = ligneNReponse.getIndisponibilite();
        if (isNull(indisponibilite)) {
            return;
        }
        ProduitRemplacant produitRemplacant = indisponibilite.getProduitRemplacant();
        if (isNull(produitRemplacant)) {
            return;
        }
        String type = produitRemplacant.getTypeRemplacement();

        if (TypeRemplacement.EP.name().equals(type)) {
            SubstitutionProposee sub = new SubstitutionProposee()
                .setCommande(commande)
                .setOrderLine(origin)
                .setFournisseur(fournisseur)
                .setCipPropose(produitRemplacant.getCodeProduit())
                .setDesignation(produitRemplacant.getDesignation())
                .setTypeCodification(produitRemplacant.getTypeCodification())
                .setQuantite(origin.getQuantityRequested());
            substitutionProposeeRepository.save(sub);
            return;
        }

        if (TypeRemplacement.EL.name().equals(type) || TypeRemplacement.RL.name().equals(type)) {
            List<FournisseurProduit> fournisseurProduits =
                this.fournisseurProduitService.findByCodeCipOrProduitcodeEan(produitRemplacant.getCodeProduit());
            if (!fournisseurProduits.isEmpty()) {
                FournisseurProduit fournisseurProduit = fournisseurProduits.stream()
                    .filter(p -> p.getCodeCip().equals(produitRemplacant.getCodeProduit()))
                    .findFirst()
                    .orElse(null);
                if (isNull(fournisseurProduit)) {
                    com.kobe.warehouse.service.dto.Pair prix = getPrixAchatPrixUni(ligneNReponse.getPrix());
                    fournisseurProduit = new FournisseurProduit();
                    fournisseurProduit.setProduit(fournisseurProduits.getFirst().getProduit());
                    fournisseurProduit.setCodeCip(produitRemplacant.getCodeProduit());
                    fournisseurProduit.setFournisseur(fournisseur);
                    fournisseurProduit.setPrixAchat(Integer.parseInt(prix.key() + ""));
                    fournisseurProduit.setPrixUni(Integer.parseInt(prix.value() + ""));
                    fournisseurProduit = fournisseurProduitService.save(fournisseurProduit);
                }
                addRemplacement(ligneNReponse, origin.getQuantityRequested(), fournisseurProduit, commande);
            }
        }
    }

    private void addRemplacement(
        LigneNReponse ligneNReponse,
        int requestedQuanty,
        FournisseurProduit fournisseurProduit,
        Commande commande
    ) {
        OrderLineDTO dto = new OrderLineDTO();
        dto.setQuantityRequested(requestedQuanty);
        dto.setQuantityReceived(ligneNReponse.getQuantiteLivree());
        dto.setInitStock(0);
        OrderLine orderLine = this.orderLineService.buildOrderLine(dto, fournisseurProduit);
        orderLine.setCommande(commande);
        commande.getOrderLines().add(this.orderLineService.save(orderLine));
    }

    private Integer creerCommandeReliquat(
        Commande parent,
        Map<OrderLine, Pair<FournisseurProduit, LigneNReponse>> lignesRupture
    ) {
        List<OrderLine> lignesReliquat = new ArrayList<>();
        for (Map.Entry<OrderLine, Pair<FournisseurProduit, LigneNReponse>> entry : lignesRupture.entrySet()) {
            OrderLine original = entry.getKey();
            LigneNReponse reponse = entry.getValue().getSecond();
            int qteReliquat = original.getQuantityRequested() - reponse.getQuantiteLivree();
            if (qteReliquat > 0) {
                OrderLineDTO dto = new OrderLineDTO();
                dto.setQuantityRequested(qteReliquat);
                dto.setQuantityReceived(0);
                dto.setInitStock(original.getInitStock() != null ? original.getInitStock() : 0);
                lignesReliquat.add(orderLineService.buildOrderLine(dto, original.getFournisseurProduit()));
            }
        }
        if (lignesReliquat.isEmpty()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        Commande reliquat = new Commande();
        reliquat.setId(commandeIdGeneratorService.getNextIdAsInt());
        reliquat.setCreatedAt(now);
        reliquat.setUpdatedAt(now);
        reliquat.setUser(storageService.getUser());
        reliquat.setOrderReference(referenceService.buildNumCommande());
        reliquat.setFournisseur(parent.getFournisseur());
        reliquat.setReliquatDeCommandeId(parent.getId().getId());
        reliquat.setGrossAmount(0);
        lignesReliquat.forEach(reliquat::addOrderLine);
        commandeRepository.saveAndFlush(reliquat);
        LOG.info("Reliquat {} créé depuis commande {}", reliquat.getOrderReference(), parent.getOrderReference());
        return reliquat.getId().getId();
    }

    // ===================== Méthodes de parsing des réponses d'information =====================

    private List<InfoProduitDTO> parseInfosResponse(HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() != 200) {
            LOG.warn("REQ_INFORMATION: HTTP {}", httpResponse.statusCode());
            return List.of();
        }
        try {
            Unmarshaller unmarshaller = JAXB_RESPONSE_CONTEXT.createUnmarshaller();
            CsrpEnveloppeResponse response = (CsrpEnveloppeResponse) unmarshaller.unmarshal(new StringReader(httpResponse.body()));
            RepInfos repInfos = getRepInfos(response);
            if (repInfos == null || CollectionUtils.isEmpty(repInfos.getLignes())) {
                return List.of();
            }
            return repInfos.getLignes().stream().map(this::toInfoProduitDTO).toList();
        } catch (JAXBException e) {
            LOG.error("Erreur de parsing de la réponse REQ_INFORMATION", e);
            return List.of();
        }
    }

    private RepInfos getRepInfos(CsrpEnveloppeResponse response) {
        if (isNull(response)) return null;
        CorpsResponse corps = response.getCorps();
        if (isNull(corps)) return null;
        MessageRepartiteur mr = corps.getMessageRepartiteur();
        if (isNull(mr)) return null;
        CorpsRepartiteur corpsR = mr.getCorps();
        if (isNull(corpsR)) return null;
        return corpsR.getRepInfos();
    }

    private InfoProduitDTO toInfoProduitDTO(LigneInfoReponse ligne) {
        int prixAchat = 0;
        if (!CollectionUtils.isEmpty(ligne.getPrix())) {
            com.kobe.warehouse.service.dto.Pair prix = getPrixAchatPrixUni(ligne.getPrix());
            prixAchat = Integer.parseInt(prix.key() + "");
        }
        return new InfoProduitDTO(
            ligne.getCodeProduit(),
            ligne.getDesignation(),
            ligne.getStockDisponible(),
            prixAchat,
            ligne.getStockDisponible() > 0
        );
    }
}


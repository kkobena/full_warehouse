package com.kobe.warehouse.service.pharmaml.service;

import static com.kobe.warehouse.service.pharmaml.PharmaMlUtils.TYPE_CODIFICATION_CIP39;
import static com.kobe.warehouse.service.pharmaml.PharmaMlUtils.TYPE_CODIFICATION_EAN;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.FournisseurService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pharmaml.PharmaMlUtils;
import com.kobe.warehouse.service.pharmaml.dto.Corps;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.Entete;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.LigneN;
import com.kobe.warehouse.service.pharmaml.dto.MessageCorps;
import com.kobe.warehouse.service.pharmaml.dto.MessageEntete;
import com.kobe.warehouse.service.pharmaml.dto.MessageOfficine;
import com.kobe.warehouse.service.pharmaml.dto.Normale;
import com.kobe.warehouse.service.pharmaml.dto.OfficinePartenaire;
import com.kobe.warehouse.service.pharmaml.dto.Partenaire;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.CorpsRepartiteur;
import com.kobe.warehouse.service.pharmaml.dto.response.CorpsResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.CsrpEnveloppeResponse;
import com.kobe.warehouse.service.pharmaml.dto.response.IndisponibiliteN;
import com.kobe.warehouse.service.pharmaml.dto.response.LigneNReponse;
import com.kobe.warehouse.service.pharmaml.dto.response.MessageRepartiteur;
import com.kobe.warehouse.service.pharmaml.dto.response.NormaleReponse;
import com.kobe.warehouse.service.pharmaml.dto.response.PrixN;
import com.kobe.warehouse.service.pharmaml.dto.response.ProduitRemplacant;
import com.kobe.warehouse.service.pharmaml.dto.response.RepCommande;
import com.kobe.warehouse.service.pharmaml.dto.response.enumeration.TypePrix;
import com.kobe.warehouse.service.pharmaml.dto.response.enumeration.TypeRemplacement;
import com.kobe.warehouse.service.rupture.service.RuptureService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.settings.FileStorageService;
import com.kobe.warehouse.service.utils.NumberUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class PharmaMlServiceImpl implements PharmaMlService {

    private static final Logger LOG = LoggerFactory.getLogger(PharmaMlServiceImpl.class);
    private final CommandeRepository commandeRepository;
    private final AppConfigurationService appConfigurationService;
    private final FileStorageService fileStorageService;
    private final HttpClient httpClient;
    private final OrderLineService orderLineService;
    private final RuptureService ruptureService;
    private final FournisseurProduitService fournisseurProduitService;
    private final FournisseurService fournisseurService;
    private Fournisseur fournisseur;

    public PharmaMlServiceImpl(
        CommandeRepository commandeRepository,
        AppConfigurationService appConfigurationService,
        FileStorageService fileStorageService,
        HttpClient httpClient,
        OrderLineService orderLineService,
        RuptureService ruptureService,
        FournisseurProduitService fournisseurProduitService,
        FournisseurService fournisseurService
    ) {
        this.commandeRepository = commandeRepository;
        this.appConfigurationService = appConfigurationService;
        this.fileStorageService = fileStorageService;
        this.httpClient = httpClient;
        this.orderLineService = orderLineService;
        this.ruptureService = ruptureService;
        this.fournisseurProduitService = fournisseurProduitService;
        this.fournisseurService = fournisseurService;
    }

    @Override
    public PharmamlCommandeResponse envoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO) {
        Commande commande = commandeRepository.getReferenceById(envoiParamsDTO.getCommandeId());
        try {
            fournisseur = isNull(envoiParamsDTO.getGrossisteId())
                ? commande.getFournisseur()
                : this.fournisseurService.findOneById(envoiParamsDTO.getGrossisteId());
            GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
            prevalidate();

            CsrpEnveloppe payLoad = buildPayload(commande, envoiParamsDTO.getCommentaire());

            JAXBContext requestContext = JAXBContext.newInstance(CsrpEnveloppe.class);
            Marshaller marshaller = requestContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(payLoad, sw); // save file // a supprimer a l'avenir
            String fileName =
                commande.getOrderReference() +
                "_" +
                StringUtils.replace(
                    fournisseur.getLibelle(),
                    org.apache.commons.lang3.StringUtils.SPACE,
                    org.apache.commons.lang3.StringUtils.EMPTY
                );
            createSaveXmlFile(marshaller, payLoad, "C", fileName);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(groupeFournisseur.getUrlPharmaMl()))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(sw.toString()))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return processResponse(httpResponse, fileName, commande);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi de la commande à PharmaML", e);
            throw new GenericError("Erreur lors de l'envoi de la commande à PharmaML", "pharmaMlError");
        }
    }

    @Override
    public void envoiPharmaInfosProduit(String commandeId) {}

    @Override
    public VerificationResponseCommandeDTO lignesCommandeRetour(String commandeRef, String orderId) {
        return null;
    }

    @Override
    public void renvoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO) {}

    @Override
    public VerificationResponseCommandeDTO reponseRupture(String ruptureId) {
        return null;
    }

    private CsrpEnveloppe buildPayload(Commande commande, String commentaire) {
        CsrpEnveloppe ce = new CsrpEnveloppe();
        ce.setUsage(PharmaMlUtils.USAGE_VALUE);
        ce.setVersionProtocole(PharmaMlUtils.VERSION_PROTOCLE_VALUE);
        ce.setVersionLogiciel(PharmaMlUtils.VERSION_LOGICIEL_VALUE);
        ce.setIdLogiciel(PharmaMlUtils.ID_LOGICIEL_VALUE);
        ce.setEntete(buildEntete());
        ce.setCorps(buildCorps(commande, commentaire));
        ce.setNatureAction(PharmaMlUtils.NATURE_ACTION_REQ_EMISSION);
        return ce;
    }

    private Entete buildEntete() {
        Entete e = new Entete();
        e.setDate(getPharmaMlDate());
        e.setRefMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        e.setEmetteur(buildEmetteur());
        e.setRecepteur(buildRecepteur());
        return e;
    }

    private Partenaire buildEmetteur() {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        String code = StringUtils.hasLength(groupeFournisseur.getCodeOfficePharmaMl())
            ? groupeFournisseur.getCodeOfficePharmaMl()
            : PharmaMlUtils.CODE_VALUE;
        Partenaire p = new Partenaire();
        p.setNature(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_OF);
        p.setCode(code);
        p.setAdresse(this.appConfigurationService.getMagasin().getName());
        p.setId(groupeFournisseur.getIdRecepteurPharmaMl());
        return p;
    }

    private Partenaire buildRecepteur() {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        Partenaire p = new Partenaire();
        p.setNature(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_RE);
        p.setCode(groupeFournisseur.getCodeRecepteurPharmaMl());
        p.setAdresse(this.appConfigurationService.getMagasin().getName());

        p.setId(fournisseur.getIdentifiantRepartiteur());
        return p;
    }

    private MessageEntete buildMessageEntete() {
        MessageEntete me = new MessageEntete();
        me.setEmetteur(buildOfficineEmetteur());
        me.setDestinataire(buildOfficineDestinataire());
        me.setDate(getPharmaMlDate());
        return me;
    }

    private OfficinePartenaire buildOfficineDestinataire() {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        OfficinePartenaire op = new OfficinePartenaire();
        op.setIdSociete(fournisseur.getIdentifiantRepartiteur());
        op.setCodeSociete(groupeFournisseur.getCodeRecepteurPharmaMl());
        op.setNaturePartenaire(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_RE);
        return op;
    }

    private OfficinePartenaire buildOfficineEmetteur() {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        OfficinePartenaire op = new OfficinePartenaire();
        op.setIdClient(groupeFournisseur.getIdRecepteurPharmaMl());
        op.setNaturePartenaire(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_OF);
        return op;
    }

    private MessageCorps buildMessageCorps(Commande commande, String commentaire) {
        MessageCorps mc = new MessageCorps();
        mc.setCommande(buildCommande(commande, commentaire));
        return mc;
    }

    private com.kobe.warehouse.service.pharmaml.dto.Commande buildCommande(Commande commande, String commentaire) {
        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();
        c.setDateLivraison(LocalDate.now().plusDays(1).toString());
        if (StringUtils.hasLength(commentaire)) {
            c.setCommentaireGeneral(commentaire);
        } else {
            c.setCommentaireGeneral(commande.getOrderReference());
        }

        c.setRefCdeClient(commande.getOrderReference());
        c.setNormale(buildNormale(commande));
        return c;
    }

    private List<LigneN> buildCommandeLigne(Commande commande) {
        AtomicInteger count = new AtomicInteger(1);
        return commande
            .getOrderLines()
            .stream()
            .map(item -> {
                LigneN ligne = new LigneN();
                FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

                String numLigne = org.apache.commons.lang3.StringUtils.leftPad(count.getAndIncrement() + "", 4, '0');
                String quantite = org.apache.commons.lang3.StringUtils.leftPad(item.getQuantityRequested() + "", 4, '0');
                String cip = fournisseurProduit.getCodeCip();

                ligne.setCodeProduit(cip);
                ligne.setQuantite(quantite);
                ligne.setNumLigne(numLigne);
                ligne.setTypeCodification(typeCodification(cip));
                return ligne;
            })
            .collect(Collectors.toList());
    }

    private String typeCodification(String cip) {
        if (cip.length() == 13) {
            return TYPE_CODIFICATION_EAN;
        }
        return TYPE_CODIFICATION_CIP39;
    }

    private Normale buildNormale(Commande commande) {
        Normale n = new Normale();
        n.setLignes(buildCommandeLigne(commande));
        return n;
    }

    private Corps buildCorps(Commande commande, String commentaire) {
        Corps c = new Corps();
        c.setMessageOfficine(buildMessageOfficine(commande, commentaire));

        return c;
    }

    private MessageOfficine buildMessageOfficine(Commande commande, String commentaire) {
        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete());
        messageOfficine.setCorps(buildMessageCorps(commande, commentaire));
        return messageOfficine;
    }

    private void createSaveXmlFile(Marshaller marshaller, Object objectToSave, String prefix, String fileName) {
        Path path = this.fileStorageService.getFilePharmamlStorageLocation().resolve(prefix.toUpperCase() + "_" + fileName + ".xml");

        try (OutputStream os = Files.newOutputStream(path)) {
            marshaller.marshal(objectToSave, os);
        } catch (IOException | JAXBException e) {
            LOG.error(e.getMessage());
        }
    }

    private void saveResponse(String response, String fileName) {
        try {
            Files.writeString(this.fileStorageService.getFilePharmamlStorageLocation().resolve("R_" + fileName + ".xml"), response);
        } catch (IOException ex) {
            LOG.error("saveResonse", ex);
        }
    }

    private CsrpEnveloppeResponse loadFromFileForTestingPurpose() {
        try {
            Path path = this.fileStorageService.getFilePharmamlStorageLocation().resolve("R_15062025_00001_UBIPHARMYOP.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(CsrpEnveloppeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (CsrpEnveloppeResponse) unmarshaller.unmarshal(path.toFile());
        } catch (Exception e) {
            LOG.error("tes", e);
        }
        return null;
    }

    private String getPharmaMlDate() {
        return LocalDate.now() + "T" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private PharmamlCommandeResponse processResponse(HttpResponse<String> httpResponse, String fileName, Commande commande) {
        int httpCode = httpResponse.statusCode();

        if (httpCode == 200) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(CsrpEnveloppeResponse.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                CsrpEnveloppeResponse response = (CsrpEnveloppeResponse) unmarshaller.unmarshal(new StringReader(httpResponse.body()));
                // a supprimer a l'avenir
                createSaveXmlFile(jaxbContext.createMarshaller(), response, "R", fileName);

                return traiterCommandeRepondue(commande, response);
            } catch (JAXBException ex) {
                LOG.error("processResponse", ex);
            }
        } else {
            saveResponse(httpResponse.body(), "LOG_" + fileName);
        }
        return new PharmamlCommandeResponse(false, 0, 0, 0);
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

    private PharmamlCommandeResponse traiterCommandeRepondue(Commande commande, CsrpEnveloppeResponse response) {
        AtomicInteger countRupture = new AtomicInteger(0);
        AtomicInteger ruptureComplet = new AtomicInteger(0);
        AtomicInteger prisEncompte = new AtomicInteger(0);
        Map<OrderLine, Pair<FournisseurProduit, LigneNReponse>> lignesRupture = new HashMap<>();
        List<LigneNReponse> lignes = getLigneNReponses(response);
        List<OrderLine> items = commande.getOrderLines();

        int itemSize = items.size();
        AtomicInteger montantCommande = new AtomicInteger();
        // si tout les produit sont zero surprime la commande
        for (LigneNReponse ligneNReponse : lignes) {
            items
                .stream()
                .filter(ord -> ligneNReponse.getCodeProduit().equals(ord.getFournisseurProduit().getCodeCip()))
                .findFirst()
                .ifPresentOrElse(
                    orderLine -> {
                        int qteLivre = ligneNReponse.getQuantiteLivree();
                        int qteCommandee = orderLine.getQuantityRequested();
                        if (qteLivre >= qteCommandee) {
                            prisEncompte.incrementAndGet();
                            processOnderDetailResponce(orderLine, ligneNReponse);
                            montantCommande.addAndGet(computeOrderAmount(ligneNReponse));
                        } else {
                            if (qteLivre > 0) {
                                prisEncompte.incrementAndGet();
                                processOnderDetailResponce(orderLine, ligneNReponse);
                                montantCommande.addAndGet(computeOrderAmount(ligneNReponse));
                            } else {
                                ruptureComplet.incrementAndGet();
                            }
                            countRupture.incrementAndGet();

                            lignesRupture.put(orderLine, Pair.of(orderLine.getFournisseurProduit(), ligneNReponse));
                        }
                        items.remove(orderLine);
                    },
                    () ->
                        LOG.error("Produit {} non trouvé dans la commande {}", ligneNReponse.getCodeProduit(), commande.getOrderReference())
                );
        }
        // la commande est en rupture totale

        if (countRupture.get() > 0) {
            createRupture(lignesRupture, commande);
        }
        if (prisEncompte.get() > 0) {
            commande.setFinalAmount(montantCommande.get());
            commande.setHtAmount(montantCommande.get());
            commande.setUpdatedAt(LocalDateTime.now());
            commande.setHasBeenSubmittedToPharmaML(true);
            commandeRepository.save(commande);
        }
        if (itemSize == ruptureComplet.get()) {
            commandeRepository.delete(commande);
        }
        // update commande montant
        return new PharmamlCommandeResponse(true, itemSize, prisEncompte.get(), countRupture.get());
    }

    private void processOnderDetailResponce(OrderLine o, LigneNReponse ligneNReponse) {
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

    private void createRupture(Map<OrderLine, Pair<FournisseurProduit, LigneNReponse>> lignesRupture, Commande commande) {
        lignesRupture.forEach((orderLine, coupleProduitResponse) -> {
            LigneNReponse ligneNReponse = coupleProduitResponse.getSecond();
            this.ruptureService.createRupture(
                    orderLine.getFournisseurProduit().getProduit(),
                    fournisseur,
                    orderLine.getQuantityRequested() - ligneNReponse.getQuantiteLivree()
                );

            processRemplacement(ligneNReponse, orderLine, commande);
            if (ligneNReponse.getQuantiteLivree() == 0) {
                this.orderLineService.delete(orderLine);
            }
        });
    }

    private void processRemplacement(LigneNReponse ligneNReponse, OrderLine origin, Commande commande) {
        IndisponibiliteN indisponibilite = ligneNReponse.getIndisponibilite();
        if (nonNull(indisponibilite)) {
            ProduitRemplacant produitRemplacant = indisponibilite.getProduitRemplacant();
            if (
                nonNull(produitRemplacant) &&
                (TypeRemplacement.EL.name().equals(produitRemplacant.getTypeRemplacement()) ||
                    TypeRemplacement.RL.name().equals(produitRemplacant.getTypeRemplacement()))
            ) {
                List<FournisseurProduit> fournisseurProduits =
                    this.fournisseurProduitService.findByCodeCipOrProduitcodeEan(produitRemplacant.getCodeProduit());
                if (!fournisseurProduits.isEmpty()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduits
                        .stream()
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
                // on ajoute la ligne a la commande
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

    private void prevalidate() {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        if (isNull(groupeFournisseur)) {
            throw new GenericError(
                String.format("Le fournisseur %s n'est pas configuré pour PharmaML", fournisseur.getLibelle()),
                "fournisseurNonConfigurePharmaMl"
            );
        }
        if (
            !StringUtils.hasLength(groupeFournisseur.getUrlPharmaMl()) ||
            !StringUtils.hasLength(groupeFournisseur.getCodeRecepteurPharmaMl()) ||
            !StringUtils.hasLength(groupeFournisseur.getIdRecepteurPharmaMl())
        ) {
            throw new GenericError(
                String.format(
                    "Veuillez configuer saisir les informations relatives à PharmaML pour ce %s dans le groupe auquel il appartient",
                    fournisseur.getLibelle()
                ),
                "fournisseurNonConfigurePharmaMl"
            );
        }
    }
}

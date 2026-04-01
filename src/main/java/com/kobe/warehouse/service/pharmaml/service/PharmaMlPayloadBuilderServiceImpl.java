package com.kobe.warehouse.service.pharmaml.service;

import static com.kobe.warehouse.service.pharmaml.PharmaMlUtils.TYPE_CODIFICATION_CIP39;
import static com.kobe.warehouse.service.pharmaml.PharmaMlUtils.TYPE_CODIFICATION_EAN;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.service.pharmaml.PharmaMlUtils;
import com.kobe.warehouse.service.pharmaml.dto.AcqReception;
import com.kobe.warehouse.service.pharmaml.dto.Annulation;
import com.kobe.warehouse.service.pharmaml.dto.Corps;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.Entete;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.Exceptionnelle;
import com.kobe.warehouse.service.pharmaml.dto.LigneN;
import com.kobe.warehouse.service.pharmaml.dto.LigneReception;
import com.kobe.warehouse.service.pharmaml.dto.LigneRetour;
import com.kobe.warehouse.service.pharmaml.dto.LigneRetourDTO;
import com.kobe.warehouse.service.pharmaml.dto.MessageCorps;
import com.kobe.warehouse.service.pharmaml.dto.MessageEntete;
import com.kobe.warehouse.service.pharmaml.dto.MessageOfficine;
import com.kobe.warehouse.service.pharmaml.dto.Normale;
import com.kobe.warehouse.service.pharmaml.dto.OfficinePartenaire;
import com.kobe.warehouse.service.pharmaml.dto.Partenaire;
import com.kobe.warehouse.service.pharmaml.dto.Retour;
import com.kobe.warehouse.service.pharmaml.dto.enumeration.TypeCommande;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implémentation du service de construction des payloads PharmaML. Ce service est stateless et ne
 * gère aucune persistance ni appel réseau.
 */
@Service
public class PharmaMlPayloadBuilderServiceImpl implements PharmaMlPayloadBuilderService {

    private final AppConfigurationService appConfigurationService;

    public PharmaMlPayloadBuilderServiceImpl(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public CsrpEnveloppe buildCommandePayload(Commande commande, EnvoiParamsDTO params,
        Fournisseur fournisseur, String refMessage) {
        CsrpEnveloppe ce = new CsrpEnveloppe();
        ce.setUsage(PharmaMlUtils.USAGE_VALUE);
        ce.setVersionProtocole(PharmaMlUtils.VERSION_PROTOCLE_VALUE);
        ce.setVersionLogiciel(PharmaMlUtils.VERSION_LOGICIEL_VALUE);
        ce.setIdLogiciel(PharmaMlUtils.ID_LOGICIEL_VALUE);
        ce.setEntete(buildEntete(fournisseur, refMessage));
        ce.setCorps(buildCorpsCommande(commande, params, fournisseur));
        ce.setNatureAction(PharmaMlUtils.NATURE_ACTION_REQ_EMISSION);
        return ce;
    }


    @Override
    public CsrpEnveloppe buildInfoPayload(Commande commande, Fournisseur fournisseur,
        String refMessage) {
        CsrpEnveloppe ce = new CsrpEnveloppe();
        ce.setUsage(PharmaMlUtils.USAGE_VALUE);
        ce.setVersionProtocole(PharmaMlUtils.VERSION_PROTOCLE_VALUE);
        ce.setVersionLogiciel(PharmaMlUtils.VERSION_LOGICIEL_VALUE);
        ce.setIdLogiciel(PharmaMlUtils.ID_LOGICIEL_VALUE);
        ce.setNatureAction(PharmaMlUtils.NATURE_ACTION_REQ_INFORMATION);
        ce.setEntete(buildEntete(fournisseur, refMessage));
        ce.setCorps(buildCorpsInfo(commande, fournisseur, refMessage));
        return ce;
    }

    @Override
    public CsrpEnveloppe buildInfoPayloadFromSuggestionLines(List<SuggestionLine> lignes,
        Fournisseur fournisseur, String refMessage) {
        CsrpEnveloppe ce = new CsrpEnveloppe();
        ce.setUsage(PharmaMlUtils.USAGE_VALUE);
        ce.setVersionProtocole(PharmaMlUtils.VERSION_PROTOCLE_VALUE);
        ce.setVersionLogiciel(PharmaMlUtils.VERSION_LOGICIEL_VALUE);
        ce.setIdLogiciel(PharmaMlUtils.ID_LOGICIEL_VALUE);
        ce.setNatureAction(PharmaMlUtils.NATURE_ACTION_REQ_INFORMATION);
        ce.setEntete(buildEntete(fournisseur, refMessage));
        ce.setCorps(buildCorpsInfoFromSuggestionLines(lignes, fournisseur, refMessage));
        return ce;
    }

    @Override
    public CsrpEnveloppe buildRetourPayload(Commande commande, Fournisseur fournisseur,
        List<LigneRetourDTO> lignes, String refMessage) {
        CsrpEnveloppe ce = new CsrpEnveloppe();
        ce.setUsage(PharmaMlUtils.USAGE_VALUE);
        ce.setVersionProtocole(PharmaMlUtils.VERSION_PROTOCLE_VALUE);
        ce.setVersionLogiciel(PharmaMlUtils.VERSION_LOGICIEL_VALUE);
        ce.setIdLogiciel(PharmaMlUtils.ID_LOGICIEL_VALUE);
        ce.setNatureAction(PharmaMlUtils.NATURE_ACTION_REQ_RETOUR);
        ce.setEntete(buildEntete(fournisseur, refMessage));
        ce.setCorps(buildCorpsRetour(commande, fournisseur, lignes));
        return ce;
    }

    @Override
    public String generateRefMessage() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    private Entete buildEntete(Fournisseur fournisseur, String refMessage) {
        Entete e = new Entete();
        e.setDate(getPharmaMlDate());
        e.setRefMessage(refMessage);
        e.setEmetteur(buildEmetteur(fournisseur));
        e.setRecepteur(buildRecepteur(fournisseur));
        return e;
    }

    private Partenaire buildEmetteur(Fournisseur fournisseur) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        String code = StringUtils.hasLength(groupeFournisseur.getCodeOfficePharmaMl())
            ? groupeFournisseur.getCodeOfficePharmaMl()
            : PharmaMlUtils.CODE_VALUE;
        Partenaire p = new Partenaire();
        p.setNature(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_OF);
        p.setCode(code);
        p.setAdresse(this.appConfigurationService.getMagasin().getName());
        p.setId(
            fournisseur.getIdentifiantRepartiteur());//groupeFournisseur.getIdRecepteurPharmaMl()
        return p;
    }

    private Partenaire buildRecepteur(Fournisseur fournisseur) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        Partenaire p = new Partenaire();
        p.setNature(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_RE);
        p.setCode(groupeFournisseur.getCodeRecepteurPharmaMl());
        p.setAdresse(this.appConfigurationService.getMagasin().getName());
        p.setId(groupeFournisseur.getIdRecepteurPharmaMl());
        return p;
    }

    private MessageEntete buildMessageEntete(Fournisseur fournisseur) {
        MessageEntete me = new MessageEntete();
        me.setEmetteur(buildOfficineEmetteur(fournisseur));
        me.setDestinataire(buildOfficineDestinataire(fournisseur));
        me.setDate(getPharmaMlDate());
        return me;
    }

    private OfficinePartenaire buildOfficineDestinataire(Fournisseur fournisseur) {
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        OfficinePartenaire op = new OfficinePartenaire();
        // op.setIdSociete(fournisseur.getIdentifiantRepartiteur());
        op.setIdSociete(groupeFournisseur.getIdRecepteurPharmaMl());
        op.setCodeSociete(groupeFournisseur.getCodeRecepteurPharmaMl());
        op.setNaturePartenaire(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_RE);
        return op;
    }

    private OfficinePartenaire buildOfficineEmetteur(Fournisseur fournisseur) {
        //  GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        OfficinePartenaire op = new OfficinePartenaire();
        // op.setIdClient(groupeFournisseur.getIdRecepteurPharmaMl());
        op.setIdClient(fournisseur.getIdentifiantRepartiteur());
        op.setNaturePartenaire(PharmaMlUtils.NATURE_PARTENAIRE_VALUE_OF);
        return op;
    }


    private Corps buildCorpsCommande(Commande commande, EnvoiParamsDTO params,
        Fournisseur fournisseur) {
        Corps c = new Corps();
        c.setMessageOfficine(buildMessageOfficineCommande(commande, params, fournisseur));
        return c;
    }

    private MessageOfficine buildMessageOfficineCommande(Commande commande, EnvoiParamsDTO params,
        Fournisseur fournisseur) {
        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(buildMessageCorpsCommande(commande, params));
        return messageOfficine;
    }

    private MessageCorps buildMessageCorpsCommande(Commande commande, EnvoiParamsDTO params) {
        MessageCorps mc = new MessageCorps();
        mc.setCommande(buildCommande(commande, params));
        return mc;
    }

    private com.kobe.warehouse.service.pharmaml.dto.Commande buildCommande(Commande commande,
        EnvoiParamsDTO params) {
        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();

        LocalDate dateLivraison = params.getDateLivraisonSouhaitee() != null
            ? params.getDateLivraisonSouhaitee()
            : LocalDate.now().plusDays(1);
        c.setDateLivraison(dateLivraison.toString());

        String commentaire = params.getCommentaire();
        c.setCommentaireGeneral(
            StringUtils.hasLength(commentaire) ? commentaire : commande.getOrderReference());
        c.setRefCdeClient(commande.getOrderReference());

        if (TypeCommande.EXCEPTIONNELLE.equals(params.getTypeCommande())) {
            Exceptionnelle exceptionnelle = new Exceptionnelle();
            exceptionnelle.setLignes(buildCommandeLignes(commande));
            c.setExceptionnelle(exceptionnelle);
        } else {
            c.setNormale(buildNormale(commande));
        }
        return c;
    }

    private List<LigneN> buildCommandeLignes(Commande commande) {
        AtomicInteger count = new AtomicInteger(1);
        return commande
            .getOrderLines()
            .stream()
            .map(item -> {
                LigneN ligne = new LigneN();
                FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

                String numLigne = org.apache.commons.lang3.StringUtils.leftPad(
                    count.getAndIncrement() + "", 4, '0');
                String quantite = org.apache.commons.lang3.StringUtils.leftPad(
                    item.getQuantityRequested() + "", 4, '0');
                String cip = fournisseurProduit.getCodeCip();

                ligne.setCodeProduit(cip);
                ligne.setQuantite(quantite);
                ligne.setNumLigne(numLigne);
                ligne.setTypeCodification(typeCodification(cip));
                ligne.setPartielle(false);
                ligne.setReliquat(false);
                ligne.setEquivalent(false);
                return ligne;
            })
            .collect(Collectors.toList());
    }

    private Normale buildNormale(Commande commande) {
        Normale n = new Normale();
        n.setLignes(buildCommandeLignes(commande));
        return n;
    }


    private Corps buildCorpsAck(Commande commande, Fournisseur fournisseur) {
        AcqReception acq = new AcqReception();
        acq.setRefCdeOfficine(commande.getOrderReference());
        acq.setRefCdeRepartiteur(commande.getReceiptReference());
        acq.setLignes(commande.getOrderLines().stream()
            .filter(ol -> ol.getQuantityReceived() != null && ol.getQuantityReceived() > 0)
            .map(ol -> {
                String cip = ol.getFournisseurProduit().getCodeCip();
                return new LigneReception()
                    .setCodeProduit(cip)
                    .setTypeCodification(typeCodification(cip))
                    .setQuantiteRecue(ol.getQuantityReceived());
            })
            .toList());

        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();
        c.setRefCdeClient(commande.getOrderReference());
        c.setAcqReception(acq);

        MessageCorps mc = new MessageCorps();
        mc.setCommande(c);

        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(mc);

        Corps corps = new Corps();
        corps.setMessageOfficine(messageOfficine);
        return corps;
    }

    private Corps buildCorpsAnnulation(Commande commande, Fournisseur fournisseur, String motif) {
        Annulation ann = new Annulation()
            .setRefCdeOfficine(commande.getOrderReference())
            .setRefCdeRepartiteur(commande.getReceiptReference())
            .setMotif(StringUtils.hasLength(motif) ? motif : "Annulation demandée par l'officine");

        MessageCorps mc = new MessageCorps();
        mc.setAnnulation(ann);

        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(mc);

        Corps corps = new Corps();
        corps.setMessageOfficine(messageOfficine);
        return corps;
    }

    private Corps buildCorpsRetour(Commande commande, Fournisseur fournisseur,
        List<LigneRetourDTO> lignes) {
        Retour retour = new Retour()
            .setRefCdeOfficine(commande.getOrderReference())
            .setRefBl(commande.getReceiptReference())
            .setLignes(lignes.stream().map(dto -> {
                String cip = dto.codeProduit();
                return new LigneRetour()
                    .setCodeProduit(cip)
                    .setTypeCodification(typeCodification(cip))
                    .setQuantite(dto.quantite())
                    .setMotifRetour(dto.motifRetour());
            }).toList());

        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();
        c.setRefCdeClient(commande.getOrderReference());
        c.setRetour(retour);

        MessageCorps mc = new MessageCorps();
        mc.setCommande(c);

        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(mc);

        Corps corps = new Corps();
        corps.setMessageOfficine(messageOfficine);
        return corps;
    }


    private Corps buildCorpsInfo(Commande commande, Fournisseur fournisseur, String refMessage) {
        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();
        c.setRefCdeClient(refMessage);
        c.setNormale(buildNormale(commande));

        MessageCorps mc = new MessageCorps();
        mc.setCommande(c);

        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(mc);

        Corps corps = new Corps();
        corps.setMessageOfficine(messageOfficine);
        return corps;
    }

    private Corps buildCorpsInfoFromSuggestionLines(List<SuggestionLine> lignes,
        Fournisseur fournisseur, String refMessage) {
        AtomicInteger count = new AtomicInteger(1);
        Normale n = new Normale();
        n.setLignes(lignes.stream().map(sl -> {
            String cip = sl.getFournisseurProduit().getCodeCip();
            LigneN ligne = new LigneN();
            ligne.setNumLigne(
                org.apache.commons.lang3.StringUtils.leftPad(count.getAndIncrement() + "", 4, '0'));
            ligne.setCodeProduit(cip);
            ligne.setTypeCodification(typeCodification(cip));
            ligne.setQuantite(
                org.apache.commons.lang3.StringUtils.leftPad(sl.getQuantity() + "", 4, '0'));
            ligne.setPartielle(false);
            ligne.setReliquat(false);
            ligne.setEquivalent(false);
            return ligne;
        }).toList());

        com.kobe.warehouse.service.pharmaml.dto.Commande c = new com.kobe.warehouse.service.pharmaml.dto.Commande();
        c.setRefCdeClient(refMessage);
        c.setNormale(n);

        MessageCorps mc = new MessageCorps();
        mc.setCommande(c);

        MessageOfficine messageOfficine = new MessageOfficine();
        messageOfficine.setEntete(buildMessageEntete(fournisseur));
        messageOfficine.setCorps(mc);

        Corps corps = new Corps();
        corps.setMessageOfficine(messageOfficine);
        return corps;
    }

    private String typeCodification(String cip) {
        if (cip.length() == 13) {
            return TYPE_CODIFICATION_EAN;
        }
        return TYPE_CODIFICATION_CIP39;
    }

    private String getPharmaMlDate() {
        return LocalDate.now() + "T" + LocalTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}


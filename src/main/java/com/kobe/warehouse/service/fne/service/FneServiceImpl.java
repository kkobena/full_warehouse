package com.kobe.warehouse.service.fne.service;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.fne.model.FneResponse;
import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FneServiceImpl implements FneService {

    private static final Logger log = LoggerFactory.getLogger(FneServiceImpl.class);
    private final FacturationRepository facturationRepository;
    private final  HttpClient httpClient;
    private final StorageService  storageService;

    public FneServiceImpl(FacturationRepository facturationRepository, HttpClient httpClient,
        StorageService storageService) {
        this.facturationRepository = facturationRepository;
        this.httpClient = httpClient;
        this.storageService = storageService;
    }

    @Override
    public void create(FactureItemId factureItemId) throws GenericError{
        createInvoice(facturationRepository.findById(factureItemId)));
    }



    private void createInvoice(FactureTiersPayant factureTiersPayant) throws GenericError {
        Magasin magasin = storageService.getConnectedUserMagasin();
        Client client = getHttpClient();
        JSONObject payload = new JSONObject(buildFromFacture(facture, officine));

        WebTarget myResource = client.target(sp.fneUrl);
        Response response = myResource.request().header("Authorization", "Bearer ".concat(sp.fnePkey))
            .post(Entity.entity(payload.toString(), MediaType.APPLICATION_JSON_TYPE));
        // String fneResponse = response.readEntity(String.class);

        FneResponse fneResponse = response.readEntity(FneResponse.class);
        LOG.log(Level.INFO, "response --- {0}", fneResponse);
        saveResponse(fneResponse, facture);

    }



    private FneInvoice buildFromFacture(TFacture facture, TOfficine officine) {

        TTiersPayant tTiersPayant = facture.getTiersPayant();
        FneInvoice fneInvoice = new FneInvoice();
        fneInvoice.setEstablishment(officine.getStrNOMCOMPLET());
        fneInvoice.setClientCompanyName(tTiersPayant.getStrFULLNAME());
        fneInvoice.setClientEmail(tTiersPayant.getStrMAIL());
        fneInvoice.setClientPhone(tTiersPayant.getStrTELEPHONE());
        fneInvoice.setPointOfSale(sp.fnepointOfSale);
        fneInvoice.setClientNcc(tTiersPayant.getStrCOMPTECONTRIBUABLE());
        List<FneInvoiceItem> fneInvoiceItems = buildFromProduitCodeTva(facture);
        fneInvoice.setItems(fneInvoiceItems);

        // Pour des logs de tests
        // double montantTotalFne = fneInvoiceItems.stream().mapToDouble(FneInvoiceItem::getAmount).sum();
        // LOG.info(String.format("montantHt fne: %s monantFacture: %s", montantTotalFne + "",
        // facture.getDblMONTANTCMDE() + ""));
        // facture.getTFactureDetailCollection().forEach(t -> fneInvoice.getItems().add(buildFrom(t)));//Flatten by code
        // tva
        return fneInvoice;
    }

    private Client getHttpClient() {
        return ClientBuilder.newClient();
    }

    private void saveResponse(FneResponse fneResponse, TFacture facture) {
        FneTiersPayantInvoice fne = new FneTiersPayantInvoice();
        fne.setFacture(facture);
        fne.getResponses().add(fneResponse);
        em.persist(fne);
    }

    private List<Item> getFactureMonatantByTva(String factureId, String tiersPayantId) {
        String sqlQuery = "SELECT d.valeurTva AS codeTva ,SUM(d.int_PRICE) AS montantTTCByCodeTva,cp.int_PERCENT AS taux FROM t_facture_detail fd JOIN t_preenregistrement_detail d ON d.lg_PREENREGISTREMENT_ID=fd.P_KEY JOIN t_preenregistrement_compte_client_tiers_payent cp JOIN t_compte_client_tiers_payant cpt ON cpt.lg_COMPTE_CLIENT_TIERS_PAYANT_ID=cp.lg_COMPTE_CLIENT_TIERS_PAYANT_ID "
            + " JOIN t_tiers_payant tp ON tp.lg_TIERS_PAYANT_ID=cpt.lg_TIERS_PAYANT_ID ON cp.lg_PREENREGISTREMENT_ID=fd.P_KEY  WHERE fd.lg_FACTURE_ID=?1 AND tp.lg_TIERS_PAYANT_ID=?2 GROUP  BY d.valeurTva,cp.int_PERCENT ORDER BY d.valeurTva";
        Query query = em.createNativeQuery(sqlQuery, Tuple.class).setParameter(1, factureId).setParameter(2,
            tiersPayantId);
        List<Tuple> list = query.getResultList();
        return list.stream().map(t -> buildFromTuple(t)).collect(Collectors.toList());

    }

    private Item buildFromTuple(Tuple tuple) {
        return new Item(tuple.get("codeTva", Integer.class),
            tuple.get("montantTTCByCodeTva", BigDecimal.class).intValue(),
            arrondiTauxCouverture(tuple.get("taux", Integer.class)));
    }

    private List<FneInvoiceItem> buildFromProduitCodeTva(TFacture facture) {
        List<Item> itemsByCodeTvaAndByTaux = getFactureMonatantByTva(facture.getLgFACTUREID(),
            facture.getTiersPayant().getLgTIERSPAYANTID());

        List<FneInvoiceItem> fneInvoiceItems = new ArrayList<>();
        String codeFacture = facture.getStrCODEFACTURE();
        String description = "FACTURATION DU " + DateCommonUtils.format(facture.getDtDEBUTFACTURE()) + " AU "
            + DateCommonUtils.format(facture.getDtFINFACTURE());
        Map<Integer, List<Item>> codeTvaMap = itemsByCodeTvaAndByTaux.stream()
            .collect(Collectors.groupingBy(Item::getCodeTva));
        codeTvaMap.forEach((codeTva, values) -> {

            FneInvoiceItem invoiceItem = new FneInvoiceItem();
            invoiceItem.setDescription(description);
            invoiceItem.setReference(codeFacture);
            TaxeEnum taxeEnum = TaxeEnum.getByValue(codeTva);
            invoiceItem.setTaxes(new String[] { taxeEnum.name() });
            invoiceItem.setAmount(computeMontantByTvaAndTaux(codeTva, values));

            fneInvoiceItems.add(invoiceItem);

        });

        return fneInvoiceItems;
    }

    /**
     * Problème probable pour des vente avec remise, prix de reference, des facture avec remise forfetaire
     *
     * @param itemsByTva
     *
     * @return
     */
    private double computeMontantByTvaAndTaux(int codeTva, List<Item> itemsByTva) {

        AtomicDouble montantAtomicHt = new AtomicDouble(0);

        Map<Integer, List<Item>> tauxMap = itemsByTva.stream().collect(Collectors.groupingBy(Item::getTaux));
        tauxMap.forEach((tauxAssure, values) -> {
            int totalTtc = values.stream().mapToInt(Item::getMontantTtc).sum();
            double montantHt = calculHt(totalTtc, codeTva);
            double partAssurence = BigDecimal.valueOf(montantHt).multiply(BigDecimal.valueOf(tauxAssure / 100.f))
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
            montantAtomicHt.addAndGet(partAssurence);

        });
        return montantAtomicHt.get();

    }

    public int arrondiTauxCouverture(int taux) {

        int arrondi = Math.round(taux / 5f) * 5;

        return Math.min(100, arrondi);

    }

    // 1428351F
    private class Item {

        private final int codeTva;
        private final int montantTtc;
        private final int taux;
        // private final int remise;

        public int getCodeTva() {
            return codeTva;
        }

        public int getMontantTtc() {
            return montantTtc;
        }

        public Item(int codeTva, int montantTtc, int taux/* , int remise */) {
            this.codeTva = codeTva;
            this.montantTtc = montantTtc;
            this.taux = taux;
            // this.remise=remise;
        }

        /*
         * public int getRemise() { return remise; }
         */
        public int getTaux() {
            return taux;
        }

        @Override
        public String toString() {
            return "Item{" + "codeTva=" + codeTva + ", montantTtc=" + montantTtc + ", taux=" + taux + '}';
        }

    }

    private double calculHt(int ttc, int tva) {
        return (ttc) * 1.0 / (1 + (tva / 100.f));
    }
}

package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class CommandeReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private CommandeDTO commande;
    private String templateFile;

    public CommandeReportReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super( storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    public byte[] export(CommandeDTO commande) {
        this.commande = commande;
        Magasin magasin = storageService.getUser().getMagasin();
        List<OrderLineDTO> orderLineDTOList = getItems();
        this.templateFile = Constant.COMMANDE_EN_COURS_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.COMMANDE, this.commande);
        getParameters().put(Constant.ITEM_SIZE, orderLineDTOList.size());
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        if (orderLineDTOList.size() > Constant.COMMANDE_PAGE_SIZE) {
            getParameters().put(Constant.COMMANDE_ITEMS, orderLineDTOList.subList(0, Constant.COMMANDE_PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);
            return super.exportMultiplePagesToByteArray();
        } else {
            getParameters().put(Constant.COMMANDE_ITEMS, orderLineDTOList);
            getParameters().put(Constant.IS_LAST_PAGE, true);
            getParameters().put(Constant.PAGE_COUNT, "1/1");
            return super.exportReportToPdf();
        }
    }

    /**
     * Les lignes de la commande, triees par libelle de produit.
     *
     * <p>Le tri appartient a cette methode et non a {@code export} : la pagination rappelle
     * {@code getItems()} pour decouper les pages suivantes. Trier une copie locale laissait donc la
     * premiere page dans l'ordre alphabetique et les suivantes dans l'ordre de la base — certaines
     * lignes paraissaient deux fois, d'autres jamais.
     */
    @Override
    protected List<OrderLineDTO> getItems() {
        List<OrderLineDTO> lignes = new ArrayList<>(this.commande.getOrderLines());
        lignes.sort(Comparator.comparing(OrderLineDTO::getProduitLibelle));
        return lignes;
    }

    @Override
    protected int getMaxiRowCount() {
        return Constant.COMMANDE_PAGE_SIZE;
    }

    /** Le gabarit de la commande lit {@code commande_items}, non la variable commune. */
    @Override
    protected String getItemsParameterName() {
        return Constant.COMMANDE_ITEMS;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(templateFile, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(templateFile, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return this.commande.getOrderReference();
    }
}

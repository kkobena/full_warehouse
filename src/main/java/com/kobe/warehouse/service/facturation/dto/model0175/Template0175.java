package com.kobe.warehouse.service.facturation.dto.model0175;

import com.kobe.warehouse.service.facturation.dto.AbstractModelFactureTemplate;
import com.kobe.warehouse.service.facturation.dto.SummaryRow;
import com.kobe.warehouse.service.facturation.dto.SummaryRowEnum;
import com.kobe.warehouse.service.facturation.dto.TemplateColumn;
import java.util.Set;

public class Template0175 extends AbstractModelFactureTemplate {

    @Override
    public Set<SummaryRow> getSummaryRows() {
        return Set.of(
            new SummaryRow(SummaryRowEnum.REMISE_FORFAITAIRE, false, 0),
            new SummaryRow(SummaryRowEnum.TOTAL_GENERAL, true, 1),
            new SummaryRow(SummaryRowEnum.ARRETE_A_PAYER, true, 2)
        );
    }

    @Override
    public Set<TemplateColumn> getColumns() {
        return Set.of(
            new TemplateColumn("Date", "updated", 0, false),
            new TemplateColumn("Matricule", "num", 1, false),
            new TemplateColumn("Nom et prénom(s)", "customerFullName", 2, false),
            new TemplateColumn("Numéro bon", "numBon", 3, false),
            new TemplateColumn("Montant", "montant", 4, true),
            new TemplateColumn("Remise", "montantRemise", 5, true),
            new TemplateColumn("Montant net", "montantNet", 6, true)
        );
    }

    @Override
    public boolean groupedByCustomer() {
        return true;
    }
}

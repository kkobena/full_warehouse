package com.kobe.warehouse.service.facturation.dto;

import java.util.Set;

public abstract class AbstractModelFactureTemplate {

    public boolean groupedByCustomer() {
        return false;
    }

    public boolean pageBreakAfterEachCustomer() {
        return false;
    }

    public boolean pageSummary() {
        return false;
    }

    protected abstract Set<SummaryRow> getSummaryRows();

    protected abstract Set<TemplateColumn> getColumns();
}

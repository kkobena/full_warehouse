package com.kobe.warehouse.service.excel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericExcelDTO {

    private final List<String> columns = new ArrayList<>();
    private final List<Object[]> data = new ArrayList<>();
    private final List<Integer> columnWidths = new ArrayList<>();

    public int getDefaultWidth() {
        return 15;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Object[]> getData() {
        return data;
    }

    public List<Integer> getColumnWidths() {
        return columnWidths;
    }

    public void addWidths(Integer... widths) {
        this.columnWidths.addAll(Arrays.asList(widths));
    }

    public void addColumn(String... columns) {
        this.columns.addAll(Arrays.asList(columns));
    }

    public void addRow(Object[] row) {
        this.data.add(row);
    }
}

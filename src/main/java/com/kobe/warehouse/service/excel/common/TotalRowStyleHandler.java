package com.kobe.warehouse.service.excel.common;

import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

public class TotalRowStyleHandler implements RowWriteHandler {

    private final int totalRowIndex;

    public TotalRowStyleHandler(int totalRowIndex) {
        this.totalRowIndex = totalRowIndex;
    }

    @Override
    public void afterRowDispose(
        WriteSheetHolder writeSheetHolder,
        WriteTableHolder writeTableHolder,
        Row row,
        Integer rowIndex,
        Boolean isHead
    ) {
        if (rowIndex != null && rowIndex == totalRowIndex + 1) { // +1 à cause de l'en-tête
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            CellStyle style = workbook.createCellStyle();

            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (Cell cell : row) {
                cell.setCellStyle(style);
            }
        }
    }
    /*
    ProductExcelModel total = new ProductExcelModel();
        total.setName("TOTAL");
        total.setQuantity(totalQuantity);
        total.setValue(totalValue);
        data.add(total);

        // Export
        EasyExcel.write("produits.xlsx", ProductExcelModel.class)
                .registerWriteHandler(new TotalRowStyleHandler(data.size() - 1)) // dernière ligne stylée
                .sheet("Produits")
                .doWrite(data);


                 public void exportExcel(HttpServletResponse response) throws Exception {
        List<ProductExcelModel> data = new ArrayList<>();
        data.add(new ProductExcelModel("Paracetamol", 50, 125.0));
        data.add(new ProductExcelModel("Ibuprofen", 30, 90.0));
        data.add(new ProductExcelModel("Aspirin", 20, 45.0));

        // Add totals
        int totalQty = data.stream().mapToInt(ProductExcelModel::getQuantity).sum();
        double totalVal = data.stream().mapToDouble(ProductExcelModel::getValue).sum();
        data.add(new ProductExcelModel("TOTAL", totalQty, totalVal));

        // Set HTTP response headers
        String fileName = URLEncoder.encode("produits.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        // Write Excel
        EasyExcel.write(response.getOutputStream(), ProductExcelModel.class)
                .registerWriteHandler(new TotalRowStyleHandler(data.size() - 1))
                .registerWriteHandler(new TitleSheetWriteHandler("Product Inventory Report", 3))
                .sheet("Produits")
                .doWrite(data);
    }

     String fileName = URLEncoder.encode("produits.csv", StandardCharsets.UTF_8);
        response.setContentType("text/csv");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        // Écriture CSV (pas de style possible)
        EasyExcel.write(response.getOutputStream())
                .csv(true) // Important pour exporter en CSV
                .head(ProductExcelModel.class)
                .sheet("Produits")
                .doWrite(data);



                public class ProductCsvListener extends AnalysisEventListener<ProductExcelModel> {

    private final List<ProductExcelModel> products = new ArrayList<>();

    @Override
    public void invoke(ProductExcelModel data, AnalysisContext context) {
        products.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Ici, tu peux traiter la liste complète des produits
        System.out.println("Import terminé. Nombre de produits: " + products.size());
    }

    public List<ProductExcelModel> getProducts() {
        return products;
    }
}


  ProductCsvListener listener = new ProductCsvListener();

        EasyExcel.read(file.getInputStream(), ProductExcelModel.class, listener)
                .csv()
                .sheet()
                .doRead();

        // Exemple : afficher la liste importée
        listener.getProducts().forEach(System.out::println);
        /  @ExcelProperty(index = 1)
 EasyExcel.read(file.getInputStream(), ProductExcelModel.class, listener)
            .csv()
            .headRowNumber(0)  // Pas d'en-tête
            .sheet()
            .doRead();
     */
}

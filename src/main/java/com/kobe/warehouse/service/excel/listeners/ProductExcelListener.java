package com.kobe.warehouse.service.excel.listeners;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.kobe.warehouse.service.excel.model.ProductExcelModel;

public class ProductExcelListener extends AnalysisEventListener<ProductExcelModel> {

    @Override
    public void invoke(ProductExcelModel data, AnalysisContext context) {
        System.out.println("Read: " + data);
        // Save to DB logic here
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("Import finished.");
    }
    /*
     public void importProducts(MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), ProductExcelModel.class, new ProductExcelListener())
                 .sheet()
                 .doRead();
    }

    public void exportProducts(HttpServletResponse response, List<ProductExcelModel> products) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=products.xlsx");
        EasyExcel.write(response.getOutputStream(), ProductExcelModel.class)
                 .sheet("Products")
                 .doWrite(products);
    }

    import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProductListener<T extends Product> extends AnalysisEventListener<T> {

    private final List<T> products = new ArrayList<>();

    @Override
    public void invoke(T data, AnalysisContext context) {
        products.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Ici tu peux faire du traitement après lecture complète
        System.out.println("Total products read: " + products.size());
    }

    public List<T> getProducts() {
        return products;
    }
}
ProductListener<ProductWithHeader> listener = new ProductListener<>();

    EasyExcel.read(file.getInputStream(), ProductWithHeader.class, listener)
            .sheet()
            .doRead();

    List<ProductWithHeader> products = listener.getProducts();

     ProductListener<ProductWithoutHeader> listener = new ProductListener<>();

    EasyExcel.read(file.getInputStream(), ProductWithoutHeader.class, listener)
            .headRowNumber(0)
            .sheet()
            .doRead();

    List<ProductWithoutHeader> products = listener.getProducts();
    List<Product> convertToParent(List<? extends Product> subList) {
    return new ArrayList<>(subList);
}
     */
}

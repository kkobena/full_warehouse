package com.kobe.warehouse.service.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kobe.warehouse.service.excel.model.ExportFormat;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportUtil {

    public static <T> void writeToResponse(
        HttpServletResponse response,
        ExportFormat format,
        String filename,
        String sheetName,
        Class<T> dtoClass,
        List<T> data
    ) throws IOException {
        String contentType = (format == ExportFormat.CSV)
            ? "text/csv"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String extension = (format == ExportFormat.CSV) ? ".csv" : ".xlsx";

        response.setContentType(contentType);
        response.setCharacterEncoding("utf-8");
        response.setHeader(
            "Content-disposition",
            "attachment;filename=" +
            filename +
            "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
            extension
        );

        EasyExcel.write(response.getOutputStream(), dtoClass)
            .excelType(format == ExportFormat.CSV ? ExcelTypeEnum.CSV : ExcelTypeEnum.XLSX)
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
            .sheet(sheetName)
            .doWrite(data);
    }
}

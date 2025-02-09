package com.kobe.warehouse.service.excel;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.errors.FileStorageException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    private static final Logger LOG = Logger.getLogger(ExcelExportService.class.getName());
    private final Path fileStorageLocation;

    public ExcelExportService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Resource generate(GenericExcelDTO genericExcel, String sheetName, String fileName) throws IOException {
        String filename =
            this.fileStorageLocation.resolve(
                    fileName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".xls"
                )
                .toFile()
                .getAbsolutePath();
        try (HSSFWorkbook workbook = new HSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filename)) {
            HSSFSheet sheet = workbook.createSheet(sheetName);
            createHeaderRow(sheet, genericExcel);
            fillData(sheet, genericExcel.getData());

            workbook.write(fileOut);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "generate", e);
            throw e;
        }
        return new UrlResource(Paths.get(filename).toUri());
    }

    private void createHeaderRow(HSSFSheet sheet, GenericExcelDTO genericExcel) {
        HSSFRow headerRow = sheet.createRow(0);
        if (genericExcel.getColumnWidths().isEmpty()) {
            sheet.setDefaultColumnWidth(genericExcel.getDefaultWidth());
        }
        HSSFCellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        HSSFFont font = sheet.getWorkbook().createFont();
        int index = 0;
        for (Integer w : genericExcel.getColumnWidths()) {
            sheet.setColumnWidth(index, w);
            index++;
        }

        font.setBold(true);
        headerStyle.setFont(font);
        List<String> columns = genericExcel.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            HSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillData(HSSFSheet sheet, List<Object[]> data) {
        int rowNum = 1;
        for (Object[] rowData : data) {
            HSSFRow row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                var rowIndexData = rowData[i];

                switch (rowIndexData) {
                    case Integer _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue((int) rowData[i]);
                    }
                    case Long _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue((long) rowData[i]);
                    }
                    case BigDecimal _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue(new BigDecimal(rowData[i].toString()).longValue());
                    }
                    case Double _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue((double) rowData[i]);
                    }
                    case Short _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue((short) rowData[i]);
                    }
                    case Float _ -> {
                        row.createCell(i).setCellType(CellType.NUMERIC);
                        row.createCell(i).setCellValue((float) rowData[i]);
                    }
                    case Boolean _ -> {
                        row.createCell(i).setCellType(CellType.BOOLEAN);
                        row.createCell(i).setCellValue((boolean) rowData[i]);
                    }
                    case null, default -> row.createCell(i).setCellValue(rowData[i].toString());
                }
            }
        }
    }
}

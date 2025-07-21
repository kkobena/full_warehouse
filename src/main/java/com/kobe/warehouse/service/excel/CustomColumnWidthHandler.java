package com.kobe.warehouse.service.excel;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class CustomColumnWidthHandler implements SheetWriteHandler {

    private final int defaultWidth;

    public CustomColumnWidthHandler(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    @Override
    public void beforeSheetCreate(SheetWriteHandlerContext context) {
        // no-op
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();

        // Try to find the first non-null row, assume it's the header
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                System.err.println("row: " + row);
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        int width = Math.max(cell.getStringCellValue().length(), defaultWidth);
                        try {
                            sheet.setColumnWidth(i, (width + 2) * 256);
                        } catch (IllegalArgumentException e) {
                            sheet.setDefaultColumnWidth(defaultWidth);
                        }
                    }
                }
                break; // Only process first header-like row
            }
        }

        // Fallback if no header row was found
        if (sheet.getPhysicalNumberOfRows() == 0) {
            sheet.setDefaultColumnWidth(defaultWidth);
        }
    }
}

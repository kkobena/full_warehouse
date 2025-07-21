package com.kobe.warehouse.service.excel;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.Sheet;

public class HeaderBasedColumnWidthHandler implements SheetWriteHandler {

    private final int defaultCharWidth;

    public HeaderBasedColumnWidthHandler(int defaultCharWidth) {
        this.defaultCharWidth = defaultCharWidth;
    }

    @Override
    public void beforeSheetCreate(SheetWriteHandlerContext context) {
        // no-op
    }

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        sheet.setDefaultColumnWidth(20);
    }
}

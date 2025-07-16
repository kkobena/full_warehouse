package com.kobe.warehouse.service.excel.common;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Sheet;

public class FooterSheetWriteHandler implements SheetWriteHandler {

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        Footer footer = sheet.getFooter();

        footer.setLeft("© Pharmacy System");
        footer.setCenter("Page &P of &N");
        footer.setRight("Exported on &D");
        /*
        &P : numéro de page

&N : nombre total de pages

&D : date d’impression
         */
    }
}

package com.kobe.warehouse.service.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;

public class DynamicColumnWidthHandler implements CellWriteHandler {

    private final Map<Integer, Integer> columnMaxCharLength;
    private final int defaultCharWidth;

    public DynamicColumnWidthHandler(int defaultCharWidth, Map<Integer, Integer> columnMaxCharLength) {
        this.defaultCharWidth = defaultCharWidth;
        this.columnMaxCharLength = columnMaxCharLength;
    }

    @Override
    public void afterCellDispose(
        WriteSheetHolder writeSheetHolder,
        WriteTableHolder writeTableHolder,
        List<WriteCellData<?>> cellDataList,
        Cell cell,
        Head head,
        Integer relativeRowIndex,
        Boolean isHead
    ) {
        if (isHead && cell != null) {
            int columnIndex = head.getColumnIndex();
            int width = columnMaxCharLength.getOrDefault(columnIndex, defaultCharWidth);
            System.err.println("width: " + width);
            writeSheetHolder.getSheet().setDefaultColumnWidth(width);
            // sheet.setColumnWidth(columnIndex, width);
        }
    }
}

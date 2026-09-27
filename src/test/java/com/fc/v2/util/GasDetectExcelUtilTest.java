package com.fc.v2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.model.custom.GasImportRow;

/**
 * 检漏导入 Excel 解析：表头跳过、空白行跳过、日期/数值单元格文本化
 */
class GasDetectExcelUtilTest {

    private static byte[] buildWorkbookBytes() throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("检漏数据");
        Row head = sheet.createRow(0);
        for (int i = 0; i < GasDetectExcelUtil.HEADERS.length; i++) {
            head.createCell(i).setCellValue(GasDetectExcelUtil.HEADERS[i]);
        }
        // 第2行：全文本
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("GP00");
        r1.createCell(1).setCellValue("2026-09-27 08:30:00");
        r1.createCell(2).setCellValue("15.5");
        r1.createCell(3).setCellValue("116.407396");
        r1.createCell(4).setCellValue("39.9042");
        // 第3行：日期单元格 + 数值单元格
        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("GP01");
        Cell dateCell = r2.createCell(1);
        dateCell.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-09-27 09:10:11"));
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        dateCell.setCellStyle(style);
        r2.createCell(2).setCellValue(20.0);
        r2.createCell(3).setCellValue(116.5);
        r2.createCell(4).setCellValue(39.9);
        // 第4行：整行空白（应跳过）
        sheet.createRow(3);
        // 第5行：部分列
        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("GP02");
        r4.createCell(2).setCellValue("abc");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    @Test
    void parse_skipsHeaderAndBlankRows_formatsCells() throws Exception {
        List<GasImportRow> rows = GasDetectExcelUtil.parse(new ByteArrayInputStream(buildWorkbookBytes()));

        assertEquals(3, rows.size());
        // 行号为 Excel 实际行号（含表头）
        assertEquals(Integer.valueOf(2), rows.get(0).getRowNo());
        assertEquals("GP00", rows.get(0).getSiteNo());
        assertEquals("2026-09-27 08:30:00", rows.get(0).getDetectText());
        assertEquals("15.5", rows.get(0).getConcText());
        assertEquals("116.407396", rows.get(0).getLngText());

        assertEquals(Integer.valueOf(3), rows.get(1).getRowNo());
        // 日期单元格 → 标准时间串；数值不带千分位/科学计数
        assertEquals("2026-09-27 09:10:11", rows.get(1).getDetectText());
        assertEquals("20", rows.get(1).getConcText());
        assertEquals("116.5", rows.get(1).getLngText());

        // 空白行已跳过，第5行为第3条
        assertEquals(Integer.valueOf(5), rows.get(2).getRowNo());
        assertEquals("GP02", rows.get(2).getSiteNo());
        assertEquals("", rows.get(2).getDetectText());
        assertEquals("abc", rows.get(2).getConcText());
    }

    @Test
    void buildTemplate_hasHeaderRow() throws Exception {
        Workbook wb = GasDetectExcelUtil.buildTemplate();
        Sheet sheet = wb.getSheetAt(0);
        assertEquals("管段编号", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("检测时间", sheet.getRow(0).getCell(1).getStringCellValue());
        wb.close();
    }

    @Test
    void buildErrorReport_writesErrorRows() throws Exception {
        TGasImpRow e = new TGasImpRow();
        e.setRowNo(5);
        e.setSiteNo("NOPE");
        e.setDetectText("2026年9月");
        e.setConcText("abc");
        e.setLngText("200");
        e.setLatText("39.9");
        e.setErrMsg("管段编号不存在；检测时间格式错误（支持yyyy-MM-dd HH:mm:ss）");

        Workbook wb = GasDetectExcelUtil.buildErrorReport(java.util.Collections.singletonList(e));
        Sheet sheet = wb.getSheetAt(0);
        assertEquals("错误原因", sheet.getRow(0).getCell(6).getStringCellValue());
        Row row = sheet.getRow(1);
        assertEquals(5.0, row.getCell(0).getNumericCellValue());
        assertEquals("NOPE", row.getCell(1).getStringCellValue());
        assertTrue(row.getCell(6).getStringCellValue().contains("管段编号不存在"));
        wb.close();
    }
}

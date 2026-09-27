package com.fc.v2.util;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.model.custom.GasImportRow;

/**
 * 检漏数据导入 Excel 工具：解析上传文件、生成导入模板与错误报告。
 * 模板列序固定：管段编号 | 检测时间 | 浓度值(ppm) | 经度 | 纬度 | 备注(可选，不导入)
 *
 * @author fuce
 * @date 2026-09-27
 */
public class GasDetectExcelUtil {

    /** 模板表头 */
    public static final String[] HEADERS = {"管段编号", "检测时间", "浓度值(ppm)", "经度", "纬度", "备注"};

    /** 错误报告表头 */
    public static final String[] ERROR_HEADERS = {"行号", "管段编号", "检测时间", "浓度值(ppm)", "经度", "纬度", "错误原因"};

    private GasDetectExcelUtil() {
    }

    /**
     * 解析上传的 Excel（xls/xlsx 均可）：第一个工作表，第 1 行为表头跳过，
     * 自第 2 行起逐行读取，整行空白行跳过。行号为 Excel 实际行号（含表头，从 1 起）。
     */
    public static List<GasImportRow> parse(InputStream in) throws Exception {
        List<GasImportRow> rows = new ArrayList<GasImportRow>();
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(in);
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                return rows;
            }
            // 首行视为表头；末行取物理最后一行
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                GasImportRow item = new GasImportRow();
                item.setRowNo(i + 1);
                item.setSiteNo(cellText(row, 0));
                item.setDetectText(cellText(row, 1));
                item.setConcText(cellText(row, 2));
                item.setLngText(cellText(row, 3));
                item.setLatText(cellText(row, 4));
                if (!item.isBlank()) {
                    rows.add(item);
                }
            }
        } finally {
            if (wb != null) {
                wb.close();
            }
        }
        return rows;
    }

    /** 生成导入模板（表头 + 一行示例） */
    public static Workbook buildTemplate() {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("检漏数据导入模板");
        Row head = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            head.createCell(i).setCellValue(HEADERS[i]);
            sheet.setColumnWidth(i, 20 * 256);
        }
        Row demo = sheet.createRow(1);
        demo.createCell(0).setCellValue("GP00");
        demo.createCell(1).setCellValue("2026-09-27 08:30:00");
        demo.createCell(2).setCellValue("15.5");
        demo.createCell(3).setCellValue("116.407396");
        demo.createCell(4).setCellValue("39.904200");
        demo.createCell(5).setCellValue("示例行，导入前请删除");
        return wb;
    }

    /** 生成错误报告（校验失败行 + 重复跳过行，含具体原因） */
    public static Workbook buildErrorReport(List<TGasImpRow> errorRows) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("校验失败明细");
        Row head = sheet.createRow(0);
        for (int i = 0; i < ERROR_HEADERS.length; i++) {
            head.createCell(i).setCellValue(ERROR_HEADERS[i]);
            sheet.setColumnWidth(i, 20 * 256);
        }
        sheet.setColumnWidth(ERROR_HEADERS.length - 1, 50 * 256);
        int r = 1;
        for (TGasImpRow e : errorRows) {
            Row row = sheet.createRow(r++);
            if (e.getRowNo() != null) {
                row.createCell(0).setCellValue(e.getRowNo());
            }
            row.createCell(1).setCellValue(nullToEmpty(e.getSiteNo()));
            row.createCell(2).setCellValue(nullToEmpty(e.getDetectText()));
            row.createCell(3).setCellValue(nullToEmpty(e.getConcText()));
            row.createCell(4).setCellValue(nullToEmpty(e.getLngText()));
            row.createCell(5).setCellValue(nullToEmpty(e.getLatText()));
            row.createCell(6).setCellValue(nullToEmpty(e.getErrMsg()));
        }
        return wb;
    }

    /** 单元格取文本：日期单元格按 yyyy-MM-dd HH:mm:ss，数值不带千分位/科学计数 */
    private static String cellText(Row row, int col) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(col);
        return cellText(cell);
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try {
                return cellTextFormula(cell);
            } catch (Exception e) {
                return "";
            }
        }
        return cellTextOfType(cell, type);
    }

    private static String cellTextFormula(Cell cell) {
        CellType cached = cell.getCachedFormulaResultType();
        return cellTextOfType(cell, cached);
    }

    private static String cellTextOfType(Cell cell, CellType type) {
        switch (type) {
            case STRING:
                return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
                }
                return new DecimalFormat("0.##########").format(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

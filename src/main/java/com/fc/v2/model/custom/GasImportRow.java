package com.fc.v2.model.custom;

import java.io.Serializable;

/**
 * 检漏数据导入：Excel 解析出的原始行（未校验，全部按文本保留）
 *
 * @author fuce
 * @date 2026-09-27
 */
public class GasImportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Excel 行号（含表头，从 1 起） */
    private Integer rowNo;

    /** 管段编号（原文） */
    private String siteNo;

    /** 检测时间（原文） */
    private String detectText;

    /** 浓度值（原文） */
    private String concText;

    /** 经度（原文） */
    private String lngText;

    /** 纬度（原文） */
    private String latText;

    public GasImportRow() {
    }

    public GasImportRow(Integer rowNo, String siteNo, String detectText, String concText,
                        String lngText, String latText) {
        this.rowNo = rowNo;
        this.siteNo = siteNo;
        this.detectText = detectText;
        this.concText = concText;
        this.lngText = lngText;
        this.latText = latText;
    }

    /** 整行是否为空（所有数据列均为空白） */
    public boolean isBlank() {
        return isEmpty(siteNo) && isEmpty(detectText) && isEmpty(concText)
                && isEmpty(lngText) && isEmpty(latText);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getDetectText() {
        return detectText;
    }

    public void setDetectText(String detectText) {
        this.detectText = detectText;
    }

    public String getConcText() {
        return concText;
    }

    public void setConcText(String concText) {
        this.concText = concText;
    }

    public String getLngText() {
        return lngText;
    }

    public void setLngText(String lngText) {
        this.lngText = lngText;
    }

    public String getLatText() {
        return latText;
    }

    public void setLatText(String latText) {
        this.latText = latText;
    }
}

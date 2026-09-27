package com.fc.v2.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TGasPipeMapper;
import com.fc.v2.model.auto.TGasDetectReport;
import com.fc.v2.model.auto.TGasImpBatch;
import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.model.auto.TGasPipe;
import com.fc.v2.model.custom.GasImportRow;
import com.fc.v2.service.ITGasDetectImportService;
import com.fc.v2.service.ITGasDetectReportService;
import com.fc.v2.service.ITGasImpBatchService;
import com.fc.v2.service.ITGasImpRowService;
import com.fc.v2.util.GasDetectExcelUtil;

/**
 * 检漏数据批量导入 Service业务层处理
 * （解析→逐项校验→成功入库成检测报告、失败/重复留核对行→落批次统计；
 *   去重口径：同一批次号下 管段编号+检测时间 唯一，重复上传自动跳过）
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TGasDetectImportServiceImpl implements ITGasDetectImportService {

    /** 单批次数据行上限 */
    private static final int MAX_ROWS = 5000;

    /** 行落地情形：1已入库 2校验失败退回 3重复跳过 */
    private static final int ROW_OK = 1;
    private static final int ROW_FAIL = 2;
    private static final int ROW_DUP = 3;

    /** 批次情形：0全部成功 1部分失败 2全部失败 */
    private static final int BATCH_ALL_OK = 0;
    private static final int BATCH_PARTIAL = 1;
    private static final int BATCH_ALL_FAIL = 2;

    /** 浓度物理上限(ppm)：100%体积浓度 */
    private static final BigDecimal CONC_MAX = new BigDecimal("1000000");
    /** 经度合理范围 */
    private static final BigDecimal LNG_MAX = new BigDecimal("180");
    /** 纬度合理范围 */
    private static final BigDecimal LAT_MAX = new BigDecimal("90");

    /** 检测时间支持的录入格式（Excel 日期单元格在解析层已转为 yyyy-MM-dd HH:mm:ss） */
    private static final String[] TIME_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd"};

    @javax.annotation.Resource
    private ITGasImpBatchService gasImpBatchService;
    @javax.annotation.Resource
    private ITGasDetectReportService gasDetectReportService;
    @javax.annotation.Resource
    private ITGasImpRowService gasImpRowService;
    @javax.annotation.Resource
    private TGasPipeMapper gasPipeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TGasImpBatch importExcel(InputStream in, String fileName, String batchNo) {
        if (in == null) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        List<GasImportRow> rows;
        try {
            rows = GasDetectExcelUtil.parse(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("Excel文件解析失败，请使用系统模板上传（支持xls/xlsx格式）");
        }
        return importRows(batchNo, fileName, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TGasImpBatch importRows(String batchNo, String fileName, List<GasImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Excel中没有可导入的数据行");
        }
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("单批次最多支持" + MAX_ROWS + "行数据，当前" + rows.size() + "行，请拆分后上传");
        }
        String batch = normalizeBatchNo(batchNo);

        // 管段底档：site_no → 档案（仅正常档案参与存在性校验）
        Map<String, TGasPipe> pipeMap = new HashMap<String, TGasPipe>();
        List<TGasPipe> pipes = this.gasPipeMapper.selectList(
                new QueryWrapper<TGasPipe>().eq("del_flag", 0));
        for (TGasPipe p : pipes) {
            if (p.getSiteNo() != null) {
                pipeMap.put(p.getSiteNo(), p);
            }
        }

        // 同批次已入库记录的去重键集合（管段编号|检测时间）
        Map<String, Boolean> existKeys = new HashMap<String, Boolean>();
        for (TGasDetectReport r : this.gasDetectReportService.listByBatchNo(batch)) {
            if (r.getSiteNo() != null && r.getDetectTime() != null) {
                existKeys.put(dedupKey(r.getSiteNo(), r.getDetectTime()), Boolean.TRUE);
            }
        }
        // 本文件内已受理行的去重键 → 首次出现行号
        Map<String, Integer> fileKeys = new HashMap<String, Integer>();

        List<TGasImpRow> impRows = new ArrayList<TGasImpRow>();
        int success = 0;
        int fail = 0;
        int dup = 0;
        for (GasImportRow row : rows) {
            TGasImpRow imp = new TGasImpRow();
            imp.setBatchNo(batch);
            imp.setRowNo(row.getRowNo());
            imp.setSiteNo(trim(row.getSiteNo()));
            imp.setDetectText(trim(row.getDetectText()));
            imp.setConcText(trim(row.getConcText()));
            imp.setLngText(trim(row.getLngText()));
            imp.setLatText(trim(row.getLatText()));
            imp.setDelFlag(0);

            ParsedRow parsed = new ParsedRow();
            Map<String, String> errors = validateRow(row, pipeMap, parsed);
            if (!errors.isEmpty()) {
                imp.setStatus(ROW_FAIL);
                imp.setItemCode(String.join(",", errors.keySet()));
                imp.setErrMsg(String.join("；", errors.values()));
                fail++;
            } else {
                imp.setQty(parsed.conc);
                String key = dedupKey(parsed.pipe.getSiteNo(), parsed.detectTime);
                Integer firstRowNo = fileKeys.get(key);
                if (firstRowNo != null) {
                    imp.setStatus(ROW_DUP);
                    imp.setErrMsg("与文件内第" + firstRowNo + "行数据重复（管段+检测时间相同）");
                    dup++;
                } else if (existKeys.containsKey(key)) {
                    imp.setStatus(ROW_DUP);
                    imp.setErrMsg("同批次已存在相同记录（管段+检测时间），重复上传自动去重");
                    dup++;
                } else {
                    try {
                        this.gasDetectReportService.insertReport(buildReport(batch, parsed));
                        fileKeys.put(key, row.getRowNo());
                        imp.setStatus(ROW_OK);
                        success++;
                    } catch (DuplicateKeyException e) {
                        // 并发/重复上传兜底：唯一索引拦截，按重复处理
                        imp.setStatus(ROW_DUP);
                        imp.setErrMsg("同批次已存在相同记录（管段+检测时间），重复上传自动去重");
                        existKeys.put(key, Boolean.TRUE);
                        dup++;
                    }
                }
            }
            impRows.add(imp);
        }

        // 核对行覆盖式落库：同一批次重复上传时以最新一次校验结果为准
        this.gasImpRowService.replaceBatchRows(batch, impRows);

        TGasImpBatch batchRecord = new TGasImpBatch();
        batchRecord.setBatchNo(batch);
        batchRecord.setFileName(fileName);
        batchRecord.setTotalRows(rows.size());
        batchRecord.setSuccessRows(success);
        batchRecord.setFailRows(fail);
        batchRecord.setDupRows(dup);
        batchRecord.setStatus(fail == 0 ? BATCH_ALL_OK : (success == 0 ? BATCH_ALL_FAIL : BATCH_PARTIAL));
        batchRecord.setDelFlag(0);
        this.gasImpBatchService.saveOrUpdateByBatchNo(batchRecord);
        return batchRecord;
    }

    /**
     * 逐项校验一行；合法项解析结果写入 parsed。
     * 返回 数据项代号 → 错误原因（有序，空表示整行合法）。
     */
    private Map<String, String> validateRow(GasImportRow row, Map<String, TGasPipe> pipeMap, ParsedRow parsed) {
        Map<String, String> errors = new LinkedHashMap<String, String>();

        String siteNo = trim(row.getSiteNo());
        if (isBlank(siteNo)) {
            errors.put("site_no", "管段编号不能为空");
        } else {
            parsed.pipe = pipeMap.get(siteNo);
            if (parsed.pipe == null) {
                errors.put("site_no", "管段编号不存在");
            }
        }

        String detectText = trim(row.getDetectText());
        if (isBlank(detectText)) {
            errors.put("detect_time", "检测时间不能为空");
        } else {
            parsed.detectTime = parseTime(detectText);
            if (parsed.detectTime == null) {
                errors.put("detect_time", "检测时间格式错误（支持yyyy-MM-dd HH:mm:ss）");
            }
        }

        String concText = trim(row.getConcText());
        if (isBlank(concText)) {
            errors.put("conc_value", "浓度值不能为空");
        } else {
            parsed.conc = parseNumber(concText);
            if (parsed.conc == null) {
                errors.put("conc_value", "浓度值不是有效数字");
            } else if (parsed.conc.compareTo(BigDecimal.ZERO) < 0) {
                errors.put("conc_value", "浓度值不能为负");
            } else if (parsed.conc.compareTo(CONC_MAX) > 0) {
                errors.put("conc_value", "浓度值超出合理范围（0~1000000ppm）");
            }
        }

        String lngText = trim(row.getLngText());
        if (isBlank(lngText)) {
            errors.put("lng", "经度不能为空");
        } else {
            parsed.lng = parseNumber(lngText);
            if (parsed.lng == null) {
                errors.put("lng", "经度不是有效数字");
            } else if (parsed.lng.abs().compareTo(LNG_MAX) > 0) {
                errors.put("lng", "经度超出合理范围（-180~180）");
            }
        }

        String latText = trim(row.getLatText());
        if (isBlank(latText)) {
            errors.put("lat", "纬度不能为空");
        } else {
            parsed.lat = parseNumber(latText);
            if (parsed.lat == null) {
                errors.put("lat", "纬度不是有效数字");
            } else if (parsed.lat.abs().compareTo(LAT_MAX) > 0) {
                errors.put("lat", "纬度超出合理范围（-90~90）");
            }
        }
        return errors;
    }

    /** 组装正式检测报告（管段代号/名称以底档为准留快照） */
    private static TGasDetectReport buildReport(String batchNo, ParsedRow parsed) {
        TGasDetectReport report = new TGasDetectReport();
        report.setBatchNo(batchNo);
        report.setPipeId(parsed.pipe.getId());
        report.setSiteNo(parsed.pipe.getSiteNo());
        report.setSiteName(parsed.pipe.getSiteName());
        report.setDetectTime(parsed.detectTime);
        report.setConcValue(parsed.conc);
        report.setLng(parsed.lng);
        report.setLat(parsed.lat);
        report.setStatus(0);
        report.setDelFlag(0);
        return report;
    }

    /** 检测时间解析：严格匹配（不允许串尾字符），命中任一所支持格式即返回 */
    private static Date parseTime(String text) {
        for (String pattern : TIME_PATTERNS) {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern);
            fmt.setLenient(false);
            ParsePosition pos = new ParsePosition(0);
            Date d = fmt.parse(text, pos);
            if (d != null && pos.getIndex() == text.length()) {
                return d;
            }
        }
        return null;
    }

    /** 数值解析：非数字返回 null */
    private static BigDecimal parseNumber(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 去重键：管段编号|检测时间(yyyy-MM-dd HH:mm:ss) */
    private static String dedupKey(String siteNo, Date detectTime) {
        return siteNo + "|" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(detectTime);
    }

    /** 批次号：空则系统生成 IMP+时间戳+随机位；非空校验合法性 */
    private static String normalizeBatchNo(String batchNo) {
        if (isBlank(batchNo)) {
            return "IMP" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    + "-" + String.format("%04d", new Random().nextInt(10000));
        }
        String batch = batchNo.trim();
        if (batch.length() > 64 || !batch.matches("[0-9A-Za-z_-]+")) {
            throw new IllegalArgumentException("批次号只能包含字母、数字、中划线、下划线，且不超过64位");
        }
        return batch;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /** 单行解析结果（仅校验通过后完整） */
    private static class ParsedRow {
        private TGasPipe pipe;
        private Date detectTime;
        private BigDecimal conc;
        private BigDecimal lng;
        private BigDecimal lat;
    }
}

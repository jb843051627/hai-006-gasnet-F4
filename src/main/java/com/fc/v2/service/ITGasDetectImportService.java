package com.fc.v2.service;

import java.io.InputStream;
import java.util.List;

import com.fc.v2.model.auto.TGasImpBatch;
import com.fc.v2.model.custom.GasImportRow;

/**
 * 检漏数据批量导入 Service接口（excel-import 形状：解析→逐项校验→成功入库成报告、失败留痕→批次统计）
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface ITGasDetectImportService {

    /**
     * 解析并导入 Excel 文件。
     *
     * @param in       文件输入流（xls/xlsx）
     * @param fileName 原始文件名（仅留痕）
     * @param batchNo  批次号，空则系统生成；同一批次号重复上传自动去重
     * @return 批次统计（总行数/成功/失败/重复）
     */
    TGasImpBatch importExcel(InputStream in, String fileName, String batchNo);

    /**
     * 导入编排核心：逐行校验（管段存在性/时间格式/浓度数值/经纬度范围），
     * 成功行写入检测报告，失败行与重复行记入核对行，最后落批次统计。
     * 同一批次（batch_no + 管段编号 + 检测时间）重复的记录自动去重。
     */
    TGasImpBatch importRows(String batchNo, String fileName, List<GasImportRow> rows);
}

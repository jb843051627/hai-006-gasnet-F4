package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.fc.v2.mapper.auto.TGasPipeMapper;
import com.fc.v2.model.auto.TGasDetectReport;
import com.fc.v2.model.auto.TGasImpBatch;
import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.model.auto.TGasPipe;
import com.fc.v2.model.custom.GasImportRow;
import com.fc.v2.service.ITGasDetectReportService;
import com.fc.v2.service.ITGasImpBatchService;
import com.fc.v2.service.ITGasImpRowService;

/**
 * 检漏数据批量导入：逐项校验、同批次去重、批次统计口径
 */
@ExtendWith(MockitoExtension.class)
class TGasDetectImportServiceImplTest {

    @Mock
    private ITGasImpBatchService gasImpBatchService;
    @Mock
    private ITGasDetectReportService gasDetectReportService;
    @Mock
    private ITGasImpRowService gasImpRowService;
    @Mock
    private TGasPipeMapper gasPipeMapper;

    @InjectMocks
    private TGasDetectImportServiceImpl service;

    @BeforeEach
    void stubPipes() {
        TGasPipe p0 = new TGasPipe();
        p0.setId(0L);
        p0.setSiteNo("GP00");
        p0.setSiteName("青坪市城区中压主干管段（在用）");
        TGasPipe p1 = new TGasPipe();
        p1.setId(9L);
        p1.setSiteNo("GP09");
        p1.setSiteName("城西调压站");
        // 共享存根：参数拒绝类用例不触达底档查询，故按 lenient 处理
        lenient().when(gasPipeMapper.selectList(any())).thenReturn(Arrays.asList(p0, p1));
        lenient().when(gasDetectReportService.listByBatchNo(any())).thenReturn(Collections.emptyList());
    }

    private static GasImportRow row(int rowNo, String siteNo, String detect, String conc,
                                    String lng, String lat) {
        return new GasImportRow(rowNo, siteNo, detect, conc, lng, lat);
    }

    private static GasImportRow validRow(int rowNo) {
        return row(rowNo, "GP00", "2026-09-27 08:30:00", "15.5", "116.407396", "39.9042");
    }

    @SuppressWarnings("unchecked")
    private List<TGasImpRow> capturedImpRows(String batchNo) {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(gasImpRowService).replaceBatchRows(eq(batchNo), captor.capture());
        return captor.getValue();
    }

    @Test
    void importRows_allValid_persistReportsAndBatch() {
        when(gasDetectReportService.insertReport(any())).thenReturn(1);

        TGasImpBatch batch = service.importRows("BATCH-1", "a.xlsx",
                Arrays.asList(validRow(2), row(3, "GP09", "2026/09/27 09:00", "20", "116.5", "39.9")));

        assertEquals("BATCH-1", batch.getBatchNo());
        assertEquals(2, batch.getTotalRows());
        assertEquals(2, batch.getSuccessRows());
        assertEquals(0, batch.getFailRows());
        assertEquals(0, batch.getDupRows());
        assertEquals(0, batch.getStatus());
        // 报告落库：管段ID/名称取底档快照
        ArgumentCaptor<TGasDetectReport> reportCaptor = ArgumentCaptor.forClass(TGasDetectReport.class);
        verify(gasDetectReportService, org.mockito.Mockito.times(2)).insertReport(reportCaptor.capture());
        TGasDetectReport r0 = reportCaptor.getAllValues().get(0);
        assertEquals(0L, r0.getPipeId());
        assertEquals("GP00", r0.getSiteNo());
        assertEquals("青坪市城区中压主干管段（在用）", r0.getSiteName());
        assertEquals("BATCH-1", r0.getBatchNo());
        // 核对行全部标记已入库
        List<TGasImpRow> impRows = capturedImpRows("BATCH-1");
        assertEquals(2, impRows.size());
        assertEquals(1, impRows.get(0).getStatus());
        assertEquals(1, impRows.get(1).getStatus());
    }

    @Test
    void importRows_eachValidationError_listedWithReason() {
        when(gasDetectReportService.insertReport(any())).thenReturn(1);

        TGasImpBatch batch = service.importRows("BATCH-2", "a.xlsx", Arrays.asList(
                row(2, "NOPE", "2026-09-27 08:30:00", "15", "116.4", "39.9"),      // 管段编号不存在
                row(3, "GP00", "2026年9月27日", "15", "116.4", "39.9"),             // 检测时间格式错误
                row(4, "GP00", "2026-09-27 08:30:00", "abc", "116.4", "39.9"),      // 浓度值不是有效数字
                row(5, "GP00", "2026-09-27 08:30:00", "15", "200", "39.9"),         // 经度超出范围
                row(6, "GP00", "2026-09-27 08:30:00", "15", "116.4", "-91"),        // 纬度超出范围
                row(7, "GP00", "2026-09-27 08:30:00", "-1", "116.4", "39.9"),       // 浓度为负
                validRow(8)));

        assertEquals(7, batch.getTotalRows());
        assertEquals(1, batch.getSuccessRows());
        assertEquals(6, batch.getFailRows());
        assertEquals(1, batch.getStatus()); // 部分失败

        List<TGasImpRow> impRows = capturedImpRows("BATCH-2");
        assertEquals("管段编号不存在", impRows.get(0).getErrMsg());
        assertTrue(impRows.get(1).getErrMsg().contains("检测时间格式错误"));
        assertEquals("浓度值不是有效数字", impRows.get(2).getErrMsg());
        assertTrue(impRows.get(3).getErrMsg().contains("经度超出合理范围"));
        assertTrue(impRows.get(4).getErrMsg().contains("纬度超出合理范围"));
        assertEquals("浓度值不能为负", impRows.get(5).getErrMsg());
        // 失败行保留原表行次与原文，供错误报告导出
        assertEquals(Integer.valueOf(2), impRows.get(0).getRowNo());
        assertEquals("NOPE", impRows.get(0).getSiteNo());
        assertEquals(Integer.valueOf(2), impRows.get(0).getStatus());
    }

    @Test
    void importRows_multipleErrorsInOneRow_joined() {
        TGasImpBatch batch = service.importRows("BATCH-3", "a.xlsx",
                Collections.singletonList(row(2, "", "", "xyz", "", "39.9")));

        assertEquals(1, batch.getFailRows());
        assertEquals(0, batch.getSuccessRows());
        assertEquals(2, batch.getStatus()); // 全部失败
        List<TGasImpRow> impRows = capturedImpRows("BATCH-3");
        String err = impRows.get(0).getErrMsg();
        assertTrue(err.contains("管段编号不能为空"));
        assertTrue(err.contains("检测时间不能为空"));
        assertTrue(err.contains("浓度值不是有效数字"));
        assertTrue(err.contains("经度不能为空"));
        assertEquals("site_no,detect_time,conc_value,lng", impRows.get(0).getItemCode());
    }

    @Test
    void importRows_duplicateWithinFile_secondRowSkipped() {
        when(gasDetectReportService.insertReport(any())).thenReturn(1);

        TGasImpBatch batch = service.importRows("BATCH-4", "a.xlsx",
                Arrays.asList(validRow(2), validRow(3)));

        assertEquals(1, batch.getSuccessRows());
        assertEquals(1, batch.getDupRows());
        List<TGasImpRow> impRows = capturedImpRows("BATCH-4");
        assertEquals(1, impRows.get(0).getStatus());
        assertEquals(3, impRows.get(1).getStatus());
        assertTrue(impRows.get(1).getErrMsg().contains("第2行"));
    }

    @Test
    void importRows_duplicateAgainstImportedRecord_skipped() throws Exception {
        // 同批次已入库 GP00|2026-09-27 08:30:00 → 重复上传自动去重
        TGasDetectReport exist = new TGasDetectReport();
        exist.setSiteNo("GP00");
        exist.setDetectTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-09-27 08:30:00"));
        when(gasDetectReportService.listByBatchNo("BATCH-5"))
                .thenReturn(Collections.singletonList(exist));

        TGasImpBatch batch = service.importRows("BATCH-5", "a.xlsx",
                Collections.singletonList(validRow(2)));

        assertEquals(0, batch.getSuccessRows());
        assertEquals(1, batch.getDupRows());
        verify(gasDetectReportService, never()).insertReport(any());
        List<TGasImpRow> impRows = capturedImpRows("BATCH-5");
        assertEquals(3, impRows.get(0).getStatus());
        assertTrue(impRows.get(0).getErrMsg().contains("自动去重"));
    }

    @Test
    void importRows_uniqueKeyConflictOnInsert_treatedAsDuplicate() {
        // 并发重复上传兜底：唯一索引拦截按重复计
        when(gasDetectReportService.insertReport(any()))
                .thenThrow(new DuplicateKeyException("uk_gas_report_batch_row"));

        TGasImpBatch batch = service.importRows("BATCH-6", "a.xlsx",
                Collections.singletonList(validRow(2)));

        assertEquals(0, batch.getSuccessRows());
        assertEquals(1, batch.getDupRows());
        List<TGasImpRow> impRows = capturedImpRows("BATCH-6");
        assertEquals(3, impRows.get(0).getStatus());
    }

    @Test
    void importRows_blankBatchNo_systemGenerated() {
        when(gasDetectReportService.insertReport(any())).thenReturn(1);

        TGasImpBatch batch = service.importRows(" ", "a.xlsx",
                Collections.singletonList(validRow(2)));

        assertTrue(batch.getBatchNo().startsWith("IMP"));
        assertEquals(1, batch.getSuccessRows());
    }

    @Test
    void importRows_illegalBatchNo_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.importRows("批次#1", "a.xlsx", Collections.singletonList(validRow(2))));
    }

    @Test
    void importRows_emptyOrOversize_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.importRows("B-1", "a.xlsx", new ArrayList<GasImportRow>()));

        List<GasImportRow> huge = new ArrayList<GasImportRow>();
        for (int i = 0; i < 5001; i++) {
            huge.add(validRow(i + 2));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.importRows("B-1", "a.xlsx", huge));
    }

    @Test
    void importRows_timeFormats_strictAndLenient() {
        when(gasDetectReportService.insertReport(any())).thenReturn(1);

        TGasImpBatch batch = service.importRows("BATCH-7", "a.xlsx", Arrays.asList(
                row(2, "GP00", "2026-09-27", "1", "116.4", "39.9"),                 // 仅日期
                row(3, "GP00", "2026-09-27 08:30", "1", "116.4", "39.9"),           // 到分钟
                row(4, "GP00", "2026-09-27 08:30:00尾部", "1", "116.4", "39.9"),    // 串尾字符 → 失败
                row(5, "GP00", "2026-13-45 99:99:99", "1", "116.4", "39.9")));      // 非法历法 → 失败

        assertEquals(2, batch.getSuccessRows());
        assertEquals(2, batch.getFailRows());
        List<TGasImpRow> impRows = capturedImpRows("BATCH-7");
        assertTrue(impRows.get(2).getErrMsg().contains("检测时间格式错误"));
        assertTrue(impRows.get(3).getErrMsg().contains("检测时间格式错误"));
    }

    @Test
    void importRows_reuploadSameBatch_replacesCheckRows() {
        // 同批次重复上传：旧核对行作废重写（replaceBatchRows 语义），报告侧按去重键跳过
        when(gasDetectReportService.insertReport(any())).thenReturn(1);
        service.importRows("BATCH-8", "a.xlsx", Collections.singletonList(validRow(2)));

        TGasDetectReport exist = new TGasDetectReport();
        exist.setSiteNo("GP00");
        exist.setDetectTime(new Date());
        when(gasDetectReportService.listByBatchNo("BATCH-8"))
                .thenReturn(Collections.singletonList(exist));

        service.importRows("BATCH-8", "a.xlsx", Collections.singletonList(validRow(2)));
        // 两次上传均以该批次号重写核对行
        verify(gasImpRowService, org.mockito.Mockito.times(2))
                .replaceBatchRows(eq("BATCH-8"), any());
    }
}

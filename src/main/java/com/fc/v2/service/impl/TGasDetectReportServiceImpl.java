package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TGasDetectReportMapper;
import com.fc.v2.model.auto.TGasDetectReport;
import com.fc.v2.service.ITGasDetectReportService;

/**
 * 检漏检测报告 Service业务层处理
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TGasDetectReportServiceImpl implements ITGasDetectReportService {

    @javax.annotation.Resource
    private TGasDetectReportMapper gasDetectReportMapper;

    @Override
    public TGasDetectReport selectTGasDetectReportById(Long id) {
        return this.gasDetectReportMapper.selectById(id);
    }

    @Override
    public List<TGasDetectReport> selectTGasDetectReportList(QueryWrapper<TGasDetectReport> queryWrapper) {
        return this.gasDetectReportMapper.selectList(queryWrapper);
    }

    @Override
    public List<TGasDetectReport> listByBatchNo(String batchNo) {
        return this.gasDetectReportMapper.selectList(new QueryWrapper<TGasDetectReport>()
                .eq("batch_no", batchNo).eq("del_flag", 0));
    }

    @Override
    public int insertReport(TGasDetectReport report) {
        int rows = this.gasDetectReportMapper.insert(report);
        // 主键(雪花)在 insert 时落定，再补报告编号，保证同秒批量入库不撞号
        if (report.getReportNo() == null || report.getReportNo().trim().isEmpty()) {
            report.setReportNo(generateReportNo(report));
            this.gasDetectReportMapper.updateById(report);
        }
        return rows;
    }

    /** 报告编号：RPT + 检测时间(yyyyMMddHHmmss) + 主键末4位（与评定记录号 LC... 同风格） */
    private static String generateReportNo(TGasDetectReport report) {
        String tail = report.getId() == null ? "0000"
                : String.format("%04d", Math.abs(report.getId() % 10000L));
        String ts = report.getDetectTime() == null ? "00000000000000"
                : new SimpleDateFormat("yyyyMMddHHmmss").format(report.getDetectTime());
        return "RPT" + ts + "-" + tail;
    }
}

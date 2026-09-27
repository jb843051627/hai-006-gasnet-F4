package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TGasDetectReport;

/**
 * 检漏检测报告 Service接口
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface ITGasDetectReportService {

    /** 按主键回查 */
    TGasDetectReport selectTGasDetectReportById(Long id);

    /** 检测报告台账查询（批次号/检测日期范围/管段等条件由 queryWrapper 组装） */
    List<TGasDetectReport> selectTGasDetectReportList(QueryWrapper<TGasDetectReport> queryWrapper);

    /** 该批次已入库报告（去重判定用） */
    List<TGasDetectReport> listByBatchNo(String batchNo);

    /** 单条入库，主键落定后回填报告编号 */
    int insertReport(TGasDetectReport report);
}

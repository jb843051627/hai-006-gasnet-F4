package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TGasImpBatch;

/**
 * 检漏数据导入批次 Service接口
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface ITGasImpBatchService {

    /** 按批次号回查批次 */
    TGasImpBatch selectByBatchNo(String batchNo);

    /** 批次台账分页查询 */
    List<TGasImpBatch> selectTGasImpBatchList(QueryWrapper<TGasImpBatch> queryWrapper);

    /** 落批次记录：批次号已存在则按最新一次上传更新统计，否则新建 */
    int saveOrUpdateByBatchNo(TGasImpBatch batch);
}

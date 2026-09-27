package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TGasImpBatchMapper;
import com.fc.v2.model.auto.TGasImpBatch;
import com.fc.v2.service.ITGasImpBatchService;

/**
 * 检漏数据导入批次 Service业务层处理
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TGasImpBatchServiceImpl implements ITGasImpBatchService {

    @javax.annotation.Resource
    private TGasImpBatchMapper gasImpBatchMapper;

    @Override
    public TGasImpBatch selectByBatchNo(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return this.gasImpBatchMapper.selectOne(new QueryWrapper<TGasImpBatch>()
                .eq("batch_no", batchNo.trim()).eq("del_flag", 0)
                .last("limit 1"));
    }

    @Override
    public List<TGasImpBatch> selectTGasImpBatchList(QueryWrapper<TGasImpBatch> queryWrapper) {
        return this.gasImpBatchMapper.selectList(queryWrapper);
    }

    @Override
    public int saveOrUpdateByBatchNo(TGasImpBatch batch) {
        TGasImpBatch exist = selectByBatchNo(batch.getBatchNo());
        if (exist == null) {
            return this.gasImpBatchMapper.insert(batch);
        }
        // 同一批次重复上传：统计口径以最新一次上传为准
        batch.setId(exist.getId());
        return this.gasImpBatchMapper.updateById(batch);
    }
}

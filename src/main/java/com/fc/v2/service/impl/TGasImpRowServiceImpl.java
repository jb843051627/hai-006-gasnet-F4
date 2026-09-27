package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TGasImpRowMapper;
import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.service.ITGasImpRowService;

/**
 * 检漏数据导入核对行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TGasImpRowServiceImpl implements ITGasImpRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;
    private static final int STATUS_DUP = 3;

    @javax.annotation.Resource
    private TGasImpRowMapper gasImpRowMapper;

    @Override
    public TGasImpRow selectTGasImpRowById(Long id) {
        return this.gasImpRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TGasImpRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TGasImpRow> errors = new java.util.ArrayList<TGasImpRow>();
        int seq = 0;
        for (TGasImpRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.gasImpRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TGasImpRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.gasImpRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TGasImpRow> listErrors(String batchNo) {
        return this.gasImpRowMapper.selectList(new QueryWrapper<TGasImpRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }

    @Override
    public int replaceBatchRows(String batchNo, List<TGasImpRow> rows) {
        // 同一批次重复上传：旧核对行整体作废，以最新一次上传的校验结果为准
        this.gasImpRowMapper.delete(new QueryWrapper<TGasImpRow>().eq("batch_no", batchNo));
        int ok = 0;
        if (rows != null) {
            for (TGasImpRow r : rows) {
                ok += this.gasImpRowMapper.insert(r);
            }
        }
        return ok;
    }

    @Override
    public List<TGasImpRow> listErrorRows(String batchNo) {
        // 2校验失败退回 3重复跳过，均列入错误报告供修正后重新上传
        return this.gasImpRowMapper.selectList(new QueryWrapper<TGasImpRow>()
                .eq("batch_no", batchNo)
                .in("status", STATUS_FAIL, STATUS_DUP)
                .orderByAsc("row_no"));
    }
}

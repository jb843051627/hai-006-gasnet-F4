package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TGasImpRow;

/**
 * 检漏数据导入核对行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITGasImpRowService {

    /** 按主键回查单行 */
    TGasImpRow selectTGasImpRowById(Long id);

    /**
     * 整批提交：逐行校验，合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TGasImpRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TGasImpRow> listErrors(String batchNo);

    /**
     * 覆盖式保存某批次的核对行：先清除该批次旧行再整批写入（同一批次重复上传时核对结果以最新一次为准）。
     * 返回写入行数。
     */
    int replaceBatchRows(String batchNo, List<TGasImpRow> rows);

    /** 该批次需列入错误报告的行（校验失败 + 重复跳过，按行号升序） */
    List<TGasImpRow> listErrorRows(String batchNo);
}

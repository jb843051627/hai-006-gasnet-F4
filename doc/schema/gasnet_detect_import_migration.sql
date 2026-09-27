-- 检漏数据批量导入 —— 存量库迁移脚本（在 hai_006 库执行）
-- 说明：doc/schema/gasnet.sql 已含全量建表口径，本脚本用于已部署库的增量升级。

-- 1. 核对行表补齐导入明细列
ALTER TABLE t_gas_imp_row
  ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '管段编号(原文)' AFTER row_no,
  ADD COLUMN detect_text varchar(64) DEFAULT NULL COMMENT '检测时间(原文)' AFTER site_no,
  ADD COLUMN conc_text varchar(64) DEFAULT NULL COMMENT '浓度值(原文)' AFTER detect_text,
  ADD COLUMN lng_text varchar(64) DEFAULT NULL COMMENT '经度(原文)' AFTER conc_text,
  ADD COLUMN lat_text varchar(64) DEFAULT NULL COMMENT '纬度(原文)' AFTER lng_text,
  ADD COLUMN err_msg varchar(500) DEFAULT NULL COMMENT '校验失败/重复跳过原因' AFTER lat_text,
  MODIFY COLUMN status int DEFAULT NULL COMMENT '行落地情形 0待核 1已入库 2校验失败退回 3重复跳过',
  ADD KEY idx_gas_imp_row_batch (batch_no);

-- 2. 导入批次表
CREATE TABLE IF NOT EXISTS t_gas_imp_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '检漏数据上传批次号',
  file_name varchar(255) DEFAULT NULL COMMENT '上传文件名',
  total_rows int DEFAULT '0' COMMENT '数据总行数',
  success_rows int DEFAULT '0' COMMENT '校验成功入库行数',
  fail_rows int DEFAULT '0' COMMENT '校验失败行数',
  dup_rows int DEFAULT '0' COMMENT '重复跳过行数',
  status int DEFAULT NULL COMMENT '批次情形 0全部成功 1部分失败 2全部失败',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gas_imp_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检漏数据导入批次';

-- 3. 检漏检测报告表（导入成功行的正式记录；批次号+管段+检测时间唯一，支撑重复上传去重）
CREATE TABLE IF NOT EXISTS t_gas_detect_report (
  id bigint NOT NULL COMMENT '主键',
  report_no varchar(64) DEFAULT NULL COMMENT '检测报告编号',
  batch_no varchar(64) DEFAULT NULL COMMENT '来源上传批次号',
  pipe_id bigint DEFAULT NULL COMMENT '管段底档 t_gas_pipe.id',
  site_no varchar(64) DEFAULT NULL COMMENT '管段编号',
  site_name varchar(128) DEFAULT NULL COMMENT '管段名称(快照)',
  detect_time datetime DEFAULT NULL COMMENT '现场检测时间',
  conc_value decimal(12,2) DEFAULT NULL COMMENT '浓度值(ppm)',
  lng decimal(10,6) DEFAULT NULL COMMENT '经度',
  lat decimal(10,6) DEFAULT NULL COMMENT '纬度',
  status int DEFAULT '0' COMMENT '记录情形 0有效 1作废',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gas_report_batch_row (batch_no, site_no, detect_time),
  KEY idx_gas_report_detect_time (detect_time),
  KEY idx_gas_report_pipe (pipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检漏检测报告(批量导入正式记录)';

-- 检漏数据批量导入 / 检测报告台账 —— 菜单权限种子
-- 执行后请在 系统管理 → 角色管理 中为相应角色勾选授权；id 可按库内实际占用调整。
INSERT INTO `t_sys_permission` VALUES (991000000000000001, '燃气检测管理', '燃气检测管理目录', '', 0, 0, '', 0, 'layui-icon layui-icon-fire', 90, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_sys_permission` VALUES (991000000000000002, '检漏数据导入', '检漏数据Excel批量导入', '/gasDetectImport/view', 0, 991000000000000001, 'gasDetectImport:view', 1, 'layui-icon layui-icon-upload', 1, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_sys_permission` VALUES (991000000000000003, '导入批次查询', '导入批次台账查询', '/gasDetectImport/batchList', 0, 991000000000000002, 'gasDetectImport:list', 2, '', NULL, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_sys_permission` VALUES (991000000000000004, '检漏数据上传', 'Excel上传校验入库', '/gasDetectImport/upload', 0, 991000000000000002, 'gasDetectImport:upload', 2, '', NULL, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_sys_permission` VALUES (991000000000000005, '检测报告台账', '检漏检测报告台账', '/gasDetectReport/view', 0, 991000000000000001, 'gasDetectReport:view', 1, 'layui-icon layui-icon-form', 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_sys_permission` VALUES (991000000000000006, '检测报告查询', '按批次号/检测日期范围/管段查询', '/gasDetectReport/list', 0, 991000000000000005, 'gasDetectReport:list', 2, '', NULL, 0, NULL, NULL, NULL, NULL, NULL);

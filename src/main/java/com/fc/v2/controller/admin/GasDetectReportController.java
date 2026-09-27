package com.fc.v2.controller.admin;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TGasDetectReport;
import com.fc.v2.service.ITGasDetectReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 检漏检测报告 Controller（已入库检测记录台账查询）
 *
 * @author fuce
 * @date 2026-09-27
 */
@Api(value = "检漏检测报告")
@Controller
@RequestMapping("/gasDetectReport")
public class GasDetectReportController extends BaseController {

    private final String prefix = "admin/gasDetectReport";

    @Autowired
    private ITGasDetectReportService gasDetectReportService;

    @ApiOperation(value = "检测报告台账跳转", notes = "检测报告台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("gasDetectReport:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "检漏检测报告查询", action = "list")
    @ApiOperation(value = "检测报告台账", notes = "按批次号/检测日期范围/管段ID/管段编号查询已入库检测记录")
    @GetMapping("/list")
    @RequiresPermissions("gasDetectReport:list")
    @ResponseBody
    public ResultTable list(TGasDetectReport record,
                            @RequestParam(value = "beginTime", required = false) Date beginTime,
                            @RequestParam(value = "endTime", required = false) Date endTime) {
        QueryWrapper<TGasDetectReport> queryWrapper = new QueryWrapper<TGasDetectReport>();
        queryWrapper.eq("del_flag", 0).eq("status", 0);
        if (record.getBatchNo() != null && !record.getBatchNo().isEmpty()) {
            queryWrapper.like("batch_no", record.getBatchNo());
        }
        if (record.getPipeId() != null) {
            queryWrapper.eq("pipe_id", record.getPipeId());
        }
        if (record.getSiteNo() != null && !record.getSiteNo().isEmpty()) {
            queryWrapper.like("site_no", record.getSiteNo());
        }
        if (record.getSiteName() != null && !record.getSiteName().isEmpty()) {
            queryWrapper.like("site_name", record.getSiteName());
        }
        if (beginTime != null) {
            // 时段绑定走墙钟字符串，规避 JDBC serverTimezone 偏移
            queryWrapper.ge("detect_time",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(beginTime));
        }
        if (endTime != null) {
            queryWrapper.le("detect_time",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime));
        }
        queryWrapper.orderByDesc("detect_time");
        startPage();
        com.github.pagehelper.PageInfo<TGasDetectReport> page =
                new com.github.pagehelper.PageInfo<TGasDetectReport>(gasDetectReportService.selectTGasDetectReportList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }
}

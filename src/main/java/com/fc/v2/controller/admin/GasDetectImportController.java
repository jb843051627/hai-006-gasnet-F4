package com.fc.v2.controller.admin;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TGasImpBatch;
import com.fc.v2.model.auto.TGasImpRow;
import com.fc.v2.service.ITGasDetectImportService;
import com.fc.v2.service.ITGasImpBatchService;
import com.fc.v2.service.ITGasImpRowService;
import com.fc.v2.util.GasDetectExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 检漏数据批量导入 Controller（Excel上传校验 + 批次台账 + 模板/错误报告下载）
 *
 * @author fuce
 * @date 2026-09-27
 */
@Api(value = "检漏数据批量导入")
@Controller
@RequestMapping("/gasDetectImport")
public class GasDetectImportController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(GasDetectImportController.class);

    private final String prefix = "admin/gasDetectImport";

    @Autowired
    private ITGasDetectImportService gasDetectImportService;

    @Autowired
    private ITGasImpBatchService gasImpBatchService;

    @Autowired
    private ITGasImpRowService gasImpRowService;

    @ApiOperation(value = "检漏数据导入跳转", notes = "检漏数据导入跳转")
    @GetMapping("/view")
    @RequiresPermissions("gasDetectImport:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "检漏数据Excel批量导入", action = "upload")
    @ApiOperation(value = "Excel批量导入", notes = "逐项校验，成功行入库生成检测报告，失败行可下载错误报告；同批次重复上传自动去重")
    @PostMapping("/upload")
    @RequiresPermissions("gasDetectImport:upload")
    @ResponseBody
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "batchNo", required = false) String batchNo) {
        if (file == null || file.isEmpty()) {
            return error("请选择要上传的Excel文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null
                || !(fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"))) {
            return error("仅支持xls/xlsx格式的Excel文件");
        }
        try (InputStream in = file.getInputStream()) {
            TGasImpBatch batch = gasDetectImportService.importExcel(in, fileName, batchNo);
            String msg = "导入完成：共" + batch.getTotalRows() + "行，成功" + batch.getSuccessRows()
                    + "行，失败" + batch.getFailRows() + "行，重复跳过" + batch.getDupRows() + "行";
            if (batch.getFailRows() > 0) {
                msg += "，可下载错误报告修正后重新上传";
            }
            return success(200, msg, batch);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            log.error("检漏数据导入失败", e);
            return error("导入失败：" + e.getMessage());
        }
    }

    @Log(title = "检漏导入批次查询", action = "list")
    @ApiOperation(value = "导入批次台账", notes = "按批次号/批次情形查询导入批次")
    @GetMapping("/batchList")
    @RequiresPermissions("gasDetectImport:list")
    @ResponseBody
    public ResultTable batchList(TGasImpBatch record) {
        QueryWrapper<TGasImpBatch> queryWrapper = new QueryWrapper<TGasImpBatch>();
        queryWrapper.eq("del_flag", 0);
        if (record.getBatchNo() != null && !record.getBatchNo().isEmpty()) {
            queryWrapper.like("batch_no", record.getBatchNo());
        }
        if (record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        queryWrapper.orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TGasImpBatch> page =
                new com.github.pagehelper.PageInfo<TGasImpBatch>(gasImpBatchService.selectTGasImpBatchList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "检漏导入模板下载", action = "template")
    @ApiOperation(value = "导入模板下载", notes = "下载检漏数据导入Excel模板")
    @GetMapping("/template")
    @RequiresPermissions("gasDetectImport:view")
    public void template(HttpServletResponse response) throws Exception {
        Workbook wb = GasDetectExcelUtil.buildTemplate();
        writeExcel(response, wb, "检漏数据导入模板.xlsx");
    }

    @Log(title = "检漏导入错误报告下载", action = "errorReport")
    @ApiOperation(value = "错误报告下载", notes = "按批次号下载校验失败/重复跳过行明细，修正后可重新上传")
    @GetMapping("/errorReport")
    @RequiresPermissions("gasDetectImport:view")
    public void errorReport(@RequestParam("batchNo") String batchNo, HttpServletResponse response) throws Exception {
        List<TGasImpRow> errorRows = gasImpRowService.listErrorRows(batchNo);
        Workbook wb = GasDetectExcelUtil.buildErrorReport(errorRows);
        writeExcel(response, wb, "检漏导入错误报告-" + batchNo + ".xlsx");
    }

    /** 工作簿写出为下载响应（文件名走UTF-8编码，兼容中文名） */
    private static void writeExcel(HttpServletResponse response, Workbook wb, String fileName) throws Exception {
        try {
            response.reset();
            response.setContentType("application/vnd.ms-excel; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + URLEncoder.encode(fileName, "UTF-8") + "\"");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        } finally {
            wb.close();
        }
    }
}

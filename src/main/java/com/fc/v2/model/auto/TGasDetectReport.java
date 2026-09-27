package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 检漏检测报告对象 t_gas_detect_report
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_gas_detect_report")
@ApiModel(value = "TGasDetectReport", description = "检漏检测报告(批量导入正式记录)")
public class TGasDetectReport implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 检测报告编号 */
    @TableField("report_no")
    @ApiModelProperty(value = "检测报告编号")
    private String reportNo;

    /** 来源上传批次号 */
    @TableField("batch_no")
    @ApiModelProperty(value = "来源上传批次号")
    private String batchNo;

    /** 管段底档 t_gas_pipe.id */
    @TableField("pipe_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "管段底档id")
    private Long pipeId;

    /** 管段编号 */
    @TableField("site_no")
    @ApiModelProperty(value = "管段编号")
    private String siteNo;

    /** 管段名称(快照) */
    @TableField("site_name")
    @ApiModelProperty(value = "管段名称(快照)")
    private String siteName;

    /** 现场检测时间 */
    @TableField("detect_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "现场检测时间")
    private Date detectTime;

    /** 浓度值(ppm) */
    @TableField("conc_value")
    @ApiModelProperty(value = "浓度值(ppm)")
    private BigDecimal concValue;

    /** 经度 */
    @TableField("lng")
    @ApiModelProperty(value = "经度")
    private BigDecimal lng;

    /** 纬度 */
    @TableField("lat")
    @ApiModelProperty(value = "纬度")
    private BigDecimal lat;

    /** 记录情形 0有效 1作废 */
    @TableField("status")
    @ApiModelProperty(value = "记录情形 0有效 1作废")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getPipeId() {
        return pipeId;
    }

    public void setPipeId(Long pipeId) {
        this.pipeId = pipeId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Date getDetectTime() {
        return detectTime;
    }

    public void setDetectTime(Date detectTime) {
        this.detectTime = detectTime;
    }

    public BigDecimal getConcValue() {
        return concValue;
    }

    public void setConcValue(BigDecimal concValue) {
        this.concValue = concValue;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

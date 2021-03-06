package com.payease.scfordermis.bo.responseBo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Created by zhoushijie on 2018/1/11.
 * 出参
 */

@ApiModel(value = "销售区域列表实体",description = "描述销售区域列表")
public class RespAreaListBean {
    @ApiModelProperty(value = "主键ID",dataType = "long")
    private Long fId;
    @ApiModelProperty(value = "上级区域ID",dataType = "long")
    private Long fParentId;
    @ApiModelProperty(value = "区域名称",dataType = "string")
    private String fName;

    @ApiModelProperty(value = "区域设置集合",dataType = "string",required = true)
    private List<RespAreaListBean> childList;

    public Long getfId() {
        return fId;
    }

    public void setfId(Long fId) {
        this.fId = fId;
    }

    public Long getfParentId() {
        return fParentId;
    }

    public void setfParentId(Long fParentId) {
        this.fParentId = fParentId;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public List<RespAreaListBean> getChildList() {
        return childList;
    }

    public void setChildList(List<RespAreaListBean> childList) {
        this.childList = childList;
    }
}

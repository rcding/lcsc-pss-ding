package com.lcsc.ding.core.model;

import java.util.Date;

/**
 * 漏卡详情
 */
public class NoSignModel {

    /**
     * 日期
     */
    private Date noSignDay;

    /**
     * 未打卡时间
     */
    private Date noSignTime;

    /**
     * 是否申请审批
     */
    private Boolean hasProcess;

    public Date getNoSignDay() {
        return noSignDay;
    }

    public void setNoSignDay(Date noSignDay) {
        this.noSignDay = noSignDay;
    }

    public Date getNoSignTime() {
        return noSignTime;
    }

    public void setNoSignTime(Date noSignTime) {
        this.noSignTime = noSignTime;
    }

    public Boolean getHasProcess() {
        return hasProcess;
    }

    public void setHasProcess(Boolean hasProcess) {
        this.hasProcess = hasProcess;
    }
}
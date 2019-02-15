package com.lcsc.ding.core.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交通补贴
 */
public class SubsidyModel {

    /**
     * 日期
     */
    private Date processDay;

    /**
     * 金额
     */
    private BigDecimal money;

    /**
     * 审批是否已同意
     */
    private Boolean isAgree;

    public Date getProcessDay() {
        return processDay;
    }

    public void setProcessDay(Date processDay) {
        this.processDay = processDay;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public Boolean getAgree() {
        return isAgree;
    }

    public void setAgree(Boolean agree) {
        isAgree = agree;
    }
}
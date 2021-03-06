package com.payease.scfordermis.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangzhili on 2018/1/19.
 */
public class OrderEntity {



    /**
     * 订单状态
     */
    public enum OrderStatus {
        ForCheck("1", "待订单审核", "待订单审核"), ForPick("2", "待提货", "待提货"), Completed
            ("3", "已完成", "已完成"), Cancelled("-1", "已作废", "已作废");
        public static Map<String, OrderStatus> orderStatusEnum = new HashMap();
        static {
            for (OrderStatus status : OrderStatus.values()) {
                orderStatusEnum.put(status.getValue(), status);
            }
        }
        private String value;
        private String viewPage;
        private String desc;
        OrderStatus(String value, String viewPage, String desc) {
            this.value = value;
            this.viewPage = viewPage;
            this.desc = desc;
        }

        public String getViewPage() {
            return viewPage;
        }
        public void setViewPage(String viewPage) {
            this.viewPage = viewPage;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }


    /**
     * 订单付款状态
     */
    public enum OrderPayStatus{
        Wait("wait", "待付款", "待付款"), Pary("part", "部分付款", "部分付款"), Completed
            ("completed", "已付款", "已付款");
        public static Map<String, OrderPayStatus> orderPayStatusEnum = new HashMap();
        static {
            for (OrderPayStatus status : OrderPayStatus.values()) {
                orderPayStatusEnum.put(status.getValue(), status);
            }
        }
        private String value;
        private String viewPage;
        private String desc;
        OrderPayStatus(String value, String viewPage, String desc) {
            this.value = value;
            this.viewPage = viewPage;
            this.desc = desc;
        }

        public String getViewPage() {
            return viewPage;
        }

        public void setViewPage(String viewPage) {
            this.viewPage = viewPage;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

}

package org.commonlib.Exception;

public enum ErrorCode {
    PRODUCT_NOT_FOUND(1001, "product not found"),
    PRODUCT_SERVICE_UNAVAILABLE(1002, "product service unavailable"),
    INVENTORY_NOT_ENOUGH(2001, "inventory not enough"),
    INVENTORY_SERVICE_UNAVAILABLE(2002, "inventory service unavailable"),
    INVENTORY_NOT_FOUND(2003, "inventory not found"),
    INVENTORY_SERVICE_FAILED(2004, "inventory service failed"),
    NOTIFICATION_SERVICE_UNAVAILABLE(3002, "notification service unavailable"),
    ORDER_FAILED(4001, "order failed"),
    REPETITIVE_OPERATION(4002, "repetitive operation"),
    SECKILL_FAILED(4003, "seckill failed"),
    TOO_MANY_REQUESTS(5001, "too many request");

    private final int code;
    private final String msg;
    private ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

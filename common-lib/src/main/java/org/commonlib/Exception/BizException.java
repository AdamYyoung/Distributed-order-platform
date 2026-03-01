package org.commonlib.Exception;

import java.io.Serializable;

public class BizException extends RuntimeException{
    private final ErrorCode errorCode;
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg()); // send err msg
        this.errorCode = errorCode;
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

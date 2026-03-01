package org.commonlib.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.commonlib.Threadlocal.TraceIdHolder;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T>{
    private int code;
    private String message;
    private String traceId;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", TraceIdHolder.get(), data);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, TraceIdHolder.get(), null);
    }
}

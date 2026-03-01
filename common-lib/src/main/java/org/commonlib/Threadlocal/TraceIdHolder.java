package org.commonlib.Threadlocal;

import org.slf4j.MDC;

public class TraceIdHolder {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    public static final String MDC_KEY = "traceId";
    public static void set(String traceId){
        TRACE_ID.set(traceId);
        MDC.put(MDC_KEY, traceId);
    }
    public static String get(){
        return TRACE_ID.get();
    }
    public static void clear(){
        TRACE_ID.remove();
        MDC.remove(MDC_KEY);
    }
}

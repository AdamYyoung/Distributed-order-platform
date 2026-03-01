package org.commonlib.Filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.commonlib.Threadlocal.TraceIdHolder;

import java.io.IOException;


public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String traceId = request.getHeader("traceId");

        if (traceId != null) {
            TraceIdHolder.set(traceId); // one thread per module(JVM process)
        }

        try {
            chain.doFilter(req, res);
        } finally {
            TraceIdHolder.clear();
        }
    }
}
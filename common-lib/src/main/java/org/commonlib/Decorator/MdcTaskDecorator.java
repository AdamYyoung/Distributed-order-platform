package org.commonlib.Decorator;


import org.commonlib.Threadlocal.TraceIdHolder;
import org.commonlib.Threadlocal.UserContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        String traceId = TraceIdHolder.get();
        Long userId = UserContext.get();
        SecurityContext context = SecurityContextHolder.getContext();
        return () ->{
            try{
                if (traceId != null) TraceIdHolder.set(traceId); // next thread
                if (userId != null) UserContext.set(userId);
                if (context != null) SecurityContextHolder.setContext(context);

                runnable.run();
            } finally {
                TraceIdHolder.clear();
                UserContext.remove();
                SecurityContextHolder.clearContext();
            }
        };
    }
}

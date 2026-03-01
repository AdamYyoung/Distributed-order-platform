package org.commonlib.Config;

import org.commonlib.Filter.TraceIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

@Configuration
public class TraceAutoConfig {
    @Bean
    public FilterRegistrationBean<Filter> traceIdFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

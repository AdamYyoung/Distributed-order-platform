package org.commonlib.Config;

import lombok.extern.slf4j.Slf4j;
import org.commonlib.DTO.UserPrincipal;
import org.commonlib.Threadlocal.TraceIdHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

@Configuration
@ConditionalOnClass(RestTemplate.class)
@Slf4j
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add((request, body, execution)->{
            // 1. traceId
            String traceId = TraceIdHolder.get();
            if (traceId != null) {
                request.getHeaders().add("traceId", traceId);
            }

            // 2. jwt token(role)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
                request.getHeaders().set("Authorization", "Bearer " + auth.getCredentials());
                Object principal = auth.getPrincipal();
                if (principal instanceof UserPrincipal user) {
                    request.getHeaders().set("X-User-Id", String.valueOf(user.getId()));
                }
                request.getHeaders().set("X-User-Name", auth.getName());
                String roles = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));
                request.getHeaders().set("X-User-Roles", roles);
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}

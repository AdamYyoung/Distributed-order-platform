package org.inventoryservice.Config;

import org.commonlib.Config.CommonSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class InventorySecurityConfig extends CommonSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        applyCommonSettings(http);
        http
                .authorizeHttpRequests(auth -> auth
                        //.requestMatchers("/inventory/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/inventory/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}

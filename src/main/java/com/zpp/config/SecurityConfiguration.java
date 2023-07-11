package com.zpp.config;

import com.alibaba.fastjson.JSONObject;
import com.zpp.Handler.FailureHandler;
import com.zpp.Handler.MyAuthenticationEntryPoint;
import com.zpp.Handler.SuccessHandler;
import com.zpp.entity.RestBean;
import com.zpp.service.AuthorizeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final SuccessHandler successHandler;
    private final FailureHandler failureHandler;
    private final MyAuthenticationEntryPoint entryPoint;
    private final AuthorizeService authorizeService;
    private final DataSource dataSource;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorizeHttpRequests) ->
                        authorizeHttpRequests
                                .requestMatchers("/api/auth/**").permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling((exceptionHandling) ->
                exceptionHandling
                        .authenticationEntryPoint(entryPoint)
                )
                .rememberMe(rememberMe -> rememberMe
                                .rememberMeParameter("remember")
                                .tokenRepository(tokenRepository())
                                .tokenValiditySeconds(3600 * 24 * 7)
                        )
                .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
                .userDetailsService(authorizeService)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin((formLogin) ->
                        formLogin
//                                .usernameParameter("username")
//                                .passwordParameter("password")
//                                .loginPage("/authentication/login")
//                                .failureUrl("/authentication/login?failed")
                                .loginProcessingUrl("/api/auth/login")
                                .successHandler(successHandler)
                                .failureHandler(failureHandler)

                )
                .logout((logout) -> logout
//                                .deleteCookies("remove")
//                                .invalidateHttpSession(true)
                        .logoutUrl("/api/auth/logout")
//                                    .logoutSuccessUrl("/logout-success")
                        .logoutSuccessHandler(this::onAuthenticationSuccess)
                );

        return http.build();
    }
    //退出登录
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        if(request.getRequestURI().endsWith("/logout"))
            response.getWriter().write(JSONObject.toJSONString(RestBean.success("退出登录成功")));

    }

    @Bean
    public PersistentTokenRepository tokenRepository(){
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        jdbcTokenRepository.setCreateTableOnStartup(false);
        return jdbcTokenRepository;
    }

    //CORS配置源
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.addAllowedOriginPattern("*");
        cors.setAllowCredentials(true);
        cors.addAllowedHeader("*");
        cors.addAllowedMethod("*");
        cors.addExposedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
    //密码策略配置
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}

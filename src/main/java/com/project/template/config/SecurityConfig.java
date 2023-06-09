package com.project.template.config;

import com.project.template.service.impl.CustomUserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  // @Autowired
  // private CustomUserDetailsServiceImpl customUserDetailsService;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  TokenAuthenticationFilter tokenAuthenticationFilter() {
    return new TokenAuthenticationFilter();
  }

  //   @Override
  //   protected void configure(AuthenticationManagerBuilder auth) throws Exception {
  //     auth
  //       .userDetailsService(customUserDetailsService)
  //       .passwordEncoder(passwordEncoder());
  //   }

  @Bean
  AuthenticationManager authenticationManager(
    HttpSecurity http,
    PasswordEncoder passwordEncoder,
    CustomUserDetailsServiceImpl customUserDetailsService
  ) throws Exception {
    return http
      .getSharedObject(AuthenticationManagerBuilder.class)
      .userDetailsService(customUserDetailsService)
      .passwordEncoder(passwordEncoder)
      .and()
      .build();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
      .cors()
      .and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and()
      .csrf()
      .disable()
      .formLogin()
      .disable()
      .httpBasic()
      .disable()
      .exceptionHandling()
      .authenticationEntryPoint(new RestAuthenticationEntryPoint())
      .and()
      .authorizeHttpRequests()
      .requestMatchers(
        "/",
        // -- Swagger UI v2
        "/v2/api-docs",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        // -- Swagger UI v3 (OpenAPI)
        "/v3/api-docs/**",
        "/swagger-ui/**",
        // other public endpoints of your API may be appended to this array

        "/error",
        "/favicon.ico",
        "/**/*.png",
        "/**/*.gif",
        "/**/*.svg",
        "/**/*.jpg",
        "/**/*.html",
        "/**/*.css",
        "/**/*.js"
      )
      .permitAll()
      .and()
      .authorizeHttpRequests()
      .requestMatchers(
        HttpMethod.POST,
        "/api/v1/users/login",
        "/api/v1/users/refresh",
        "/api/v1/users"
      )
      .permitAll()
      .anyRequest()
      .authenticated()
      .and()
      .addFilterBefore(
        tokenAuthenticationFilter(),
        UsernamePasswordAuthenticationFilter.class
      )
      .build();
  }
  //   @Bean
  //   CorsFilter corsFilter() {
  //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  //     CorsConfiguration config = new CorsConfiguration();
  //     config.setAllowCredentials(true);
  //     config.addAllowedOriginPattern("*");
  //     config.addAllowedHeader("*");
  //     config.addAllowedMethod("*");
  //     source.registerCorsConfiguration("/**", config);
  //     return new CorsFilter(source);
  //   }
}

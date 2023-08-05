package com.ricemarch.config;

import com.ricemarch.service.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailServiceImpl userService;

    // AuthenticationManager 这个东西怎么进行 @Autowire注入呢？
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // 这上边这个方法泰拉跨了，你直接用super的authenticationManagerBean方法，有点low。
    // 我们高级着呢，我们控制super.authenticationManagerBean()方法中所使用的AuthenticationManagerBuilder delegateBuilder
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                return passwordEncoder.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                return passwordEncoder.matches(rawPassword, encodedPassword);
            }
        });
    }

    // 跨域问题 cors，csrf
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().authenticated()
                .and().httpBasic()
                .and().cors()
                .and().csrf().disable();
        // 为什么disable csrf
        // spring security，引入了csrf，默认是开启的。
        // csrf 和 rest（post）有冲突，所以基于目前我们大部分都是走的 rest的post访问，所以需要disable。
        // csrf默认支持：GET/HEAD/TRACE/OPTIONS, 不支持post
    }

    // 所有的访问都需要oauth2 验证码？ api-doc、swagger。 过滤点东西
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/swagger-*");
    }
}

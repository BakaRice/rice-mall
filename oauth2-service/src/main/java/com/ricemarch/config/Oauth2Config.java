package com.ricemarch.config;

import com.ricemarch.service.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;


@Configuration
@EnableAuthorizationServer
public class Oauth2Config extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserDetailServiceImpl userService;

    @Autowired
    private AuthenticationManager manager;

    // oauth2是为了生成 token令牌的，token 令牌需要存储到那里呢? 所以需要先解决存储问题
    @Bean
    public TokenStore tokenStore() {
        return new JdbcTokenStore(dataSource);
    }

    // token是不是得有过期时间啊？ 过期时间咋搞
    // oauth2的默认token的过期时间是 12个小时，如果想自定义它的过期时间，我们需要用到defaulttokenservice，并进行set
    @Bean
    @Primary
    public DefaultTokenServices defaultTokenServices() {
        DefaultTokenServices services = new DefaultTokenServices();
        services.setAccessTokenValiditySeconds(30 * 24 * 3600); // 30天过期
        services.setTokenStore(tokenStore());
        return services;
    }


    // 关心一下我们的 client details中的内容：client_id 和 client_secret.从哪里搞？
    // 对于client id 或者 secret 也好，如果小伙伴还有不理解的，很正常，别着急，我们先写代码，后续我会
    // 带大家进行实际的postman的调用，并且为大家演示 四种 token获取方式。到时候你们就知道，username+password和clientid+secret的
    // 区别和联系了
    @Bean
    public ClientDetailsService clientDetails() {
        return new JdbcClientDetailsService(dataSource);
    }

    // 我们可以通过ClientDetailsServiceConfig 将我们的 ClientDetailsService 设置到oauth中
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientDetails());
    }


    // 用户密码和 client-secret 是不是不能存储明文 啊？ 你是不是得搞一个加密方式
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 若果企业要求自己的加密算法，可以通过这种形式进行encode以及校验是否相符
//        return new PasswordEncoder() {
//            @Override
//            public String encode(CharSequence rawPassword) {
//                return null;
//            }
//
//            @Override
//            public boolean matches(CharSequence rawPassword, String encodedPassword) {
//                return false;
//            }
//        }
        return new BCryptPasswordEncoder();
    }

    // 添加自定义的安全配置。你可以选择不添加
    // 往往我们会将这个配置用于 ： 放开一些接口的查询权限.比如说 checkToken 接口
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients() // 可以进行表单验证
                .checkTokenAccess("permitAll()"); // checkToken
    }

    // 至少这个接口，我得把 userdetailsservice 安排了
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.userDetailsService(userService);
        endpoints.tokenServices(defaultTokenServices());
        endpoints.authenticationManager(this.manager);
//        endpoints.tokenStore(tokenStore()); //重复项
    }
}

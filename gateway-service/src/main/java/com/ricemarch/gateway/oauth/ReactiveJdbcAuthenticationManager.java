// package com.ricemarch.gateway.oauth;
//
// import org.springframework.security.authentication.ReactiveAuthenticationManager;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.oauth2.common.OAuth2AccessToken;
// import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
// import org.springframework.security.oauth2.provider.OAuth2Authentication;
// import org.springframework.security.oauth2.provider.token.TokenStore;
// import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
// import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
// import reactor.core.publisher.Mono;
//
// import javax.sql.DataSource;
//
// public class ReactiveJdbcAuthenticationManager implements ReactiveAuthenticationManager {
//
//     private TokenStore tokenStore;
//
//     public ReactiveJdbcAuthenticationManager(DataSource dataSource) {
//         this.tokenStore = new JdbcTokenStore(dataSource);
//     }
//
//     @Override
//     public Mono<Authentication> authenticate(Authentication authentication) {
//
//
//         return Mono.justOrEmpty(authentication)
//                 .filter(a -> a instanceof BearerTokenAuthenticationToken)
//                 .cast(BearerTokenAuthenticationToken.class)
//                 .map(BearerTokenAuthenticationToken::getToken)
//                 .flatMap((accessToken -> {
//                     OAuth2AccessToken oAuth2AccessToken = this.tokenStore.readAccessToken(accessToken);
//                     if (oAuth2AccessToken == null) {
//                         return Mono.error(new InvalidTokenException("InvalidTokenException!"));
//                     } else if (oAuth2AccessToken.isExpired()) { // token 是否过期
//                         return Mono.error(new InvalidTokenException("InvalidTokenException!, isExpired!"));
//                     }
//                     // 问： token存在于db，没有过期，难道就一定是oauth2的token吗？ 我们仅仅是查询出来了，然后
//                     // 判断了一个过期时间
//                     // 如果有一个黑客，在你的db里插入了一个 token 是abc，过期时间是永久，然后带着这个abc来访问你的gateway，如果仅仅
//                     // 靠以上两个验证，不太严谨。
//                     OAuth2Authentication oAuth2Authentication = this.tokenStore.readAuthentication(accessToken);
//                     if (oAuth2Authentication == null) {
//                         return Mono.error(new InvalidTokenException("Fake token!"));
//                     }
//                     return Mono.justOrEmpty(oAuth2Authentication);
//                 })).cast(Authentication.class);
//
//     }
// }

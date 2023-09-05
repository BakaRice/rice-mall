package com.ricemarch.gateway.filter;

import com.ricemarch.gateway.feignclient.Oauth2ServiceClient;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AuthFilter implements GlobalFilter,Ordered {

    //你不要天真的以为，这东西真能用，这么搞用不了
    @Autowired
    @Lazy
    private Oauth2ServiceClient oauth2ServiceClient;

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();
        //你自己选，校长之前给你们演示过 正则的；现在这块儿演示建议的，你想用正则，我不拦你
        if(path.contains("/oauth")
                || path.contains("/user/register")) {
            return chain.filter(exchange);
        }

        //我也用 Authorization，但是我不像oauth2那么矫情，还加一个 bearer ，我这个不加了，直接要token
        String token = request.getHeaders().getFirst("Authorization");
//        Map<String, Object> result = oauth2ServiceClient.checkToken(token); //这是同步的调用，rest发送请求，等待结果回执
        //改为异步形式，看看效果
        CompletableFuture<Map> future = CompletableFuture.supplyAsync(() -> {
            return oauth2ServiceClient.checkToken(token);
        });
        Map<String, Object> result = future.get();
        boolean active = (boolean) result.get("active");
        if(!active) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //比如说我们可以给微服务转发请求的时候带上一些header
        ServerHttpRequest httpRequest = request.mutate().headers(httpHeaders -> {
            httpHeaders.set("personId", request.getHeaders().getFirst("personId"));
//            httpHeaders.set("tracingId", "");
        }).build();
        exchange.mutate().request(httpRequest);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}


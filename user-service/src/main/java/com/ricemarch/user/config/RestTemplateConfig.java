package com.ricemarch.user.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    // restTemplate底层使用的jdk的url connection，没有什么过期时间的设置，对于一些复杂场景
    // 不太适用，所以我们打算替换掉底层 的url connection，转而使用 apache的 httpclient
    // 并且在 http client中，配置我们的 链接超时，线程池大小，keep-live配置，定时清理空闲线程的任务等等。
    // 整个resttempalte的配置是可控的且多元化，而且比较稳定

    // 你通过 autowired 注入进来就算是替换了？ 玩儿呢
    @Autowired
    private CloseableHttpClient httpClient;

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
                = new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setHttpClient(httpClient);
        return httpComponentsClientHttpRequestFactory;
    }

    @Bean(name = "restTemplate")
    @LoadBalanced
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        // 我们一般接收的 uat-8 形式的消息
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        Iterator<HttpMessageConverter<?>> iterator = messageConverters.iterator();
        while (iterator.hasNext()) {
            HttpMessageConverter<?> converter = iterator.next();
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
            }
        }
        return restTemplate;
    }

    // @Bean(name = "outerRestTemplate")
    // public RestTemplate getOuterRestTemplate() {
    //     RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
    //     // 我们一般接收的 uat-8 形式的消息
    //     List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
    //     Iterator<HttpMessageConverter<?>> iterator = messageConverters.iterator();
    //     while (iterator.hasNext()) {
    //         HttpMessageConverter<?> converter = iterator.next();
    //         if (converter instanceof StringHttpMessageConverter) {
    //             ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
    //         }
    //     }
    //     return restTemplate;
    // }
    //
    // @Bean(name = "innerRestTemplate")
    // @LoadBalanced
    // public RestTemplate getRestTemplate() {
    //     RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
    //     // 我们一般接收的 uat-8 形式的消息
    //     List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
    //     Iterator<HttpMessageConverter<?>> iterator = messageConverters.iterator();
    //     while (iterator.hasNext()) {
    //         HttpMessageConverter<?> converter = iterator.next();
    //         if (converter instanceof StringHttpMessageConverter) {
    //             ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
    //         }
    //     }
    //     return restTemplate;
    // }
}

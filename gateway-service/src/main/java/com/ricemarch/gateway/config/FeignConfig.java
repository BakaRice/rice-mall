package com.ricemarch.gateway.config;

import feign.codec.Decoder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class FeignConfig {

    @Bean
    public Decoder feignDecoder() {
        // 我们需要在这里边完成message的converter
        ObjectFactory<HttpMessageConverters> objectFactory = new ObjectFactory() {
            @Override
            public HttpMessageConverters getObject() throws BeansException {
                MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
                        = new MappingJackson2HttpMessageConverter();
                List<MediaType> mediaTypes = new ArrayList<>();
                mediaTypes.add(MediaType.valueOf(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"));
                mappingJackson2HttpMessageConverter.setSupportedMediaTypes(mediaTypes);
                final HttpMessageConverters httpMessageConverters
                        = new HttpMessageConverters(mappingJackson2HttpMessageConverter);
                return httpMessageConverters;
            }
        };

        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }
}

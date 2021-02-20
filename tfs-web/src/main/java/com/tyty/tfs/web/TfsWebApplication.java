package com.tyty.tfs.web;

import com.tyty.tfs.dao.DaoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Import(DaoConfiguration.class)
@EnableTransactionManagement
@SpringBootApplication
public class TfsWebApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TfsWebApplication.class, args);
        context.registerShutdownHook();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 取消登录拦截
        /*registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns("/error", "/favicon.ico", "/user/login", "/login.html", "/css/**", "/imgs/**", "/js/**", "/vendor/**");*/

    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                List<MediaType> mediaTypeList = new ArrayList<>(converter.getSupportedMediaTypes());
                mediaTypeList.add(MediaType.TEXT_HTML);
                ((MappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(mediaTypeList);
            }
        }
        return restTemplate;
    }
}

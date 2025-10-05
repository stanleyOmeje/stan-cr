package com.stan.stancore.extended.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@EnableConfigurationProperties(VendingCoreProperties.class)
@ComponentScan(basePackages = { "com.systemspecs", "com.systemspecs.remita.sdk", "com.systemspecs.remita.dto.account" })
@Configuration
public class ExtendedAppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    //   @Bean
    //    public CoreSDKAuth coreSDKAuth()
    //    {
    //        return new CoreSDKAuth(null,null);
    //    }
    //
    //    @Bean
    //    public CoreSdkAccount coreSdkAccount()
    //    {
    //        return new CoreSdkAccount(httpHandlerUtil(),null);
    //    }
    //
    //    @Bean
    //    public HttpHandlerUtil httpHandlerUtil()
    //    {
    //        return new HttpHandlerUtil(restTemplate());
    //    }

    //    @Bean
    //    public CoreSdkHttpUtils coreSdkHttpUtils()
    //    {
    //        return new CoreSdkHttpUtils();
    //    }
}

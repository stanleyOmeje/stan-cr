package com.stan.stancore.extended.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemspecs.remita.annotation.EnableActivityLog;
import com.systemspecs.remita.reciever.impl.DefaultActivityLogReceiver;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableActivityLog
public class ActivityLoggerConfiguration {

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = ObjectMapper();

    @Getter
    private final VendingCoreProperties properties;

    public ActivityLoggerConfiguration(VendingCoreProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DefaultActivityLogReceiver activityLogReceiverInfo() {
        return new DefaultActivityLogReceiver(restTemplate, properties.getActivityServiceUrl(), objectMapper);
    }

    public ObjectMapper ObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}

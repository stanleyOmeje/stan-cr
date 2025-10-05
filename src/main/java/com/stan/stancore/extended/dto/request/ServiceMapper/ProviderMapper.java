package com.stan.stancore.extended.dto.request.ServiceMapper;

import com.systemspecs.remita.vending.extended.dto.request.DisplayProvider;
import com.systemspecs.remita.vending.vendingcommon.entity.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProviderMapper {

    public DisplayProvider mapToDisplayProvider(Provider provider) {
        log.info(">>> Mapping Provider to DisplayProvider");
        DisplayProvider displayProviderRequest = new DisplayProvider();
        displayProviderRequest.setName(provider.getName());
        displayProviderRequest.setDescription(provider.getDescription());
        displayProviderRequest.setCode(provider.getCode().replaceAll("\\s+", "-"));
        displayProviderRequest.setLogoUrl(provider.getLogoUrl());
        displayProviderRequest.setCategory(provider.getCategory());
        return displayProviderRequest;
    }
}

package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.service.ExtendedSmsTemplateService;
import com.systemspecs.remita.vending.vendingcommon.entity.SmsTemplate;
import com.systemspecs.remita.vending.vendingcommon.repository.ExtendedSmsTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExtendedSmsTemplateServiceImpl implements ExtendedSmsTemplateService {

    @Autowired
    ExtendedSmsTemplateRepository smsTemplateRepository;

    @Override
    public SmsTemplate findSmsTemplateByTemplateName(String templateName) {
        Optional<SmsTemplate> smsTemplate = smsTemplateRepository.findSmsTemplateByTemplateName(templateName);
        if (smsTemplate.isEmpty()) {
            return null;
        }
        return smsTemplate.get();
    }
}

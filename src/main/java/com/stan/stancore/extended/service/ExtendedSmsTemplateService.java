package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.vendingcommon.entity.SmsTemplate;

public interface ExtendedSmsTemplateService {
    SmsTemplate findSmsTemplateByTemplateName(String templateName);
}

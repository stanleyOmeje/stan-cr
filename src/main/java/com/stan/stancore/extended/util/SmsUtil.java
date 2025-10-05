package com.stan.stancore.extended.util;

import com.systemspecs.remita.extended.sms.SmsNotificationMessage;
import com.systemspecs.remita.extended.sms.SmsNotificationUtil;
import com.systemspecs.remita.extended.sms.SmsSendException;
import com.systemspecs.remita.vending.extended.dto.sms.Sms;
import com.systemspecs.remita.vending.extended.enums.SmsTemplateName;
import com.systemspecs.remita.vending.extended.service.impl.ExtendedSmsTemplateServiceImpl;
import com.systemspecs.remita.vending.vendingcommon.entity.SmsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsUtil {

    @Autowired
    ExtendedSmsTemplateServiceImpl smsTemplateService;

    @Autowired
    private SmsNotificationUtil smsNotificationUtil;

    @Async("taskExecutor")
    public void sendTokenSms(Sms smsRequest) {
        log.info("Inside sendOtpSms method of SmsUtil");
        SmsNotificationMessage smsNotificationMessage = new SmsNotificationMessage();
        SmsTemplate smsTemplate = getSmsTemplate(SmsTemplateName.TOKEN.name());
        if (smsTemplate == null) {
            log.error("Could not get {} sms from the database", SmsTemplateName.TOKEN.name());
            return;
        }
        if (!smsTemplate.getSmsNotificationStatus()) {
            log.info("This sms has been disabled and therefore can't be sent: {} ......", SmsTemplateName.TOKEN.name());
        }

        smsNotificationMessage.setMobileNumber(smsRequest.getMobileNumber());
        String message = String.format(smsTemplate.getTemplateMessage(), smsRequest.getToken());
        log.info(message);
        smsNotificationMessage.setSmsMessage(message);
        boolean isSmsSent = false;

        try {
            log.info("Sending Sms to {} for token", smsRequest.getMobileNumber());
            isSmsSent = smsNotificationUtil.sendSmsAsync(smsNotificationMessage);
        } catch (SmsSendException e) {
            log.error("An Error Occurred while sending email: {}", e.getMessage());
            e.printStackTrace();
        }

        log.info("the status for sms sent, {}", isSmsSent);
        if (isSmsSent) log.info("Sms Sent Successfully");
    }

    private SmsTemplate getSmsTemplate(String templateName) {
        return smsTemplateService.findSmsTemplateByTemplateName(templateName);
    }
}

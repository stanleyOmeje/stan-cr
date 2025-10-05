package com.stan.stancore.extended.service;


import com.systemspecs.remita.extended.email.EmailNotificationUtil;
import com.systemspecs.remita.extended.email.NotificationMessage;
import com.systemspecs.remita.extended.email.exception.EmailSendException;
import com.systemspecs.remita.extended.utils.RedisUtility;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.service.impl.BalanceNotificationPublisher;
import com.systemspecs.remita.vending.extended.util.MailMessageRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.EmailDto;
import com.systemspecs.remita.vending.vendingcommon.entity.EmailTemplate;
import com.systemspecs.remita.vending.vendingcommon.enums.EmailTemplateName;
import com.systemspecs.remita.vending.vendingcommon.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Component
public class BalanceEmailNotificationService {

    private final BalanceNotificationPublisher notificationPublisher;
    private final EmailNotificationUtil emailNotificationUtil;
    private final VendingCoreProperties properties;
    private final RedisUtility redisUtility;
    private final EmailTemplateRepository emailTemplateRepository;

    @Async("taskExecutor")
    public void sendLowBalanceAlert(EmailDto emailRequest) {
        NotificationMessage notificationMessage = new NotificationMessage();
        EmailTemplate emailTemplate = getEmailTemplate(EmailTemplateName.LOW_BALANCE_ALERT_NOTIFICATION.name());

        if (emailTemplate == null) {
            log.info("Template not found for {}", EmailTemplateName.LOW_BALANCE_ALERT_NOTIFICATION.name());
            return;
        }

        notificationMessage.setSubject(emailTemplate.getSubject());
        notificationMessage.setFromEmail(properties.getFromEmail());
        notificationMessage.setToEmail(emailRequest.getToEmail());
        notificationMessage.setTemplateId(emailTemplate.getTemplateId());

        Map<String, Object> message = new HashMap<>();
        message.put("processorName", emailRequest.getProcessorName());
        message.put("accountBalance", emailRequest.getAccountBalance());
        message.put("minimumBalance", emailRequest.getMinimumBalance());
        message.put("year", String.valueOf(LocalDateTime.now().getYear()));
        notificationMessage.setMessage(message);

        boolean isEmailSent = false;
        try {
            log.debug("Sending Low Balance Alert Email");
            if (properties.isSendEmails()) {
                if (properties.isSendEmailsWithKafka()) {
                    MailMessageRequest msgRequest = buildKafkaMessage(notificationMessage);
                    notificationPublisher.publishEmailEvent(msgRequest);
                } else {
                    isEmailSent = emailNotificationUtil.sendMailAsync(notificationMessage);
                }
            } else {
                log.debug("Email Send Mode OFF");
            }
        } catch (EmailSendException e) {
            log.error("An error occurred while sending email..", e);
        }

        if (isEmailSent) log.debug("Low Balance Alert Email Sent Successfully");
    }

    private EmailTemplate getEmailTemplate(String templateName) {
        String cacheKey = "email_template::" + templateName;

        EmailTemplate emailTemplate = redisUtility.getObjectFromRedis(cacheKey, EmailTemplate.class);
        if (Objects.nonNull(emailTemplate)) {
            log.info("Email template [{}] fetched from Redis cache", templateName);
            return emailTemplate;
        }

        Optional<EmailTemplate> templateFromDb = emailTemplateRepository.findByTemplateName(templateName);
        if (templateFromDb.isPresent()) {
            emailTemplate = templateFromDb.get();
            log.info("Email template [{}] fetched from DB", templateName);

            try {
                redisUtility.saveObjectToRedis(cacheKey, emailTemplate, 300);
                log.info("Email template [{}] cached in Redis for {} seconds",
                    templateName, 300);
            } catch (Exception e) {
                log.warn("Failed to cache email template [{}] in Redis", templateName, e);
            }

            return emailTemplate;
        }
        log.warn("Email template [{}] not found in DB or cache", templateName);
        return null;
    }

    private MailMessageRequest buildKafkaMessage(NotificationMessage notificationMessage) {
        log.debug("Building MailMessageRequest with notification message {}", notificationMessage);
        Map<String, Object> mapMessage = this.generateMapNotificationData(notificationMessage);
       MailMessageRequest msgRequest = new MailMessageRequest();
        msgRequest.setMessageMap(mapMessage);
        String requestId = (String) StringUtils.defaultIfBlank(notificationMessage.getRequestId(), "" + System.nanoTime());
        msgRequest.setRequestId(requestId);
        return msgRequest;
    }

    public Map<String, Object> generateMapNotificationData(NotificationMessage nofiMsg) {
        Map<String, Object> map = new HashMap<>();

        map.put("productName", Optional.ofNullable(nofiMsg.getProductName()).orElse("Vending"));
        map.put("toEmail", nofiMsg.getToEmail());

        if (StringUtils.isNotBlank(nofiMsg.getFromEmail())) {
            map.put("from", nofiMsg.getFromEmail());
        }

        map.put("subject", nofiMsg.getSubject());
        map.put("vmTemplate", StringUtils.defaultIfBlank(nofiMsg.getTemplateId(), "low_balance_email_template"));
        map.put("mailCC", nofiMsg.getCcEmails());

        if (nofiMsg.getMessage() != null) {
            map.put("processorName", nofiMsg.getMessage().get("processorName"));
            map.put("accountBalance", nofiMsg.getMessage().get("accountBalance"));
            map.put("minimumBalance", nofiMsg.getMessage().get("minimumBalance"));
            map.put("year", nofiMsg.getMessage().get("year"));
        }
        return map;
    }


}

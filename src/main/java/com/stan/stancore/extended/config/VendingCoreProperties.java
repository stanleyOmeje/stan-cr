package com.stan.stancore.extended.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "core")
public class VendingCoreProperties {

    private boolean enableTransactionUpdateCron = true;

    private String sessionTokenUrl;
    private String criteriaTypeUrl;
    private String searchCustomerUrl;
    private String calculatePaymentUrl;
    private String makePaymentUrl;
    private String retrieveDetailedPaymentUrl;
    private String shiftEnquiryUrl;
    private String customerEnquiryUrl;
    private String vendorTransactionsUrl;
    private String vendorInformationUrl;
    private String newUserUrl;
    private String modifyUserUrl;
    private String validatePasswordUrl;
    private String changePasswordUrl;
    private String forgotPasswordUrl;
    private String searchUserUrl;
    private String vendorTopupsUrl;
    private String vendorStatementUrl;
    private String idVendor;
    private String codUser;
    private String codType;
    private int channel;
    private BigDecimal totalPayment;
    private String authorization;

    @JsonProperty("grant_type")
    private String grantType;

    private String username;
    private String password;
    private boolean useSecretKey;
    private String allowedIP;
    private boolean enablePostpaidCron = false;
    private String cronConfiguredTime;
    private String activityServiceUrl;
    private String walletHistoryUrl;
    private String merchantId;
    private String token;

    private boolean enabledBulkVendCron = false;
    private int pageSize;
    private boolean enabledBulkRevendCron = false;

    private int maxPollRecords = 500;
    private String offSetConfig = "latest";
    private int maxPollInterval = 60000;
    private int concurrency = 4;
    private String callBackUrl;

    private boolean enableFundRecoupCron = false;
    private boolean enableEkedcKeepAlive = false;
    private String clientIp;
    private String remitaXKey;
    private String fromEmail;
    private boolean sendEmails;
    private boolean sendEmailsWithKafka;
    private List<String> emails;
}

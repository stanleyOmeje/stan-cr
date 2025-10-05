package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.service.BalanceEmailNotificationService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.EmailDto;
import com.systemspecs.remita.vending.vendingcommon.dto.response.GetBalanceResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.ProcessorBalance;
import com.systemspecs.remita.vending.vendingcommon.repository.ProcessorBalanceRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessorBalanceUpdater {
    private final ProcessorBalanceRepository repository;
    private final ApplicationContext applicationContext;
    private final BalanceEmailNotificationService emailNotificationService;
    private final VendingCoreProperties vendingCoreProperties;


    @Transactional
    public void updateAllBalances() {
        List<String> processorIds = repository.findAllProcessorIds();

        for (String processorId : processorIds) {
            try {
                String beanName = processorId.toLowerCase() + "ProcessorService";
                AbstractVendingService service =
                    (AbstractVendingService) applicationContext.getBean(beanName);

                GetBalanceResponse balanceResponse = service.getBalance();

                BigDecimal balance = (balanceResponse == null || balanceResponse.getAmount() == null)
                    ? BigDecimal.ZERO
                    : balanceResponse.getAmount();

                log.info("Fetched balance for {} = {}", processorId, balance);

                ProcessorBalance pb = upsertBalance(processorId, balance);

                if (pb.getMinimumBalance() != null && pb.getMinimumBalance().compareTo(balance) > 0) {
                    EmailDto mail = new EmailDto();
                    // Join all support emails into a single comma-separated string, to send a single copy to all
                    mail.setToEmail(String.join(",", vendingCoreProperties.getEmails()));
                    mail.setProcessorName(processorId);
                    mail.setAccountBalance(balance);
                    mail.setMinimumBalance(pb.getMinimumBalance());

                    emailNotificationService.sendLowBalanceAlert(mail);
                    log.info("📧 Low balance alert sent to {}", mail.getToEmail());
                }


            } catch (Exception e) {
                log.error("Error fetching balance for processor {}", processorId, e);
            }
        }
    }

    private ProcessorBalance upsertBalance(String processorId, BigDecimal balance) {
        ProcessorBalance pb = repository.findByProcessorId(processorId)
                .orElseGet(() -> {
                    ProcessorBalance np = new ProcessorBalance();
                    np.setProcessorId(processorId);
                    return np;
                });

        pb.setAccountBalance(balance);
        return repository.save(pb);
    }

}

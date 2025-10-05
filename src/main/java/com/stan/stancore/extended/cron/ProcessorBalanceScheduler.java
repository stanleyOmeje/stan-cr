package com.stan.stancore.extended.cron;

import com.systemspecs.remita.vending.extended.service.impl.ProcessorBalanceUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessorBalanceScheduler {
    private final ProcessorBalanceUpdater updater;

    @Scheduled(cron = "${processor.balance.cron:0 0/30 * * * ?}")
    public void runCron() {
        log.info("Running scheduled processor balance sync...");
        updater.updateAllBalances();
    }
}

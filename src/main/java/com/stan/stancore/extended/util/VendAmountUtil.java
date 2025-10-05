package com.stan.stancore.extended.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class VendAmountUtil {

    public static boolean validMinAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.valueOf(1000)) >= 0;
    }
}

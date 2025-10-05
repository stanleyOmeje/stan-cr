package com.stan.stancore.extended.util;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.vendingcommon.entity.CustomCommission;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.repository.CustomCommissionRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Data
@Component
public class CommissionUtil {
    private final CustomCommissionRepository customCommissionRepository;
    private final ProductRepository productRepository;


    public CommissionDTO resolveAmountWithCommission(String productCode, BigDecimal amount, MerchantDetailsDto merchantDetailsDto, String Processor) {
        CommissionDTO commissionDTO = new CommissionDTO();
        //check if merchant, product and processor are configured
        Optional<CustomCommission> optionalCustomCommissionWithProcessor = customCommissionRepository.findFirstByMerchantIdAndProductCodeAndProcessor(
            merchantDetailsDto.getOrgId(),
            productCode,
            Processor
        );
        if (optionalCustomCommissionWithProcessor.isPresent()) {
            CustomCommission commission = optionalCustomCommissionWithProcessor.get();
            if (Boolean.TRUE.equals(commission.getIsAppliedCommission())) {
                if (Boolean.TRUE.equals(commission.getIsFixedCommission())) {
                    commissionDTO.setCommission(commission.getFixedCommission());
                    commissionDTO.setDiscountedAmount(amount.subtract(commission.getFixedCommission()));
                    return commissionDTO;
                } else {
                    return calculateAmountForPercentageCommission(
                        amount,
                        commission.getPercentageCommission(),
                        commission.getPercentageMin(),
                        commission.getPercentageMax()
                    );
                }
            }
        }
        //check if merchant, product are configured
        Optional<CustomCommission> optionalCustomCommission = customCommissionRepository.findFirstByMerchantIdAndProductCodeAndProcessorIsNull(
            merchantDetailsDto.getOrgId(),
            productCode
        );
        if (optionalCustomCommission.isPresent()) {
            CustomCommission commission = optionalCustomCommission.get();
            if (Boolean.TRUE.equals(commission.getIsAppliedCommission())) {
                if (Boolean.TRUE.equals(commission.getIsFixedCommission())) {
                    commissionDTO.setCommission(commission.getFixedCommission());
                    commissionDTO.setDiscountedAmount(amount.subtract(commission.getFixedCommission()));
                    return commissionDTO;
                } else {
                    return calculateAmountForPercentageCommission(
                        amount,
                        commission.getPercentageCommission(),
                        commission.getPercentageMin(),
                        commission.getPercentageMax()
                    );
                }
            }
        }
        Product product = getProduct(productCode);
        if (Boolean.TRUE.equals(product.getApplyCommission())) {
            if (Boolean.TRUE.equals(product.getIsFixedCommission())) {
                commissionDTO.setCommission(product.getFixedCommission());
                commissionDTO.setDiscountedAmount(amount.subtract(product.getFixedCommission()));
                return commissionDTO;
            } else {
                return calculateAmountForPercentageCommission(
                    amount,
                    product.getPercentageCommission(),
                    product.getPercentageMinCap(),
                    product.getPercentageMaxCap()
                );
            }
        }
        commissionDTO.setCommission(new BigDecimal("0"));
        commissionDTO.setDiscountedAmount(amount);
        return commissionDTO;
    }

    public CommissionDTO calculateAmountForPercentageCommission(
        BigDecimal originalAmount,
        String percentageCommission,
        BigDecimal minAmountCap,
        BigDecimal maxAmountCap
    ) {
        CommissionDTO commissionDTO = new CommissionDTO();
        BigDecimal percentageComm = new BigDecimal(percentageCommission);

        BigDecimal commissionAmount =
            (originalAmount.multiply(percentageComm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalCommissionAmount = commissionAmount;
        if (minAmountCap != null) {
            if (commissionAmount.compareTo(minAmountCap) < 0 || commissionAmount.compareTo(minAmountCap) == 0) {
                finalCommissionAmount = minAmountCap;
            }
        }
        if (maxAmountCap != null) {
            if (commissionAmount.compareTo(maxAmountCap) > 0 || commissionAmount.compareTo(maxAmountCap) == 0) {
                finalCommissionAmount = maxAmountCap;
            }
        }
        BigDecimal discountedAmount = originalAmount.subtract(finalCommissionAmount).setScale(2, RoundingMode.HALF_UP);

        commissionDTO.setDiscountedAmount(discountedAmount);
        commissionDTO.setCommission(finalCommissionAmount);
        return commissionDTO;
    }

    public Product getProduct(String code) {
        log.info(">>> Getting Product from code:{}", code);
        return productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));
    }
}

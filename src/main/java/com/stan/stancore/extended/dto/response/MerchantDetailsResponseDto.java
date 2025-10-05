package com.stan.stancore.extended.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDetailsResponseDto {
    private String status;
    private String message;
    private MerchantAuthData data;
}

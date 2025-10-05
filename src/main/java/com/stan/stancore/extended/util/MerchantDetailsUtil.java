package com.stan.stancore.extended.util;

import com.google.gson.Gson;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.MerchantDetailsResponseDto;
import com.systemspecs.remita.vending.vendingcommon.util.RestTemplateUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantDetailsUtil {

    private final RestTemplateUtility restTemplateUtility;
    private final Gson gson;

    public Optional<MerchantDetailsDto> getMerchantDetails(HttpServletRequest request) {
        try {
            String details = request.getHeader("merchant");
            if (details == null || details.isEmpty()) {
                log.warn("Merchant header missing in request");
                return Optional.empty();
            }

            String secretKey = request.getHeader("secretKey");
            String clientIp = (String) request.getAttribute("clientIp");

            byte[] decode = Base64.getDecoder().decode(details);
            details = new String(decode, StandardCharsets.UTF_8);
            log.info("Decoded merchant details: {}", details);

            Gson gson = new Gson();
            MerchantDetailsDto merchantDetailsDto = gson.fromJson(details, MerchantDetailsDto.class);

            merchantDetailsDto.setApiKey(secretKey);
            merchantDetailsDto.setRequestIp(clientIp);

            log.info("Merchant Details extracted from request: {}", merchantDetailsDto);
            return Optional.of(merchantDetailsDto);
        } catch (Exception e) {
            log.error("Error occurred extracting integrator details from request", e);
        }
        return Optional.empty();
    }


    public MerchantDetailsResponseDto authenticateMerchant(String clientIp, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("remita-x-key", bearerToken);
            headers.set("remita-client-ip", clientIp);
            headers.setBearerAuth(bearerToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            String url = "https://api-gateway-qa.systemspecsng.com/services/merchant-service/api/v1/auth/auth-merchant/secret/VENDING";

            log.info(">>> Calling merchant auth service at {}", url);

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplate()
                .exchange(url, HttpMethod.POST, requestEntity, String.class);

            log.info("Merchant Auth raw response => {}", responseEntity);

            MerchantDetailsResponseDto response = gson.fromJson(responseEntity.getBody(), MerchantDetailsResponseDto.class);
            log.info("Merchant Auth mapped response => {}", new Gson().toJson(response));

            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[-] Merchant Auth API error: {}", e.getResponseBodyAsString(), e);
            return new MerchantDetailsResponseDto("12", "Merchant auth failed: " + e.getMessage(), null);
        } catch (ResourceAccessException e) {
            log.error("[-] Merchant Auth API timeout or unreachable: {}", e.getMessage(), e);
            return new MerchantDetailsResponseDto("98", "Merchant auth service unavailable", null);
        } catch (Exception e) {
            log.error("[-] Unexpected error calling Merchant Auth API", e);
            return new MerchantDetailsResponseDto("99", "Unexpected error in merchant auth", null);
        }
    }

}

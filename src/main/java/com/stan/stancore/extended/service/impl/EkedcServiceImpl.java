package com.stan.stancore.extended.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.systemspecs.remita.vending.ekedcmodule.dto.response.SessionTokenResponse;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.ekedcdto.request.*;
import com.systemspecs.remita.vending.extended.dto.ekedcdto.response.*;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.EkedcService;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.util.RestTemplateUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EkedcServiceImpl implements EkedcService {

    private final VendingCoreProperties properties;
    private final RestTemplateUtility restTemplateUtility;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    ObjectMapper objectMapper = new ObjectMapper();
    Gson gson = new Gson();

    @Override
    public ResponseEntity<DefaultResponse> createUser(CreateUserRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getNewUserUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if ("201".equals(String.valueOf(responseEntity.getStatusCode().value()))) {
                // response = responseEntity.getBody();
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> modifyUser(ModifyUserRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getModifyUserUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if ("200".equals(String.valueOf(responseEntity.getStatusCode().value()))) {
                // response = responseEntity.getBody();
                response.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> searchUser(SearchUserRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getSearchUserUrl();

            ResponseEntity<SearchUserResponse[]> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, SearchUserResponse[].class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if (responseEntity != null && responseEntity.getBody() != null) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> validatePassword(ValidatePasswordRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getValidatePasswordUrl();

            // ResponseEntity<ValidatePasswordResponse> responseEntity = restTemplateUtility.getRestTemplateIgnoreHostName().exchange(validateUrl, HttpMethod.POST, httpEntity, ValidatePasswordResponse.class);
            ResponseEntity<String> responseEntityS = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntityS);
            ValidatePasswordResponse responseEntity = new ObjectMapper()
                .readValue(responseEntityS.getBody(), ValidatePasswordResponse.class);

            if ("200".equals(String.valueOf(responseEntityS.getStatusCode().value()))) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntityS.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntityS.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity);
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> changePassword(ChangePasswordRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getChangePasswordUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if ("200".equals(String.valueOf(responseEntity.getStatusCode().value()))) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> forgotPassword(ForgotPasswordRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to forgotPassword is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getChangePasswordUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if ("200".equals(String.valueOf(responseEntity.getStatusCode().value()))) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("New password sent to your email");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchCriteriaType(CriteriaTypeRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getCriteriaTypeUrl();

            ResponseEntity<CriteriaType[]> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, CriteriaType[].class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if (responseEntity != null && responseEntity.getBody() != null) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> customerEnquiry(CustomerEnquiryRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getCustomerEnquiryUrl();

            ResponseEntity<CustomerEnquiryResponse[]> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, CustomerEnquiryResponse[].class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if (responseEntity != null && responseEntity.getBody() != null) {
                // response = responseEntity.getBody();
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> shiftEnquiry(ShiftEnquiryRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getShiftEnquiryUrl();

            ResponseEntity<ShiftEnquiryResponse[]> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, ShiftEnquiryResponse[].class);
            log.info("Raw response from provider is ...{}", responseEntity);
            String shiftEnquiryCode = String.valueOf(responseEntity.getStatusCodeValue());
            if ("200".equals(shiftEnquiryCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            if ("204".equals(shiftEnquiryCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("No records associated to user");
                response.setData(responseEntity.getBody());
            }
            if ("403".equals(shiftEnquiryCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("User does not have permissions ");
                response.setData(responseEntity.getBody());
            }

            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> vendorInfo(VendorInformationRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getVendorInformationUrl();

            ResponseEntity<VendorInfoResponse> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, VendorInfoResponse.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if (responseEntity != null && responseEntity.getBody() != null) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> vendorTransaction(VendorTransactionRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to getVendorInformation is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getVendorTransactionsUrl();

            ResponseEntity<VendorTransactionResponse> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, VendorTransactionResponse.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            if (responseEntity != null && responseEntity.getBody() != null) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<DefaultResponse> calculatePayment(CalculatePaymentRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        // CalculatePaymentResponse response = null;
        try {
            log.info("Request payload to calculatePayment is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String validateUrl = properties.getCalculatePaymentUrl();

            ResponseEntity<CalculatePaymentResponse> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(validateUrl, HttpMethod.POST, httpEntity, CalculatePaymentResponse.class);
            log.info("Raw response from provider is ...{}", responseEntity);

            if (responseEntity != null && responseEntity.getBody() != null) {
                response.setStatus(
                    "200".equals(String.valueOf(responseEntity.getStatusCodeValue()))
                        ? TransactionStatus.SUCCESS.getCode()
                        : String.valueOf(responseEntity.getStatusCodeValue())
                );
                response.setMessage("Successful");
                response.setData(responseEntity.getBody());
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            //return new CalculatePaymentResponse(true, (String) errorMap.get("message"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResponseEntity<DefaultResponse> walletHistory(WalletHistoryRequest request) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        try {
            HttpEntity<?> httpEntity = new HttpEntity<>(getHeaders());
            String walletHistoryUrl = String.format(
                properties.getWalletHistoryUrl(),
                request.getMerchantId(),
                request.getToken(),
                request.getStart().format(DATE_FORMATTER),
                request.getEnd().format(DATE_FORMATTER)
            );
            log.info("Wallet History URL {}", walletHistoryUrl);

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplate()
                .exchange(walletHistoryUrl, HttpMethod.GET, httpEntity, String.class);
            log.info("API Response from Wallet History {} ", responseEntity);

            if (responseEntity.getBody() == null) {
                log.info("[-] Wallet history response body is null");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

            WalletHistoryResponse[] walletHistoryResponses = new ObjectMapper()
                .readValue(responseEntity.getBody(), WalletHistoryResponse[].class);

            log.info(">>>>>WalletHistoryResponses{}", walletHistoryResponses);

            response.setStatus(TransactionStatus.SUCCESS.getCode());
            response.setMessage(TransactionStatus.SUCCESS.getMessage());
            response.setData(walletHistoryResponses);
            log.info(">>> Wallet History data {}", response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.info("[-] Client side Error occurred while getting wallet history{} ", e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            log.info("[-] Exception happened while getting wallet history {} ", e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<DefaultResponse> vendorTopUps(VendorTopUpsRequest request) {
        DefaultResponse response = new DefaultResponse();
        VendorTopUpsResponse vendorTopUpsResponse = new VendorTopUpsResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to vendorTopUps is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String vendorTopUpsUrl = properties.getVendorTopupsUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(vendorTopUpsUrl, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider is ...{}", responseEntity);
            vendorTopUpsResponse = gson.fromJson(responseEntity.getBody(), VendorTopUpsResponse.class);
            String vendorTopUpsCode = String.valueOf(responseEntity.getStatusCodeValue());
            if ("200".equals(vendorTopUpsCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("Successful");
                response.setData(vendorTopUpsResponse);
            }
            if ("204".equals(vendorTopUpsCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("No records associated to user");
                response.setData(vendorTopUpsResponse);
            }
            if ("403".equals(vendorTopUpsCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("User does not have permissions ");
                response.setData(vendorTopUpsResponse);
            }

            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DefaultResponse> vendorStatement(VendorStatementRequest request) {
        DefaultResponse response = new DefaultResponse();
        VendorStatementResponse vendorStatementResponse = new VendorStatementResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        try {
            log.info("Request payload to vendorStatement is ...{}", request);
            String mappedRequest = new ObjectMapper().writeValueAsString(request);
            HttpEntity<?> httpEntity = new HttpEntity<>(mappedRequest, getRequestHeaders());
            String vendorStatement = properties.getVendorStatementUrl();

            ResponseEntity<String> responseEntity = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .exchange(vendorStatement, HttpMethod.POST, httpEntity, String.class);
            log.info("Raw response from provider vendorStatement is ...{}", responseEntity);
            vendorStatementResponse = gson.fromJson(responseEntity.getBody(), VendorStatementResponse.class);
            String vendorStatementCode = String.valueOf(responseEntity.getStatusCodeValue());
            if ("200".equals(vendorStatementCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("Successful");
                response.setData(vendorStatementResponse);
            }
            if ("204".equals(vendorStatementCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("No records associated to user");
                response.setData(vendorStatementResponse);
            }
            if ("403".equals(vendorStatementCode)) {
                response.setStatus(TransactionStatus.SUCCESS.getCode());
                response.setMessage("User does not have permissions ");
                response.setData(vendorStatementResponse);
            }

            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            log.info("[-] Exception happened while vending {} ", e.getMessage());
            log.info("[-] Client side Error occured {} ", e.getResponseBodyAsString());
            Map<String, String> errorMap = resolveErrorMessage(e.getResponseBodyAsString());
            log.info("Exception errorMap...{}", errorMap);
            response.setStatus((String) errorMap.get("code"));
            response.setMessage((String) errorMap.get("msgDeveloper"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
        return headers;
    }

    public HttpHeaders getRequestHeaders() {
        String token = null;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            log.info("Calling getSessionToken() ");
            token = getSessionToken();
            log.info("login sessionToken ...{}", token);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could Not get Token");
        }
        requestHeaders.set("Authorization", String.format("Bearer %s", token));
        return requestHeaders;
    }

    private Map<String, String> resolveErrorMessage(String responseBodyAsString) {
        Map<String, String> map = new Gson().fromJson(responseBodyAsString, Map.class);
        return map;
    }

    public String getSessionToken() throws Exception {
        String loginToken = null;
        String loginUrl = properties.getSessionTokenUrl();
        log.info("--loginToken url in getSessionToken is ---{}", loginUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", String.format("Basic %s", properties.getAuthorization()));
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", properties.getGrantType());
        map.add("username", properties.getUsername());
        map.add("password", properties.getPassword());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        try {
            log.info(
                "checking for grant_type {} username {} and password {} ....",
                properties.getGrantType(),
                properties.getUsername(),
                properties.getPassword()
            );
            ResponseEntity<String> response = restTemplateUtility
                .getRestTemplateIgnoreHostName()
                .postForEntity(loginUrl, entity, String.class);
            if (response != null && response.hasBody()) {
                SessionTokenResponse loginResponse = objectMapper.readValue(response.getBody(), SessionTokenResponse.class);
                log.info("Response from Session Token Provider's access token => {}", loginResponse);
                loginToken = loginResponse.getAccessToken();
                return loginToken;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Could not get a valid login token from the Provider service");
        }
        log.info("loginToken in getLoginToken token is: {} ", loginToken);
        return loginToken;
    }
}

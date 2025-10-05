package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ResponseCodeMappingService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ResponseCodeMappingRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.ResponseCodeMapping;
import com.systemspecs.remita.vending.vendingcommon.repository.ResponseCodeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseCodeMappingServiceImpl implements ResponseCodeMappingService {

    private final ResponseCodeMappingRepository repository;

    @Override
    public ResponseEntity<DefaultResponse> addProcessorResponseMapping(ResponseCodeMappingRequest request) {
        log.info("<<<<<Adding processor response code with: {}", request);
        if (StringUtils.isBlank(request.getProcessorId())) {
            throw new NotFoundException("ProcessorId can not be empty ");
        }
        if (StringUtils.isBlank(request.getProcessorResponseCode())) {
            throw new NotFoundException("ProcessorResponseCode can not be empty ");
        }
        if (StringUtils.isBlank(request.getStandardResponseCode())) {
            throw new NotFoundException("StandardResponseCode can not be empty ");
        }
        if (StringUtils.isBlank(request.getStandardResponseMessage())) {
            throw new NotFoundException("StandardResponseMessage can not be empty ");
        }
        List<ResponseCodeMapping> optionalResponseCodeMapping = repository.findByProcessorIdAndProcessorResponseCode(
            request.getProcessorId(),
            request.getProcessorResponseCode()
        );

        ResponseCodeMapping responseCodeMapping = getResponseCodeMapping(request);
        repository.save(responseCodeMapping);

        DefaultResponse response = new DefaultResponse();
        response.setStatus(ResponseStatus.SUCCESS.getCode());
        response.setMessage(ResponseStatus.SUCCESS.getMessage());
        response.setData(responseCodeMapping);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private static ResponseCodeMapping getResponseCodeMapping(ResponseCodeMappingRequest request) {
        ResponseCodeMapping responseCodeMapping = new ResponseCodeMapping();
        responseCodeMapping.setProcessorId(request.getProcessorId());
        responseCodeMapping.setProcessorResponseMessage(request.getProcessorResponseMessage());
        responseCodeMapping.setProcessorResponseCode(request.getProcessorResponseCode());
        responseCodeMapping.setStandardResponseMessage(request.getStandardResponseMessage());
        responseCodeMapping.setStandardResponseCode(request.getStandardResponseCode());
        responseCodeMapping.setCustomMessage(request.getCustomMessage());
        return responseCodeMapping;
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllResponseCode(Pageable pageable) {
        Page<ResponseCodeMapping> responseCodeMappings = repository.findAll(pageable);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(responseCodeMappings);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}

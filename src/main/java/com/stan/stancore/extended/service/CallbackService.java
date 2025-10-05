package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.dtonemodule.dto.response.IntlQueryResponse.QueryRes;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface CallbackService {
    ResponseEntity<DefaultResponse> processCallback(QueryRes queryRes);
}

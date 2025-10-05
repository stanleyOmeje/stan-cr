package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.dtonemodule.dto.response.IntlQueryResponse.QueryRes;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.CallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/vending/")
public class CallbackController {

    private final CallbackService callbackService;

    @ActivityTrail
    @PostMapping("/callback")
    public ResponseEntity<DefaultResponse> postCallback(@RequestBody QueryRes queryRes) {
        return callbackService.processCallback(queryRes);
    }
}

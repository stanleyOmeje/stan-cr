package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkRevendRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface BulkRevendService {
    ResponseEntity<DefaultResponse> processBulkRevend(BulkRevendRequest bulkRevendRequest);

    ResponseEntity<DefaultResponse> processBulkRevendFile(MultipartFile file);

    ResponseEntity<DefaultResponse> getUploadedFile(String bulkRevendReferenceId, Pageable pageable);
}

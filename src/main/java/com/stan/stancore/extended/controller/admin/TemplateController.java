package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.vending.extended.enums.Templates;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/admin/template")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

    @GetMapping("/{templateName}")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable() String templateName) {
        try {
            if (isValidPath(templateName)) {
                Templates template = Templates.valueOf(templateName.toUpperCase());

                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/" + template.path);
                if (inputStream == null) {
                    log.info("Could not resolve resource path >> ejecting");
                    throw new UnknownException("Could not resolve resource path", "99");
                }

                InputStreamResource resource = new InputStreamResource(inputStream);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + template.path)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);
            } else {
                throw new BadRequestException("Invalid template provided", "99");
            }
        } catch (UnknownException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.info("Get template request failed => {}", e.getMessage());
            throw new UnknownException("Could not fetch template", "99");
        }
    }

    private boolean isValidPath(String path) {
        return Arrays.stream(Templates.values()).anyMatch(i -> i.toString().equalsIgnoreCase(path));
    }
}

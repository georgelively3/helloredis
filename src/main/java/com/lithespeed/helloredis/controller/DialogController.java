package com.lithespeed.helloredis.controller;

import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.model.DialogResponseDTO;
import com.lithespeed.helloredis.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import static java.util.Map.entry;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/dialogs")
@RequiredArgsConstructor
@Tag(name = "Dialog", description = "Dialog management operations")
public class DialogController {

    private final DialogService dialogService;

    @Value("${aws.elasticache.enabled:NOT_SET}")
    private String elasticacheEnabled;

    @Value("${aws.elasticache.cluster-endpoint:NOT_SET}")
    private String clusterEndpoint;

    @Value("${aws.elasticache.cluster-name:NOT_SET}")
    private String clusterName;

    @Value("${aws.elasticache.region:NOT_SET}")
    private String region;

    @Value("${aws.elasticache.iam-username:NOT_SET}")
    private String iamUsername;

    @Value("${aws.elasticache.port:NOT_SET}")
    private String port;

    @GetMapping
    @Operation(summary = "Get all dialogs")
    public ResponseEntity<List<Dialog>> getAllDialogs() {
        return ResponseEntity.ok(dialogService.getAllDialogs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a dialog by id and request")
    public ResponseEntity<DialogResponseDTO> getDialog(@PathVariable int id, @RequestParam String request) {
        DialogResponseDTO response = dialogService.getDialogByIdAndRequest(id, request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new dialog")
    public ResponseEntity<String> createDialog(@RequestBody Dialog dialog) {
        Dialog created = dialogService.createDialog(dialog);
        return ResponseEntity.ok("Dialog created with id=" + created.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing dialog")
    public ResponseEntity<String> updateDialog(@PathVariable int id, @RequestBody Dialog dialog) {
        return dialogService.updateDialog(id, dialog)
                .<ResponseEntity<String>>map(d -> ResponseEntity.ok("Dialog updated with id=" + d.getId()))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dialog not found with id=" + id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dialog by id")
    public ResponseEntity<String> deleteDialog(@PathVariable int id) {
        if (dialogService.deleteDialog(id)) {
            return ResponseEntity.ok("Dialog deleted with id=" + id);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dialog not found with id=" + id);
    }

    @GetMapping("/debug/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("aws.elasticache.enabled", elasticacheEnabled);
        config.put("aws.elasticache.cluster-endpoint", clusterEndpoint);
        config.put("aws.elasticache.cluster-name", clusterName);
        config.put("aws.elasticache.region", region);
        config.put("aws.elasticache.iam-username", iamUsername);
        config.put("aws.elasticache.port", port);
        return config;
    }
}

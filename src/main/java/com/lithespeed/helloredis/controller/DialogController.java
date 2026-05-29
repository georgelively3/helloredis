package com.lithespeed.helloredis.controller;

import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dialogs")
@RequiredArgsConstructor
@Tag(name = "Dialog", description = "Dialog management operations")
public class DialogController {

    private final DialogService dialogService;

    @GetMapping
    @Operation(summary = "Get all dialogs")
    public ResponseEntity<List<Dialog>> getAllDialogs() {
        return ResponseEntity.ok(dialogService.getAllDialogs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a dialog by id and request")
    public ResponseEntity<Dialog> getDialog(@PathVariable int id, @RequestParam String request) {
        return dialogService.getDialogByIdAndRequest(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new dialog")
    public ResponseEntity<Dialog> createDialog(@RequestBody Dialog dialog) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dialogService.createDialog(dialog));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing dialog")
    public ResponseEntity<Dialog> updateDialog(@PathVariable int id, @RequestBody Dialog dialog) {
        return dialogService.updateDialog(id, dialog)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dialog by id")
    public ResponseEntity<Void> deleteDialog(@PathVariable int id) {
        return dialogService.deleteDialog(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

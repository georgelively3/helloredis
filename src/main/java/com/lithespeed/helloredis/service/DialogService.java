package com.lithespeed.helloredis.service;

import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.model.DialogResponseDTO;
import com.lithespeed.helloredis.repository.DialogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DialogService {

    private final DialogRepository dialogRepository;

    public List<Dialog> getAllDialogs() {
        return dialogRepository.findAll();
    }

    public DialogResponseDTO getDialogByIdAndRequest(int id, String request) {
        if (request == null) {
            log.warn("getDialogByIdAndRequest called with null request for id={}", id);
            return null;
        }
        return dialogRepository.getDialogByIdAndRequest(id, request).orElse(null);
    }

    public Dialog createDialog(Dialog dialog) {
        return dialogRepository.save(dialog);
    }

    public Optional<Dialog> updateDialog(int id, Dialog updated) {
        if (!dialogRepository.existsById(id)) {
            log.warn("Attempted to update non-existent dialog id={}", id);
            return Optional.empty();
        }
        updated.setId(id);
        return Optional.of(dialogRepository.save(updated));
    }

    public boolean deleteDialog(int id) {
        if (!dialogRepository.existsById(id)) {
            log.warn("Attempted to delete non-existent dialog id={}", id);
            return false;
        }
        dialogRepository.deleteById(id);
        return true;
    }
}

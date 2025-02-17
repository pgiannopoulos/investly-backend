package com.investly.app.services;

import com.investly.app.dao.ResponseEntity;
import com.investly.app.dao.ResponseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ResponseService {

    private final ResponseRepository responseRepository;

    public ResponseService(ResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    public void saveResponse(ResponseEntity aiResponse) {
        responseRepository.save(aiResponse);
    }

    public ResponseEntity getResponseByMessageId(Long messageId) {
        Optional<ResponseEntity> response = responseRepository.findById(messageId);
        return response.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Response not found"));
    }
}

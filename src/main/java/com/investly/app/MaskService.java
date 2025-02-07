package com.investly.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MaskService {

    private final MaskRepository maskRepository;

    @Autowired
    public MaskService(MaskRepository maskRepository) {
        this.maskRepository = maskRepository;
    }

    public List<MaskEntity> getAllMasks() {
        return maskRepository.findAll();
    }
}

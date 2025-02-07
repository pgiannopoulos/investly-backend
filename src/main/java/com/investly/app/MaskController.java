package com.investly.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/getmasks")
public class MaskController {

    private final MaskService maskService;

    @Autowired
    public MaskController(MaskService maskService) {
        this.maskService = maskService;
    }

    @GetMapping
    public ResponseEntity<List<MaskEntity>> getAllMasks() {
        List<MaskEntity> masks = maskService.getAllMasks();
        return ResponseEntity.ok(masks);
    }
}

package com.cinebook.controller;

import com.cinebook.dto.RefundResponse;
import com.cinebook.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable Long refundId) {
        RefundResponse refund = refundService.getRefundResponseById(refundId);
        return ResponseEntity.ok(refund);
    }
}

package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public String initiateDeposit(@RequestParam BigDecimal amount) throws IOException {
        return walletService.initiateDeposit(amount);
    }

    @GetMapping("/deposit/success")
    public String captureDeposit(@RequestParam("token") String orderId) throws IOException {
        return walletService.captureDeposit(orderId);
    }

    @GetMapping("/deposit/cancel")
    public String cancelDeposit() {
        return "Deposit cancelled";
    }
}
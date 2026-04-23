package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.service.WalletService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private static final String FRONTEND_BASE = "http://localhost:5173";

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public String initiateDeposit(@RequestParam BigDecimal amount) throws IOException {
        return walletService.initiateDeposit(amount);
    }

    @GetMapping("/deposit/success")
    public void captureDeposit(@RequestParam("token") String orderId,
                               HttpServletResponse response) throws IOException {
        try {
            WalletService.DepositResult result = walletService.captureDeposit(orderId);
            String redirectUrl = FRONTEND_BASE + "/wallet/success"
                    + "?amount=" + result.amount()
                    + "&balance=" + result.newBalance()
                    + "&message=" + URLEncoder.encode(result.message(), StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            String errorUrl = FRONTEND_BASE + "/wallet/cancel"
                    + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    @GetMapping("/deposit/cancel")
    public void cancelDeposit(HttpServletResponse response) throws IOException {
        response.sendRedirect(FRONTEND_BASE + "/wallet/cancel");
    }
}
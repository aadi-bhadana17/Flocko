package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.model.User;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.core.PayPalHttpClient;
import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final UserAuthorization userAuthorization;
    private final PayPalHttpClient payPalHttpClient;

    public WalletService(UserRepository userRepository, UserAuthorization userAuthorization, PayPalHttpClient payPalHttpClient) {
        this.userRepository = userRepository;
        this.userAuthorization = userAuthorization;
        this.payPalHttpClient = payPalHttpClient;
    }

    /** Immutable result returned after a successful deposit capture. */
    public record DepositResult(BigDecimal amount, BigDecimal newBalance, String message) {}

    public String initiateDeposit(BigDecimal amount) throws IOException {
        User user = userAuthorization.authorizeUser();

        if (amount.compareTo(BigDecimal.valueOf(100)) < 0 ||
                amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            throw new IllegalArgumentException("Amount must be between 100 and 5000");
        }

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        AmountWithBreakdown amount_ = new AmountWithBreakdown()
                .currencyCode("USD")
                .value(amount.toString());

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .amountWithBreakdown(amount_);

        orderRequest.purchaseUnits(List.of(purchaseUnit));

        ApplicationContext context = new ApplicationContext()
                .returnUrl("http://localhost:8080/api/wallet/deposit/success")
                .cancelUrl("http://localhost:8080/api/wallet/deposit/cancel");

        orderRequest.applicationContext(context);

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
        HttpResponse<Order> response = payPalHttpClient.execute(request);

        String approvalUrl = response.result().links().stream()
                .filter(link -> link.rel().equals("approve"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No approval URL from PayPal"))
                .href();

        String paypalOrderId = response.result().id();
        user.setPendingDepositAmount(amount);
        user.setPendingPaypalOrderId(paypalOrderId);
        userRepository.save(user);

        return approvalUrl;
    }

    public DepositResult captureDeposit(String orderId) throws IOException {
        User user = userRepository.findByPendingPaypalOrderId(orderId);

        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.requestBody(new OrderRequest());
        HttpResponse<Order> response = payPalHttpClient.execute(request);

        if (response.result().status().equals("COMPLETED")) {
            BigDecimal depositAmount = user.getPendingDepositAmount();
            user.setWalletBalance(user.getWalletBalance().add(depositAmount));
            user.setPendingDepositAmount(null);
            user.setPendingPaypalOrderId(null);
            userRepository.save(user);

            return new DepositResult(
                    depositAmount,
                    user.getWalletBalance(),
                    "₹" + depositAmount + " deposited successfully"
            );
        }
        throw new RuntimeException("Payment capture failed");
    }
}
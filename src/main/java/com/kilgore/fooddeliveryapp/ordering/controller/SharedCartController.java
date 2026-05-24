package com.kilgore.fooddeliveryapp.ordering.controller;

import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.SharedCartRequest;
import com.kilgore.fooddeliveryapp.wallet.dto.request.WalletContributionRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.SharedCartResponse;
import com.kilgore.fooddeliveryapp.ordering.service.SharedCartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared-cart")
public class SharedCartController {

    private final SharedCartService sharedCartService;

    public SharedCartController(SharedCartService sharedCartService) {
        this.sharedCartService = sharedCartService;
    }

    @GetMapping("/current")
    public SharedCartResponse getCurrentSharedCart() {
        return sharedCartService.getCurrentSharedCart();
    }

    @PostMapping
    public SharedCartResponse createSharedCart(@RequestBody SharedCartRequest request) {
        return sharedCartService.createSharedCart(request);
    }

    @GetMapping("/{code}")
    public SharedCartResponse getSharedCartByCode(@PathVariable String code) {
        return sharedCartService.getSharedCartByCode(code);
    }

    @PostMapping("/{code}/join")
    public String joinSharedCart(@PathVariable String code) {
        return sharedCartService.joinSharedCart(code);
    }

    @PostMapping("/{code}/cart")
    public SharedCartResponse addItemToSharedCart(@PathVariable String code, @RequestBody AddToCartRequest request) {
        return sharedCartService.addItemToSharedCart(code, request);
    }

    @PatchMapping("/{code}/contribute")
    public SharedCartResponse contributeToSharedCart(@PathVariable String code,
                                                     @RequestBody WalletContributionRequest request) {
        return sharedCartService.contributeToSharedCart(code, request);
    }

    @DeleteMapping("/{code}/cart/{cartItemId}")
    public SharedCartResponse removeItemFromSharedCart(@PathVariable String code, @PathVariable Long cartItemId) {
        return sharedCartService.removeItemFromSharedCart(code, cartItemId);
    }

    @PostMapping("/{code}/checkout")
    public String checkoutSharedCart(@PathVariable String code,
                                     @RequestBody PlaceOrderRequest request) {
        return sharedCartService.checkoutSharedCart(code, request);
    }

}

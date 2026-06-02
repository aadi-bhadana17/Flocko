package com.kilgore.fooddeliveryapp.ordering.controller;

import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateCartItemRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.CartResponse;
import com.kilgore.fooddeliveryapp.ordering.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping()
    public CartResponse addToCart(@RequestBody AddToCartRequest request) {
        return cartService.addToCart(request);
    }

    @GetMapping()
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PutMapping("/{cartItemId}")
    public CartResponse updateCartItemQuantity(@PathVariable Long cartItemId,
                                               @RequestBody UpdateCartItemRequest request) {
        return cartService.updateCartItemQuantity(cartItemId, request);
    }

    @DeleteMapping("/{cartItemId}")
    public CartResponse deleteCartItem(@PathVariable Long cartItemId) {
        return cartService.removeCartItem(cartItemId);
    }

    @DeleteMapping()
    public String deleteCart() {
        return cartService.clearCart();
    }
}

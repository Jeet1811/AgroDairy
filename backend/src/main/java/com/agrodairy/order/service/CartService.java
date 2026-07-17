package com.agrodairy.order.service;

import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.order.dto.AddCartItemRequest;
import com.agrodairy.order.dto.CartItemResponse;
import com.agrodairy.order.dto.CartResponse;
import com.agrodairy.order.dto.UpdateCartItemRequest;
import com.agrodairy.order.entity.Cart;
import com.agrodairy.order.entity.CartItem;
import com.agrodairy.order.repository.CartItemRepository;
import com.agrodairy.order.repository.CartRepository;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(UUID userId) {
        return toResponse(getOrCreateCart(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = findActiveProductOrThrow(request.productId());
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> CartItem.builder().cart(cart).product(product).quantity(0).build());
        item.setQuantity(item.getQuantity() + request.quantity());
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID productId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresent(cartItemRepository::delete);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    /** Used by OrderService to read the cart contents it needs to build an order from. */
    @Transactional
    public List<CartItem> getCartItemEntities(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private Product findActiveProductOrThrow(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.isActive()) {
            throw new NotFoundException("Product not found");
        }
        return product;
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getProduct().getPrice();
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(item.getProduct().getId(), item.getProduct().getName(),
                            unitPrice, item.getQuantity(), subtotal);
                })
                .toList();
        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(itemResponses, total);
    }
}

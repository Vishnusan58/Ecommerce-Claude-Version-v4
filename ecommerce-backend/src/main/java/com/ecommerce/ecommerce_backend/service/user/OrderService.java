package com.ecommerce.ecommerce_backend.service.user;

import com.ecommerce.ecommerce_backend.enums.OrderStatus;
import com.ecommerce.ecommerce_backend.model.*;
import com.ecommerce.ecommerce_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final PremiumSubscriptionRepository premiumSubscriptionRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        CartRepository cartRepository,
                        PremiumSubscriptionRepository premiumSubscriptionRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.premiumSubscriptionRepository = premiumSubscriptionRepository;
        this.productRepository = productRepository;
    }

    // Original method signature for backward compatibility
    public Order placeOrder(User user, UserAddress address, LocalDate preferredDeliveryDate) {
        return placeOrder(user, address, preferredDeliveryDate, "COD", null);
    }

    // New method with payment method and coupon code support
    @Transactional
    public Order placeOrder(User user, UserAddress address, LocalDate preferredDeliveryDate,
                           String paymentMethod, String couponCode) {

        // 1️⃣ Fetch cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 2️⃣ Validate stock availability for all items
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockQuantity() + ", Requested: " + item.getQuantity());
            }
        }

        // 3️⃣ Calculate total product amount
        double totalAmount = 0;
        for (CartItem item : cartItems) {
            totalAmount += item.getProduct().getPrice() * item.getQuantity();
        }

        // 3️⃣ Check premium status
        boolean isPremium = premiumSubscriptionRepository
                .existsByUserAndActiveTrue(user);

        // 4️⃣ Create Order
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setCouponCode(couponCode);

        // 5️⃣ Delivery charge logic
        double deliveryCharge = isPremium ? 0 : 50;
        order.setDeliveryCharge(deliveryCharge);

        // 6️⃣ Priority shipping
        order.setPriority(isPremium);

        // 7️⃣ Flexible delivery date (premium only)
        if (preferredDeliveryDate != null) {
            if (!isPremium) {
                throw new RuntimeException(
                        "Only premium users can choose delivery date"
                );
            }
            order.setPreferredDeliveryDate(preferredDeliveryDate);
        }

        // 8️⃣ Apply coupon discount if provided
        double discount = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            // TODO: Implement coupon validation and discount calculation
            // For now, just log it
        }

        // 9️⃣ Final amount
        order.setFinalAmount(totalAmount + deliveryCharge - discount);

        // 🔟 Save order first
        Order savedOrder = orderRepository.save(order);

        // 1️⃣1️⃣ Create order items and reduce stock
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setSeller(product.getSeller());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setDiscountAtPurchase(0);

            orderItemRepository.save(orderItem);

            // Reduce stock quantity
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // 1️⃣2️⃣ Clear cart after successful order
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

    public List<Order> getOrders(User user) {
        return orderRepository.findByUser(user);
    }

    public Order getOrderById(Long orderId, User user) {
        return orderRepository.findByIdAndUser(orderId, user)
                .orElse(null);
    }

    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order can be cancelled
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel a delivered order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }

        // Restore stock for all order items
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public void requestRefund(Long orderId, User user, String reason) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order can be refunded
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Only delivered orders can be refunded");
        }

        // Mark as refund requested (you might want to add a REFUND_REQUESTED status)
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private boolean isPremiumUser(User user) {
        return premiumSubscriptionRepository
                .existsByUserAndActiveTrue(user);
    }

}

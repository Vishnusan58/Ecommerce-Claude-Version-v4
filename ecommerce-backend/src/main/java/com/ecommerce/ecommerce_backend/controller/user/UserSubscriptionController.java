package com.ecommerce.ecommerce_backend.controller.user;

import com.ecommerce.ecommerce_backend.dto.subscription.SubscribePremiumDTO;
import com.ecommerce.ecommerce_backend.dto.subscription.SubscriptionResponseDTO;
import com.ecommerce.ecommerce_backend.model.PremiumSubscription;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.service.auth.AuthService;
import com.ecommerce.ecommerce_backend.service.user.PremiumSubscriptionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/subscription")
public class UserSubscriptionController {

    private final PremiumSubscriptionService premiumSubscriptionService;
    private final AuthService authService;

    public UserSubscriptionController(
            PremiumSubscriptionService premiumSubscriptionService,
            AuthService authService
    ) {
        this.premiumSubscriptionService = premiumSubscriptionService;
        this.authService = authService;
    }

    @PostMapping("/subscribe")
    public SubscriptionResponseDTO subscribe(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody SubscribePremiumDTO dto
    ) {
        User user = authService.getUserById(userId);
        PremiumSubscription subscription = premiumSubscriptionService.subscribe(user, dto.getPlanType());

        SubscriptionResponseDTO response = new SubscriptionResponseDTO();
        response.setSubscriptionId(subscription.getId());
        response.setPlanType(subscription.getPlanType());
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        response.setActive(subscription.isActive());
        response.setAutoRenew(subscription.isAutoRenew());
        response.setMessage("Premium subscription activated successfully");

        return response;
    }

    @DeleteMapping("/cancel")
    public SubscriptionResponseDTO cancelSubscription(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        User user = authService.getUserById(userId);
        premiumSubscriptionService.cancelSubscription(user);

        SubscriptionResponseDTO response = new SubscriptionResponseDTO();
        response.setMessage("Subscription cancelled successfully");
        response.setActive(false);

        return response;
    }
}

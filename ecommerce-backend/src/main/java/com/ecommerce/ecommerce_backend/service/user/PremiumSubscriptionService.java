package com.ecommerce.ecommerce_backend.service.user;

import com.ecommerce.ecommerce_backend.enums.SubscriptionPlan;
import com.ecommerce.ecommerce_backend.model.PremiumSubscription;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.repository.PremiumSubscriptionRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PremiumSubscriptionService {

    private final PremiumSubscriptionRepository premiumSubscriptionRepository;
    private final UserRepository userRepository;

    public PremiumSubscriptionService(
            PremiumSubscriptionRepository premiumSubscriptionRepository,
            UserRepository userRepository
    ) {
        this.premiumSubscriptionRepository = premiumSubscriptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PremiumSubscription subscribe(User user, SubscriptionPlan planType) {

        // Check for existing active subscription
        boolean alreadyActive = premiumSubscriptionRepository.existsByUserAndActiveTrue(user);
        if (alreadyActive) {
            throw new RuntimeException("User already has an active premium subscription");
        }

        // Check if user has an existing (inactive) subscription to reactivate
        Optional<PremiumSubscription> existingSubscription = premiumSubscriptionRepository.findByUser(user);

        PremiumSubscription subscription;
        if (existingSubscription.isPresent()) {
            // Reactivate existing subscription
            subscription = existingSubscription.get();
        } else {
            // Create new subscription
            subscription = new PremiumSubscription();
            subscription.setUser(user);
        }

        subscription.setPlanType(planType);
        subscription.setStartDate(LocalDate.now());
        subscription.setActive(true);
        subscription.setAutoRenew(true);

        if (planType == SubscriptionPlan.MONTHLY) {
            subscription.setEndDate(LocalDate.now().plusMonths(1));
        } else {
            subscription.setEndDate(LocalDate.now().plusYears(1));
        }

        // Update user's premium status
        user.setPremiumStatus(true);
        userRepository.save(user);

        return premiumSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(User user) {
        PremiumSubscription subscription =
                premiumSubscriptionRepository.findByUserAndActiveTrue(user)
                        .orElseThrow(() -> new RuntimeException("No active subscription found"));

        subscription.setActive(false);
        subscription.setAutoRenew(false);
        premiumSubscriptionRepository.save(subscription);

        // Update user's premium status
        user.setPremiumStatus(false);
        userRepository.save(user);
    }
}

package com.example.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for managing ticket inventory using Redis.
 * Uses atomic operations to prevent overselling under high traffic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StringRedisTemplate redisTemplate;

    private static final String INVENTORY_KEY_PREFIX = "inventory:event:";
    private static final String HOLD_KEY_PREFIX = "hold:order:";

    /**
     * Initialize inventory for an event in Redis.
     */
    public void initializeInventory(Long eventId, int quantity) {
        String key = getInventoryKey(eventId);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));
        log.info("Initialized inventory for event {}: {} tickets", eventId, quantity);
    }

    /**
     * Get current available inventory for an event.
     */
    public int getAvailableInventory(Long eventId) {
        String key = getInventoryKey(eventId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Atomically hold tickets for an order.
     * Uses DECR to prevent race conditions.
     * 
     * @param eventId  The event ID
     * @param quantity Number of tickets to hold
     * @return true if tickets were successfully held, false if not enough inventory
     */
    public boolean holdTickets(Long eventId, int quantity) {
        // Artificial delay for stress testing (Mock Payment Gateway latency)
        simulateDelay();

        String key = getInventoryKey(eventId);

        // Atomic decrement
        Long remaining = redisTemplate.opsForValue().decrement(key, quantity);

        if (remaining == null || remaining < 0) {
            // Rollback - not enough inventory
            if (remaining != null) {
                redisTemplate.opsForValue().increment(key, quantity);
            }
            log.warn("Failed to hold {} tickets for event {}: insufficient inventory", quantity, eventId);
            return false;
        }

        log.info("Held {} tickets for event {}. Remaining: {}", quantity, eventId, remaining);
        return true;
    }

    /**
     * Release held tickets back to inventory.
     */
    public void releaseTickets(Long eventId, int quantity) {
        String key = getInventoryKey(eventId);
        Long newValue = redisTemplate.opsForValue().increment(key, quantity);
        log.info("Released {} tickets for event {}. New total: {}", quantity, eventId, newValue);
    }

    /**
     * Set a hold key with TTL for order expiration tracking.
     */
    public void setOrderHold(String orderCode, Long eventId, int quantity, Duration ttl) {
        String key = getHoldKey(orderCode);
        String value = eventId + ":" + quantity;
        redisTemplate.opsForValue().set(key, value, ttl);
        log.info("Set hold for order {} with TTL {}", orderCode, ttl);
    }

    /**
     * Get hold information for an order.
     * 
     * @return Array of [eventId, quantity] or null if not found
     */
    public Long[] getOrderHold(String orderCode) {
        String key = getHoldKey(orderCode);
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            String[] parts = value.split(":");
            return new Long[] { Long.parseLong(parts[0]), Long.parseLong(parts[1]) };
        }
        return null;
    }

    /**
     * Remove the hold key after payment success.
     */
    public void removeOrderHold(String orderCode) {
        String key = getHoldKey(orderCode);
        redisTemplate.delete(key);
        log.info("Removed hold for order {}", orderCode);
    }

    /**
     * Check if hold exists for an order.
     */
    public boolean hasOrderHold(String orderCode) {
        String key = getHoldKey(orderCode);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String getInventoryKey(Long eventId) {
        return INVENTORY_KEY_PREFIX + eventId;
    }

    private String getHoldKey(String orderCode) {
        return HOLD_KEY_PREFIX + orderCode;
    }

    /**
     * Artificial delay to simulate processing/latency.
     */
    private void simulateDelay() {
        try {
            // Delay 10-50ms
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

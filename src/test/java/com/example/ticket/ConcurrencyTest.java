package com.example.ticket;

import com.example.ticket.domain.entity.Event;
import com.example.ticket.domain.enums.EventStatus;
import com.example.ticket.domain.repository.EventRepository;
import com.example.ticket.dto.request.CreateOrderRequest;
import com.example.ticket.service.InventoryService;
import com.example.ticket.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private com.example.ticket.domain.repository.UserRepository userRepository;

    @Test
    public void testTicketRaceCondition() throws InterruptedException {
        // 0. Setup User
        com.example.ticket.domain.entity.User user = com.example.ticket.domain.entity.User.builder()
                .email("test_race@example.com")
                .password("password")
                .fullName("Race Condition User")
                .role("USER")
                .enabled(true)
                .build();
        user = userRepository.save(user);
        Long userId = user.getId();

        // 1. Setup: Create a HOT event with only 10 tickets
        Event event = Event.builder()
                .name("HOT CONCERT")
                .description("Limited tickets")
                .venue("Small Venue")
                .eventDate(LocalDateTime.now().plusDays(1))
                .basePrice(new BigDecimal("100000"))
                .totalTickets(10)
                .availableTickets(10)
                .status(EventStatus.ON_SALE)
                .build();

        event = eventRepository.save(event);
        inventoryService.initializeInventory(event.getId(), 10);

        // 2. Simulation: 50 users trying to buy 1 ticket at the exact same time
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        Long eventId = event.getId();

        System.out.println("--- STARTING RACE CONDITION TEST ---");
        System.out.println("Tickets available: 10");
        System.out.println("Concurrent users: " + numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // Create request
                    CreateOrderRequest request = CreateOrderRequest.builder()
                            .eventId(eventId)
                            .quantity(1)
                            .customerName("User")
                            .customerEmail("user@example.com")
                            .customerPhone("123456789")
                            .build();

                    // Try to buy
                    orderService.createOrder(request, "127.0.0.1", userId);
                    successfulOrders.incrementAndGet();
                    System.out.println("Success! Ticket bought.");
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    // System.out.println("Failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to finish
        latch.await();

        // 3. Verification
        int remainingInventory = inventoryService.getAvailableInventory(eventId);

        System.out.println("--- TEST RESULT ---");
        System.out.println("Successful orders: " + successfulOrders.get());
        System.out.println("Failed orders: " + failedOrders.get());
        System.out.println("Remaining inventory (Redis): " + remainingInventory);

        // Assertions
        assertEquals(10, successfulOrders.get(), "Should only sell exactly 10 tickets");
        assertEquals(40, failedOrders.get(), "Should reject exactly 40 users");
        assertEquals(0, remainingInventory, "Inventory should be 0");
    }
}

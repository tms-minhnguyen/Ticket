package com.example.ticket.config;

import com.example.ticket.domain.entity.Event;
import com.example.ticket.domain.entity.User;
import com.example.ticket.domain.enums.EventStatus;
import com.example.ticket.domain.repository.EventRepository;
import com.example.ticket.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final com.example.ticket.domain.repository.OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("Starting Data Seeding...");

            // CLEANUP DATA (Order -> Event/User)
            orderRepository.deleteAll();
            eventRepository.deleteAll();
            // Optional: clean users if you want a full reset, but usually we keep users or
            // check count
            // userRepository.deleteAll();

            // Seed Users
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Admin User")
                        .role("ADMIN")
                        .enabled(true)
                        .phone("0987654321")
                        .build();
                userRepository.save(admin);

                User user = User.builder()
                        .email("user@example.com")
                        .password(passwordEncoder.encode("user123"))
                        .fullName("Normal User")
                        .role("USER")
                        .enabled(true)
                        .phone("0123456789")
                        .build();
                userRepository.save(user);

                log.info("Seeded Users: admin@example.com, user@example.com");
            }

            // Seed Events - FLASH SALE MODE
            // Always clear old events to ensure clean slate for Flash Sale demo
            eventRepository.deleteAll();

            // Create Single Flash Sale Event
            Event event1 = Event.builder()
                    .name("BLACKPINK WORLD TOUR 2026")
                    .description("The ultimate Flash Sale event! Only 50 tickets available.")
                    .venue("My Dinh Stadium")
                    .address("Le Duc Tho, Hanoi")
                    // Sale starts 10 seconds after server start/restart
                    .saleStartTime(LocalDateTime.now().plusSeconds(20))
                    // Sale ends 10 minutes later
                    .saleEndTime(LocalDateTime.now().plusMinutes(20))
                    .eventDate(LocalDateTime.now().plusMonths(1))
                    .basePrice(new BigDecimal("9999999"))
                    .totalTickets(1)
                    .availableTickets(1)
                    .maxTicketsPerUser(1) // Limit 2 per user
                    .imageUrl("https://images.unsplash.com/photo-1459749411177-3a293022c4d3")
                    .status(EventStatus.ON_SALE)
                    .build();

            event1 = eventRepository.save(event1);

            // IMPORTANT: Sync with Redis for InventoryService
            String redisKey = "inventory:event:" + event1.getId();
            redisTemplate.opsForValue().set(redisKey, 50);

            log.info("Seeded Flash Sale Event: BLACKPINK WORLD TOUR 2026 (Starts in 10s)");

            log.info("Data Seeding Completed.");
        };
    }
}

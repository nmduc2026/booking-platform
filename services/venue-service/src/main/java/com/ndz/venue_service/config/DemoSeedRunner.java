package com.ndz.venue_service.config;

import com.ndz.venue_service.entity.Resource;
import com.ndz.venue_service.entity.ResourceType;
import com.ndz.venue_service.entity.Shop;
import com.ndz.venue_service.entity.ShopStatus;
import com.ndz.venue_service.entity.SlotStatus;
import com.ndz.venue_service.entity.TimeSlot;
import com.ndz.venue_service.repository.ResourceRepository;
import com.ndz.venue_service.repository.ShopRepository;
import com.ndz.venue_service.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    private static final List<LocalTime> SLOT_STARTS = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(16, 0)
    );

    private final ShopRepository shopRepository;
    private final ResourceRepository resourceRepository;
    private final TimeSlotRepository timeSlotRepository;

    public DemoSeedRunner(
            ShopRepository shopRepository,
            ResourceRepository resourceRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        this.shopRepository = shopRepository;
        this.resourceRepository = resourceRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Shop shop = shopRepository.findById(DemoSeedIds.SHOP_ID).orElseGet(() -> {
            Shop created = new Shop();
            created.setId(DemoSeedIds.SHOP_ID);
            created.setName("Serenity Spa");
            created.setAddress("12 Nguyen Hue, District 1, HCMC");
            created.setDescription("Demo spa with massage rooms and therapists.");
            created.setCreatedByAdminId(DemoSeedIds.ADMIN_ID);
            created.setStatus(ShopStatus.ACTIVE);
            return shopRepository.save(created);
        });

        Resource room = ensureResource(
                DemoSeedIds.ROOM_RESOURCE_ID,
                shop.getId(),
                "Massage Room 1",
                ResourceType.ROOM
        );
        Resource staff = ensureResource(
                DemoSeedIds.STAFF_RESOURCE_ID,
                shop.getId(),
                "Therapist Linh",
                ResourceType.STAFF
        );

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int createdSlots = 0;
        for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            createdSlots += seedSlotsForDay(shop.getId(), room.getId(), date, new BigDecimal("45.00"));
            createdSlots += seedSlotsForDay(shop.getId(), staff.getId(), date, new BigDecimal("60.00"));
        }

        log.info(
                "Demo venue seed ready — shop={} ({}) resources=2 slotsAdded={}",
                shop.getName(),
                shop.getId(),
                createdSlots
        );
    }

    private Resource ensureResource(UUID id, UUID shopId, String name, ResourceType type) {
        return resourceRepository.findById(id).orElseGet(() -> {
            Resource resource = new Resource();
            resource.setId(id);
            resource.setShopId(shopId);
            resource.setName(name);
            resource.setType(type);
            return resourceRepository.save(resource);
        });
    }

    private int seedSlotsForDay(UUID shopId, UUID resourceId, LocalDate date, BigDecimal price) {
        int created = 0;
        for (LocalTime startLocal : SLOT_STARTS) {
            var start = date.atTime(startLocal).toInstant(ZoneOffset.UTC);
            var end = date.atTime(startLocal.plusHours(1)).toInstant(ZoneOffset.UTC);
            if (timeSlotRepository.existsByResourceIdAndStartTime(resourceId, start)) {
                continue;
            }
            TimeSlot slot = new TimeSlot();
            slot.setShopId(shopId);
            slot.setResourceId(resourceId);
            slot.setStartTime(start);
            slot.setEndTime(end);
            slot.setPrice(price);
            slot.setStatus(SlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
            created++;
        }
        return created;
    }
}

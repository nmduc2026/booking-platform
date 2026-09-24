package com.ndz.venue_service.service;

import com.ndz.venue_service.dto.CreateResourceRequest;
import com.ndz.venue_service.dto.CreateShopRequest;
import com.ndz.venue_service.dto.CreateTimeSlotRequest;
import com.ndz.venue_service.dto.ResourceResponse;
import com.ndz.venue_service.dto.ShopResponse;
import com.ndz.venue_service.dto.TimeSlotResponse;
import com.ndz.venue_service.entity.Resource;
import com.ndz.venue_service.entity.Shop;
import com.ndz.venue_service.entity.ShopStatus;
import com.ndz.venue_service.entity.SlotStatus;
import com.ndz.venue_service.entity.TimeSlot;
import com.ndz.venue_service.exception.ApiException;
import com.ndz.venue_service.repository.ResourceRepository;
import com.ndz.venue_service.repository.ShopRepository;
import com.ndz.venue_service.repository.TimeSlotRepository;
import com.ndz.venue_service.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class VenueService {

    private final ShopRepository shopRepository;
    private final ResourceRepository resourceRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SlotCacheService slotCacheService;

    public VenueService(
            ShopRepository shopRepository,
            ResourceRepository resourceRepository,
            TimeSlotRepository timeSlotRepository,
            SlotCacheService slotCacheService
    ) {
        this.shopRepository = shopRepository;
        this.resourceRepository = resourceRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.slotCacheService = slotCacheService;
    }

    @Transactional
    public ShopResponse createShop(CreateShopRequest request, UUID adminId) {
        Shop shop = new Shop();
        shop.setName(request.name().trim());
        shop.setAddress(request.address());
        shop.setDescription(request.description());
        shop.setCreatedByAdminId(adminId);
        shop.setStatus(ShopStatus.ACTIVE);
        shopRepository.save(shop);
        return ShopResponse.from(shop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> listActiveShops() {
        return shopRepository.findByStatusOrderByNameAsc(ShopStatus.ACTIVE).stream()
                .map(ShopResponse::from)
                .toList();
    }

    @Transactional
    public ResourceResponse createResource(UUID shopId, CreateResourceRequest request, UserPrincipal principal) {
        assertCanManageShop(principal, shopId);
        requireActiveShop(shopId);

        Resource resource = new Resource();
        resource.setShopId(shopId);
        resource.setName(request.name().trim());
        resource.setType(request.type());
        resourceRepository.save(resource);
        return ResourceResponse.from(resource);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID shopId, UserPrincipal principal) {
        assertCanManageShop(principal, shopId);
        requireActiveShop(shopId);
        return resourceRepository.findByShopIdOrderByNameAsc(shopId).stream()
                .map(ResourceResponse::from)
                .toList();
    }

    @Transactional
    public TimeSlotResponse createSlot(UUID shopId, CreateTimeSlotRequest request, UserPrincipal principal) {
        assertCanManageShop(principal, shopId);
        requireActiveShop(shopId);

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        Resource resource = resourceRepository.findByIdAndShopId(request.resourceId(), shopId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Resource not found in this shop"));

        if (timeSlotRepository.existsByResourceIdAndStartTime(resource.getId(), request.startTime())) {
            throw new ApiException(HttpStatus.CONFLICT, "Slot already exists for this resource and start time");
        }

        TimeSlot slot = new TimeSlot();
        slot.setShopId(shopId);
        slot.setResourceId(resource.getId());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setPrice(request.price());
        slot.setStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);
        slotCacheService.invalidateShop(shopId);
        return TimeSlotResponse.from(slot);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> listAvailableSlots(UUID shopId, LocalDate date) {
        requireActiveShop(shopId);

        List<TimeSlotResponse> cached = slotCacheService.get(shopId, date);
        if (cached != null) {
            return cached;
        }

        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<TimeSlotResponse> slots = timeSlotRepository
                .findByShopIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        shopId,
                        SlotStatus.AVAILABLE,
                        start,
                        end
                )
                .stream()
                .map(TimeSlotResponse::from)
                .toList();

        slotCacheService.put(shopId, date, slots);
        return slots;
    }

    @Transactional(readOnly = true)
    public TimeSlotResponse getSlot(UUID slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        return TimeSlotResponse.from(slot);
    }

    public void invalidateShopSlotCache(UUID shopId) {
        slotCacheService.invalidateShop(shopId);
    }

    private void assertCanManageShop(UserPrincipal principal, UUID shopId) {
        if (!principal.canManageShop(shopId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not manage this shop");
        }
    }

    private void requireActiveShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shop not found"));
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Shop is inactive");
        }
    }
}

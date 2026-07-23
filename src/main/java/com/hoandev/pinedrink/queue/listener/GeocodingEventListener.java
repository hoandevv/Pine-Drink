package com.hoandev.pinedrink.queue.listener;

import com.hoandev.pinedrink.entity.CustomerAddress;
import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import com.hoandev.pinedrink.queue.event.geocoding.GeocodingRequestEvent;
import com.hoandev.pinedrink.repository.CustomerAddressRepository;
import com.hoandev.pinedrink.service.GeocodingCacheService;
import com.hoandev.pinedrink.service.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listener for geocoding request events.
 * Processes geocoding requests asynchronously.
 */
@Component
@Slf4j
public class GeocodingEventListener {

    private final CustomerAddressRepository customerAddressRepository;
    
    @Qualifier("nominatimGeocodingService")
    private final GeocodingService geocodingService;
    
    private final GeocodingCacheService geocodingCacheService;

    public GeocodingEventListener(CustomerAddressRepository customerAddressRepository, GeocodingService geocodingService, GeocodingCacheService geocodingCacheService) {
        this.customerAddressRepository = customerAddressRepository;
        this.geocodingService = geocodingService;
        this.geocodingCacheService = geocodingCacheService;
    }

    /**
     * Handles geocoding request events.
     * Attempts to geocode the address and update the database.
     *
     * @param event the geocoding request event
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.geocoding:geocoding-queue}")
    public void handleGeocodingRequest(GeocodingRequestEvent event) {
        log.info("Processing geocoding request for addressId: {}", event.getAddressId());

        try {
            // Check cache first
            Optional<GeocodingResult> cachedResult = geocodingCacheService.get(event.getFullAddress());
            
            GeocodingResult result;
            if (cachedResult.isPresent()) {
                log.info("Using cached geocoding result for address: {}", event.getFullAddress());
                result = cachedResult.get();
            } else {
                // Call geocoding service
                Optional<GeocodingResult> geocodingResult = geocodingService.geocode(event.getFullAddress());
                
                if (geocodingResult.isEmpty()) {
                    log.warn("Geocoding failed for addressId: {}, address: {}", 
                            event.getAddressId(), event.getFullAddress());
                    return;
                }
                
                result = geocodingResult.get();
                
                // Cache the result
                geocodingCacheService.put(event.getFullAddress(), result);
            }

            // Update address with coordinates
            Optional<CustomerAddress> addressOpt = customerAddressRepository.findById(event.getAddressId());
            
            if (addressOpt.isEmpty()) {
                log.warn("Address not found for addressId: {}", event.getAddressId());
                return;
            }

            CustomerAddress address = addressOpt.get();
            address.setLatitude(result.getLatitude());
            address.setLongitude(result.getLongitude());
            customerAddressRepository.save(address);

            log.info("Successfully geocoded addressId: {}, lat: {}, lng: {}", 
                    event.getAddressId(), result.getLatitude(), result.getLongitude());

        } catch (Exception e) {
            log.error("Error processing geocoding request for addressId: {}", 
                    event.getAddressId(), e);
        }
    }
}

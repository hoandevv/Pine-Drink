package com.hoandev.pinedrink.queue.event.geocoding;

import com.hoandev.pinedrink.queue.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a customer address needs geocoding.
 * This event is processed asynchronously to avoid blocking the main request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingRequestEvent implements DomainEvent {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique event identifier.
     */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    /**
     * Event type discriminator.
     */
    @Builder.Default
    private String eventType = "GEOCODING_REQUEST";
    
    /**
     * Timestamp when the event occurred.
     */
    @Builder.Default
    private Instant occurredAt = Instant.now();
    
    /**
     * Source service name.
     */
    @Builder.Default
    private String source = "pine-drink";
    
    /**
     * The ID of the customer address to geocode.
     */
    private String addressId;
    
    /**
     * The full address string to geocode.
     */
    private String fullAddress;
    
    @Override
    public String eventId() {
        return eventId;
    }
    
    @Override
    public String eventType() {
        return eventType;
    }
    
    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
    
    @Override
    public String source() {
        return source;
    }
    
    /**
     * Factory method to create a geocoding request event.
     *
     * @param addressId the address ID
     * @param fullAddress the full address string
     * @return the event
     */
    public static GeocodingRequestEvent of(String addressId, String fullAddress) {
        return GeocodingRequestEvent.builder()
                .addressId(addressId)
                .fullAddress(fullAddress)
                .build();
    }
}

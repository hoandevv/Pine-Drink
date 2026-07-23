package com.hoandev.pinedrink.queue.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base contract for all domain events published through the messaging layer.
 * <p>
 * Every event carries a unique identifier, a type discriminator, a timestamp,
 * and the name of the originating service for traceability.
 */
public interface DomainEvent extends Serializable {
    /** Unique event identifier (UUID). */
    String eventId();
    /** Event type discriminator, e.g. {@code "EMAIL_OTP_VERIFICATION"}. */
    String eventType();
    /** Timestamp of when the event occurred. */
    Instant occurredAt();
    /** Name of the service that published the event. */
    String source();
}

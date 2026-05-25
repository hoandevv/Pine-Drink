package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "ce_pickup_time_slot", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "slot_code"}))
public class PickupTimeSlot extends BaseEntity {

    @Column(name = "slot_code")
    private String slotCode;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_orders")
    private Integer maxOrders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}

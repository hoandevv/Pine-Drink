package com.hoandev.pinedrink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ch_message")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private Account senderAccount;

    @Column(name = "message_type", nullable = false)
    private String messageType = "TEXT";

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
}

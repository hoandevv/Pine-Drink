# Chat Architecture - AI Ready

## Goals

- Keep exactly one active customer support conversation per `customer + branch` pair.
- Let branch staff and managers collaborate in the same conversation.
- Let admins monitor the full system without needing room assignment.
- Keep staff collaboration simple for MVP without room assignment.
- Prepare the message model for future AI and system actors.

## Conversation Model

### Active rule

- One active conversation is reused for the same `customer + branch`.
- `orderId` no longer determines room identity.
- `orderId` can still be attached when a room is first created, but future order context should live at the message level.

### Why this model

- Matches common marketplace and food ordering support flows.
- Avoids duplicate rooms and fragmented history.
- Makes realtime subscription and unread tracking simpler.
- Gives AI a single continuous history per customer-branch relationship.

## Access Rules

- Customer can access their own branch conversation.
- Branch-scoped staff and managers can access any conversation in their branch.
- System-scoped admins can access all conversations.

## MVP Collaboration Model

- Rooms do not track a primary handler.
- Any staff or admin with valid scope can open and reply in the room.
- Backend does not expose room assignment endpoints in MVP.

## Realtime Topics

- Room topic: `/topic/chat.rooms.{roomId}`
- Branch overview topic: `/topic/branches.{branchId}.chat.rooms`

These topic names are aligned with RabbitMQ STOMP broker relay expectations.

## AI-Ready Message Shape

### Current backend additions

- `senderType` is now stored on each chat message.
- Realtime chat payload now includes:
  - `messageId`
  - `senderId`
  - `senderType`
  - `senderName`
  - `messageType`
  - `content`
  - `metadata`
  - `sentAt`

### Current sender type values

- `CUSTOMER`
- `STAFF`
- `ADMIN`

### Planned future sender type values

- `BOT`
- `SYSTEM`

This lets AI join the same conversation history without pretending to be a human account.

## Recommended Next Steps

1. Add message-level order cards using `messageType = ORDER`.
2. Add soft delete for messages instead of hard deletion.
3. Add conversation status values such as `OPEN`, `PENDING`, `RESOLVED`, `CLOSED`.
4. Add explicit bot/system message creation flows.
5. Add a dedicated event path for AI workers:
   - message saved
   - bot reply requested
   - bot reply persisted
   - realtime published

## Notes For Frontend

- Reuse the same room for the same `customer + branch` instead of expecting a new room per order.
- Subscribe to dot-style topics only.
- Realtime message rendering should rely on backend event payloads instead of reloading room history after send.

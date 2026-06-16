# Ozon Chat Local Flow Design

**Goal:** Build a minimal Ozon chat workflow that can be verified locally without relying on the real Ozon chat API.

**Scope:** This slice covers local JSON import of sessions and messages, session/message browsing, unread counts, and local reply audit recording. It does not send messages to Ozon and does not implement read-state sync back to Ozon.

## Design Summary

The chat slice follows the same bounded-context rule already used by Ozon auth, posting, error center, and finance: raw channel-shaped data stays in `db_ozon`, and the UI operates directly on Ozon-native tables instead of forcing the data into an ERP-specific model. Import is driven by local JSON payloads so the feature can be deployed and verified in the current WSL environment without external API dependencies.

The backend exposes four endpoints:

- `POST /ozon/api/v1/chat/import`
- `GET /ozon/api/v1/chat/session/list`
- `GET /ozon/api/v1/chat/message/list`
- `POST /ozon/api/v1/chat/reply/record`

All endpoints continue to use `UserInfoContext` + `OzonAuthAccessService` for shop scoping, matching the existing Ozon modules.

## Data Model

### `t_ozon_chat_session`

Stores session-level summary data for list rendering.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `session_id`
- `customer_name`
- `last_message_text`
- `last_message_at`
- `unread_count`
- `session_status`
- `create_time`
- `update_time`

Rules:

- `auth_id + session_id` acts as the business key.
- `unread_count` is stored directly on the session row so the list page does not need to aggregate messages every time.

### `t_ozon_chat_message`

Stores individual messages.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `session_id`
- `message_id`
- `sender_type`
- `message_text`
- `message_time`
- `read_flag`
- `raw_line_json`
- `create_time`

Rules:

- `auth_id + message_id` is the message idempotency key.
- Re-import updates existing messages instead of duplicating them.

### `t_ozon_chat_reply_audit`

Stores local reply actions only.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `session_id`
- `reply_text`
- `reply_status`
- `operator`
- `create_time`
- `update_time`

Rules:

- This table is a local audit trail.
- No credential material is written here.
- This slice records replies but does not send them externally.

## Import Payload Shape

The local import endpoint accepts:

- `authId`
- `rawContent`

`rawContent` is JSON and should support the following minimal shape:

```json
{
  "sessions": [
    {
      "sessionId": "session-1",
      "customerName": "Buyer A",
      "sessionStatus": "OPEN",
      "messages": [
        {
          "messageId": "msg-1",
          "senderType": "BUYER",
          "messageText": "hello",
          "messageTime": "2026-03-26T10:00:00Z",
          "read": false
        }
      ]
    }
  ]
}
```

Import behavior:

- Invalid JSON fails the whole import.
- Sessions may exist without messages.
- Messages are upserted by `messageId`.
- Session summaries are recalculated from the imported messages.

## UI Structure

Page path: `/ozon/chat`

The page is split into three areas:

1. Import area
2. Session list area
3. Message + reply area

### Import Area

Inputs:

- auth selector
- raw JSON textarea
- import button

### Session List

Columns:

- customer name
- last message text
- last message at
- unread count
- session status

Filters:

- unread only
- keyword

### Message + Reply Area

Shows:

- sorted message timeline
- sender type
- message text
- message time
- local reply box
- local reply audit list (latest few entries is enough)

## Error Handling

- Empty import payload: reject
- Empty reply text: reject
- Missing `sessionId` or `messageId`: reject that import batch
- Unauthorized auth scope: reject via existing auth access service

## Verification Strategy

Verification must include:

- targeted backend tests for import/list/reply audit
- frontend route/build verification
- local deployment verification in WSL using a seeded auth row and a locally injected session token

## Out of Scope

- Real Ozon chat API pull
- Real reply send
- Read-state sync back to Ozon
- WebSocket/polling
- Customer-service assignment workflow

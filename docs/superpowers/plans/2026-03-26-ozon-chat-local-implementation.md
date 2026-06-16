# Ozon Chat Local Flow Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a locally verifiable Ozon chat flow with JSON import, session/message browsing, unread counts, and local reply audit recording.

**Architecture:** Keep chat data fully inside `db_ozon` and expose a thin set of Ozon-native endpoints for import, session list, message list, and reply audit. Reuse the existing Ozon auth scoping, controller error wrapper, and frontend route patterns established by auth, posting, error center, and finance.

**Tech Stack:** Spring Boot 2.6, MyBatis-Plus, MySQL, Vue 3, Vite, Element Plus.

---

## File Structure Map

### Backend

- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/controller/OzonChatController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/mapper/OzonChatSessionMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/mapper/OzonChatMessageMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/mapper/OzonChatReplyAuditMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/dto/OzonChatImportCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/dto/OzonChatMessageQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/dto/OzonChatReplyRecordCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/entity/OzonChatSession.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/entity/OzonChatMessage.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/entity/OzonChatReplyAudit.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/pojo/vo/OzonChatImportResult.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/service/IOzonChatService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/service/impl/OzonChatServiceImpl.java`

### SQL

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_session.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_message.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_reply_audit.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`

### Frontend

- Create: `wimoorui/src/api/ozon/chat/chatApi.js`
- Create: `wimoorui/src/views/ozon/chat/index.vue`
- Modify: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/scripts/check_ozon_entry.mjs`

### Tests

- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/chat/OzonChatSyncTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/chat/OzonChatServiceSpringBeanTests.java`

## Chunk 1: Backend Chat Flow

### Task 1: Implement chat import and query service

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/chat/OzonChatSyncTests.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/...`

- [ ] **Step 1: Write the failing import/list/reply test**

```java
class OzonChatSyncTests {
    @Test
    void importCreatesSessionsMessagesAndReplyAudit() {
        OzonChatImportResult result = service.importMessages(user, command);
        assertEquals(1, result.getSessionCount());
        assertEquals(2, result.getMessageCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonChatSyncTests test
```

Expected: compilation failure because chat package does not exist yet.

- [ ] **Step 3: Add DTOs, entities, and mappers**

Implement:

- `OzonChatImportCommand`
- `OzonChatMessageQuery`
- `OzonChatReplyRecordCommand`
- `OzonChatSession`
- `OzonChatMessage`
- `OzonChatReplyAudit`
- mappers for the three tables

- [ ] **Step 4: Implement service**

Service must support:

- `importMessages`
- `listSessions`
- `listMessages`
- `recordReply`

Rules:

- use `OzonAuthAccessService`
- import fails on invalid JSON
- upsert messages by `messageId`
- update session summary on import
- replies only write audit rows

- [ ] **Step 5: Implement controller**

Add endpoints:

- `POST /ozon/api/v1/chat/import`
- `GET /ozon/api/v1/chat/session/list`
- `GET /ozon/api/v1/chat/message/list`
- `POST /ozon/api/v1/chat/reply/record`

- [ ] **Step 6: Run test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonChatSyncTests test
```

Expected: PASS

## Chunk 2: SQL and Frontend

### Task 2: Add chat tables and page

**Files:**
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_session.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_message.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_reply_audit.sql`
- Create: `wimoorui/src/api/ozon/chat/chatApi.js`
- Create: `wimoorui/src/views/ozon/chat/index.vue`
- Modify: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/scripts/check_ozon_entry.mjs`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`

- [ ] **Step 1: Write the failing route/build presence checks**

Use the existing Ozon entry check:

- add expected route `ozon/chat`
- run the route check before implementing

- [ ] **Step 2: Run the check to verify it fails**

Run:

```bash
cd wimoorui && node scripts/check_ozon_entry.mjs
```

Expected: FAIL because `ozon/chat` is missing.

- [ ] **Step 3: Add SQL files**

Tables should include the fields confirmed in the spec and indexes on:

- `auth_id + session_id`
- `auth_id + message_id`
- `auth_id + unread_count`

- [ ] **Step 4: Add frontend API**

Implement:

- `importMessages`
- `listSessions`
- `listMessages`
- `recordReply`

- [ ] **Step 5: Add frontend page**

Page should contain:

- auth selector
- raw JSON textarea
- import button
- session list with unread filter
- message timeline
- local reply box

- [ ] **Step 6: Add route and menu seeds**

Add:

- `/ozon/chat`
- menu row
- permission rows

- [ ] **Step 7: Re-run the route check**

Run:

```bash
cd wimoorui && node scripts/check_ozon_entry.mjs
```

Expected: PASS

## Chunk 3: Verification

### Task 3: Verify backend, frontend, and local deployment

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/chat/OzonChatServiceSpringBeanTests.java`

- [ ] **Step 1: Add spring bean construction test**

Create a minimal `ApplicationContextRunner` test to ensure `OzonChatServiceImpl` can be instantiated by Spring without constructor ambiguity.

- [ ] **Step 2: Run backend chat tests**

Run:

```bash
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonChatSyncTests,OzonChatServiceSpringBeanTests test
```

Expected: PASS

- [ ] **Step 3: Run frontend checks and build**

Run:

```bash
cd wimoorui && node scripts/check_linux_path_case.mjs
cd wimoorui && node scripts/check_ozon_entry.mjs
cd wimoorui && timeout 600s ./scripts/build_in_linux_fs.sh
```

Expected: PASS

- [ ] **Step 4: Local deployment verification**

Use the same local deployment strategy already validated for finance:

- build `ozon-boot.jar`
- start with local overrides
- seed `db_ozon`
- seed Redis `login_tokens:<token>`
- call:
  - `GET /api/v1/auth/list`
  - `POST /api/v1/chat/import`
  - `GET /api/v1/chat/session/list`
  - `GET /api/v1/chat/message/list`
  - `POST /api/v1/chat/reply/record`

Expected:

- session list returns imported session
- message list returns imported messages
- reply record returns success and creates audit row

Plan complete and saved to `docs/superpowers/plans/2026-03-26-ozon-chat-local-implementation.md`. Please review the spec at [2026-03-26-ozon-chat-local-design.md](/mnt/d/project/wimoor/docs/superpowers/specs/2026-03-26-ozon-chat-local-design.md) and let me know if you want any changes before我开始按这个计划实现。*** End Patch

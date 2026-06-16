# Ozon Product Publish Full Flow Design

**Goal:** Build a real Ozon product listing and publish workflow for mixed-source merchandise, including multi-variant drafts, public and variant-scoped attributes, image management, preview assembly, and real publish task execution.

**Scope:** This slice covers the Ozon product workbench, draft/domain tables, mixed-source override rules, variant modeling, preview generation, and real publish orchestration. It does not include stock/price push, order sync, finance, chat send, ads sync, or generalized cross-platform abstraction beyond the minimum interfaces needed by this Ozon product domain.

## Design Summary

The current Ozon product slice only supports ERP draft import plus manual mapping of `offer_id / ozon_sku / ozon_product_id`. That is sufficient for SKU mapping but not for real publishing. Full publishing requires a separate listing domain that can hold category, attributes, images, variant grouping, operator overrides, publish tasks, and publish results without overloading the existing mapping table.

The recommended shape is:

- keep `t_ozon_product_map` as the ERP SKU to Ozon identity layer
- add a dedicated listing draft domain for authoring and publish orchestration
- keep ERP as the default base source
- allow operator overrides for listing content
- require every variant to bind to exactly one ERP SKU
- assemble publish payloads only on the backend
- separate `save`, `preview`, and `publish` into different API actions

This follows the same bounded-context rule already used by Ozon auth, posting, finance, chat, ads, task center, and error center: Ozon-native listing state lives in `db_ozon`, while ERP remains the source of stable product master data.

## Confirmed Context

### Current Product Slice

The existing product module currently provides:

- ERP SKU draft import
- mapping maintenance
- mapping list view

It does **not** yet provide:

- category selection
- attribute templates
- variant grouping
- image set management
- publish preview
- real publish client calls
- publish task/audit records

### Source-of-Truth Rule

This design is based on the user-confirmed rule:

- ERP provides the base product data
- operators may override listing content in the Ozon workbench
- publish payload assembly uses operator override first, ERP fallback second

### Variant Rule

This design is also based on the user-confirmed rule:

- every Ozon publish variant must bind to one ERP SKU
- there are no operator-only variants without ERP SKU backing in the first version

### Official API Evidence Boundary

The user provided the official Ozon documentation entry:

- `https://docs.ozon.ru/api/seller/zh/#tag/ProductAPI`

Implementation of the request builder must begin with verification of the exact Product API endpoint, request schema, and response schema from the official Product API page or equivalent first-party evidence. This is an explicit implementation gate, not an optional follow-up.

V1 planning is narrowed to the verified contract captured below. Optional or undocumented publish fields are out of scope for the first implementation plan.

### Verified External Contract

The following Ozon Product API facts have been verified by real API calls with intentionally incomplete payloads:

- real publish/import entry:
  - `POST /v3/product/import`
- async task detail query:
  - `POST /v1/product/import/info`
- category tree metadata:
  - `POST /v1/description-category/tree`
- category/type attribute template:
  - `POST /v1/description-category/attribute`

Verified request and response observations:

#### `POST /v3/product/import`

Request shape:

```json
{
  "items": [
    {
      "offer_id": "TEST-OFFER",
      "type_id": 971445087,
      "description_category_id": 200001483
    }
  ]
}
```

Verified behavior:

- `items` must contain between 1 and 1000 items
- `type_id` must be greater than 0
- valid requests return:

```json
{
  "result": {
    "task_id": 4036602972
  }
}
```

#### `POST /v1/product/import/info`

Request shape:

```json
{
  "task_id": 4036602384
}
```

Verified behavior:

- `task_id` must be an integer greater than 0
- result returns per-item publish/import state
- observed response shape:

```json
{
  "result": {
    "items": [
      {
        "offer_id": "TEST-OFFER-NEXT",
        "product_id": 3911142260,
        "status": "imported",
        "errors": [
          {
            "code": "missing_dimension",
            "field": "weight",
            "attribute_id": 0,
            "level": "error",
            "attribute_name": "",
            "message": "..."
          }
        ]
      }
    ],
    "total": 1
  }
}
```

Interpretation rule for v1:

- `status = imported` with empty `errors[]` is treated as publish success
- `status = imported` with non-empty `errors[]` is treated as `PARTIAL`
- missing or zero `product_id` with errors is treated as publish failure

Verified required-field evidence from real task results:

- `description_category_id` is required
- `type_id` is required
- dimensions/weight are required for publishability
- effective price is required for publishability
- category-dependent required attributes are enforced asynchronously in task detail

This means preview validation must cover:

- category
- type
- weight/dimension block
- effective price
- required category attributes

V1 verified publish contract:

- endpoint:
  - `POST /v3/product/import`
- async result polling:
  - `POST /v1/product/import/info`
- verified static request fields per item:
  - `offer_id`
  - `name`
  - `description_category_id`
  - `type_id`
  - `barcode`
  - `images[]`
- verified publishability fields from async validation:
  - price
  - weight
  - dimensions
  - category-required attributes

V1 implementation plan must target this verified contract only. Any additional optional Product API fields remain explicitly out of scope until separately verified.

Verified request-field mapping decisions for v1:

- title maps to top-level `name`
- barcode maps to top-level `barcode`
- images map to top-level `images[]`
- brand maps to `attributes[]` using the remote attribute id returned by metadata
- description maps to `attributes[]` using the remote attribute id returned by metadata when such attribute exists for the selected category/type
- other category-specific business content maps to `attributes[]`

## Data Model

### Keep: `t_ozon_product_map`

This table remains the SKU mapping layer only.

Responsibilities:

- store ERP SKU to Ozon identifiers
- store latest sync result
- receive successful publish result writeback

It must **not** become the full listing draft table.

### Add: `t_ozon_listing_draft`

One row represents one publishable Ozon product group.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `draft_name`
- `description_category_id`
- `description_category_name`
- `type_id`
- `type_name`
- `title_source_value`
- `title_override_value`
- `brand_source_value`
- `brand_override_value`
- `description_source_value`
- `description_override_value`
- `status`
- `last_preview_status`
- `last_preview_message`
- `last_publish_task_id`
- `create_time`
- `update_time`

Rules:

- one draft may contain one or many variants
- source/original values are persisted separately from operator override values
- effective publish value is resolved at assembly time

### Add: `t_ozon_listing_variant`

One row represents one publish variant.

Fields:

- `id`
- `draft_id`
- `auth_id`
- `shop_id`
- `material_sku`
- `material_name`
- `offer_id_override`
- `barcode_override`
- `price_override`
- `weight_source_value`
- `weight_override_value`
- `length_source_value`
- `length_override_value`
- `width_source_value`
- `width_override_value`
- `height_source_value`
- `height_override_value`
- `variant_label`
- `status`
- `last_sync_status`
- `last_sync_message`
- `create_time`
- `update_time`

Rules:

- each variant must bind to one ERP SKU
- `draft_id + material_sku` acts as the business uniqueness rule
- one draft may have many variants, but one variant belongs to one draft only
- `effective_offer_id` is resolved in this order:
  - `offer_id_override`
  - existing `t_ozon_product_map.ozon_offer_id`
  - ERP `material_sku`
- `effective_offer_id` is mandatory at publish time
- `effective_price` is resolved from operator override first, ERP fallback second, and is mandatory at publish time
- `barcode` is optional by default and becomes mandatory only if remote category metadata marks it required
- dimensions and weight are resolved from operator override first, ERP fallback second
- effective `weight`, `length`, `width`, and `height` are mandatory at publish time

### Add: `t_ozon_listing_attribute`

Stores both common and variant-specific attributes.

Fields:

- `id`
- `draft_id`
- `variant_id`
- `auth_id`
- `shop_id`
- `scope`
- `attribute_id`
- `attribute_name`
- `attribute_value_json`
- `required_flag`
- `create_time`
- `update_time`

Rules:

- `scope = COMMON` means the attribute belongs to the draft as a whole
- `scope = VARIANT` means the attribute belongs to one specific variant
- when `scope = COMMON`, `variant_id` is null
- when `scope = VARIANT`, `variant_id` is required

### Add: `t_ozon_listing_image`

Stores public and variant-scoped images.

Fields:

- `id`
- `draft_id`
- `variant_id`
- `auth_id`
- `shop_id`
- `scope`
- `source`
- `image_url`
- `sort_order`
- `is_primary`
- `create_time`
- `update_time`

Rules:

- `scope = GROUP` means the image is public to the listing group
- `scope = VARIANT` means the image belongs only to one variant
- `source = ERP | OPERATOR`
- operator images override ERP images only for the same scope target

### Add: `t_ozon_listing_publish_task`

Stores real publish attempts.

Fields:

- `id`
- `draft_id`
- `auth_id`
- `shop_id`
- `task_status`
- `request_payload_json`
- `response_payload_json`
- `remote_task_id`
- `error_message`
- `operator`
- `create_time`
- `update_time`

Rules:

- every real publish creates one task row
- preview never writes a publish task row
- request/response snapshots are retained for troubleshooting

## Metadata Boundary

Category tree and attribute template data should not be stored in the core listing tables above.

Recommended v1 boundary:

- fetch category and attribute metadata through an Ozon-specific metadata client
- cache metadata outside the listing draft tables
- keep listing tables focused on operator choices, not remote catalog definitions
- use a bounded server-side cache owned by the product domain, not by the frontend

This keeps the draft model stable even if Ozon category metadata changes.

Authoritative local metadata API paths for v1:

- `GET /api/v1/product/category/tree`
- `GET /api/v1/product/category/template`

Recommended metadata endpoints:

- `GET /api/v1/product/category/tree`
- `GET /api/v1/product/category/template`

`GET /api/v1/product/category/tree` request:

- `authId`
- optional `keyword`

`GET /api/v1/product/category/tree` response:

- `categories[]`
  - `descriptionCategoryId`
  - `descriptionCategoryName`
  - `hasChildren`
  - `leaf`
  - optional `types[]`
    - `typeId`
    - `typeName`

`GET /api/v1/product/category/template` request:

- `authId`
- `descriptionCategoryId`
- `typeId`

`GET /api/v1/product/category/template` response:

- `descriptionCategoryId`
- `descriptionCategoryName`
- `typeId`
- `typeName`
- `commonAttributes[]`
- `variantAttributes[]`
- `requiredImageCount`
- `requiresBarcode`

Cache strategy:

- cache key is scoped by `authId + descriptionCategoryId + typeId`
- default TTL is 6 hours
- preview and publish use cached metadata if present
- on cache miss, backend fetches remote metadata synchronously
- on remote metadata failure:
  - return stale cache if available
  - otherwise reject preview/publish

Verified remote metadata call shapes:

- `POST /v1/description-category/tree`
  - empty `{}` body is accepted
  - response contains nested `description_category_id`, `category_name`, `children[]`, `type_name`, and `type_id`
- `POST /v1/description-category/attribute`
  - request uses:

```json
{
  "description_category_id": 200001483,
  "type_id": 971445087
}
```

  - response is an attribute array containing fields such as:
    - `id`
    - `name`
    - `description`
    - `type`
    - `is_aspect`
    - `is_collection`
    - `is_required`
    - `dictionary_id`

Attribute split rule for v1:

- `variantAttributes[]` are attributes where `is_aspect = true`
- `commonAttributes[]` are attributes where `is_aspect = false`

This split is deterministic and owned by the backend metadata adapter.

Normalized attribute DTO contract for v1:

```json
{
  "attributeId": 85,
  "attributeName": "Бренд",
  "mode": "TEXT",
  "values": [
    {
      "text": "Brand Name"
    }
  ]
}
```

Supported modes:

- `TEXT`
- `DICT`
- `MULTI_TEXT`
- `MULTI_DICT`

Free-text example:

```json
{
  "attributeId": 4182,
  "attributeName": "Автор на обложке",
  "mode": "TEXT",
  "values": [{"text": "Author A"}]
}
```

Dictionary-backed example:

```json
{
  "attributeId": 23273,
  "attributeName": "Направление",
  "mode": "DICT",
  "values": [{"dictionaryValueId": 123456, "text": "Учебная литература"}]
}
```

Multi-value dictionary-backed example:

```json
{
  "attributeId": 4149,
  "attributeName": "Рекомендуемый возраст ребёнка",
  "mode": "MULTI_DICT",
  "values": [
    {"dictionaryValueId": 1, "text": "6+"},
    {"dictionaryValueId": 2, "text": "12+"}
  ]
}
```

## Backend Units

### `OzonProductMapService`

Keeps current responsibilities:

- draft import from ERP SKU
- mapping save
- mapping list

No real publish logic should be added here beyond result writeback helpers.

### `OzonListingDraftService`

Responsibilities:

- create or update draft header
- create or update variants
- create or update common attributes
- create or update variant attributes
- create or update image sets
- resolve operator override precedence

### `OzonListingPreviewService`

Responsibilities:

- load draft + variants + attributes + images + stored source snapshots
- build the effective publish snapshot
- run validation before publish
- return missing-field diagnostics

### `OzonProductPublishService`

Responsibilities:

- enforce feature gates
- create publish task row
- call real Ozon Product API
- parse publish result
- write publish task result
- write result back to `t_ozon_product_map`

### `OzonProductApiClient`

Recommended as a separate client under product domain rather than extending the generic seller client with many product-specific publishing methods.

Responsibilities:

- fetch product metadata
- submit real publish requests
- parse raw responses into product-domain DTOs

### `OzonProductMetadataService`

Responsibilities:

- call `OzonProductApiClient` for category tree and attribute template
- own the metadata cache
- normalize remote category and attribute payloads into local `category/tree` and `category/template` responses
- split remote attributes into `commonAttributes[]` and `variantAttributes[]`

## API Surface

The current `/api/v1/product` namespace should be extended with listing-draft APIs.

Recommended endpoints:

- `GET /api/v1/product/list`
- `POST /api/v1/product/importDraft`
- `POST /api/v1/product/map/save`
- `GET /api/v1/product/draft/list`
- `POST /api/v1/product/draft/save`
- `GET /api/v1/product/draft/detail`
- `GET /api/v1/product/category/tree`
- `GET /api/v1/product/category/template`
- `POST /api/v1/product/preview`
- `POST /api/v1/product/publish`
- `GET /api/v1/product/publish/task/detail`

Rules:

- `save` persists authoring state only
- `preview` assembles effective payload and returns validation errors
- `publish` performs the real external publish
- `publish/task/detail` reads the normalized publish task result after asynchronous polling
- `GET /api/v1/product/list` remains the existing mapping list API
- `GET /api/v1/product/draft/list` is the left-rail draft navigation API

Field-level contract for `draft/save`:

- request:
  - `authId`
  - `draftId` optional for update
  - `draftName`
  - `descriptionCategoryId`
  - `descriptionCategoryName`
  - `typeId`
  - `typeName`
  - `titleOverrideValue`
  - `brandOverrideValue`
  - `descriptionOverrideValue`
  - `commonAttributes[]`
  - `commonImages[]`
  - `variants[]`
- response:
  - `draftId`
  - `status`
  - `updatedAt`

Nested DTO example for `draft/save`:

```json
{
  "authId": "auth-1",
  "draftId": "draft-1",
  "draftName": "Books Spring Launch",
  "descriptionCategoryId": 200001483,
  "descriptionCategoryName": "Современные печатные издания (с 2011 г.)",
  "typeId": 971445087,
  "typeName": "Печатная книга",
  "titleOverrideValue": "Book Title",
  "brandOverrideValue": "Brand Name",
  "descriptionOverrideValue": "Short description",
  "commonAttributes": [
    {
      "attributeId": 85,
      "attributeName": "Бренд",
      "values": ["Brand Name"]
    }
  ],
  "commonImages": [
    {
      "imageUrl": "https://example.com/group-main.jpg",
      "sortOrder": 1,
      "primary": true,
      "source": "OPERATOR"
    }
  ],
  "variants": [
    {
      "variantId": "variant-1",
      "materialSku": "ERP-SKU-1",
      "variantLabel": "Default",
      "offerIdOverride": "BOOK-001",
      "barcodeOverride": "9781234567890",
      "priceOverride": "99.00",
      "weightOverrideValue": "0.60",
      "lengthOverrideValue": "22.00",
      "widthOverrideValue": "15.00",
      "heightOverrideValue": "3.00",
      "attributes": [
        {
          "attributeId": 4182,
          "attributeName": "Автор на обложке",
          "values": ["Author A"]
        }
      ],
      "images": [
        {
          "imageUrl": "https://example.com/variant-1.jpg",
          "sortOrder": 1,
          "primary": true,
          "source": "OPERATOR"
        }
      ]
    }
  ]
}
```

Mutation semantics for `draft/save`:

- `draft/save` uses full-snapshot replace semantics inside one draft
- `commonAttributes[]`, `commonImages[]`, and `variants[]` replace the previously stored sets for that draft
- nested `attributes[]` and `images[]` inside each variant also use replace semantics
- removal is represented by omission from the submitted full snapshot

Failure cases for `draft/save`:

- invalid auth scope
- duplicate `materialSku` in `variants[]`
- variant without `materialSku`

Field-level contract for `draft/detail`:

- request:
  - `authId`
  - `draftId`
- response:
  - draft header
  - common attributes
  - common images
  - variant rows
  - latest preview summary
  - latest publish task summary

Field-level contract for `draft/list`:

- request:
  - `authId`
  - optional `status`
  - optional `keyword`
- response:
  - `drafts[]`
    - `draftId`
    - `draftName`
    - `descriptionCategoryName`
    - `typeName`
    - `status`
    - `variantCount`
    - `lastPublishAt`

Field-level contract for `importDraft`:

- request:
  - `authId`
  - optional `draftId`
  - optional `draftName`
  - `skus[]`
- response:
  - `draftId`
  - `importedCount`
  - `createdVariantCount`
  - `updatedVariantCount`
  - `skippedSkus[]`

Idempotency semantics for `importDraft`:

- repeated `importDraft` for the same `draftId + materialSku` updates source snapshot fields instead of creating duplicate variants
- omitted existing variants are preserved; `importDraft` is additive/update, not full replace

Failure cases for `importDraft`:

- invalid auth scope
- empty `skus[]`
- ERP material lookup failure
- one or more SKUs not found in ERP do not roll back successful imports; they are returned in `skippedSkus[]`

Field-level contract for `preview`:

- request:
  - `authId`
  - `draftId`
- response:
  - `effectivePayloadSummary`
  - `validationErrors[]`
  - `variantIssues[]`
  - `canPublish`

Nested DTO example for `preview` response:

```json
{
  "effectivePayloadSummary": {
    "draftId": "draft-1",
    "topLevelFields": {
      "offerIdCount": 2,
      "hasImages": true,
      "hasDimensions": true
    },
    "variants": [
      {
        "variantId": "variant-1",
        "materialSku": "ERP-SKU-1",
        "effectiveOfferId": "BOOK-001",
        "effectiveBarcode": "9781234567890",
        "effectivePrice": "99.00",
        "effectiveWeight": "0.60",
        "effectiveDimensions": {
          "depth": "22.00",
          "width": "15.00",
          "height": "3.00"
        },
        "effectiveImageCount": 1
      }
    ]
  },
  "validationErrors": [],
  "variantIssues": [],
  "canPublish": true
}
```

Field-level contract for `publish`:

- request:
  - `authId`
  - `draftId`
- response:
  - `draftId`
  - `localTaskId`
  - `remoteTaskId`
  - `taskStatus`
  - optional `resultSummary`

Field-level contract for `publish/task/detail`:

- request:
  - `authId`
  - `taskId` (`localTaskId`)
- response:
  - `taskStatus`
  - `remoteTaskId`
  - `requestPayloadJson`
  - `responsePayloadJson`
  - `normalizedItems[]`
  - `errorSummary`

Nested DTO example for `publish/task/detail` response:

```json
{
  "taskStatus": "PARTIAL",
  "remoteTaskId": 4036602384,
  "normalizedItems": [
    {
      "offerId": "BOOK-001",
      "productId": 3911142260,
      "remoteStatus": "imported",
      "hasErrors": true,
      "errors": [
        {
          "code": "missing_dimension",
          "field": "weight",
          "attributeId": 0,
          "attributeName": "",
          "message": "..."
        }
      ]
    }
  ],
  "errorSummary": "1 variant has remote validation errors"
}
```

Failure cases for `publish/task/detail`:

- invalid auth scope
- task not found
- remote task lookup network error
- remote task still non-terminal

## Authoring and Publish Flow

### 1. Draft Bootstrap

Operators start from ERP-backed draft import:

- choose one auth/store
- import ERP SKUs
- create one draft group
- assign one or many variants to that group

### 2. Content Completion

Operators then complete:

- description category
- type
- title
- brand
- description
- public attributes
- public images
- variant-specific attributes
- variant-specific images

### 3. Preview Assembly

Backend preview assembles the effective payload:

- ERP base values
- operator overrides
- public attributes
- variant attributes
- public images
- variant images

### 4. Local Validation

Validation runs before any remote call:

- description category selected
- type selected
- required common attributes present
- required variant attributes present
- each variant bound to ERP SKU
- each variant has required publishing identifiers and media

Preview returns:

- effective payload summary
- validation errors
- per-variant error list

### 5. Real Publish

When preview is clean:

- create publish task
- create one summary sync job row with job type `PRODUCT_PUBLISH`
- call real Ozon Product API
- parse response
- mark task `SUCCESS`, `FAILED`, or `PARTIAL`

V1 publish control flow:

- `POST /api/v1/product/publish` creates one local publish task
- backend calls `POST /v3/product/import`
- backend receives `task_id`
- backend stores `remote_task_id` in `t_ozon_listing_publish_task`
- backend polls `POST /v1/product/import/info` every 2 seconds for up to 30 seconds
- if a terminal normalized result is available within that window, `/publish` returns that result directly
- if the remote task is still not terminal at timeout, `/publish` returns `taskStatus = RUNNING`
- frontend then refreshes via `GET /api/v1/product/publish/task/detail`

V1 async completion model:

- there is no background polling worker in v1
- `publish/task/detail` performs on-demand remote polling when the local task is still `RUNNING`
- once a terminal result is observed, `publish/task/detail` persists the normalized result and stops polling

Async failure handling:

- if `/publish` fails before remote `task_id` is obtained:
  - local task becomes `FAILED`
- if remote `task_id` is obtained but polling times out:
  - local task remains `RUNNING`
  - timeout text is stored in task message
- if `publish/task/detail` hits a transient network failure:
  - local task remains `RUNNING`
  - last poll error is appended to task message
- if a task remains `RUNNING` for more than 15 minutes:
  - UI may show retry-eligible warning
  - actual retry is still blocked until operator explicitly re-runs publish

### 6. Result Writeback

On success:

- update draft/variant state
- write `offer_id / ozon_product_id` back into `t_ozon_product_map`
- update `last_sync_status / message / time`

On failure:

- keep draft editable
- retain publish task with request/response payloads
- expose the error to the workbench and task/error center

`PARTIAL` writeback rule for v1:

- successful variants in a partial publish do write back `offer_id / ozon_product_id`
- successful variants move to `PUBLISHED`
- failed variants move to `FAILED`
- draft moves to `PARTIAL`
- whole-draft retry preserves already written successful mappings unless a later successful publish returns different ids for the same variant
- published drafts remain editable; any edit requires a new `draft/save`, which moves the draft back to `READY` or `DRAFT` before republish

## State Machine

### Draft / Variant / Task Transition Table

| Action | Precondition | Draft Status After | Variant Status After | Task Status After |
|---|---|---|---|---|
| `importDraft` | valid auth + ERP SKUs | `DRAFT` | imported rows start as `DRAFT` | none |
| `draft/save` with missing required local fields | existing draft | `DRAFT` | incomplete rows stay `DRAFT` | none |
| `draft/save` with locally complete fields | existing draft | `READY` | complete rows move to `READY` | none |
| `publish` start | draft `READY` and no active task | `PUBLISHING` | participating rows move to `PUBLISHING` | `RUNNING` |
| publish terminal success | remote result without errors | `PUBLISHED` | all rows `PUBLISHED` | `SUCCESS` |
| publish terminal failure | remote result failed | `FAILED` | affected rows `FAILED` | `FAILED` |
| publish terminal partial | remote result mixed/errored | `PARTIAL` | success rows `PUBLISHED`, failed rows `FAILED` | `PARTIAL` |
| save after failure/partial | operator edits draft | `READY` if locally complete else `DRAFT` | per-row reevaluated | no active task change |
| save after published draft edit | operator edits draft | `READY` if locally complete else `DRAFT` | affected rows reevaluated | no active task change |

Variant statuses in v1:

- `DRAFT`
- `READY`
- `PUBLISHING`
- `PUBLISHED`
- `FAILED`

Button availability rules:

- `Preview` is enabled when draft exists
- `Publish` is enabled only when draft status is `READY` and no active `RUNNING` task exists
- `Retry Publish` is the same action as `Publish`, but only after a save operation has moved the draft back to `READY`

Ozon field assembly rule for v1:

- top-level Ozon item fields:
  - `offer_id`
  - `name`
  - `description_category_id`
  - `type_id`
  - `barcode`
  - `price`
  - `weight`
  - `depth`
  - `width`
  - `height`
  - `images[]`
- `brand`, `description`, and category-specific content are assembled into `attributes[]`

## Status and Idempotency Rules

### Draft Status

- `DRAFT`
- `READY`
- `PUBLISHING`
- `PUBLISHED`
- `FAILED`
- `PARTIAL`

Allowed transitions:

- `DRAFT -> READY`
- `READY -> PUBLISHING`
- `PUBLISHING -> PUBLISHED`
- `PUBLISHING -> FAILED`
- `PUBLISHING -> PARTIAL`
- `FAILED -> READY`
- `PARTIAL -> READY`

### Variant Status

- `DRAFT`
- `READY`
- `PUBLISHED`
- `FAILED`
- `PARTIAL`

### Publish Task Status

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `PARTIAL`

### Retry Rule

V1 retry is whole-draft only.

That means:

- no per-variant retry in v1
- after `FAILED` or `PARTIAL`, operator fixes the draft and re-runs full preview + full publish
- retry creates a new publish task row

### Concurrency Rule

Only one active publish is allowed per draft.

Enforcement:

- a draft in `PUBLISHING` cannot accept another publish request
- backend uses `draft_id` as the publish lock key
- duplicate submit while one task is active is rejected

### Idempotency Rule

Preview is side-effect free.

Publish is idempotent at the draft-level task boundary:

- repeated clicks while the same draft is `PUBLISHING` are rejected
- retries after terminal states create new tasks, not task reuse
- successful retry marks the previous product-publish error event `RESOLVED`

## UI Structure

Page path stays under:

- `/ozon/product`

The page uses the user-approved workbench layout rather than a linear wizard.

### Left Rail

Shows:

- auth selector
- draft list
- status filter
- create draft action

### Main Work Area

#### Basic Info Block

Fields:

- title
- description category
- type
- brand
- description

The UI must show ERP source values and operator override values clearly.

Source model rule for v1:

- `*_source_value` fields are ERP snapshot values captured at `importDraft` time
- preview never calls ERP directly
- publish never calls ERP directly
- refreshing ERP source values requires re-running `importDraft` for the target draft/SKUs
- operator override values always win over stored source snapshot values

#### Common Attributes Block

Shows:

- public attributes for the whole listing group
- required-field hints
- category-driven template rendering

#### Common Images Block

Shows:

- ERP-origin images
- operator-added images
- drag-sort / primary-image selection

Image assembly rule for v1:

- if operator group images exist, they replace ERP group images for the draft
- otherwise ERP group images are used as the group image set
- if operator variant images exist for one variant, they replace ERP-derived images for that variant
- otherwise that variant falls back to the effective group image set
- primary image is resolved by explicit `is_primary = true`, otherwise by lowest `sort_order`
- group image replacement is replace-not-merge
- variant image replacement is replace-not-merge

#### Variant Matrix Block

Each row represents one ERP SKU-backed variant.

Columns should include at minimum:

- ERP SKU
- variant label
- offer id override
- barcode
- price
- variant attributes
- variant images
- validation state

#### Preview Block

Shows:

- payload summary
- missing-field diagnostics
- per-variant issues

#### Publish Result Block

Shows:

- latest publish task
- result summary
- request/response inspection
- whole-draft retry entry after terminal failure or partial result

## Validation Rules

At minimum:

- draft must have auth id
- draft must have description category
- draft must have type
- every variant must bind to ERP SKU
- every variant must belong to exactly one draft
- every variant must resolve an `effective_offer_id`
- required common attributes must be present
- required variant attributes must be present
- every variant must have an effective price
- if category metadata marks barcode required, every variant must have an effective barcode
- every variant must have effective `weight`, `length`, `width`, and `height`
- at least one publishable image must exist for the group or per variant according to Ozon rules

## Error Handling

- invalid auth scope: reject using existing auth access rules
- missing ERP SKU material: reject draft bootstrap for that SKU
- category metadata unavailable: reject preview and publish
- publish task remote failure: store full response and mark task failed
- partial publish result: mark task partial and keep draft editable
- failed or partial publish opens one `OzonErrorEvent` with source type `PRODUCT`, keyed by `draft_id`
- successful retry marks the latest open `PRODUCT` error for that draft as `RESOLVED`

## Feature Gates

This full publish slice is a real external write capability and must not be enabled implicitly.

Resolved gate behavior for v1:

- product authoring pages may stay behind `ozon.feature.product`
- preview remains behind `ozon.feature.product`
- real publish action requires a dedicated `ozon.feature.product.write`

Reason:

- product authoring and real publish do not have the same risk profile
- preview is local assembly only
- publish is a real external write

## Task and Error Center Integration

Publish detail is owned by `t_ozon_listing_publish_task`.

Existing centers integrate as follows:

- task center reads the summary row from `t_ozon_sync_job` with job type `PRODUCT_PUBLISH`
- product workbench reads publish detail from `t_ozon_listing_publish_task`
- error center reads `OzonErrorEvent` rows opened for failed or partial product publish

This avoids overloading the global task center with product-specific payload detail while still preserving cross-module visibility.

## ERP Source Mapping

Verified current ERP source entry point:

- `ErpClientOneFeign.findMaterialMapBySku`

V1 source mapping table:

| Listing Field | ERP Source | Fallback Rule |
|---|---|---|
| `materialSku` | `msku` | required |
| `title_source_value` | `name` | required for draft bootstrap |
| `price` source snapshot | `price` | operator override may replace |
| group image source snapshot | `image` | operator images may replace |
| brand source snapshot | none from verified ERP entry | operator fill only |
| description source snapshot | none from verified ERP entry | operator fill only |
| dimensions source snapshot | none from verified ERP entry | operator fill only |
| weight source snapshot | none from verified ERP entry | operator fill only |

This means the first version depends on ERP for:

- SKU identity
- base title
- base price
- base image

and depends on operator entry for:

- brand
- description
- dimensions
- weight
- category-bound attributes

## Auth and Shop Scope

In v1, `authId` is the only request-scoped identifier required by the new APIs.

Rule:

- `authId` uniquely determines `shop_id` through the existing Ozon auth ownership lookup
- `shop_id` remains persisted on rows for indexing and reporting
- frontend does not submit `shopId` on the new product draft APIs

## Verification Strategy

Verification for this slice must include:

- product draft service unit tests
- preview assembly tests for common and variant attributes
- publish service tests for success, failure, and partial result
- controller tests for gate behavior
- smoke test proving new beans are wired
- frontend route/build verification
- local deployment verification with a non-production store only

## Out of Scope

- stock/price synchronization
- order and shipment flow
- chat send
- ads sync
- generalized cross-platform listing engine
- operator-only variants without ERP SKU backing

# Generic Service Test Case Catalog

Use this as a reusable checklist for service-layer unit tests (JUnit + Mockito).
These cases are written in generic language so you can adapt them to other services.

## 1) Read / Profile-like Operations

- [ ] **TC-01: Return current entity details successfully**  
  When the authorized/current entity exists, the service returns a correctly mapped response DTO.

- [ ] **TC-02: Update details when unique field is not in use**  
  If a unique field value (for example, email/username) is not used by another entity, update succeeds and changes are persisted.

- [ ] **TC-03: Update details when unique field belongs to same entity**  
  If the unique field value already belongs to the same current entity, update still succeeds.

- [ ] **TC-04: Reject update when unique field belongs to different entity**  
  If the unique field is already used by another entity, service throws a domain conflict exception and does not save.

## 2) Credential / Sensitive Update Operations

- [ ] **TC-05: Update sensitive credential on valid current value**  
  If old/current credential matches, service encodes new value, updates state flags if any, and persists.

- [ ] **TC-06: Reject sensitive update on invalid current value**  
  If old/current credential does not match, service throws validation/auth exception and does not persist.

## 3) Child Resource Management (Address-like)

- [ ] **TC-07: Set one child record as default when another default exists**  
  Service unsets previous default, sets target as default, persists both changes, and returns updated mapping.

- [ ] **TC-08: Set default when no previous default exists**  
  Service marks selected record as default and persists only required change(s).

- [ ] **TC-09: Fail default update when child record is not found**  
  Service throws not-found exception and performs no save.

- [ ] **TC-10: Reject child update when record belongs to another owner**  
  Service throws access-denied exception and does not persist.

- [ ] **TC-11: Update owned child record successfully**  
  Service updates mutable fields, persists, and returns correctly mapped DTO.

- [ ] **TC-12: Fail child update when record is not found**  
  Service throws not-found exception and does not persist.

- [ ] **TC-13: Return empty list when no child records exist**  
  Service returns an empty collection instead of null.

- [ ] **TC-14: Return mapped child list when records exist**  
  Service maps all relevant child fields correctly in response list.

- [ ] **TC-15: Add new child record successfully**  
  Service creates child object, links it to owner/current entity, persists through aggregate root, and returns mapped DTO.

- [ ] **TC-16: Delete owned child record successfully**  
  Service deletes record and returns success message/result.

- [ ] **TC-17: Fail delete when child record is not found**  
  Service throws not-found exception and does not call delete.

- [ ] **TC-18: Reject delete when child record belongs to another owner**  
  Service throws access-denied exception and does not call delete.

## 4) Association / Favorites-like Operations

- [ ] **TC-19: Add related entity to collection successfully**  
  Service resolves related entity, adds association, persists owner entity, and returns success.

- [ ] **TC-20: Fail add association when related entity is not found**  
  Service throws not-found exception and does not persist owner.

- [ ] **TC-21: Remove related entity from collection successfully**  
  Service removes association, persists owner entity, and returns success.

- [ ] **TC-22: Fail remove association when related entity lookup fails**  
  Service throws not-found exception and does not persist owner.

- [ ] **TC-23: Remove association is idempotent when item not currently associated**  
  Service still returns success and persists expected state when removal target is absent from collection.

- [ ] **TC-24: Return mapped summary list for associated entities**  
  Service maps summary fields (id/name/type/rating-like attributes) accurately.

## 5) Request / Workflow Initiation

- [ ] **TC-25: Create workflow/request entry successfully**  
  Service builds request entity from input + current user context, saves it, and returns response with expected initial workflow fields.

## 6) Subscription / Enrollment-like Operations

- [ ] **TC-26: Create time-bound enrollment successfully**  
  With all prerequisites satisfied, service creates active enrollment, sets status fields, sets start/end dates, saves, and returns success message.

- [ ] **TC-27: Fail enrollment when prerequisite default child data is missing**  
  Service throws not-found exception before dependent lookups/saves.

- [ ] **TC-28: Fail enrollment when target plan/resource is not found**  
  Service throws not-found exception and does not save enrollment.

- [ ] **TC-29: Fail enrollment when active enrollment already exists**  
  Service throws business/access exception and does not save a duplicate active enrollment.

## 7) Filtered Retrieval

- [ ] **TC-30: Return only active records when active filter is true**  
  Service filters repository data by active state and maps nested summary fields correctly.

- [ ] **TC-31: Return only inactive records when active filter is false**  
  Service applies inverse active filter and returns expected subset.

- [ ] **TC-32: Return all records when filter is null/unspecified**  
  Service skips active-state filter and returns full mapped list.

- [ ] **TC-33: Return empty result when repository has no records**  
  Service returns empty collection and handles no-data case safely.

## Optional Reuse Guidance

- Keep test names in `method_condition_expectedResult` style.
- Use helper builders/factories for common entity setup.
- Verify both outcome and interactions (`save`, `delete`, `never`, `times`) for behavior correctness.
- Use `ArgumentCaptor` when created/mutated entity contents matter.


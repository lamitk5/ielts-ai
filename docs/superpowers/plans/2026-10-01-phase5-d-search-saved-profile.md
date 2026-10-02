# Phase 5D — Search, Saved Practice, and Profile Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Connect approved learning search, saved practices, and learner profile completion without regressing the accepted Liquid Gold Search and Typography UI.

**Architecture:** Search combines public approved learning content with only the current learner’s own history through server-side visibility rules. Saved practices pin a published version. Profile/Settings reuses current auth and preference services while keeping roles and password handling server-owned.

**Tech Stack:** Spring Boot, Java 21, JDBC/PostgreSQL, existing `PracticeSearchService`, auth/preferences APIs, React/Vite/React Router, current Search/Settings components, JUnit 5, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-5-learner-journey-design.md`

**Global Constraints:** Execute only after the final accepted Search + Typography HEAD is integrated into the canonical implementation base. Do not redesign Liquid Gold interaction or typography. Never expose Phase 3 drafts, admin data, another learner’s submissions, role fields, or passwords.

**Review Focus:**

1. Search never exposes unapproved Phase 3 content.
2. Search never exposes another user's submission.
3. Bookmarks are owner-scoped.
4. Profile update cannot alter roles or auth privilege.
5. Search remains compatible with the accepted Liquid Gold UI.

### Task 1: Freeze the visual/product prerequisite and search contract

**Files:**
- Create: none
- Inspect: `frontend/src/pages/SearchPage.jsx`, `frontend/src/components/home/HeroSearch.jsx`, `frontend/src/styles/globals.css`
- Modify: only those same Search files if the prerequisite regression test proves that the accepted Search + Typography behavior is missing from the canonical base
- Test: `frontend/src/__tests__/search.test.jsx`, typography/search regression tests from the accepted UI HEAD

**Interfaces:**
- Consumes: final accepted Search + Typography commit and canonical branch integration result
- Produces: confirmed visual base and stable query/result contract before any Phase 5D product work

- [ ] Step 1: Write a failing integration assertion if accepted Search/typography behavior is absent or stale on the implementation base.
- [ ] Step 2: Run the targeted tests and verify RED against the stale base, if applicable.
- [ ] Step 3: Integrate only the approved Search/typography changes or add the smallest compatibility adapter; do not redesign.
- [ ] Step 4: Run search/typography tests and verify GREEN before continuing.
- [ ] Step 5: Commit with `chore(search): establish accepted visual base` only when a real integration change was required.

### Task 2: Expand server search over approved content and owned history

**Files:**
- Modify: `backend/src/main/java/com/ieltsaitutor/search/PracticeSearchService.java`, `PracticeSearchController.java`, `PracticeSearchResult.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogService.java` and 4E history query only for shared visibility contract
- Test: `backend/src/test/java/com/ieltsaitutor/search/PracticeSearchServiceTest.java`, `PracticeSearchSecurityTest.java`

**Interfaces:**
- Consumes: authenticated owner, query, skill/topic/type filters, bounded page/size
- Produces: approved practices/prompts/topics plus only owner’s submissions, with type/route metadata and no admin/generator payload

- [ ] Step 1: Write failing tests for approved visibility, Phase 3 state exclusion, User A/B history isolation, pagination, and exact Liquid Gold query behavior.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement indexed/bounded server search and visibility filters; keep result DTO provider-neutral.
- [ ] Step 4: Run search/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(search): add owner-safe learning search`.

### Task 3: Integrate Search UI and result actions

**Files:**
- Modify: `frontend/src/pages/SearchPage.jsx`, `frontend/src/components/home/HeroSearch.jsx`
- Modify: `frontend/src/services/searchApi.js`
- Test: `frontend/src/__tests__/search.test.jsx`, `frontend/src/__tests__/search-owned-history.test.jsx`

**Interfaces:**
- Consumes: server search DTOs, query params, loading/error/empty states
- Produces: approved content/history results, keyboard-safe navigation, preserved Liquid Gold glow/light trace/reduced-motion behavior

- [ ] Step 1: Write failing UI/API tests for content/history grouping, forbidden result omission, loading/error/empty, keyboard submit, and theme/reduced motion.
- [ ] Step 2: Run targeted tests and verify RED.
- [ ] Step 3: Add data integration without replacing accepted Search visuals or hardcoding private data.
- [ ] Step 4: Run targeted Search/UI regressions and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): integrate learning search results`.

### Task 4: Add owner-scoped Saved Practices persistence and API

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/practice/saved/SavedPractice.java`, repository, service, controller
- Create: additive Flyway migration allocated after the Phase 5C base
- Test: `backend/src/test/java/com/ieltsaitutor/practice/saved/SavedPracticeServiceTest.java`, `SavedPracticeSecurityTest.java`

**Interfaces:**
- Consumes: authenticated owner, active published set/version ID, save/unsave/list commands
- Produces: idempotent owner/version bookmark, paginated list, and unavailable state when publication is no longer visible

- [ ] Step 1: Write failing tests for save/unsave/list idempotency, owner isolation, approved-only visibility, deactivation, and duplicate requests.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement minimal persistence/service/controller and unique owner/version constraint.
- [ ] Step 4: Run repository/service/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(practice): add saved practices`.

### Task 5: Integrate Saved Practices into catalog/search UI

**Files:**
- Create: `frontend/src/services/savedPracticeApi.js`, `frontend/src/pages/SavedPracticesPage.jsx`
- Modify: `frontend/src/pages/PracticeCatalogPage.jsx`, `frontend/src/components/practice/PracticeCatalogCard.jsx`, `frontend/src/pages/SearchPage.jsx`, `frontend/src/App.jsx`
- Test: `frontend/src/__tests__/saved-practices.test.jsx`

**Interfaces:**
- Consumes: saved-practice API, active catalog/search records, auth state
- Produces: save/unsave controls, Saved Practices route, unavailable-version explanation, and keyboard-accessible feedback

- [ ] Step 1: Write failing tests for toggle, reload persistence, guest behavior, deactivated content, loading/error, and no fake entries.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement minimal UI integration while retaining accepted visual treatment.
- [ ] Step 4: Run catalog/search/full adjacent tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add saved practice actions`.

### Task 6: Complete learner profile/settings through existing auth/preferences

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/profile/LearnerProfile.java`, `LearnerProfileRepository.java`, `LearnerProfileService.java`, `LearnerProfileController.java`; `frontend/src/services/profileApi.js`; `frontend/src/pages/ProfilePage.jsx`
- Modify: `backend/src/main/java/com/ieltsaitutor/auth/AuthUser.java` only if a read-only display identity is required, `backend/src/main/java/com/ieltsaitutor/preferences/UserPreferencesService.java`, `backend/src/main/java/com/ieltsaitutor/preferences/UserPreferencesController.java`, `frontend/src/components/settings/SettingsDrawer.jsx`, `frontend/src/features/preferences/PreferenceProvider.jsx`, `frontend/src/App.jsx`
- Test: `backend/src/test/java/com/ieltsaitutor/profile/LearnerProfileServiceTest.java`, `LearnerProfileSecurityTest.java`, `frontend/src/__tests__/preferences-persistence.test.jsx`, `frontend/src/__tests__/settings-drawer.test.jsx`, `frontend/src/__tests__/profile.test.jsx`

**Interfaces:**
- Consumes: authenticated profile/goal/preferences and current password/logout APIs
- Produces: name/avatar reference, target band/date/daily goal/schedule/Tutor preferences, theme, password change, logout; role remains immutable to learner input

- [ ] Step 1: Write failing tests for allowed fields, role/privilege immutability, owner isolation, persistence/reload, password error handling, and logout.
- [ ] Step 2: Run targeted tests and verify RED.
- [ ] Step 3: Reuse current auth/profile/preferences services and add only missing bounded profile fields; do not duplicate auth tables.
- [ ] Step 4: Run backend/frontend settings/profile tests and verify GREEN.
- [ ] Step 5: Commit with `feat(profile): complete learner profile settings`.

### Task 7: Verify Search/Saved/Profile integration and visual compatibility

**Files:**
- Modify: only 5D files required by acceptance failures
- Test: `backend/src/test/java/com/ieltsaitutor/acceptance/SearchSavedProfileAcceptanceTest.java`, `frontend/src/__tests__/search.test.jsx`, `saved-practices.test.jsx`, `preferences-persistence.test.jsx`

**Interfaces:**
- Consumes: accepted Search + Typography base, approved catalog, two learners, admin fixture
- Produces: secure search/history/bookmark/profile flow with unchanged Liquid Gold interaction, responsive behavior, reduced motion, and no data leakage

- [ ] Step 1: Write failing cross-module tests for forbidden search data, owner bookmarks, role protection, reload persistence, and visual behavior hooks.
- [ ] Step 2: Run acceptance tests and verify RED.
- [ ] Step 3: Make minimal integration fixes only.
- [ ] Step 4: Run full frontend/backend verification and diff checks; verify GREEN.
- [ ] Step 5: Commit with `test(phase5d): verify search saved profile integration`.

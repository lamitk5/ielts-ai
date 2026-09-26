# Smart IELTS Learning Experience — UI/UX Phase 1 Design Spec

**Date:** 2026-09-26  
**Status:** Design only — awaiting human review  
**Repository:** \`ielts-ai-tutor\`  
**Scope:** Product shell, personalized learning UX, workspace interaction, motivation, and speaking experience

## 1. Purpose

This document defines the Phase 1 UI/UX direction for the Smart IELTS Learning Experience. It turns the existing Academic Luxury homepage and practice flows into a coherent authenticated learning environment without implementing product code, database migrations, packages, provider integrations, or future AI generation workflows.

The intended learner should be able to understand where to continue, choose a skill, work inside an appropriate study workspace, ask the Tutor for contextual help, and see meaningful progress without being exposed to fake scores, fake speech analysis, vendor-specific AI payloads, or administrative implementation details.

The primary success measure is not the number of widgets on the screen. It is a calm, trustworthy learning flow in which every prominent action has a clear next step, every personalized datum has an ownership boundary, and the interface remains useful while AI or media capabilities are unavailable.

This is a design specification only. It deliberately stops before an implementation plan. No product code, migration, dependency, or runtime behavior is changed by this document.

## 2. Current UI Baseline

The repository already provides a strong starting point:

- The default visual language is Academic Luxury: navy surfaces, restrained gold accents, Playfair Display headings, Inter body/UI text, glass cards, editorial spacing, and reduced-motion handling.
- The homepage already has Hero, four equal skill cards, guest/member progress presentation, Tutor preview, final CTA, and a floating Tutor entry point.
- Reading and Listening use the existing practice catalog and server-owned attempt validation. The current practice page is a single-column question flow rather than a split learning workspace.
- Writing has task selection, prompt presentation, a text area, word count, submission, history, and an estimate disclaimer. It does not yet have autosave, a timer, a rich editor, or a split prompt/editor layout.
- Speaking has Part 1/2/3 prompt selection, text input, save/history, and an explicit text-only/STT-not-configured boundary. It must not imply that microphone capture, transcript generation, pronunciation analysis, or a band estimate already exists.
- The floating Tutor already has a keyboard-accessible dialog, bounded visible history, loading skeleton, timeout/error handling, grounding/source display, and trusted exercise context assembled by the server.
- Auth is represented in the frontend by \`AuthProvider\` and in the backend by server-side principals and ownership checks. Personalized AI must remain an authenticated capability even if a frontend gate is bypassed.
- The member homepage has four-skill progress, a radar chart, countdown, and mistakes area. Progress and mistakes should be supplied by the learning intelligence model rather than by a second UI-specific analytics system.
- The backend already has user-scoped attempts, activities, writing submissions, speaking attempts, trusted Tutor context resolution, deterministic Tutor tools, RAG governance, and the provider-neutral \`/api/ai/chat\` boundary.

The design therefore extends existing boundaries instead of introducing a second application shell, second Tutor contract, second learning-event stream, or duplicate scoring model.

## 3. Design Goals

1. Make the next meaningful learning action obvious within one scan.
2. Preserve Academic Luxury as a learning environment rather than turning the product into a generic SaaS dashboard.
3. Make personalization feel useful while keeping guest state honest and account state isolated.
4. Give each skill a first-class workspace appropriate to its learning task.
5. Make Tutor context visible, trusted, and bounded without exposing provider details.
6. Make asynchronous work truthful: loading, processing, retrying, and unavailable states must be understandable.
7. Support a calm desktop composition and a practical mobile flow at 375px, 768px, 1024px, and 1440px.
8. Treat accessibility, keyboard operation, reduced motion, ownership, and stale-data safety as core design requirements.

## 4. Non-Goals

This phase does not implement or design a commitment to:

- Real authentication changes beyond the existing auth boundary.
- A real LLM, RAG, STT, pronunciation, or provider integration.
- IELTS official affiliation, certified scores, or unsupported band claims.
- A full exam simulator, timed exam engine, or complete question bank.
- A 3D engine, game world, social feed, leaderboard, or reward economy.
- Persistent AI conversation memory or autonomous learning decisions.
- Automatic public content generation, publication, or admin approval workflows.
- A replacement for the AI Phase 2 adaptive learning model.

## 5. Design Principles

### 5.1 Academic Luxury, not decorative luxury

Use hierarchy, typography, contrast, and considered whitespace to signal quality. Gold marks focus and progress; it does not turn every component into a highlighted control. Surfaces should recede behind the learner's task.

### 5.2 One primary action per view

Each major surface has one recommended next action. Secondary actions remain visible but visually quieter. A Tutor response can suggest a next action, but the app owns the primary navigation and does not allow generated text to create arbitrary controls.

### 5.3 Evidence before confidence

An estimate is labeled as an estimate. An unavailable capability is labeled as unavailable. A loading state describes work that is actually known to be happening; it never simulates hidden provider stages merely to look responsive.

### 5.4 Context without surveillance

The learner can see why Tutor context is attached to a request, but raw internal payloads, provider names, answer keys, ownership identifiers, and implementation metadata are never placed in the UI.

### 5.5 Reversible, recoverable learning

Drafts, layout choices, and session position should be recoverable where safe. A stale AI exchange must not silently restore as if it were current exercise truth.

## 6. Information Architecture

The main navigation remains intentionally small:

| Surface | Primary job | Primary action |
| --- | --- | --- |
| Home / dashboard | Decide what to do next | Continue learning |
| Skill practice | Complete a focused learning activity | Answer, write, or speak |
| Search | Find a relevant practice item | Open result |
| Tutor | Ask about the current trusted context | Send question |
| Assessment | Establish a starting point | Start assessment |
| Settings | Control presentation and learning preferences | Save preference |

The authenticated home hierarchy is:

1. Continue learning / Today's Focus.
2. Four-skill progress.
3. Roadmap and next milestone.
4. Common mistakes or recurring patterns.
5. Streak and activity summary.

There is one prominent CTA in the dashboard's first viewport. Secondary cards do not compete with it through equal gold buttons.

The guest homepage retains the assessment CTA and neutral capability preview. It does not show personal band data, exam dates, mistakes, streaks, or private recommendations.

## 7. Theme and Settings System

### 7.1 Settings drawer

Settings opens from a clearly labeled button in the navigation shell. On desktop it is a right-side drawer with a scrim. On mobile it becomes a full-height panel or bottom-sheet-style page with a persistent close control. Both variants:

- use \`role="dialog"\`, an accessible name, focus containment, Escape close, backdrop close where safe, and focus return to the opener;
- keep the primary content visible behind a dimmed scrim rather than navigating away;
- show a compact preview of selected theme, accent, typography, density, and motion choices;
- save changes intentionally with an explicit “Lưu thay đổi” action or clearly documented immediate-save behavior;
- expose “Khôi phục mặc định” with a confirmation step because it affects multiple preferences.

### 7.2 Appearance

Appearance controls are:

- Theme: \`Sáng\`, \`Tối\`, \`Theo hệ thống\`.
- Accent: \`Gold\`, \`Sapphire\`, \`Emerald\`, \`Burgundy\`, \`Violet\`, \`Slate\`.
- Typography scale: \`Nhỏ\`, \`Mặc định\`, \`Lớn\`.
- Density: \`Thoáng\`, \`Mặc định\`, \`Gọn\`.
- Motion: \`Theo hệ thống\`, \`Giảm chuyển động\`, \`Cho phép chuyển động\`.

Academic Luxury remains the design system across all presets. Accent presets change a complete token set—focus ring, link, selected state, border emphasis, primary action, and chart emphasis—not just a single highlight color. Every preset must pass contrast checks for text, controls, focus indicators, and selected states. A custom color picker is deliberately deferred to avoid uncontrolled contrast combinations.

The system preference is the default for theme and motion. An explicit learner choice overrides it. If the learner has OS-level reduced motion enabled and selects “Cho phép chuyển động,” the UI must still honor the operating-system safety default for essential motion and preserve a non-animated equivalent.

### 7.3 Learning preferences

The same drawer can contain learning preferences that affect future presentation, not truth:

- proactive Tutor suggestions on/off;
- show cross-highlighting on/off;
- default practice timer visibility on/off;
- preferred split ratio for Reading and Writing;
- preferred language for UI copy when localization is later introduced.

These controls do not change server scoring, answer keys, evidence, or generated assessment content.

### 7.4 Token architecture

The implementation should use semantic CSS tokens:

\`\`\`text
surface/background/text/muted
accent/accent-strong/accent-soft
border/border-strong/focus
success/warning/danger/info
shadow/radius/space/type-scale
\`\`\`

Components consume semantic tokens rather than directly branching on an accent name. Charts and status labels receive the same semantic state tokens so a color preset cannot create an isolated visual language.

## 8. Preference Persistence

### 8.1 Guest persistence

Guest preferences are stored in a versioned local-storage record such as \`ielts-ai-tutor.preferences.v1\`. It contains presentation and non-sensitive learning preferences only. It must not contain:

- access tokens, refresh tokens, or credentials;
- private Tutor history;
- source text, uploads, drafts, answer keys, scores, or user IDs;
- a copy of authenticated learning events.

Malformed records are discarded and replaced with defaults. Reads and writes are guarded so storage failure does not prevent the application from rendering.

### 8.2 Authenticated persistence

Authenticated preferences are server-owned through a versioned \`GET/PUT /api/user/preferences\` contract, with a local cache namespaced by stable account identity. The server response is authoritative after login and after every successful save. The local cache is an optimistic rendering aid, never the source of truth for authorization.

On login:

1. Render safe defaults or the last non-sensitive cache while preferences load.
2. Fetch server preferences.
3. Replace the cache with the server record and apply it to the shell.
4. Do not merge guest private data into the account.

Guest visual preferences may be offered as a deliberate, explicit “Áp dụng thiết lập khách” choice later; they must never be silently copied into a user account. The initial ruling is server wins and no silent merge.

On logout, account-scoped preference cache, Tutor messages, uploads, drafts, and session snapshots are removed from the active client store. Public UI defaults may remain. A new account must never see the prior account's state through a shared browser tab or stale in-memory provider.

### 8.3 Conflict and failure behavior

Preferences use a monotonically increasing \`version\` or \`updatedAt\` value. A stale update returns a conflict that the UI resolves by refetching server state and showing a small, non-blocking notice. If saving fails, retain the last saved value and explain that the current change was not persisted; do not claim success.

## 9. Authentication and AI Access

The frontend \`AuthGate\` is a UX boundary: it explains why authentication is needed, preserves a safe return path, and routes to login/register. It is not a security boundary. Every personalized Tutor request, upload, preference read/write, draft read/write, and account learning endpoint must be enforced by the backend using the authenticated principal and ownership checks.

The product rule for this phase is:

- Guest users can browse public practice descriptions and assessment entry points.
- Personalized AI Tutor functionality requires authentication.
- A guest may see a neutral Tutor preview or an unavailable explanation, but the client must not send private context or imply that a real personalized conversation occurred.
- Backend responses use provider-neutral auth/error states; they do not expose provider names, raw exceptions, or credentials.

The existing \`/api/ai/chat\` contract remains the canonical entry point. The UI depends on normalized status, message, grounding, sources, references, and optional working state—not Groq, Cloudflare, Gemini, or any vendor response shape.

## 10. AI Tutor Shell

The current floating Tutor becomes a reusable shell with these display states:

1. \`closed\`: floating button with an accessible name and a short context-aware label when appropriate.
2. \`compact\`: small panel for a quick question on desktop.
3. \`standard\`: default desktop panel with messages, context badge, actions, and composer.
4. \`expanded\`: large workspace overlay when a response or references require more reading room.
5. \`fullscreen\`: explicit user-selected desktop mode for deep Tutor reading.
6. \`mobile\`: viewport-safe full-height or near-full-height panel with safe-area padding and no clipped composer.

The shell always provides:

- title and close button;
- trusted context badge, for example \`Đang xem: Reading · Passage 2 · Câu 7\`;
- clear “Xóa ngữ cảnh” or “Hỏi chung” action when the current context can be detached;
- message list with polite live-region behavior that does not repeatedly announce the whole history;
- quick contextual actions that are app-owned and deterministic;
- composer with label, send, disabled, retry, cancel, and timeout states;
- source chips and references when returned by the normalized contract;
- an insufficient-context explanation that tells the learner what can be asked next.

The context badge is derived from trusted app state and server-resolved identifiers. AI text cannot rewrite it. If the learner navigates away, the shell marks the old context stale and offers a new request rather than silently attaching the old reference to the new skill.

The Tutor panel uses \`max-height: min(42rem, calc(100dvh - 1.5rem))\`-style constraints, an independently scrollable message body, and a composer that remains visible. Mobile uses \`100dvh\`, safe-area insets, and a fixed or sticky composer inside the panel. No desktop or laptop-height viewport may clip send/cancel controls.

## 11. File Attachment UX

Attachments are a staged shell for authenticated Tutor use. They are not a shortcut for arbitrary model context.

Supported initial types are an explicit allowlist such as PDF, DOCX, and TXT. The maximum size is configurable and shown before selection. The UI validates extension and MIME, displays filename/size/type, and lets the learner remove a file before send. It does not inspect or render untrusted HTML in the Tutor panel.

The lifecycle is:

\`SELECTED → UPLOADING → UPLOADED → PROCESSING → READY\`

with terminal states \`FAILED\`, \`REMOVED\`, and \`EXPIRED\`.

The composer shows progress, cancellation where supported, retry for a failed upload, and a readable error. Sending a question while an attachment is processing is either disabled with an explanation or explicitly sends without that attachment; it must never imply that an unready file was used.

The proposed authenticated contract is \`POST /api/ai/attachments\` with multipart upload and a normalized attachment result. Ownership is tied to the authenticated user. The server—not the browser—enforces size, MIME, malware/content checks when available, retention, and authorization. Provider API keys never reach the client. Phase 1 designs the interaction boundary; it does not implement extraction, indexing, or generation.

## 12. Reusable Split Learning Workspace

\`SplitLearningWorkspace\` is the shared layout primitive for Reading and Writing. It owns layout mechanics, not skill-specific answer logic.

### 12.1 Desktop layout

At 1024px and above, the workspace has two independently scrollable panes and a divider:

- Reading: passage left, questions right.
- Writing: prompt/context left, editor right.

The default ratio is 40/60 for Reading and 40/60 for Writing, with supported presets 40/60, 50/50, and 60/40. The layout uses CSS grid or flex with min-width constraints and a visually clear divider handle. The divider cannot collapse either pane below a readable minimum.

### 12.2 Divider interaction

The divider is keyboard operable with \`role="separator"\`, \`aria-valuenow\`, \`aria-valuemin\`, \`aria-valuemax\`, and a label. Arrow keys adjust by a small increment, Home/End move to the minimum/maximum preset, and a shortcut or button cycles 40/50/60. Pointer dragging is enhanced interaction, never the only route.

The ratio is saved to the appropriate user preference for authenticated users and the guest preference record for guests. It is restored only when the workspace and viewport support split mode.

### 12.3 Mobile layout

Below the desktop threshold, panes do not remain compressed side by side. The default is a tabbed mobile workspace with \`Nội dung\` and \`Câu hỏi\`/\`Bài viết\` tabs, preserving scroll position per pane. A stacked mode may be used for short prompt content, but the learner must not lose the active pane or unsaved editor state.

The active tab is a real button with selected state, keyboard operation, and an accessible relationship to its panel. On mobile, the Tutor opens as an overlay and never pushes the core editor offscreen without a visible return path.

## 13. Reading Workspace

Reading is a two-pane evidence workspace:

- Left pane: passage title, paragraphs, paragraph identifiers, optional vocabulary/evidence rail, and selection-safe text.
- Right pane: question progress, current question, answer controls, flag control, and navigation.

Question state is explicit: \`unanswered\`, \`answered\`, \`flagged\`, \`current\`, and \`reviewed\`. These states use text, icons, and structure in addition to color. The question rail provides a compact overview without becoming a dashboard.

Cross-highlighting can link a Tutor explanation to a paragraph, sentence, or evidence range. Clicking a valid reference scrolls and focuses the relevant content. Hover is a preview only; keyboard focus and activation provide the same result. If the passage changes or a reference is stale, the UI says that the reference is no longer available and leaves the learner in the current question.

The server remains authoritative for questions, answer keys, scoring, and attempt ownership. The client submits only allowed answer choices and identifiers already supported by the practice contract.

## 14. Writing Workspace

Writing uses the same split primitive:

- Left pane: task type, prompt, constraints, optional planning checklist, and visible task context.
- Right pane: editor, word count, autosave status, timer visibility, submit action, and optional Tutor entry.

The editor must remain a plain, accessible text editing surface unless a future approved requirement justifies a richer editor. Formatting controls cannot imply that the submitted IELTS response is being scored by an official service.

Autosave, if implemented, saves a bounded draft owned by the user. It shows \`Đã lưu\`, \`Đang lưu\`, \`Chưa lưu\`, or \`Không thể lưu\` and never blocks typing on a network round trip. Debouncing and versioning prevent stale writes from overwriting newer text. A draft excludes AI conversation context, answer keys, hidden test content, and credentials.

Timers are opt-in or clearly configurable. A timer is a study aid, not an official exam timer unless the product later implements and labels that mode. Pausing, expiration, or hiding the timer must not delete a draft.

Tutor actions may reference task type, prompt ID, and the learner's current draft only through a trusted server contract. The Tutor must not receive an arbitrary client-created score or band.

## 15. Structured Tutor References

Tutor responses use structured references rather than natural-language DOM instructions. A normalized response may contain:

\`\`\`text
references: [
  {
    referenceType: "READING_PARAGRAPH" | "READING_SENTENCE" |
      "READING_QUESTION" | "WRITING_PROMPT" | "WRITING_DRAFT" |
      "LISTENING_ITEM" | "SPEAKING_PROMPT" | "LEARNING_ISSUE",
    targetId: stable application identifier,
    questionId: optional stable question identifier,
    paragraphId: optional stable paragraph identifier,
    sentenceId: optional stable sentence identifier,
    startOffset: optional bounded text offset,
    endOffset: optional bounded text offset,
    evidenceId: optional server-issued evidence identifier,
    severity: optional "INFO" | "TIP" | "WARNING",
    label: short trusted display label
  }
]
\`\`\`

The actual API may omit fields that are not relevant. The important rules are:

- references are validated against the current trusted workspace model;
- the client never receives CSS selectors, XPath, arbitrary HTML, executable code, or provider-native function payloads;
- \`targetId\` is a stable app identifier, not a generated DOM node ID;
- unknown or stale references are ignored safely and surfaced as a non-blocking status when useful;
- labels are sanitized text and do not become markup;
- a reference cannot cross user ownership boundaries.

This keeps the frontend vendor-neutral and allows provider changes without redesigning the UI contract.

## 16. Cross-Highlighting

Cross-highlighting is a shared interaction layer used by Reading first and extended to Writing, Listening, Speaking, and learning issues.

Interaction model:

1. Hovering a valid reference previews the target with a subtle outline.
2. Focusing or activating the reference persists the highlight, scrolls the target into view, and moves focus when safe.
3. The target exposes a non-color state such as an icon, label, outline, or \`aria-describedby\` relationship.
4. Escape clears the persistent highlight.
5. Navigating to a different exercise clears references that no longer resolve.

The target registry is supplied by the workspace, not discovered by querying arbitrary DOM. A \`TutorReferenceResolver\` returns either a known target or a safe “reference unavailable” result. Stale references must never throw, steal focus, or reveal hidden exercise data.

## 17. Dynamic AI Working States

The interface distinguishes known request state from inferred progress:

- \`IDLE\`
- \`PREPARING\`
- \`PROCESSING\`
- \`FINALIZING\`
- \`ANSWERED\`
- \`INSUFFICIENT_CONTEXT\`
- \`TIMEOUT\`
- \`RATE_LIMITED\`
- \`UNAVAILABLE\`
- \`CANCELLED\`
- \`ERROR\`

\`PREPARING\`, \`PROCESSING\`, and \`FINALIZING\` are displayed as named stages only when the normalized backend contract supplies a truthful working state. If the backend only reports that a request is in progress, show one honest message such as “Đang chuẩn bị phản hồi…” with a skeleton and elapsed-time-aware hint. Never show a fake sequence of completed stages.

Every non-terminal state has a settlement path:

- timeout explains what happened and offers retry;
- 429 offers a calm retry-later message and does not spam requests;
- provider unavailable offers deterministic fallback or a clear unavailable state;
- cancel stops the visible request and settles the UI;
- network failure preserves the draft and the user message for deliberate retry;
- loading always ends in a bounded timeout or response state.

SkeletonBlock remains the loading primitive. Live-region announcements are short and deduplicated.

## 18. Immersive Speaking Experience

Speaking receives an immersive but honest room, using the same Academic Luxury tokens:

- a central Tutor/speaking orb with a calm state label;
- Part 1, Part 2, and Part 3 navigation;
- prompt and preparation timer where configured;
- microphone control with permission state;
- recording, paused, processing, saved, and feedback-ready states;
- a local amplitude visualizer while the microphone is active;
- a clear text-only fallback when microphone/STT is unavailable;
- a feedback panel that labels any estimate and identifies what evidence was actually used.

The state machine is:

\`READY → PROMPT → PREPARATION → RECORDING → PROCESSING → FEEDBACK_READY\`

with exits for \`PERMISSION_DENIED\`, \`UNAVAILABLE\`, \`CANCELLED\`, and \`ERROR\`.

The visualizer may render local amplitude or energy only. It must not claim pronunciation quality, transcript accuracy, fluency, or a band from amplitude. No fake transcript, fake pronunciation markers, or fake speech analysis is permitted. STT and pronunciation engines remain explicit future integration points.

## 19. Gamification

Gamification is a quiet feedback layer, not a reward economy.

### 19.1 Meaningful streak

A day counts only when the learner completes a meaningful educational action in their configured timezone, such as completing a practice attempt, submitting Writing, saving a Speaking response, or completing an approved roadmap item. Opening the app, opening Tutor, or changing settings does not count.

The UI shows current streak and the last meaningful activity date only when account data supports it. A missed day ends the consecutive streak without shame copy. No artificial freeze tokens, public ranking, or pressure notification is included in this phase.

### 19.2 Skill energy and progress

Skill energy is an interpretation of recent learning activity and confidence signals from the Phase 2 learning model. It is not an official band and does not replace the four-skill progress model. Labels must say “Band ước lượng” wherever an estimated band is shown.

### 19.3 Today’s Focus and common mistakes

Today’s Focus is one actionable recommendation derived from trusted roadmap, issue, and activity data. Common mistakes are grouped patterns backed by learning events or reviewed issues. Empty data produces a useful invitation to start, not fake examples. The interface avoids fireworks, endless badges, and unsupported claims of improvement.

## 20. Dashboard Hierarchy

The authenticated dashboard composition is:

### First viewport

- greeting and current learning context;
- \`Tiếp tục học\` card tied to the last meaningful activity;
- \`Today's Focus\` card with one primary CTA;
- compact status indicator for the four skills.

### Subsequent sections

1. Four equal skill progress cards.
2. Roadmap with the next milestone and optional expanded path.
3. Common mistakes with a route into targeted practice.
4. Streak and recent activity.

The radar chart remains a compact orientation aid rather than the main dashboard. Charts must use all four skills, a sensible 0–9 scale when bands are present, and neutral states for missing evidence. The dashboard should look like a learning studio, not an admin table.

## 21. Learning Session Continuity

The product restores the last meaningful activity, not every open browser tab.

Safe continuity includes:

- last route and skill;
- practice set/question identifiers that still exist and remain accessible;
- last valid split ratio;
- a bounded Writing draft when autosave is enabled;
- roadmap position and last completed milestone;
- selected display preferences.

It does not restore stale AI messages as current context, hidden answer state, expired uploads, private data after logout, or an exercise that the server no longer authorizes.

On resume, the client revalidates the server-owned exercise and draft version. If invalid, it shows a clear recovery choice and starts safely at the current practice entry point. Session restoration is a convenience, never an authorization shortcut.

## 22. Responsive Behavior

### 375px

- one-column skill and content layouts;
- Tutor is a viewport-safe near-fullscreen panel with safe-area padding;
- CTA buttons stack where necessary;
- search, editor, and recording controls remain usable without horizontal scrolling;
- mobile workspace uses tabs or stacked panels;
- heading scale remains readable and does not force clipped decorative art.

### 768px

- two-column cards where content remains readable;
- settings can use a wide bottom sheet or side panel;
- Tutor remains bounded and the composer stays visible;
- split workspace may remain mobile-tabbed until minimum pane widths are met.

### 1024px

- desktop split workspace becomes available if both panes meet minimum widths;
- dashboard can use two-column supporting composition;
- navigation remains accessible without crowding;
- Tutor standard/expanded states have enough height for message list and composer.

### 1440px

- editorial max-width prevents over-wide text lines;
- split panes, dashboard hierarchy, and Tutor overlay use generous negative space;
- the primary CTA remains visually obvious without competing card noise;
- no panel, chart, or decorative visual clips at normal laptop/desktop heights.

At every breakpoint, \`overflow-x: hidden\` is not a substitute for fixing an overflowing child. Focus targets, menus, dialogs, and sticky controls must remain reachable and visible.

## 23. Accessibility

Required behavior:

- one meaningful H1 per route;
- heading order follows the visual hierarchy;
- buttons are buttons, links are links, and tabs expose selected state;
- dialogs have accessible names, focus containment, Escape behavior, and focus return;
- all icon-only controls have accessible names;
- visible \`:focus-visible\` styles meet contrast requirements;
- keyboard access exists for settings, Tutor, split divider, tabs, audio controls, attachments, and all primary actions;
- status messages use appropriate polite/assertive live regions without flooding the screen reader;
- all non-text state indicators have text, icon, pattern, or structure in addition to color;
- timers do not create inaccessible auto-dismiss behavior;
- reduced motion disables decorative parallax, shimmer, orb pulsing, and non-essential transitions while preserving state changes;
- touch targets remain comfortably operable on mobile;
- text resizing and large font settings do not hide core actions;
- editor, upload, and recording errors are associated with their controls.

Accessibility testing should cover keyboard-only use, screen-reader landmarks, reduced-motion mode, forced colors/high contrast where available, 200% text zoom, and mobile safe-area behavior.

## 24. Performance

The experience must remain responsive on ordinary laptop and mobile hardware.

- Do not add a 3D rendering engine.
- Lazy-load the expanded Tutor, speaking visualizer, and heavy chart code when those surfaces are opened.
- Use \`ResizeObserver\` carefully and batch divider/layout updates.
- Keep editor autosave debounced and version-aware.
- Use \`requestAnimationFrame\` or a lightweight canvas only for local microphone amplitude visualization, with cleanup on unmount.
- Keep chat history bounded in the client and virtualize only if evidence later requires it.
- Avoid provider payloads, repeated context objects, or full dashboard rerenders on every keystroke.
- Respect the existing shared components and CSS tokens rather than adding a second styling system.

## 25. Data Model Additions

Only data that cannot be derived from the existing and AI Phase 2 domains is proposed.

### 25.1 \`user_preferences\`

One row per user, server-owned:

\`\`\`text
user_id                  primary ownership key
theme_mode               LIGHT | DARK | SYSTEM
accent_preset            GOLD | SAPPHIRE | EMERALD | BURGUNDY | VIOLET | SLATE
font_scale               SMALL | DEFAULT | LARGE
density                  COMFORTABLE | DEFAULT | COMPACT
reduce_motion             SYSTEM | REDUCED | ALLOWED
proactive_ai_enabled     boolean
cross_highlight_enabled  boolean
timer_default_enabled    boolean
reading_split_ratio      40 | 50 | 60
writing_split_ratio      40 | 50 | 60
version                  optimistic concurrency value
updated_at               server timestamp
\`\`\`

The first value in each ratio represents the left pane. The table is intentionally limited to presentation and interaction preferences. It does not duplicate learning events or AI memory.

### 25.2 \`learning_drafts\` (only if autosave is implemented)

Writing autosave needs a server-owned, bounded draft record because local storage alone cannot safely support authenticated multi-device continuity:

\`\`\`text
id
user_id                  ownership key
skill                    WRITING or future supported skill
reference_id             task/prompt identifier
content_snapshot         bounded plain text
version                  monotonic draft version
status                   ACTIVE | SUBMITTED | EXPIRED | DELETED
updated_at
expires_at
\`\`\`

Drafts contain no hidden answer keys, raw AI context, provider data, or credentials. If the product chooses not to ship autosave in a first implementation, this table is not needed yet.

### 25.3 Deliberately not added

Do not create new tables for roadmap, streak, mistakes, skill energy, learner profile, conversations, or learning events. Those concepts belong to the AI Phase 2 learning intelligence design and should be consumed by this UI. Duplicating them would create conflicting definitions of progress and streaks.

## 26. API Contracts

These are proposed UI contracts, not implementation work.

### Preferences

\`\`\`text
GET  /api/user/preferences
PUT  /api/user/preferences
\`\`\`

The update accepts only the allowlisted preference fields, validates enum values and ratios, returns the complete authoritative record, and requires authentication.

### Tutor

Keep:

\`\`\`text
POST /api/ai/chat
\`\`\`

The request remains provider-neutral and includes only trusted context identifiers resolved server-side. The normalized response may include:

\`\`\`text
status
message
grounding
sources[]
references[]
workingState
retryable
errorCode
\`\`\`

\`workingState\` is optional and must describe real backend state. \`references[]\` uses the structured contract in section 15. Provider-native fields are not exposed.

### Attachments

\`\`\`text
POST   /api/ai/attachments
GET    /api/ai/attachments/{id}
DELETE /api/ai/attachments/{id}
\`\`\`

All endpoints require authentication and ownership validation. The response contains status, safe metadata, and user-facing normalized errors.

### Drafts, if shipped

\`\`\`text
GET   /api/learning/drafts/current?skill=WRITING&referenceId=...
PUT   /api/learning/drafts/{id}
DELETE /api/learning/drafts/{id}
\`\`\`

The server enforces ownership, bounded content, version checks, expiry, and safe transitions. The client never invents a draft score or assessment.

### Phase 2 learning data

The dashboard consumes the Phase 2 profile, issue, roadmap, and event contracts. Exact paths should follow the approved AI Phase 2 spec; the UI must not create parallel endpoints for the same concepts.

## 27. Security and Ownership

- Authentication is required for personalized AI, uploads, private preferences, drafts, progress, issues, roadmap, and history.
- The backend derives \`userId\` from the authenticated principal; client-supplied IDs are hints at most and must not establish ownership.
- Every attachment, draft, reference, Tutor context, and learning record is authorized against the current user.
- Upload validation uses an allowlist, size limits, safe filenames, lifecycle status, and server-side processing boundaries.
- No provider API key, raw provider error, database identifier, answer key, or internal trace is rendered to the learner.
- Tutor responses cannot inject HTML, CSS, selectors, scripts, or arbitrary navigation targets.
- Cross-highlighting resolves only stable app IDs in the current workspace.
- Logout clears account-scoped client state and aborts in-flight private requests where feasible.
- UI copy never claims official IELTS affiliation or certified scoring.

## 28. AI Phase 2 Integration

The UI consumes AI Phase 2 as the source of adaptive intelligence:

- learner profile supplies skill context and learning preferences that are safe to display;
- roadmap supplies Today’s Focus and next milestones;
- issues/mistakes supply common patterns and targeted practice suggestions;
- learning events supply meaningful streak and activity calculations;
- Tutor context can display trusted learning issue or roadmap references;
- the frontend renders estimated bands only with explicit “Band ước lượng” labeling.

The UI does not recalculate profile state, invent streak rules, generate a second issue taxonomy, or persist a second conversation/analytics model. If Phase 2 data is unavailable, components degrade to neutral empty states and retain the assessment or practice path.

The existing provider-neutral AI response contract remains compatible. Structured references and truthful working state are additive, optional response fields; the core \`status\`, \`message\`, \`grounding\`, and \`sources\` behavior remains intact.

## 29. AI Phase 3 Extension Points

The Phase 1 shell reserves a safe future workflow without implementing it:

\`authenticated upload → document analysis → generation proposal → validation → pending review → admin approval → practice bank → learner assignment\`

The upload shell, attachment status, Tutor reference model, and admin-safe ownership boundaries are designed so Phase 3 can add this workflow without allowing generated content to become public or learner-facing practice automatically.

Phase 3 may add content provenance, review status, generated-question previews, and approval actions. It must reuse the same provider-neutral states, structured references, sanitized content boundaries, and non-public default. This spec does not design the generation algorithms or admin workflow screens in detail.

## 30. Error and Empty States

Every new surface needs an intentional state:

| Surface | Empty state | Failure state | Recovery |
| --- | --- | --- | --- |
| Settings | Defaults shown | Save failed | Retry, keep last saved value |
| Tutor | Contextual prompt suggestions | Timeout/429/unavailable | Retry, cancel, ask later |
| Attachments | No file selected | Invalid/too large/failed | Remove, choose another, retry |
| Reading references | No highlight | Stale reference | Explain unavailable, continue |
| Writing draft | No draft | Save failed | Keep local text, retry save |
| Speaking | Ready prompt | Permission/STT unavailable | Text-only input or retry permission |
| Dashboard | No activity | Data unavailable | Start assessment/practice |
| Roadmap | No roadmap yet | Loading/error | Assessment or refresh |
| Mistakes | No reviewed patterns | Data unavailable | Complete more practice |

Empty states are invitations, not fake data. Error copy is concise, localized in product language, and does not expose raw provider or database details.

## 31. Testing Strategy

### Unit and component tests

- settings drawer opens, closes, traps focus, restores focus, and resets preferences;
- theme and accent token application uses allowlisted values and preserves contrast;
- guest preferences survive reload while authenticated cache is namespaced and cleared on logout;
- auth gate blocks personalized Tutor and upload flows in the UI;
- Tutor shell transitions across closed/compact/standard/expanded/fullscreen/mobile;
- Tutor composer settles loading, timeout, cancel, 429, retry, and unavailable states;
- attachment validation rejects disallowed MIME/size and displays lifecycle status;
- split workspace exposes valid ARIA separator semantics and keyboard ratio changes;
- mobile tabs preserve pane state and editor text;
- structured references resolve valid targets and safely ignore stale or unknown IDs;
- cross-highlighting is not color-only and supports keyboard activation;
- Writing autosave versions do not let stale responses overwrite newer text;
- Speaking visualizer renders local amplitude only and does not create transcript/band claims;
- dashboard shows neutral states when Phase 2 data is absent;
- streak calculation uses meaningful events rather than app opens.

### Backend and contract tests

- preference ownership and allowlist validation;
- attachment ownership, limits, lifecycle, and normalized errors;
- draft ownership, version conflicts, expiry, and bounded content;
- \`/api/ai/chat\` remains compatible with existing provider-neutral clients;
- references are validated and cannot cross user or exercise ownership;
- auth is enforced even when frontend gates are bypassed;
- no raw provider error or secret appears in API responses.

### End-to-end and accessibility checks

- guest → login → authenticated Tutor flow;
- logout → second account state isolation;
- resume Reading and Writing activity at each viewport;
- keyboard-only settings, Tutor, divider, tabs, upload, and speaking controls;
- reduced motion snapshot/interaction checks;
- 375/768/1024/1440 layout checks with no horizontal overflow;
- focused control remains visible inside drawers, overlays, and panes.

## 32. Research Evaluation Hooks

Instrumentation should measure learning-flow quality without logging private content:

- time from dashboard load to first meaningful action;
- which primary CTA is chosen;
- resume success/failure and stale-session recovery;
- settings completion and reset frequency;
- Tutor context attachment rate, retry rate, and abandonment—not raw private messages;
- time to resolve a Reading reference;
- draft save success and conflict rate;
- Speaking permission failure and text-only fallback use;
- completion of Today’s Focus and targeted practice;
- accessibility task completion and keyboard friction in research sessions.

Research events must be user-consented where required, minimized, owned, and documented. They must not become a second learning-event system or capture provider prompts, raw answer content, or private drafts by default.

## 33. Implementation Decomposition

The future implementation is intentionally divided into three reviewable subphases. This is decomposition, not an implementation plan.

### UI/UX-1A — Product Shell and Personalization

Scope:

- settings drawer and preference provider;
- semantic theme tokens, light/dark/system, accent presets, typography, density, motion;
- guest/server preference persistence and logout isolation;
- authenticated \`AuthGate\` for personalized AI;
- responsive Tutor shell states, context badge, quick actions, composer states;
- attachment selection/status shell and viewport-safe panel.

Exit signal: the shell is keyboard-safe, responsive, provider-neutral, and honest when AI or upload capability is unavailable.

### UI/UX-1B — Smart Learning Workspace

Scope:

- reusable \`SplitLearningWorkspace\`;
- Reading passage/question panes and keyboard divider;
- Writing prompt/editor pane, word count, autosave indicator, timer boundary;
- mobile workspace tabs;
- structured Tutor references and safe resolver;
- cross-highlighting across supported targets;
- session continuity and stale-reference recovery;
- dynamic working-state, timeout, retry, cancel, and reduced-motion behavior.

Exit signal: a learner can complete a Reading or Writing session without losing context, text, focus, or truth about AI state.

### UI/UX-1C — Motivation and Speaking Experience

Scope:

- speaking room/orb, Part 1/2/3, timers, mic permission, recording and local amplitude visualizer;
- text-only/STT boundary and feedback-ready shell;
- dashboard hierarchy with Today’s Focus, skill energy, streak, roadmap, and common mistakes;
- Phase 2 data adapters and neutral empty states;
- meaningful progress labels and no unsupported official claims.

Exit signal: the dashboard motivates a next learning action and Speaking feels immersive without inventing transcript, pronunciation, or score evidence.

## 34. Risks and Mitigations

- **Theme customization dilutes Academic Luxury.** Mitigate with allowlisted token sets, contrast tests, and no free-form color picker initially.
- **Preferences leak between accounts.** Mitigate with server authority, account-namespaced caches, logout clearing, and account-switch e2e tests.
- **Tutor context becomes stale or misleading.** Mitigate with trusted IDs, resolver validation, context badge, and stale-reference no-op behavior.
- **Split panes become difficult on small screens.** Mitigate with minimum widths, mobile tabs, preserved scroll positions, and explicit ratio presets.
- **Autosave creates privacy or data-loss risk.** Mitigate with bounded plain text, ownership checks, versioning, expiry, draft status, and visible save state.
- **AI working states become deceptive.** Mitigate by rendering named stages only when supplied by the backend and using one honest fallback state otherwise.
- **Speaking UI implies unsupported capability.** Mitigate with a local-amplitude-only rule, explicit STT boundary, and state-specific copy.
- **Gamification pressures learners or duplicates Phase 2.** Mitigate with meaningful-action streak rules, quiet language, and reuse of Phase 2 events.
- **Dashboard becomes an admin dashboard.** Mitigate with one primary action, editorial composition, four-skill equality, and progressive detail.
- **Heavy interactions cause re-render storms.** Mitigate with local state boundaries, debounced persistence, lazy surfaces, and measured resize/audio loops.
- **Attachment support expands scope into a content platform.** Mitigate by shipping only the interaction/status shell in this phase and reserving extraction/generation for later approved work.

## 35. Major Rulings

- Academic Luxury remains the default and the source of truth for visual language.
- Accent presets are complete semantic token sets, not arbitrary colors.
- Authenticated server preferences win over local cache; guest preferences never silently become private account data.
- Frontend auth gates improve UX; backend auth and ownership checks remain mandatory.
- Personalized AI is not available to guests; guest UI may show only neutral preview or auth guidance.
- \`/api/ai/chat\` stays provider-neutral. References and working state are normalized, optional additions.
- AI cannot return arbitrary DOM selectors, HTML, CSS, scripts, or navigation commands.
- No fake loading stages, fake transcript, fake pronunciation analysis, fake band, or official IELTS claim.
- Local microphone visualization represents amplitude only.
- Reading/Writing use the shared split primitive; mobile uses tabs or stacked mode rather than compressed panes.
- Streaks count meaningful learning actions, not logins or screen opens.
- Roadmap, learner profile, issues, mistakes, and learning events come from AI Phase 2 rather than duplicate UI systems.
- UI/UX-1A, 1B, and 1C are the future implementation boundaries.
- This document is a spec only. No implementation plan is created until the human reviews and approves this document.

## 36. Definition of Done for a Future Implementation

The future implementation is ready for review when:

- settings, persistence, theme tokens, auth gate, Tutor shell, workspace, speaking room, and dashboard behavior follow this spec;
- all four skills remain first-class and equal in navigation and presentation;
- guest/member ownership and empty states are honest;
- structured references and cross-highlighting are safe and keyboard accessible;
- AI states always settle and never claim unsupported provider work;
- Writing drafts and session continuity are recoverable without leaking private data;
- Speaking never fabricates STT, pronunciation, or scores;
- responsive checks pass at 375, 768, 1024, and 1440 widths;
- reduced motion, focus-visible states, semantic headings, live regions, and screen-reader labels are verified;
- backend contract, ownership, and security tests pass;
- AI Phase 2 remains the source of adaptive learning truth;
- no Phase 2/3 implementation has been started early;
- a human review has approved the spec and a separate implementation plan before code changes begin.

## Current Status

Implementation: **NOT STARTED**  
Open questions: **NONE**  
Next gate: human review of this written spec. Do not create an implementation plan until the human approves it.


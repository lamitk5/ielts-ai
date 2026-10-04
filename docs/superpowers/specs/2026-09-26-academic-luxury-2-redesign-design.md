# Academic Luxury 2.0 — Full Redesign Design Specification

Status: design-only specification

This document defines a presentation and interaction redesign for the existing IELTS AI Tutor product. It does not authorize product implementation, database migrations, dependency installation, provider changes, or deployment.

## 1. Purpose

Academic Luxury 2.0 makes the existing learner experience feel premium, editorial, calm, and useful in both dark and light modes. The redesign is intentionally additive: it refines the shell, themes, Tutor, attachment UX, dashboard presentation, and learning workspaces while preserving the existing authenticated flows, server-owned learning rules, provider-neutral AI contract, and truthful AI boundaries.

The primary users are IELTS learners using a focused practice workspace and members returning to a personal learning command center. Success means a learner can understand where to go next, work comfortably for a long session, and trust what the product claims.

## 2. Current UI Problems

The current implementation is functional and already contains the Phase 1 shell, preferences, Reading/Writing workspaces, Speaking room, Tutor states, and learning-intelligence adapters. The current problems are presentation and contract alignment problems, not a reason to rewrite trusted domain logic.

- The visual system still reads as grey and heavy in places, with weak separation between page, section, card, and elevated surfaces.
- Light mode is technically available but still feels like a pale variant of the dark theme rather than a designed warm editorial theme.
- Some sections use generous empty space without a corresponding hierarchy or clear continuation cue.
- Several cards and status treatments feel like generic SaaS dashboard components instead of calm academic instruments.
- The current navigation exposes a large logout action where a compact authenticated profile menu would provide a cleaner hierarchy.
- Tutor behavior is functional, but the panel can spend too much space on chrome or dead space. Fullscreen, restore, close, composer, context, references, and attachments need one coherent hierarchy.
- The frontend attachment contract accepts PNG, JPG, JPEG, and WEBP, while the current backend service accepts only PDF, DOCX, and TXT. A JPG selected in the browser therefore fails at upload with the document-only server message.
- Upload support, extraction/indexing, and multimodal visual understanding are not the same capability, but the current surface does not make that boundary explicit enough.
- Reading and Writing workspaces are now split and responsive, but their visual treatment should feel more like an exam desk than a collection of nested cards.
- Speaking has a truthful local visualizer boundary, but the room should feel more intentional and exam-focused in both themes.
- Dashboard and homepage sections must not imply adaptive intelligence when Phase 2 evidence is unavailable.

## 3. Approved Visual Direction

Academic Luxury 2.0 is a premium academic interface with deep navy, warm champagne gold, warm ivory, and strong editorial typography.

- Dark mode: deep navy canvas, warm gold emphasis, ivory typography, restrained glow, cinematic but readable.
- Light mode: warm ivory and soft cream canvas, deep navy typography, champagne gold emphasis, clean editorial contrast, never cold hospital white.
- Playfair Display remains reserved for hero, high-value editorial headings, and select labels; Inter remains the default for navigation, forms, controls, body copy, and data.
- Surfaces are layered by semantic role rather than by repeated translucent glass. Glass is an accent treatment, not the default for every card.
- Gold is an emphasis signal, not a full-page fill. No neon palette, childish game treatment, or constant glow.
- Practice workspaces remain calm and information-first even when the marketing homepage is more expressive.

## 4. Design Principles

1. Trust before spectacle: every score, recommendation, status, and AI claim must reflect a real source or be clearly marked unavailable.
2. Two first-class themes: dark and light use the same semantic component contracts but have intentionally different surface, contrast, and shadow decisions.
3. Editorial hierarchy: typography, spacing, and alignment do more work than borders, pills, and decorative effects.
4. One source of truth: components consume semantic tokens and existing shared primitives; they do not branch on accent names or hardcode theme colors.
5. Focused learning: Reading and Writing should look like workspaces; Tutor should support the task without stealing it.
6. Progressive disclosure: secondary detail appears when useful, not as a wall of badges.
7. Honest capability boundaries: upload support is not vision; local microphone amplitude is not speech recognition; an estimate is not an official IELTS result.
8. Keyboard and reduced-motion parity are design requirements, not post-release polish.

## 5. Dark Theme

Dark mode is the default cinematic expression of the brand.

- Page canvas is a deep navy-black with a subtle cool-to-warm depth shift, not a pure black void.
- Sections use adjacent navy values so hierarchy is visible without heavy outlines.
- Elevated surfaces use a slightly lighter navy with a controlled shadow and an optional low-opacity gold edge only for selected or high-value states.
- Primary text is warm ivory. Secondary text is a readable blue-grey ivory; muted text is reserved for metadata and never for essential instructions.
- Gold is warm champagne rather than bright yellow. Interactive gold must have a light-mode counterpart and a dark-mode contrast-safe counterpart.
- Tutor and Speaking may use a slightly deeper exam surface, but they remain within the same token system.
- Long Reading passages use a stable, low-glare surface and comfortable line-height.

## 6. Light Theme

Light mode is a complete Academic Luxury theme, not a white filter over dark mode.

- Page canvas is warm ivory or soft cream.
- Sections alternate between ivory and barely warmer cream to create rhythm without grey blocks.
- Cards use white or soft ivory surfaces with warm neutral borders and shallow navy-tinted shadows.
- Primary text is deep navy. Secondary text is slate navy. Muted text is used sparingly and must remain readable for long-form instructions.
- Champagne gold is used for emphasis, selection, links, and progress accents through contrast-safe semantic roles.
- Inputs and editor surfaces are visibly distinct from the page without relying on a grey fill.
- Tutor and settings maintain the same sense of depth as dark mode with fewer blur effects and more editorial whitespace.
- Focus rings, warning text, success text, and selected states each receive light-theme values that pass their relevant contrast role.

## 7. Semantic Tokens

Components use semantic tokens only. A component must never select `#060b16`, `#f5f7fa`, or an accent name directly. Token application remains centralized in the preference/theme layer.

### Required token families

`--bg-page`, `--bg-section`, `--surface-1`, `--surface-2`, `--surface-elevated`, `--surface-interactive`, `--text-primary`, `--text-secondary`, `--text-muted`, `--text-inverse`, `--border-subtle`, `--border-strong`, `--accent`, `--accent-hover`, `--accent-soft`, `--focus-ring`, `--success`, `--warning`, `--danger`, `--shadow-sm`, `--shadow-md`, `--shadow-lg`, `--overlay`, and `--glass-bg`.

Existing aliases such as `--background`, `--surface`, `--surface-alt`, `--text`, `--text-secondary`, and `--muted` may remain temporarily as compatibility aliases, but new or redesigned components consume the canonical roles above. The token adapter owns alias mapping during migration.

### Conceptual value matrix

| Role | Dark | Light |
|---|---|---|
| page | deep navy-black | warm ivory |
| section | layered navy | soft cream |
| surface 1 | navy panel | warm white |
| surface 2 | raised blue-navy | ivory panel |
| elevated | high-contrast navy | white with navy-tinted shadow |
| primary text | warm ivory | deep navy |
| secondary text | cool ivory-grey | slate navy |
| muted text | subdued ivory-grey | readable warm slate |
| accent | champagne gold light value | contrast-safe champagne gold dark value |
| accent soft | translucent warm gold | translucent warm gold |
| border | low-opacity warm gold/navy | warm neutral/navy |
| focus | bright, contrast-safe gold/ivory ring | deep gold/navy ring |
| overlay | deep navy translucent | deep navy translucent |
| glass | navy with controlled opacity | warm white with controlled opacity |

Status roles are independent of accent choice. Every theme/preset combination receives tested values for text and non-text contrast. Shadows use semantic roles, not repeated hardcoded rgba values in components.

## 8. Typography

- Playfair Display: hero heading, major page title, section title, and a small number of editorial labels.
- Inter: navigation, body, forms, data, buttons, metadata, Tutor content, and workspace controls.
- Base body size remains readable at 16px-equivalent. Font scale preferences change the type scale, line-height, and control wrapping together rather than only enlarging one property.
- Hero headings use a capped measure and intentional `clamp()` sizing so the exact slogan wraps naturally without an orphan word.
- Section titles are visually subordinate to the hero while remaining clearly discoverable.
- Reading passage and Writing editor use generous line-height and a measure that supports sustained focus.
- Labels use sentence case where possible; all-caps eyebrow labels are short and not required for comprehension.
- Vietnamese diacritics must be tested at all supported font scales.

## 9. Spacing/Grid

Use a small spacing scale based on semantic increments rather than ad hoc margins. The redesign targets continuity without making the page dense.

- Content max width: 1200–1280px depending on route; learning workspaces may use the available viewport after stable page gutters.
- Desktop gutters: 32–48px; tablet gutters: 24–32px; mobile gutters: 16–20px.
- Homepage section gap: 72–96px desktop, 56–72px tablet, 40–56px mobile.
- Card gap: 20–24px desktop and tablet, 16px mobile.
- Interactive controls have a minimum 44px touch target.
- Dashboard cards use fewer, larger zones instead of many tiny metrics.
- Tutor panel body has an independently scrollable conversation region and a composer that remains reachable within the viewport.
- Negative space is preserved around high-value headings and workspaces, but empty space must communicate grouping or calm focus.

## 10. Header/Nav

The header is light, compact, and consistent across themes.

Desktop structure:

- Brand: `IELTS AI Tutor` with a restrained mark/wordmark treatment.
- Primary links: `Trang chủ`, `4 kỹ năng`, `Trợ giảng AI`, and `Tiến độ`.
- Right controls: settings gear, then an authenticated profile menu or compact account action; guests see a clear `Đăng nhập` action.
- Avoid a large logout pill as the dominant authenticated control. Logout remains available inside the profile menu.
- Active navigation uses text weight, a subtle underline or tokenized accent, and a non-color cue.

Mobile structure:

- One compact menu button with an accessible name and expanded state.
- Drawer or expanded navigation has a clear close action, safe focus behavior, and no horizontal overflow.
- Settings can open from the mobile navigation without duplicating provider state.

Hash links such as `#skills`, `#progress`, and `#ai-tutor` remain valid. Route changes must not silently discard active practice state.

## 11. Homepage

The homepage is a premium learning landing/dashboard hybrid.

1. Header: compact navigation and personalization entry.
2. Hero: concise value proposition, exact product slogan, primary assessment CTA, secondary four-skills CTA, and a Learning Intelligence visual that clearly distinguishes available product capability from future intelligence.
3. Four Skills: equal Reading, Listening, Writing, and Speaking cards with one action each. No card is visually declared the default skill.
4. Member Learning Overview: only show personalized sections when authenticated and backed by existing member data. Candidate zones are Continue Learning, four-skill progress, streak, recent activity, and a roadmap preview.
5. Truthful empty states: when Phase 2 evidence is unavailable, use `Chưa đủ dữ liệu` and `Hoàn thành thêm bài luyện để nhận gợi ý cá nhân hóa` rather than invented weaknesses, focus, mistakes, or roadmap items.
6. Tutor preview: a small, useful introduction to the contextual Tutor with source/grounding language that does not claim unsupported capabilities.
7. Final CTA: one clear next action, with no false performance claims or official affiliation language.

Avoid tiny pill clusters, excessive glass blur, grey filler blocks, giant dead zones, and gamification that competes with learning.

## 12. Settings

Settings remains an accessible right drawer on desktop and a full-height sheet/full-screen panel on mobile.

Navigation groups:

- Giao diện
- Ngôn ngữ
- Phông chữ
- Mật độ
- Chuyển động
- Trợ giảng AI
- Quyền riêng tư
- Đặt lại

Main controls:

- Theme cards: `Tối`, `Sáng`, `Hệ thống` with visual previews.
- Accent presets: Gold, Sapphire, Emerald, Burgundy, Violet, Slate.
- Font scale and density.
- Motion preference: system, reduced, allowed.
- Proactive AI suggestions, cross-highlighting, and timer defaults.

Changes apply immediately and autosave through the existing guest/account preference contract. Status must distinguish loading, saving, synced, unsynced, and conflict. Reset requires confirmation. The modal traps focus, makes the background inert to assistive technology, restores prior focus, supports Escape, and exposes state through text as well as color.

## 13. Tutor

Tutor is a premium study companion, not a second dashboard.

### Normal panel

- Header: `Trợ giảng AI`, trusted context badge when applicable, one fullscreen/restore toggle, and X close.
- Conversation: distinct user/assistant bubbles, readable spacing, source chips, grounding status, and stale-context labels where applicable.
- Quick actions are short, contextual, app-owned prompts: `Giải thích lỗi Writing`, `Tại sao đáp án Reading này sai?`, `Luyện Speaking Part 2`, and `Lên kế hoạch luyện tập`. Only actions valid for the current context appear.
- Composer: plus attachment control, single message input, send action, and cancel while pending. Empty space is used for readable conversation, not as a decorative void.
- The pending HTTP state is one honest generic state such as `Đang chuẩn bị phản hồi…`; no fake staged grammar/vocabulary/rubric progress.

### Fullscreen

Fullscreen uses the same toggle and becomes a focused learning workspace. On wide screens, conversation may occupy the left and trusted context/references the right. On narrow screens it becomes a safe full-height single column. The design must not introduce a second conversation model or hide the composer below the viewport.

The current `TutorShell`, message list, source chip, skeleton, retry, cancel, and provider-neutral API contracts are extended rather than replaced wholesale.

## 14. Attachments

Attachment UI is compact and explicit about what the product supports.

- One active attachment per Tutor request.
- 10 MiB maximum.
- Documents: PDF, DOCX, TXT.
- Images: PNG, JPG, JPEG, WEBP.
- Lifecycle shown to the learner: `SELECTED`, `VALIDATING`, `UPLOADING`, `READY`, `FAILED`, `REMOVED`, `EXPIRED`.
- Image thumbnail; document icon/chip; filename; human-readable size; remove; retry.
- The selected file is presented near the composer, not as a large empty card.

The frontend performs extension/MIME precheck for immediate UX. The backend remains authoritative for MIME/content, size, ownership, and one-active-file policy. The contract must be shared or generated from one allowlist so a JPG cannot pass browser validation and fail as an unsupported backend type.

Upload support is separate from vision. An accepted image may receive status `IMAGE_READY`, while Tutor remains `VISION_NOT_ENABLED` until a real multimodal provider path exists. No copy may imply that the AI saw or analyzed an image when it only stored the file.

## 15. Dashboard

The dashboard is a learner command center, not an admin panel.

Recommended order:

1. Continue Learning.
2. Today's Focus, only when backed by a real API; otherwise a truthful empty state.
3. Four Skill Progress with equal visual treatment.
4. Roadmap preview, only when server-provided.
5. Common Mistakes, only from centralized real data.
6. Study Streak using meaningful activity rules.
7. Recent activity.

Progress uses restrained meters and editorial labels. Avoid childish energy bars, unbounded celebration, fake personal bands, fake exam dates, or invented adaptive recommendations.

## 16. Reading

Reading remains an exam-like split workspace.

- Desktop: passage left, questions right, draggable divider with 40/50/60 ratios, independent scroll containers.
- Mobile/narrow tablet: real tabs or stacked panes, never compressed desktop columns.
- Passage typography is calm, readable, and low-glare in both themes.
- Question hierarchy is stronger than decorative cards. The rail clearly communicates current, unanswered, answered, flagged, and reviewed through text/icon/structure as well as color.
- Submit and review actions remain obvious without moving answer ownership to the browser.
- Passage and paragraph IDs are stable application targets for future Tutor references. The browser never accepts arbitrary CSS selectors or XPath from AI text.

## 17. Writing

Writing is a serious editor workspace.

- Desktop: prompt/chart/task left, editor right; mobile uses tabs or stacked panes.
- Top action bar may contain timer, word count, autosave status, Tutor entry, and submit.
- The editor is a purpose-built surface with comfortable line-height, padding, focus ring, and a readable measure; it should not look like a generic browser textarea.
- Autosave status is truthful: `Đang lưu`, `Đã lưu`, `Chưa lưu`, and `Không thể lưu`/conflict equivalents.
- Draft references are bound to exact `draftVersion`; edited text makes old offsets stale and they must never be approximated onto the new text.
- Submission remains the existing server-owned flow. `Band ước lượng` remains explicit; no official IELTS score claim is allowed.
- Light mode receives dedicated attention because it is the preferred long-form writing environment for many learners.

## 18. Speaking

Speaking remains a truthful local room.

- Desktop concept: left part selector/cue, center examiner orb/timer/local amplitude visualizer, right prompt/help/status when useful.
- Dark-first exam surface is acceptable if it remains clearly connected to the theme system and has an intentional light equivalent.
- Mobile becomes a single-column room with prompt, timer, local visualizer, and text response controls.
- Allowed: microphone permission, local amplitude visualization, timers, prompt selection, text response, and saving text attempts.
- Not allowed: STT, fake transcript, pronunciation score, audio fluency score, claim that Tutor heard the learner, or audio-derived band.

## 19. Responsive

The redesign is verified at 375, 768, 1024, and 1440+ widths.

| Width | Behavior |
|---|---|
| 375 | single-column home/cards; mobile nav; full-height settings/Tutor; workspace tabs; no horizontal overflow |
| 768 | two-column cards where readable; tablet nav/spacing; workspace tabs or stacked panes; Tutor remains viewport-safe |
| 1024 | desktop shell begins; Reading/Writing split is allowed with readable minimum pane widths; settings remains a drawer |
| 1440+ | editorial max-width; generous but bounded grid; Tutor fullscreen can use two regions; no clipped visual or controls |

All responsive changes preserve focus order, state, active tabs, pane scroll positions, and text content.

## 20. Accessibility

- One logical H1 per page and semantic landmark structure.
- All dialogs have names, focus containment, Escape/backdrop behavior, background inertness, and focus return.
- Settings controls expose their current value and status to assistive technology.
- Divider exposes `role="separator"`, value min/max/now, a name, and Arrow/Home/End keyboard behavior.
- Mobile workspace tabs use selected state and valid panel relationships.
- Every critical state has text/icon/structure in addition to color.
- Focus-visible indicators use `--focus-ring` and meet contrast requirements in both themes.
- Buttons and icon-only controls have accessible names and 44px-class touch targets.
- Tutor source/reference chips are keyboard reachable; stale references are readable and do not silently no-op.
- Inputs and editor content retain labels and error/status associations.
- Reduced motion applies to CSS and motion-library paths, including explicit user preference and OS changes.
- Long text and large font settings must not clip controls or create horizontal overflow.

## 21. Motion

Motion is subtle and purposeful:

- soft hover lift;
- opacity and small scale transitions around 0.98–1.00;
- drawer open/close;
- Tutor orb glow;
- bounded reference highlight pulse.

Avoid constant glow, large parallax, particles, scroll hijacking, and continuous decorative animation. The explicit reduced-motion preference must disable relevant transitions and Framer Motion behavior; `prefers-reduced-motion` must remain a safe override. All pending states settle through response, timeout, cancel, retry, unavailable, or error.

## 22. Performance

- Reuse React, CSS, Framer Motion already present, and Lucide icons.
- Do not add a 3D engine, animation framework, large state library, or component-kit migration.
- Keep drag updates local and frame-batched; do not rerender the whole application on every pointer move.
- Keep Tutor conversation scrolling independent from the page.
- Avoid rendering both heavyweight fullscreen and compact visual trees when one stateful shell can serve both.
- Prefer CSS tokens and existing primitives over duplicated style systems.
- Verify bundle growth and preserve route-level loading behavior.

## 23. Data/AI Truthfulness

AI Phase 2 learning intelligence is not implemented by this design. UI must not invent:

- weaknesses;
- personalized mistakes;
- roadmap items;
- Today's Focus;
- adaptive recommendations;
- official IELTS scoring;
- vision analysis;
- STT or pronunciation analysis.

When evidence is unavailable, show a calm empty state, for example `Chưa đủ dữ liệu` and `Hoàn thành thêm bài luyện để nhận gợi ý cá nhân hóa`. Existing adapters and server response contracts remain provider-neutral. Generic Tutor chat must not trigger RAG or claim contextual evidence that was not supplied.

## 24. Component Architecture

### Keep

- `Button`, `GlassCard`, `AnimatedSection`, `SectionTitle`, and `SkeletonBlock` as shared primitives.
- `AppLayout`, `Navbar`, `Footer`, `PreferenceProvider`, `SettingsDrawer`, `TutorShell`, `TutorComposer`, source chips, workspace divider/tabs, Speaking room, and existing API adapters.

### Extend

- semantic token adapter and theme previews;
- `PageContainer`/section spacing styles if a small shared primitive is justified;
- `Surface`/`Card` variants only where they remove repeated surface rules;
- `EmptyState`, `IconButton`, `SegmentedControl`, `ProgressMeter`, `TutorBubble`, and `AttachmentChip` only when existing components cannot cover the need.

### Replace selectively

- repeated grey dashboard card styling;
- large authenticated logout treatment;
- duplicated attachment status presentation;
- Tutor layout spacing that clips or leaves dead space.

### Delete only after consumer migration

- compatibility token aliases that no longer have consumers;
- redundant resize controls or duplicate message/composer implementations.

Do not create a parallel theme provider, chat state machine, page layout system, or backend ownership model.

## 25. Backend Upload Contract

The attachment contract is one explicit boundary shared by frontend and backend.

### Accepted file policy

| Category | Extensions | MIME families |
|---|---|---|
| documents | `.pdf`, `.docx`, `.txt` | `application/pdf`, DOCX OpenXML, `text/plain` |
| images | `.png`, `.jpg`, `.jpeg`, `.webp` | `image/png`, `image/jpeg`, `image/webp` |

- Maximum size: 10 MiB (10 × 1024 × 1024 bytes).
- Maximum active attachment: one per Tutor request.
- Reject empty files, HTML, SVG, JavaScript, executables, scripts, path traversal, and arbitrary server paths.
- Backend checks authenticated owner from `AuthPrincipal`, never from a client-supplied user ID.
- Backend checks safe content/MIME and size; frontend checks are advisory UX only.
- Store safe metadata only in the current bounded Phase 1 flow. Extraction, indexing, and vision are separate capabilities.

### Lifecycle and capability boundary

`SELECTED → VALIDATING → UPLOADING → READY` is the success path. Failure, removal, and expiry are terminal or recoverable states. Image readiness is represented as `IMAGE_READY` only when the backend has accepted the image. Tutor displays `VISION_NOT_ENABLED` until a real multimodal provider path is implemented. No fake image description or “AI analyzed this image” copy is permitted.

The current bug is resolved at design level by requiring the same allowlist to drive both browser `accept`/validation and backend validation. Implementation must add backend image types and normalized image-aware messages in a later approved work item; this specification itself does not alter them.

## 26. Testing Strategy

### Component

- Semantic dark/light token application, contrast roles, settings previews, explicit reduced motion, and system media-query changes.
- Navbar/mobile menu, Tutor states, fullscreen/restore, composer, source chips, skeleton, attachments, cards, empty states, and progress meters.
- Reading divider/tabs/rail/passage targets and Writing editor/status/reference staleness.

### Integration

- Light/dark switching and server-authoritative preference persistence.
- Guest/member Tutor gate, expand/restore, timeout/retry/cancel, and viewport-safe composer.
- One attachment flow for each accepted document/image type, including 10 MiB boundary and one-active-file rule.
- Reading/Writing state preservation across mobile tabs and route changes.
- Draft version change makes old Tutor offsets stale.

### Backend

- MIME/extension allowlist, content and size validation, ownership, one-active attachment, normalized errors, removal/expiry, and image-ready/no-vision boundary.
- No real provider quota in tests.

### Visual/responsive

Review at 375, 768, 1024, and 1440. Capture both themes for homepage, settings, Tutor, Reading, Writing, and Speaking. Check no horizontal overflow, no clipped Tutor composer, and no empty-state claims that imply unavailable data exists.

### Accessibility

Keyboard focus and return, dialogs, divider, tabs, source/reference chips, contrast, large text, non-color state signals, reduced motion, and screen-reader status announcements.

## 27. Migration / Compatibility

This is a presentation and contract-alignment design, not a database migration request.

- Preserve existing route paths, authentication/session behavior, practice submission contracts, draft version semantics, Tutor reference semantics, and Speaking local boundary.
- Preserve existing local-storage preference versioning and server-authoritative preference API.
- Token changes require compatibility aliases until all current consumers migrate.
- Upload contract alignment may require a later backend code change and, only if durable metadata changes, a separately reviewed migration. This spec does not create one.
- Existing Phase 1 learning-intelligence adapters remain empty-state safe. No Phase 2 endpoint is invented to make the redesign look populated.
- Any upload API response extension must be additive and must not break existing `status`, `answer`, `sources`, `grounding`, or `meta` fields in AI chat responses.

## 28. Implementation Decomposition

### AL2-A — Design System & Product Shell

Semantic tokens, first-class Dark/Light themes, typography, spacing, header, profile menu, settings previews, shared surfaces, and responsive shell polish.

### AL2-B — Homepage, Dashboard & Tutor

Homepage hierarchy, member/guest empty states, dashboard composition, Tutor normal/fullscreen shell, composer hierarchy, source chips, and attachment presentation.

### AL2-C — Learning Workspaces

Reading visual treatment, Writing editor polish, Speaking room composition, mobile workspace tabs, divider/accessibility refinements, and reduced-motion review.

### AL2-D — Attachment Contract Alignment

One shared document/image allowlist, backend image MIME support, normalized upload lifecycle, ownership/size/content enforcement, safe error copy, and explicit `IMAGE_READY`/`VISION_NOT_ENABLED` boundary.

### AL2-E — Verification and compatibility hardening

Responsive/visual/accessibility verification, contract regression, route regression, bundle review, and final theme parity review.

## 29. Risks

- Token migration can cause regressions if compatibility aliases are removed before all consumers move to semantic roles.
- Light-mode contrast can regress when accent presets are applied to borders, status text, selected controls, and focus rings; test each role rather than one gold value.
- Tutor fullscreen can become overbuilt; keep the right-side context area optional and reuse existing message state.
- Attachment allowlist changes may create a security gap if MIME is trusted without content checks or if frontend and backend lists diverge again.
- Adding image acceptance without a vision provider can create misleading copy; keep upload and analysis visibly separate.
- Dashboard polish can accidentally create fake learner intelligence; empty states must remain first-class.
- Reading/Writing visual changes can damage long-session readability through excessive glass, low contrast, or insufficient pane width.
- A broad global CSS rewrite can break existing pages; migrate route by route behind semantic aliases.

## 30. Non-Goals

- AI Phase 2 implementation.
- AI Phase 3 generator or adaptive-learning backend.
- Fake vision, STT, transcript, pronunciation score, audio band, or official IELTS result.
- New microservices, provider routing, or model changes.
- PostgreSQL/pgvector/RAG redesign unrelated to the UI contract.
- 3D avatars, particle systems, heavy animation frameworks, or large frontend framework migration.
- Real authentication rewrite.
- Unrelated backend refactor.

## 31. Definition of Done

The redesign is ready for implementation review when:

- Dark and Light themes are visually intentional, tokenized, and contrast-tested across all accent presets.
- The header, homepage, dashboard, settings, Tutor, attachments, Reading, Writing, and Speaking have a coherent Academic Luxury 2.0 hierarchy.
- Light mode is first-class and no longer reads as grey/dim.
- Tutor has one fullscreen/restore control, X close, compact attachment UI, reachable composer, truthful pending/error states, source chips, and no clipped controls.
- Browser and backend attachment contracts accept the same documents/images with the same 10 MiB and one-active-file rules.
- Image upload never implies vision analysis when vision is unavailable.
- Reading, Writing, and Speaking preserve existing trusted data rules and responsive state behavior.
- No fake Phase 2 learning intelligence, fake scores, fake STT, or fake vision appears.
- 375/768/1024/1440 checks show no horizontal overflow or clipped controls.
- Keyboard, screen-reader, focus-visible, contrast, reduced-motion, and large-text checks pass.
- Existing frontend/backend tests, lint, build, and backend package remain green.
- The final implementation is split into approved subphases and does not introduce unnecessary dependencies.

## 32. Open Questions / Rulings

The following decisions are made for implementation planning; they do not require new product behavior in this design-only task.

1. **Theme default:** retain the current system-aware default, while making both explicit Light and explicit Dark first-class. This avoids silently changing returning users’ appearance.
2. **Accent semantics:** retain the six existing accent choices, but map them to role-specific contrast-safe tokens. Accent choice never selects component markup.
3. **Tutor fullscreen layout:** use an optional two-region context panel only when trusted context/references exist; otherwise keep a centered conversation workspace.
4. **Image acceptance:** accept PNG/JPG/JPEG/WEBP at the upload boundary, but expose `VISION_NOT_ENABLED` until a real multimodal provider contract exists.
5. **Attachment persistence:** retain the current bounded attachment lifecycle and ownership model unless a later implementation task explicitly approves durable storage changes.
6. **Dashboard intelligence:** show only existing server/member data and adapter-backed empty states. Do not create local placeholder values that look personal.
7. **Reading/Writing mobile:** use the accepted tabs/stacked-pane model below the readable split breakpoint; preserve text, draft version, active tab, and pane scroll.
8. **Visual QA evidence:** implementation work should capture both themes at 375, 768, 1024, and 1440, with targeted route checks for Tutor, upload, Reading, Writing, Speaking, settings, and dashboard.
9. **Implementation boundary:** this document is the design approval artifact only. A separate approved implementation plan is required before product code changes begin.

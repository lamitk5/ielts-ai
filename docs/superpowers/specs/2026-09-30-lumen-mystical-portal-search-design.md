# LUMEN Mystical Portal Search — Design Specification

**Status:** Design only; implementation has not started.

**Scope:** Frontend search-entry experience for the LUMEN IELTS AI Tutor homepage and its existing practice-search route. This design preserves the current Academic Luxury system, real practice-search API, authentication, Én, attachment handling, Groq, Gemini, Cloudflare, RAG, and Tutor behavior.

## 1. Context

The homepage already provides a functional hero search entry point. The desired next experience is a more distinctive LUMEN interaction: a calm, mystical portal that helps a learner move from an intention to a practice destination without turning search into a game or hiding the existing content architecture.

The approved direction is **Option B — Mystical Portal**: an editorial portal aperture with a restrained gold/navy aura, subtle ambient particles, and an intentional transition into real practice results.

## 2. Goals

1. Make the hero search feel like a premium LUMEN gateway into practice.
2. Preserve the existing search URL contract and result behavior.
3. Make the input, submit action, suggestions, loading state, empty state, and error state clearer rather than less usable.
4. Reuse the existing Academic Luxury palette, typography, preference system, reduced-motion behavior, and ambient particle layer.
5. Keep pointer and keyboard interaction predictable, accessible, and performant.
6. Make the future implementation independently testable without introducing a backend migration or provider-specific payload dependency.

## 3. Non-goals

- No new backend endpoint, database table, migration, search index, or AI search call.
- No replacement of `GET /api/practice/search?q=...`.
- No change to practice-card routes, authentication, role guards, learner data, Én, attachment upload, Groq, Gemini, Cloudflare, RAG, or Tutor contracts.
- No fake recent searches, fake popularity metrics, fake recommendations, or fabricated learner personalization.
- No full-screen takeover that hides the global header, page navigation, or footer.
- No large image/video background, heavy particle library, WebGL requirement, or canvas requirement.
- No change to the existing quick-suggestion destinations unless a later approved product task explicitly changes them.

## 4. Existing architecture findings

### Current search entry

- `frontend/src/components/home/HeroSearch.jsx` owns the query input, validation message, submit action, and quick suggestions.
- A non-empty submit trims the query and navigates to `/practice/search?q=<encoded query>` using React Router.
- Empty submission stays on the page and renders `Nhập nội dung bạn muốn luyện tập.`.
- Reading and Listening suggestions navigate directly to `/practice/reading` and `/practice/listening`.
- Writing Task 2 and Speaking Part 2 suggestions navigate to the existing search route with encoded query text.
- The input is labelled `Tìm nội dung luyện tập IELTS`; the form has `role="search"` and an accessible name.

### Current search results

- `frontend/src/App.jsx` maps `/practice/search` to `SearchPage`.
- `frontend/src/pages/SearchPage.jsx` reads the `q` query parameter, calls `searchPractice(query)`, renders loading/status text, result cards, and an empty state.
- `frontend/src/services/searchApi.js` calls `GET /api/practice/search?q=...` and accepts an array payload only.
- Search result cards use the shared `GlassCard` and link to the route supplied by each result.
- If the API fails, the current page has a narrow fallback for writing queries; other failed or empty searches show the existing empty-safe state. The portal must not broaden that fallback into fabricated content.
- The route is already a real route; the older search branch in `PlaceholderPage` is not the active route target in `App.jsx`.

### Current shared visual and preference architecture

- `AppLayout` renders the shared `AmbientGoldenParticles` layer, cursor layer, skip link, navbar, main outlet, and footer.
- `AmbientGoldenParticles` is a fixed, `pointer-events: none`, `aria-hidden` decorative layer. It currently renders 32 DOM particles, performs window-level mouse repulsion with `requestAnimationFrame`, and falls back to timer scheduling when necessary.
- Particle drift and twinkle are CSS animations. Reduced motion removes the movement loop/listeners and switches the layer to a static state. The preference provider also exposes reduced motion and applies theme/accent tokens to `document.documentElement`.
- Existing CSS uses the approved navy/gold/ivory family and existing `Playfair Display`/`Inter` typography conventions.
- This design treats ambient particles as a shared visual substrate. The portal may add a bounded visual target or accent layer, but it must not create a second global pointer loop or duplicate the particle engine.

### Worktree constraint

The current worktree contains unrelated local upload-trace changes and QA artifacts. The implementation of this design must not edit, remove, or depend on those files. The spec itself changes no product code.

## 5. User experience

### Entry state

The existing hero search remains the primary control. Visually, it sits inside a restrained portal aperture: a rounded editorial frame with a dark/navy core in dark mode or a warm ivory core in light mode, a champagne edge glow, and a soft aura that suggests depth without looking like a game portal.

The portal has one clear heading/label, one text input, one submit control, and the existing quick suggestions. Decorative copy must not compete with the input label. The primary action remains `Tìm bài luyện`.

### Focus state

When the input receives focus, the portal aperture becomes slightly more defined: a low-amplitude ring glow and a subtle inward/outward particle response may be shown. The input receives the existing visible focus treatment. Focus must not move automatically to another control, and the portal must not trap focus.

### Submit state

On valid submit:

1. Trim the query.
2. Preserve the current encoded URL navigation to `/practice/search?q=...`.
3. Let the destination page own data fetching and loading state.
4. Apply a short, interruptible portal transition only if it does not delay navigation or make keyboard users wait.

The transition is an affordance, not a loading replacement. Browser history and direct links remain unchanged.

### Loading, result, empty, and error states

The search page continues to render a semantic status while its request is pending. A future portal transition may use a small aperture pulse or a skeleton-like result entrance, but it must not remove `role="status"` or imply results exist before the API responds.

The portal is visually subordinate once results are shown. Result cards remain the primary content. The result heading keeps the actual query, and every result retains its real skill, title, description, and route. The portal must not stay as a large sticky obstruction over the result grid.

The existing empty-safe message remains visible and actionable through a clear return/focus path to search. API failure must never show fabricated success. If the service layer can distinguish failure from legitimate emptiness without changing the backend contract, the UI may expose that distinction; otherwise the current safe fallback behavior remains the source of truth.

## 6. Visual system

### Composition

- The portal is a bounded hero treatment, not a full-screen modal.
- The aperture uses layered borders and radial gradients rather than a hard triangle, stock illustration, or unrelated neon palette.
- The center remains calm enough for readable input text and the submit button.
- Champagne/gold is used for focus, edge light, and small accents; it is not used as a large saturated fill behind text.
- Existing `Playfair Display` remains for editorial heading treatments and `Inter` remains for labels, controls, statuses, and result metadata.

### Tokens

Implementation should use existing CSS variables (`--surface-*`, `--text-*`, `--gold`, `--gold-light`, `--accent-*`, `--focus`, and existing shadow/radius tokens). New portal tokens, if needed, must be scoped to the portal component and derive from existing theme/accent tokens, for example:

- `--portal-aura`
- `--portal-ring`
- `--portal-core`
- `--portal-depth`

### Layer order

1. Portal aura and decorative rings.
2. Reused ambient particle substrate or a small portal-local accent layer.
3. Search surface and controls.
4. Validation/status text.

Decorative layers remain non-interactive. Search controls remain in the normal interaction layer.

## 7. Search behavior

The existing behavioral contract is normative:

```text
query = input.trim()
if query is empty: show validation, do not navigate
else: navigate('/practice/search?q=' + encodeURIComponent(query))
```

The portal must support:

- typing and editing without animation-induced layout shift;
- Enter to submit from the input;
- explicit submit activation by mouse, touch, Enter, or Space;
- quick-suggestion activation with the existing destinations;
- browser Back/Forward and direct loading of `/practice/search?q=...`;
- deep-linked queries containing spaces, punctuation, Vietnamese characters, and URL-unsafe characters;
- a query that is cleared after validation without leaving stale error text;
- no request on whitespace-only submission.

The portal must not debounce or send requests from every keystroke. Fetching remains owned by `SearchPage` after navigation.

## 8. Motion/particle behavior

### Normal motion

- Portal aura: slow, low-amplitude breathing/pulse, approximately 8–14 seconds per cycle.
- Portal ring: short focus/submit response, approximately 240–520 ms, with an ease-out curve.
- Particle response: reuse the existing global pointer repulsion loop. If a portal-local target is required, expose a lightweight pointer position/target signal rather than adding a second `pointermove` listener per particle.
- Submit transition: one short aperture opening/crossfade; no spinning loader in the input control unless the page is actually waiting for a request.

### Reduced motion and animation setting

When system reduced motion or the product animation preference is active:

- Disable continuous portal pulse, particle drift, shimmer, and pointer repulsion.
- Keep a static, high-contrast portal frame and visible focus state.
- Keep manual search, suggestions, and navigation fully functional.
- Avoid opacity-only transitions that make content appear/disappear without a status announcement.

Eye tracking and unrelated Én behavior remain governed by their existing components and are not changed by this feature.

## 9. Light/dark theme behavior

### Dark

Use deep navy core/surfaces, ivory text, and restrained champagne edge light. The aura may use low-opacity gold over navy, with enough local contrast for the input hint, typed text, and submit label.

### Light

Use warm ivory/cream surfaces, deep navy text, and darker champagne/gold accents where needed for contrast. The aperture should read as a warm architectural opening rather than a gray overlay. Gold glow opacity must be reduced or darkened as necessary so it does not wash out text.

Theme changes must update without a page reload through the existing preference token application. No hard-coded dark-only text or focus color is permitted.

## 10. Responsive behavior

### 1440 and 1024 px

The portal may occupy the hero search column width but must preserve the current two-column hero composition and HeroVisual. The search control remains one coherent surface with a readable input and compact submit button.

### 768 px

The portal remains bounded and centered within the hero content. Controls may tighten, but the input and submit label must remain readable and keyboard reachable. Decorative rings should reduce rather than overflow the hero bounds.

### 375 px

The portal becomes a vertical search surface: the input row and submit action may stack as the existing mobile search CSS already permits. Quick suggestions wrap. The aura is clipped only inside a deliberate decorative container; no horizontal page overflow is allowed. The input remains large enough for comfortable touch use.

Responsive rules must avoid fixed viewport-height assumptions and remain safe with browser zoom and mobile keyboard appearance.

## 11. Accessibility

- Preserve the semantic `form` with `role="search"` and its accessible name.
- Preserve a real `label` associated with the input; visible decorative text cannot be the only label.
- Keep `aria-invalid` and `aria-describedby` for validation.
- Keep validation in a `role="alert"` or equivalent announced status.
- Preserve logical DOM order: label/input, submit, suggestions, then decorative content where possible.
- All icon-only controls, if introduced, require explicit accessible names. The portal should not need icon-only controls for core search.
- Decorative rings, particles, and aura use `aria-hidden="true"` and `pointer-events: none`.
- Visible `:focus-visible` styling must remain stronger than the decorative glow.
- Do not rely on hover, color, or animation to communicate query validity or result availability.
- Ensure contrast for both themes and supported accent presets.
- The base portal is not a modal and does not require Escape behavior.

## 12. Component architecture

The preferred implementation is additive and small:

- `HeroSearch` remains the owner of query state, submit validation, and navigation.
- A presentational `MysticalPortalFrame` may own portal rings/aura and accept `children`, `isFocused`, `isSubmitting`, and `reducedMotion` props. It must not own search data or navigation.
- `HeroSuggestions` remains a sibling/child of the search experience and retains its existing route/query mapping.
- `SearchPage` remains the owner of query-param reading, API loading, result state, and empty/result rendering.
- `searchApi.js` remains the only service boundary for the existing search request.
- `AmbientGoldenParticles` remains the shared global particle engine. If it gains portal awareness, expose a bounded optional target contract; do not duplicate its window listener or move search state into the global layout.

Possible future file additions are limited to a portal presentational component and its focused test. No backend files, database files, or provider files are in scope.

## 13. State/data flow

The implementation should keep a small local visual state model:

```text
IDLE -> FOCUSED -> IDLE
IDLE/FOCUSED -> SUBMITTING -> navigate('/practice/search?q=...')
SUBMITTING -> IDLE only if navigation is prevented by validation
```

`query`, `error`, and navigation remain in `HeroSearch`. `SearchPage` has its existing `query`, `loading`, and `results` state. The portal frame receives derived visual flags only; it never receives provider-specific AI payloads or learner-private data.

The implementation must not introduce a global search store, URL mutation on every keystroke, persistence of query history, or analytics side effects without a separate approved requirement.

## 14. Performance

- Keep the portal DOM shallow and decorative elements bounded.
- Reuse the existing particle loop and CSS animations; do not add one animation frame loop per component.
- Avoid React state updates on every pointer movement. Pointer coordinates/repulsion remain DOM/CSS-variable driven as in the existing particle architecture.
- Prefer CSS transforms/opacity for motion; avoid layout-affecting width/height animation.
- Keep containment/overflow boundaries on decorative layers where appropriate.
- Clean up all timers, media-query listeners, and pointer listeners on unmount.
- Keep the feature dependency-free.
- Ensure the portal is inert when hidden or when reduced motion/animation-off requires static rendering.

## 15. Error/empty states

| Situation | Required outcome |
| --- | --- |
| Empty or whitespace query | Stay on the current page; announce `Nhập nội dung bạn muốn luyện tập.`; expose invalid state. |
| Valid query, request pending | Navigate to search route; show `role="status"` loading state. |
| Valid query, results returned | Show real API results and their supplied routes. |
| Valid query, no results | Show the existing empty-safe message without fake cards. |
| API failure | Use the existing safe fallback behavior; never show fabricated success. |
| Direct search URL without `q` | Show the existing empty-safe state and usable search/navigation context. |

Any copy changes require separate product approval because the current Vietnamese copy is established UI behavior.

## 16. Testing strategy

### Component tests

Add focused tests for the portal frame and extend existing search tests only where behavior is touched:

- portal renders the existing search label, input, submit control, and suggestions;
- portal focus state is derived from input focus and does not steal focus;
- valid submission preserves the encoded `/practice/search?q=...` navigation;
- whitespace submission preserves validation behavior;
- quick suggestions preserve their current direct/search destinations;
- decorative layers are `aria-hidden` and non-interactive;
- reduced-motion/animation-off renders a static portal state and still allows submission;
- dark/light theme smoke checks use existing preference tokens without hard-coded vendor/theme assumptions.

### Route/service tests

Preserve and extend existing search-route coverage:

- query results render real skill links;
- loading status is present while the request is pending;
- empty-safe state renders for an empty result;
- failed request does not render a false successful result;
- encoded queries with spaces and non-ASCII characters reach the service boundary correctly.

### Manual verification

At 1440, 1024, 768, and 375 px, verify dark and light themes, keyboard-only search, pointer/touch interaction, no horizontal overflow, no focus loss, readable contrast, smooth but restrained transition, and no interference with HeroVisual, Hero CTA, Én, or the global header.

## 17. Acceptance criteria

1. The homepage shows an unmistakable but restrained Mystical Portal search treatment in the hero.
2. The search input remains labelled, keyboard reachable, and usable with Enter.
3. The submit button remains the primary action and keeps `Tìm bài luyện`.
4. Empty submission retains existing validation and does not navigate.
5. Valid submission still navigates to `/practice/search?q=<encoded trimmed query>`.
6. Existing quick suggestions retain their current routes and query behavior.
7. SearchPage continues to use the real `GET /api/practice/search?q=...` boundary.
8. Real results, result links, loading status, empty state, and safe error behavior remain intact.
9. No fake learner data, fake recommendations, fake recent searches, or AI/provider payload assumptions are introduced.
10. Dark and light themes maintain readable contrast with supported accent presets.
11. Reduced motion and animation-off disable continuous decorative motion while preserving all search functionality.
12. The portal and particles never capture pointer events or block the input, CTA, HeroVisual, navbar, footer, or Én.
13. The layout has no horizontal overflow at 375 px and remains balanced at 768, 1024, and 1440 px.
14. Tests cover search behavior, accessibility states, reduced motion, and new visual state transitions.
15. The implementation adds no backend, database, auth, Tutor, RAG, upload, or provider changes.

## 18. Risks/tradeoffs

- A large visual portal could reduce hero content space. The design bounds the aperture to the existing search column and allows mobile stacking.
- Reusing global particles keeps performance consistent but limits portal-specific choreography. That tradeoff is intentional; a second pointer loop would increase jank and maintenance cost.
- A portal transition can make search feel slower if it waits before navigation. Navigation must remain immediate; visual motion is interruptible and secondary.
- The current service fallback is narrower than a complete search error model. This spec preserves it instead of inventing a new API contract; a separate task can improve error typing later.
- Light-mode gold glow can reduce contrast. Theme-specific token values and contrast tests are required before approval.
- Deep-link searches bypass the hero. SearchPage must therefore remain complete and usable without portal state.

## 19. Out-of-scope future ideas

- Search across Tutor conversations or uploaded documents.
- Semantic/AI search and natural-language query expansion.
- Recent-search persistence or learner-specific search history.
- Search filters, sorting, difficulty facets, duration facets, or skill facets.
- Admin-managed search synonyms or indexing controls.
- Portal scenes, 3D/WebGL effects, audio feedback, or full-screen transition pages.
- Analytics events, A/B experiments, or personalization beyond current real API results.

## Review resolution

- Every required section has a concrete decision, behavior, or acceptance rule; no unresolved requirement remains in this specification.
- “Mystical” is resolved as a bounded visual portal with low-amplitude aura/rings, not a modal, game, or provider feature.
- “Search” is resolved as the existing hero-to-`/practice/search` flow, not a new backend search system.
- “Particle interaction” is resolved as reuse/extension of the existing shared engine, not a second global animation loop.
- The implementation boundary is explicit: frontend presentation and tests only; no source implementation is authorized by this document.

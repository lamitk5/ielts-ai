# IELTS AI Tutor — Frontend Foundation & Phase 1 Premium Homepage Design Spec

Date: 2026-09-19  
Status: Approved v3 — RAG/Trust UI contract integrated  
Project type: Research prototype / Web platform  
Official project name: **Nghiên cứu và xây dựng Nền tảng Web Trợ giảng AI hỗ trợ luyện thi IELTS 4 kỹ năng trực tuyến**

---

## 1. Product goal

Build a premium web-based AI teaching assistant for IELTS that supports the full four-skill learning journey:

- Reading
- Listening
- Writing
- Speaking

The product should feel **academic, trustworthy, intelligent, premium, and calm**, not flashy. The UI must remain extensible enough for future AI grading, RAG-based tutoring, speech processing, practice history, analytics, authentication, and admin operations.

The frontend should communicate three core ideas immediately:

1. This is an IELTS learning platform covering all four skills.
2. AI acts as a contextual teaching assistant rather than a generic chatbot.
3. The interface is modern, premium, readable, and suitable for long study sessions.

### Technology target

- Frontend: React + Vite
- Styling: Tailwind CSS
- Motion: Framer Motion
- Routing: React Router
- Charting for Phase 1: Recharts
- Backend integration target: Java Spring Boot REST API
- Database target: PostgreSQL
- Optional queue/cache target: Redis
- AI integration target: provider abstraction layer for Gemini / SambaNova / Groq / Azure Speech or other approved services

---

## 2. Product scope and four-skill architecture

The frontend architecture must reserve a clear feature boundary for all four IELTS skills from the beginning.

Suggested feature structure after the homepage foundation is established:

```text
src/features/
  reading/
  listening/
  writing/
  speaking/
  tutor/
  dashboard/
  auth/
  admin/
```

### Skill responsibilities at product level

#### Reading
- Practice fixed-answer IELTS question types
- Track accuracy and progress
- AI Tutor explains why an answer is correct or incorrect using the current passage/question context

#### Listening
- Practice audio-based fixed-answer IELTS question types
- Track accuracy and progress
- AI Tutor explains answers based on transcript/question context when available

#### Writing
- Task 1 and Task 2 practice
- Timed writing environment
- Word count
- AI-assisted feedback based on IELTS rubric
- Estimated band score, error explanation, and improvement suggestions

#### Speaking
- Speaking Part 1, Part 2, Part 3 practice
- Audio recording / future STT integration
- Simulated speaking room
- AI-assisted feedback and estimated skill analysis

Phase 1 does **not** implement the full learning workflows. It establishes the homepage entry points and visual architecture for all four skills.

---

## 3. Visual direction

### Design concept

**Academic Luxury**

The interface should combine:

- premium dark academic tone
- modern AI product clarity
- restrained gold accents
- spacious layouts
- refined motion
- high readability

The result must not resemble a casino, crypto product, gaming dashboard, nightclub, or luxury hotel website.

### Color palette

- Background: `#060B16`
- Surface 1: `#0C1424`
- Surface 2: `#101B30`
- Navy: `#071426`
- Deep Navy: `#102746`
- Gold: `#CFAE67`
- Light Gold: `#E5C982`
- Primary text: `#F5F7FA`
- Secondary text: `#B7C0CF`
- Muted text: `#697386`

Gold is an accent, not the dominant color.

### Typography

- Heading / editorial accent: Playfair Display
- UI / body / data: Inter

### Surface style

- Dark navy backgrounds
- Thin low-contrast borders
- Subtle glassmorphism
- Soft shadows
- Large negative space
- Controlled glow on interactive emphasis only
- Avoid excessive blur layers or transparency that reduces readability

### Homepage mood

The homepage should look closer to a premium academic technology platform than a traditional exam-prep website.

Key visual signals:

- strong editorial headline
- high-contrast typography
- restrained gold highlights
- layered depth in the hero
- clean analytics visuals
- elegant glass cards
- smooth but subtle interaction feedback

---

## 4. Phase 0 — Frontend Foundation

Phase 0 creates the reusable frontend foundation. It does not implement IELTS business logic, API integration, authentication, AI grading, or backend code.

### Deliverables

- React + Vite app
- Tailwind CSS configured
- Framer Motion installed and wired
- React Router installed and wired
- Global design tokens
- Global typography and page background
- Reusable Button component
- Reusable GlassCard component
- Reusable SectionTitle component
- Reusable AnimatedSection component
- App shell / layout
- Navbar skeleton
- Responsive base styles
- Reduced-motion accessibility support
- Lint and production build passing

### Recommended initial structure

```text
src/
  components/
    common/
      Button.jsx
      GlassCard.jsx
      SectionTitle.jsx
      AnimatedSection.jsx
    layout/
      Navbar.jsx
      AppLayout.jsx
  pages/
    HomePage.jsx
  styles/
    globals.css
  App.jsx
  main.jsx
```

### Shared component rules

#### Button
- Variants: primary, secondary, ghost
- Sizes: sm, md, lg
- Keyboard accessible
- Shared focus-visible treatment
- Gold accents driven by tokens
- Prepared for Liquid Hover in Phase 1

#### GlassCard
- Shared surface, border, blur, and shadow treatment
- Optional hover interaction
- No skill-specific business logic

#### AnimatedSection
- Reveal-on-scroll wrapper
- Framer Motion
- Honors reduced motion

#### SectionTitle
- Reusable eyebrow, heading, and optional description
- Consistent spacing

#### Navbar
Phase 0 provides only the responsive shell:
- Working brand placeholder: `IELTS AI Tutor`
- Main navigation placeholders
- CTA placeholder
- Lightweight mobile menu

The official research title remains the formal project name in documentation. The shorter `IELTS AI Tutor` label is a temporary UI brand for layout purposes and can be renamed later without changing architecture.

---

## 4.1 Future AI Grounding Architecture — frontend-facing contract

Phase 0–1 does not execute RAG, but the frontend foundation must be compatible with a grounded AI pipeline so later phases do not require redesign. The planned backend flow is:

```text
User input / learner submission
  → Spring Boot API
  → AI Orchestrator
  → task/skill metadata filter
  → retrieval from approved knowledge base
  → PostgreSQL + pgvector similarity search
  → top-k grounded context
  → rubric-aware prompt
  → LLM / speech service
  → structured-output validation
  → output guardrail
  → frontend response
```

### Knowledge-source policy

The product must distinguish **approved/legally usable sources** from merely relevant sources. Do not describe the knowledge base as “exclusive/proprietary” and do not ingest or advertise specific copyrighted commercial books or internal third-party materials unless the research team can document the right to use them.

Preferred source categories:

- official/public IELTS band descriptors and assessment guidance that the team is permitted to use
- licensed or otherwise permitted reference materials
- teacher/expert-created rubric notes and annotated examples with permission
- anonymized learner samples collected with appropriate consent
- project-generated teaching notes and error taxonomies

Every retrievable chunk should be able to carry metadata such as:

```text
sourceId
sourceTitle
sourceType
skill
taskType
rubricCriterion
rightsStatus
version
section
```

### Retrieval behavior

`topK` should be configurable rather than hard-coded. A sensible initial experiment is 3–5 chunks after filtering by skill/task/rubric metadata, but retrieval quality must be evaluated rather than assumed.

RAG is a grounding mechanism that **reduces hallucination risk and improves traceability**; the product must not claim that RAG “eliminates hallucinations.” If retrieval is insufficient, the system should be allowed to return an `insufficient_context` state instead of inventing an answer.

### Prompt-role policy

System prompts must define an assessment role and rules, but must not fabricate credentials such as “former IELTS examiner with 15 years of experience.” Use wording such as:

```text
You are an IELTS Writing assessment assistant.
Evaluate the learner submission only against the supplied rubric and retrieved evidence.
Do not invent criteria, source claims, or examiner credentials.
If evidence is insufficient, explicitly return insufficient_context.
Return output that conforms to the required JSON schema.
```

For Writing, Task 1 and Task 2 must use the correct criterion label where applicable (`Task Achievement` vs `Task Response`) rather than a single hard-coded label.

### Structured result contract

Later phases should expose a stable frontend-safe response shape similar to:

```js
{
  assessmentType: 'writing_task_2',
  overallBandEstimate: 6.5,
  criteria: [
    {
      code: 'TR',
      label: 'Task Response',
      bandEstimate: 6.5,
      summary: '...',
    },
    { code: 'CC', label: 'Coherence & Cohesion', bandEstimate: 6.0, summary: '...' },
    { code: 'LR', label: 'Lexical Resource', bandEstimate: 6.5, summary: '...' },
    { code: 'GRA', label: 'Grammatical Range & Accuracy', bandEstimate: 6.0, summary: '...' },
  ],
  strengths: ['...'],
  issues: [
    { category: 'grammar', location: 'paragraph-2', explanation: '...', suggestion: '...' },
  ],
  suggestions: ['...'],
  citations: [
    { sourceId: 'rubric-writing-task2-v1', title: '...', section: '...' },
  ],
  grounding: { status: 'grounded', sourceCount: 4 },
  disclaimer: 'Band điểm do AI ước lượng nhằm hỗ trợ luyện tập, không phải kết quả IELTS chính thức.'
}
```

The frontend must never depend on raw provider-specific payloads. Spring Boot will normalize provider responses to this application contract.

### Evaluation contract

The research plan may test 100 diverse Writing/Speaking samples with at least two independent qualified human graders, then compare AI and human grading using MAE, Pearson/Spearman correlation, and an agreement measure such as weighted Cohen’s Kappa or ICC where appropriate. Any target such as “85% within ±0.5 band” is an **experimental target/hypothesis**, not a guaranteed product claim. Human–human agreement should also be reported so AI–human agreement has a meaningful baseline.

---

# 5. Phase 1 — Premium Homepage

## 5.1 Phase objective

Phase 1 turns the frontend foundation into a polished, interactive homepage that visually communicates the final four-skill product vision without implementing the full backend workflows.

The homepage must work in two presentation states:

1. **Guest state** — introduces the product and drives the user toward an assessment or practice flow.
2. **Logged-in state** — additionally shows a compact progress dashboard with four-skill band visualization, exam countdown, and common mistakes.

For Phase 1, authentication state may be represented by a local/mock state or development flag only. Real authentication belongs to Phase 2.

---

## 5.2 Homepage information architecture

Recommended section order:

```text
Navbar
  ↓
Hero Section
  ↓
Four Skill Cards
  ↓
Logged-in Progress Overview / Guest Dashboard Preview
  ↓
AI Tutor value preview
  ↓
Final CTA
  ↓
Footer

Floating AI Tutor Widget remains available above the page content.
```

The layout must remain spacious. Do not force every feature into the first viewport.

---

## 5.3 Hero Section

### Purpose

The hero should immediately explain what the platform is, what makes it different, and what the user can do next.

### Required slogan

**“Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền”**

### Supporting copy direction

A concise supporting statement should communicate:

- support for all four IELTS skills
- contextual AI tutoring
- personalized practice and feedback
- study-anytime convenience

Keep the body copy short enough to remain readable in the first viewport.

### Required elements

#### A. Hero headline
- Large serif headline
- Maximum 2–3 visual lines on desktop
- Important phrase may use subtle gold emphasis
- Avoid gradient-filled body text

#### B. Quick Search Bar

Purpose: allow learners to quickly enter a topic, skill, or exercise type.

Example placeholder:

`Tìm dạng bài hoặc kỹ năng muốn luyện...`

Example query suggestions:

- IELTS Writing Task 1 Line Graph
- Reading True / False / Not Given
- Listening Map Labelling
- Speaking Part 2 Technology

Phase 1 behavior:
- UI interaction only
- submit action may navigate to a placeholder route or show a development toast
- no real semantic search required yet

Suggested component:

```text
QuickPracticeSearch
```

Required UX:
- search icon
- visible focus state
- suggestion chips below or inside an expandable suggestion area
- mobile-friendly full-width layout

#### C. Primary CTA

Required text:

**“Làm bài Test đánh giá năng lực ngay”**

CTA should use the primary gold-accent button treatment and Liquid Hover effect.

Optional secondary CTA:

`Khám phá 4 kỹ năng`

### Hero visual layer

Use abstract academic-AI visuals instead of a generic stock illustration.

Recommended composition:

- subtle radial navy lighting
- restrained gold light orbit / line accent
- floating glass data card previews
- tiny labels such as Reading / Listening / Writing / Speaking
- optional decorative AI orb or abstract neural pattern

No constant high-speed animation.

---

## 5.4 Four Skill Cards

### Purpose

Create a clear visual entry point into the four-skill architecture requested for the final product.

### Required cards

1. Reading
2. Listening
3. Writing
4. Speaking

Each skill uses the shared `GlassCard` foundation.

### Card content

Each card should contain:

- skill icon
- skill name
- concise one-line description
- lightweight status or sample metric area
- CTA such as `Luyện tập`

Optional placeholder data may show sample values such as:

- số bộ đề
- số bài đã hoàn thành
- estimated band if logged in

These values must be clearly mocked in Phase 1 and not presented as real backend data.

### Visual differentiation

Cards should share the same layout and system. Different skills may use different small icons or subtle decorative glyphs, but not unrelated color themes.

### Hover behavior

- translateY up to 6px
- scale max 1.01
- border becomes slightly brighter
- gold glow remains subtle
- animation approximately 250–300ms

---

## 5.5 Logged-in Progress Dashboard Section

### Visibility

This section is shown in the logged-in homepage state.

For guest users, it may be replaced by a simplified dashboard preview card with a CTA to sign in or start an assessment.

### Layout

Desktop recommendation:

```text
┌──────────────────────────────┬───────────────────────┐
│ Four-skill Radar Chart       │ Exam Countdown        │
│                              ├───────────────────────┤
│                              │ Common Mistakes       │
└──────────────────────────────┴───────────────────────┘
```

Tablet/mobile should stack these modules vertically.

### A. Band Radar Chart

Suggested component:

```text
BandRadarChart
```

Data axes:

- Reading
- Listening
- Writing
- Speaking

Recommended implementation:

- Recharts `RadarChart`
- responsive container
- scale visually aligned to IELTS band range
- accessible text summary below or via `aria-label`

Example mocked development data:

```js
[
  { skill: 'Reading', band: 6.5 },
  { skill: 'Listening', band: 7.0 },
  { skill: 'Writing', band: 6.0 },
  { skill: 'Speaking', band: 6.5 }
]
```

Do not imply these are actual user scores until API integration is implemented.

### B. Exam Countdown

Suggested component:

```text
ExamCountdownCard
```

Display:

- target IELTS exam date
- number of days remaining
- small progress indicator
- optional CTA `Cập nhật ngày thi`

Phase 1 may use mock date values.

### C. Common Mistakes Widget

Suggested component:

```text
CommonMistakesWidget
```

Display a short weekly summary such as:

- article usage
- subject–verb agreement
- weak topic sentence

The visual treatment should look diagnostic rather than alarming.

Possible fields:

```text
Mistake label
Frequency
Affected skill
Small improvement hint
```

Phase 1 uses mock content only.

---

## 5.6 Floating AI Tutor Widget

### Purpose

The AI Tutor should feel continuously available without obstructing study content.

### Default state

A floating button appears in the lower-right corner.

Suggested component:

```text
FloatingTutorButton
```

Visual treatment:
- compact circular or rounded-square glass button
- AI/tutor icon
- subtle gold ring
- soft idle glow only
- no aggressive bouncing animation

### Expanded state

Clicking the button opens a chat panel.

Suggested component:

```text
TutorChatPanel
```

Recommended desktop size:
- width approximately 360–420px
- maximum height around 65–75vh

Mobile behavior:
- near-full-screen bottom sheet or full-screen overlay

### Phase 1 chat contents

- header: `Trợ giảng AI`
- status label: `Sẵn sàng hỗ trợ`
- trust/status badge placeholder: `Dựa trên nguồn tham chiếu đã kiểm chứng`
- welcome message
- a few prompt suggestion chips
- text input
- send button
- mock source/citation chips on demo tutor answers so the future RAG interaction model is visible

The Phase 1 UI must not name a commercial book, internal organization document, or licensed source as if it had already been ingested. Use generic mock labels such as `Rubric Writing Task 2` or `Nguồn tham chiếu 01` until rights and source metadata are confirmed.

Example suggestions:

- `Giải thích lỗi Writing của tôi`
- `Vì sao đáp án Reading này sai?`
- `Luyện Speaking Part 2`

### Scope boundary

Phase 1 implements only the interaction shell:

- open
- close
- type locally
- optional fake response state
- skeleton loading demonstration
- mock `grounded` / `insufficient_context` visual states
- mock source chips for future RAG citations

No LLM/API integration, vector retrieval, embedding generation, or provider call exists in Phase 1.

### Accessibility

- Esc closes the panel
- keyboard focus moves into panel when opened
- focus returns to floating button when closed
- button has an accessible label
- mobile overlay prevents background scroll while open

---

# 6. Phase 1 Motion System Upgrade

Motion must communicate quality, hierarchy, and responsiveness. It must not become a visual distraction.

## 6.1 Base entrance motion

### Section reveal

```text
opacity: 0 → 1
y: 20 → 0
duration: 0.5–0.7s
```

Use stagger sparingly for groups such as skill cards.

Recommended stagger:

```text
0.06–0.10s per card
```

---

## 6.2 Parallax Scrolling

### Goal

Create a subtle sense of depth in the hero and selected decorative layers.

### Implementation guideline

Use Framer Motion:

- `useScroll`
- `useTransform`

Parallax must apply only to decorative layers, not primary text or controls.

Recommended movement range:

```text
background orb: 0 → 24px
light line layer: 0 → -18px
floating decorative card: 0 → 12px
```

### Restrictions

- no large 100px+ travel
- no parallax on long text
- disable or greatly reduce on mobile
- disable when `prefers-reduced-motion` is enabled
- avoid multiple full-screen blur layers moving simultaneously

---

## 6.3 Liquid Hover Buttons

### Goal

Buttons should feel premium and responsive without becoming flashy.

### Required behavior

On hover:

- gold light sweep moves across the border or surface
- glow gently expands
- transition completes around `0.3s`

Implementation may use:

- pseudo-element gradient sweep
- masked border gradient
- CSS transform + opacity

Do not introduce a JavaScript animation loop solely for this effect.

### Touch devices

Liquid hover is decorative. Touch users must receive the same hierarchy through static states and active feedback.

### Focus state

Keyboard focus must remain clearer than the hover effect.

---

## 6.4 Skeleton Loading

### Rule

Use skeleton placeholders instead of rotating spinners for AI/data-heavy interface states.

Suggested components:

```text
SkeletonBlock
SkeletonText
TutorMessageSkeleton
DashboardCardSkeleton
```

### Skeleton style

- dark navy base
- slightly lighter moving highlight
- low contrast
- no bright white shimmer
- animation roughly 1.2–1.6s

### Usage examples

- AI Tutor waiting response
- Dashboard metrics loading
- Radar chart data loading
- future Writing feedback loading

### Accessibility

- loading container uses `aria-busy="true"`
- skeletons are hidden from screen readers where appropriate
- meaningful loading label is available

---

## 6.5 Reduced motion

When `prefers-reduced-motion: reduce` is enabled:

- parallax is disabled
- section reveals become immediate or simple fades
- continuous shimmer may become a static placeholder or slower minimal transition
- liquid sweep is replaced by a simple border/background change

---

# 7. Phase 1 Component Architecture

Recommended structure after Phase 1:

```text
src/
  components/
    common/
      Button.jsx
      GlassCard.jsx
      SectionTitle.jsx
      AnimatedSection.jsx
      SkeletonBlock.jsx
    charts/
      BandRadarChart.jsx
    motion/
      ParallaxLayer.jsx
    layout/
      Navbar.jsx
      AppLayout.jsx
      Footer.jsx
    home/
      HeroSection.jsx
      QuickPracticeSearch.jsx
      SkillCardsSection.jsx
      SkillCard.jsx
      ProgressOverviewSection.jsx
      ExamCountdownCard.jsx
      CommonMistakesWidget.jsx
      TutorPreviewSection.jsx
      CTASection.jsx
    tutor/
      FloatingTutorButton.jsx
      TutorChatPanel.jsx
      TutorMessageSkeleton.jsx
      TutorSourceChips.jsx
      TutorGroundingBadge.jsx
  pages/
    HomePage.jsx
  data/
    homepageMockData.js
  styles/
    globals.css
  App.jsx
  main.jsx
```

### Component isolation rules

- `HomePage.jsx` only composes sections; it must not contain all UI logic.
- Radar chart must remain isolated from dashboard layout.
- Floating Tutor components must not contain AI provider logic.
- Tutor UI must render application-level `grounding` and `citations` fields rather than provider-specific payloads.
- AI-generated band values must be labeled as estimates and must never appear as official IELTS results.
- Mock homepage data should live in a dedicated file.
- Shared components must not hardcode homepage-specific copy.
- Motion helpers must not depend on IELTS business state.

---

# 8. Homepage State Model

Phase 1 does not implement real authentication, but components should be prepared for it.

Suggested development shape:

```js
const homepageState = {
  isAuthenticated: false,
  user: null,
  progress: null,
};
```

Logged-in mock example may include:

```js
{
  isAuthenticated: true,
  user: {
    firstName: 'Đăng',
    targetBand: 7.0,
    examDate: '2026-12-20'
  },
  progress: {
    reading: 6.5,
    listening: 7.0,
    writing: 6.0,
    speaking: 6.5
  }
}
```

This shape is provisional and may be replaced by the real Spring Boot API contract later.

---

### Future AI response state shape

Homepage Phase 1 may use a tiny mock of the future normalized contract:

```js
const tutorDemoResponse = {
  content: '...',
  grounding: { status: 'grounded', sourceCount: 2 },
  citations: [
    { sourceId: 'demo-rubric-01', title: 'Rubric Writing Task 2', section: 'Task Response' },
  ],
}
```

Supported UI states to reserve now:

- `idle`
- `retrieving`
- `generating`
- `grounded`
- `insufficient_context`
- `error`

These are user-facing processing states only; the UI must never expose private chain-of-thought or hidden model reasoning.


# 9. Responsive Behavior

## Desktop

- Hero may use a two-column composition
- Search bar can be wide and prominent
- Skill cards: 4 columns if space allows
- Dashboard: radar chart + stacked side widgets
- AI Tutor: floating panel

## Tablet

- Hero shifts toward 1-column or balanced stacked layout
- Skill cards: 2 columns
- Dashboard modules stack or use 2-column hybrid layout

## Mobile

- Hero is single-column
- Search and CTA become full-width where appropriate
- Skill cards: 1 column
- Dashboard cards stack vertically
- AI Tutor opens as a bottom sheet or near-full-screen panel
- Parallax is reduced or disabled

No horizontal scrolling is allowed at standard viewport widths.

---

# 10. Accessibility Requirements

Phase 1 must include:

- semantic headings
- keyboard-operable search, cards, CTA, and tutor widget
- visible focus states
- sufficient text contrast
- `aria-label` for icon-only buttons
- Escape-to-close for tutor dialog
- focus management for dialog open/close
- reduced-motion support
- chart text summary or accessible label
- skeleton loading states that do not create screen-reader noise

---

# 11. Performance Requirements

The premium experience must remain lightweight.

Rules:

- avoid large video backgrounds
- avoid canvas particle engines in Phase 1
- use CSS gradients and small SVG accents where possible
- keep parallax transforms GPU-friendly
- avoid animating layout properties such as width/height/top/left continuously
- lazy-load non-critical heavy sections if necessary
- Recharts should be used only where needed
- image assets should be optimized before inclusion

Target perception:

- first viewport appears quickly
- interactions respond immediately
- motion never causes scroll jank

---

# 12. Phase 1 Engineering Rules

- Reuse the design tokens established in Phase 0.
- Do not create page-specific duplicate versions of Button or GlassCard.
- Do not introduce new font families.
- Do not add UI libraries that override the design language.
- Recharts is the only expected new visual dependency for the radar chart.
- No real backend/API calls in Phase 1.
- No AI provider keys in frontend code.
- Do not claim that RAG eliminates hallucinations; wording is limited to grounding, traceability, and hallucination-risk reduction.
- Do not fabricate AI examiner credentials.
- Do not hard-code unverified commercial/internal source names as ingested knowledge-base content.
- Reserve UI support for citations, grounding status, and estimated-score disclaimers.
- Mock values must be centralized and clearly separated from production integration code.
- Do not implement Reading/Listening/Writing/Speaking full pages yet.
- Routes may point to lightweight placeholders for navigation continuity.
- Keep HomePage composition readable and small.

---

# 13. Phase 1 Task Breakdown

The implementation should be split so Codex works on one task at a time.

## P1-01 — Homepage shell

Create:
- final Navbar styling
- HomePage section composition
- Footer
- responsive page spacing

Acceptance:
- no horizontal overflow
- desktop/tablet/mobile structure works
- no feature-specific logic yet

## P1-02 — Hero Section

Create:
- headline
- supporting copy
- quick search
- primary CTA
- optional secondary CTA
- premium decorative background layers

Acceptance:
- slogan is present exactly as approved
- search is keyboard accessible
- first viewport remains readable at common laptop sizes

## P1-03 — Motion foundation upgrade

Create:
- ParallaxLayer
- Liquid Hover treatment for Button
- reduced-motion variants

Acceptance:
- no scroll jank
- reduced-motion disables nonessential movement
- hover effect completes around 0.3s

## P1-04 — Four Skill Cards

Create:
- reusable SkillCard
- four cards for Reading / Listening / Writing / Speaking
- responsive grid

Acceptance:
- 4 skills clearly visible
- identical system, no four unrelated visual themes
- hover motion remains subtle

## P1-05 — Logged-in Progress Overview

Create:
- BandRadarChart
- ExamCountdownCard
- CommonMistakesWidget
- guest preview fallback

Acceptance:
- chart renders all 4 skills
- all data comes from centralized mock data
- mobile stacking works

## P1-06 — Floating AI Tutor

Create:
- floating button
- chat panel
- open/close interaction
- prompt chips
- local input
- responsive mobile overlay
- mock grounding badge
- mock source/citation chips
- `insufficient_context` demo state

Acceptance:
- Escape closes panel
- focus management works
- mock grounded answer can show source chips
- insufficient-context state is readable and non-alarming
- no AI API/vector DB call exists

## P1-07 — Skeleton Loading

Create:
- base skeleton component
- tutor response skeleton
- dashboard skeleton states

Acceptance:
- no spinner used for these states
- accessible loading state exists
- animation respects reduced motion

## P1-08 — Final responsive and polish pass

Check:
- desktop
- tablet
- mobile
- focus states
- motion consistency
- overflow
- typography
- section rhythm

Run:

```bash
npm run lint
npm run build
```

Phase 1 is not complete unless both pass.

---

# 14. Phase 1 Acceptance Criteria

Phase 1 is accepted when all conditions below are true:

1. Homepage uses the approved Academic Luxury design system.
2. Official project scope visibly supports Reading, Listening, Writing, and Speaking.
3. Hero contains the approved slogan.
4. Hero contains quick search and the primary assessment CTA.
5. Four skill cards are present and responsive.
6. Logged-in state contains a four-skill radar chart.
7. Logged-in state contains exam countdown and common-mistakes widgets.
8. Guest state does not expose fake personal progress as real user data.
9. Floating AI Tutor can open and close smoothly.
10. Tutor panel has accessible keyboard behavior.
11. Skeleton loading exists for simulated AI/data waiting states.
12. Liquid Hover is implemented without harming focus accessibility.
13. Subtle parallax exists only on decorative layers.
14. Reduced-motion mode disables or simplifies nonessential animation.
15. No backend/API or real authentication is implemented.
16. No AI key or provider secret appears in frontend code.
17. Tutor demo supports visible grounding status and mock source chips without claiming unverified data rights.
18. Any displayed AI band is explicitly labeled as an estimate, not an official IELTS result.
19. The UI includes an `insufficient_context` fallback state instead of implying the AI must always answer.
20. `npm run lint` passes.
21. `npm run build` passes.
22. No console errors appear during normal homepage interaction.
23. Desktop, tablet, and mobile layouts are usable without horizontal overflow.

---

# 15. AI Collaboration Workflow

## ChatGPT

Responsible for:
- architecture
- UX structure
- task breakdown
- acceptance criteria
- prompt design
- review guidance

## Codex

Primary implementation agent.

Rules:
- implement one numbered task at a time
- read this spec before coding
- avoid unrelated refactors
- run lint/build after implementation tasks
- report only changed files, verification result, and blockers

## OpenCode

Focused reviewer/fixer after Codex.

Rules:
- review only the current task
- check responsiveness, accessibility, duplication, build/lint, and performance
- fix confirmed issues only
- do not redesign the page independently

## Gemini

Primary support for:
- IELTS domain content
- rubric structure
- RAG knowledge design
- research methodology
- content validation
- candidate source inventory and metadata proposals

Any Gemini-proposed source must still pass the project source-rights policy before ingestion. Gemini does not define the frontend payload shape; the Spring Boot application contract and this spec do.

Gemini is not a parallel frontend implementer unless explicitly assigned a separate non-overlapping task.

Codex and OpenCode must not edit the same task concurrently.

---

# 16. Phase sequence after Phase 1

- Phase 2: Authentication
- Phase 3: Student Dashboard
- Phase 4: Reading
- Phase 5: Listening
- Phase 6: Writing
- Phase 7: Speaking
- Phase 8: AI Tutor + RAG integration (approved-source pipeline, pgvector retrieval, normalized citations/grounding contract, output guardrails)
- Phase 9: Admin
- Phase 10: UX polish, responsive QA, motion tuning, performance and accessibility audit

The homepage created in Phase 1 must therefore act as the visual and interaction foundation for the full four-skill platform rather than a Writing-only landing page.

# IELTS AI Tutor Frontend Foundation + Premium Homepage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the React frontend foundation and a polished Phase 1 premium homepage for the four-skill IELTS AI Tutor research platform, with guest/member states, motion, radar progress, and a floating AI Tutor shell.

**Architecture:** Use a small React + Vite app organized by shared primitives, layout, homepage sections, chart, motion, tutor, and centralized mock data. The homepage is presentation-only: routes and interactions work locally, while authentication, Spring Boot APIs, AI providers, and real IELTS workflows remain out of scope. Accessibility and reduced-motion behavior are first-class constraints.

**Tech Stack:** React + Vite, Tailwind CSS, Framer Motion, React Router, Recharts, Vitest, Testing Library.

**Spec:** `/mnt/data/ielts-ai-tutor-design.md`

## Global Constraints

- Official project name: **Nghiên cứu và xây dựng Nền tảng Web Trợ giảng AI hỗ trợ luyện thi IELTS 4 kỹ năng trực tuyến**.
- Frontend: React + Vite.
- Styling: Tailwind CSS.
- Motion: Framer Motion.
- Routing: React Router.
- Charting for Phase 1: Recharts.
- Visual direction: **Academic Luxury**; dark navy surfaces, restrained gold accents, Playfair Display headings, Inter body/UI.
- Phase 1 must visibly support Reading, Listening, Writing, and Speaking.
- No real backend/API calls in Phase 1.
- No real authentication in Phase 1.
- No AI provider key or secret in frontend code.
- RAG is described as grounding/traceability and hallucination-risk reduction, never as eliminating hallucinations.
- Do not fabricate AI/examiner credentials in UI copy or mock responses.
- Do not hard-code unverified commercial/internal source names as ingested knowledge-base content.
- Tutor UI reserves normalized `grounding`, `citations`, and estimated-score disclaimer fields for future Spring Boot responses.
- Mock values live in centralized mock data, not inside presentation components.
- Parallax applies only to decorative layers and is reduced/disabled on mobile and reduced-motion settings.
- Skeleton placeholders replace spinners for AI/data loading states.
- Liquid hover remains decorative and never weakens keyboard focus visibility.
- No horizontal scrolling at standard desktop/tablet/mobile widths.

## Review Focus

1. **Narrow mobile viewport (320–375px):** no horizontal overflow; hero search, CTAs, cards, dashboard, and tutor panel remain usable. Covered in Task 8 verification and responsive CSS assertions in Tasks 3–7.
2. **`prefers-reduced-motion: reduce`:** parallax and reveal movement stop; skeleton shimmer and liquid sweep simplify. Covered by Task 2 tests.
3. **Keyboard-only tutor use:** opening moves focus into the dialog, `Escape` closes it, closing returns focus to the floating button. Covered by Task 7 tests.
4. **Guest state:** fake personal bands/mistakes/countdown are never presented as real user data. Covered by Task 6 tests.
5. **Grounded-tutor trust state:** mock answers can show generic approved-source chips, while `insufficient_context` is shown when evidence is absent; no unverified Cambridge/British Council ingestion claim appears. Covered by Task 7 tests.

---

## File Map

Create the project under:

```text
ielts-ai-tutor/
  AGENTS.md
  docs/
    UI_SPEC.md
    TASKS.md
  frontend/
    package.json
    vite.config.js
    src/
      App.jsx
      main.jsx
      styles/globals.css
      test/setup.js
      components/
        common/
          Button.jsx
          GlassCard.jsx
          SectionTitle.jsx
          AnimatedSection.jsx
          SkeletonBlock.jsx
        motion/
          ParallaxLayer.jsx
        layout/
          AppLayout.jsx
          Navbar.jsx
          Footer.jsx
        charts/
          BandRadarChart.jsx
        home/
          HeroSection.jsx
          QuickPracticeSearch.jsx
          SkillCard.jsx
          SkillCardsSection.jsx
          ProgressOverviewSection.jsx
          ExamCountdownCard.jsx
          CommonMistakesWidget.jsx
          TutorPreviewSection.jsx
          CTASection.jsx
        tutor/
          FloatingTutorButton.jsx
          TutorChatPanel.jsx
          TutorMessageSkeleton.jsx
          TutorGroundingBadge.jsx
          TutorSourceChips.jsx
      data/homepageMockData.js
      pages/
        HomePage.jsx
        PlaceholderPage.jsx
      __tests__/
        app.test.jsx
        shared-ui.test.jsx
        hero.test.jsx
        skills.test.jsx
        progress.test.jsx
        tutor.test.jsx
```

Responsibility boundaries:

- `components/common/*`: reusable visual primitives only; no IELTS page logic.
- `components/motion/*`: reusable motion behavior only; no business state.
- `components/home/*`: homepage-specific presentation and interaction.
- `components/charts/*`: chart rendering and accessible text summary only.
- `components/tutor/*`: local chat shell plus normalized grounding/source presentation only; no provider/API/vector DB code.
- `data/homepageMockData.js`: all Phase 1 demo data.
- `HomePage.jsx`: section composition + demo state selection only.

---

### Task 1: Scaffold the frontend and lock project conventions

**Files:**
- Create: `ielts-ai-tutor/AGENTS.md`
- Create: `ielts-ai-tutor/docs/UI_SPEC.md`
- Create: `ielts-ai-tutor/docs/TASKS.md`
- Create: `ielts-ai-tutor/frontend/*` via Vite
- Modify: `ielts-ai-tutor/frontend/package.json`
- Modify: `ielts-ai-tutor/frontend/vite.config.js`
- Create: `ielts-ai-tutor/frontend/src/test/setup.js`
- Create: `ielts-ai-tutor/frontend/src/styles/globals.css`
- Test: `ielts-ai-tutor/frontend/src/__tests__/app.test.jsx`

**Interfaces:**
- Produces: runnable Vite app, Tailwind CSS pipeline, router-ready app, Vitest test environment, global design tokens.
- Consumes: approved design spec only.

- [ ] **Step 1: Create the project structure and Vite app**

Run from the parent workspace:

```bash
mkdir ielts-ai-tutor
cd ielts-ai-tutor
mkdir -p docs
npm create vite@latest frontend -- --template react
cd frontend
npm install
npm install react-router-dom framer-motion recharts
npm install -D tailwindcss @tailwindcss/vite vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

Expected: `frontend/` starts with `npm run dev` and package installation exits successfully.

- [ ] **Step 2: Configure Vite, Tailwind, and Vitest**

Set `frontend/vite.config.js` to:

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    css: true,
  },
})
```

Add package scripts:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "lint": "eslint .",
    "preview": "vite preview",
    "test": "vitest run"
  }
}
```

- [ ] **Step 3: Add test environment shims**

Create `src/test/setup.js`:

```js
import '@testing-library/jest-dom'

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

window.ResizeObserver = ResizeObserverMock
```

- [ ] **Step 4: Write the initial failing render test**

Create `src/__tests__/app.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'

test('renders the IELTS AI Tutor homepage route', () => {
  render(
    <MemoryRouter initialEntries={['/']}>
      <App />
    </MemoryRouter>,
  )

  expect(screen.getByRole('main')).toBeInTheDocument()
})
```

- [ ] **Step 5: Run the test and verify it fails before the app shell exists**

Run:

```bash
npm test -- --run src/__tests__/app.test.jsx
```

Expected: FAIL because the final app/router shell is not implemented yet.

- [ ] **Step 6: Add global design tokens and typography**

Create `src/styles/globals.css`:

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@500;600;700&display=swap');
@import "tailwindcss";

:root {
  --bg: #060b16;
  --surface-1: #0c1424;
  --surface-2: #101b30;
  --navy: #071426;
  --deep-navy: #102746;
  --gold: #cfae67;
  --gold-light: #e5c982;
  --text: #f5f7fa;
  --text-secondary: #b7c0cf;
  --muted: #697386;
  --border: rgba(229, 201, 130, 0.16);
  --glass: rgba(12, 20, 36, 0.72);
  --focus: #e5c982;
  font-family: 'Inter', system-ui, sans-serif;
  color: var(--text);
  background: var(--bg);
}

html {
  background: var(--bg);
  scroll-behavior: smooth;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 15% 10%, rgba(16, 39, 70, 0.34), transparent 34rem),
    var(--bg);
  color: var(--text);
}

* { box-sizing: border-box; }

.font-display { font-family: 'Playfair Display', Georgia, serif; }

:focus-visible {
  outline: 2px solid var(--focus);
  outline-offset: 3px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

- [ ] **Step 7: Create project instruction files**

Create `AGENTS.md` with these rules:

```md
# Agent Rules

- Read `docs/UI_SPEC.md` and `docs/TASKS.md` before editing.
- Implement only the requested task.
- Frontend stack: React + Vite + Tailwind CSS + Framer Motion + React Router.
- Recharts is allowed for the Phase 1 radar chart.
- Do not add dependencies without explicit task need.
- Do not add backend/API/auth/AI-provider code in Phase 1.
- Do not introduce new colors or font families without approval.
- Reuse shared Button, GlassCard, SectionTitle, AnimatedSection, SkeletonBlock.
- Respect `prefers-reduced-motion`.
- Keep mobile width >= 320px free of horizontal overflow.
- Run the task-specific test before each task commit. Run the full `npm test`, `npm run lint`, and `npm run build` at the end of Tasks 3 and 8.
- Final response: files changed, verification result, blockers only.
```

Copy the approved visual/phase requirements into `docs/UI_SPEC.md`. Create `docs/TASKS.md` with the task IDs `P0`, `P1-01` through `P1-08` and their acceptance criteria from the spec.

- [ ] **Step 8: Commit scaffold**

```bash
git init
git add .
git commit -m "chore: scaffold IELTS AI Tutor frontend"
```

---

### Task 2: Build shared primitives and motion foundation

**Files:**
- Create: `src/components/common/Button.jsx`
- Create: `src/components/common/GlassCard.jsx`
- Create: `src/components/common/SectionTitle.jsx`
- Create: `src/components/common/AnimatedSection.jsx`
- Create: `src/components/common/SkeletonBlock.jsx`
- Create: `src/components/motion/ParallaxLayer.jsx`
- Modify: `src/styles/globals.css`
- Test: `src/__tests__/shared-ui.test.jsx`

**Interfaces:**
- Produces:
  - `Button({ variant, size, className, children, ...props })`
  - `GlassCard({ interactive, className, children, ...props })`
  - `SectionTitle({ eyebrow, title, description, align })`
  - `AnimatedSection({ children, className, delay })`
  - `SkeletonBlock({ className, label })`
  - `ParallaxLayer({ children, distance, className })`
- Consumes: global CSS variables from Task 1.

- [ ] **Step 1: Write failing primitive tests**

Create `src/__tests__/shared-ui.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import Button from '../components/common/Button'
import SkeletonBlock from '../components/common/SkeletonBlock'
import AnimatedSection from '../components/common/AnimatedSection'

test('primary button is keyboard-focusable and keeps its button semantics', () => {
  render(<Button variant="primary">Bắt đầu</Button>)
  expect(screen.getByRole('button', { name: 'Bắt đầu' })).toBeEnabled()
})

test('skeleton exposes a meaningful busy label without noisy decorative content', () => {
  render(<SkeletonBlock label="Đang tải dữ liệu tiến độ" />)
  expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
  expect(screen.getByText('Đang tải dữ liệu tiến độ')).toHaveClass('sr-only')
})

test('animated section still renders content when reduced motion is requested', () => {
  window.matchMedia = vi.fn().mockImplementation((query) => ({
    matches: query.includes('prefers-reduced-motion'),
    addEventListener: () => {},
    removeEventListener: () => {},
  }))
  render(<AnimatedSection>Readable content</AnimatedSection>)
  expect(screen.getByText('Readable content')).toBeVisible()
})
```

- [ ] **Step 2: Run tests and verify failure**

```bash
npm test -- --run src/__tests__/shared-ui.test.jsx
```

Expected: FAIL because shared components do not exist.

- [ ] **Step 3: Implement `Button` with liquid hover hook classes**

Create `Button.jsx`:

```jsx
const variants = {
  primary: 'btn-liquid bg-[var(--gold)] text-[#071426] border-[var(--gold-light)] shadow-[0_12px_36px_rgba(207,174,103,.18)]',
  secondary: 'btn-liquid bg-white/5 text-[var(--text)] border-[var(--border)]',
  ghost: 'bg-transparent text-[var(--text-secondary)] border-transparent hover:text-[var(--text)]',
}

const sizes = {
  sm: 'h-9 px-4 text-sm',
  md: 'h-11 px-5 text-sm',
  lg: 'h-13 px-6 text-base',
}

export default function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...props
}) {
  return (
    <button
      className={`relative inline-flex items-center justify-center overflow-hidden rounded-full border font-semibold transition duration-300 ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    >
      <span className="relative z-10">{children}</span>
    </button>
  )
}
```

Add liquid-hover CSS:

```css
.btn-liquid::before {
  content: '';
  position: absolute;
  inset: -2px;
  transform: translateX(-120%) skewX(-18deg);
  background: linear-gradient(100deg, transparent 20%, rgba(255,255,255,.38) 50%, transparent 80%);
  transition: transform .3s ease;
  pointer-events: none;
}

.btn-liquid:hover::before { transform: translateX(120%) skewX(-18deg); }
.btn-liquid:hover { box-shadow: 0 0 0 1px rgba(229,201,130,.22), 0 14px 40px rgba(207,174,103,.2); }
```

- [ ] **Step 4: Implement `GlassCard`, `SectionTitle`, and `SkeletonBlock`**

Use shared token-driven classes. `GlassCard` must accept `interactive` and only add hover movement when true. `SkeletonBlock` must render:

```jsx
export default function SkeletonBlock({ className = '', label = 'Đang tải' }) {
  return (
    <div role="status" aria-busy="true" className={className}>
      <span className="sr-only">{label}</span>
      <div aria-hidden="true" className="skeleton h-full w-full rounded-[inherit]" />
    </div>
  )
}
```

Add skeleton CSS:

```css
.skeleton {
  background: linear-gradient(90deg, rgba(255,255,255,.045) 25%, rgba(255,255,255,.085) 50%, rgba(255,255,255,.045) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

@keyframes skeleton-shimmer {
  to { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .btn-liquid::before { display: none; }
  .skeleton { animation: none; background: rgba(255,255,255,.055); }
}
```

- [ ] **Step 5: Implement `AnimatedSection` and `ParallaxLayer`**

Use Framer Motion `useReducedMotion`, `useScroll`, and `useTransform`. `ParallaxLayer` must return static content when reduced motion is enabled, and use a small `distance` default of `18` pixels.

- [ ] **Step 6: Run shared tests**

```bash
npm test -- --run src/__tests__/shared-ui.test.jsx
```

Expected: PASS.

- [ ] **Step 7: Commit shared UI foundation**

```bash
git add src/components src/styles/globals.css src/__tests__/shared-ui.test.jsx
git commit -m "feat: add shared premium UI primitives"
```

---

### Task 3: Build the app shell, navbar, footer, routes, and mock data

**Files:**
- Create: `src/components/layout/AppLayout.jsx`
- Create: `src/components/layout/Navbar.jsx`
- Create: `src/components/layout/Footer.jsx`
- Create: `src/pages/HomePage.jsx`
- Create: `src/pages/PlaceholderPage.jsx`
- Create: `src/data/homepageMockData.js`
- Modify: `src/App.jsx`
- Modify: `src/main.jsx`
- Modify: `src/__tests__/app.test.jsx`

**Interfaces:**
- Produces:
  - routes `/`, `/assessment`, `/practice/:skill`, `/login`
  - demo state selection via `?demo=member`; default is guest
  - centralized exports `skillCards`, `memberDemo`, `guestDemo`, `commonMistakes`
- Consumes: shared primitives from Task 2.

- [ ] **Step 1: Extend app test for routing and guest-default state**

Add assertions that `/` renders the main shell and that guest mode exposes a `Bắt đầu đánh giá`/assessment call to action without fake personal band values.

- [ ] **Step 2: Run app test and verify failure**

```bash
npm test -- --run src/__tests__/app.test.jsx
```

- [ ] **Step 3: Add centralized mock data**

Create `homepageMockData.js` with:

```js
export const skillCards = [
  { id: 'reading', name: 'Reading', description: 'Luyện đọc theo dạng đề và hỏi AI vì sao đáp án đúng hoặc sai.', metric: '40 bộ đề mẫu' },
  { id: 'listening', name: 'Listening', description: 'Luyện nghe theo cấu trúc IELTS và xem giải thích theo ngữ cảnh.', metric: '32 bộ đề mẫu' },
  { id: 'writing', name: 'Writing', description: 'Task 1 & 2 với môi trường viết, band ước lượng và phản hồi theo rubric.', metric: '24 chủ đề mẫu' },
  { id: 'speaking', name: 'Speaking', description: 'Mô phỏng Part 1–3, sẵn sàng cho STT và phản hồi AI ở phase sau.', metric: '18 chủ đề mẫu' },
]

export const memberDemo = {
  user: { firstName: 'Đăng', targetBand: 7, examDate: '2026-12-20' },
  progress: [
    { skill: 'Reading', band: 6.5 },
    { skill: 'Listening', band: 7.0 },
    { skill: 'Writing', band: 6.0 },
    { skill: 'Speaking', band: 6.5 },
  ],
  mistakes: [
    { label: 'Article usage', frequency: 8, skill: 'Writing', hint: 'Kiểm tra a/an/the theo danh từ đếm được.' },
    { label: 'Subject–verb agreement', frequency: 5, skill: 'Writing', hint: 'Soát chủ ngữ số ít trước khi chia động từ.' },
    { label: 'Weak topic sentence', frequency: 3, skill: 'Writing', hint: 'Nêu rõ luận điểm chính ngay đầu đoạn.' },
  ],
}

export const guestDemo = { user: null, progress: null, mistakes: [] }
```

- [ ] **Step 4: Implement layout and routes**

`App.jsx` must define router routes without wrapping another router when tests use `MemoryRouter`.

`HomePage.jsx` reads `useSearchParams()` and selects:

```js
const isMemberDemo = searchParams.get('demo') === 'member'
const homepageState = isMemberDemo ? memberDemo : guestDemo
```

`HomePage.jsx` must remain a composition file rather than containing complete section markup.

- [ ] **Step 5: Implement responsive Navbar and Footer**

Navbar links: `Trang chủ`, `4 kỹ năng`, `Trợ giảng AI`, `Tiến độ`. Provide a lightweight mobile menu with an accessible toggle label. Footer includes the short UI brand `IELTS AI Tutor` and the four-skill scope.

- [ ] **Step 6: Run tests, lint, and build**

```bash
npm test
npm run lint
npm run build
```

Expected: all pass.

- [ ] **Step 7: Commit shell**

```bash
git add src
git commit -m "feat: add homepage shell and demo state"
```

---

### Task 4: Implement the premium Hero and quick practice search

**Files:**
- Create: `src/components/home/HeroSection.jsx`
- Create: `src/components/home/QuickPracticeSearch.jsx`
- Modify: `src/pages/HomePage.jsx`
- Test: `src/__tests__/hero.test.jsx`

**Interfaces:**
- Produces:
  - `HeroSection()`
  - `QuickPracticeSearch({ onSubmit })`
- Consumes: `Button`, `GlassCard`, `ParallaxLayer`, router navigation.

- [ ] **Step 1: Write failing Hero tests**

Test exact slogan:

```jsx
expect(screen.getByRole('heading', {
  name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền',
})).toBeInTheDocument()
```

Test CTA `Làm bài Test đánh giá năng lực ngay` exists. Test whitespace-only search does not navigate. Test `Enter` with `IELTS Writing Task 1 Line Graph` calls `onSubmit` once with the trimmed value.

- [ ] **Step 2: Run Hero tests and verify failure**

```bash
npm test -- --run src/__tests__/hero.test.jsx
```

- [ ] **Step 3: Implement `QuickPracticeSearch`**

Use a semantic `<form role="search">`, labeled input, inline SVG search icon, submit button, and four suggestion chips:

- IELTS Writing Task 1 Line Graph
- Reading True / False / Not Given
- Listening Map Labelling
- Speaking Part 2 Technology

Submit logic:

```js
const value = query.trim()
if (!value) return
onSubmit(value)
```

- [ ] **Step 4: Implement `HeroSection`**

Required copy:

- Slogan exactly as approved.
- Supporting text mentions 4 skills, contextual AI tutor, personalized feedback, and 24/7 availability in 1–2 concise sentences.
- Primary CTA navigates to `/assessment`.
- Secondary CTA anchors to `#skills`.

Decorative layers use `ParallaxLayer` only, with distances at or below 24px. Keep interactive controls outside moving layers.

- [ ] **Step 5: Run Hero tests**

```bash
npm test -- --run src/__tests__/hero.test.jsx
```

Expected: PASS.

- [ ] **Step 6: Commit Hero**

```bash
git add src/components/home src/pages/HomePage.jsx src/__tests__/hero.test.jsx
git commit -m "feat: add premium IELTS hero"
```

---

### Task 5: Add the four skill cards

**Files:**
- Create: `src/components/home/SkillCard.jsx`
- Create: `src/components/home/SkillCardsSection.jsx`
- Modify: `src/pages/HomePage.jsx`
- Test: `src/__tests__/skills.test.jsx`

**Interfaces:**
- Produces: `SkillCard({ skill })`, `SkillCardsSection({ skills })`.
- Consumes: `skillCards`, `GlassCard`, `AnimatedSection`, React Router links.

- [ ] **Step 1: Write failing skill-card tests**

Render `SkillCardsSection` and assert the four headings `Reading`, `Listening`, `Writing`, `Speaking` are present exactly once. Assert each card includes a `Luyện tập` link to `/practice/<skill-id>`.

- [ ] **Step 2: Run skill tests and verify failure**

```bash
npm test -- --run src/__tests__/skills.test.jsx
```

- [ ] **Step 3: Implement card components**

Use one consistent card system. Each card contains inline SVG icon, name, one-line/short description, mock metric clearly styled as demo content, and `Luyện tập` CTA. Grid rules:

```text
mobile: 1 column
tablet: 2 columns
desktop: 4 columns
```

Hover: max `translateY(-6px)` and `scale(1.01)` with 250–300ms transition.

- [ ] **Step 4: Run skill tests**

```bash
npm test -- --run src/__tests__/skills.test.jsx
```

Expected: PASS.

- [ ] **Step 5: Commit skill cards**

```bash
git add src/components/home src/pages/HomePage.jsx src/__tests__/skills.test.jsx
git commit -m "feat: add four IELTS skill cards"
```

---

### Task 6: Build guest/member progress overview and radar chart

**Files:**
- Create: `src/components/charts/BandRadarChart.jsx`
- Create: `src/components/home/ProgressOverviewSection.jsx`
- Create: `src/components/home/ExamCountdownCard.jsx`
- Create: `src/components/home/CommonMistakesWidget.jsx`
- Modify: `src/pages/HomePage.jsx`
- Test: `src/__tests__/progress.test.jsx`

**Interfaces:**
- Produces:
  - `BandRadarChart({ data })`
  - `ExamCountdownCard({ examDate })`
  - `CommonMistakesWidget({ mistakes })`
  - `ProgressOverviewSection({ isAuthenticated, state })`
- Consumes: `memberDemo`, `guestDemo`, `GlassCard`, Recharts.

- [ ] **Step 1: Write failing progress tests**

Member test: render with `memberDemo`; assert accessible summary contains all four values, e.g. `Reading 6.5`, `Listening 7`, `Writing 6`, `Speaking 6.5`; assert `Lỗi thường gặp tuần này` is visible.

Guest test: render guest state; assert `Xem tiến độ cá nhân sau khi bắt đầu đánh giá` is visible and `Reading 6.5` is absent.

- [ ] **Step 2: Run progress tests and verify failure**

```bash
npm test -- --run src/__tests__/progress.test.jsx
```

- [ ] **Step 3: Implement `BandRadarChart`**

Use Recharts `ResponsiveContainer`, `RadarChart`, `PolarGrid`, `PolarAngleAxis`, `PolarRadiusAxis`, and `Radar`. Set domain `[0, 9]`. Use CSS-variable-compatible colors or fixed approved token hex values only. Include an `.sr-only` text summary generated from `data` so the chart is understandable without vision.

- [ ] **Step 4: Implement countdown and mistakes widgets**

`ExamCountdownCard` computes remaining whole days from current date to `examDate`, never showing a negative number; if the date is past, show `0 ngày` and `Ngày thi đã đến hoặc đã qua`.

`CommonMistakesWidget` maps centralized mock mistakes and uses calm diagnostic copy, not warning-red styling.

- [ ] **Step 5: Implement guest fallback**

Guest mode renders a preview glass card explaining that personalized band, countdown, and common-mistake analytics appear after assessment/login. It must not render `memberDemo` score values.

- [ ] **Step 6: Run progress tests**

```bash
npm test -- --run src/__tests__/progress.test.jsx
```

Expected: PASS.

- [ ] **Step 7: Commit progress section**

```bash
git add src/components/charts src/components/home src/pages/HomePage.jsx src/__tests__/progress.test.jsx
git commit -m "feat: add four-skill progress overview"
```

---

### Task 7: Add the floating AI Tutor and skeleton loading

**Files:**
- Create: `src/components/tutor/FloatingTutorButton.jsx`
- Create: `src/components/tutor/TutorChatPanel.jsx`
- Create: `src/components/tutor/TutorMessageSkeleton.jsx`
- Create: `src/components/tutor/TutorGroundingBadge.jsx`
- Create: `src/components/tutor/TutorSourceChips.jsx`
- Create: `src/components/home/TutorPreviewSection.jsx`
- Create: `src/components/home/CTASection.jsx`
- Modify: `src/pages/HomePage.jsx`
- Test: `src/__tests__/tutor.test.jsx`

**Interfaces:**
- Produces:
  - `FloatingTutorButton({ onClick, buttonRef })`
  - `TutorChatPanel({ open, onClose, triggerRef })`
  - `TutorMessageSkeleton()`
  - `TutorGroundingBadge({ status, sourceCount })`
  - `TutorSourceChips({ citations })`
- Consumes: `Button`, `GlassCard`, `SkeletonBlock`, Framer Motion, normalized mock tutor response data from `homepageMockData.js`.

- [ ] **Step 1: Write failing tutor tests**

Use `userEvent` to test:

1. Clicking `Mở Trợ giảng AI` opens a dialog named `Trợ giảng AI`.
2. Focus moves to the text input or first interactive control inside the dialog.
3. Pressing `Escape` closes the dialog.
4. Focus returns to the floating tutor button.
5. Typing a local message and pressing send displays the user message, then renders a skeleton response state without calling `fetch`.
6. A mock grounded response renders `Dựa trên nguồn tham chiếu đã kiểm chứng` and at least one generic source chip such as `Rubric Writing Task 2`.
7. An `insufficient_context` mock renders a calm message asking for more context and does not invent citations.
8. No text claims that RAG eliminates hallucinations or that the AI is a certified/former examiner.

- [ ] **Step 2: Run tutor tests and verify failure**

```bash
npm test -- --run src/__tests__/tutor.test.jsx
```

- [ ] **Step 3: Implement the homepage AI Tutor value preview and final CTA**

Create `TutorPreviewSection.jsx` with a short explanation that the contextual tutor can explain Reading/Listening answers, discuss Writing feedback, and support Speaking practice. Include three non-interactive example prompt chips matching the tutor scope.

Create `CTASection.jsx` with heading `Sẵn sàng bắt đầu hành trình IELTS?`, supporting copy about the four-skill assessment, primary link/button to `/assessment`, and secondary anchor back to `#skills`. Both components use existing shared primitives and contain no provider/API code.

- [ ] **Step 4: Implement floating button and accessible dialog shell**

Desktop panel width: `min(420px, calc(100vw - 2rem))`; max height about `72vh`. Mobile: fixed inset with bottom-sheet/near-full-screen layout. Lock background body scroll while open and restore it on close.

Prompt chips:

- `Giải thích lỗi Writing của tôi`
- `Vì sao đáp án Reading này sai?`
- `Luyện Speaking Part 2`

Add a small non-dominant grounding badge area below assistant messages. Use only generic mock source titles until source rights are confirmed; do not display “Cambridge IELTS 10–18” or “British Council internal” as ingested data in Phase 1.

- [ ] **Step 5: Implement local fake-response state using skeletons**

Do not call any API. After a local send, add the user message and show `TutorMessageSkeleton` for a short demo-only state; a static helper response may replace it after a small `setTimeout`. Clear the timeout during unmount.

Centralize two demo response shapes in `homepageMockData.js`:

```js
export const tutorGroundedDemo = {
  content: 'Mình sẽ giải thích dựa trên rubric và ngữ cảnh bài đang luyện.',
  grounding: { status: 'grounded', sourceCount: 2 },
  citations: [
    { sourceId: 'demo-rubric-01', title: 'Rubric Writing Task 2', section: 'Task Response' },
    { sourceId: 'demo-guide-01', title: 'Hướng dẫn cải thiện lập luận', section: 'Phát triển ý' },
  ],
}

export const tutorInsufficientDemo = {
  content: 'Chưa đủ ngữ cảnh để trả lời chắc chắn. Hãy cung cấp câu hỏi, đoạn văn hoặc bài làm liên quan.',
  grounding: { status: 'insufficient_context', sourceCount: 0 },
  citations: [],
}
```

`TutorGroundingBadge` renders a positive neutral badge for `grounded`, and a muted `Cần thêm ngữ cảnh` state for `insufficient_context`. `TutorSourceChips` renders source title/section only; no long source excerpts in Phase 1.

- [ ] **Step 6: Run tutor tests**

```bash
npm test -- --run src/__tests__/tutor.test.jsx
```

Expected: PASS and no network call.

- [ ] **Step 7: Commit tutor shell**

```bash
git add src/components/tutor src/components/home src/pages/HomePage.jsx src/__tests__/tutor.test.jsx
git commit -m "feat: add floating AI tutor shell"
```

---

### Task 8: Final responsive, accessibility, motion, and production verification

**Files:**
- Modify only files with confirmed issues discovered by checks.
- Update: `docs/TASKS.md` to mark completed Phase 0/1 tasks.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: Phase 1 release candidate with passing tests/lint/build.

- [ ] **Step 1: Run the full automated suite**

```bash
cd ielts-ai-tutor/frontend
npm test
npm run lint
npm run build
```

Expected: all commands exit `0`.

- [ ] **Step 2: Start the app for manual verification**

```bash
npm run dev -- --host 0.0.0.0
```

Verify both states:

```text
http://localhost:5173/
http://localhost:5173/?demo=member
```

- [ ] **Step 3: Verify viewport behavior**

Check at minimum:

```text
360 × 800
768 × 1024
1366 × 768
1440 × 900
```

Acceptance:

- no horizontal overflow
- hero title/search/CTA remain readable
- 4 skill cards become 1/2/4 columns appropriately
- dashboard stacks on smaller screens
- tutor panel fits the viewport
- body does not scroll behind open mobile tutor panel

- [ ] **Step 4: Verify reduced motion**

Enable OS/browser reduced motion and confirm:

- parallax stops
- section reveals become static or simple fades
- skeleton shimmer becomes static/minimal
- liquid sweep is removed
- no functionality changes

- [ ] **Step 5: Verify keyboard navigation**

Using keyboard only:

- navigate Navbar, search, CTA, skill CTAs
- open tutor
- type into tutor input
- close with Escape
- confirm focus returns to floating tutor button
- confirm focus rings remain clearly visible over gold hover effects

- [ ] **Step 6: Verify no forbidden integration exists**

Run:

```bash
grep -RInE "OPENAI|GEMINI_API|AZURE.*KEY|SAMBA|GROQ.*KEY|fetch\(|axios" src || true
```

Expected: no provider secret, backend URL, or real network integration in Phase 1. Any `fetch`/`axios` result must be removed unless it is unrelated generated tooling outside `src`.

Also run:

```bash
grep -RInE "former IELTS examiner|15 years|eliminates hallucinations|Cambridge IELTS 10-18|British Council internal" src || true
```

Expected: no matches in product copy or mock knowledge-base claims.

- [ ] **Step 7: Final verification rerun after fixes**

```bash
npm test && npm run lint && npm run build
```

Expected: PASS / exit `0` for all commands.

- [ ] **Step 8: Commit release candidate**

```bash
git add .
git commit -m "feat: complete premium IELTS homepage"
```

---

## Completion Report Format

The implementing agent must return only:

```text
PHASE 0-1 STATUS: PASS | BLOCKED
Tests: <result>
Lint: <result>
Build: <result>
Files changed: <short list or count>
Blockers: <none or concise blocker>
```

Do not include a long narrative unless a blocker requires explanation.

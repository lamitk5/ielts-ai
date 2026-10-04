# UI/UX-1C Speaking Experience and Learner Motivation Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Create an immersive but truthful Speaking room and a data-grounded learner dashboard that consumes AI Phase 2 intelligence without implementing STT, audio storage, or a second learning analytics system.

**Architecture:** Consume UI/UX-1A theme/preferences/AuthGate/TutorShell primitives and the UI/UX-1B workspace/reference contracts where relevant. Speaking remains text-first with optional local microphone permission and amplitude visualization only. Dashboard cards are adapters over trusted Phase 2 profile, issue, roadmap, and learning-event APIs; missing data renders neutral empty states.

**Tech Stack:** React, React Router, Vitest, Testing Library, existing CSS tokens, Framer Motion only where already used and reduced-motion safe, Web Audio API for local amplitude only, Spring Boot learning APIs only when required by the approved Phase 2 contracts.

**Spec:** docs/superpowers/specs/2026-09-26-ui-ux-phase-1-learning-experience-design.md

## Global Constraints

- Dependency order is UI/UX-1A → UI/UX-1B → UI/UX-1C.
- Use existing Academic Luxury tokens, shared Button/GlassCard/AnimatedSection/SectionTitle/SkeletonBlock, and existing AuthProvider.
- Speaking states must reflect local capability only: READY, PROMPT, PREPARATION, RECORDING_LOCAL, TEXT_RESPONSE, MIC_PERMISSION_DENIED, MIC_UNAVAILABLE, CANCELLED, ERROR.
- Do not add persistent audio upload/storage, server audio processing, STT, transcription, pronunciation scoring, audio-based fluency scoring, audio-derived bands, or AI feedback claiming it heard audio.
- Local microphone use is limited to permission, local amplitude visualization, and future-ready shell behavior; text response remains the supported feedback path.
- Timers are practice aids and never claims of official exam infrastructure.
- Dashboard consumes AI Phase 2 profile, skills, issues/mistakes, roadmap, activity, and meaningful learning events; it does not duplicate their storage or derivation.
- Any band is labeled “Band ước lượng”; insufficient evidence is labeled “Chưa đủ dữ liệu”.
- No fake streak, fake recommendation, fake mistake, unsupported precision, or official IELTS claim.
- No 3D engine, permanent microphone loop, unbounded chat history, or giant state manager.
- All surfaces support 375px, 768px, 1024px, and 1440px+, keyboard access, focus-visible states, and reduced motion.
- External AI and Phase 2 services are mocked in tests and consume zero provider quota.

## Review Focus

1. **UI implies audio was analyzed:** only local capture/amplitude states may render; no copy or result state may imply upload, transcript, pronunciation, fluency, or audio band.
2. **Microphone denied breaks Speaking route:** permission denial/unavailability must return to the existing text path without trapping the learner.
3. **Streak counts login or screen open:** only meaningful learning events in the learner timezone count; test day boundaries and duplicate events.
4. **Dashboard invents recommendations:** Today’s Focus, mistakes, roadmap, and energy must render neutral states or server data, never frontend-generated production facts.
5. **Animation ignores reduced motion:** orb/visualizer/timers must have static or non-animated equivalents and clean up media/animation loops.

---

### Task 1: Define Speaking local-capability state model

Files:
- Create: frontend/src/features/speaking/speakingRoomState.js
- Create: frontend/src/components/speaking/SpeakingRoom.jsx
- Create: frontend/src/components/speaking/SpeakingOrb.jsx
- Create: frontend/src/components/speaking/SpeakingPromptCard.jsx
- Modify: frontend/src/pages/SpeakingPage.jsx
- Test: frontend/src/__tests__/speaking-room.test.jsx
- Modify: frontend/src/__tests__/speaking.test.jsx

Interfaces:
- Consumes: existing speaking prompts, speakingApi, SpeakingPage text submission, AuthProvider, UI/UX-1A preferences, and existing TutorShell/FloatingTutor.
- Produces: states READY | PROMPT | PREPARATION | RECORDING_LOCAL | TEXT_RESPONSE | MIC_PERMISSION_DENIED | MIC_UNAVAILABLE | CANCELLED | ERROR; SpeakingRoom({ prompt, state, onStateChange, onTextResponse, onPermissionState }).

- [ ] Step 1: Write failing tests for Part 1/2/3 prompt selection, state transitions, text response path, permission denied/unavailable fallback, and absence of transcript/audio-result claims.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/speaking-room.test.jsx src/__tests__/speaking.test.jsx; expected missing room/state behavior.
- [ ] Step 3: Implement the state model and room composition around the current SpeakingPage prompt/text APIs. Keep text response as the only feedback/submission path.
- [ ] Step 4: Ensure RECORDING_LOCAL means local capture only and cannot transition to server feedback, transcription, or band state.
- [ ] Step 5: Run and verify GREEN with targeted Speaking tests.
- [ ] Step 6: Commit with git add frontend/src/features/speaking frontend/src/components/speaking frontend/src/pages/SpeakingPage.jsx frontend/src/__tests__/speaking-room.test.jsx frontend/src/__tests__/speaking.test.jsx && git commit -m "feat: add truthful Speaking room states".

### Task 2: Add examiner orb, preparation timer, and speaking timer

Files:
- Create: frontend/src/components/speaking/SpeakingTimer.jsx
- Create: frontend/src/features/speaking/useSpeakingTimer.js
- Modify: frontend/src/components/speaking/SpeakingOrb.jsx
- Modify: frontend/src/components/speaking/SpeakingRoom.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/speaking-timer.test.jsx

Interfaces:
- Consumes: Speaking room state, timer preference from UI/UX-1A, and prefers-reduced-motion media query.
- Produces: SpeakingTimer({ durationSeconds, mode, running, onComplete }), timer modes PREPARATION | PRACTICE; SpeakingOrb({ state, reducedMotion }).

- [ ] Step 1: Write failing tests for Part 2 preparation timing, practice timing, pause/cancel/reset, no negative display, timer preference, cleanup on unmount, and reduced-motion static orb.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/speaking-timer.test.jsx; expected missing timer hook/components.
- [ ] Step 3: Implement timer state with interval cleanup, bounded zero display, and practice-only labels. Use CSS transforms/opacity for the orb, not a 3D engine.
- [ ] Step 4: Add reduced-motion behavior that removes pulse/transition animation while retaining state color, label, and focus semantics.
- [ ] Step 5: Run and verify GREEN with targeted timer tests and accessibility checks.
- [ ] Step 6: Commit with git add frontend/src/components/speaking frontend/src/features/speaking frontend/src/styles/globals.css frontend/src/__tests__/speaking-timer.test.jsx && git commit -m "feat: add Speaking timers and orb states".

### Task 3: Add microphone permission boundary and local amplitude visualizer

Files:
- Create: frontend/src/components/speaking/MicrophonePermissionState.jsx
- Create: frontend/src/components/speaking/LocalAudioVisualizer.jsx
- Create: frontend/src/features/speaking/useLocalAudioAmplitude.js
- Modify: frontend/src/components/speaking/SpeakingRoom.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/local-audio-visualizer.test.jsx

Interfaces:
- Consumes: navigator.mediaDevices.getUserMedia when present, AudioContext/AnalyserNode when available, reduced-motion preference, and SpeakingRoom state model.
- Produces: local permission states, amplitude-only samples, cleanup function, and a visualizer that never emits transcript, pronunciation, fluency, score, or band data.

- [ ] Step 1: Write failing tests for unsupported media devices, permission denial, successful local stream setup, amplitude rendering, cleanup of tracks/AudioContext/animation frame, reduced-motion static rendering, and text fallback.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/local-audio-visualizer.test.jsx; expected missing hook/components.
- [ ] Step 3: Implement the local-only media hook with feature detection, explicit cleanup, and a requestAnimationFrame loop only while recording locally. Do not persist the stream or send it to any endpoint.
- [ ] Step 4: Implement accessible permission/error copy and a text-only path that remains usable when microphone access fails.
- [ ] Step 5: Run and verify GREEN with targeted tests; expected no audio API call beyond local permission and no server request.
- [ ] Step 6: Commit with git add frontend/src/components/speaking frontend/src/features/speaking frontend/src/styles/globals.css frontend/src/__tests__/local-audio-visualizer.test.jsx && git commit -m "feat: add local Speaking visualizer boundary".

### Task 4: Add Phase 2 learning intelligence adapters

Files:
- Create: frontend/src/services/learningIntelligenceApi.js
- Create: frontend/src/features/learning/learningIntelligenceAdapters.js
- Create: frontend/src/features/learning/learningIntelligenceState.js
- Test: frontend/src/__tests__/learning-intelligence-adapters.test.jsx
- Test: frontend/src/__tests__/learning-intelligence-state.test.jsx

Interfaces:
- Consumes: approved AI Phase 2 contracts GET /api/learning/profile, GET /api/learning/skills, GET /api/learning/issues, GET /api/learning/mistakes, GET /api/learning/roadmap, GET /api/learning/activity.
- Produces: provider-neutral adapters for profile, four skill progress records, issues, roadmap, Today’s Focus, and meaningful activity; states IDLE | LOADING | READY | EMPTY | ERROR.

- [ ] Step 1: Write failing tests for normalized Phase 2 responses, missing endpoints/data, malformed records, loading/error/empty states, estimated-band labeling, all four skills, and no frontend-derived recommendation.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/learning-intelligence-adapters.test.jsx src/__tests__/learning-intelligence-state.test.jsx; expected missing adapters.
- [ ] Step 3: Implement thin fetch/normalization adapters. They must preserve server evidence and return neutral EMPTY/ERROR states; they must not compute a second roadmap, issue taxonomy, streak, or profile.
- [ ] Step 4: Add bounded request cancellation and no fake production fallback data.
- [ ] Step 5: Run and verify GREEN with targeted adapter/state tests.
- [ ] Step 6: Commit with git add frontend/src/services/learningIntelligenceApi.js frontend/src/features/learning frontend/src/__tests__/learning-intelligence-adapters.test.jsx frontend/src/__tests__/learning-intelligence-state.test.jsx && git commit -m "feat: add learning intelligence adapters".

### Task 5: Implement Today’s Focus, roadmap, and common-mistake cards

Files:
- Create: frontend/src/components/learning/TodaysFocusCard.jsx
- Create: frontend/src/components/learning/RoadmapWidget.jsx
- Create: frontend/src/components/learning/CommonMistakesPanel.jsx
- Modify: frontend/src/components/home/CommonMistakesWidget.jsx
- Modify: frontend/src/components/home/ProgressOverviewSection.jsx
- Modify: frontend/src/pages/HomePage.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/learning-dashboard-cards.test.jsx
- Modify: frontend/src/__tests__/progress.test.jsx

Interfaces:
- Consumes: learning intelligence adapters/state, existing ProgressOverviewSection, CommonMistakesWidget, Button, GlassCard, SectionTitle, and routes to practice.
- Produces: one primary Today’s Focus CTA, roadmap current priority/next milestone/completion, 2–4 high-value server-backed mistakes, and neutral states when Phase 2 data is absent.

- [ ] Step 1: Write failing tests for card hierarchy, one primary CTA, Phase 2 data rendering, neutral empty/error state, 2–4 mistake limit, no fake data, estimated-band copy, and four-skill equality.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/learning-dashboard-cards.test.jsx src/__tests__/progress.test.jsx; expected missing cards/adapters.
- [ ] Step 3: Implement cards as display adapters only. Today’s Focus uses a server-provided roadmap/issue action; if absent, show assessment or practice invitation without inventing a recommendation.
- [ ] Step 4: Keep radar/progress composition calm and editorial, not admin-like. Common mistakes remain server-backed and route to existing practice.
- [ ] Step 5: Run and verify GREEN with targeted dashboard tests and existing homepage tests.
- [ ] Step 6: Commit with git add frontend/src/components/learning frontend/src/components/home frontend/src/pages/HomePage.jsx frontend/src/styles/globals.css frontend/src/__tests__/learning-dashboard-cards.test.jsx frontend/src/__tests__/progress.test.jsx && git commit -m "feat: add data-grounded learning dashboard cards".

### Task 6: Derive meaningful streak and skill energy presentation

Files:
- Create: frontend/src/features/learning/streakRules.js
- Create: frontend/src/components/learning/StreakCard.jsx
- Create: frontend/src/components/learning/SkillEnergyGrid.jsx
- Modify: frontend/src/components/home/ProgressOverviewSection.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/streak-rules.test.jsx
- Test: frontend/src/__tests__/skill-energy.test.jsx

Interfaces:
- Consumes: normalized Phase 2 learning activity/events and skill profile evidence.
- Produces: calculateMeaningfulStreak(events, timezone, today), SkillEnergyGrid data for Reading/Listening/Writing/Speaking, and neutral “Chưa đủ dữ liệu” states.

- [ ] Step 1: Write failing tests for meaningful event types, duplicate same-day events, learner timezone day boundary, login/homepage/Tutor/settings exclusion, missed-day behavior, 0–9 evidence scale, and insufficient-data copy.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/streak-rules.test.jsx src/__tests__/skill-energy.test.jsx; expected missing rule/grid modules.
- [ ] Step 3: Implement pure streak rules using server event timestamps converted to the learner timezone. Do not count login, homepage open, settings interaction, or merely opening Tutor.
- [ ] Step 4: Render skill energy from trusted Phase 2 evidence. Label estimates as Band ước lượng and avoid unsupported decimal precision or official claims.
- [ ] Step 5: Run and verify GREEN with targeted tests.
- [ ] Step 6: Commit with git add frontend/src/features/learning frontend/src/components/learning frontend/src/components/home/ProgressOverviewSection.jsx frontend/src/styles/globals.css frontend/src/__tests__/streak-rules.test.jsx frontend/src/__tests__/skill-energy.test.jsx && git commit -m "feat: add meaningful learning motivation signals".

### Task 7: Integrate Speaking, dashboard responsive behavior, and reduced motion

Files:
- Modify: frontend/src/pages/SpeakingPage.jsx
- Modify: frontend/src/pages/HomePage.jsx
- Modify: frontend/src/components/home/ProgressOverviewSection.jsx
- Modify: frontend/src/components/layout/AppLayout.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/speaking.test.jsx
- Test: frontend/src/__tests__/app.test.jsx
- Test: frontend/src/__tests__/rag-accessibility.test.jsx

Interfaces:
- Consumes: Tasks 1–6 plus UI/UX-1A/1B interfaces.
- Produces: integrated Speaking route and authenticated dashboard at 375/768/1024/1440+ with no horizontal overflow, no fake audio/learning data, and keyboard/reduced-motion support.

- [ ] Step 1: Write failing integration tests for route stability after microphone denial, keyboard Speaking controls, dashboard order, one primary CTA, all four skills, neutral Phase 2 states, and reduced-motion classes.
- [ ] Step 2: Run and verify RED with targeted frontend tests; expected integration failures.
- [ ] Step 3: Integrate SpeakingRoom into the current SpeakingPage without changing the existing text submission contract or adding an audio endpoint.
- [ ] Step 4: Integrate dashboard cards in the approved order: Continue Learning, Today’s Focus, Four Skill Progress, Roadmap, Common Mistakes, Study Streak/Activity.
- [ ] Step 5: Add responsive and reduced-motion CSS. Verify local visualizer cleanup and static orb behavior when reduced motion is active.
- [ ] Step 6: Run and verify GREEN with npm test -- --run, npm run lint, npm run build, and browser-capable checks at 375/768/1024/1440+ where infrastructure permits.
- [ ] Step 7: Commit with git add frontend/src && git commit -m "feat: integrate Speaking and learner motivation".

### Task 8: Whole-branch Phase 1C verification

Files:
- Test: frontend/src/__tests__/speaking-room.test.jsx
- Test: frontend/src/__tests__/local-audio-visualizer.test.jsx
- Test: frontend/src/__tests__/learning-dashboard-cards.test.jsx
- Test: frontend/src/__tests__/streak-rules.test.jsx
- Test: frontend/src/__tests__/skill-energy.test.jsx
- Test: frontend/src/__tests__/app.test.jsx
- Test: backend/src/test/java/com/ieltsaitutor/learning/LearningControllerTest.java

Interfaces:
- Consumes: Tasks 1–7 and approved AI Phase 2 API contracts.
- Produces: verified Speaking and motivation implementation with no Phase 2/3 duplication and no hidden STT dependency.

- [ ] Step 1: Write or extend failing regression assertions for every Review Focus item.
- [ ] Step 2: Run and verify RED before final integration corrections.
- [ ] Step 3: Make only scoped integration corrections; do not implement AI Phase 2 or Phase 3.
- [ ] Step 4: Run and verify GREEN with npm test -- --run, npm run lint, npm run build, .\mvnw.cmd test if backend contracts were touched, .\mvnw.cmd package if backend contracts were touched, and git diff --check.
- [ ] Step 5: Commit with git add frontend/src backend/src && git commit -m "feat: finalize Speaking and motivation experience".

## Plan-level Verification

Run after all tasks:

- npm test -- --run
- npm run lint
- npm run build
- .\mvnw.cmd test if backend contracts were touched
- .\mvnw.cmd package if backend contracts were touched
- git diff --check

Do not run live provider calls. Do not implement STT, persistent audio, AI Phase 2, or AI Phase 3.

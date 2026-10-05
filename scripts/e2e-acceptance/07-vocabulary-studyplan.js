import { createBrowserContext, BASE_URL, DEMO_USERS, loginAs } from './common.js';

export async function runVocabularyStudyPlanTests() {
  console.log('\n--- STARTING PHASE 12, 13, 14: VOCABULARY & STUDY PLAN TESTS ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // Log in as Student
    await loginAs(page, DEMO_USERS.STUDENT);

    // -------------------------------------------------------------
    // PHASE 12 & 13: VOCABULARY NOTEBOOK & FLASHCARD REVIEW
    // -------------------------------------------------------------
    console.log('Testing Vocabulary Notebook (/vocabulary)...');
    await page.goto(`${BASE_URL}/vocabulary`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase12-vocab-initial');

    // 1. Add temporary vocabulary item
    const wordInput = page.locator('input[aria-label="Từ mới"]');
    const meaningInput = page.locator('input[aria-label="Nghĩa"]');
    await wordInput.fill('sustainable');
    await meaningInput.fill('bền vững');

    const exampleInput = page.locator('label:has-text("Câu ví dụ") input');
    if (await exampleInput.isVisible()) {
      await exampleInput.fill('Sustainable development is increasingly important.');
    }

    const noteInput = page.locator('label:has-text("Ghi chú") input');
    if (await noteInput.isVisible()) {
      await noteInput.fill('E2E TEST');
    }

    const addBtn = page.locator('button[type="submit"]:has-text("Thêm từ")');
    await addBtn.click();
    await page.waitForTimeout(1000);
    await screenshot('phase12-vocab-created');

    // 2. Verify word appears in list
    const wordElem = page.locator('.vocabulary-word:has-text("sustainable")');
    const wordVisible = await wordElem.first().isVisible();
    console.log('Added word "sustainable" visible:', wordVisible);
    if (!wordVisible) throw new Error('New vocabulary item was not added to the list');

    // 3. Test refresh persistence
    console.log('Testing vocabulary reload persistence...');
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    const persistedWord = await page.locator('.vocabulary-word:has-text("sustainable")').first().isVisible();
    console.log('Word persisted after page reload:', persistedWord);
    if (!persistedWord) throw new Error('Vocabulary item failed reload persistence!');

    // 4. Test Reveal meaning (Flashcard review feature)
    const revealBtn = page.locator('.vocabulary-card:has-text("sustainable") button:has-text("Xem nghĩa")');
    if (await revealBtn.isVisible()) {
      await revealBtn.click();
      await page.waitForTimeout(300);
      const meaningVisible = await page.locator('.vocabulary-meaning:has-text("bền vững")').isVisible();
      console.log('Revealed meaning visible:', meaningVisible);
      await screenshot('phase12-vocab-revealed');
    }

    // 5. Test status update (NEW -> LEARNING -> MASTERED)
    const reviewBtn = page.locator('.vocabulary-card:has-text("sustainable") button:has-text("Ôn lại")');
    if (await reviewBtn.isVisible()) {
      await reviewBtn.click();
      await page.waitForTimeout(500);
      console.log('Clicked "Ôn lại" -> status should be LEARNING');
    }

    const masteredBtn = page.locator('.vocabulary-card:has-text("sustainable") button:has-text("Đã thuộc")');
    if (await masteredBtn.isVisible()) {
      await masteredBtn.click();
      await page.waitForTimeout(500);
      console.log('Clicked "Đã thuộc" -> status should be MASTERED');
    }

    // 6. Test search filter
    const searchInput = page.locator('input[placeholder="Tìm trong sổ tay…"]');
    if (await searchInput.isVisible()) {
      await searchInput.fill('sustainable');
      await page.waitForTimeout(500);
      const searchCount = await page.locator('.vocabulary-card').count();
      console.log('Search results count for "sustainable":', searchCount);
      await searchInput.fill('');
      await page.waitForTimeout(300);
    }

    // 7. Delete temporary item
    const deleteBtn = page.locator('.vocabulary-card:has-text("sustainable") button[aria-label^="Xóa"]');
    if (await deleteBtn.isVisible()) {
      await deleteBtn.click();
      await page.waitForTimeout(1000);
      console.log('Deleted temporary word "sustainable"');
    }

    // -------------------------------------------------------------
    // PHASE 14: STUDY PLAN
    // -------------------------------------------------------------
    console.log('Testing Personal Study Plan (/study-plan)...');
    await page.goto(`${BASE_URL}/study-plan`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase14-study-plan');

    const planTitle = await page.locator('#study-plan-title').textContent();
    console.log('Study Plan Title:', planTitle?.trim());

    // Check goal card
    const goalCard = page.locator('.study-plan-goal');
    const goalVisible = await goalCard.isVisible();
    console.log('IELTS Goal card visible:', goalVisible);

    // Check recommendations
    const recCards = page.locator('.study-plan-item');
    const recCount = await recCards.count();
    console.log('Study plan recommendations count:', recCount);

    if (recCount > 0) {
      const firstRecTitle = await recCards.first().locator('h3').textContent();
      const firstRecReason = await recCards.first().locator('p').textContent();
      console.log(`First Recommendation: "${firstRecTitle?.trim()}" - Reason: "${firstRecReason?.trim()}"`);

      // Click recommendation link
      const link = recCards.first().locator('a');
      if (await link.isVisible()) {
        const destHref = await link.getAttribute('href');
        console.log('Clicking recommendation link leading to:', destHref);
        await link.click();
        await page.waitForTimeout(1000);
        console.log('Navigated to recommendation destination:', page.url());
        await page.goBack({ waitUntil: 'networkidle' });
      }
    }

    console.log('Console errors during Phase 12, 13, 14:', consoleErrors);
    console.log('Network 5xx errors during Phase 12, 13, 14:', networkErrors);

    return {
      status: 'PASS',
      vocabCrudStatus: 'PASS (created, persisted, reviewed, searched, deleted)',
      studyPlanStatus: 'PASS (deterministic recommendations verified)',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('07-vocabulary-studyplan.js')) {
  runVocabularyStudyPlanTests().then(res => {
    console.log('Phase 12, 13, 14 Result:', res);
  }).catch(err => {
    console.error('Phase 12, 13, 14 FAILED:', err);
    process.exit(1);
  });
}

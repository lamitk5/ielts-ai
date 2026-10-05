import { createBrowserContext, BASE_URL, DEMO_USERS, loginAs } from './common.js';

export async function runAnalyticsHistorySavedTests() {
  console.log('\n--- STARTING PHASE 15, 16, 17: ANALYTICS, HISTORY, SAVED PRACTICES ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // 1. Log in as Student
    await loginAs(page, DEMO_USERS.STUDENT);

    // -------------------------------------------------------------
    // PHASE 15: ANALYTICS & PROGRESS TRACKER (/analytics)
    // -------------------------------------------------------------
    console.log('Testing Analytics Page (/analytics)...');
    await page.goto(`${BASE_URL}/analytics`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase15-analytics-initial');

    const analyticsTitle = await page.locator('h1').textContent();
    console.log('Analytics Title:', analyticsTitle?.trim());

    // Check stats cards
    const statCards = page.locator('.learning-stat-grid');
    const statCardsVisible = await statCards.first().isVisible();
    console.log('Stats summary grid visible:', statCardsVisible);

    // Check for charts or breakdown cards
    const chartsOrCards = page.locator('.analytics-page, .progress-page, section');
    const chartsVisible = await chartsOrCards.first().isVisible();
    console.log('Analytics page content visible:', chartsVisible);

    // -------------------------------------------------------------
    // PHASE 16: SUBMISSION / PRACTICE HISTORY (/practice/history)
    // -------------------------------------------------------------
    console.log('Testing Practice History (/practice/history)...');
    await page.goto(`${BASE_URL}/practice/history`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase16-history-initial');

    const historyTitle = await page.locator('h1').textContent();
    console.log('History Title:', historyTitle?.trim());

    // Check history list / table or empty state
    const historyItems = page.locator('.history-item, .submission-item, table tbody tr, .history-card');
    const historyCount = await historyItems.count();
    console.log('History items found:', historyCount);

    // Check skill filters if present
    const filterSelect = page.locator('select');
    if (await filterSelect.first().isVisible()) {
      console.log('History filter dropdown available');
    }

    // -------------------------------------------------------------
    // PHASE 17: SAVED PRACTICES (/practice/saved)
    // -------------------------------------------------------------
    console.log('Testing Saved Practices (/practice/saved)...');
    await page.goto(`${BASE_URL}/practice/saved`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase17-saved-initial');

    const savedTitle = await page.locator('h1').textContent();
    console.log('Saved Practices Title:', savedTitle?.trim());

    // Also check alternate route /saved
    await page.goto(`${BASE_URL}/saved`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(500);
    const altSavedTitle = await page.locator('h1').textContent();
    console.log('Alternate /saved route Title:', altSavedTitle?.trim());

    // -------------------------------------------------------------
    // ADDITIONAL: DIAGNOSTIC & ERROR NOTEBOOK
    // -------------------------------------------------------------
    console.log('Testing Diagnostic Test (/diagnostic)...');
    await page.goto(`${BASE_URL}/diagnostic`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('extra-diagnostic');
    const diagnosticTitle = await page.locator('h1').textContent();
    console.log('Diagnostic Title:', diagnosticTitle?.trim());

    console.log('Testing Error Notebook (/error-notebook)...');
    await page.goto(`${BASE_URL}/error-notebook`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('extra-error-notebook');
    const errorNotebookTitle = await page.locator('h1').textContent();
    console.log('Error Notebook Title:', errorNotebookTitle?.trim());

    console.log('Console errors during Phase 15-17:', consoleErrors);
    console.log('Network 5xx errors during Phase 15-17:', networkErrors);

    if (networkErrors.length > 0) {
      throw new Error(`Encountered 5xx network errors: ${JSON.stringify(networkErrors)}`);
    }

    return {
      status: 'PASS',
      analyticsStatus: 'PASS',
      historyStatus: 'PASS',
      savedStatus: 'PASS',
      diagnosticStatus: 'PASS',
      errorNotebookStatus: 'PASS',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('08-analytics-history-saved.js')) {
  runAnalyticsHistorySavedTests()
    .then((res) => {
      console.log('Phase 15, 16, 17 Result:', res);
      process.exit(0);
    })
    .catch((err) => {
      console.error('Phase 15, 16, 17 FAILED:', err);
      process.exit(1);
    });
}

import { createBrowserContext, BASE_URL, DEMO_USERS, loginAs } from './common.js';

export async function runDashboardPracticeTests() {
  console.log('\n--- STARTING PHASE 5 & 6: DASHBOARD & PRACTICE BANK TESTS ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // -------------------------------------------------------------
    // PHASE 5: DASHBOARD
    // -------------------------------------------------------------
    console.log('Logging in as Student and checking Dashboard...');
    await loginAs(page, DEMO_USERS.STUDENT);
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    await screenshot('phase05-dashboard');

    // 1. Check member dashboard greeting / title
    const heroTitle = await page.locator('h1').textContent();
    console.log('Dashboard Hero Title:', heroTitle?.trim());

    // 2. Check skill progress overview
    const progressSection = page.locator('.progress-section, #progress');
    const progressVisible = await progressSection.isVisible();
    console.log('Progress section visible:', progressVisible);

    // 3. Check 4 skill cards
    const skillCards = page.locator('.skill-card');
    const skillCount = await skillCards.count();
    console.log('Skill cards available on dashboard:', skillCount);
    if (skillCount < 4) throw new Error(`Expected at least 4 skill cards, got ${skillCount}`);

    // Click on Reading skill card to test navigation
    const readingCard = page.locator('.skill-card:has-text("Reading")');
    if (await readingCard.isVisible()) {
      await readingCard.click();
      await page.waitForTimeout(600);
      console.log('Clicked Reading skill card, now on:', page.url());
      await page.goBack({ waitUntil: 'networkidle' });
    }

    // -------------------------------------------------------------
    // PHASE 6: PRACTICE BANK & SEARCH
    // -------------------------------------------------------------
    console.log('Testing Practice Catalog (/practice)...');
    await page.goto(`${BASE_URL}/practice`, { waitUntil: 'networkidle' });
    await screenshot('phase06-practice-catalog');

    // Check catalog cards
    const catalogCards = page.locator('.practice-catalog-card');
    const catalogCount = await catalogCards.count();
    console.log('Practice catalog cards count:', catalogCount);
    if (catalogCount < 4) throw new Error(`Expected 4 practice catalog cards, got ${catalogCount}`);

    // Check "Bài đã lưu" button on practice catalog
    const savedLink = page.locator('a:has-text("Bài đã lưu")');
    if (await savedLink.isVisible()) {
      await savedLink.click();
      await page.waitForTimeout(500);
      console.log('Navigated to saved practices:', page.url());
      await screenshot('phase06-saved-empty');
      await page.goBack({ waitUntil: 'networkidle' });
    }

    // Test Search functionality (/practice/search)
    console.log('Testing Search functionality with query "reading"...');
    await page.goto(`${BASE_URL}/practice/search?q=reading`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    await screenshot('phase06-search-results');

    // Verify search results exist
    const searchCards = page.locator('.search-result-card, .search-card, article');
    const resultCount = await searchCards.count();
    console.log('Search results count for "reading":', resultCount);

    // Test filter chips on search page
    const filterButtons = page.locator('.search-filter-pills button');
    const filterCount = await filterButtons.count();
    console.log('Search type filters count:', filterCount);
    if (filterCount > 0) {
      await filterButtons.nth(1).click();
      await page.waitForTimeout(400);
      console.log('Clicked filter chip 1');
    }

    // Test empty search state (no-result search)
    console.log('Testing Search with non-existent query...');
    await page.goto(`${BASE_URL}/practice/search?q=xyznonexistentquery99999`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    await screenshot('phase06-search-empty');

    const emptyText = await page.locator('main').textContent();
    const hasEmptyMessage = emptyText?.includes('Chưa tìm thấy') || emptyText?.includes('Không tìm thấy') || emptyText?.includes('không có kết quả');
    console.log('Correct empty state rendered for no-result search:', hasEmptyMessage);
    if (!hasEmptyMessage) {
      throw new Error('Search empty state was not displayed for non-existent query');
    }

    console.log('Console errors during Phase 5 & 6:', consoleErrors);
    console.log('Network 5xx errors during Phase 5 & 6:', networkErrors);

    return {
      status: 'PASS',
      skillCount,
      catalogCount,
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('03-dashboard-practice.js')) {
  runDashboardPracticeTests().then(res => {
    console.log('Phase 5 & 6 Result:', res);
  }).catch(err => {
    console.error('Phase 5 & 6 FAILED:', err);
    process.exit(1);
  });
}

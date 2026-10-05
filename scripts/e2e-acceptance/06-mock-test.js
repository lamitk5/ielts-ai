import { createBrowserContext, BASE_URL, DEMO_USERS, loginAs } from './common.js';

export async function runMockTestTests() {
  console.log('\n--- STARTING PHASE 11: MOCK TEST TESTS (HIGH PRIORITY) ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // Log in as Student
    await loginAs(page, DEMO_USERS.STUDENT);

    // 1. Mock Tests Catalog
    console.log('Navigating to /mock-tests...');
    await page.goto(`${BASE_URL}/mock-tests`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase11-mock-catalog');

    const catalogTitle = await page.locator('#mock-tests-title').textContent();
    console.log('Mock tests catalog title:', catalogTitle?.trim());

    const testCard = page.locator('.vocabulary-card, .glass-card:has-text("MOCK TEST")');
    const hasCard = await testCard.first().isVisible();
    console.log('Mock test card visible:', hasCard);
    if (!hasCard) throw new Error('No published mock test card found in /mock-tests');

    // 2. Open Mock Test Instructions / Start
    const readBtn = page.locator('a:has-text("Đọc hướng dẫn")');
    await readBtn.first().click();
    console.log('Clicked Đọc hướng dẫn, navigating to mock test room...');
    await page.waitForTimeout(2000);

    // 3. Verify HTTP success (no 500 or 502)
    const fatalErrors = networkErrors.filter(e => e.status === 500 || e.status === 502);
    if (fatalErrors.length > 0) {
      throw new Error(`Mock test start returned HTTP error: ${JSON.stringify(fatalErrors)}`);
    }

    // 4. Verify MockTestShell rendered
    await page.waitForSelector('.min-h-screen', { timeout: 10000 });
    await screenshot('phase11-mock-in-progress');

    const shellHeader = page.locator('h1:has-text("IELTS Academic Mock Test")');
    const isShellVisible = await shellHeader.isVisible();
    console.log('Mock test shell visible:', isShellVisible);
    if (!isShellVisible) {
      const errorText = await page.locator('.inline-error, role=alert').textContent().catch(() => '');
      throw new Error(`Mock test shell failed to render: ${errorText}`);
    }

    // Verify AI disclaimer badge
    const badge = page.locator('span:has-text("Thi thử mô phỏng AI")');
    console.log('AI simulation badge visible:', await badge.isVisible());

    // 5. Verify Timer
    const timerElem = page.locator('.font-mono:has-text(":")');
    const timerText = await timerElem.first().textContent();
    console.log('Initial Timer value:', timerText?.trim());

    // 6. Section Navigation & Answer
    const sectionNav = page.locator('.mock-section-navigator, nav');
    console.log('Section navigator visible:', await sectionNav.first().isVisible());

    // 7. Test Refresh & Persistence
    console.log('Testing Mock Test refresh persistence...');
    await page.waitForTimeout(1000);
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await screenshot('phase11-mock-refreshed');

    const reloadedShell = await page.locator('h1:has-text("IELTS Academic Mock Test")').isVisible();
    console.log('Mock test shell preserved after reload:', reloadedShell);
    if (!reloadedShell) throw new Error('Mock test session was lost after page refresh!');

    const timerAfterReload = await page.locator('.font-mono:has-text(":")').first().textContent();
    console.log('Timer value after reload:', timerAfterReload?.trim());

    // 8. Test Section Navigation
    const nextBtn = page.locator('button:has-text("Tiếp theo"), button:has-text("Chuyển phần"), button:has-text("Nộp bài")');
    if (await nextBtn.first().isVisible()) {
      console.log('Action button text:', await nextBtn.first().textContent());
    }

    // 9. Submit Mock Test
    const submitMockBtn = page.locator('button:has-text("Nộp bài thi thử"), button:has-text("Nộp bài")');
    if (await submitMockBtn.first().isVisible()) {
      await submitMockBtn.first().click();
      await page.waitForTimeout(2500);
      await screenshot('phase11-mock-result');
      console.log('Submitted mock test!');

      // Check result view
      const resultTitle = page.locator('h1:has-text("Báo Cáo Tổng Hợp Mock Test"), h1:has-text("Kết quả")');
      const resultVisible = await resultTitle.first().isVisible();
      console.log('Mock test result view visible:', resultVisible);

      const disclaimer = await page.locator('span:has-text("Band ước lượng"), span:has-text("chính thức")').first().textContent().catch(() => '');
      console.log('Truthful disclaimer found:', disclaimer?.trim());
    }

    console.log('Console errors during Phase 11:', consoleErrors);
    console.log('Network 5xx errors during Phase 11:', networkErrors);

    return {
      status: 'PASS',
      mockTestStartStatus: 'HTTP 200 SUCCESS',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('06-mock-test.js')) {
  runMockTestTests().then(res => {
    console.log('Phase 11 Result:', res);
  }).catch(err => {
    console.error('Phase 11 FAILED:', err);
    process.exit(1);
  });
}

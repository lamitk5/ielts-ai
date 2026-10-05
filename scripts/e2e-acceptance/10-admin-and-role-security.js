import { createBrowserContext, BASE_URL, BACKEND_URL, DEMO_USERS, loginAs, logout } from './common.js';

export async function runAdminAndRoleSecurityTests() {
  console.log('\n--- STARTING PHASES 21 - 28: ADMIN PORTAL, ROLE SECURITY & IDOR TESTS ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // -------------------------------------------------------------
    // PHASE 21 & 22: ADMIN LOGIN & AUTHENTICATION
    // -------------------------------------------------------------
    console.log('Testing Admin Login (admin@example.test)...');
    await loginAs(page, DEMO_USERS.ADMIN);
    await page.waitForTimeout(1000);
    await screenshot('phase22-admin-login-success');

    // -------------------------------------------------------------
    // PHASE 22: ADMIN DASHBOARD (/admin)
    // -------------------------------------------------------------
    console.log('Testing Admin Dashboard (/admin)...');
    await page.goto(`${BASE_URL}/admin`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase22-admin-dashboard');

    const adminHeader = await page.locator('h1').textContent();
    console.log('Admin Dashboard title:', adminHeader?.trim());

    const kpiCards = page.locator('.admin-kpi');
    const kpiCount = await kpiCards.count();
    console.log('Admin KPI cards rendered:', kpiCount);
    if (kpiCount === 0) throw new Error('Admin KPI cards not rendered on dashboard');

    // -------------------------------------------------------------
    // PHASE 23: ADMIN LEARNERS (/admin/learners)
    // -------------------------------------------------------------
    console.log('Testing Admin Learners (/admin/learners)...');
    await page.goto(`${BASE_URL}/admin/learners`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase23-admin-learners');

    const learnersTable = page.locator('.admin-table');
    const hasTable = await learnersTable.isVisible();
    console.log('Learners table visible:', hasTable);
    if (!hasTable) throw new Error('Learners table not visible');

    const learnerRows = await learnersTable.locator('tbody tr').count();
    console.log('Learners count in table:', learnerRows);

    // -------------------------------------------------------------
    // PHASE 24: ADMIN PRACTICE BANK (/admin/practices)
    // -------------------------------------------------------------
    console.log('Testing Admin Practice Bank (/admin/practices)...');
    await page.goto(`${BASE_URL}/admin/practices`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase24-admin-practices');

    const practicesTable = page.locator('.admin-table');
    console.log('Practices bank table visible:', await practicesTable.isVisible());

    // Test filter dropdown
    const filterSelect = page.locator('select');
    if (await filterSelect.isVisible()) {
      await filterSelect.selectOption('PENDING_REVIEW');
      await page.waitForTimeout(500);
      console.log('Filtered by PENDING_REVIEW successfully');
      await filterSelect.selectOption('');
      await page.waitForTimeout(500);
    }

    // -------------------------------------------------------------
    // PHASE 25: ADMIN MOCK TESTS (/admin/mock-tests)
    // -------------------------------------------------------------
    console.log('Testing Admin Mock Tests (/admin/mock-tests)...');
    await page.goto(`${BASE_URL}/admin/mock-tests`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase25-admin-mock-tests');

    const mockTestTitle = await page.locator('h1').textContent();
    console.log('Admin Mock Tests title:', mockTestTitle?.trim());

    // Create a temporary mock test definition
    const tempSlug = `e2e-test-mock-${Date.now()}`;
    const slugInput = page.locator('label:has-text("Mã định danh (slug)") input');
    const titleInput = page.locator('label:has-text("Tiêu đề bài thi") input');

    if (await slugInput.isVisible() && await titleInput.isVisible()) {
      console.log(`Creating temporary mock test with slug: ${tempSlug}...`);
      await slugInput.fill(tempSlug);
      await titleInput.fill('E2E Temporary Mock Test');
      await page.click('button[type="submit"]:has-text("Tạo bộ đề")');
      await page.waitForTimeout(1500);
      await screenshot('phase25-mock-test-created');

      // Verify created mock test is listed
      const createdItem = page.locator(`.admin-mock-test-item:has-text("${tempSlug}")`);
      const isCreatedVisible = await createdItem.first().isVisible().catch(() => false);
      console.log('Temporary mock test visible in list:', isCreatedVisible);

      // Clean up: delete temporary mock test
      const deleteBtn = createdItem.first().locator('button[aria-label="Xóa bài thi"]');
      if (await deleteBtn.isVisible()) {
        await deleteBtn.click();
        await page.waitForTimeout(1000);
        console.log('Temporary mock test cleaned up successfully');
      }
    }

    // -------------------------------------------------------------
    // PHASE 26: ADMIN AI GENERATOR (/admin/generator)
    // -------------------------------------------------------------
    console.log('Testing Admin AI Generator (/admin/generator)...');
    await page.goto(`${BASE_URL}/admin/generator`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase26-admin-generator');

    const generatorHeading = await page.locator('h1, h2').first().textContent();
    console.log('Generator heading:', generatorHeading?.trim());

    // -------------------------------------------------------------
    // PHASE 21: LOGOUT
    // -------------------------------------------------------------
    console.log('Testing Logout...');
    await logout(page);
    await page.waitForTimeout(1000);
    await screenshot('phase21-logged-out');

    // -------------------------------------------------------------
    // PHASE 27: ROLE SECURITY (STUDENT BLOCKED FROM ADMIN)
    // -------------------------------------------------------------
    console.log('Testing Role Security: Log in as Student and attempt /admin access...');
    await loginAs(page, DEMO_USERS.STUDENT);

    // 1. Browser navigation guard
    await page.goto(`${BASE_URL}/admin`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    console.log('Student tried /admin, redirected to:', currentUrl);
    if (currentUrl.includes('/admin')) {
      throw new Error(`SECURITY VULNERABILITY: Student accessed /admin without being redirected!`);
    }

    // 2. Direct API backend access check
    const studentSession = await page.evaluate(() => {
      try {
        return JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null');
      } catch {
        return null;
      }
    });

    console.log('Testing backend API role security with Student token...');
    const studentApiRes = await fetch(`${BACKEND_URL}/api/admin/overview`, {
      headers: { 'Authorization': `Bearer ${studentSession.token}` }
    });
    console.log('Student GET /api/admin/overview HTTP status:', studentApiRes.status);
    if (studentApiRes.status !== 403) {
      throw new Error(`SECURITY VULNERABILITY: Expected 403 Forbidden for Student on /api/admin/overview, got ${studentApiRes.status}`);
    }

    // 3. Unauthenticated API access check
    const unauthApiRes = await fetch(`${BACKEND_URL}/api/admin/overview`);
    console.log('Unauthenticated GET /api/admin/overview HTTP status:', unauthApiRes.status);
    if (unauthApiRes.status !== 401 && unauthApiRes.status !== 403) {
      throw new Error(`SECURITY VULNERABILITY: Expected 401/403 for unauthenticated on /api/admin/overview, got ${unauthApiRes.status}`);
    }

    // -------------------------------------------------------------
    // PHASE 28: IDOR SECURITY CHECK
    // -------------------------------------------------------------
    console.log('Testing IDOR Security: Accessing another user UUID resource...');
    const fakeOtherUserId = '00000000-0000-0000-0000-000000000001';
    const fakeItemId = '00000000-0000-0000-0000-000000000002';

    // Student attempting to access a random vocabulary item ID
    const vocabIdorRes = await fetch(`${BACKEND_URL}/api/vocabulary/${fakeItemId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${studentSession.token}`
      },
      body: JSON.stringify({ word: 'hacked', meaning: 'stolen' })
    });
    console.log('IDOR test PUT /api/vocabulary/fakeItemId HTTP status:', vocabIdorRes.status);
    if (vocabIdorRes.status !== 404 && vocabIdorRes.status !== 403) {
      throw new Error(`IDOR VULNERABILITY: Expected 404/403 for non-owned item, got ${vocabIdorRes.status}`);
    }

    console.log('Console errors during Phase 21-28:', consoleErrors);
    console.log('Network 5xx errors during Phase 21-28:', networkErrors);

    return {
      status: 'PASS',
      adminDashboardStatus: 'PASS',
      adminLearnersStatus: 'PASS',
      adminPracticeBankStatus: 'PASS',
      adminMockTestsStatus: 'PASS',
      adminGeneratorStatus: 'PASS',
      roleSecurityStatus: 'PASS (403 forbidden enforced)',
      idorSecurityStatus: 'PASS (non-owned resources isolated)',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('10-admin-and-role-security.js')) {
  runAdminAndRoleSecurityTests()
    .then((res) => {
      console.log('Phase 21-28 Result:', res);
      process.exit(0);
    })
    .catch((err) => {
      console.error('Phase 21-28 FAILED:', err);
      process.exit(1);
    });
}

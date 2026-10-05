import { createBrowserContext, BASE_URL, DEMO_USERS, turnOnLamp, logout } from './common.js';

export async function runAuthProfileTests() {
  console.log('\n--- STARTING PHASE 3 & 4: AUTH & PROFILE TESTS ---');
  const { page, context, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // -------------------------------------------------------------
    // PHASE 3: REGISTRATION TESTS
    // -------------------------------------------------------------
    console.log('Testing Registration flows...');
    await page.goto(`${BASE_URL}/register`, { waitUntil: 'networkidle' });
    await turnOnLamp(page);
    await screenshot('phase03-register-initial');

    // 1. Password confirmation mismatch
    await page.fill('#register-name', 'Test Mismatch');
    await page.fill('#register-email', 'mismatch@example.test');
    await page.fill('#register-password', 'ValidPass123!');
    await page.fill('#register-password-confirmation', 'DifferentPass456!');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(400);
    const mismatchError = await page.locator('.auth-error').textContent();
    console.log('Mismatch error displayed:', mismatchError);
    if (!mismatchError?.includes('không khớp')) {
      throw new Error(`Expected password mismatch error, got: ${mismatchError}`);
    }

    // 2. Duplicate email error (using existing student@example.test)
    await page.fill('#register-name', 'Duplicate User');
    await page.fill('#register-email', DEMO_USERS.STUDENT.email);
    await page.fill('#register-password', 'ValidPass123!');
    await page.fill('#register-password-confirmation', 'ValidPass123!');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1000);
    const duplicateError = await page.locator('.auth-error').textContent();
    console.log('Duplicate email error displayed:', duplicateError);
    if (!duplicateError || duplicateError.length === 0) {
      throw new Error('Expected duplicate email error, but none was displayed');
    }

    // 3. Valid registration with temporary user
    const tempUserEmail = `e2e_student_${Date.now()}@example.test`;
    await page.fill('#register-name', 'E2E Temporary Student');
    await page.fill('#register-email', tempUserEmail);
    await page.fill('#register-password', 'TempPass2026!');
    await page.fill('#register-password-confirmation', 'TempPass2026!');
    await page.click('button[type="submit"]');
    await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
    console.log('Registered temporary user successfully, redirected to:', page.url());
    await screenshot('phase03-registered-session');

    // Logout temporary student
    await logout(page);
    console.log('Logged out temporary student');

    // -------------------------------------------------------------
    // PHASE 3: STUDENT LOGIN TESTS
    // -------------------------------------------------------------
    console.log('Testing Student Login flows...');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await turnOnLamp(page);

    // 1. Invalid password
    await page.fill('#login-email', DEMO_USERS.STUDENT.email);
    await page.fill('#login-password', 'WrongPassword123!');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(800);
    const loginError = await page.locator('.auth-error').textContent();
    console.log('Invalid password error displayed:', loginError);
    if (!loginError || loginError.length === 0) {
      throw new Error('Expected login error on invalid password');
    }

    // 2. Invalid email
    await page.fill('#login-email', 'nonexistent_user_9999@example.test');
    await page.fill('#login-password', DEMO_USERS.STUDENT.password);
    await page.click('button[type="submit"]');
    await page.waitForTimeout(800);
    const loginError2 = await page.locator('.auth-error').textContent();
    console.log('Invalid email error displayed:', loginError2);
    if (!loginError2 || loginError2.length === 0) {
      throw new Error('Expected login error on invalid email');
    }

    // 3. Valid Student Login
    await page.fill('#login-email', DEMO_USERS.STUDENT.email);
    await page.fill('#login-password', DEMO_USERS.STUDENT.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
    console.log('Student logged in successfully!');
    await screenshot('phase03-student-logged-in');

    // 4. Session persistence after refresh
    await page.reload({ waitUntil: 'networkidle' });
    const hasAccountTrigger = await page.locator('.account-trigger').isVisible();
    console.log('Account trigger visible after refresh:', hasAccountTrigger);
    if (!hasAccountTrigger) throw new Error('Session lost after page refresh!');

    // 5. Session persistence in another tab
    const secondTab = await context.newPage();
    await secondTab.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    const secondTabAccount = await secondTab.locator('.account-trigger').isVisible();
    console.log('Account trigger visible in second tab:', secondTabAccount);
    await secondTab.close();
    if (!secondTabAccount) throw new Error('Session not shared in second tab!');

    // -------------------------------------------------------------
    // PHASE 4: PROFILE & SETTINGS
    // -------------------------------------------------------------
    console.log('Testing Profile & Onboarding...');
    await page.goto(`${BASE_URL}/profile`, { waitUntil: 'networkidle' });
    await page.waitForSelector('#profile-name', { timeout: 10000 });
    await screenshot('phase04-profile');

    const originalName = await page.inputValue('#profile-name');
    const originalBand = await page.inputValue('#profile-target-band');
    const originalMinutes = await page.inputValue('#profile-daily-minutes');
    console.log(`Original Profile: Name=${originalName}, Band=${originalBand}, DailyMinutes=${originalMinutes}`);

    // Update profile with temporary values
    await page.fill('#profile-target-band', '8.0');
    await page.fill('#profile-daily-minutes', '45');
    await page.selectOption('#profile-weak-skill', 'WRITING');

    // Click Save
    const saveBtn = page.locator('button[type="submit"]:has-text("Lưu thay đổi")');
    await saveBtn.click();
    await page.waitForTimeout(1000);
    await screenshot('phase04-profile-saved');

    // Refresh and verify persistence
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForSelector('#profile-name');
    const updatedBand = await page.inputValue('#profile-target-band');
    const updatedMinutes = await page.inputValue('#profile-daily-minutes');
    const updatedSkill = await page.inputValue('#profile-weak-skill');
    console.log(`Persisted Profile: Band=${updatedBand}, DailyMinutes=${updatedMinutes}, Skill=${updatedSkill}`);

    if (updatedBand !== '8.0' && updatedBand !== '8') {
      throw new Error(`Target band persistence failed! Got ${updatedBand}, expected 8.0`);
    }
    if (updatedMinutes !== '45') {
      throw new Error(`Daily minutes persistence failed! Got ${updatedMinutes}, expected 45`);
    }

    // Restore original values
    await page.fill('#profile-target-band', originalBand || '7.0');
    await page.fill('#profile-daily-minutes', originalMinutes || '30');
    await saveBtn.click();
    await page.waitForTimeout(500);

    // Test Settings Drawer
    console.log('Testing Settings drawer...');
    const settingsBtn = page.locator('.settings-btn');
    if (await settingsBtn.isVisible()) {
      await settingsBtn.click();
      await page.waitForTimeout(400);
      const drawerVisible = await page.locator('.settings-drawer').isVisible();
      console.log('Settings drawer visible:', drawerVisible);
      await screenshot('phase04-settings-drawer');
      // Close drawer with escape or close button
      await page.keyboard.press('Escape');
      await page.waitForTimeout(300);
    }

    console.log('Console errors during Phase 3 & 4:', consoleErrors);
    console.log('Network 5xx errors during Phase 3 & 4:', networkErrors);

    return {
      status: 'PASS',
      tempUser: tempUserEmail,
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('02-auth-profile.js')) {
  runAuthProfileTests().then(res => {
    console.log('Phase 3 & 4 Result:', res);
  }).catch(err => {
    console.error('Phase 3 & 4 FAILED:', err);
    process.exit(1);
  });
}

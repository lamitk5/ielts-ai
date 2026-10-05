import { createBrowserContext, DEMO_USERS, BASE_URL } from './common.js';

async function run() {
  const { browser, page, screenshot, close } = await createBrowserContext();
  try {
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await screenshot('01-login-initial');

    const lampBtn = page.locator('.auth-lamp-pull-control');
    await lampBtn.click();
    await page.waitForTimeout(400);
    await screenshot('01-login-lamp-on');

    await page.fill('#login-email', DEMO_USERS.STUDENT.email);
    await page.fill('#login-password', DEMO_USERS.STUDENT.password);
    await screenshot('01-login-filled');

    await page.click('button[type="submit"]');
    await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
    await page.waitForTimeout(1000);
    await screenshot('02-student-dashboard');

    const brand = await page.locator('.brand').textContent();
    console.log('Login successful! Brand:', brand.trim());
  } finally {
    await close();
  }
}

run().catch(err => {
  console.error('Test run failed:', err);
  process.exit(1);
});

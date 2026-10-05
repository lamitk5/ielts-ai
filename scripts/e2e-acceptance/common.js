import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';

export const BASE_URL = 'http://127.0.0.1:5173';
export const BACKEND_URL = 'http://127.0.0.1:8081';

export const DEMO_USERS = {
  STUDENT: {
    email: 'student@example.test',
    password: 'StudentDemo!2026',
    name: 'Học viên Demo'
  },
  ADMIN: {
    email: 'admin@example.test',
    password: 'AdminDemo!2026',
    name: 'Quản trị viên'
  }
};

export const SCREENSHOT_DIR = path.resolve('docs/e2e-screenshots');
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

export async function createBrowserContext(options = {}) {
  const browser = await chromium.launch({
    headless: true,
    ...options
  });

  const context = await browser.newContext({
    viewport: options.viewport || { width: 1440, height: 900 },
    permissions: options.permissions || []
  });

  const consoleErrors = [];
  const networkErrors = [];

  const page = await context.newPage();

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      const text = msg.text();
      // Ignore known benign third-party or expected errors if any
      consoleErrors.push(text);
    }
  });

  page.on('pageerror', (err) => {
    consoleErrors.push(err.message);
  });

  page.on('response', (res) => {
    if (res.status() >= 500) {
      networkErrors.push({ url: res.url(), status: res.status() });
    }
  });

  return {
    browser,
    context,
    page,
    consoleErrors,
    networkErrors,
    async screenshot(name) {
      const filePath = path.join(SCREENSHOT_DIR, `${name}.png`);
      await page.screenshot({ path: filePath, fullPage: false });
      return filePath;
    },
    async close() {
      await browser.close();
    }
  };
}

export async function turnOnLamp(page) {
  const lampBtn = page.locator('.auth-lamp-pull-control');
  if (await lampBtn.isVisible()) {
    const isPressed = await lampBtn.getAttribute('aria-pressed');
    if (isPressed !== 'true') {
      await lampBtn.click();
      await page.waitForTimeout(300);
    }
  }
}

export async function loginAs(page, credentials) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await turnOnLamp(page);
  await page.fill('#login-email', credentials.email);
  await page.fill('#login-password', credentials.password);
  await page.click('button[type="submit"]');
  await page.waitForTimeout(1000);
}

export async function logout(page) {
  // Check if account dropdown is visible
  const trigger = page.locator('.account-trigger');
  if (await trigger.isVisible()) {
    await trigger.click();
    await page.waitForTimeout(200);
    const logoutBtn = page.locator('.account-panel button:has-text("Đăng xuất")');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await page.waitForTimeout(500);
    }
  } else {
    // Alternatively clear localStorage
    await page.evaluate(() => {
      localStorage.removeItem('ielts-ai-tutor.session');
    });
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
  }
}

import { createBrowserContext, BASE_URL } from './common.js';

export async function runPublicWebsiteTests() {
  console.log('\n--- STARTING PHASE 2: PUBLIC WEBSITE TESTS ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // 1. Home page
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    const title = await page.title();
    console.log('Page Title:', title);
    if (!title.includes('IELTS')) throw new Error(`Unexpected page title: ${title}`);

    // Check navbar and brand
    const brandVisible = await page.locator('.brand').isVisible();
    if (!brandVisible) throw new Error('Navbar brand is not visible');

    // Check nav links
    const navLinks = await page.locator('.nav-link').allTextContents();
    console.log('Nav links found:', navLinks);

    // Check Hero & CTAs
    const heroTitle = await page.locator('h1').first().textContent();
    console.log('Hero Title:', heroTitle?.trim());

    // Check Skill cards
    const skillCards = page.locator('.skill-card');
    const skillCount = await skillCards.count();
    console.log('Skill cards count:', skillCount);
    if (skillCount < 4) throw new Error(`Expected at least 4 skill cards, found ${skillCount}`);

    // Check horizontal overflow
    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    console.log('Has horizontal overflow on desktop:', hasHorizontalOverflow);
    if (hasHorizontalOverflow) throw new Error('Horizontal overflow detected on desktop home page!');

    await screenshot('phase02-home');

    // 2. Test Anchor Navigation
    const skillsLink = page.locator('.nav-link[href="/#skills"]');
    if (await skillsLink.isVisible()) {
      await skillsLink.click();
      await page.waitForTimeout(400);
      console.log('Clicked #skills anchor');
    }

    // 3. Test Back / Forward
    await page.goto(`${BASE_URL}/mock-tests`, { waitUntil: 'networkidle' });
    console.log('Navigated to /mock-tests');
    await page.goBack({ waitUntil: 'networkidle' });
    console.log('Navigated back to Home');
    if (!page.url().includes('127.0.0.1:5173')) throw new Error('Failed to goBack to home');
    await page.goForward({ waitUntil: 'networkidle' });
    console.log('Navigated forward to /mock-tests');
    await page.goBack({ waitUntil: 'networkidle' });

    // 4. Test Page Refresh
    await page.reload({ waitUntil: 'networkidle' });
    console.log('Reloaded home page successfully');

    // 5. Test 404 behavior
    await page.goto(`${BASE_URL}/route-does-not-exist-xyz`, { waitUntil: 'networkidle' });
    const notFoundText = await page.locator('main').textContent();
    console.log('404 content check:', notFoundText?.includes('not found') || notFoundText?.includes('Không tìm thấy'));
    await screenshot('phase02-404');
    if (!notFoundText?.includes('Page not found') && !notFoundText?.includes('Không tìm thấy')) {
      throw new Error('404 route did not render placeholder page');
    }

    // Check footer
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    const footerVisible = await page.locator('footer').isVisible();
    console.log('Footer visible:', footerVisible);

    console.log('Console errors during Phase 2:', consoleErrors);
    console.log('Network 5xx errors during Phase 2:', networkErrors);

    return {
      status: 'PASS',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('01-public-website.js')) {
  runPublicWebsiteTests().then(res => {
    console.log('Phase 2 Result:', res);
  }).catch(err => {
    console.error('Phase 2 FAILED:', err);
    process.exit(1);
  });
}

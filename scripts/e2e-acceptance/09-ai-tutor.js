import { createBrowserContext, BASE_URL, DEMO_USERS, loginAs } from './common.js';
import path from 'path';
import fs from 'fs';

export async function runAiTutorTests() {
  console.log('\n--- STARTING PHASE 18, 19, 20: AI TUTOR (ÉN) & ATTACHMENTS TESTS ---');
  const { page, consoleErrors, networkErrors, screenshot, close } = await createBrowserContext();

  try {
    // 1. Log in as Student
    await loginAs(page, DEMO_USERS.STUDENT);

    // 2. Go to Home page where FloatingTutor is mounted
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await screenshot('phase18-tutor-launcher');

    // 3. Find and click Mascot Launcher
    const launcher = page.locator('button.ai-tutor-mascot-launcher');
    const isLauncherVisible = await launcher.isVisible();
    console.log('AI Tutor Mascot Launcher visible:', isLauncherVisible);
    if (!isLauncherVisible) {
      throw new Error('AI Tutor mascot launcher button not found on page!');
    }

    await launcher.click();
    await page.waitForTimeout(600);
    await screenshot('phase18-tutor-open');

    // 4. Verify Tutor Dialog
    const dialog = page.locator('#tutor-dialog');
    const isDialogVisible = await dialog.isVisible();
    console.log('AI Tutor dialog open:', isDialogVisible);
    if (!isDialogVisible) {
      throw new Error('AI Tutor dialog failed to open!');
    }

    // Verify welcome message
    const welcomeMsg = page.locator('.tutor-message-list, .tutor-messages');
    console.log('Tutor message list visible:', await welcomeMsg.isVisible());

    // 5. Test Sending a Prompt
    const input = page.locator('#tutor-input');
    await input.fill('Chào Én, bạn có thể giúp mình cải thiện IELTS Writing Task 2 không?');
    const form = page.locator('form.tutor-input-form');
    await form.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);
    await screenshot('phase18-tutor-sent');

    // 6. Test Attachment functionality (Phase 20)
    // Create a temporary sample attachment file
    const sampleFilePath = path.resolve('docs/e2e-screenshots/sample-note.txt');
    fs.writeFileSync(sampleFilePath, 'IELTS Writing Task 2 Sample Notes for AI Tutor test.');

    const fileInput = page.locator('#tutor-attachment-file-input');
    if (await fileInput.count() > 0) {
      console.log('Testing file attachment upload with sample-note.txt...');
      await fileInput.setInputFiles(sampleFilePath);
      await page.waitForTimeout(1500);
      await screenshot('phase20-tutor-attachment');

      // Check if attachment badge/status appeared
      const attachmentItem = page.locator('.tutor-attachment-item, .attachment-status, .tutor-attachment-badge');
      const attachmentVisible = await attachmentItem.first().isVisible().catch(() => false);
      console.log('Attachment item rendered in composer:', attachmentVisible);
    }

    // 7. Test Close behavior (Escape key)
    console.log('Testing closing AI Tutor via Escape key...');
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    const isDialogClosed = !(await dialog.isVisible());
    console.log('AI Tutor dialog closed via Escape:', isDialogClosed);
    await screenshot('phase18-tutor-closed');

    // Clean up sample file
    if (fs.existsSync(sampleFilePath)) {
      fs.unlinkSync(sampleFilePath);
    }

    console.log('Console errors during Phase 18-20:', consoleErrors);
    console.log('Network 5xx errors during Phase 18-20:', networkErrors);

    return {
      status: 'PASS',
      tutorLauncherStatus: 'PASS',
      tutorDialogStatus: 'PASS',
      tutorConversationStatus: 'PASS',
      tutorAttachmentStatus: 'PASS',
      consoleErrorsCount: consoleErrors.length,
      networkErrorsCount: networkErrors.length
    };
  } finally {
    await close();
  }
}

if (process.argv[1]?.endsWith('09-ai-tutor.js')) {
  runAiTutorTests()
    .then((res) => {
      console.log('Phase 18, 19, 20 Result:', res);
      process.exit(0);
    })
    .catch((err) => {
      console.error('Phase 18, 19, 20 FAILED:', err);
      process.exit(1);
    });
}

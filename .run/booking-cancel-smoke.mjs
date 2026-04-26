import fs from 'node:fs/promises';
import path from 'node:path';

const { chromium } = await import('file:///C:/Users/Are/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');

const root = 'D:/DevelopmentLOOK/Idea/idea_project_workspace/huang-parent';
const outputDir = path.join(root, 'frontend-app/output/playwright');
const statePath = path.join(outputDir, 'booking-state.json');
const chromePath = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const reportPath = path.join(outputDir, 'booking-cancel-smoke-result.json');
const report = {
  startedAt: new Date().toISOString(),
  initialUrl: '',
  orderUrl: '',
  returnUrl: '',
  currentTodoVisible: false,
  cancelButtonVisible: false,
  cancelSuccess: false,
  currentTodoTextAfterCancel: '',
  coachPickerHeadingAfterCancel: '',
  coachOptionsAfterCancel: [],
  errors: []
};

await fs.mkdir(outputDir, { recursive: true });
const storageStateRaw = await fs.readFile(statePath, 'utf8');
const storageState = JSON.parse(storageStateRaw.replace(/^\uFEFF/, ''));
const browser = await chromium.launch({ headless: true, executablePath: chromePath });
const context = await browser.newContext({ storageState, viewport: { width: 1560, height: 1200 } });
const page = await context.newPage();

try {
  await page.goto('http://127.0.0.1:5174/booking?bookingTab=bookings', { waitUntil: 'networkidle', timeout: 30000 });
  report.initialUrl = page.url();
  await page.screenshot({ path: path.join(outputDir, 'booking-before-cancel.png'), fullPage: true });

  const currentTodoSection = page.locator('section').filter({ hasText: '当前待办' }).first();
  report.currentTodoVisible = await currentTodoSection.isVisible().catch(() => false);
  if (!report.currentTodoVisible) {
    throw new Error('预约页未显示“当前待办”卡片，无法继续验证取消链路');
  }

  await currentTodoSection.getByRole('button', { name: '订单详情' }).click();
  await page.waitForURL(/\/orders/, { timeout: 30000 });
  await page.waitForLoadState('networkidle');
  report.orderUrl = page.url();
  await page.screenshot({ path: path.join(outputDir, 'orders-before-cancel.png'), fullPage: true });

  const cancelButton = page.getByRole('button', { name: '取消预约' });
  report.cancelButtonVisible = await cancelButton.first().isVisible().catch(() => false);
  if (!report.cancelButtonVisible) {
    throw new Error('订单页未找到“取消预约”按钮');
  }

  await cancelButton.first().click();
  await page.getByRole('button', { name: '确认取消' }).click();
  await page.waitForTimeout(1200);
  await page.waitForLoadState('networkidle');

  const successToast = page.locator('.el-message').filter({ hasText: '预约已取消' });
  report.cancelSuccess = (await successToast.count()) > 0;
  await page.screenshot({ path: path.join(outputDir, 'orders-after-cancel.png'), fullPage: true });

  await page.getByRole('button', { name: '返回我的预约' }).click();
  await page.waitForURL(/\/booking/, { timeout: 30000 });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);
  report.returnUrl = page.url();

  const todoAfterCancel = page.locator('section').filter({ hasText: '当前待办' }).first();
  if (await todoAfterCancel.isVisible().catch(() => false)) {
    report.currentTodoTextAfterCancel = (await todoAfterCancel.innerText()).trim();
  }

  await page.screenshot({ path: path.join(outputDir, 'booking-after-cancel.png'), fullPage: true });

  await page.getByRole('button', { name: '选择教练' }).first().click();
  const dialog = page.locator('.el-dialog').filter({ hasText: '选择教练' }).last();
  await dialog.waitFor({ state: 'visible', timeout: 15000 });
  await page.screenshot({ path: path.join(outputDir, 'booking-coach-picker-after-cancel.png'), fullPage: true });

  report.coachPickerHeadingAfterCancel = (await dialog.locator('strong').first().innerText()).trim();
  report.coachOptionsAfterCancel = await dialog.locator('.coach-picker-card strong').allInnerTexts();
} catch (error) {
  report.errors.push(String(error?.stack || error));
} finally {
  report.finishedAt = new Date().toISOString();
  await fs.writeFile(reportPath, JSON.stringify(report, null, 2), 'utf8');
  await context.close();
  await browser.close();
}

if (report.errors.length) {
  console.error(report.errors.join('\n\n'));
  process.exit(1);
}

console.log(JSON.stringify(report, null, 2));

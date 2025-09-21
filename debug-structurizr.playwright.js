const { chromium } = require('playwright');

if (process.argv.length < 3) {
  console.log('Usage: node debug-structurizr.playwright.js <url>');
  process.exit(1);
}

const url = process.argv[2];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  console.log('Navigating to:', url);
  await page.goto(url, { waitUntil: 'domcontentloaded' });

  // List frames and check structurizr presence
  const frames = page.frames();
  console.log('Frames found:', frames.length);
  for (const f of frames) {
    let href = 'n/a';
    try { href = await f.evaluate(() => location.href).catch(() => f.url()); } catch (_) {}
    const hasStruct = await f
      .evaluate(() => !!(window.structurizr && window.structurizr.scripting))
      .catch(() => false);
    console.log('- Frame URL:', f.url());
    console.log('  Location:', href);
    console.log('  structurizr.scripting:', hasStruct);
  }

  await browser.close();
})();

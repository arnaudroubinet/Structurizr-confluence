const { chromium } = require('playwright');
const fs = require('fs');

const PNG_FORMAT = 'png';
const SVG_FORMAT = 'svg';

const IGNORE_HTTPS_ERRORS = true;
const HEADLESS = true;

const IMAGE_VIEW_TYPE = 'Image';

if (process.argv.length < 4) {
  console.log("Usage: <structurizrUrl> <png|svg> [username] [password]")
  process.exit(1);
}

const url = process.argv[2];
const format = process.argv[3];

if (format !== PNG_FORMAT && format !== SVG_FORMAT) {
  console.log("The output format must be '" + PNG_FORMAT + "' or '" + SVG_FORMAT + "'.");
  process.exit(1);
}

let username;
let password;

if (process.argv.length > 3) {
  username = process.argv[4];
  password = process.argv[5];
}

let expectedNumberOfExports = 0;
let actualNumberOfExports = 0;

(async () => {
  const browser = await chromium.launch({
    ignoreHTTPSErrors: IGNORE_HTTPS_ERRORS,
    headless: HEADLESS,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage'
    ]
  });

  const context = await browser.newContext({ ignoreHTTPSErrors: IGNORE_HTTPS_ERRORS });
  const page = await context.newPage();

  if (username !== undefined && password !== undefined) {
    // sign in (approximate same logic as Puppeteer)
    const parts = url.split('://');
    const signinUrl = parts[0] + '://' + parts[1].substring(0, parts[1].indexOf('/')) + '/dashboard';
    console.log(' - Signing in via ' + signinUrl);

    await page.goto(signinUrl, { waitUntil: 'networkidle' });
    await page.fill('#username', username);
    await page.fill('#password', password);
    await page.keyboard.press('Enter');
    await page.waitForSelector('div#dashboard', { timeout: 20000 });
  }

  const viewerUrl = /\/workspace\/(\d+)(\/?$)/.test(url) ? (url.replace(/\/?$/, '') + '/diagrams') : url;
  console.log(" - Opening " + viewerUrl);
  await page.goto(viewerUrl, { waitUntil: 'domcontentloaded' });

  // Helper to find the frame hosting structurizr
  const findStructurizrFrame = async () => {
    for (const f of page.frames()) {
      const ok = await f.evaluate(() => !!(window.structurizr && window.structurizr.scripting)).catch(() => false);
      if (ok) return f;
    }
    return null;
  };

  // Wait up to 60s for structurizr frame
  let structurizrFrame = null;
  const start = Date.now();
  while (!structurizrFrame && Date.now() - start < 60000) {
    structurizrFrame = await findStructurizrFrame();
    if (!structurizrFrame) await page.waitForTimeout(500);
  }
  if (!structurizrFrame) {
    console.error('Structurizr scripting not found in any frame');
    process.exit(1);
  }

  await structurizrFrame.waitForFunction(() => window.structurizr && window.structurizr.scripting && window.structurizr.scripting.isDiagramRendered && window.structurizr.scripting.isDiagramRendered() === true, { timeout: 60000 });

  // get the array of views
  const views = await structurizrFrame.evaluate(() => {
    return window.structurizr.scripting.getViews();
  });

  views.forEach(function(view) {
    if (view.type === IMAGE_VIEW_TYPE) {
      expectedNumberOfExports++; // diagram only
    } else {
      expectedNumberOfExports++; // diagram
      expectedNumberOfExports++; // key
    }
  });

  console.log(" - Starting export");
  for (let i = 0; i < views.length; i++) {
    const view = views[i];

    await structurizrFrame.evaluate((v) => {
      window.structurizr.scripting.changeView(v.key);
    }, view);

    await structurizrFrame.waitForFunction(() => window.structurizr.scripting.isDiagramRendered() === true, { timeout: 30000 });

    if (format === SVG_FORMAT) {
      const diagramFilename = view.key + '.svg';
      const diagramKeyFilename = view.key + '-key.svg';

      const svgForDiagram = await structurizrFrame.evaluate(() => {
        return window.structurizr.scripting.exportCurrentDiagramToSVG({ includeMetadata: true });
      });

      console.log(" - " + diagramFilename);
      fs.writeFileSync(diagramFilename, svgForDiagram);
      actualNumberOfExports++;

      if (view.type !== IMAGE_VIEW_TYPE) {
        const svgForKey = await structurizrFrame.evaluate(() => {
          return window.structurizr.scripting.exportCurrentDiagramKeyToSVG();
        });

        console.log(" - " + diagramKeyFilename);
        fs.writeFileSync(diagramKeyFilename, svgForKey);
        actualNumberOfExports++;
      }
    } else {
      const diagramFilename = view.key + '.png';
      const diagramKeyFilename = view.key + '-key.png';

      const pngForDiagram = await structurizrFrame.evaluate(() => {
        return new Promise((resolve) => {
          window.structurizr.scripting.exportCurrentDiagramToPNG({ includeMetadata: true, crop: false }, function(png) {
            resolve(png);
          });
        });
      });

      console.log(" - " + diagramFilename);
      const content = pngForDiagram.replace(/^data:image\/png;base64,/, "");
      fs.writeFileSync(diagramFilename, content, 'base64');
      actualNumberOfExports++;

      if (view.type !== IMAGE_VIEW_TYPE) {
        const pngForKey = await structurizrFrame.evaluate(() => {
          return new Promise((resolve) => {
            window.structurizr.scripting.exportCurrentDiagramKeyToPNG(function(png) {
              resolve(png);
            });
          });
        });

        console.log(" - " + diagramKeyFilename);
        const contentKey = pngForKey.replace(/^data:image\/png;base64,/, "");
        fs.writeFileSync(diagramKeyFilename, contentKey, 'base64');
        actualNumberOfExports++;
      }
    }
  }

  if (actualNumberOfExports === expectedNumberOfExports) {
    console.log(" - Finished");
  } else {
    console.warn(` - Finished with mismatch: expected ${expectedNumberOfExports}, got ${actualNumberOfExports}`);
  }

  await browser.close();
})();

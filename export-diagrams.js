const puppeteer = require('puppeteer');
const fs = require('fs');

const PNG_FORMAT = 'png';
const SVG_FORMAT = 'svg';

const IGNORE_HTTPS_ERRORS = true;
const HEADLESS = "new";

const IMAGE_VIEW_TYPE = 'Image';

if (process.argv.length < 4) {
  console.log("Usage: <structurizrUrl> <png|svg> [username] [password]")
  process.exit(1);
}

let url = process.argv[2];
const format = process.argv[3];

if (format !== PNG_FORMAT && format !== SVG_FORMAT) {
  console.log("The output format must be ' + PNG_FORMAT + ' or ' + SVG_FORMAT + '.");
  process.exit(1);
}

var username;
var password;

if (process.argv.length > 3) {
  username = process.argv[4];
  password = process.argv[5];
}

var expectedNumberOfExports = 0;
var actualNumberOfExports = 0;
var manifest = { views: [] };

(async () => {
  const browser = await puppeteer.launch({
    ignoreHTTPSErrors: IGNORE_HTTPS_ERRORS,
    headless: HEADLESS,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage'
    ]
  });
  const page = await browser.newPage();

  if (username !== undefined && password !== undefined) {
    // sign in
    const parts = url.split('://');
    const host = parts[0] + '://' + parts[1].substring(0, parts[1].indexOf('/'));
    const signinUrl = host + '/signin';
    console.log(' - Signing in via ' + signinUrl);

    await page.goto(signinUrl, { waitUntil: 'networkidle2' });

    // Try multiple selectors for username/email and password
    const userSelectors = ['#username', 'input[name="username"]', '#email', '#emailAddress', 'input[type="email"]', 'input[name="email"]'];
    const passSelectors = ['#password', 'input[name="password"]', 'input[type="password"]'];

    let userField = null;
    for (const sel of userSelectors) {
      const el = await page.$(sel);
      if (el) { userField = sel; break; }
    }
    let passField = null;
    for (const sel of passSelectors) {
      const el = await page.$(sel);
      if (el) { passField = sel; break; }
    }

    if (!userField || !passField) {
      console.log(' - Could not find login fields, current URL:', page.url());
    } else {
      await page.focus(userField);
      await page.keyboard.type(username);
      await page.focus(passField);
      await page.keyboard.type(password);

      // Try submit
      const submitSelectors = ['button[type="submit"]', 'button:has-text("Sign in")', 'input[type="submit"]', 'button:has-text("Se connecter")'];
      let clicked = false;
      for (const sel of submitSelectors) {
        const btn = await page.$(sel);
        if (btn) { await btn.click(); clicked = true; break; }
      }
      if (!clicked) {
        await page.keyboard.press('Enter');
      }

      // Wait for redirect away from /signin or dashboard present
      try {
        await page.waitForFunction(() => !location.pathname.includes('/signin'), { timeout: 20000 });
      } catch (_) {}
      try {
        await page.waitForSelector('div#dashboard', { timeout: 10000 });
      } catch (_) {}
    }
  }

  // ensure viewer URL (append /diagrams if not present)
  const viewerUrl = url.match(/\/workspace\/(\d+)(\/?$)/) ? (url.replace(/\/?$/, '') + '/diagrams') : url;
  // visit the diagrams page
  console.log(" - Opening " + viewerUrl);
  await page.goto(viewerUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForFunction('structurizr && structurizr.scripting', { timeout: 60000 });
  await page.waitForFunction('structurizr.scripting.isDiagramRendered() === true', { timeout: 60000 });

  if (format === PNG_FORMAT) {
    // add a function to the page to save the generated PNG images
    await page.exposeFunction('savePNG', (content, filename) => {
      console.log(" - " + filename);
      content = content.replace(/^data:image\/png;base64,/, "");
      fs.writeFile(filename, content, 'base64', function (err) {
        if (err) throw err;
      });
      
      actualNumberOfExports++;

      if (actualNumberOfExports === expectedNumberOfExports) {
        console.log(" - Finished");
        browser.close();
      }
    });
  }

  // get the array of views
  const views = await page.evaluate(() => {
    return structurizr.scripting.getViews();
  });

  views.forEach(function(view) {
    if (view.type === IMAGE_VIEW_TYPE) {
      expectedNumberOfExports++; // diagram only
    } else {
      expectedNumberOfExports++; // diagram
      expectedNumberOfExports++; // key
    }
    // Prepare manifest entry
    manifest.views.push({
      key: view.key,
      title: view.title || view.name || view.key,
      type: view.type,
      png: view.key + '.png',
      pngKey: view.key + '-key.png',
      svg: view.key + '.svg',
      svgKey: view.key + '-key.svg'
    });
  });

  console.log(" - Starting export");
  for (var i = 0; i < views.length; i++) {
    const view = views[i];

    await page.evaluate((view) => {
      structurizr.scripting.changeView(view.key);
    }, view);

    await page.waitForFunction('structurizr.scripting.isDiagramRendered() === true');

    if (format === SVG_FORMAT) {
      const diagramFilename = view.key + '.svg';
      const diagramKeyFilename = view.key + '-key.svg'

      var svgForDiagram = await page.evaluate(() => {
        return structurizr.scripting.exportCurrentDiagramToSVG({ includeMetadata: true });
      });
    
      console.log(" - " + diagramFilename);
      fs.writeFile(diagramFilename, svgForDiagram, function (err) {
        if (err) throw err;
      });
      actualNumberOfExports++;
    
      if (view.type !== IMAGE_VIEW_TYPE) {
        var svgForKey = await page.evaluate(() => {
          return structurizr.scripting.exportCurrentDiagramKeyToSVG();
        });
      
        console.log(" - " + diagramKeyFilename);
        fs.writeFile(diagramKeyFilename, svgForKey, function (err) {
          if (err) throw err;
        });
        actualNumberOfExports++;
      }

      if (actualNumberOfExports === expectedNumberOfExports) {
        // Write manifest
        try { fs.writeFileSync('diagram-manifest.json', JSON.stringify(manifest, null, 2)); } catch(e) { console.warn('Failed to write manifest', e); }
        console.log(" - Finished");
        browser.close();
      }    
    } else {
      const diagramFilename = view.key + '.png';
      const diagramKeyFilename = view.key + '-key.png'

      page.evaluate((diagramFilename) => {
        structurizr.scripting.exportCurrentDiagramToPNG({ includeMetadata: true, crop: false }, function(png) {
          window.savePNG(png, diagramFilename);
        })
      }, diagramFilename);

      if (view.type !== IMAGE_VIEW_TYPE) {
        page.evaluate((diagramKeyFilename) => {
          structurizr.scripting.exportCurrentDiagramKeyToPNG(function(png) {
            window.savePNG(png, diagramKeyFilename);
          })
        }, diagramKeyFilename);
      }
    }
  }

  // Write manifest at end in case of PNG path termination as well
  const interval = setInterval(() => {
    if (actualNumberOfExports === expectedNumberOfExports) {
      try { fs.writeFileSync('diagram-manifest.json', JSON.stringify(manifest, null, 2)); } catch(e) { console.warn('Failed to write manifest', e); }
      clearInterval(interval);
    }
  }, 500);

})();
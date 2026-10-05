import { test, expect } from '@playwright/test';

const PROD_URL = process.env.BASE_URL || 'https://lostlink-public-frontend-3znt.vercel.app';

test.describe('Production E2E Smoke Tests', () => {

    test('Loads home catalog, verifies HTTP 200 on all images, and measures TTFB', async ({ page }) => {
        const failedRequests = [];

        // Track network failures and 404s
        page.on('response', (response) => {
            const url = response.url();
            const status = response.status();
            // Flag any 4xx/5xx responses from critical endpoints or images
            if (status >= 400 && (url.includes('/api/') || url.match(/\.(jpg|jpeg|png|webp|svg)/i))) {
                failedRequests.push({ url, status });
            }
        });

        const startTime = Date.now();
        const response = await page.goto(PROD_URL, { waitUntil: 'domcontentloaded', timeout: 45000 });
        const ttfb = Date.now() - startTime;
        console.log(`[PERF] Navigation TTFB / DOM Loaded: ${ttfb} ms`);

        expect(response?.status()).toBeLessThan(400);

        // Wait for page header / navbar to be visible
        await expect(page.locator('text=LostLink')).toBeVisible({ timeout: 15000 });

        // Wait for catalog items to appear
        const itemLinks = page.locator('a[href^="/items/"]');
        await expect(itemLinks.first()).toBeVisible({ timeout: 20000 });

        const count = await itemLinks.count();
        console.log(`[PASS] Detected ${count} item listing links on production catalog`);
        expect(count).toBeGreaterThan(0);

        // Assert no critical image or API 404s occurred
        expect(failedRequests).toEqual([]);

        // Verify that images on cards have valid naturalWidth (not broken)
        const images = page.locator('a[href^="/items/"] img');
        const imgCount = await images.count();
        console.log(`[CHECK] Validating rendered image status for ${imgCount} card images...`);

        for (let i = 0; i < Math.min(imgCount, 6); i++) {
            const img = images.nth(i);
            const isLoaded = await img.evaluate((el) => el.complete && el.naturalWidth > 0);
            expect(isLoaded).toBeTruthy();
        }
    });

    test('Navigates from Home to Item Details and back with instant cache recovery', async ({ page }) => {
        await page.goto(PROD_URL, { waitUntil: 'domcontentloaded', timeout: 45000 });

        // Locate first item card link
        const firstItem = page.locator('a[href^="/items/"]').first();
        await expect(firstItem).toBeVisible({ timeout: 20000 });

        // Navigate to item details
        await firstItem.click();
        await page.waitForURL(/\/items\/\d+/, { timeout: 15000 });
        console.log(`[PASS] Navigated to detail page: ${page.url()}`);

        // Verify item detail layout rendered
        await expect(page.locator('h1, h2').first()).toBeVisible({ timeout: 10000 });

        // Navigate back to Home
        const homeLink = page.locator('a[href="/"]').first();
        const backStart = Date.now();
        await homeLink.click();
        await page.waitForURL(`${PROD_URL}/`, { timeout: 10000 }).catch(async () => {
            // Also accept URL matching root origin
            await expect(page.locator('a[href^="/items/"]').first()).toBeVisible({ timeout: 5000 });
        });

        // Verify instant recovery from in-memory cache without blank loading screen
        const returnCards = page.locator('a[href^="/items/"]').first();
        await expect(returnCards).toBeVisible({ timeout: 3000 });
        const recoveryTime = Date.now() - backStart;
        console.log(`[PERF] Cache recovery & re-render completed in: ${recoveryTime} ms`);
        expect(recoveryTime).toBeLessThan(3000);
    });
});

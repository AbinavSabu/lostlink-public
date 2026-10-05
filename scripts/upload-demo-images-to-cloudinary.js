#!/usr/bin/env node

/**
 * Cloudinary Migration Tool for LostLink Curated Demo Images
 *
 * This script uploads the 12 curated demo photos (1 photo per demo item)
 * to your Cloudinary account and outputs ready-to-execute SQL statements
 * to update PostgreSQL/Supabase.
 *
 * Usage:
 *   CLOUDINARY_CLOUD_NAME=xxx CLOUDINARY_API_KEY=yyy CLOUDINARY_API_SECRET=zzz node scripts/upload-demo-images-to-cloudinary.js
 */

import fs from 'fs';
import path from 'path';
import https from 'https';
import crypto from 'crypto';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
const apiKey = process.env.CLOUDINARY_API_KEY;
const apiSecret = process.env.CLOUDINARY_API_SECRET;

const CATALOG_ITEMS = [
  { id: 1, name: 'Apple iPhone 15 Pro (Natural Titanium)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8001.jpg' },
  { id: 2, name: 'Apple MacBook Air M2 (Space Gray)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8002.jpg' },
  { id: 3, name: 'Navy Blue Leather Bi-Fold Wallet', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8003.jpg' },
  { id: 4, name: 'The North Face Borealis Backpack (Black)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8004.jpg' },
  { id: 5, name: 'Set of Dorm & Bike Keys on Black Carabiner', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8005.jpg' },
  { id: 6, name: 'Apple Watch Series 9 (Midnight Aluminum 45mm)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8006.jpg' },
  { id: 7, name: 'Sony WH-1000XM5 Wireless Headphones', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8007.jpg' },
  { id: 8, name: 'Stainless Steel Hydro Flask (Olive Green 32oz)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8008.jpg' },
  { id: 9, name: 'Texas Instruments TI-84 Plus CE Calculator', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8009.jpg' },
  { id: 10, name: 'Campus Student ID & Access Smartcard', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8010.jpg' },
  { id: 11, name: 'Compact Windproof Travel Umbrella (Navy Blue)', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8011.jpg' },
  { id: 12, name: 'Trek FX 2 Disc City Commuter Bicycle', filename: 'd1b7a240-6f81-423c-91d1-61019a6d8012.jpg' },
];

function uploadToCloudinary(filePath, publicId) {
  return new Promise((resolve, reject) => {
    const timestamp = Math.floor(Date.now() / 1000);
    const folder = 'lostlink/catalog';
    
    // Cloudinary signature calculation: sorted params + api_secret hashed with sha1
    const paramsToSign = `folder=${folder}&public_id=${publicId}&timestamp=${timestamp}${apiSecret}`;
    const signature = crypto.createHash('sha1').update(paramsToSign).digest('hex');

    const boundary = '----CloudinaryBoundary' + Math.random().toString(36).substring(2);
    const fileData = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);

    const parts = [
      `--${boundary}\r\nContent-Disposition: form-data; name="api_key"\r\n\r\n${apiKey}\r\n`,
      `--${boundary}\r\nContent-Disposition: form-data; name="timestamp"\r\n\r\n${timestamp}\r\n`,
      `--${boundary}\r\nContent-Disposition: form-data; name="folder"\r\n\r\n${folder}\r\n`,
      `--${boundary}\r\nContent-Disposition: form-data; name="public_id"\r\n\r\n${publicId}\r\n`,
      `--${boundary}\r\nContent-Disposition: form-data; name="signature"\r\n\r\n${signature}\r\n`,
      `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: image/jpeg\r\n\r\n`,
    ];

    const bodyPrefix = Buffer.from(parts.join(''));
    const bodySuffix = Buffer.from(`\r\n--${boundary}--\r\n`);
    const fullBody = Buffer.concat([bodyPrefix, fileData, bodySuffix]);

    const req = https.request(
      `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`,
      {
        method: 'POST',
        headers: {
          'Content-Type': `multipart/form-data; boundary=${boundary}`,
          'Content-Length': fullBody.length,
        },
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve(json.secure_url);
            } else {
              reject(new Error(json.error?.message || `HTTP ${res.statusCode}: ${data}`));
            }
          } catch (e) {
            reject(new Error(`Failed to parse response: ${data}`));
          }
        });
      }
    );

    req.on('error', reject);
    req.write(fullBody);
    req.end();
  });
}

async function main() {
  console.log('====================================================');
  console.log('LostLink — Cloudinary Demo Catalog Image Migration');
  console.log('====================================================\n');

  if (!cloudName || !apiKey || !apiSecret) {
    console.error('ERROR: Cloudinary credentials missing!\n');
    console.log('Please provide your Cloudinary credentials via environment variables:');
    console.log('  export CLOUDINARY_CLOUD_NAME="your-cloud-name"');
    console.log('  export CLOUDINARY_API_KEY="your-api-key"');
    console.log('  export CLOUDINARY_API_SECRET="your-api-secret"');
    console.log('\nThen run:');
    console.log('  node scripts/upload-demo-images-to-cloudinary.js\n');
    console.log('List of 12 Curated Demo Images to be migrated:');
    CATALOG_ITEMS.forEach((item) => {
      console.log(`  - Item #${item.id}: ${item.name} (${item.filename})`);
    });
    process.exit(1);
  }

  const uploadsDir = path.resolve(__dirname, '../lostfound/uploads');
  if (!fs.existsSync(uploadsDir)) {
    console.error(`Uploads directory not found at: ${uploadsDir}`);
    process.exit(1);
  }

  console.log(`Connecting to Cloudinary account: ${cloudName}...`);
  console.log(`Reading demo image assets from: ${uploadsDir}\n`);

  const sqlStatements = [];

  for (const item of CATALOG_ITEMS) {
    const filePath = path.join(uploadsDir, item.filename);
    if (!fs.existsSync(filePath)) {
      console.warn(`WARNING: File not found: ${filePath}`);
      continue;
    }

    const publicId = `item_${item.id}_${item.filename.replace(/\.jpg$/, '')}`;
    process.stdout.write(`Uploading [Item #${item.id}] ${item.name}... `);

    try {
      const secureUrl = await uploadToCloudinary(filePath, publicId);
      console.log(`DONE`);
      console.log(`  -> URL: ${secureUrl}`);
      sqlStatements.push(`UPDATE items SET image_url = '${secureUrl}' WHERE id = ${item.id};`);
    } catch (err) {
      console.log(`FAILED (${err.message})`);
    }
  }

  console.log('\n====================================================');
  console.log('Migration Complete! Generated SQL Updates:');
  console.log('====================================================\n');
  console.log(sqlStatements.join('\n'));

  // Save to file for easy reference
  const outputPath = path.resolve(__dirname, '../update-cloudinary-image-urls.sql');
  fs.writeFileSync(outputPath, sqlStatements.join('\n') + '\n', 'utf8');
  console.log(`\nSQL updates saved to: ${outputPath}`);
}

main().catch((err) => {
  console.error('Fatal Error:', err);
  process.exit(1);
});

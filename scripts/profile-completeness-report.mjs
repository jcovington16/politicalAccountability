#!/usr/bin/env node

const apiBase = process.env.API_BASE_URL ?? 'http://localhost:8080';
const adminToken = process.env.ADMIN_API_TOKEN;
const args = process.argv.slice(2);
const namesFlag = args.indexOf('--names');
const names = namesFlag >= 0
  ? (args[namesFlag + 1] ?? '').split(',').map((name) => name.trim()).filter(Boolean)
  : args;

if (!adminToken) {
  console.error('ADMIN_API_TOKEN is required. Load .env or export it before running this report.');
  process.exit(1);
}
if (names.length === 0) {
  console.error('Usage: node scripts/profile-completeness-report.mjs --names "Name One,Name Two"');
  process.exit(1);
}

async function getJson(path, admin = false) {
  const response = await fetch(`${apiBase}${path}`, {
    headers: admin ? { Accept: 'application/json', 'X-Admin-Token': adminToken } : { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}: ${path}`);
  return response.json();
}

let failed = false;
for (const name of names) {
  try {
    const matches = await getJson(`/politicians/search/name?name=${encodeURIComponent(name)}`);
    if (matches.length === 0) {
      console.log(`\n${name}: no stored live profile`);
      failed = true;
      continue;
    }

    for (const politician of matches) {
      const report = await getJson(`/review/politicians/${politician.id}/completeness`, true);
      console.log(`\n${politician.firstName} ${politician.lastName}: ${report.score}% (${report.status})`);
      for (const field of report.fields) {
        const marker = field.complete ? 'OK' : 'MISSING';
        console.log(`  ${marker.padEnd(7)} ${field.label}: ${field.count}`);
        if (!field.complete) console.log(`          Next: ${field.internalNextStep}`);
      }
    }
  } catch (error) {
    failed = true;
    console.error(`\n${name}: ${error.message}`);
  }
}

if (failed) process.exitCode = 2;

#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const root = process.argv[2] ?? 'data/templates';
const errors = [];

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/;
const isoDateTimePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(Z)?$/;

function fail(file, row, message) {
  errors.push(`${file}${row ? ` row ${row}` : ''}: ${message}`);
}

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8').trim();
}

function parseCsv(file) {
  const lines = read(file).split(/\r?\n/);
  const headers = splitCsvLine(lines[0]);
  return lines.slice(1).map((line, index) => {
    const values = splitCsvLine(line);
    return Object.fromEntries(headers.map((header, column) => [header, values[column] ?? '']));
  }).map((row, index) => ({ ...row, __row: index + 2 }));
}

function splitCsvLine(line) {
  const values = [];
  let current = '';
  let quoted = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];

    if (char === '"' && quoted && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === ',' && !quoted) {
      values.push(current);
      current = '';
    } else {
      current += char;
    }
  }

  values.push(current);
  return values;
}

function requireFields(file, row, fields) {
  for (const field of fields) {
    if (!row[field]) fail(file, row.__row, `missing ${field}`);
  }
}

function validateUuid(file, row, field) {
  if (row[field] && !uuidPattern.test(row[field])) fail(file, row.__row, `${field} must be a UUID`);
}

function validateDate(file, row, field) {
  if (row[field] && !isoDatePattern.test(row[field])) fail(file, row.__row, `${field} must be YYYY-MM-DD`);
}

function validateDateTime(file, row, field) {
  if (row[field] && !isoDateTimePattern.test(row[field])) fail(file, row.__row, `${field} must be ISO local datetime or UTC datetime`);
}

const politicians = parseCsv('politicians.csv');
const bills = parseCsv('bills.csv');
const votes = parseCsv('votes.csv');
const politicianIds = new Set(politicians.map((row) => row.id));
const politicianExternalIds = new Set(politicians.map((row) => row.external_id));
const billIds = new Set(bills.map((row) => row.id));
const billExternalIds = new Set(bills.map((row) => row.external_id));

for (const row of politicians) {
  requireFields('politicians.csv', row, ['id', 'external_id', 'first_name', 'last_name', 'state', 'office', 'source_name', 'source_url', 'last_verified_at']);
  validateUuid('politicians.csv', row, 'id');
  validateDateTime('politicians.csv', row, 'last_verified_at');
}

for (const row of bills) {
  requireFields('bills.csv', row, ['id', 'external_id', 'bill_number', 'title', 'jurisdiction', 'chamber', 'introduced_date', 'status', 'source_name', 'source_url', 'last_verified_at']);
  validateUuid('bills.csv', row, 'id');
  validateDate('bills.csv', row, 'introduced_date');
  validateDateTime('bills.csv', row, 'last_verified_at');
  if (row.introduced_by && !politicianIds.has(row.introduced_by)) fail('bills.csv', row.__row, 'introduced_by does not match a politician id');
}

for (const row of votes) {
  requireFields('votes.csv', row, ['id', 'external_id', 'politician_id', 'bill_id', 'vote_type', 'vote_date', 'source_name', 'source_url', 'last_verified_at']);
  validateUuid('votes.csv', row, 'id');
  validateDate('votes.csv', row, 'vote_date');
  validateDateTime('votes.csv', row, 'last_verified_at');
  if (!['YEA', 'NAY', 'ABSTAIN'].includes(row.vote_type)) fail('votes.csv', row.__row, 'vote_type must be YEA, NAY, or ABSTAIN');
  if (!politicianIds.has(row.politician_id)) fail('votes.csv', row.__row, 'politician_id does not match a politician id');
  if (row.politician_external_id && !politicianExternalIds.has(row.politician_external_id)) fail('votes.csv', row.__row, 'politician_external_id does not match a politician external_id');
  if (!billIds.has(row.bill_id)) fail('votes.csv', row.__row, 'bill_id does not match a bill id');
  if (row.bill_external_id && !billExternalIds.has(row.bill_external_id)) fail('votes.csv', row.__row, 'bill_external_id does not match a bill external_id');
}

const articles = JSON.parse(read('news_articles.json'));
if (!Array.isArray(articles)) fail('news_articles.json', null, 'root value must be an array');

for (const [index, article] of articles.entries()) {
  const row = index + 1;
  for (const field of ['id', 'external_id', 'politician_id', 'title', 'source', 'url', 'published_at', 'article_type', 'source_quality', 'last_verified_at']) {
    if (!article[field]) fail('news_articles.json', row, `missing ${field}`);
  }
  if (article.id && !uuidPattern.test(article.id)) fail('news_articles.json', row, 'id must be a UUID');
  if (article.politician_id && !politicianIds.has(article.politician_id)) fail('news_articles.json', row, 'politician_id does not match a politician id');
  if (article.politician_external_id && !politicianExternalIds.has(article.politician_external_id)) fail('news_articles.json', row, 'politician_external_id does not match a politician external_id');
  if (article.published_at && !isoDateTimePattern.test(article.published_at)) fail('news_articles.json', row, 'published_at must be ISO local datetime or UTC datetime');
  if (article.last_verified_at && !isoDateTimePattern.test(article.last_verified_at)) fail('news_articles.json', row, 'last_verified_at must be ISO local datetime or UTC datetime');
}

if (errors.length > 0) {
  console.error(`Sample data dry-run failed with ${errors.length} issue(s):`);
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

console.log(`Sample data dry-run passed: ${politicians.length} politician(s), ${bills.length} bill(s), ${votes.length} vote(s), ${articles.length} article(s).`);

#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', 'pharmaSmart-app', 'src', 'main', 'webapp');
const MARKER = '$safeNavigationMigration(';

function walk(dir) {
  const results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) results.push(...walk(p));
    else if (entry.isFile() && (entry.name.endsWith('.html') || entry.name.endsWith('.ts'))) results.push(p);
  }
  return results;
}

function strip(text) {
  let out = '';
  let i = 0;
  let removed = 0;
  while (i < text.length) {
    const idx = text.indexOf(MARKER, i);
    if (idx === -1) { out += text.slice(i); break; }
    out += text.slice(i, idx);
    let depth = 1;
    let j = idx + MARKER.length;
    while (j < text.length && depth > 0) {
      const c = text[j];
      if (c === '(') depth++;
      else if (c === ')') { depth--; if (depth === 0) break; }
      j++;
    }
    if (depth !== 0) {
      out += text.slice(idx);
      break;
    }
    out += text.slice(idx + MARKER.length, j);
    i = j + 1;
    removed++;
  }
  return { text: out, removed };
}

let totalRemoved = 0;
let filesTouched = 0;
for (const file of walk(ROOT)) {
  const original = fs.readFileSync(file, 'utf8');
  if (!original.includes(MARKER)) continue;
  const { text, removed } = strip(original);
  if (removed > 0) {
    fs.writeFileSync(file, text);
    totalRemoved += removed;
    filesTouched++;
  }
}
console.log(`Removed ${totalRemoved} wrappers across ${filesTouched} files`);

#!/usr/bin/env node

/**
 * Prepare Sidecar Script
 * This script prepares the Spring Boot backend for bundling with Tauri:
 * 1. Builds the Spring Boot JAR (if not present)
 * 2. Copies the JAR to src-tauri/sidecar/
 * 3. Ensures the wrapper script is executable
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Paths
const projectRoot = path.resolve(__dirname, '..');
const targetDir = path.join(projectRoot, 'pharmaSmart-app', 'target');
const batchTargetDir = path.join(projectRoot, 'pharmaSmart-batch', 'target');
const sidecarDir = path.join(projectRoot, 'src-tauri', 'sidecar');
const wrapperScript = path.join(sidecarDir, 'pharmasmart-backend.bat');

console.log('📦 Preparing sidecar for Tauri build...');

// Ensure sidecar directory exists
if (!fs.existsSync(sidecarDir)) {
  fs.mkdirSync(sidecarDir, { recursive: true });
}

// Check if wrapper script exists
if (!fs.existsSync(wrapperScript)) {
  console.error('❌ Wrapper script not found:', wrapperScript);
  console.error('Please ensure pharmasmart-backend.bat exists in src-tauri/sidecar/');
  process.exit(1);
}

// Absolute path to Maven wrapper — execSync on Windows doesn't search cwd for .cmd files.
const mvnCmd = process.platform === 'win32'
  ? path.join(projectRoot, 'mvnw.cmd')
  : path.join(projectRoot, 'mvnw');

function findJars() {
  if (!fs.existsSync(targetDir)) return [];
  return fs
    .readdirSync(targetDir)
    .filter(f => f.startsWith('pharmaSmart-') && f.endsWith('.jar') && !f.includes('javadoc') && !f.includes('sources'));
}

// pharmaSmart-batch (nightly pipeline) — bundled and installed as a second Windows
// service alongside pharmaSmart-app. Its jar starts with "pharmaSmart-" too, so it
// is picked up by the same sidecar/pharmaSmart-*.jar resource glob in tauri.bundled*.conf.json.
function findBatchJars() {
  if (!fs.existsSync(batchTargetDir)) return [];
  return fs
    .readdirSync(batchTargetDir)
    .filter(
      f =>
        f.startsWith('pharmaSmart-batch-') &&
        f.endsWith('.jar') &&
        !f.includes('javadoc') &&
        !f.includes('sources') &&
        !f.startsWith('original-')
    );
}

// Find the Spring Boot JARs in the target directories
let jarFiles = findJars();
let batchJarFiles = findBatchJars();

// Angular pre-built flag: webapp:build:tauri (or webapp:prod) outputs index.html here.
// When it exists BEFORE prepare-sidecar runs, the existing JAR was assembled before
// Angular was available — repackage it so the frontend is included in classpath:/static/.
const angularIndexHtml = path.join(targetDir, 'classes', 'static', 'index.html');
const angularPreBuilt = fs.existsSync(angularIndexHtml);

if (jarFiles.length === 0 || batchJarFiles.length === 0) {
  console.log('⚠️  No JAR file found in target/ (app and/or batch)');
  console.log('🔨 Building Spring Boot application with Angular (-Pprod)...');
  try {
    execSync(`"${mvnCmd}" clean package -Pprod -DskipTests`, {
      cwd: projectRoot, stdio: 'inherit', shell: true,
    });
    jarFiles = findJars();
    batchJarFiles = findBatchJars();
    if (jarFiles.length === 0) {
      console.error('❌ Failed to build JAR file');
      process.exit(1);
    }
    if (batchJarFiles.length === 0) {
      console.error('❌ Failed to build pharmaSmart-batch JAR file');
      process.exit(1);
    }
  } catch (error) {
    console.error('❌ Maven build failed:', error.message);
    process.exit(1);
  }
} else if (angularPreBuilt) {
  // JAR exists but Angular was built after it — inject Angular files directly into the JAR
  // (ZIP update via PowerShell). Avoids Maven-in-Maven conflict when called from exec-maven-plugin.
  console.log('🔄 Angular already built — injecting into JAR (direct ZIP update)...');
  const jarPath = path.join(targetDir, jarFiles[0]);
  const staticDir = path.join(targetDir, 'classes', 'static');
  const psScript = path.join(projectRoot, 'scripts', 'inject-angular-into-jar.ps1');
  try {
    execSync(
      `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "${psScript}" -JarPath "${jarPath}" -StaticDir "${staticDir}"`,
      { cwd: projectRoot, stdio: 'inherit' }
    );
  } catch (error) {
    console.error('❌ Angular injection failed:', error.message);
    process.exit(1);
  }
}

// Copy the JAR file to sidecar directory
const jarFile = jarFiles[0];
const sourcePath = path.join(targetDir, jarFile);
const destPath = path.join(sidecarDir, jarFile);

console.log(`📋 Copying ${jarFile} to sidecar directory...`);

// Remove old JAR files from sidecar directory
const oldJars = fs.readdirSync(sidecarDir).filter(file => file.startsWith('pharmaSmart-') && file.endsWith('.jar'));
oldJars.forEach(oldJar => {
  fs.unlinkSync(path.join(sidecarDir, oldJar));
});

// Copy new JAR
fs.copyFileSync(sourcePath, destPath);
console.log(`✅ Copied ${jarFile} to sidecar directory`);

// Copy the pharmaSmart-batch JAR file to the sidecar directory
const batchJarFile = batchJarFiles[0];
const batchSourcePath = path.join(batchTargetDir, batchJarFile);
const batchDestPath = path.join(sidecarDir, batchJarFile);

console.log(`📋 Copying ${batchJarFile} to sidecar directory...`);

const oldBatchJars = fs
  .readdirSync(sidecarDir)
  .filter(file => file.startsWith('pharmaSmart-batch-') && file.endsWith('.jar'));
oldBatchJars.forEach(oldJar => {
  fs.unlinkSync(path.join(sidecarDir, oldJar));
});

fs.copyFileSync(batchSourcePath, batchDestPath);
console.log(`✅ Copied ${batchJarFile} to sidecar directory`);

// Make wrapper script executable on Unix-like systems
if (process.platform !== 'win32') {
  const shScript = path.join(sidecarDir, 'pharmasmart-backend.sh');
  if (fs.existsSync(shScript)) {
    try {
      fs.chmodSync(shScript, '755');
    } catch (error) {
      console.warn('⚠️  Could not make shell script executable:', error.message);
    }
  }
}

// Copy WinSW.exe to src-tauri/service/ so Tauri can bundle it as a resource
require('./copy-winsw');

// Copy pharmasmart-backup.exe to src-tauri/backup/ so Tauri can bundle it as a resource
require('./copy-backup-exe');

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
const targetDir = path.join(projectRoot, 'target');
const sidecarDir = path.join(projectRoot, 'src-tauri', 'sidecar');
const wrapperScript = path.join(sidecarDir, 'pharmasmart-backend.bat');

console.log('📦 Preparing sidecar for Tauri build...');

// Ensure sidecar directory exists
if (!fs.existsSync(sidecarDir)) {
  fs.mkdirSync(sidecarDir, { recursive: true });
  console.log('✅ Created sidecar directory');
}

// Check if wrapper script exists
if (!fs.existsSync(wrapperScript)) {
  console.error('❌ Wrapper script not found:', wrapperScript);
  console.error('Please ensure pharmasmart-backend.bat exists in src-tauri/sidecar/');
  process.exit(1);
}

// Find the Spring Boot JAR in target directory
let jarFiles = [];
if (fs.existsSync(targetDir)) {
  jarFiles = fs
    .readdirSync(targetDir)
    .filter(file => file.startsWith('pharmaSmart-') && file.endsWith('.jar') && !file.includes('javadoc') && !file.includes('sources'));
}

if (jarFiles.length === 0) {
  console.log('⚠️  No JAR file found in target/');
  console.log('🔨 Building Spring Boot application...');

  try {
    const mvnCmd = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
    execSync(`${mvnCmd} clean package -Pprod -DskipTests`, {
      cwd: projectRoot,
      stdio: 'inherit',
    });

    // Re-check for JAR files
    jarFiles = fs
      .readdirSync(targetDir)
      .filter(file => file.startsWith('pharmaSmart-') && file.endsWith('.jar') && !file.includes('javadoc') && !file.includes('sources'));

    if (jarFiles.length === 0) {
      console.error('❌ Failed to build JAR file');
      process.exit(1);
    }
  } catch (error) {
    console.error('❌ Maven build failed:', error.message);
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
  console.log(`🗑️  Removed old JAR: ${oldJar}`);
});

// Copy new JAR
fs.copyFileSync(sourcePath, destPath);
console.log(`✅ Copied ${jarFile} to sidecar directory`);

// Make wrapper script executable on Unix-like systems
if (process.platform !== 'win32') {
  const shScript = path.join(sidecarDir, 'pharmasmart-backend.sh');
  if (fs.existsSync(shScript)) {
    try {
      fs.chmodSync(shScript, '755');
      console.log('✅ Made pharmasmart-backend.sh executable');
    } catch (error) {
      console.warn('⚠️  Could not make shell script executable:', error.message);
    }
  }
}

console.log('✅ Sidecar preparation complete!');
console.log(`📦 Backend JAR: ${jarFile}`);
console.log(`📁 Sidecar directory: ${sidecarDir}`);

#!/bin/bash
set -e

echo "=== Packaging MaviMusic.dmg ==="

APP_DIR="MaviMusic_macOS/build/MaviMusic.app"
DMG_STAGING="MaviMusic_macOS/build/dmg_staging"
DMG_OUT="MaviMusic.dmg"

rm -rf "$DMG_STAGING" "$DMG_OUT"
mkdir -p "$DMG_STAGING"

# Copy MaviMusic.app
cp -R "$APP_DIR" "$DMG_STAGING/"

# Create symlink to /Applications
ln -s /Applications "$DMG_STAGING/Applications"

# Create disk image
hdiutil create -volname "MaviMusic" \
               -srcfolder "$DMG_STAGING" \
               -ov \
               -format UDZO \
               "$DMG_OUT"

echo "=== MaviMusic.dmg created successfully! ==="
ls -lh "$DMG_OUT"

#!/bin/bash
set -e

echo "=== Compiling MaviMusic (Native Swift macOS) ==="

APP_DIR="MaviMusic_macOS/build/MaviMusic.app"
CONTENTS="$APP_DIR/Contents"
MACOS="$CONTENTS/MacOS"
RESOURCES="$CONTENTS/Resources"

# Clean previous build
rm -rf "MaviMusic_macOS/build"
mkdir -p "$MACOS" "$RESOURCES"

# Copy resources
cp "MaviMusic_macOS/Sources/MaviMusic/Resources/AppIcon.icns" "$RESOURCES/"
cp "MaviMusic_macOS/Sources/MaviMusic/Resources/MenuBarIcon.png" "$RESOURCES/"
cp "MaviMusic_macOS/Sources/MaviMusic/Resources/player_host.html" "$RESOURCES/"

# Create Info.plist
cat << 'EOF' > "$CONTENTS/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>es</string>
    <key>CFBundleExecutable</key>
    <string>MaviMusic</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.music.mavimusic</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>MaviMusic</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>2.0.0</string>
    <key>CFBundleVersion</key>
    <string>200</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsArbitraryLoads</key>
        <true/>
    </dict>
</dict>
</plist>
EOF

echo "Compiling Swift sources with optimizations..."
SWIFT_FILES=$(find MaviMusic_macOS/Sources/MaviMusic -name "*.swift")

swiftc -target arm64-apple-macosx14.0 \
       -O \
       -framework SwiftUI \
       -framework AppKit \
       -framework WebKit \
       -framework AVFoundation \
       -framework MediaPlayer \
       $SWIFT_FILES \
       -o "$MACOS/MaviMusic"

echo "Signing application bundle..."
codesign --force --deep --sign - "$APP_DIR" || true

echo "=== MaviMusic.app compiled successfully! ==="
ls -lh "$MACOS/MaviMusic"

#!/bin/sh

set -eu

REPOSITORY_ROOT="$SRCROOT/.."
APP_INFO_PLIST="$TARGET_BUILD_DIR/$INFOPLIST_PATH"

VERSION_NAME="$(git -C "$REPOSITORY_ROOT" describe --tags --always --exclude beta)"
VERSION_CODE="$(git -C "$REPOSITORY_ROOT" rev-list --count HEAD)"
MARKETING_VERSION="$(printf '%s' "$VERSION_NAME" | sed -E 's/^v?([0-9]+\.[0-9]+\.[0-9]+).*/\1/')"

if ! printf '%s' "$MARKETING_VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  MARKETING_VERSION="0.0.0"
fi

/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $MARKETING_VERSION" "$APP_INFO_PLIST"
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $VERSION_CODE" "$APP_INFO_PLIST"

if /usr/libexec/PlistBuddy -c "Print :NEUPassVersionName" "$APP_INFO_PLIST" >/dev/null 2>&1; then
  /usr/libexec/PlistBuddy -c "Set :NEUPassVersionName $VERSION_NAME" "$APP_INFO_PLIST"
else
  /usr/libexec/PlistBuddy -c "Add :NEUPassVersionName string $VERSION_NAME" "$APP_INFO_PLIST"
fi

#!/bin/bash
set -e

# Find script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

BUILD_GRADLE="build.gradle.kts"
APP_YAML="src/main/appengine/app.yaml"

# Extract current version from build.gradle.kts
CURRENT_VERSION=$(grep -E '^version = "[0-9]+\.[0-9]+\.[0-9]+"' "$BUILD_GRADLE" | sed -E 's/version = "([^"]+)"/\1/')

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Could not find current version in $BUILD_GRADLE"
    exit 1
fi

# Bump patch version (X.Y.Z -> X.Y.Z+1)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"

echo "Bumping version: $CURRENT_VERSION -> $NEW_VERSION"

# Update build.gradle.kts
python3 -c "
with open('$BUILD_GRADLE', 'r') as f:
    content = f.read()
new_content = content.replace('version = \"$CURRENT_VERSION\"', 'version = \"$NEW_VERSION\"')
with open('$BUILD_GRADLE', 'w') as f:
    f.write(new_content)
"

# Update app.yaml
python3 -c "
with open('$APP_YAML', 'r') as f:
    content = f.read()
new_content = content.replace('ChoresServer-$CURRENT_VERSION-all.jar', 'ChoresServer-$NEW_VERSION-all.jar')
with open('$APP_YAML', 'w') as f:
    f.write(new_content)
"

echo "Updated $BUILD_GRADLE and $APP_YAML"

# Git commit, push, tag, push tag
echo "Committing release $NEW_VERSION..."
git add -A
git commit -m "Release $NEW_VERSION"

echo "Pushing code to origin..."
git push origin "$(git branch --show-current)"

echo "Creating tag v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Tagging v$NEW_VERSION"

echo "Pushing tag v$NEW_VERSION to origin..."
git push origin "v$NEW_VERSION"

echo "Successfully released v$NEW_VERSION!"

#!/usr/bin/env bash
set -e

# Zyvro Wiki Sync Script
# Synchronizes all markdown documentation in wiki/ to the GitHub Wiki repository.
# Usage: ./scripts/sync_wiki.sh [GITHUB_TOKEN]

TOKEN="${1:-$GITHUB_TOKEN}"

if [ -z "$TOKEN" ]; then
    echo "Usage: ./scripts/sync_wiki.sh <GITHUB_TOKEN>"
    echo "Or set GITHUB_TOKEN environment variable."
    exit 1
fi

WIKI_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../wiki" && pwd)"
TEMP_DIR="$(mktemp -d)"

REPO="Rishav7324/zyvro.wiki.git"
WIKI_URL="https://${TOKEN}@github.com/${REPO}"

echo "==> Preparing wiki deployment from $WIKI_DIR..."

cd "$TEMP_DIR"

if git clone "$WIKI_URL" repo 2>/dev/null; then
    cd repo
    echo "==> Cloned existing wiki repository."
else
    mkdir repo && cd repo
    git init
    git branch -m master
    git remote add origin "$WIKI_URL"
    echo "==> Initialized new local wiki repository."
fi

git config user.name "Rishav Raj"
git config user.email "rishav@zyvro.app"

# Copy all wiki markdown files
cp -v "$WIKI_DIR"/*.md .

git add .
if git diff-index --quiet HEAD -- 2>/dev/null; then
    echo "==> No changes to commit in wiki."
else
    git commit -m "docs(wiki): sync wiki pages from main repository"
    echo "==> Pushing wiki pages to GitHub..."
    git push -u origin master
    echo "==> Wiki synchronized successfully!"
fi

# Cleanup
rm -rf "$TEMP_DIR"

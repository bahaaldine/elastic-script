#!/bin/bash

set -e

# Configurable values
ES_REPO_URL="https://github.com/elastic/elasticsearch.git"
ES_TAG="v9.0.1"
ES_DIR="../elasticsearch"  # Where to clone the ES repo
PLUGIN_NAME="elastic-script"
PLUGIN_SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/$PLUGIN_NAME"
PLUGIN_DEST_DIR="$ES_DIR/x-pack/plugin/$PLUGIN_NAME"

echo "🔧 Setting up development environment for $PLUGIN_NAME"
echo "📁 Plugin source: $PLUGIN_SOURCE_DIR"
echo "📁 Target Elasticsearch dir: $ES_DIR"

# Step 1: Clone Elasticsearch if needed
if [ ! -d "$ES_DIR" ]; then
  echo "📥 Cloning Elasticsearch repo into $ES_DIR"
  git clone "$ES_REPO_URL" "$ES_DIR"
  cd "$ES_DIR"
  echo "🔀 Checking out tag $ES_TAG"
  git checkout tags/"$ES_TAG" -b "$PLUGIN_NAME-dev"
  cd -
else
  echo "✅ Elasticsearch already cloned at $ES_DIR"
fi

# Step 2: Remove previous plugin dir or symlink if it exists
if [ -L "$PLUGIN_DEST_DIR" ] || [ -d "$PLUGIN_DEST_DIR" ]; then
  echo "🔁 Removing existing plugin directory or symlink at $PLUGIN_DEST_DIR"
  rm -rf "$PLUGIN_DEST_DIR"
fi

# Step 3: Symlink plugin into x-pack
echo "🔗 Linking plugin directory into Elasticsearch x-pack"
ln -s "$PLUGIN_SOURCE_DIR" "$PLUGIN_DEST_DIR"

echo ""
echo "✅ Plugin linked at: $PLUGIN_DEST_DIR"
echo ""
echo "🚀 To build the plugin:"
echo "   cd $ES_DIR"
echo "   ./gradlew :x-pack:plugin:$PLUGIN_NAME:build"
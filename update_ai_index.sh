#!/bin/bash

# Create temporary file
TMP_FILE="/tmp/readme_content.md"

# Download the file
curl -s https://raw.githubusercontent.com/xemantic/xemantic-ai/refs/heads/main/README.md > "$TMP_FILE"

# Process the content and save to another temp file
TMP_PROCESSED="/tmp/processed_content.md"
sed -n '/^##/,$p' "$TMP_FILE" | sed 's/^##/###/g' > "$TMP_PROCESSED"

# Create a temporary file for the final content
TMP_FINAL="/tmp/final_content.md"

# Replace the content between markers in ai/index.md
awk '
  /<!-- include-start:https:\/\/raw\.githubusercontent\.com\/xemantic\/xemantic-ai\/refs\/heads\/main\/README\.md -->/ {
    print
    system("cat '"$TMP_PROCESSED"'")
    in_section=1
    next
  }
  /<!-- include-end:https:\/\/raw\.githubusercontent\.com\/xemantic\/xemantic-ai\/refs\/heads\/main\/README\.md -->/ {
    in_section=0
    print
    next
  }
  !in_section {
    print
  }
' ai/index.md > "$TMP_FINAL"

# Move the temporary file back to the original
mv "$TMP_FINAL" ai/index.md

# Clean up
rm "$TMP_FILE" "$TMP_PROCESSED"

echo "Content updated successfully!"
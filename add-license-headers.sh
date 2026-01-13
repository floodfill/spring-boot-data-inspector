#!/bin/bash

# Script to add MIT license headers to all Java files
# Copyright (c) 2026 Data Inspector Contributors

LICENSE_HEADER="/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */"

echo "Adding license headers to Java files..."

# Find all Java files that don't already have the license header
find data-inspector/src demo-app/src -name "*.java" -type f | while read -r file; do
    # Check if file already has license header
    if ! grep -q "Copyright (c) 2026 Data Inspector Contributors" "$file"; then
        echo "Adding license to: $file"
        # Create temp file with license header and original content
        echo "$LICENSE_HEADER" > "$file.tmp"
        cat "$file" >> "$file.tmp"
        mv "$file.tmp" "$file"
    else
        echo "Skipping (already has license): $file"
    fi
done

echo "Done! License headers added."

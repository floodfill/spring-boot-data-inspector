#!/bin/bash

echo "🚀 Data Inspector Demo"
echo "======================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. You have Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Check if Gradle is available
if command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
elif [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    echo "❌ Gradle is not installed and gradlew is not available."
    echo "Please install Gradle or create a wrapper with: gradle wrapper"
    exit 1
fi

echo "📦 Building the project..."
$GRADLE_CMD clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "🎯 Starting the demo application..."
echo ""
echo "Once started, open your browser to:"
echo "  📊 Dashboard: http://localhost:8080/data-inspector"
echo "  🔌 API:       http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

$GRADLE_CMD :demo-app:bootRun

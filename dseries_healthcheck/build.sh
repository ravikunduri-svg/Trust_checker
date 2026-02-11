#!/bin/bash
################################################################################
# ESP dSeries Health Check Tool - Build Script
# Version: 1.0.0
# Date: 2026-02-11
################################################################################

echo "═══════════════════════════════════════════════════════════════"
echo "  ESP dSeries Health Check Tool - Build Script"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ ERROR: Maven is not installed"
    echo "   Please install Maven 3.6 or higher"
    echo ""
    echo "   Linux: sudo yum install maven"
    echo "   Ubuntu: sudo apt-get install maven"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ ERROR: Java is not installed"
    echo "   Please install Java 8 or higher"
    echo ""
    echo "   Linux: sudo yum install java-1.8.0-openjdk"
    echo "   Ubuntu: sudo apt-get install openjdk-8-jdk"
    exit 1
fi

echo "✅ Maven version:"
mvn -version | head -1
echo ""

echo "✅ Java version:"
java -version 2>&1 | head -1
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "  Building Project"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Clean and build
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  ✅ BUILD SUCCESSFUL"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "  JAR file created:"
    echo "    target/dseries-healthcheck-1.0.0.jar"
    echo ""
    echo "  To run:"
    echo "    java -jar target/dseries-healthcheck-1.0.0.jar --quick"
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
else
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  ❌ BUILD FAILED"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "  Please check the error messages above"
    echo ""
    exit 1
fi

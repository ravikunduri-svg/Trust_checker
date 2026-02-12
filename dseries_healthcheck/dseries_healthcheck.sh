#!/bin/sh
# ============================================================================
# dSeries Health Check Tool - Unix/Linux/AIX Launcher
# ============================================================================
# Version: 2.1.0
# Date: 2026-02-12
#
# Usage: dseries_healthcheck.sh <dSeries_install_directory>
# Example: dseries_healthcheck.sh /opt/CA/WA_DE
# ============================================================================

# ============================================================================
# CONFIGURATION
# ============================================================================

TOOL_VERSION="2.1.0"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ============================================================================
# FUNCTIONS
# ============================================================================

print_banner() {
    echo ""
    echo "========================================================================"
    echo "  dSeries Health Check Tool v${TOOL_VERSION}"
    echo "========================================================================"
    echo ""
}

print_usage() {
    echo "ERROR: dSeries installation directory not provided"
    echo ""
    echo "Usage: $0 <dSeries_install_directory>"
    echo ""
    echo "Example:"
    echo "  $0 /opt/CA/WA_DE"
    echo "  $0 /usr/local/dseries"
    echo ""
}

print_error() {
    echo ""
    echo "========================================================================"
    echo "  Health Check Failed"
    echo "========================================================================"
    echo ""
}

print_complete() {
    EXIT_CODE=$1
    echo ""
    echo "========================================================================"
    echo "  Health Check Complete"
    echo "========================================================================"
    echo "  Exit Code: ${EXIT_CODE}"
    echo ""
    
    if [ ${EXIT_CODE} -eq 0 ]; then
        echo "  Status: SUCCESS - System is healthy"
    elif [ ${EXIT_CODE} -eq 1 ]; then
        echo "  Status: FAILURE - Critical issues found"
    else
        echo "  Status: ERROR - Health check encountered errors"
    fi
    
    echo ""
    echo "  Check the generated report for detailed results"
    echo "========================================================================"
    echo ""
}

# Detect OS
detect_os() {
    OS_TYPE=$(uname -s)
    case "${OS_TYPE}" in
        Linux*)     OS_NAME="Linux";;
        Darwin*)    OS_NAME="MacOS";;
        AIX*)       OS_NAME="AIX";;
        SunOS*)     OS_NAME="Solaris";;
        HP-UX*)     OS_NAME="HP-UX";;
        *)          OS_NAME="Unknown";;
    esac
    echo "  Detected OS: ${OS_NAME}"
}

# Find Java
find_java() {
    JAVA_CMD=""
    
    # Try dSeries bundled Java first
    if [ -x "${DSERIES_HOME}/jre/bin/java" ]; then
        JAVA_CMD="${DSERIES_HOME}/jre/bin/java"
        echo "  Using dSeries bundled Java: ${DSERIES_HOME}/jre"
        return 0
    elif [ -x "${DSERIES_HOME}/java/bin/java" ]; then
        JAVA_CMD="${DSERIES_HOME}/java/bin/java"
        echo "  Using dSeries bundled Java: ${DSERIES_HOME}/java"
        return 0
    fi
    
    # Try JAVA_HOME
    if [ -n "${JAVA_HOME}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        JAVA_CMD="${JAVA_HOME}/bin/java"
        echo "  Using Java from JAVA_HOME: ${JAVA_HOME}"
        return 0
    fi
    
    # Try PATH
    if command -v java >/dev/null 2>&1; then
        JAVA_CMD="java"
        echo "  Using system Java from PATH"
        return 0
    fi
    
    # Java not found
    echo "  ERROR: Java not found"
    echo "  Please ensure Java is installed and available in:"
    echo "    - ${DSERIES_HOME}/jre/bin/java (dSeries bundled)"
    echo "    - JAVA_HOME environment variable"
    echo "    - System PATH"
    return 1
}

# Build classpath
build_classpath() {
    CLASSPATH="${SCRIPT_DIR}/dseries-healthcheck.jar"
    
    # Add dSeries lib directory (use wildcard for all JARs)
    if [ -d "${DSERIES_HOME}/lib" ]; then
        echo "  Adding dSeries libraries from: ${DSERIES_HOME}/lib"
        
        # Use wildcard for all JARs (Java 6+ supports this)
        CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/lib/*"
        
        # Also add subdirectories if they exist
        if [ -d "${DSERIES_HOME}/lib/jdbc" ]; then
            CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/lib/jdbc/*"
        fi
        if [ -d "${DSERIES_HOME}/lib/ext" ]; then
            CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/lib/ext/*"
        fi
    fi
    
    # Add dSeries third-party libs
    if [ -d "${DSERIES_HOME}/third-party" ]; then
        echo "  Adding third-party libraries from: ${DSERIES_HOME}/third-party"
        CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/third-party/*"
    fi
    
    # Add dSeries ext directory
    if [ -d "${DSERIES_HOME}/ext" ]; then
        echo "  Adding extension libraries from: ${DSERIES_HOME}/ext"
        CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/ext/*"
    fi
    
    # Add webserver libs (often contains JDBC drivers)
    if [ -d "${DSERIES_HOME}/webserver/lib" ]; then
        echo "  Adding webserver libraries from: ${DSERIES_HOME}/webserver/lib"
        CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/webserver/lib/*"
    fi
    
    # AIX specific - add additional library paths
    if [ "${OS_NAME}" = "AIX" ]; then
        if [ -d "${DSERIES_HOME}/aix_lib" ]; then
            echo "  Adding AIX-specific libraries from: ${DSERIES_HOME}/aix_lib"
            CLASSPATH="${CLASSPATH}:${DSERIES_HOME}/aix_lib/*"
        fi
    fi
    
    echo "  OK: Classpath built with wildcard support"
    echo "  Classpath includes all JARs from lib directories"
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

print_banner

# Validate arguments
if [ -z "$1" ]; then
    print_usage
    exit 2
fi

DSERIES_HOME="$1"

# Remove trailing slash if present
DSERIES_HOME=$(echo "${DSERIES_HOME}" | sed 's:/*$::')

# Step 1: Validate dSeries installation
echo "[1/7] Validating dSeries installation directory..."

if [ ! -d "${DSERIES_HOME}" ]; then
    echo "  ERROR: Directory not found: ${DSERIES_HOME}"
    print_error
    exit 2
fi

if [ ! -d "${DSERIES_HOME}/conf" ]; then
    echo "  ERROR: conf directory not found in: ${DSERIES_HOME}"
    echo "  This does not appear to be a valid dSeries installation"
    print_error
    exit 2
fi

echo "  OK: ${DSERIES_HOME}"
echo ""

# Step 2: Detect OS
echo "[2/7] Detecting operating system..."
detect_os
echo "  OK"
echo ""

# Step 3: Check Java
echo "[3/7] Checking Java installation..."

if ! find_java; then
    print_error
    exit 2
fi

# Verify Java works
if ! "${JAVA_CMD}" -version >/dev/null 2>&1; then
    echo "  ERROR: Java command failed"
    print_error
    exit 2
fi

echo "  OK: Java is available"
echo ""

# Step 4: Build classpath
echo "[4/7] Building classpath..."
build_classpath
echo "  OK: Classpath built"
echo ""

# Step 5: Check health check JAR
echo "[5/7] Checking health check JAR..."

if [ ! -f "${SCRIPT_DIR}/dseries-healthcheck.jar" ]; then
    echo "  ERROR: dseries-healthcheck.jar not found"
    echo "  Location: ${SCRIPT_DIR}/dseries-healthcheck.jar"
    echo ""
    echo "  Please build the JAR file first:"
    echo "    cd ${SCRIPT_DIR}"
    echo "    javac -encoding UTF-8 DSeriesHealthCheck.java"
    echo "    jar cfm dseries-healthcheck.jar manifest.txt DSeriesHealthCheck*.class"
    print_error
    exit 2
fi

echo "  OK: ${SCRIPT_DIR}/dseries-healthcheck.jar"
echo ""

# Step 6: Check SQL queries file
echo "[6/7] Checking SQL queries configuration..."

SQL_CONFIG="${SCRIPT_DIR}/config/health_check_queries.sql"

if [ -f "${SQL_CONFIG}" ]; then
    echo "  OK: ${SQL_CONFIG}"
else
    echo "  WARNING: SQL queries file not found"
    echo "  Location: ${SQL_CONFIG}"
    echo "  Database checks will use defaults"
fi
echo ""

# Step 7: Run health check
echo "[7/7] Running health check..."
echo ""
echo "========================================================================"
echo ""

# Set Java options
JAVA_OPTS="-Xmx512m -Dfile.encoding=UTF-8"

# OS-specific Java options
case "${OS_NAME}" in
    AIX)
        # AIX specific options
        JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"
        ;;
    Solaris)
        # Solaris specific options
        JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"
        ;;
esac

# Run the health check
if [ -f "${SQL_CONFIG}" ]; then
    "${JAVA_CMD}" ${JAVA_OPTS} -cp "${CLASSPATH}" DSeriesHealthCheck "${DSERIES_HOME}" "${SQL_CONFIG}"
else
    "${JAVA_CMD}" ${JAVA_OPTS} -cp "${CLASSPATH}" DSeriesHealthCheck "${DSERIES_HOME}"
fi

EXIT_CODE=$?

print_complete ${EXIT_CODE}

exit ${EXIT_CODE}

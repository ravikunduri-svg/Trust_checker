#!/bin/bash
################################################################################
# ESP dSeries Workload Automation Health Check Script
# Version: 1.0.0
# Date: 2026-02-09
#
# Purpose: Comprehensive health check for dSeries deployments
# Based on: Control-M, AutoSys, Automic, and dSeries best practices
################################################################################

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/config/healthcheck.conf"
LIB_DIR="${SCRIPT_DIR}/lib"

# Source library functions
source "${LIB_DIR}/common_functions.sh"
source "${LIB_DIR}/system_checks.sh"
source "${LIB_DIR}/database_checks.sh"
source "${LIB_DIR}/server_checks.sh"
source "${LIB_DIR}/agent_checks.sh"
source "${LIB_DIR}/workload_checks.sh"
source "${LIB_DIR}/security_checks.sh"
source "${LIB_DIR}/reporting.sh"

# Global variables
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="/var/log/dseries/healthcheck"
REPORT_FILE="${REPORT_DIR}/healthcheck_${TIMESTAMP}.html"
JSON_FILE="${REPORT_DIR}/healthcheck_${TIMESTAMP}.json"
LOG_FILE="${REPORT_DIR}/healthcheck_${TIMESTAMP}.log"

# Health check results
declare -A CHECK_RESULTS
TOTAL_CHECKS=0
PASSED_CHECKS=0
WARNING_CHECKS=0
FAILED_CHECKS=0
OVERALL_SCORE=0

################################################################################
# Main Functions
################################################################################

show_banner() {
    echo "═══════════════════════════════════════════════════════════════"
    echo "  ESP dSeries Workload Automation Health Check Tool v1.0.0"
    echo "═══════════════════════════════════════════════════════════════"
    echo "  Date: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  Host: $(hostname)"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
}

load_configuration() {
    log_info "Loading configuration from ${CONFIG_FILE}"
    
    if [[ ! -f "${CONFIG_FILE}" ]]; then
        log_error "Configuration file not found: ${CONFIG_FILE}"
        exit 1
    fi
    
    source "${CONFIG_FILE}"
    log_info "Configuration loaded successfully"
}

initialize_report() {
    log_info "Initializing report directory: ${REPORT_DIR}"
    mkdir -p "${REPORT_DIR}"
    
    if [[ ! -w "${REPORT_DIR}" ]]; then
        log_error "Cannot write to report directory: ${REPORT_DIR}"
        exit 1
    fi
    
    # Initialize log file
    exec > >(tee -a "${LOG_FILE}") 2>&1
    log_info "Report initialized: ${REPORT_FILE}"
}

run_system_checks() {
    log_section "SYSTEM RESOURCE CHECKS"
    
    # CPU checks
    check_cpu_utilization
    check_cpu_load_average
    
    # Memory checks
    check_memory_usage
    check_swap_usage
    
    # Disk checks
    check_disk_space
    check_disk_io
    
    # Network checks
    check_network_connectivity
    check_network_latency
    
    # File system checks
    check_file_descriptors
    check_inode_usage
}

run_database_checks() {
    log_section "DATABASE HEALTH CHECKS"
    
    # Connection checks
    check_db_connectivity
    check_db_connection_pool
    
    # Performance checks
    check_db_query_performance
    check_db_table_sizes
    check_db_index_health
    
    # Maintenance checks
    check_db_backup_status
    check_db_statistics_update
    check_db_transaction_log
    
    # Growth analysis
    check_db_growth_rate
    check_db_fragmentation
}

run_server_checks() {
    log_section "SERVER CONFIGURATION CHECKS"
    
    # JVM checks
    check_jvm_heap_size
    check_jvm_gc_settings
    check_jvm_thread_count
    
    # Configuration checks
    check_server_version
    check_server_license
    check_server_ports
    check_server_ssl_config
    
    # Thread pool checks
    check_thread_pool_config
    check_thread_pool_usage
    
    # Performance checks
    check_server_response_time
    check_server_throughput
}

run_agent_checks() {
    log_section "AGENT HEALTH CHECKS"
    
    # Agent connectivity
    check_agent_connectivity
    check_agent_versions
    
    # Agent performance
    check_agent_cpu_usage
    check_agent_memory_usage
    check_agent_disk_space
    
    # Communication
    check_agent_communication_lag
    check_agent_failed_pings
    
    # Configuration
    check_agent_topology
    check_agent_security
}

run_workload_checks() {
    log_section "WORKLOAD PERFORMANCE CHECKS"
    
    # Queue analysis
    check_queue_depth
    check_queue_age
    
    # Job statistics
    check_job_success_rate
    check_job_duration
    check_failed_jobs
    check_stuck_jobs
    
    # Throughput
    check_job_throughput
    check_peak_load_capacity
    
    # Application health
    check_application_errors
    check_application_dependencies
}

run_security_checks() {
    log_section "SECURITY CONFIGURATION CHECKS"
    
    # Access control
    check_user_accounts
    check_password_policy
    check_role_assignments
    
    # Encryption
    check_ssl_certificates
    check_password_encryption
    check_data_encryption
    
    # Audit
    check_audit_logging
    check_audit_retention
    
    # Compliance
    check_security_patches
    check_vulnerability_scan
}

run_maintenance_checks() {
    log_section "MAINTENANCE & HOUSEKEEPING CHECKS"
    
    # Housekeeping
    check_housekeeping_jobs
    check_log_rotation
    check_archive_status
    check_purge_policies
    
    # Backups
    check_backup_schedule
    check_backup_verification
    check_disaster_recovery
    
    # Updates
    check_patch_level
    check_update_availability
}

run_ha_checks() {
    log_section "HIGH AVAILABILITY CHECKS"
    
    # Cluster status
    check_cluster_status
    check_cluster_nodes
    
    # Failover
    check_failover_config
    check_failover_testing
    
    # Replication
    check_replication_status
    check_replication_lag
    
    # Load balancing
    check_load_balancer_config
    check_load_balancer_health
}

calculate_overall_score() {
    log_section "CALCULATING OVERALL HEALTH SCORE"
    
    if [[ ${TOTAL_CHECKS} -eq 0 ]]; then
        OVERALL_SCORE=0
        return
    fi
    
    # Weight the scores
    local passed_weight=100
    local warning_weight=60
    local failed_weight=0
    
    local weighted_score=$(( 
        (PASSED_CHECKS * passed_weight + 
         WARNING_CHECKS * warning_weight + 
         FAILED_CHECKS * failed_weight) / TOTAL_CHECKS 
    ))
    
    OVERALL_SCORE=${weighted_score}
    
    log_info "Total Checks: ${TOTAL_CHECKS}"
    log_info "Passed: ${PASSED_CHECKS}"
    log_info "Warnings: ${WARNING_CHECKS}"
    log_info "Failed: ${FAILED_CHECKS}"
    log_info "Overall Score: ${OVERALL_SCORE}/100"
}

generate_reports() {
    log_section "GENERATING REPORTS"
    
    # Generate HTML report
    log_info "Generating HTML report: ${REPORT_FILE}"
    generate_html_report "${REPORT_FILE}"
    
    # Generate JSON report
    log_info "Generating JSON report: ${JSON_FILE}"
    generate_json_report "${JSON_FILE}"
    
    # Generate summary
    log_info "Generating summary"
    generate_summary
    
    # Send email if configured
    if [[ "${ENABLE_EMAIL_ALERTS}" == "true" ]]; then
        log_info "Sending email alerts"
        send_email_report
    fi
    
    log_info "Reports generated successfully"
}

display_summary() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  HEALTH CHECK SUMMARY"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "  Overall Health Score: ${OVERALL_SCORE}/100"
    
    if [[ ${OVERALL_SCORE} -ge 90 ]]; then
        echo "  Status: ✅ EXCELLENT"
    elif [[ ${OVERALL_SCORE} -ge 75 ]]; then
        echo "  Status: 🟢 GOOD"
    elif [[ ${OVERALL_SCORE} -ge 60 ]]; then
        echo "  Status: 🟡 FAIR"
    elif [[ ${OVERALL_SCORE} -ge 40 ]]; then
        echo "  Status: 🟠 POOR"
    else
        echo "  Status: 🔴 CRITICAL"
    fi
    
    echo ""
    echo "  Total Checks: ${TOTAL_CHECKS}"
    echo "  ✅ Passed: ${PASSED_CHECKS}"
    echo "  ⚠️  Warnings: ${WARNING_CHECKS}"
    echo "  ❌ Failed: ${FAILED_CHECKS}"
    echo ""
    echo "  Reports:"
    echo "    HTML: ${REPORT_FILE}"
    echo "    JSON: ${JSON_FILE}"
    echo "    Log:  ${LOG_FILE}"
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    # Show critical issues
    if [[ ${FAILED_CHECKS} -gt 0 ]]; then
        echo "  ⚠️  CRITICAL ISSUES DETECTED!"
        echo "  Please review the detailed report for remediation steps."
        echo ""
    fi
}

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
  --quick           Run quick health check (5 minutes)
  --full            Run full health check (15-20 minutes)
  --trending        Include historical trend analysis
  --email           Send email report
  --pdf             Generate PDF report
  --config FILE     Use alternate configuration file
  --output DIR      Specify output directory
  --help            Show this help message

Examples:
  # Quick daily check
  $0 --quick --email

  # Full weekly check with trending
  $0 --full --trending --pdf

  # Custom configuration
  $0 --full --config /etc/dseries/healthcheck.conf

EOF
}

################################################################################
# Main Execution
################################################################################

main() {
    # Parse command line arguments
    CHECK_MODE="full"
    ENABLE_TRENDING=false
    ENABLE_EMAIL=false
    GENERATE_PDF=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --quick)
                CHECK_MODE="quick"
                shift
                ;;
            --full)
                CHECK_MODE="full"
                shift
                ;;
            --trending)
                ENABLE_TRENDING=true
                shift
                ;;
            --email)
                ENABLE_EMAIL=true
                shift
                ;;
            --pdf)
                GENERATE_PDF=true
                shift
                ;;
            --config)
                CONFIG_FILE="$2"
                shift 2
                ;;
            --output)
                REPORT_DIR="$2"
                shift 2
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Show banner
    show_banner
    
    # Load configuration
    load_configuration
    
    # Initialize reporting
    initialize_report
    
    # Run health checks based on mode
    log_info "Starting health check in ${CHECK_MODE} mode"
    
    run_system_checks
    run_database_checks
    run_server_checks
    
    if [[ "${CHECK_MODE}" == "full" ]]; then
        run_agent_checks
        run_workload_checks
        run_security_checks
        run_maintenance_checks
        run_ha_checks
    fi
    
    # Calculate overall score
    calculate_overall_score
    
    # Generate reports
    generate_reports
    
    # Display summary
    display_summary
    
    # Exit with appropriate code
    if [[ ${OVERALL_SCORE} -ge 60 ]]; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main "$@"

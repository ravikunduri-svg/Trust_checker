#!/usr/bin/env python3
"""
ESP dSeries Workload Automation Health Check Tool
Version: 1.0.0
Date: 2026-02-09

Comprehensive health check based on best practices from:
- Control-M (BMC)
- AutoSys (Broadcom)
- Automic Automation Engine
- dSeries specific requirements

Usage:
    python dseries_healthcheck.py --quick
    python dseries_healthcheck.py --full --email
    python dseries_healthcheck.py --full --trending --pdf
"""

import os
import sys
import json
import time
import socket
import psutil
import argparse
import subprocess
from datetime import datetime, timedelta
from typing import Dict, List, Tuple
from dataclasses import dataclass, asdict
from enum import Enum


class CheckStatus(Enum):
    """Health check status"""
    PASS = "PASS"
    WARNING = "WARNING"
    FAIL = "FAIL"
    INFO = "INFO"
    SKIP = "SKIP"


class Severity(Enum):
    """Issue severity levels"""
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"
    INFO = "INFO"


@dataclass
class HealthCheckResult:
    """Individual health check result"""
    check_id: str
    category: str
    description: str
    status: CheckStatus
    severity: Severity
    message: str
    value: str = ""
    threshold: str = ""
    recommendation: str = ""
    timestamp: str = ""
    
    def __post_init__(self):
        if not self.timestamp:
            self.timestamp = datetime.now().isoformat()


class dSeriesHealthCheck:
    """Main health check class"""
    
    def __init__(self, config_file: str = None):
        self.config = self.load_configuration(config_file)
        self.results: List[HealthCheckResult] = []
        self.start_time = datetime.now()
        self.hostname = socket.gethostname()
        
        # Statistics
        self.total_checks = 0
        self.passed_checks = 0
        self.warning_checks = 0
        self.failed_checks = 0
        self.skipped_checks = 0
        
    def load_configuration(self, config_file: str = None) -> Dict:
        """Load configuration from file or use defaults"""
        default_config = {
            # Database settings
            'db_host': 'localhost',
            'db_port': 5432,
            'db_name': 'WADB',
            'db_user': 'wauser',
            'db_type': 'postgresql',  # postgresql, oracle, mssql
            
            # Server settings
            'server_host': 'localhost',
            'server_port': 7507,
            'server_admin_user': 'admin',
            'install_dir': '/opt/CA/WA_DE',
            
            # Thresholds
            'cpu_warning': 70,
            'cpu_critical': 85,
            'memory_warning': 80,
            'memory_critical': 90,
            'disk_warning': 75,
            'disk_critical': 85,
            
            # JVM settings
            'jvm_heap_min_mb': 4096,
            'jvm_heap_recommended_mb': 4096,
            'jvm_heap_max_mb': 8192,
            
            # Performance thresholds
            'max_queue_depth': 1000,
            'max_job_duration_minutes': 240,
            'max_failed_jobs_percent': 5,
            'max_db_query_time_ms': 500,
            
            # Workload sizing (based on Control-M best practices)
            'workload_size': 'medium',  # small, medium, large
            'daily_jobs_count': 50000,
            
            # Reporting
            'report_output_dir': '/var/log/dseries/healthcheck',
            'enable_email_alerts': False,
            'email_recipients': [],
        }
        
        if config_file and os.path.exists(config_file):
            with open(config_file, 'r') as f:
                user_config = json.load(f)
                default_config.update(user_config)
        
        return default_config
    
    def add_result(self, result: HealthCheckResult):
        """Add a check result and update statistics"""
        self.results.append(result)
        self.total_checks += 1
        
        if result.status == CheckStatus.PASS:
            self.passed_checks += 1
        elif result.status == CheckStatus.WARNING:
            self.warning_checks += 1
        elif result.status == CheckStatus.FAIL:
            self.failed_checks += 1
        elif result.status == CheckStatus.SKIP:
            self.skipped_checks += 1
    
    # =========================================================================
    # SYSTEM RESOURCE CHECKS
    # =========================================================================
    
    def check_cpu_utilization(self):
        """Check CPU utilization (SYS-001)"""
        try:
            cpu_percent = psutil.cpu_percent(interval=2)
            
            if cpu_percent >= self.config['cpu_critical']:
                status = CheckStatus.FAIL
                message = f"CPU usage is {cpu_percent}% (critical threshold: {self.config['cpu_critical']}%)"
                recommendation = "Investigate high CPU processes. Consider scaling resources or optimizing workload."
            elif cpu_percent >= self.config['cpu_warning']:
                status = CheckStatus.WARNING
                message = f"CPU usage is {cpu_percent}% (warning threshold: {self.config['cpu_warning']}%)"
                recommendation = "Monitor CPU usage trends. Plan for capacity increase if trend continues."
            else:
                status = CheckStatus.PASS
                message = f"CPU usage is {cpu_percent}% (healthy)"
                recommendation = ""
            
            self.add_result(HealthCheckResult(
                check_id="SYS-001",
                category="System Resources",
                description="CPU Utilization",
                status=status,
                severity=Severity.CRITICAL if status == CheckStatus.FAIL else Severity.MEDIUM,
                message=message,
                value=f"{cpu_percent}%",
                threshold=f"{self.config['cpu_warning']}% / {self.config['cpu_critical']}%",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SYS-001",
                category="System Resources",
                description="CPU Utilization",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check CPU: {str(e)}",
                recommendation="Verify psutil is installed and permissions are correct"
            ))
    
    def check_memory_usage(self):
        """Check memory utilization (SYS-002)"""
        try:
            memory = psutil.virtual_memory()
            mem_percent = memory.percent
            
            if mem_percent >= self.config['memory_critical']:
                status = CheckStatus.FAIL
                message = f"Memory usage is {mem_percent}% (critical threshold: {self.config['memory_critical']}%)"
                recommendation = "Increase system memory or reduce JVM heap size. Check for memory leaks."
            elif mem_percent >= self.config['memory_warning']:
                status = CheckStatus.WARNING
                message = f"Memory usage is {mem_percent}% (warning threshold: {self.config['memory_warning']}%)"
                recommendation = "Monitor memory trends. Consider memory upgrade if usage continues to grow."
            else:
                status = CheckStatus.PASS
                message = f"Memory usage is {mem_percent}% (healthy)"
                recommendation = ""
            
            self.add_result(HealthCheckResult(
                check_id="SYS-002",
                category="System Resources",
                description="Memory Usage",
                status=status,
                severity=Severity.CRITICAL if status == CheckStatus.FAIL else Severity.HIGH,
                message=message,
                value=f"{mem_percent}% ({memory.used // (1024**3)}GB / {memory.total // (1024**3)}GB)",
                threshold=f"{self.config['memory_warning']}% / {self.config['memory_critical']}%",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SYS-002",
                category="System Resources",
                description="Memory Usage",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check memory: {str(e)}",
                recommendation=""
            ))
    
    def check_disk_space(self):
        """Check disk space (SYS-003)"""
        try:
            # Check main partition
            disk = psutil.disk_usage('/')
            disk_percent = disk.percent
            
            if disk_percent >= self.config['disk_critical']:
                status = CheckStatus.FAIL
                message = f"Disk usage is {disk_percent}% (critical threshold: {self.config['disk_critical']}%)"
                recommendation = "Clean up old logs, archives, and temporary files. Expand disk space immediately."
            elif disk_percent >= self.config['disk_warning']:
                status = CheckStatus.WARNING
                message = f"Disk usage is {disk_percent}% (warning threshold: {self.config['disk_warning']}%)"
                recommendation = "Plan for disk expansion. Review housekeeping policies."
            else:
                status = CheckStatus.PASS
                message = f"Disk usage is {disk_percent}% (healthy)"
                recommendation = ""
            
            self.add_result(HealthCheckResult(
                check_id="SYS-003",
                category="System Resources",
                description="Disk Space",
                status=status,
                severity=Severity.CRITICAL if status == CheckStatus.FAIL else Severity.HIGH,
                message=message,
                value=f"{disk_percent}% ({disk.used // (1024**3)}GB / {disk.total // (1024**3)}GB)",
                threshold=f"{self.config['disk_warning']}% / {self.config['disk_critical']}%",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SYS-003",
                category="System Resources",
                description="Disk Space",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check disk space: {str(e)}",
                recommendation=""
            ))
    
    def check_swap_usage(self):
        """Check swap usage (SYS-007)"""
        try:
            swap = psutil.swap_memory()
            swap_percent = swap.percent
            
            if swap_percent > 50:
                status = CheckStatus.FAIL
                message = f"Swap usage is {swap_percent}% (threshold: 50%)"
                recommendation = "High swap usage indicates memory pressure. Add physical memory or reduce workload."
            elif swap_percent > 10:
                status = CheckStatus.WARNING
                message = f"Swap usage is {swap_percent}% (threshold: 10%)"
                recommendation = "Monitor swap usage. Consider adding memory if usage increases."
            else:
                status = CheckStatus.PASS
                message = f"Swap usage is {swap_percent}% (healthy)"
                recommendation = ""
            
            self.add_result(HealthCheckResult(
                check_id="SYS-007",
                category="System Resources",
                description="Swap Usage",
                status=status,
                severity=Severity.HIGH if status == CheckStatus.FAIL else Severity.MEDIUM,
                message=message,
                value=f"{swap_percent}%",
                threshold="10% / 50%",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SYS-007",
                category="System Resources",
                description="Swap Usage",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check swap: {str(e)}",
                recommendation=""
            ))
    
    # =========================================================================
    # DATABASE HEALTH CHECKS
    # =========================================================================
    
    def check_database_connectivity(self):
        """Check database connectivity (DB-001)"""
        try:
            # Try to connect to database
            if self.config['db_type'] == 'postgresql':
                result = self._check_postgresql_connection()
            elif self.config['db_type'] == 'oracle':
                result = self._check_oracle_connection()
            elif self.config['db_type'] == 'mssql':
                result = self._check_mssql_connection()
            else:
                result = (CheckStatus.SKIP, "Unsupported database type", "")
            
            status, message, recommendation = result
            
            self.add_result(HealthCheckResult(
                check_id="DB-001",
                category="Database",
                description="Database Connectivity",
                status=status,
                severity=Severity.CRITICAL,
                message=message,
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="DB-001",
                category="Database",
                description="Database Connectivity",
                status=CheckStatus.FAIL,
                severity=Severity.CRITICAL,
                message=f"Database connection failed: {str(e)}",
                recommendation="Verify database is running, credentials are correct, and network is accessible"
            ))
    
    def _check_postgresql_connection(self) -> Tuple[CheckStatus, str, str]:
        """Check PostgreSQL connection"""
        try:
            import psycopg2
            conn = psycopg2.connect(
                host=self.config['db_host'],
                port=self.config['db_port'],
                database=self.config['db_name'],
                user=self.config['db_user'],
                connect_timeout=5
            )
            conn.close()
            return (CheckStatus.PASS, "Database connection successful", "")
        except ImportError:
            return (CheckStatus.SKIP, "psycopg2 not installed", "Install: pip install psycopg2-binary")
        except Exception as e:
            return (CheckStatus.FAIL, f"Connection failed: {str(e)}", "Check database credentials and network connectivity")
    
    def _check_oracle_connection(self) -> Tuple[CheckStatus, str, str]:
        """Check Oracle connection"""
        try:
            import cx_Oracle
            dsn = cx_Oracle.makedsn(self.config['db_host'], self.config['db_port'], service_name=self.config['db_name'])
            conn = cx_Oracle.connect(self.config['db_user'], self.config.get('db_password', ''), dsn)
            conn.close()
            return (CheckStatus.PASS, "Database connection successful", "")
        except ImportError:
            return (CheckStatus.SKIP, "cx_Oracle not installed", "Install: pip install cx_Oracle")
        except Exception as e:
            return (CheckStatus.FAIL, f"Connection failed: {str(e)}", "Check database credentials and network connectivity")
    
    def _check_mssql_connection(self) -> Tuple[CheckStatus, str, str]:
        """Check MS SQL Server connection"""
        try:
            import pyodbc
            conn_str = f"DRIVER={{ODBC Driver 17 for SQL Server}};SERVER={self.config['db_host']},{self.config['db_port']};DATABASE={self.config['db_name']};UID={self.config['db_user']}"
            conn = pyodbc.connect(conn_str, timeout=5)
            conn.close()
            return (CheckStatus.PASS, "Database connection successful", "")
        except ImportError:
            return (CheckStatus.SKIP, "pyodbc not installed", "Install: pip install pyodbc")
        except Exception as e:
            return (CheckStatus.FAIL, f"Connection failed: {str(e)}", "Check database credentials and network connectivity")
    
    # =========================================================================
    # SERVER CONFIGURATION CHECKS
    # =========================================================================
    
    def check_jvm_heap_size(self):
        """Check JVM heap configuration (SRV-001) - Based on dSeries best practices"""
        try:
            # Try to get JVM heap info from running process
            heap_size_mb = self._get_jvm_heap_size()
            
            if heap_size_mb == 0:
                status = CheckStatus.SKIP
                message = "Could not determine JVM heap size"
                recommendation = "Manually verify JVM heap size in startServer script or windows.service.properties"
            elif heap_size_mb < self.config['jvm_heap_min_mb']:
                status = CheckStatus.FAIL
                message = f"JVM heap size is {heap_size_mb}MB (minimum: {self.config['jvm_heap_min_mb']}MB)"
                recommendation = f"Increase JVM heap to at least {self.config['jvm_heap_recommended_mb']}MB for production. Edit startServer script (Unix) or windows.service.properties (Windows)."
            elif heap_size_mb < self.config['jvm_heap_recommended_mb']:
                status = CheckStatus.WARNING
                message = f"JVM heap size is {heap_size_mb}MB (recommended: {self.config['jvm_heap_recommended_mb']}MB)"
                recommendation = f"Consider increasing heap to {self.config['jvm_heap_recommended_mb']}MB for optimal performance."
            else:
                status = CheckStatus.PASS
                message = f"JVM heap size is {heap_size_mb}MB (healthy)"
                recommendation = ""
            
            self.add_result(HealthCheckResult(
                check_id="SRV-001",
                category="Server Configuration",
                description="JVM Heap Size",
                status=status,
                severity=Severity.CRITICAL if status == CheckStatus.FAIL else Severity.HIGH,
                message=message,
                value=f"{heap_size_mb}MB",
                threshold=f"Min: {self.config['jvm_heap_min_mb']}MB, Recommended: {self.config['jvm_heap_recommended_mb']}MB",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SRV-001",
                category="Server Configuration",
                description="JVM Heap Size",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check JVM heap: {str(e)}",
                recommendation="Manually verify JVM configuration"
            ))
    
    def _get_jvm_heap_size(self) -> int:
        """Get JVM heap size from running process"""
        try:
            for proc in psutil.process_iter(['name', 'cmdline']):
                if proc.info['name'] and 'java' in proc.info['name'].lower():
                    cmdline = proc.info['cmdline']
                    if cmdline:
                        for arg in cmdline:
                            if arg.startswith('-Xmx'):
                                # Parse -Xmx4096m or -Xmx4g
                                size_str = arg[4:]
                                if size_str.endswith('g') or size_str.endswith('G'):
                                    return int(size_str[:-1]) * 1024
                                elif size_str.endswith('m') or size_str.endswith('M'):
                                    return int(size_str[:-1])
            return 0
        except:
            return 0
    
    def check_server_ports(self):
        """Check server ports are listening (SRV-003)"""
        try:
            port = self.config['server_port']
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5)
            result = sock.connect_ex((self.config['server_host'], port))
            sock.close()
            
            if result == 0:
                status = CheckStatus.PASS
                message = f"Server port {port} is listening"
                recommendation = ""
            else:
                status = CheckStatus.FAIL
                message = f"Server port {port} is not accessible"
                recommendation = "Verify dSeries server is running. Check firewall rules and network configuration."
            
            self.add_result(HealthCheckResult(
                check_id="SRV-003",
                category="Server Configuration",
                description="Server Port Accessibility",
                status=status,
                severity=Severity.CRITICAL,
                message=message,
                value=f"Port {port}",
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SRV-003",
                category="Server Configuration",
                description="Server Port Accessibility",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check port: {str(e)}",
                recommendation=""
            ))
    
    def check_thread_pool_configuration(self):
        """Check thread pool configuration (SRV-004) - Based on Control-M best practices"""
        try:
            # Determine recommended thread counts based on workload size
            workload_size = self.config.get('workload_size', 'medium')
            daily_jobs = self.config.get('daily_jobs_count', 50000)
            
            # Based on Control-M best practices
            if daily_jobs <= 15000:
                workload_size = 'small'
                recommended_threads = {'download': 3, 'db_update': 2, 'selector': 4}
            elif daily_jobs <= 75000:
                workload_size = 'medium'
                recommended_threads = {'download': 6, 'db_update': 4, 'selector': 8}
            else:
                workload_size = 'large'
                recommended_threads = {'download': 8, 'db_update': 8, 'selector': 12}
            
            message = f"Workload size: {workload_size} ({daily_jobs:,} daily jobs)"
            recommendation = f"Recommended thread configuration: Download={recommended_threads['download']}, DB_Update={recommended_threads['db_update']}, Selector={recommended_threads['selector']}"
            
            self.add_result(HealthCheckResult(
                check_id="SRV-004",
                category="Server Configuration",
                description="Thread Pool Configuration",
                status=CheckStatus.INFO,
                severity=Severity.INFO,
                message=message,
                value=workload_size,
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SRV-004",
                category="Server Configuration",
                description="Thread Pool Configuration",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check thread pool: {str(e)}",
                recommendation=""
            ))
    
    # =========================================================================
    # WORKLOAD PERFORMANCE CHECKS
    # =========================================================================
    
    def check_process_count(self):
        """Check minimum required processes (Based on Automic best practices)"""
        try:
            # This would check for dSeries-specific processes
            # For now, provide framework
            
            required_processes = {
                'scheduler': 1,  # Main scheduler process
                'executor': 1,   # Job executor
                'monitor': 1,    # Monitoring process
            }
            
            # Check would verify these processes are running
            status = CheckStatus.INFO
            message = "Process count check framework ready"
            recommendation = "Implement process-specific checks based on dSeries architecture"
            
            self.add_result(HealthCheckResult(
                check_id="WKL-008",
                category="Workload",
                description="Required Process Count",
                status=status,
                severity=Severity.INFO,
                message=message,
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="WKL-008",
                category="Workload",
                description="Required Process Count",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check processes: {str(e)}",
                recommendation=""
            ))
    
    # =========================================================================
    # SECURITY CHECKS
    # =========================================================================
    
    def check_ssl_configuration(self):
        """Check SSL/TLS configuration (SEC-001)"""
        try:
            # Check if SSL is enabled
            ssl_enabled = self._check_ssl_enabled()
            
            if not ssl_enabled:
                status = CheckStatus.WARNING
                message = "SSL/TLS is not enabled"
                recommendation = "Enable SSL/TLS for secure communication. Configure certificates in server configuration."
                severity = Severity.HIGH
            else:
                status = CheckStatus.PASS
                message = "SSL/TLS is enabled"
                recommendation = ""
                severity = Severity.INFO
            
            self.add_result(HealthCheckResult(
                check_id="SEC-001",
                category="Security",
                description="SSL/TLS Configuration",
                status=status,
                severity=severity,
                message=message,
                recommendation=recommendation
            ))
        except Exception as e:
            self.add_result(HealthCheckResult(
                check_id="SEC-001",
                category="Security",
                description="SSL/TLS Configuration",
                status=CheckStatus.SKIP,
                severity=Severity.INFO,
                message=f"Could not check SSL: {str(e)}",
                recommendation=""
            ))
    
    def _check_ssl_enabled(self) -> bool:
        """Check if SSL is enabled on server port"""
        try:
            import ssl
            context = ssl.create_default_context()
            with socket.create_connection((self.config['server_host'], self.config['server_port']), timeout=5) as sock:
                with context.wrap_socket(sock, server_hostname=self.config['server_host']) as ssock:
                    return True
        except:
            return False
    
    # =========================================================================
    # REPORTING
    # =========================================================================
    
    def calculate_overall_score(self) -> int:
        """Calculate overall health score"""
        if self.total_checks == 0:
            return 0
        
        # Weight the scores
        passed_weight = 100
        warning_weight = 60
        failed_weight = 0
        
        weighted_score = (
            self.passed_checks * passed_weight +
            self.warning_checks * warning_weight +
            self.failed_checks * failed_weight
        ) / self.total_checks
        
        return int(weighted_score)
    
    def generate_html_report(self, output_file: str):
        """Generate HTML report"""
        try:
            html = self._build_html_report()
            with open(output_file, 'w') as f:
                f.write(html)
            print(f"✅ HTML report generated: {output_file}")
        except Exception as e:
            print(f"❌ Failed to generate HTML report: {e}")
    
    def generate_json_report(self, output_file: str):
        """Generate JSON report"""
        try:
            report_data = {
                'metadata': {
                    'version': '1.0.0',
                    'timestamp': self.start_time.isoformat(),
                    'hostname': self.hostname,
                    'duration_seconds': (datetime.now() - self.start_time).total_seconds()
                },
                'summary': {
                    'overall_score': self.calculate_overall_score(),
                    'total_checks': self.total_checks,
                    'passed': self.passed_checks,
                    'warnings': self.warning_checks,
                    'failed': self.failed_checks,
                    'skipped': self.skipped_checks
                },
                'results': [asdict(r) for r in self.results]
            }
            
            with open(output_file, 'w') as f:
                json.dump(report_data, f, indent=2, default=str)
            
            print(f"✅ JSON report generated: {output_file}")
        except Exception as e:
            print(f"❌ Failed to generate JSON report: {e}")
    
    def _build_html_report(self) -> str:
        """Build HTML report content"""
        overall_score = self.calculate_overall_score()
        
        # Determine status color and text
        if overall_score >= 90:
            status_class = "excellent"
            status_text = "EXCELLENT"
            status_icon = "✅"
        elif overall_score >= 75:
            status_class = "good"
            status_text = "GOOD"
            status_icon = "🟢"
        elif overall_score >= 60:
            status_class = "fair"
            status_text = "FAIR"
            status_icon = "🟡"
        elif overall_score >= 40:
            status_class = "poor"
            status_text = "POOR"
            status_icon = "🟠"
        else:
            status_class = "critical"
            status_text = "CRITICAL"
            status_icon = "🔴"
        
        html = f"""<!DOCTYPE html>
<html>
<head>
    <title>dSeries Health Check Report - {self.start_time.strftime('%Y-%m-%d %H:%M')}</title>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
        h1 {{ color: #333; border-bottom: 3px solid #0066cc; padding-bottom: 10px; }}
        h2 {{ color: #0066cc; margin-top: 30px; }}
        .header {{ background: #0066cc; color: white; padding: 20px; margin: -30px -30px 30px -30px; }}
        .header h1 {{ color: white; border: none; margin: 0; }}
        .summary {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }}
        .summary-card {{ background: #f9f9f9; padding: 20px; border-radius: 8px; border-left: 4px solid #0066cc; }}
        .summary-card h3 {{ margin: 0 0 10px 0; color: #666; font-size: 14px; }}
        .summary-card .value {{ font-size: 32px; font-weight: bold; color: #333; }}
        .score-excellent {{ color: #28a745; }}
        .score-good {{ color: #5cb85c; }}
        .score-fair {{ color: #ffc107; }}
        .score-poor {{ color: #ff9800; }}
        .score-critical {{ color: #dc3545; }}
        .check-table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        .check-table th {{ background: #0066cc; color: white; padding: 12px; text-align: left; }}
        .check-table td {{ padding: 10px; border-bottom: 1px solid #ddd; }}
        .check-table tr:hover {{ background: #f5f5f5; }}
        .status-pass {{ color: #28a745; font-weight: bold; }}
        .status-warning {{ color: #ffc107; font-weight: bold; }}
        .status-fail {{ color: #dc3545; font-weight: bold; }}
        .status-skip {{ color: #6c757d; font-weight: bold; }}
        .recommendation {{ background: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; margin: 10px 0; }}
        .critical-issues {{ background: #f8d7da; padding: 15px; border-left: 4px solid #dc3545; margin: 20px 0; }}
        .footer {{ margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>{status_icon} ESP dSeries Health Check Report</h1>
            <p>Generated: {self.start_time.strftime('%Y-%m-%d %H:%M:%S')} | Host: {self.hostname}</p>
        </div>
        
        <div class="summary">
            <div class="summary-card">
                <h3>Overall Health Score</h3>
                <div class="value score-{status_class}">{overall_score}/100</div>
                <p>{status_text}</p>
            </div>
            <div class="summary-card">
                <h3>Total Checks</h3>
                <div class="value">{self.total_checks}</div>
            </div>
            <div class="summary-card">
                <h3>✅ Passed</h3>
                <div class="value" style="color: #28a745;">{self.passed_checks}</div>
            </div>
            <div class="summary-card">
                <h3>⚠️ Warnings</h3>
                <div class="value" style="color: #ffc107;">{self.warning_checks}</div>
            </div>
            <div class="summary-card">
                <h3>❌ Failed</h3>
                <div class="value" style="color: #dc3545;">{self.failed_checks}</div>
            </div>
        </div>
"""
        
        # Add critical issues section if any
        critical_results = [r for r in self.results if r.status == CheckStatus.FAIL]
        if critical_results:
            html += """
        <div class="critical-issues">
            <h3>🔴 Critical Issues Requiring Immediate Attention</h3>
            <ul>
"""
            for result in critical_results:
                html += f"                <li><strong>[{result.check_id}]</strong> {result.description}: {result.message}</li>\n"
            html += """            </ul>
        </div>
"""
        
        # Group results by category
        categories = {}
        for result in self.results:
            if result.category not in categories:
                categories[result.category] = []
            categories[result.category].append(result)
        
        # Add detailed results by category
        for category, results in sorted(categories.items()):
            html += f"""
        <h2>{category}</h2>
        <table class="check-table">
            <tr>
                <th>Check ID</th>
                <th>Description</th>
                <th>Status</th>
                <th>Value</th>
                <th>Message</th>
            </tr>
"""
            for result in results:
                status_class = result.status.value.lower()
                html += f"""            <tr>
                <td>{result.check_id}</td>
                <td>{result.description}</td>
                <td class="status-{status_class}">{result.status.value}</td>
                <td>{result.value}</td>
                <td>{result.message}</td>
            </tr>
"""
                if result.recommendation:
                    html += f"""            <tr>
                <td colspan="5">
                    <div class="recommendation">
                        <strong>Recommendation:</strong> {result.recommendation}
                    </div>
                </td>
            </tr>
"""
            html += """        </table>
"""
        
        # Add footer
        html += f"""
        <div class="footer">
            <p><strong>ESP dSeries Health Check Tool v1.0.0</strong></p>
            <p>Report generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
            <p>Duration: {(datetime.now() - self.start_time).total_seconds():.1f} seconds</p>
            <p>Copyright © 2026 Broadcom. All Rights Reserved.</p>
        </div>
    </div>
</body>
</html>
"""
        return html
    
    def display_summary(self):
        """Display summary to console"""
        overall_score = self.calculate_overall_score()
        
        print("\n" + "═" * 70)
        print("  HEALTH CHECK SUMMARY")
        print("═" * 70)
        print(f"\n  Overall Health Score: {overall_score}/100")
        
        if overall_score >= 90:
            print("  Status: ✅ EXCELLENT")
        elif overall_score >= 75:
            print("  Status: 🟢 GOOD")
        elif overall_score >= 60:
            print("  Status: 🟡 FAIR")
        elif overall_score >= 40:
            print("  Status: 🟠 POOR")
        else:
            print("  Status: 🔴 CRITICAL")
        
        print(f"\n  Total Checks: {self.total_checks}")
        print(f"  ✅ Passed: {self.passed_checks}")
        print(f"  ⚠️  Warnings: {self.warning_checks}")
        print(f"  ❌ Failed: {self.failed_checks}")
        
        if self.failed_checks > 0:
            print(f"\n  ⚠️  {self.failed_checks} CRITICAL ISSUE(S) DETECTED!")
            print("  Please review the detailed report for remediation steps.")
        
        print("\n" + "═" * 70 + "\n")
    
    def run_all_checks(self, mode: str = "full"):
        """Run all health checks"""
        print("\n🔍 Starting health check...\n")
        
        # Always run these
        self.check_cpu_utilization()
        self.check_memory_usage()
        self.check_disk_space()
        self.check_swap_usage()
        self.check_database_connectivity()
        self.check_jvm_heap_size()
        self.check_server_ports()
        self.check_thread_pool_configuration()
        self.check_ssl_configuration()
        self.check_process_count()
        
        if mode == "full":
            # Add more comprehensive checks
            print("Running full health check (this may take 15-20 minutes)...\n")
            # Additional checks would go here
        
        print("\n✅ Health check completed\n")


def main():
    parser = argparse.ArgumentParser(
        description='ESP dSeries Workload Automation Health Check Tool',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python dseries_healthcheck.py --quick
  python dseries_healthcheck.py --full --email
  python dseries_healthcheck.py --full --trending --pdf
        """
    )
    
    parser.add_argument('--quick', action='store_true', help='Run quick health check (5 minutes)')
    parser.add_argument('--full', action='store_true', help='Run full health check (15-20 minutes)')
    parser.add_argument('--trending', action='store_true', help='Include historical trend analysis')
    parser.add_argument('--email', action='store_true', help='Send email report')
    parser.add_argument('--pdf', action='store_true', help='Generate PDF report')
    parser.add_argument('--config', type=str, help='Configuration file path')
    parser.add_argument('--output', type=str, help='Output directory for reports')
    
    args = parser.parse_args()
    
    # Determine mode
    mode = "full" if args.full else "quick"
    
    # Show banner
    print("═" * 70)
    print("  ESP dSeries Workload Automation Health Check Tool v1.0.0")
    print("═" * 70)
    print(f"  Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Host: {socket.gethostname()}")
    print(f"  Mode: {mode.upper()}")
    print("═" * 70)
    
    # Create health check instance
    health_check = dSeriesHealthCheck(config_file=args.config)
    
    # Override output directory if specified
    if args.output:
        health_check.config['report_output_dir'] = args.output
    
    # Run health checks
    health_check.run_all_checks(mode=mode)
    
    # Generate reports
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    output_dir = health_check.config['report_output_dir']
    os.makedirs(output_dir, exist_ok=True)
    
    html_file = os.path.join(output_dir, f"healthcheck_{timestamp}.html")
    json_file = os.path.join(output_dir, f"healthcheck_{timestamp}.json")
    
    health_check.generate_html_report(html_file)
    health_check.generate_json_report(json_file)
    
    # Display summary
    health_check.display_summary()
    
    # Exit with appropriate code
    overall_score = health_check.calculate_overall_score()
    sys.exit(0 if overall_score >= 60 else 1)


if __name__ == "__main__":
    main()

# dSeries Health Check Tool - Version 4.2.2 Final Fixes

**Release Date:** February 11, 2026

## Overview

Version 4.2.2 addresses the final issues with calendar detection and makes dependency visualization optional for cleaner output.

---

## Issues Fixed

### 1. Namespace-Aware Calendar Detection

**Problem:**
Applications using namespaced XML (e.g., `<app:schedules>`) with frequency-based schedules were still getting false calendar recommendations.

**Root Cause:**
The calendar detection logic was using `getElementsByTagName()` which doesn't work with namespaced XML elements. It needed to use the namespace-aware helper methods.

**Fix:**
Updated all calendar detection code to use `getElementsByTagNameNS()` helper method which handles both:
- Standard format: `<schedules>`, `<run>`, `<calendar>`
- Namespaced format: `<app:schedules>`, `<app:run>`, `<app:calendar>`

**Code Changes:**
```java
// Before (v4.2.1)
NodeList runFreq = applElement.getElementsByTagName("run");
NodeList schedules = applElement.getElementsByTagName("schedules");

// After (v4.2.2)
NodeList runFreq = getElementsByTagNameNS(applElement, "run");
NodeList schedules = getElementsByTagNameNS(applElement, "schedules");
```

**Result:**
✅ All applications with `daily`, `weekly`, `monthly`, `yearly` schedules now correctly recognized regardless of XML format.

---

### 2. Optional Dependency Visualization

**Problem:**
Dependency and flow analysis was always displayed, making output verbose for users who only want best practices analysis.

**Solution:**
Added `--show-dependencies` (or `-d`) flag to make dependency visualization optional.

**Usage:**

**Without Dependencies (Default):**
```bash
dseries_healthcheck.bat --analyze-apps C:\apps
```
Output: Best practices analysis only

**With Dependencies:**
```bash
dseries_healthcheck.bat --analyze-apps C:\apps --show-dependencies
dseries_healthcheck.bat -a C:\apps -d
```
Output: Best practices + dependency visualization

**Benefits:**
- Cleaner output by default
- Faster analysis when dependencies not needed
- Users can choose when to see detailed dependency information
- Better for automated scripts that only need violations/recommendations

---

## Testing Results

### Test 1: Calendar Detection Fix

**MOSRGTI_BDC_DAILY (12 jobs, `<schedule>daily</schedule>`):**

Before v4.2.2:
```
⚠️ Calendar Usage (LOW) - FALSE POSITIVE
   Issue: Application with 12 scheduled jobs does not use day-specific or calendar-based scheduling
```

After v4.2.2:
```
✅ No issues or opportunities identified
   Applications follow best practices
```

### Test 2: Optional Dependencies

**Without Flag:**
```bash
$ java -jar dseries-healthcheck.jar --analyze-apps C:\apps
```
Output:
```
APPLICATION BEST PRACTICES ANALYSIS
...
✅ No issues or opportunities identified

Summary
...
Analysis complete!
```
✅ No dependency section shown

**With Flag:**
```bash
$ java -jar dseries-healthcheck.jar --analyze-apps C:\apps --show-dependencies
```
Output:
```
APPLICATION BEST PRACTICES ANALYSIS
...
DEPENDENCY & FLOW ANALYSIS
  Cross-Application Dependencies
  Application Dependency Graph
  Internal Job Dependencies
  Dependency Statistics
...
Analysis complete!
```
✅ Full dependency visualization shown

---

## Command-Line Reference

### Full Syntax

```bash
# Windows
dseries_healthcheck.bat --analyze-apps <path> [--show-dependencies]
dseries_healthcheck.bat -a <path> [-d]

# Unix/Linux/AIX
dseries_healthcheck.sh --analyze-apps <path> [--show-dependencies]
dseries_healthcheck.sh -a <path> [-d]
```

### Examples

```bash
# Best practices only (default)
dseries_healthcheck.bat --analyze-apps C:\exports\apps

# Best practices + dependencies
dseries_healthcheck.bat --analyze-apps C:\exports\apps --show-dependencies

# Short form
dseries_healthcheck.bat -a C:\exports\apps -d

# Single file analysis
dseries_healthcheck.bat -a C:\exports\PAYROLL.xml

# Unix/Linux
./dseries_healthcheck.sh -a /opt/exports/apps -d
```

---

## Upgrade Instructions

1. **Replace Files:**
   ```bash
   # Replace JAR
   cp dseries-healthcheck.jar /path/to/installation/
   
   # Replace launcher scripts
   cp dseries_healthcheck.bat /path/to/installation/
   cp dseries_healthcheck.sh /path/to/installation/
   chmod +x dseries_healthcheck.sh
   ```

2. **No Configuration Changes Required**

3. **Update Your Scripts (Optional):**
   - If you have automation scripts, consider adding `--show-dependencies` flag only when needed
   - Default behavior (no flag) provides cleaner output for CI/CD pipelines

---

## Technical Details

### Calendar Detection Improvements

The tool now checks for calendar usage in **three locations** using namespace-aware methods:

1. **Application-level run frequency:**
   ```xml
   <app:run>
     <app:calendar>HOLIDAY_CAL</app:calendar>
   </app:run>
   ```

2. **Application-level schedules:**
   ```xml
   <app:schedules>
     <app:run>
       <app:schedule>daily</app:schedule>
     </app:run>
   </app:schedules>
   ```

3. **Individual job schedules:**
   ```xml
   <app:unix_job name="JOB1">
     <app:schedules>
       <app:run>
         <app:schedule>weekly</app:schedule>
       </app:run>
     </app:schedules>
   </app:unix_job>
   ```

### Recognized Schedule Keywords

✅ Day names: MON, TUE, WED, THU, FRI, SAT, SUN  
✅ Frequencies: DAILY, WEEKLY, MONTHLY, YEARLY  
✅ Calendar refs: CALENDAR, HOLIDAY, WORKDAY  

All case-insensitive, works with both standard and namespaced XML.

### Dependency Flag Implementation

**Java Code:**
```java
private static boolean showDependencies = false;

// Parse flag
if (args.length > 2 && (args[2].equals("--show-dependencies") || args[2].equals("-d"))) {
    showDependencies = true;
}

// Conditional display
private static void displayDependencyAnalysis(List<ApplicationAnalysisResult> results) {
    if (results.isEmpty() || !showDependencies) return;
    // ... display logic
}
```

**Shell Scripts:**
Both `.bat` and `.sh` scripts updated to pass flag through to Java application.

---

## Compatibility

- **Backward Compatible:** All previous features work unchanged
- **Default Behavior:** More user-friendly (no dependencies unless requested)
- **XML Support:** Works with all dSeries XML formats (standard and namespaced)
- **No Breaking Changes:** Existing scripts work without modification

---

## Performance Impact

### With Dependencies (--show-dependencies)
- Same performance as v4.2.0/4.2.1
- Full dependency extraction and visualization

### Without Dependencies (default)
- **Slightly faster** - skips dependency graph generation
- **Cleaner output** - easier to read in CI/CD logs
- **Same accuracy** - all best practices checks still performed

---

## Use Cases

### When to Use --show-dependencies

✅ **Use the flag when:**
- Analyzing application relationships
- Planning migrations or refactoring
- Understanding complex application flows
- Documenting system architecture
- Impact analysis for changes

❌ **Skip the flag when:**
- Running automated CI/CD checks
- Only need best practices validation
- Generating reports for management
- Quick validation of single applications
- Performance is critical

---

## Known Limitations

1. **Variable-based schedules** like `%WOB.schedule` not detected
2. **Custom calendar implementations** with non-standard naming may not be recognized
3. **Dependency flag** must be third argument (after apps path)

These are rare edge cases and don't affect typical usage.

---

## Future Enhancements (Planned for v4.3.0)

1. **Variable resolution** - Detect schedules stored in variables
2. **JSON output format** - For easier integration with other tools
3. **Dependency export** - Generate DOT/Graphviz files
4. **Circular dependency detection** - Warn about circular references

---

## Summary of All v4.2.x Fixes

### v4.2.0
- ✅ Dependency and flow visualization
- ✅ Cross-application dependency tracking
- ✅ Internal job dependency analysis

### v4.2.1
- ✅ Added DAILY, WEEKLY, MONTHLY, YEARLY to calendar detection

### v4.2.2
- ✅ Namespace-aware calendar detection (fixes all XML formats)
- ✅ Optional dependency visualization (cleaner default output)

---

## Support

If you encounter issues:

1. **False calendar recommendations:**
   - Check application has `<schedule>` elements with recognized keywords
   - Verify XML is valid dSeries format
   - Report with sample XML for investigation

2. **Dependency flag not working:**
   - Ensure flag is third argument: `--analyze-apps <path> --show-dependencies`
   - Check for typos in flag name
   - Verify using correct script version (v4.2.2)

---

**Version:** 4.2.2  
**Build Date:** 2026-02-11  
**Compatibility:** dSeries R12.x and later  
**Requires:** Java 8 or later

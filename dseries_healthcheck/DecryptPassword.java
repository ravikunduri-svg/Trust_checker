import java.lang.reflect.Method;

/**
 * dSeries Password Decryption Utility
 * 
 * Decrypts passwords encrypted with dSeries Scrambler encryption.
 * Must be run with dSeries classpath to access Scrambler and RelationalDatabaseManager classes.
 * 
 * Usage:
 *   java DecryptPassword <encrypted_password>
 * 
 * Example:
 *   java DecryptPassword rRQcfTTiTZPX3plzpBwmvA==
 */
public class DecryptPassword {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DecryptPassword <encrypted_password>");
            System.out.println("Example: java DecryptPassword rRQcfTTiTZPX3plzpBwmvA==");
            System.exit(1);
        }
        
        String encryptedPassword = args[0];
        
        System.out.println("======================================================================");
        System.out.println("dSeries Password Decryption Utility");
        System.out.println("======================================================================");
        System.out.println();
        System.out.println("Encrypted Password: " + encryptedPassword);
        System.out.println();
        
        // Method 1: Try with RelationalDatabaseManager.getKey()
        String decrypted = decryptWithDynamicKey(encryptedPassword);
        if (decrypted != null && !decrypted.isEmpty()) {
            System.out.println("SUCCESS: Decryption successful!");
            System.out.println("Decrypted Password: " + decrypted);
            System.out.println();
            System.out.println("Method: Scrambler.recover() with RelationalDatabaseManager.getKey()");
            return;
        }
        
        // Method 2: Try with default key
        decrypted = decryptWithDefaultKey(encryptedPassword);
        if (decrypted != null && !decrypted.isEmpty()) {
            System.out.println("SUCCESS: Decryption successful!");
            System.out.println("Decrypted Password: " + decrypted);
            System.out.println();
            System.out.println("Method: Scrambler.recover() with default key 'RelationalDatabaseManager'");
            return;
        }
        
        // Method 3: Try Base64 decoding
        decrypted = decryptBase64(encryptedPassword);
        if (decrypted != null && !decrypted.isEmpty()) {
            System.out.println("SUCCESS: Decryption successful!");
            System.out.println("Decrypted Password: " + decrypted);
            System.out.println();
            System.out.println("Method: Base64 decoding");
            return;
        }
        
        // All methods failed
        System.out.println("ERROR: Decryption failed!");
        System.out.println();
        System.out.println("Possible reasons:");
        System.out.println("  1. dSeries libraries not in classpath");
        System.out.println("  2. Password is not encrypted with Scrambler");
        System.out.println("  3. Different encryption key is used");
        System.out.println();
        System.out.println("To run with dSeries classpath:");
        System.out.println("  Windows: java -cp \"<dSeries_home>\\lib\\*;.\" DecryptPassword " + encryptedPassword);
        System.out.println("  Linux:   java -cp \"<dSeries_home>/lib/*:.\" DecryptPassword " + encryptedPassword);
        System.exit(1);
    }
    
    /**
     * Decrypt using Scrambler with dynamic key from RelationalDatabaseManager
     */
    private static String decryptWithDynamicKey(String encryptedPassword) {
        System.out.println("Attempting decryption with RelationalDatabaseManager.getKey()...");
        try {
            Class<?> scramblerClass = Class.forName("com.ca.wa.publiclibrary.engine.library.crypto.Scrambler");
            Class<?> rdbmClass = Class.forName("com.ca.wa.core.engine.rdbms.RelationalDatabaseManager");
            
            // Get the encryption key from RelationalDatabaseManager
            Method getKeyMethod = rdbmClass.getMethod("getKey");
            Object keyObj = getKeyMethod.invoke(null);
            String key = keyObj != null ? keyObj.toString() : "RelationalDatabaseManager";
            
            System.out.println("  Key retrieved: " + key);
            
            // Decrypt using Scrambler.recover()
            Method recoverMethod = scramblerClass.getMethod("recover", String.class, String.class);
            Object result = recoverMethod.invoke(null, encryptedPassword, key);
            
            if (result != null && !result.toString().isEmpty()) {
                return result.toString();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("  WARNING: dSeries classes not found in classpath");
        } catch (Exception e) {
            System.out.println("  WARNING: Decryption failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Cause: " + e.getCause().getMessage());
            }
        }
        System.out.println();
        return null;
    }
    
    /**
     * Decrypt using Scrambler with default key
     */
    private static String decryptWithDefaultKey(String encryptedPassword) {
        System.out.println("Attempting decryption with default key 'RelationalDatabaseManager'...");
        try {
            Class<?> scramblerClass = Class.forName("com.ca.wa.publiclibrary.engine.library.crypto.Scrambler");
            
            // Decrypt using Scrambler.recover() with default key
            Method recoverMethod = scramblerClass.getMethod("recover", String.class, String.class);
            Object result = recoverMethod.invoke(null, encryptedPassword, "RelationalDatabaseManager");
            
            if (result != null && !result.toString().isEmpty()) {
                return result.toString();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("  WARNING: Scrambler class not found in classpath");
        } catch (Exception e) {
            System.out.println("  WARNING: Decryption failed: " + e.getMessage());
        }
        System.out.println();
        return null;
    }
    
    /**
     * Try Base64 decoding as fallback
     */
    private static String decryptBase64(String encryptedPassword) {
        System.out.println("Attempting Base64 decoding...");
        try {
            if (encryptedPassword.matches("^[A-Za-z0-9+/]+=*$") && encryptedPassword.length() > 10) {
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(encryptedPassword);
                String decoded = new String(decodedBytes);
                
                // Check if decoded string is printable ASCII
                if (decoded.matches("^[\\x20-\\x7E]+$")) {
                    return decoded;
                }
            }
        } catch (Exception e) {
            System.out.println("  WARNING: Base64 decoding failed: " + e.getMessage());
        }
        System.out.println();
        return null;
    }
}

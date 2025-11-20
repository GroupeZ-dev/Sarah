package fr.maxlego08.sarah.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Secure ObjectInputStream that prevents deserialization attacks by using a whitelist of allowed classes.
 * This prevents arbitrary code execution vulnerabilities (CVE-2015-7501, CVE-2017-7525, etc.)
 *
 * @since 1.0.0
 */
public class SecureObjectInputStream extends ObjectInputStream {

    /**
     * Whitelist of allowed classes for deserialization.
     * Only classes in this set can be deserialized.
     */
    private final Set<String> allowedClasses;

    /**
     * Whitelist of allowed package prefixes.
     * Classes from these packages can be deserialized.
     */
    private final Set<String> allowedPackagePrefixes;

    /**
     * Creates a SecureObjectInputStream with the specified allowed classes.
     *
     * @param in              the input stream to read from
     * @param allowedClasses  classes that are allowed to be deserialized
     * @throws IOException if an I/O error occurs
     */
    public SecureObjectInputStream(InputStream in, Class<?>... allowedClasses) throws IOException {
        super(in);
        this.allowedClasses = new HashSet<>();
        this.allowedPackagePrefixes = new HashSet<>();

        // Add safe primitive wrapper classes by default
        addSafeDefaults();

        // Add user-specified classes
        for (Class<?> clazz : allowedClasses) {
            this.allowedClasses.add(clazz.getName());
        }
    }

    /**
     * Creates a SecureObjectInputStream with allowed classes and package prefixes.
     *
     * @param in                      the input stream to read from
     * @param allowedClasses          specific classes that are allowed
     * @param allowedPackagePrefixes  package prefixes that are allowed (e.g., "fr.maxlego08.sarah.models")
     * @throws IOException if an I/O error occurs
     */
    public SecureObjectInputStream(InputStream in, Set<String> allowedClasses, Set<String> allowedPackagePrefixes) throws IOException {
        super(in);
        this.allowedClasses = new HashSet<>(allowedClasses);
        this.allowedPackagePrefixes = new HashSet<>(allowedPackagePrefixes);
        addSafeDefaults();
    }

    /**
     * Adds safe default classes that are unlikely to be exploited.
     */
    private void addSafeDefaults() {
        // Java primitive wrappers and basic types
        allowedClasses.add("java.lang.String");
        allowedClasses.add("java.lang.Integer");
        allowedClasses.add("java.lang.Long");
        allowedClasses.add("java.lang.Double");
        allowedClasses.add("java.lang.Float");
        allowedClasses.add("java.lang.Boolean");
        allowedClasses.add("java.lang.Byte");
        allowedClasses.add("java.lang.Short");
        allowedClasses.add("java.lang.Character");
        allowedClasses.add("java.lang.Number");

        // Date and time
        allowedClasses.add("java.util.Date");
        allowedClasses.add("java.sql.Date");
        allowedClasses.add("java.sql.Time");
        allowedClasses.add("java.sql.Timestamp");

        // Arrays of primitives
        allowedClasses.add("[B");  // byte[]
        allowedClasses.add("[C");  // char[]
        allowedClasses.add("[I");  // int[]
        allowedClasses.add("[J");  // long[]
        allowedClasses.add("[F");  // float[]
        allowedClasses.add("[D");  // double[]
        allowedClasses.add("[Z");  // boolean[]
        allowedClasses.add("[S");  // short[]

        // Common safe collections (empty or with safe elements only)
        allowedClasses.add("java.util.ArrayList");
        allowedClasses.add("java.util.HashMap");
        allowedClasses.add("java.util.HashSet");
        allowedClasses.add("java.util.LinkedList");
        allowedClasses.add("java.util.TreeMap");
        allowedClasses.add("java.util.TreeSet");

        // UUID
        allowedClasses.add("java.util.UUID");
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();

        // Check if class is explicitly allowed
        if (allowedClasses.contains(className)) {
            return super.resolveClass(desc);
        }

        // Check if class package prefix is allowed
        for (String prefix : allowedPackagePrefixes) {
            if (className.startsWith(prefix)) {
                return super.resolveClass(desc);
            }
        }

        // Reject all other classes to prevent deserialization attacks
        throw new InvalidClassException(
                "Unauthorized deserialization attempt",
                "Class " + className + " is not in the whitelist. " +
                        "This is a security measure to prevent deserialization attacks."
        );
    }

    /**
     * Adds a class to the whitelist of allowed classes.
     *
     * @param clazz the class to allow
     */
    public void allowClass(Class<?> clazz) {
        this.allowedClasses.add(clazz.getName());
    }

    /**
     * Adds a package prefix to the whitelist.
     * All classes from packages starting with this prefix will be allowed.
     *
     * @param packagePrefix the package prefix (e.g., "fr.maxlego08.sarah.models")
     */
    public void allowPackagePrefix(String packagePrefix) {
        this.allowedPackagePrefixes.add(packagePrefix);
    }
}
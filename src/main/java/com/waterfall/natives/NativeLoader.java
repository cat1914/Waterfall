package com.waterfall.natives;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Native library loader that extracts and loads native libraries from resources.
 *
 * After extraction, the temp directory is registered as {@code jna.library.path}
 * so that JNA's {@code Native.load("name")} can find them by short name.
 */
public class NativeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Boolean> loaded = new HashMap<>();
    private static final Map<String, String> loadedNames = new HashMap<>(); // shortName -> actual .so name (e.g., "heavy" -> "libheavy-0.0.1.so")

    /**
     * Load a native library from resources
     *
     * @param libraryName The name of the library (e.g., "direction" or "heavy")
     * @param version The version suffix (e.g., "0.0.1" or empty)
     */
    public static void loadLibrary(String libraryName, String version) {
        if (loaded.getOrDefault(libraryName, false)) {
            LOGGER.debug("Library {} already loaded", libraryName);
            return;
        }

        try {
            // Try to load directly first (may work if in system path)
            try {
                System.loadLibrary(libraryName);
                LOGGER.info("Loaded library {} from system path", libraryName);
                loaded.put(libraryName, true);
                loadedNames.put(libraryName, libraryName);
                return;
            } catch (UnsatisfiedLinkError e) {
                // Library not found in system path, try to extract from resources
            }

            // Get the OS-specific library name
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            String nativeName;
            String prefix = "";
            String suffix = ".so";

            if (osName.contains("linux")) {
                suffix = ".so";
                prefix = "lib";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                suffix = ".dylib";
                prefix = "lib";
            } else if (osName.contains("windows")) {
                suffix = ".dll";
                prefix = "";
            }

            String fullName = prefix + libraryName + "-" + version + suffix;
            String resourcePath = "/natives/" + fullName;

            LOGGER.info("Attempting to load native library from: {}", resourcePath);

            // Extract from resources to temp file
            InputStream in = NativeLoader.class.getResourceAsStream(resourcePath);
            if (in == null) {
                // Try without version
                fullName = prefix + libraryName + suffix;
                resourcePath = "/natives/" + fullName;
                in = NativeLoader.class.getResourceAsStream(resourcePath);
            }

            if (in == null) {
                LOGGER.error("Native library {} not found in resources", fullName);
                throw new UnsatisfiedLinkError("Library not found in resources: " + fullName);
            }

            // Create temp file
            Path tempDir = Files.createTempDirectory("waterfall-natives");
            File tempFile = new File(tempDir.toFile(), fullName);

            // Copy to temp file
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            in.close();

            // Make it executable on Unix
            if (!osName.contains("windows")) {
                tempFile.setExecutable(true, false);
            }

            // Register temp dir in jna.library.path so JNA's Native.load() can find it
            registerJnaLibraryPath(tempDir.toAbsolutePath().toString());

            // Load the library
            System.load(tempFile.getAbsolutePath());
            LOGGER.info("Loaded native library {} (short name: {}) from {}", libraryName, fullName, tempFile);
            loaded.put(libraryName, true);
            loadedNames.put(libraryName, fullName);

        } catch (Exception e) {
            LOGGER.error("Failed to load native library " + libraryName, e);
            throw new UnsatisfiedLinkError("Failed to load library " + libraryName + ": " + e.getMessage());
        }
    }

    /**
     * Append a directory to the JNA library search path. JNA reads the
     * {@code jna.library.path} system property when resolving short names.
     */
    private static synchronized void registerJnaLibraryPath(String dir) {
        String existing = System.getProperty("jna.library.path", "");
        String sep = System.getProperty("path.separator", ":");
        String updated;
        if (existing.isEmpty()) {
            updated = dir;
        } else {
            updated = existing + sep + dir;
        }
        System.setProperty("jna.library.path", updated);
        LOGGER.debug("jna.library.path set to: {}", updated);
    }

    /** Get the actual .so/.dylib/.dll file name registered for a short name. */
    public static String getLoadedFileName(String libraryName) {
        return loadedNames.get(libraryName);
    }

    /**
     * Load the direction library
     */
    public static void loadDirection() {
        loadLibrary("direction", "0.0.1");
    }

    /**
     * Load the heavy library
     */
    public static void loadHeavy() {
        loadLibrary("heavy", "0.0.1");
    }
}

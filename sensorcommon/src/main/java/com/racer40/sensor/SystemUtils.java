
package com.racer40.sensor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils {
	static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

	private static final String JAVA_LIBRARY_PATH = "java.library.path";

	public static final String PLUGINS = "plugins";

	private static Set<String> copied = new HashSet<>();
	private static Set<String> loaded = new HashSet<>();
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static void initializeEnvironment() {
		// check external libraries have been installed
		if (!isWindows()) {
			return;
		}

		// create folder hierarchy
		String path = getAsoluteAppFolder() + SystemUtils.PLUGINS;
		File f = new File(path);
		if (!f.exists()) {
			f.mkdirs();
		}
		if (isWindows64bits()) {
			copyResourceFolderToPlugins("x64/");
		} else {
			copyResourceFolderToPlugins("x32/");
		}
		copyResourceFolderToPlugins("tools/");

		// loadSystemLibs();
	}

	/**
	 * copy resources into pluggin folder
	 * 
	 * @param resourceFolder
	 */
	private static void copyResourceFolderToPlugins(String resourceFolder) {
		InputStream resourceAsStream = SystemUtils.class.getClassLoader().getResourceAsStream(resourceFolder);
		if (resourceAsStream == null) {
			return;
		}
		try {
			@SuppressWarnings("deprecation")
			List<String> files = IOUtils.readLines(resourceAsStream, Charsets.UTF_8);
			if (files.isEmpty()) {
				return;
			}
			String libraryPath = getAsoluteAppFolder() + SystemUtils.PLUGINS + "//" + resourceFolder;
			File dst = new File(libraryPath);
			if (!dst.exists()) {
				dst.mkdirs();
			}
			for (String f : files) {
				URL inputUrl = SystemUtils.class.getResource("/" + resourceFolder + "/" + f);
				String dstpath = libraryPath + "//" + f;
				dstpath = dstpath.replace("\\", "//");
				if (!copied.contains(dstpath)) {
					dst = new File(dstpath);
					FileUtils.copyURLToFile(inputUrl, dst);
					copied.add(dstpath);

					// // load library
					// if (dstpath.toLowerCase().endsWith(".dll")
					// && (dstpath.contains("x64") || dstpath.contains("x86"))) {
					// // try {
					// // System.load(dstpath);
					// // } catch (UnsatisfiedLinkError e) {
					// // logger.error("Native code library failed to load - {}", e);
					// // } finally {
					// // }
					// }

				}
			}

		} catch (IOException | SecurityException | IllegalArgumentException e) {
			logger.error("{}", e);
		}
	}

	public static String getLibraryFolder() {
		String path = getAsoluteAppFolder();
		if (isWindows64bits()) {
			path += "\\plugins\\x64\\";
		} else {
			path += "\\plugins\\x86\\";
		}
		return path;
	}

	public static String getToolsFolder() {
		return getAsoluteAppFolder() + "\\plugins\\tools\\";
	}

	public static boolean isWindows64bits() {
		boolean is64bit = false;
		if (System.getProperty("os.name").contains("Windows")) {
			is64bit = (System.getenv("ProgramFiles(x86)") != null);
		} else {
			is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
		}
		return is64bit;
	}

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	public static boolean isSolaris() {
		return (OS.indexOf("sunos") >= 0);
	}

	public static String getAsoluteAppFolder() {
		String path = System.getProperty("user.dir");
		path = path.replace("\\\\", "/");
		return path + "/";
	}

	/**
	 * update application library path
	 */
	public static boolean loadSystemLibs() {
		if (!isWindows()) {
			return false;
		}
		String libraryPath = getAsoluteAppFolder() + SystemUtils.PLUGINS;

		if (isWindows64bits()) {
			libraryPath += "\\x64";
		} else {
			libraryPath += "\\x86";
		}
		libraryPath = libraryPath.replace("/", "\\");
		try {
			String javapath = System.getProperty(SystemUtils.JAVA_LIBRARY_PATH);
			if (!javapath.contains(libraryPath)) {
				// System.setProperty(SystemUtils.JAVA_LIBRARY_PATH, javapath + ";" +
				// libraryPath);
			}
			String gamepath = System.getProperty("net.java.games.input.librarypath", "");
			if (!gamepath.contains(libraryPath)) {
				// System.setProperty("net.java.games.input.librarypath", libraryPath);
			}

		} catch (IllegalArgumentException | SecurityException e) {
			logger.error("{}", e);
		}

		return true;
	}

}

package com.racer40.sensor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils {
	private static final String JAVA_LIBRARY_PATH = "java.library.path";

	public static final String PLUGINS = "plugins";

	static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

	// additional libraries and tools installed
	protected static boolean isInitialized = false;

	private static String OS = System.getProperty("os.name").toLowerCase();

	public static void initializeEnvironment() {
		// check external libraries have been installed
		if (SystemUtils.isInitialized || !isWindows()) {
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

		SystemUtils.isInitialized = true;
	}

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
				dst = new File(libraryPath + "//" + f);
				FileUtils.copyURLToFile(inputUrl, dst);
			}

			// update library path
			// if (!System.getProperty(SystemUtils.JAVA_LIBRARY_PATH).contains(libraryPath))
			// {
			// String libpath = System.getProperty(SystemUtils.JAVA_LIBRARY_PATH) +
			// File.pathSeparator + libraryPath;
			// libpath = libpath.replace("/", "\\").replace("//", "\\");
			// logger.debug("sys path: {}", libpath);
			// System.setProperty(SystemUtils.JAVA_LIBRARY_PATH, libpath);
			// Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			// fieldSysPath.setAccessible(true);
			// fieldSysPath.set(null, null);
			// }

		} catch (IOException | SecurityException | IllegalArgumentException e) {
			logger.error("{}", e);
		}
	}

	public static String getLibraryPath() {
		String path = getAsoluteAppFolder();
		if (isWindows64bits()) {
			path += "\\x64";
		} else {
			path += "\\x86";
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

	/*
	 * load system dependant libraries
	 */
	public static boolean loadSystemLibs() {
		if (!isWindows()) {
			return false;
		}
		String path = getAsoluteAppFolder();
		logger.debug("app path: {}", path);
		if (isWindows64bits()) {
			path += "\\x64";
		} else {
			path += "\\x86";
		}
		try {
			System.setProperty(SystemUtils.JAVA_LIBRARY_PATH, path);
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("{}", e);
		} catch (NoSuchFieldException e) {
			logger.error("{}", e);
		} catch (SecurityException e) {
			logger.error("{}", e);
		}
		return true;
	}

}

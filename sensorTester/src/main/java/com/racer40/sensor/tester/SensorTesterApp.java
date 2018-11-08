package com.racer40.sensor.tester;

import java.io.File;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.common.SystemUtils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SensorTesterApp extends Application {

	private static final Logger log = LoggerFactory.getLogger(SensorTesterApp.class);

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		// load sensor libraries
		// SystemUtils.loadSystemLibs();

		String libraryPath = SystemUtils.getAsoluteAppFolder();
		if (SystemUtils.isWindows64bits()) {
			libraryPath += "\\plugins\\x64";
		} else {
			libraryPath += "\\plugins\\x86";
		}
		try {
			if (!System.getProperty("java.library.path").contains(libraryPath)) {
				String libpath = System.getProperty("java.library.path") + File.pathSeparator + libraryPath;
				libpath = libpath.replace("/", "\\").replace("//", "\\");
				System.setProperty("java.library.path", libpath);
				Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
				fieldSysPath.setAccessible(true);
				fieldSysPath.set(null, null);
			}

		} catch (IllegalArgumentException | IllegalAccessException e) {

		}

		String fxmlFile = "/fxml/sensorTester.fxml";
		log.debug("Loading FXML for main view from: {}", fxmlFile);
		FXMLLoader loader = new FXMLLoader();
		Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));

		log.debug("Showing JFX scene");
		Scene scene = new Scene(rootNode, 1250, 700);
		scene.getStylesheets().add("/styles/styles.css");

		stage.setTitle("RMS sensor tester");
		stage.setScene(scene);
		stage.show();
	}
}

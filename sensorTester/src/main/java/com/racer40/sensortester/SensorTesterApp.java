package com.racer40.sensortester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

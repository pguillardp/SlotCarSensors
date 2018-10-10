package com.racer40.sensor.tester;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.racer40.sensor.arduino.ArduinoMega;
import com.racer40.sensor.arduino.ArduinoUno;
import com.racer40.sensor.common.SensorInterface;
import com.racer40.sensor.common.SensorPinInterface;
import com.racer40.sensor.legacy.CarreraBB;
import com.racer40.sensor.legacy.CarreraCU;
import com.racer40.sensor.legacy.DS045;
import com.racer40.sensor.legacy.DS200;
import com.racer40.sensor.legacy.DS300;
import com.racer40.sensor.legacy.Gamepad;
import com.racer40.sensor.legacy.Scalex7042;
import com.racer40.sensor.legacy.ScalexRMS8143;
import com.racer40.sensor.legacy.Serial;
import com.racer40.sensor.legacy.Trackmate;
import com.racer40.sensor.phidget.Phidget1012;
import com.racer40.sensor.phidget.Phidget1014;
import com.racer40.sensor.phidget.Phidget1017;
import com.racer40.sensor.phidget.Phidget1018;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

public class TesterController implements Initializable {
	private static final Logger logger = LoggerFactory.getLogger(TesterController.class);

	@FXML
	private ListView<SensorInterface> cmbFoundSensors;

	@FXML
	private ImageView viewSensor;

	@FXML
	private ImageView viewSensorPinout;

	@FXML
	private ListView<SensorInterface> cmbSensorTypes;

	@FXML
	private Label sensorName;

	@FXML
	private AnchorPane main;

	@FXML
	private TextArea logs;

	@FXML
	private Button btnStartStop;

	@FXML
	private Label sensorVersion;

	@FXML
	private TextField editSetup;

	@FXML
	private ImageView statusImage;

	@FXML
	private ComboBox<String> cmbPort;

	@FXML
	void onClearLogs(ActionEvent event) {
		this.logs.setText("");
	}

	@FXML
	void onClearFoundSensors(ActionEvent event) {
		this.foundSensors.clear();
	}

	@FXML
	void onRefreshSensorTypes(ActionEvent event) {
		this.sensorTypes.clear();

		sensorTypes.add(new Serial());
		sensorTypes.add(new Gamepad());
		sensorTypes.add(new CarreraBB());
		sensorTypes.add(new CarreraCU());
		sensorTypes.add(new DS300());
		sensorTypes.add(new DS200());
		sensorTypes.add(new DS045());
		sensorTypes.add(new Scalex7042());
		sensorTypes.add(new ScalexRMS8143());
		sensorTypes.add(new Trackmate());

		sensorTypes.add(new ArduinoUno());
		sensorTypes.add(new ArduinoMega());

		sensorTypes.add(new Phidget1012());
		sensorTypes.add(new Phidget1014());
		sensorTypes.add(new Phidget1017());
		sensorTypes.add(new Phidget1018());
	}

	@FXML
	void onResetSensor(ActionEvent event) {
		if (this.currentSensor() != null) {
			this.currentSensor().reset();
		}
	}

	@FXML
	void onStartSensor(ActionEvent event) {
		if (this.currentSensor() != null) {
			if (this.currentSensor().isStarted()) {
				this.stopSensor();
			} else {
				this.startSensor();
			}
		}
	}

	@FXML
	void onDiscover(ActionEvent event) {
		if (!this.cmbSensorTypes.getSelectionModel().isEmpty()) {
			this.stopSensor();
			onClearFoundSensors(null);
			SensorInterface selectedItem = this.cmbSensorTypes.getSelectionModel().getSelectedItem();

			// listener may have been already attached => remove it first
			selectedItem.getEventLogger().removeListener(this.logListener);
			selectedItem.getEventLogger().addListener(this.logListener);

			selectedItem.getDiscoveredInterface().removeListener(this.discoveryListener);
			selectedItem.getDiscoveredInterface().addListener(this.discoveryListener);
			selectedItem.discover(-1l);
		}
	}

	@FXML
	void onPortChange(ActionEvent event) {
		if (this.currentSensor() != null) {
			this.stopSensor();
		}
	}

	private ObservableList<SensorInterface> sensorTypes = FXCollections.observableArrayList();
	private ObservableList<SensorInterface> foundSensors = FXCollections.observableArrayList();
	private ChangeListener<? super String> logListener;
	private ChangeListener<? super SensorInterface> discoveryListener;
	private ChangeListener<? super SensorPinInterface> pinChangeListener;
	private Map<SensorInterface, Group> interfaceGui = new HashMap<>();
	private Image led_off;
	private Image led_on;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		this.cmbSensorTypes.setItems(sensorTypes);
		this.cmbFoundSensors.setItems(foundSensors);

		InputStream is = getClass().getClassLoader().getResourceAsStream("images/led_off.png");
		this.led_off = new Image(is);
		this.statusImage.setImage(this.led_off);
		is = getClass().getClassLoader().getResourceAsStream("images/led_on.png");
		this.led_on = new Image(is);

		this.onRefreshSensorTypes(null);

		this.cmbPort.getItems().add("USB");
		for (int i = 1; i <= 32; i++) {
			this.cmbPort.getItems().add("COM" + i);
		}

		// appends text to log text area
		this.logListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			if (newValue != null) {
				Platform.runLater(() -> logs.appendText(newValue + "\n"));
			}
		};

		// called when a sensor has been discovered
		this.discoveryListener = (ObservableValue<? extends SensorInterface> observable, SensorInterface oldValue,
				SensorInterface newValue) -> {
			if (newValue != null) {
				Platform.runLater(() -> {
					currentSensor().setPort(newValue.getPort());
					currentSensor().setSetup(newValue.getSetup());
					foundSensors.add(newValue);
				});
				logger.debug("listener sensor found: {}", newValue);
			}
		};

		// called when apin status changes: change button color
		pinChangeListener = (ObservableValue<? extends SensorPinInterface> observable, SensorPinInterface oldValue,
				SensorPinInterface newValue) -> {
			if (newValue != null) {
				Platform.runLater(() -> {
					setPinButtonColor(newValue);
				});
			}
		};

		this.cmbSensorTypes.setCellFactory(new Callback<ListView<SensorInterface>, ListCell<SensorInterface>>() {
			@Override
			public ListCell<SensorInterface> call(ListView<SensorInterface> p) {
				return new ListCell<SensorInterface>() {
					@Override
					protected void updateItem(SensorInterface t, boolean bln) {
						super.updateItem(t, bln);
						if (t != null) {
							setText(t.getName());
						} else {
							setGraphic(null);
						}
					}
				};
			}
		});

		this.cmbSensorTypes.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				selectSensor(newValue, oldValue);
			}
		});

		cmbFoundSensors.setCellFactory(new Callback<ListView<SensorInterface>, ListCell<SensorInterface>>() {
			@Override
			public ListCell<SensorInterface> call(ListView<SensorInterface> param) {
				return new ListCell<SensorInterface>() {
					@Override
					public void updateItem(SensorInterface item, boolean empty) {
						super.updateItem(item, empty);
						if (!empty) {
							setText(item.getName() + " - " + item.getPort() + " - " + item.getSetup());
						} else {
							setGraphic(null);
						}
					}
				};
			}
		});

		this.cmbFoundSensors.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> {
					if (newValue != null) {
						for (int i = 0; i < this.sensorTypes.size(); i++) {
							if (this.sensorTypes.get(i).getType() == newValue.getType()) {
								this.cmbSensorTypes.getSelectionModel().select(i);
								break;
							}
						}
						for (int i = 0; i < this.cmbPort.getItems().size(); i++) {
							if (this.cmbPort.getItems().get(i).equalsIgnoreCase(newValue.getPort())) {
								this.cmbPort.getSelectionModel().select(i);
								break;
							}
						}
						editSetup.setText(newValue.getSetup());
					}
				});
	}

	private SensorInterface currentSensor() {
		return this.cmbSensorTypes.getSelectionModel().getSelectedItem();
	}

	private void startSensor() {
		if (this.currentSensor() != null) {
			this.stopSensor();
			this.currentSensor().setPort(this.cmbPort.getSelectionModel().getSelectedItem());
			SensorInterface currentSensor = this.currentSensor();
			currentSensor.setSetup(this.editSetup.getText());
			this.currentSensor().start();
			if (this.currentSensor().isStarted()) {
				this.statusImage.setImage(this.led_on);
				this.btnStartStop.setText("Stop");
			}

			// update pin colors
			for (SensorPinInterface pin : currentSensor.getPinList()) {
				setPinButtonColor(pin);
			}
		}
	}

	private void stopSensor() {
		if (this.currentSensor() != null) {
			this.currentSensor().stop();
			this.btnStartStop.setText("Start");
			this.statusImage.setImage(this.led_off);
		}
	}

	/**
	 * select selected type created sensor
	 * 
	 * @param sensorInterface
	 * @param oldValue
	 */
	private void selectSensor(SensorInterface sensorInterface, SensorInterface oldValue) {
		Platform.runLater(() -> {

			try {
				if (oldValue != null) {
					oldValue.stop();
					this.main.getChildren().remove(interfaceGui.get(oldValue));
					oldValue.getEventLogger().removeListener(logListener);
					oldValue.pinChangeProperty().removeListener(pinChangeListener);
				}

				viewSensor.setImage(sensorInterface.getSensorImage());
				Image pinout = sensorInterface.getPinout();
				this.viewSensorPinout.setImage(pinout);
				this.viewSensorPinout.setFitWidth(pinout.getWidth());
				this.viewSensorPinout.setFitHeight(pinout.getHeight());

				// build sensor pin GUI if needed
				if (!interfaceGui.containsKey(sensorInterface)) {
					Group group = new Group();
					for (SensorPinInterface pin : sensorInterface.getPinList()) {
						Button pinButton = new Button();
						pinButton.setId(pin.getPinIdentifier());
						pinButton.setTranslateX(pin.getBounds().getLayoutX());
						pinButton.setTranslateY(pin.getBounds().getLayoutY());
						pinButton.setPrefWidth(pin.getBounds().getWidth());
						pinButton.setPrefHeight(pin.getBounds().getHeight());
						Tooltip tt = new Tooltip();
						tt.setText(pin.getPinIdentifier() + " #" + pin.getName());
						pinButton.setTooltip(tt);
						if (pin.isOutput()) {
							pinButton.setOnAction((event) -> {
								int value = sensorInterface.getPinValue(pin.getPinIdentifier());
								sensorInterface.setOutputPinValue(pinButton.getId(), value > 0 ? 0 : 255);
							});
						}
						group.getChildren().add(pinButton);
					}
					interfaceGui.put(sensorInterface, group);
				}

				Group pinGroup = interfaceGui.get(sensorInterface);
				if (!this.main.getChildren().contains(pinGroup)) {
					this.main.getChildren().add(pinGroup);
					pinGroup.setTranslateX(this.viewSensorPinout.getLayoutX());
					pinGroup.setTranslateY(this.viewSensorPinout.getLayoutY());
				}

				this.currentSensor().getEventLogger().addListener(logListener);
				this.currentSensor().pinChangeProperty().addListener(pinChangeListener);

				this.editSetup.setText(this.currentSensor().getSetup());
				this.cmbPort.getSelectionModel().clearSelection();
				for (String s : this.cmbPort.getItems()) {
					if (s.equalsIgnoreCase(this.currentSensor().getPort())) {
						this.cmbPort.getSelectionModel().select(s);
						break;
					}
				}
				this.sensorName.setText(this.currentSensor().getName());

			} catch (Exception e) {
				logger.error("{}", e);
			} finally {

			}
		});
	}

	/**
	 * change pin interface button style/color
	 * 
	 * @param pinInterface
	 */
	protected void setPinButtonColor(SensorPinInterface pinInterface) {
		Group group = interfaceGui.get(this.currentSensor());
		String pinid = pinInterface.getPinIdentifier();
		if (group != null) {
			for (Node btn : group.getChildren()) {
				if (pinid.equals(btn.getId())) {
					if (pinInterface.isInput()) {
						if (pinInterface.getPinValue() > 0) {
							btn.setStyle(
									"-fx-background-color: #00FF00; -fx-border-color: #00FF00; -fx-border-width: 0px;");
						} else {
							btn.setStyle(
									"-fx-background-color: #008800; -fx-border-color: #008800; -fx-border-width: 0px;");
						}
					} else {
						if (pinInterface.getPinValue() > 0) {
							btn.setStyle(
									"-fx-background-color: #FF0000; -fx-border-color: #FF0000; -fx-border-width: 0px;");
						} else {
							btn.setStyle(
									"-fx-background-color: #880000; -fx-border-color: #880000; -fx-border-width: 0px;");
						}
					}
					break;
				}
			}
		}
	}

}

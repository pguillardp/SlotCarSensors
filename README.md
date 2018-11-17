# SlotCarSensors
This project contains code to manage miscellaneous slot car detectors for analogic (ex arduino) and digital (ex carrera control unit) circuits.
Its added value is a sensor interface normalization to split sensing logic developmend and race management system development.
The normalization relies on pin names, pin values, event dating and sensor usual feature wrapping (sensor detection, sensor configuration, sensor start/stop...).

Modules:
- sensorArduino, phidget, legacy: sensor source code
- sensorCommon: utilities used by all sensors
- sensorTester: javafx gui to test sensors and their code

All sensors implement the same interfaces:
- SensorInterface: normalizes acces to any kind of sensors (methods documented)
- SensorPinInterface: normalizes access to sensor pins

x2 utility classes implement the most commonly used functions:
SensorImpl, mother class of all sensor implementations
SensorPinImpl, pin management class used by all sensors

The sensor interface implementation is in charge to manage sensor/pin interfaces by changing slot car detector value in case of output, or raise a value change event to rms in case of input pin change.

Analogic sensors use digital io pins normalized by SensorPinInterface.
Digital sensors manage "virtual" pins corresponding to detection events (inputs, like car detection) or car setting changes (outputs like car speed change)
A pin name is normalized to be compliant with any race management system, and its value is set or changed according to detected events or modified parameteras follow:
<sensor setting/event>.<in|out>.<pin number>
  
Ex:
digital.in.10: digital pin 10 output for analogic circuit
digital.out.20: digital pin 20 inputs
car.in.5: car id=5 detection input ("virtual" pin)
pause.out.1: pause command for the circuit digital system

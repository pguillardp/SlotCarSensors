package com.racer40.joystickrmiserver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIInterface extends Remote {

	public void getComponents(List<String> components) throws RemoteException;

	public void getEvents(List<String> components) throws RemoteException;

	public void closeRMIServer() throws RemoteException;
}

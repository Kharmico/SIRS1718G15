package server.Services;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

public interface GatewayService extends Remote {
	//Admin Requests
	public String RegisterUser(String adminUsername, String adminPassword, String name, String password) throws RemoteException;
	
	public String DeleteUser(String adminUsername, String adminPassword, String name) throws RemoteException;
	
	//General Requests
	public List<ArrayList<String>> GetDeviceStatus(String username) throws RemoteException;
	
	public List<String> GetDeviceCommands(String username, String deviceName) throws RemoteException;
	
	public String SendCommand(String username, String deviceName, String command) throws RemoteException;
	
	public SecretKey ReplenishLogin(SecretKey userPublicKey, String username, String password, String authString) throws RemoteException;
	
	public String Login(String username, String password) throws RemoteException;
	
}


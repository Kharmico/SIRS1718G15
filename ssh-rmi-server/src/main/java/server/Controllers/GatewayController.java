package server.Controllers;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import server.Services.GatewayService;

public class GatewayController extends UnicastRemoteObject implements GatewayService {
	
	private static final long serialVersionUID = 1L;
	// GatewayController Variables
	public List<ArrayList<String>> devices = new ArrayList<ArrayList<String>>();
	
	
	// GatewayController Constructor
	public GatewayController() throws RemoteException {}
	
	//GatewayController Endpoints
	public String RegisterUser(String adminUsername, String adminPassword, String name, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	public String DeleteUser(String adminUsername, String adminPassword, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ArrayList<String>> GetDeviceStatus(String username) {
		return devices;
	}

	public List<String> GetDeviceCommands(String username, String deviceName) {
		// TODO Auto-generated method stub
		return null;
	}

	public String SendCommand(String username, String deviceName, String command) {
		// TODO Auto-generated method stub
		return null;
	}

	public SecretKey ReplenishLogin(SecretKey userPublicKey, String username, String password, String authString) {
		// TODO Auto-generated method stub
		return null;
	}

	public String Login(String username, String password) {
		// TODO Auto-generated method stub
		return null;
	}

}

package client;

import java.rmi.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.SecretKey;

import server.Services.GatewayService;

public class User {
	private SecretKey serverPublicKey;
	private GatewayService gateway;
	private Scanner keyboardSc;
	
	public User(GatewayService stub) throws RemoteException {
		gateway = stub;
		keyboardSc = new Scanner(System.in);
	}

	public String RegisterUser(String name, String password) throws RemoteException {
		String response = gateway.RegisterUser(null, null, name, password);
	
		return response;
	}

	public String DeleteUser(String name) throws RemoteException {
		String response = gateway.DeleteUser(null, null, name);
	
		return response;
	}

	public void GetDeviceStatus() throws RemoteException {
		List<List<String>> response = gateway.GetDeviceStatus(null);
		
		System.out.println("Device Name\t|\tStatus\t|\tType");
		for(List<String> device: response) {
			System.out.println( device.get(0) +"\t|\t"+ device.get(1) +"\t|\t"+ device.get(2));
		}
	}

	public void GetDeviceCommands(String deviceName) throws RemoteException {
		List<String> response = gateway.GetDeviceCommands(null, deviceName);
		
		System.out.println("Device Commands");
		for(String command: response) {
			System.out.println("-" + command);
		}
		
	}

	public String SendCommand(String command) throws RemoteException {
		String response = gateway.SendCommand(null, null, command);
		
		return response;
	}

	public String Login(String username, String password, String authString) throws RemoteException {
		String response = "";
		SecretKey responseKey = null;
		
		if(serverPublicKey != null ) {
			//Normal Login
			response = gateway.Login(username, password);
		}
		else {
			//Replenish Login
			if(authString != null) {
				responseKey = gateway.ReplenishLogin(null, username, password, authString);
				
				response = (responseKey != null)? "OK" : "NOK";
				
			}
			else {
				response = "NO_AUTH_STRING";
			}
		}
		
		return response;
	}
	
	
    public static void main(String[] args) throws Exception {

    	GatewayService server = null;
		try {
			server = (GatewayService) Naming.lookup(args[0]);
			System.out.println("Found server");

			while(true){
				User g = new User(server);
			}
			
			
			} catch (Exception e) {
				System.out.println("Lookup: " + e.getMessage());
		}

    }
}

package client;

import java.rmi.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.SecretKey;

import server.Services.GatewayService;

public class User {
	private SecretKey serverPublicKey;
	private GatewayService gateway;
	public Boolean adminFlag;
	
	
	public User(GatewayService stub) throws RemoteException {
		gateway = stub;
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

	public String SendCommand(String deviceName, String command) throws RemoteException {
		String response = gateway.SendCommand(null, deviceName,command);
		
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

    	StringBuilder sb = new StringBuilder();
    	
    	String input;
    	String[] parsedInput;
    	
    	Scanner keyboardSc = new Scanner(System.in);
    	
    	GatewayService server = null;
		
    	try {
			server = (GatewayService) Naming.lookup(args[0]);
			System.out.println("Found server");
			User g = new User(server);

			ioLoop: while(true){
				input = keyboardSc.nextLine();
				
				parsedInput = input.split(" ");
				
				switch(parsedInput[0]) {

				case "devices":
					g.GetDeviceStatus();
					break;

				case "commands":
					g.GetDeviceCommands(parsedInput[1]);
					break;

				case "request":					
					String command = String.join(" ", Arrays.copyOfRange(parsedInput, 3, parsedInput.length));
					
					g.SendCommand(parsedInput[1], command);
					break;

				case "login":
					if(parsedInput.length == 3) {
						g.Login(parsedInput[1], parsedInput[2], null);
					}
					else if(parsedInput.length == 4) {
						g.Login(parsedInput[1], parsedInput[2], parsedInput[3]);
					}
					else {
						System.out.println("Unrecognized login command");
					}
					break;

				case "register":

					if(parsedInput.length == 3) {
						g.RegisterUser(parsedInput[1], parsedInput[2]);
					}
					else {
						System.out.println("Unrecognized register command");
					}
					break;

				case "delete":

					if(parsedInput.length == 2) {
						g.DeleteUser(parsedInput[1]);
					}
					else {
						System.out.println("Unrecognized delete command");
					}
					
					break;

				case "exit":
					break ioLoop;
					
				case "help":
						System.out.println("devices - for existing devices and their status\n"
								+ ">> devices");
						System.out.println("commands - request the commands available for a device\n"
								+ ">> commands deviceName");
						System.out.println("request - to request a command on a device\n"
								+ ">> request deviceName -c command");
						System.out.println("login - used to start a session on the gateway\n"
								+ ">> login username password [authenticationString]");
						System.out.println("register - [ADMIN ONLY]register a new user in the network\n"
								+ ">> register username password");
						System.out.println("delete - [AMIN ONLY]delete a user from the network\n"
								+ ">> delete username");
						System.out.println("help - for existing commands\n"
								+ ">> help");
						System.out.println("exit - to exit the client program\n"
								+ ">> exit");
						break;
				
					default:
						System.out.println("Command not recognized");
						break;
				}
				
			}
			
			
			} catch (Exception e) {
				System.out.println("Lookup: " + e.getMessage());
		}

    }
}

package client;

import java.io.UnsupportedEncodingException;
import java.rmi.*;
import java.security.Key;
import java.security.PublicKey;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.joda.time.DateTime;

import server.Services.GatewayService;
import utils.DateUtil;
import utils.EncryptionUtil;
import utils.MaintenanceUtil;

public class User {
	//Static
	private static final String UTF8 = "UTF-8";
	
	//Encryption
	private String gatewayPublicKeyPath;
	
	private EncryptionUtil svEncryption;
	private EncryptionUtil encryption;
	private ArrayList<String> nounces = new ArrayList<String>();
	
	private DateTime cleanSchedule = new DateTime().now();
	
	
	
	//Program
	private GatewayService gateway;
	private byte[] token;
	
	//Methods
	public User(GatewayService stub) throws RemoteException {
		gateway = stub;
		token = null;
		
		encryption = new EncryptionUtil("keys/adminUserPublicKey.key","keys/adminUserPrivateKey.key");
		svEncryption = new EncryptionUtil();

		byte[] response = stub.GetPublicKey();
		byte[] decoded = svEncryption.base64Decoder(response);
		Key pureResponse =svEncryption.byteArrayToPubKey(decoded);
		
		svEncryption.setPublicKey(pureResponse, "gateway");
		
		gatewayPublicKeyPath = "gatewayPublicKey.key";
		
		System.out.println("You can now communicate with the Server [Help for commands]");
	}

	public void RegisterUser(String adminName, String adminPassword, String name, String password) throws RemoteException, UnsupportedEncodingException {
		if(token == null) {
			System.out.println("Login before doing an action!");
			return;
		}

		
		String pureAuthCode = randomString();
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = adminName + adminPassword + name + password + pureAuthCode + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}
		
		//encrypt
		byte[] aName = svEncryption.encrypt(adminName.getBytes(UTF8));
		byte[] aPassword = svEncryption.encrypt(adminPassword.getBytes(UTF8));

		byte[] nName = svEncryption.encrypt(name.getBytes(UTF8));
		byte[] nPassword = svEncryption.encrypt(password.getBytes(UTF8));
		
		byte[] authcode = svEncryption.encrypt(pureAuthCode.getBytes(UTF8));
		
		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<byte[]> response = gateway.RegisterUser(aName, aPassword, nName, nPassword, authcode, nounce, signature, token);
		
		//decrypt response
		String pureResponse = new String(encryption.decrypt(response.get(2)), UTF8);

		
		try {	
			if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse, cleanSchedule, nounces, encryption, svEncryption)) {
				if(pureResponse.equals("OK")) {
					System.out.println("User succesfully registered");
					System.out.println("Your new user should use this string to authenticate: " + pureAuthCode);
					new EncryptionUtil().generateKeys(name + "User");
				}
				else if(pureResponse.equals("NOK")) {
					System.out.println("User could not be registered");
				}
				else if(pureResponse.equals("INVALID_TOKEN")) {
					System.out.println("Your session has ended");
				}
				else {
					System.out.println("Something went wrong!");
				}
				
				return;
			}
			System.out.println("Something went wrong with your request!");
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("[WARNING] Unsupported encoding!");
		}catch (ParseException e) {
			System.out.println("[WARNING] Couldn't parse some data!");
		}catch (SignatureException e) {
			System.out.println("[WARNING] Couldn't verify some data!");
		}
		
		return;
		
	}

	public void DeleteUser(String adminName, String adminPassword, String name) throws RemoteException, UnsupportedEncodingException {
		if(token == null) {
			System.out.println("Login before doing an action!");
			return;
		}
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = adminName + adminPassword + name + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}
		
		//encrypt
		byte[] aName = svEncryption.encrypt(adminName.getBytes(UTF8));
		byte[] aPassword = svEncryption.encrypt(adminPassword.getBytes(UTF8));

		byte[] nName = svEncryption.encrypt(name.getBytes(UTF8));
		
		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<byte[]> response = gateway.DeleteUser(aName, aPassword, nName, nounce, signature, token);
	
		//decrypt response
		String pureResponse = new String(encryption.decrypt(response.get(2)), UTF8);

		
		try {	
			if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse, cleanSchedule, nounces, encryption, svEncryption)) {
				if(pureResponse.equals("OK")) {
					System.out.println("User succesfully deleted");
				}
				else if(pureResponse.equals("NOK")) {
					System.out.println("User could not be deleted");
				}
				else if(pureResponse.equals("INVALID_TOKEN")) {
					System.out.println("Your session has ended");
				}
				else {
					System.out.println("Something went wrong!");
				}
				return;
			}
			System.out.println("Something went wrong with your request!");
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("[WARNING] Unsupported encoding!");
		}catch (ParseException e) {
			System.out.println("[WARNING] Couldn't parse some data!");
		}catch (SignatureException e) {
			System.out.println("[WARNING] Couldn't verify some data!");
		}
		
		return;
	}

	public void GetDeviceStatus() throws RemoteException, UnsupportedEncodingException {
		if(token == null) {
			System.out.println("Login before doing an action!");
			return;
		}
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = "getDeviceStatus" + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}
		
		//encrypt		
		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<ArrayList<byte[]>> response = gateway.GetDeviceStatus(nounce, signature, token);
		
		//decrypt response
		List<ArrayList<String>> pureResponse = new ArrayList<ArrayList<String>>();

		for(ArrayList<byte[]> byteArray: response.subList(1, response.size() - 1)) {
			String deviceName = new String(encryption.decrypt(byteArray.get(0)), UTF8);
			String deviceStatus = new String(encryption.decrypt(byteArray.get(1)), UTF8);
			String deviceType = new String(encryption.decrypt(byteArray.get(2)), UTF8);
			
			pureResponse.add(new ArrayList<String>(Arrays.asList(deviceName, deviceStatus, deviceType)));
		}
		
		try {	
			if(MaintenanceUtil.checkResponse(response.get(0).get(0), response.get(0).get(1), pureResponse.toString(), cleanSchedule, nounces, encryption, svEncryption)) {
				if(pureResponse.get(0).equals("INVALID_TOKEN")) {
					System.out.println("Your session has ended");
					return;
				}
				
				System.out.println("Device Name\t|\tStatus\t|\tType");
				if(response != null) {
					for(int i = 1; i < pureResponse.size(); i++) {
						List<String> device = pureResponse.get(i);
						System.out.println( device.get(0) +"\t|\t"+ device.get(1) +"\t|\t"+ device.get(2));
					}
				}
			}
			System.out.println("Something went wrong with your request!");
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("[WARNING] Unsupported encoding!");
		}catch (ParseException e) {
			System.out.println("[WARNING] Couldn't parse some data!");
		}catch (SignatureException e) {
			System.out.println("[WARNING] Couldn't verify some data!");
		}
	}

	public void GetDeviceCommands(String deviceName) throws RemoteException, UnsupportedEncodingException {
		if(token == null) {
			System.out.println("Login before doing an action!");
			return;
		}
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = deviceName + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}
		
		//encrypt
		byte[] dName = svEncryption.encrypt(deviceName.getBytes(UTF8));
		
		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));
		
		List<byte[]> response = gateway.GetDeviceCommands(dName, nounce, signature, token);
		
		//decrypt response		
		List<String> pureResponse = new ArrayList<String>();
		
		for(byte[] byteArray: response.subList(2, response.size() - 1)) {
			pureResponse.add(new String(encryption.decrypt(byteArray), UTF8));
		}

		
		try {	
			if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse.toString(), cleanSchedule, nounces, encryption, svEncryption)) {
				if(pureResponse.get(0).equals("INVALID_TOKEN")) {
					System.out.println("Your session has ended");
					return;
				}
				
				System.out.println("Device Commands");
				for(String command: pureResponse) {
					System.out.println(">> " + command);
				}
			}
			System.out.println("Something went wrong with your request!");
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("[WARNING] Unsupported encoding!");
		}catch (ParseException e) {
			System.out.println("[WARNING] Couldn't parse some data!");
		}catch (SignatureException e) {
			System.out.println("[WARNING] Couldn't verify some data!");
		}
		
		
		
	}

	public void SendCommand(String deviceName, String command) throws RemoteException, UnsupportedEncodingException {
		if(token == null) {
			System.out.println("Login before doing an action!");
			return;
		}
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = deviceName + command + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}
		
		//encrypt
		byte[] dName = svEncryption.encrypt(deviceName.getBytes(UTF8));
		byte[] sCommand = svEncryption.encrypt(command.getBytes(UTF8));
		
		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<byte[]> response = gateway.SendCommand(dName, sCommand, nounce, signature, token);
		
		//decrypt response
		String pureResponse = new String(encryption.decrypt(response.get(3)), UTF8);

		
		try {	
			if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse, cleanSchedule, nounces, encryption, svEncryption)) {
				if(pureResponse.equals("OK")) {
					System.out.println("Command Succesfully Executed");
				}
				else if(pureResponse.equals("NOK")) {
					System.out.println("Command could not be Executed");
				}
				else if(pureResponse.equals("INVALID_TOKEN")) {
					System.out.println("Your session has ended");
				}
				else {
					System.out.println("Something went wrong!");
				}
			}
			System.out.println("Something went wrong with your request!");
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("[WARNING] Unsupported encoding!");
		}catch (ParseException e) {
			System.out.println("[WARNING] Couldn't parse some data!");
		}catch (SignatureException e) {
			System.out.println("[WARNING] Couldn't verify some data!");
		}
	}

	public void Login(String username, String password, String authString) throws RemoteException, UnsupportedEncodingException {

		encryption.setKeyPaths("keys/"+username+"UserPublicKey.key", "keys/"+username+"UserPrivateKey.key");
		
		
		List<byte[]> response = null;
		
		
		//make nounce
		String timestamp = DateUtil.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = username + password + authString + pureNounce;
		
		byte[] signature = null;
		
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
		}

		//encrypt
		byte[] name = svEncryption.encrypt(username.getBytes(UTF8));
		byte[] pass = svEncryption.encrypt(password.getBytes(UTF8));

		byte[] nounce = svEncryption.encrypt(pureNounce.getBytes(UTF8));

		//TODO: Later Check if the user has a token! This should happen to every method
		if(authString.equals("")) {
			//Normal Login
			response = gateway.Login(name, pass, nounce, signature);
			
			//decrypt response
			String pureResponse = new String(encryption.decrypt(response.get(2)), UTF8);

			
			try {
				if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse, cleanSchedule, nounces, encryption, svEncryption)) {
					if(pureResponse.equals("OK")) {
						System.out.println("Succesfully Authenticated");
						token = response.get(3);
						return;
					}
					else if(pureResponse.equals("NOK")) {
						System.out.println("Wrong Username or Password!");
						return;
					}
					else {
						System.out.println("Unrecognizable response! - " + pureResponse);
					}
				}
				
				System.out.println("Something went wrong with your request!");
				
				} catch (UnsupportedEncodingException e) {
					System.out.println("[WARNING] Unsupported encoding!");
				}catch (ParseException e) {
					System.out.println("[WARNING] Couldn't parse some data!");
				}catch (SignatureException e) {
					System.out.println("[WARNING] Couldn't verify some data!");
				}
			
			return;
		}
		else {
			//Replenish Login
			byte[] auth = svEncryption.encrypt(authString.getBytes(UTF8));
			
			response = gateway.ReplenishLogin(encryption.base64Encoder(encryption.pubKeyToByteArray()), name, pass, auth, nounce, signature);
			
			//decrypt response
			String pureResponse = new String(response.get(2), UTF8);

			try {	
				if(MaintenanceUtil.checkResponse(response.get(0), response.get(1), pureResponse, cleanSchedule, nounces, encryption, svEncryption)) {
					System.out.println("Succesfully Authenticated");
					token = response.get(3);
					return;
				}
				else if(pureResponse.equals("NOK")) {
					System.out.println("Wrong Username or Password!");
					return;
					
				}
				
				System.out.println("Something went wrong with your request!");
				
				} catch (UnsupportedEncodingException e) {
					System.out.println("[WARNING] Unsupported encoding!");
				}catch (ParseException e) {
					System.out.println("[WARNING] Couldn't parse some data!");
				}catch (SignatureException e) {
					System.out.println("[WARNING] Couldn't verify some data!");
				}
			
			return;
		}
	}
	
	//Helpers
	private String getUUID(){
		return UUID.randomUUID().toString();
	}
	
	private String randomString(){
		Random rand = new Random();
		
		int value = 0;
		char[] string = new char[4];
		
		for(int i = 0; i < 4; i++) {
			value = rand.nextInt(25);
			string[i] = (char) (65 + value);
		}
		
		return new String(string);
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
						if(parsedInput.length == 2) {
							g.GetDeviceCommands(parsedInput[1]);
						}
						else {
							System.out.println("Unrecognized login command");
						}
						break;
	
					case "request":	
						if(parsedInput.length > 3) {				
							String command = String.join(" ", Arrays.copyOfRange(parsedInput, 3, parsedInput.length));
							
							g.SendCommand(parsedInput[1], command);
						}
						else {
							System.out.println("Unrecognized login command");
						}
						break;
	
					case "login":
						if(parsedInput.length == 3) {
							g.Login(parsedInput[1], parsedInput[2], "");
						}
						else if(parsedInput.length == 4) {
							g.Login(parsedInput[1], parsedInput[2], parsedInput[3]);
						}
						else {
							System.out.println("Unrecognized login command");
						}
						break;
	
					case "register":
	
						if(parsedInput.length == 5) {
							g.RegisterUser(parsedInput[1], parsedInput[2], parsedInput[3], parsedInput[4]);
						}
						else {
							System.out.println("Unrecognized register command");
						}
						break;
	
					case "delete":
	
						if(parsedInput.length == 4) {
							g.DeleteUser(parsedInput[1], parsedInput[2], parsedInput[3]);
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
								+ ">> register AdminUsername AdminPassword username password");
						System.out.println("delete - [AMIN ONLY]delete a user from the network\n"
								+ ">> delete AdminUsername AdminPassword username");
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

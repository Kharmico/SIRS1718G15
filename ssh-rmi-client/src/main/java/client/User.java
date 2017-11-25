package client;

import java.io.UnsupportedEncodingException;
import java.rmi.*;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import server.Services.GatewayService;
import utils.EncryptionUtil;

public class User {
	//Static
	private static String UTF8 = "UTF-8";
	
	
	//Encryption
	private EncryptionUtil encryption;
	private ArrayList<String> nounces = new ArrayList<String>();
	private Date cleanSchedule = new Date();
	
	
	//Program
	private GatewayService gateway;
	
	//METHODS
	public User(GatewayService stub) throws RemoteException {
		gateway = stub;
		encryption = new EncryptionUtil();
		
	}

	public String RegisterUser(String adminName, String adminPassword, String name, String password) throws RemoteException, UnsupportedEncodingException {
		//make nounce
		String timestamp = this.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = adminName + adminPassword + name + password + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			return "[ERROR] Couldn't generate signature";
		}
		
		//encrypt
		byte[] aName = encryption.encrypt(adminName.getBytes(UTF8));
		byte[] aPassword = encryption.encrypt(adminPassword.getBytes(UTF8));

		byte[] nName = encryption.encrypt(name.getBytes(UTF8));
		byte[] nPassword = encryption.encrypt(password.getBytes(UTF8));
		
		byte[] nounce = encryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<byte[]> response = gateway.RegisterUser(aName, aPassword, nName, nPassword, nounce, signature);
		
		//decrypt response
		String pureResponse = new String(encryption.decrypt(response.get(3)), UTF8);

		
		try {	
			if(this.checkResponse(response.get(0), response.get(1), pureResponse)) {
				return pureResponse;
			}
			return "Something went wrong with your request!";
			
		} catch (UnsupportedEncodingException e) {
			return "[WARNING] Unsupported encoding!";
		}catch (ParseException e) {
			return "[WARNING] Couldn't parse some data!";
		}catch (SignatureException e) {
			return "[WARNING] Couldn't verify some data!";
		}
		
	}

	public String DeleteUser(String adminName, String adminPassword, String name) throws RemoteException, UnsupportedEncodingException {
		//make nounce
		String timestamp = this.getTimestamp();
		String uuid = this.getUUID();
		
		String pureNounce = uuid + "%" + timestamp;
		
		//make signature
		
		String pureSignature = adminName + adminPassword + name + pureNounce;
		
		byte[] signature = null;
		try {
			signature = encryption.generateSignature(pureSignature.getBytes(UTF8));
		} catch (SignatureException e) {
			return "[ERROR] Couldn't generate signature";
		}
		
		//encrypt
		byte[] aName = encryption.encrypt(adminName.getBytes(UTF8));
		byte[] aPassword = encryption.encrypt(adminPassword.getBytes(UTF8));

		byte[] nName = encryption.encrypt(name.getBytes(UTF8));
		
		byte[] nounce = encryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<byte[]> response = gateway.DeleteUser(aName, aPassword, nName, nounce, signature);
	
		//decrypt response
		String pureResponse = new String(encryption.decrypt(response.get(3)), UTF8);

		
		try {	
			if(this.checkResponse(response.get(0), response.get(1), pureResponse)) {
				return pureResponse;
			}
			return "Something went wrong with your request!";
			
		} catch (UnsupportedEncodingException e) {
			return "[WARNING] Unsupported encoding!";
		}catch (ParseException e) {
			return "[WARNING] Couldn't parse some data!";
		}catch (SignatureException e) {
			return "[WARNING] Couldn't verify some data!";
		}
	}

	public void GetDeviceStatus() throws RemoteException, UnsupportedEncodingException {
		//make nounce
		String timestamp = this.getTimestamp();
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
		byte[] nounce = encryption.encrypt(pureNounce.getBytes(UTF8));
		
		
		List<ArrayList<byte[]>> response = gateway.GetDeviceStatus(nounce, signature);
		
		List<ArrayList<byte[]>> subResponse = response.subList(1, response.size());
		
		//decrypt response
		List<ArrayList<String>> pureResponse = new ArrayList<ArrayList<String>>();

		for(ArrayList<byte[]> byteArray: subResponse) {
			String deviceName = new String(byteArray.get(0), UTF8);
			String deviceStatus = new String(byteArray.get(1), UTF8);
			String deviceType = new String(byteArray.get(2), UTF8);
			
			pureResponse.add(new ArrayList<String>(Arrays.asList(deviceName, deviceStatus, deviceType)));
		}
		
		try {	
			if(this.checkResponse(response.get(0).get(0), response.get(0).get(1), pureResponse.toString())) {
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

	public void GetDeviceCommands(String deviceName) throws RemoteException {
		List<String> response = gateway.GetDeviceCommands(null, deviceName);
		
		System.out.println("Device Commands");
		for(String command: response) {
			System.out.println("-" + command);
		}
		
	}

	public void SendCommand(String deviceName, String command) throws RemoteException {
		String response = gateway.SendCommand(null, deviceName,command);
		if(response.equals("OK")) {
			System.out.println("Command Succesfully Executed");
		}
		else if(response.equals("NOK")) {
			System.out.println("Command could not be Executed");
		}
		else {
			System.out.println("Something went wrong!");
		}
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
				
				if(responseKey != null) {
					serverPublicKey = responseKey;
					System.out.println("Successfully Authenticated!");
				}
				else {
					System.out.println("Something went wrong when authenticating!");
				}
				
			}
			else {
				response = "NO_AUTH_STRING";
			}
		}
		
		return response;
	}
	
	//HELPERS
	private String getTimestamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	private Date convertDate(String timestamp) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return dateFormat.parse(timestamp);
	}
	
	private String getUUID(){
		return UUID.randomUUID().toString();
	}
	
	private boolean checkResponse(byte[] nounce, byte[]signature, String response) throws UnsupportedEncodingException, ParseException, SignatureException {
		this.cleanNounces();
		//check nounce
		
		String pureNounce = new String(encryption.decrypt(nounce), UTF8);
		
		String[] parsedNounce = pureNounce.split("%");
		
		//checks if nounce doesnt exist and its still fresh
		if(nounces.contains(pureNounce)) {
			nounces.add(pureNounce);
		}
		else {
			return false;
		}
		
		if(!this.checkFreshness(this.convertDate(parsedNounce[1]))) {
			return false;
		}
		
		//check signature
		String signatureGuess = response.concat(pureNounce);
		
		
		return encryption.verifySignature(signatureGuess.getBytes(UTF8), signature);
	}

	private boolean checkFreshness(Date date) {
		Date now = new Date();
		
		if(now.before(date)) {
			return false;
		}
		
		long timeout = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
		long diff = now.getTime() - date.getTime();
		
		
		return   diff <= timeout;
	}
	
	private void cleanNounces() throws ParseException {
		if(this.checkStink(cleanSchedule)) {
			for(int i = 0; i < nounces.size(); i++) {
				String[] parsedNounce = nounces.get(i).split("%");
				Date timestamp = this.convertDate(parsedNounce[1]);
				if(this.checkStink(timestamp)) {
					nounces.remove(i);
				}
			}
		}
	}
	
	private boolean checkStink(Date date) {
		Date now = new Date();
		
		long timeout = TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS);
		long diff = now.getTime() - cleanSchedule.getTime();
		
		return   diff >= timeout;
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
						g.RegisterUser(parsedInput[1], parsedInput[2], parsedInput[3], parsedInput[4]);
					}
					else {
						System.out.println("Unrecognized register command");
					}
					break;

				case "delete":

					if(parsedInput.length == 2) {
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

package server.Controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import utils.DateUtil;
import utils.EncryptionUtil;
import utils.BufferUtil;
import server.Services.GatewayService;
import server.entities.Device;
import server.entities.User;

public class GatewayController extends UnicastRemoteObject implements GatewayService {

	private static final long serialVersionUID = 1L;
	private static final String UTF8 = "UTF-8";
	// GatewayController Variables
	public List<Device> devices = new ArrayList<Device>();
	public Map<String, Helper> devConnections = new HashMap<String,Helper>(); 	 //Stores the threads with the connections
	private ServerSocket registerSocket;
	private ArrayList<String> nonceList = new ArrayList<String>();
	private EncryptionUtil encUtil = new EncryptionUtil();
	private List<User> users = new ArrayList<User>();
	private Set<String> b64keys= new HashSet<String>();

	// GatewayController Constructor
	public GatewayController() throws RemoteException {
		encUtil.generateKeys("gateway");
		User user = new User("admin", "admin", "ADMIN");
		user.getEncUtils().setKeyPaths("keys/adminUserPublicKey.key", "");
		users.add(user);
		setDevicesTestingPurposes();
	}

	//GatewayController Endpoints
	

	public List<byte[]> RegisterUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] password, byte[] authCode, byte[] nonce, byte[] signature, byte[] token) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String usernameToCheck = new String(encUtil.decrypt(name), UTF8);
			String passwordToCheck = new String(encUtil.decrypt(password), UTF8);
			String adminUserToCheck = new String(encUtil.decrypt(adminUsername), UTF8);
			String adminPassToCheck = new String(encUtil.decrypt(adminPassword), UTF8);
			String pureAuthCode = new String(encUtil.decrypt(authCode), UTF8);
			
			User user = null;
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(adminUserToCheck) && userCheck.getPassword().equals(adminPassToCheck) && 
						userCheck.getType().equals("ADMIN")) {
					user = userCheck;
					break;
				}
			}
			
			if(user == null) {
				System.out.println("WRONG DELETE ATTEMPT: LOGIN CRAP VERIFICATION!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG REGISTER ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			System.out.println("BEFORE DECRYPTING TOKEN!!!");
			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			if(!tokenToCheck.equals(user.lastToken())) {
				System.out.println("WRONG REGISTER ATTEMPT: INVALID TOKEN!!!");
				return answerRequest("INVALID_TOKEN", user);
			}
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			String dataToCheck = adminUserToCheck + adminPassToCheck + usernameToCheck + passwordToCheck + pureAuthCode + pure_nonce;
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG REGISTER ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
			System.out.println("BEFORE USER ALREADY EXISTS CHECKING!!!\n");
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(usernameToCheck)) {
					System.out.println("WRONG REGISTER ATTEMPT: WRONG LOGIN OR USER EXISTS VERIFICATION!!!");
					return answerRequest("NOK", user);
				}
			}
			
			nonceList.add(pure_nonce);
			users.add(new User(usernameToCheck, passwordToCheck, "REGULAR", pureAuthCode));
			return answerRequest("OK", user);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<byte[]> DeleteUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] nonce, byte[] signature, byte[] token) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String usernameToCheck = new String(encUtil.decrypt(name), UTF8);
			String adminUserToCheck = new String(encUtil.decrypt(adminUsername), UTF8);
			String adminPassToCheck = new String(encUtil.decrypt(adminPassword), UTF8);
			String dataToCheck = adminUserToCheck + adminPassToCheck + usernameToCheck + pure_nonce;
			
			User user = null;
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(adminUserToCheck) && userCheck.getPassword().equals(adminPassToCheck) && 
						userCheck.getType().equals("ADMIN")) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG DELETE ATTEMPT: LOGIN CRAP VERIFICATION!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG DELETE ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			if(!tokenToCheck.equals(user.lastToken())) {
				System.out.println("WRONG DELETE ATTEMPT: INVALID TOKEN!!!");
				return answerRequest("INVALID_TOKEN", user);
			}
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG DELETE ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
			System.out.println("BEFORE USER ALREADY EXISTS CHECKING!!!\n");
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(usernameToCheck)) {
					nonceList.add(pure_nonce);
					users.remove(userCheck);
					return answerRequest("OK", user);
				}
			}
			
			System.out.println("WRONG DELETE ATTEMPT: USER DOES NOT EXIST!!!");
			return answerRequest("DELETE_ERROR", user);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<ArrayList<byte[]>> GetDeviceStatus(byte[] nonce, byte[] signature, byte[] token) {
		List<ArrayList<byte[]>> answerToRet = new ArrayList<ArrayList<byte[]>>();
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);
			
			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			User user = null;
			for(User userCheck : users) {
				if(userCheck.lastToken().equals(tokenToCheck)) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG DEVICESTATUS ATTEMPT: INVALID TOKEN!!!");
				answerToRet.add((ArrayList) answerRequest("INVALID_TOKEN", user));
				return answerToRet;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG DEVICESTATUS ATTEMPT: FRESHNESS ISSUES!!!");
				answerToRet.add((ArrayList) answerRequest("NOFRESH", user));
				return answerToRet;
			}
			
			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String dataToCheck = "getDeviceStatus" + pure_nonce;
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG DEVICESTATUS ATTEMPT: SIGNATURE VERIFICATION!!!");
				answerToRet.add((ArrayList) answerRequest("WRONGSIG", user));
				return answerToRet;
			}
			
			if(devices == null) {
				System.out.println("WRONG DEVICESTATUS ATTEMPT: NO_DEVICES!!!");
				answerToRet.add((ArrayList) answerRequest("NODEV", user));
				return answerToRet;
			}
			
			nonceList.add(pure_nonce);
			return answerRequestDevs(devices, user);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<byte[]> GetDeviceCommands(byte[] deviceName, byte[] nonce, byte[] signature, byte[] token) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			User user = null;
			for(User userCheck : users) {
				if(userCheck.lastToken().equals(tokenToCheck)) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG DEVICECMD ATTEMPT: INVALID TOKEN!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG DEVICECMD ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String dataToCheck = deviceToCheck + pure_nonce;
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG DEVICECMD ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
//			if(!devConnections.containsKey(deviceToCheck)) {
//				System.out.println("WRONG DEVICECMD ATTEMPT: DEVICE DOES NOT EXIST");
//				return answerRequest("DEVICE_ERROR");
//			}
			
			List<String> devCmds = new ArrayList<String>();
			for(Device dev : devices) {
				if(dev.getName().equals(deviceToCheck))
					devCmds = dev.getCommands();
			}
			
			if(devCmds.isEmpty()) {
				System.out.println("WRONG DEVICECMD ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);
			}
			
			nonceList.add(pure_nonce);
			return answerRequest(devCmds, user);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<byte[]> SendCommand(byte[] deviceName, byte[] command, byte[] nonce, byte[] signature, byte[] token) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			User user = null;
			for(User userCheck : users) {
				if(userCheck.lastToken().equals(tokenToCheck)) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG SENDCMD ATTEMPT: INVALID TOKEN!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG SENDCMD ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String commandToCheck = new String(encUtil.decrypt(command), UTF8);
			String dataToCheck = deviceToCheck + commandToCheck + pure_nonce;
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG SENDCMD ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
			List<String> devCmds = new ArrayList<String>();
			for(Device dev : devices) {
				if(dev.getName().equals(deviceToCheck))
					devCmds = dev.getCommands();
			}
			
			if(devCmds.isEmpty()) {
				System.out.println("WRONG DEVICECMD ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);
			}
			
			if(!devConnections.containsKey(deviceToCheck)) {
				System.out.println("WRONG SENDCMD: DEVICE DOES NOT EXIST");
				return answerRequest("DEVICE_ERROR", user);
			}
			
			//TODO: Get this done right!!!
			//Idea is to send command to the device!!!
			nonceList.add(pure_nonce);
//			Helper deviceCon = devConnections.get(deviceToCheck);
//			deviceCon.getDeviceState();
			return answerRequest("OK", user);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<byte[]> ReplenishLogin(byte[] userPublicKey, byte[] username, byte[] password, byte[] authString, byte[] nonce, byte[] signature) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String usernameToCheck = new String(encUtil.decrypt(username), UTF8);
			String passwordToCheck = new String(encUtil.decrypt(password), UTF8);
			String authStringToCheck = new String(encUtil.decrypt(authString), UTF8);
			String dataToCheck = usernameToCheck + passwordToCheck + authStringToCheck + pure_nonce;
			
			User user = null;
			for(User userCheck : users) {
				if(userCheck.getAuthCode().equals(authStringToCheck) && userCheck.getUsername().equals(usernameToCheck) && 
						userCheck.getPassword().equals(passwordToCheck)) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG REFRESH ATTEMPT: WRONG AUTHCODE VERIFICATION!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG REFRESH ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG REFRESH ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
			System.out.println("BEFORE LOGIN CREDENTIALS CHECKING!!!\n" + usernameToCheck + " " + passwordToCheck + " " + user.getUsername() + " " + user.getPassword());
			if(!(usernameToCheck.equals(user.getUsername()) && passwordToCheck.equals(user.getPassword()))) {
				System.out.println("WRONG REFRESH ATTEMPT: LOGIN CRAP VERIFICATION!!!");
				return answerRequest("NOK", user);
			}
			
			nonceList.add(pure_nonce);
			Key pubKey = encUtil.byteArrayToPubKey(encUtil.base64Decoder(userPublicKey));
			user.getEncUtils().setPublicKey(pubKey, usernameToCheck);
			String token = user.generateToken();
			return answerRequest("OK", token, user);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		
		return null;
	}

	public List<byte[]> Login(byte[] username, byte[] password, byte[] nounce, byte[] signature) {
		byte[] dec_nonce = encUtil.decrypt(nounce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String usernameToCheck = new String(encUtil.decrypt(username), UTF8);
			String passwordToCheck = new String(encUtil.decrypt(password), UTF8);
			String dataToCheck = usernameToCheck + passwordToCheck + pure_nonce;
			
			User user = null;
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(usernameToCheck) && userCheck.getPassword().equals(passwordToCheck)) {
					user = userCheck;
					break;
				}	
			}
			
			if(user == null) {
				System.out.println("WRONG LOGIN ATTEMPT: LOGIN CRAP VERIFICATION!!!");
				return null;
			}
			
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG LOGIN ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG LOGIN ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}
			
			nonceList.add(pure_nonce);
			String token = user.generateToken();
			return answerRequest("OK", token, user);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		}
		return null;
	}

	public byte[] GetPublicKey() throws RemoteException {
		byte[] pubKeyGetter = encUtil.pubKeyToByteArray();
		return encUtil.base64Encoder(pubKeyGetter);
	}
	
	public String registerNewDevice(){
		return null;

	}
	
	public void addKey(String key){
		b64keys.add(key);
	}
	public boolean removeKey(String key){
		boolean removed = false;
		for(String k: b64keys) {
			if (k.equals(key)){
				b64keys.remove(key);
				removed = true;
			}
		}
		return removed;
	}
	public void startListeningDevices() throws IOException {
		Thread t1 = new Thread(new Runnable() {
			public void run() {

				while (true) { /* or some other condition you wish */
					Socket connection;
					try {
						System.out.println("Ready to accept another device...");
						connection = registerSocket.accept(); /* will wait here */
						System.out.println("\nNow connected to " 
								+ connection.getInetAddress().toString() +":" + connection.getPort());

						InputStream in = connection.getInputStream();

						byte[] data = new byte[100];
						int count = BufferUtil.readInputStreamWithTimeout( in, data, 1000 ); 
						String deviceName ="";

						if ( count >0 ) {
							deviceName = new String(data, "UTF-8").trim().split(":")[0];
							System.out.println(deviceName);
						}
						else {
							System.out.println("Connection not successful");
							connection.close();
							continue;
						};
						String devPort = new String(data, "UTF-8").trim().split(":")[1] ;
						Helper con = new Helper(connection, Integer.parseInt(devPort)); /* call a nanny to take
	     	                                                            care of it */
						devConnections.put(deviceName, con); /* make++ sure you keep a ref to it, just in case */
						con.start();	/* this code is executed when a client connects... */
						/* tell nanny to get to work as an independent thread */


					} catch (IOException e) {
						e.printStackTrace();
					} 
				}


			}
		});  
		t1.start();
		
	}
	
	public int createListeningSocket(int port) throws IOException{

		registerSocket = new ServerSocket(port); 		//If port == 0 find an available port to listen for new devices registration
		return registerSocket.getLocalPort();			//Else listen on the requested port
	}
	
	public void closeListeningDevicesSocket() throws IOException{
		registerSocket.close();
	}
	
	//------------------------------------ PRIVATE AUXILIARY FUNCTIONS ------------------------------------------------//
	
	private List<byte[]> answerRequest(List<String> reqResponse, User user) throws UnsupportedEncodingException, SignatureException {
		String timestamp = DateUtil.getTimestamp();
		String uuid = UUID.randomUUID().toString();
		String pureNounce = uuid + "%" + timestamp;
		String pureSignature = reqResponse.toString().concat(pureNounce);
		
		byte[] sigToSend = null;
		sigToSend = encUtil.generateSignature(pureSignature.getBytes(UTF8));

		byte[] nounceToSend = user.getEncUtils().encrypt(pureNounce.getBytes(UTF8));
		
		List<byte[]> answerRequest = new ArrayList<byte[]>();
		answerRequest.add(nounceToSend);
		answerRequest.add(sigToSend);
		for(String command : reqResponse)
			answerRequest.add(user.getEncUtils().encrypt(command.getBytes(UTF8)));
		
		return answerRequest;
	}

	private List<ArrayList<byte[]>> answerRequestDevs(List<Device> reqResponse, User user) throws UnsupportedEncodingException, SignatureException {
		String timestamp = DateUtil.getTimestamp();
		String uuid = UUID.randomUUID().toString();
		String pureNounce = uuid + "%" + timestamp;
		String pureSignature = "";
		byte[] sigToSend = null;
		byte[] nounceToSend = null;
		List<ArrayList<String>> pureRespDevs = new ArrayList<ArrayList<String>>();
		
		for(Device device : reqResponse) {
			String deviceName = device.getName();
			String deviceStatus = device.getStatus();
			String deviceType = device.getType();
			pureRespDevs.add(new ArrayList<String>(Arrays.asList(deviceName, deviceStatus, deviceType)));
		}
		
		pureSignature = pureRespDevs.toString().concat(pureNounce);
		sigToSend = encUtil.generateSignature(pureSignature.getBytes(UTF8));
		nounceToSend = user.getEncUtils().encrypt(pureNounce.getBytes(UTF8));

		System.out.println(pureRespDevs.toString());
		
		
		ArrayList<byte[]> answerRequest = new ArrayList<byte[]>();
		answerRequest.add(nounceToSend);
		answerRequest.add(sigToSend);
		List<ArrayList<byte[]>> answerReturn = new ArrayList<ArrayList<byte[]>>();
		answerReturn.add(answerRequest);
		for(Device device : reqResponse) {
			ArrayList<byte[]> aux = new ArrayList<byte[]>();
			aux.add(user.getEncUtils().encrypt(device.getName().getBytes(UTF8)));
			aux.add(user.getEncUtils().encrypt(device.getStatus().getBytes(UTF8)));
			aux.add(user.getEncUtils().encrypt(device.getType().getBytes(UTF8)));
			answerReturn.add(aux);
		}
		
		return answerReturn;
	}
	
	private List<byte[]> answerRequest(String reqResponse, User user) throws UnsupportedEncodingException, SignatureException {
		String response = reqResponse;
		String timestamp = DateUtil.getTimestamp();
		String uuid = UUID.randomUUID().toString();
		String pureNounce = uuid + "%" + timestamp;
		String pureSignature = response.concat(pureNounce);
		
		byte[] sigToSend = null;
		sigToSend = encUtil.generateSignature(pureSignature.getBytes(UTF8));

		byte[] nounceToSend = user.getEncUtils().encrypt(pureNounce.getBytes(UTF8));
		byte[] responseToSend = user.getEncUtils().encrypt(response.getBytes(UTF8));
		
		List<byte[]> answerRequest = new ArrayList<byte[]>();
		answerRequest.add(nounceToSend);
		answerRequest.add(sigToSend);
		answerRequest.add(responseToSend);
		
		return answerRequest;
	}
	
	private List<byte[]> answerRequest(String reqResponse, String token, User user) throws UnsupportedEncodingException, SignatureException {
		String response = reqResponse;
		String timestamp = DateUtil.getTimestamp();
		String uuid = UUID.randomUUID().toString();
		String pureNounce = uuid + "%" + timestamp;
		String pureSignature = response.concat(pureNounce);
		
		byte[] sigToSend = null;
		sigToSend = encUtil.generateSignature(pureSignature.getBytes(UTF8));

		byte[] nounceToSend = user.getEncUtils().encrypt(pureNounce.getBytes(UTF8));
		byte[] responseToSend = user.getEncUtils().encrypt(response.getBytes(UTF8));
		byte[] tokenToSend = encUtil.encrypt(token.getBytes(UTF8));
		
		List<byte[]> answerRequest = new ArrayList<byte[]>();
		answerRequest.add(nounceToSend);
		answerRequest.add(sigToSend);
		answerRequest.add(responseToSend);
		answerRequest.add(tokenToSend);
		
		return answerRequest;
	}
	
	
	private void setDevicesTestingPurposes(){
		Device devs = new Device("lampada", "ligado", "lampada");
		Device devs2 = new Device("frigo", "ligado", "frigorifico");

		ArrayList<String> devsCmds = new ArrayList<String>();
		ArrayList<String> devs2Cmds = new ArrayList<String>();
		
		devsCmds.add("TURN_ON");
		devsCmds.add("TURN_OFF");
		devs2Cmds.add("CHECK_TEMP");
		devs2Cmds.add("CHECK_SCHEDULE");
		devs2Cmds.add("CHECK_POWER");
		
		devs.setCommands(devsCmds);
		devs2.setCommands(devs2Cmds);
		devices.add(devs);
		devices.add(devs2);
	}
}

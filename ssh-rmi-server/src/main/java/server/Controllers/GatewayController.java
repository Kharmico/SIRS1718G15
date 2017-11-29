package server.Controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.joda.time.DateTime;

import utils.DateUtil;
import utils.EncryptionUtil;
import utils.MaintenanceUtil;
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
	private User user;

	// GatewayController Constructor
	public GatewayController() throws RemoteException {
		encUtil.generateKeys("gateway");
		user = new User("admin", "admin", "ADMIN");
		user.getEncUtils().setKeyPaths("keys/adminUserPublicKey.key", "");
	}

	//GatewayController Endpoints
	
	
	// Hash -> Username+Type(type of user, i.e. admin, regular user, etc.)+LastLoginDate+LastLoginUUID H{U+T+D+I}
	public List<byte[]> RegisterUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] password, byte[] nonce, byte[] signature, byte[] token) {
		
		return null;
	}

	public List<byte[]> DeleteUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] nonce, byte[] signature, byte[] token) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ArrayList<byte[]>> GetDeviceStatus(byte[] nonce, byte[] signature, byte[] token) {
		return null;
	}

	public List<byte[]> GetDeviceCommands(byte[] deviceName, byte[] nonce, byte[] signature, byte[] token) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> SendCommand(byte[] deviceName, byte[] command, byte[] nonce, byte[] signature, byte[] token) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> ReplenishLogin(byte[] userPublicKey, byte[] username, byte[] password, byte[] authString, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> Login(byte[] username, byte[] password, byte[] nounce, byte[] signature) {
		String str_nonce;
		byte[] dec_nonce = encUtil.decrypt(nounce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);
			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2)) {
				System.out.println("WRONG LOGIN ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH");
			}
			//TODO: Verify signature, if it passes then verify login info!!! If all good, then...
			//LOGGED IN!!! Must create a token!!!
			System.out.println("BEFORE DECRYPTING SHIT!!!");
			String usernameToCheck = new String(encUtil.decrypt(username), UTF8);
			String passwordToCheck = new String(encUtil.decrypt(password), UTF8);
			String dataToCheck = usernameToCheck + passwordToCheck + pure_nonce;
			
			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG LOGIN ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG");
			}
			
			System.out.println("BEFORE LOGIN CREDENTIALS CHECKING!!!\n" + usernameToCheck + " " + passwordToCheck + " " + user.getUsername() + " " + user.getPassword());
			if(!(usernameToCheck.equals(user.getUsername()) && passwordToCheck.equals(user.getPassword()))) {
				System.out.println("WRONG LOGIN ATTEMPT: LOGIN CRAP VERIFICATION!!!");
				return answerRequest("NOK");
			}

			System.out.println("I GOT HERE!!! WOOHOO!!!");
			
			String token = user.generateToken();
			return answerRequest("OK", token);
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
						int count = in.read(data);
						String deviceName ="";

						if ( count >0 ) {
							deviceName = new String(data, "UTF-8");
							System.out.println(deviceName);
						}
						else {
							System.out.println("Connection not successful");
							connection.close();
							continue;
						};

						Helper con = new Helper(connection); /* call a nanny to take
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
	
	private List<byte[]> answerRequest(String reqResponse) throws UnsupportedEncodingException, SignatureException {
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
	
	private List<byte[]> answerRequest(String reqResponse, String token) throws UnsupportedEncodingException, SignatureException {
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
	
}

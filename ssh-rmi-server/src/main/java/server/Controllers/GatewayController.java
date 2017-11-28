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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import server.Services.GatewayService;
import utils.EncryptionUtil;
import utils.DateUtil;
import utils.MaintenanceUtil;
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
		user = new User("admin", "ADMIN", "admin");
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
			str_nonce = new String(dec_nonce, "UTF-8");
			String[] strings_nonce = str_nonce.split("%");
			Date nonceDate = DateUtil.convertDate(strings_nonce[1]);
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2)) {
				// create nonce+timestamp, concatenate with % in the middle
				// choose which type of response to send!!! (check on User from user-side for types)
				// sign everything!!!
				String response = "NOFRESH";
				String timestamp = DateUtil.getTimestamp();
				String uuid = UUID.randomUUID().toString();
				String pureNounce = uuid + "%" + timestamp;
				String pureSignature = response.concat(pureNounce);
				
				byte[] sigToSend = null;
				sigToSend = encUtil.generateSignature(pureSignature.getBytes(UTF8));

				byte[] nounceToSend = encUtil.encrypt(pureNounce.getBytes(UTF8));
				byte[] responseToSend = encUtil.encrypt(response.getBytes(UTF8));
				
				List<byte[]> answerRequest = new ArrayList<byte[]>();
				answerRequest.add(nounceToSend);
				answerRequest.add(sigToSend);
				answerRequest.add(responseToSend);
				
				System.out.println("WRONG LOGIN ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest;
			}
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
	
}

package server.Controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import server.Services.GatewayService;
import utils.EncryptionUtil;
import server.entities.Device;

public class GatewayController extends UnicastRemoteObject implements GatewayService {

	private static final long serialVersionUID = 1L;
	// GatewayController Variables
	public List<Device> devices = new ArrayList<Device>();
	private ServerSocket registerSocket;
	private Map<String, Helper> devConnections = new HashMap<String,Helper>(); 	 //Stores the threads with the connections
	private ArrayList<String> nonceList = new ArrayList<String>();
	private EncryptionUtil encUtil = new EncryptionUtil();

	// GatewayController Constructor
	public GatewayController() throws RemoteException {}

	//GatewayController Endpoints
	
	public List<byte[]> RegisterUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] password, byte[] nonce, byte[] signature) {
		String str_nonce;
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			str_nonce = new String(dec_nonce, "UTF-8");
			String[] strings_nonce = str_nonce.split("%");
			if(nonceList.contains(str_nonce) || checkTimestamp(strings_nonce[1])){
				System.out.println("NONCE ALREADY SEEN!!!");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> DeleteUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ArrayList<byte[]>> GetDeviceStatus(byte[] nonce, byte[] signature) {
		return null;
	}

	public List<byte[]> GetDeviceCommands(byte[] deviceName, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> SendCommand(byte[] deviceName, byte[] command, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> ReplenishLogin(byte[] userPublicKey, byte[] username, byte[] password, byte[] authString, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<byte[]> Login(byte[] username, byte[] password, byte[] nonce, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public String registerNewDevice(){
		return null;

	}
	public void startListeningDevices() throws IOException{
		while (true) { /* or some other condition you wish */
			Socket connection = registerSocket.accept(); /* will wait here */
			/* this code is executed when a client connects... */

			System.out.println("\nNow connected to " 
					+ connection.getInetAddress().toString() +":" + connection.getPort());
			
			InputStream stream = connection.getInputStream();
			
			byte[] data = new byte[100];
			int count = stream.read(data);
			String deviceName ="";
			
			if ( count >0 ) {
				deviceName = new String(data, "UTF-8");
				System.out.println();
			}
			else continue;
			
			Helper con = new Helper(connection); /* call a nanny to take
	                                                            care of it */
			devConnections.put(deviceName, con); /* make++ sure you keep a ref to it, just in case */
			con.start();						 /* tell nanny to get to work as an independent thread */

		}
	}
	
	public int createListeningSocket(int port) throws IOException{

		registerSocket = new ServerSocket(port); 		//If port == 0 find an available port to listen for new devices registration
		return registerSocket.getLocalPort();			//Else listen on the requested port
	}
	
	public void closeListeningDevicesSocket() throws IOException{
		registerSocket.close();
	}
	
	// (yyyy/mm/dd hh:mm:ss)
	private boolean checkTimestamp(String timest){
		LocalDateTime currentTime;
		String[] divide = timest.split("[ /:]");
		int[] timestCalendar = new int[3];
		int[] timestHours = new int[3];
		
		for(int i = 0; i < 3; i++){
			timestCalendar[i] = Integer.parseInt(divide[i]);
			timestHours[i] = Integer.parseInt(divide[i+3]);
		}
		
		currentTime = LocalDateTime.now();
		//LocalDateTime has functions to get the year, month, day, hour, minute, second from it, just need to do a manual diff, no need
		//to waste time, resources, computation on extra shit...
		return true;
	}

}

package server.Controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import server.Services.GatewayService;
import server.entities.Device;

public class GatewayController extends UnicastRemoteObject implements GatewayService {

	private static final long serialVersionUID = 1L;
	// GatewayController Variables
	public List<Device> devices = new ArrayList<Device>();
	private ServerSocket registerSocket;
	private Map<String, Helper> devConnections = new HashMap<String,Helper>(); //Stores the threads with the connections


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
		//name status type
		return null;
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

}

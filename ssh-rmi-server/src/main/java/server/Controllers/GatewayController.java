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
import java.util.concurrent.SynchronousQueue;

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
	public Set<String> b64keys= new HashSet<String>();

	// GatewayController Constructor
	public GatewayController() throws RemoteException {
		encUtil.generateKeys("gateway");
		User user = new User("admin", "admin", "ADMIN");
		user.getEncUtils().setKeyPaths("keys/adminUserPublicKey.key", "");
		users.add(user);
		//setDevicesTestingPurposes();
	}

	//GatewayController Endpoints
	public List<byte[]> RegisterUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] password, byte[] authCode, byte[] nonce, byte[] signature, byte[] token) {
		byte[] dec_nonce = encUtil.decrypt(nonce);
		try {
			// nonce%timestamp
			String pure_nonce = new String(dec_nonce, UTF8);
			String[] strings_nonce = pure_nonce.split("%");
			DateTime nonceDate = (DateTime) DateUtil.convertDate(strings_nonce[1]);

			System.out.println("REGISTER: BEFORE DECRYPTING!!!");
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
				System.out.println("WRONG REGISTER ATTEMPT: WRONG LOGIN!!!");
				return null;
			}

			System.out.println("REGISTER: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG REGISTER ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("REGISTER: BEFORE DECRYPTING TOKEN!!!");
			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			if(!tokenToCheck.equals(user.lastToken())) {
				System.out.println("WRONG REGISTER ATTEMPT: INVALID TOKEN!!!");
				return answerRequest("INVALID_TOKEN", user);
			}

			System.out.println("REGISTER: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			String dataToCheck = adminUserToCheck + adminPassToCheck + usernameToCheck + passwordToCheck + pureAuthCode + pure_nonce;
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG REGISTER ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			System.out.println("REGISTER: BEFORE USER ALREADY EXISTS CHECKING!!!\n");
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

			System.out.println("DELETE: BEFORE DECRYPTING!!!");
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
				System.out.println("WRONG DELETE ATTEMPT: WRONG LOGIN!!!");
				return null;
			}

			System.out.println("DELETE: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG DELETE ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			String tokenToCheck = new String(encUtil.decrypt(token), UTF8);
			if(!tokenToCheck.equals(user.lastToken())) {
				System.out.println("WRONG DELETE ATTEMPT: INVALID TOKEN!!!");
				return answerRequest("INVALID_TOKEN", user);
			}

			System.out.println("DELETE: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG DELETE ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			System.out.println("DELETE: BEFORE USER ALREADY EXISTS CHECKING!!!\n");
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

			System.out.println("DEVICESTATUS: BEFORE DECRYPTING!!!");
			String dataToCheck = "getDeviceStatus" + pure_nonce;

			System.out.println("DEVIVESTATUS: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
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

			System.out.println("DEVICECMD: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG DEVICECMD ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("DEVICECMD: BEFORE DECRYPTING!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String dataToCheck = deviceToCheck + pure_nonce;

			System.out.println("DEVICECMD: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
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

			System.out.println("SENDCMD: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG SENDCMD ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("SENDCMD: BEFORE DECRYPTING!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String commandToCheck = new String(encUtil.decrypt(command), UTF8);
			String dataToCheck = deviceToCheck + commandToCheck + pure_nonce;

			System.out.println("SENDCMD: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG SENDCMD ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			Device d =null;

			List<String> devCmds = new ArrayList<String>();
			for(Device dev : devices) {
				if(dev.getName().equals(deviceToCheck)){
					devCmds = dev.getCommands();
					d = dev;
				}
			}

			boolean cmd_exists = false;
			for(String s : devCmds) {
				String parts[] = commandToCheck.split(" ",2);
				if(parts.length == 1){
					if(s.equals(commandToCheck)){
						cmd_exists=true;
						break;
					}
				}
				else{
					if(s.contains(parts[0])){
						cmd_exists=true;
						break;
					}
				}
			}

			if(devCmds.isEmpty()) {
				System.out.println("SENDCMD: WRONG DEVICECMD ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);
			}

			if(!devConnections.containsKey(deviceToCheck) || d == null) {
				System.out.println("WRONG SENDCMD: DEVICE DOES NOT EXIST");
				return answerRequest("DEVICE_ERROR", user);
			}
			if(!cmd_exists) {
				System.out.println("WRONG SENDCMD: COMMAND DOES NOT EXIST");
				return answerRequest("DEVICE_ERROR", user);
			}
			//TODO: Get this done right!!!
			//Idea is to send command to the device!!!
			nonceList.add(pure_nonce);
			//			Helper deviceCon = devConnections.get(deviceToCheck);
			//			deviceCon.getDeviceState();
			d.sendCommand(commandToCheck);
			return answerRequest("OK", user);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			System.out.println("[ERROR] Couldn't generate signature");
			e.printStackTrace();
		} catch (Exception e){
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
				System.out.println("WRONG FIRSTLOGIN ATTEMPT: WRONG AUTHCODE VERIFICATION!!!");
				return null;
			}

			EncryptionUtil eu = new EncryptionUtil();
			eu.setPublicKey(encUtil.byteArrayToPubKey(encUtil.base64Decoder(userPublicKey)), user.getUsername());

			user.setEncUtils(eu);

			System.out.println("FIRSTLOGIN: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG FIRSTLOGIN ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("FIRSTLOGIN: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG FIRSTLOGIN ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			nonceList.add(pure_nonce);
			Key pubKey = encUtil.byteArrayToPubKey(encUtil.base64Decoder(userPublicKey));
			user.getEncUtils().setPublicKey(pubKey, usernameToCheck);
			user.setStatus("VERIFIED");
			String token = user.generateToken();
			user.rstAuthCode();
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

			System.out.println("LOGIN: BEFORE DECRYPTING!!!");
			String usernameToCheck = new String(encUtil.decrypt(username), UTF8);
			String passwordToCheck = new String(encUtil.decrypt(password), UTF8);
			String dataToCheck = usernameToCheck + passwordToCheck + pure_nonce;

			User user = null;
			for(User userCheck : users) {
				if(userCheck.getUsername().equals(usernameToCheck) && userCheck.getPassword().equals(passwordToCheck) && 
						userCheck.getAuthCode().equals("")) {
					user = userCheck;
					break;
				}	
			}

			if(user == null || user.getStatus().equals("PENDING")) {
				System.out.println("WRONG LOGIN ATTEMPT: WRONG LOGIN!!!");
				return null;
			}

			System.out.println("LOGIN: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG LOGIN ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("LOGIN: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
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

	public List<byte[]> AcceptDevice(byte[] deviceName, byte[] code, byte[] nonce, byte[] signature, byte[] token) throws RemoteException {
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
				System.out.println("WRONG ACCEPTDEV ATTEMPT: INVALID TOKEN!!!");
				return null;
			}

			if(!user.getType().equals("ADMIN")) {
				System.out.println("WRONG ACCEPTDEV ATTEMPT: NO PERMISSION!!!");
				return answerRequest("NOPERMISSION", user);
			}

			System.out.println("BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG ACCEPTDEV ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("ACCEPTDEV: BEFORE DECRYPTING!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String codeToCheck = new String(encUtil.decrypt(code), UTF8);
			String dataToCheck = deviceToCheck + codeToCheck + pure_nonce;

			System.out.println("ACCEPTDEV: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG ACCEPTDEV ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			Boolean found = false;
			/*Bfor(Device dev : devices) {
				if(dev.getName().equals(deviceToCheck)) {
					dev.setAccepted(true);
					found = true;
				}
			}

			if(!found) {
				System.out.println("WRONG ACCEPTDEV ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);
			}*/
			try{
				found = b64keys.add(codeToCheck);
				if(!found){
					System.out.println("WRONG ACCEPTDEV ATTEMPT: ALREADY EXISTS KEY!!!");
					return answerRequest("NODEV", user);
				}
			}catch(Exception e){
				System.out.println("WRONG ACCEPTDEV ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);

			}

			nonceList.add(pure_nonce);
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

	public List<byte[]> RemoveDevice(byte[] deviceName, byte[] nonce, byte[] signature, byte[] token) throws RemoteException {
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
				System.out.println("WRONG RMDEV ATTEMPT: INVALID TOKEN!!!");
				return null;
			}

			if(!user.getType().equals("ADMIN")) {
				System.out.println("WRONG RMDEV ATTEMPT: NO PERMISSION!!!");
				return answerRequest("NOPERMISSION", user);
			}

			System.out.println("RMDEV: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG RMDEV ATTEMPT: FRESHNESS ISSUES!!!");
				return answerRequest("NOFRESH", user);
			}

			System.out.println("RMDEV: BEFORE DECRYPTING SHIT!!!");
			String deviceToCheck = new String(encUtil.decrypt(deviceName), UTF8);
			String dataToCheck = deviceToCheck + pure_nonce;

			System.out.println("RMDEV: BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG RMDEV ATTEMPT: SIGNATURE VERIFICATION!!!");
				return answerRequest("WRONGSIG", user);
			}

			Boolean found = false;
			for(Device dev : devices) {
				if(dev.getName().equals(deviceToCheck)) {
					dev.setAccepted(true);
					found = true;
				}
			}

			if(!found) {
				System.out.println("WRONG RMDEV ATTEMPT: DEVICE VERIFICATION!!!");
				return answerRequest("NODEV", user);
			}

			//TODO FAZ AQUI O CODIGO DE REMOVER O DEVICE
			for ( Map.Entry<String, Helper> e : devConnections.entrySet()){
				String name = e.getKey();
				if ( name.equals(deviceName)){
					try{
						e.getValue().closeAllConnections();
						System.out.print(e.getKey() +":");
						devConnections.remove(e.getKey());
						System.out.print("REMOVED.\n");
					}
					catch(Exception exc){

					}

				}
			}

			//TODO FIM

			nonceList.add(pure_nonce);
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

	public List<ArrayList<byte[]>> GetUsers(byte[] nonce, byte[] signature, byte[] token) throws RemoteException {
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
				System.out.println("WRONG GETUSERS ATTEMPT: INVALID TOKEN!!!");
				answerToRet.add((ArrayList) answerRequest("INVALID_TOKEN", user));
				return answerToRet;
			}

			if(!user.getType().equals("ADMIN")) {
				System.out.println("WRONG GETUSERS ATTEMPT: NO PERMISSION!!!");
				answerToRet.add((ArrayList) answerRequest("NOPERMISSION", user));
				return answerToRet;
			}

			System.out.println("GETUSERS: BEFORE FRESHNESS CHECKING!!!");
			if(!DateUtil.checkFreshnessMinutes(nonceDate, 2) || nonceList.contains(pure_nonce)) {
				System.out.println("WRONG GETUSERS ATTEMPT: FRESHNESS ISSUES!!!");
				answerToRet.add((ArrayList) answerRequest("NOFRESH", user));
				return answerToRet;
			}

			System.out.println("GETUSERS: BEFORE DECRYPTING SHIT!!!");
			String dataToCheck = "getListUsers" + pure_nonce;

			System.out.println("BEFORE SIGNATURE VERIFICATION CHECKING!!!");
			if(!user.getEncUtils().verifySignature(dataToCheck.getBytes(UTF8), signature)) {
				System.out.println("WRONG GETUSERS ATTEMPT: SIGNATURE VERIFICATION!!!");
				answerToRet.add((ArrayList) answerRequest("WRONGSIG", user));
				return answerToRet;
			}

			if(users.size() == 0) {
				System.out.println("WRONG GETUSERS ATTEMPT: NO_USERS!!!");
				answerToRet.add((ArrayList) answerRequest("NOUSR", user));
				return answerToRet;
			}

			nonceList.add(pure_nonce);
			return answerRequestUsers(users, user);
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
					Socket connection = null;
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
						Helper con = new Helper(deviceName,connection, Integer.parseInt(devPort),b64keys); 
						devices.add(con.login());
						devConnections.put(deviceName, con);
						con.start();	/* this code is executed when a device connects... */



					} catch (IOException | NullPointerException e ) {
						try {
							if(connection != null) 
								connection.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
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

	private List<ArrayList<byte[]>> answerRequestUsers(List<User> reqResponse, User user) throws UnsupportedEncodingException, SignatureException {
		String timestamp = DateUtil.getTimestamp();
		String uuid = UUID.randomUUID().toString();
		String pureNounce = uuid + "%" + timestamp;
		String pureSignature = "";
		byte[] sigToSend = null;
		byte[] nounceToSend = null;
		List<ArrayList<String>> pureRespDevs = new ArrayList<ArrayList<String>>();

		for(User usr : reqResponse) {
			String userName = usr.getUsername();
			String userType = usr.getType();
			String userStatus = usr.getStatus();
			pureRespDevs.add(new ArrayList<String>(Arrays.asList(userName, userType, userStatus)));
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
		for(User usr : reqResponse) {
			ArrayList<byte[]> aux = new ArrayList<byte[]>();
			aux.add(user.getEncUtils().encrypt(usr.getUsername().getBytes(UTF8)));
			aux.add(user.getEncUtils().encrypt(usr.getType().getBytes(UTF8)));
			aux.add(user.getEncUtils().encrypt(usr.getStatus().getBytes(UTF8)));
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

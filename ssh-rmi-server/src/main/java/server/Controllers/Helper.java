package server.Controllers;

import java.io.IOException;

import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.plaf.synth.SynthSeparatorUI;

import server.entities.Device;
import utils.BufferUtil;
import utils.EncryptionUtil;

public class Helper extends Thread{
	private Socket socketOUT = null;
	private Socket socketIN = null;

	private DataInputStream OUTin= null;
	private PrintStream OUTout= null;
	private DataInputStream INin= null;
	private PrintStream INout= null;

	private volatile String deviceState = "";
	private String deviceName="";
	private String deviceKey ="";
	private String sessionKey="";
	private String tempSessionKey="";
	private byte[] Hmac_key;
	private volatile byte[] challenge;
	
	private volatile int keyRenewedTimes = 0;
	public volatile boolean isValidDevice = true;
	private final int SESKEY_EXPIRE_TIME= 5000;

	private LinkedBlockingQueue <String> msgToSend = new LinkedBlockingQueue <String>();
	private EncryptionUtil enc = new EncryptionUtil();

	private Set<String> keys;

	public String getDeviceState() {

		try {
			String message = encryptMessage("GETSTATUS" + "," + enc.base64SEncoder(challenge), sessionKey);
			OUTout.write(message.getBytes());
			byte[] bytes = new byte[1024];

			OUTin.read(bytes);
			String devState[] = new String(bytes, "UTF-8").trim().split(":");
			
			String temp[] = processMessage(devState).split(","); // [b64 ACK/NACK, b64Challenge]
			this.challenge = enc.base64SDecoder(temp[1]);
			message = new String(enc.base64SDecoder(temp[0]));
			return message;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Not able to get state of device. Please try again.";

	}
	
	public String switchDeviceState() {

		try {
			String message = encryptMessage("SWITCH" + "," + enc.base64SEncoder(challenge), sessionKey);
			OUTout.write(message.getBytes());
			byte[] bytes = new byte[1024];

			OUTin.read(bytes);
			String devState[] = new String(bytes, "UTF-8").trim().split(":");
			
			String temp[] = processMessage(devState).split(","); // [b64 ACK/NACK, b64Challenge]
			this.challenge = enc.base64SDecoder(temp[1]);
			message = new String(enc.base64SDecoder(temp[0]));
			return message;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Not able to switch state of device. Please try again.";

	}
	
	public String setCustomState(String stateName) {

		try {
			String message = encryptMessage("SETSTATE " +stateName+ "," + enc.base64SEncoder(challenge), sessionKey);
			OUTout.write(message.getBytes());
			byte[] bytes = new byte[1024];

			OUTin.read(bytes);
			String devState[] = new String(bytes, "UTF-8").trim().split(":");
			
			String temp[] = processMessage(devState).split(","); // [b64 ACK/NACK, b64Challenge]
			this.challenge = enc.base64SDecoder(temp[1]);
			message = new String(enc.base64SDecoder(temp[0]));
			return message;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return "Not able to switch state of device. Please try again.";

	}
	
	public String renewSessionKey() throws Exception{

	
			int nrRenews = this.keyRenewedTimes +1;
			
			byte sessionkey[] = enc.secureRandom(16);
			String newSessionKey = enc.base64SEncoder(sessionkey);
			this.tempSessionKey = newSessionKey;

			String m = "RENEW " + newSessionKey + "," + enc.base64SEncoder(challenge);			// [SESSIONKEY, CHALLENGE]Dk, H(S,Ch), IV

			m = encryptMessage(m, deviceKey);

			OUTout.write(m.getBytes());				//SEND session key
			byte[] bytes = new byte[1024];

			OUTin.read(bytes);						//Expect thing encrypted with new Session key
			String devState[] = new String(bytes, "UTF-8").trim().split(":");
			
			String temp[] = processMessage(devState).split(","); // [b64 ACK/NACK, b64Challenge]
			this.challenge = enc.base64SDecoder(temp[1]);
			String message = new String(enc.base64SDecoder(temp[0]));
			if(message.equals("ACK")){
				this.sessionKey = newSessionKey;
				this.keyRenewedTimes = nrRenews;
				return message;
			}
			else
				return "Not able to renew key of device. Please try again.";

	}
	
	

	public Helper(String deviceName, Socket socket, int deviceListenPort, Set<String> keys) throws UnknownHostException, IOException {

		super("Helper");
		this.socketOUT = socket;
		this.keys = keys;
		this.deviceName=deviceName;

		socketIN = new Socket(socket.getInetAddress().getHostAddress(), deviceListenPort);

		OUTin = new DataInputStream(new BufferedInputStream(socketOUT.getInputStream()));
		OUTout = new PrintStream(socketOUT.getOutputStream());
		INin = new DataInputStream(new BufferedInputStream(socketIN.getInputStream()));
		INout = new PrintStream(socketIN.getOutputStream());
	}

	public void run(){
		
		int errorcount = 0;
		
		while(isValidDevice){
			//PERIODICALLY RENEW SESSION KEY
			String response =""; 
			try {
				Thread.sleep(SESKEY_EXPIRE_TIME);
				response = renewSessionKey();
				/*if(!response.equals("ACK")){
					System.out.println(response);
					isValidDevice=false;
				}*/
			} catch (Exception e) {
				if(!response.equals("ACK")){
					System.out.println("Coulnd't renew session key with device " + deviceName);
					isValidDevice=false;
					break;
				}
			}

		}

	}
	public void pollMsgToSend(String m) {
		try {
			msgToSend.put(m);
		} catch (InterruptedException e) {
			System.out.println("Interrompido enquanto queueing message para mandar");
			e.printStackTrace();
		}
	}

	public Device login() throws IOException{

		byte[] bytes = new byte[1024];

		INin.read(bytes);
		String rcvdMessage = new String(bytes, "UTF-8").trim();
		String cryptogram []=  rcvdMessage.split(":");

		String msg = processMessage(cryptogram);
		String devicedata[] = msg.split(",");

		this.challenge = enc.base64SDecoder(devicedata[3]);

		byte sessionkey[] = enc.secureRandom(16);
		byte IV[] = enc.secureRandom(16);
		this.sessionKey = enc.base64SEncoder(sessionkey);

		String m = this.sessionKey + "," + enc.base64SEncoder(challenge);			// [SESSIONKEY, CHALLENGE]Dk, H(S,Ch), IV

		m = encryptMessage(m, deviceKey);

		try {
			INout.write(m.getBytes());
			
		} catch (NullPointerException e) {
			throw new IOException(e.getMessage());
		}
		
		bytes = new byte[1028];
		INin.read(bytes);
		rcvdMessage = new String(bytes, "UTF-8").trim();
		cryptogram =  rcvdMessage.split(":");
		msg = processMessage(cryptogram);
		String devicemsg[] = msg.split(",");
		this.challenge = enc.base64SDecoder(devicemsg[1]);
		if(new String(enc.base64SDecoder(devicemsg[0])).equals("NACK"))
			throw new IOException();
		
		//IF reached here, all ok.
		Device d = new Device(deviceName,devicedata[1],devicedata[2], this);
		return d;
	}

	public String receiveACKorNACK(DataInputStream in){
		byte[] response = new byte[1024];
		String msg = null;
		try {
			BufferUtil.readInputStreamWithTimeout(in, response, 1000);
			String rcvdMessage = new String(response, "UTF-8").trim();
			String cryptogram []=  rcvdMessage.split(":");

			msg = processMessage(cryptogram);		// [ACK/NACK,CHALLENGE]
			String devicedata[] = msg.split(",");
			this.challenge = enc.base64SDecoder(devicedata[1]);
			msg = new String(enc.base64SDecoder(devicedata[0]));
			
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		
		return msg;

	}
	

	public String processMessage(String[] cryptogram) throws IOException{


		String msg = null;

		if(cryptogram.length!=3) {
			System.out.println("erro 1: Mensagem invalida"); 
			throw new IOException();
		} 

		byte[] Hmac =    enc.base64SDecoder(cryptogram[1]);
		byte[] IV = 	 enc.base64SDecoder(cryptogram[2]);
		String m = null;
		if (deviceKey.equals("")){
			for(String key : keys){
				// If we dont know yet who is is this device, we shall run all the keys available for decryption
				byte[] decryptkey = (enc.base64SDecoder(key));

				System.out.println(BufferUtil.toHexString(decryptkey));
				m = enc.decryptAES(decryptkey, IV, cryptogram[0]);
				if(m != null){
					System.out.println(m);
					deviceKey= key;
					Hmac_key = enc.toSHA1(enc.base64SDecoder(key));
					System.out.println("HMAC KEY:" + BufferUtil.toHexString(Hmac_key));

					try {
						String calculatedMac = enc.calculateHMAC(enc.decryptAESwithPadding(decryptkey, IV, cryptogram[0] ).getBytes(), Hmac_key);

						if(calculatedMac.equals(BufferUtil.toHexString(Hmac)))
							msg = m;
						else continue;
					} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
						e.getMessage();
					}
					break;
				}
			}
		}
		else{
			//we know which device is this
			byte[] decryptkey = (enc.base64SDecoder(sessionKey));
			m = enc.decryptAES(decryptkey, IV, cryptogram[0]);
			if(m != null){ //try decrypt with session key
				try {
					String calculatedMac = enc.calculateHMAC(enc.decryptAESwithPadding(decryptkey, IV, cryptogram[0] ).getBytes(), Hmac_key);

					if(calculatedMac.equals(BufferUtil.toHexString(Hmac)))
						msg = m;
				} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
					e.getMessage();
				}
			}
			else{ // try decrypt with temporary session key
				decryptkey = (enc.base64SDecoder(tempSessionKey));
				m = enc.decryptAES(decryptkey, IV, cryptogram[0]);
				if(m != null){
					try {
						String calculatedMac = enc.calculateHMAC(enc.decryptAESwithPadding(decryptkey, IV, cryptogram[0] ).getBytes(), Hmac_key);

						if(calculatedMac.equals(BufferUtil.toHexString(Hmac)))
							msg = m;
					} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
						e.getMessage();
					}
				}
			}

		}
		if (m == null){ //nenhuma das chave deu	ou a mensagem é invalida
			System.out.println("erro 2: Nao foi possivel decifrar a mensgem");
			throw new IOException();
		}

		return msg;

	}

	public String encryptMessage(String message, String key){
		byte iv[] = enc.secureRandom(16);

		byte[] cryptmsg = enc.encryptAESwithPadding(enc.base64SDecoder(key), iv, enc.base64SEncoder(message.getBytes()));
		try {
			message = enc.base64SEncoder(cryptmsg) +":"+ enc.base64SEncoder(enc.calculateHMACb(message.getBytes(), Hmac_key)) +":"+ enc.base64SEncoder(iv);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e3) {
			e3.printStackTrace();
			message = null;
		}


		return message;

	}
	
	public void closeAllConnections() throws IOException{
		String message = encryptMessage("KILL" + "," + enc.base64SEncoder(challenge), sessionKey);
		OUTout.write(message.getBytes());
		
		OUTin.close();
		OUTout.close();
		INin.close();
		INout.close();
		socketIN.close();
		socketOUT.close();
	}

	public int getKeyRenewedTimes() {
		return keyRenewedTimes;
	}
}

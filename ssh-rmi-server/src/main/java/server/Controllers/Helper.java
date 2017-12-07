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
	private String deviceKey ="";
	private String sessionKey="";
	private String tempSessionKey="";
	private byte[] Hmac_key;
	private volatile byte[] challenge;

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
	
	public String renewSessionKey() {

		try {
			
			
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
			if(message.equals("ACK"))
				this.sessionKey = newSessionKey;
			return message;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Not able to renew key of device. Please try again.";

	}

	public Helper(Socket socket, int deviceListenPort, Set<String> keys) throws UnknownHostException, IOException {

		super("Helper");
		this.socketOUT = socket;
		this.keys = keys;

		socketIN = new Socket(socket.getInetAddress().getHostAddress(), deviceListenPort);

		OUTin = new DataInputStream(new BufferedInputStream(socketOUT.getInputStream()));
		OUTout = new PrintStream(socketOUT.getOutputStream());
		INin = new DataInputStream(new BufferedInputStream(socketIN.getInputStream()));
		INout = new PrintStream(socketIN.getOutputStream());
	}

	public void run(){

		/*String teste = "Mensagemparaagateway" + "," + enc.base64SEncoder(challenge);


		teste = encryptMessage(teste, sessionKey);

		try {
			byte[] response = new byte[1024];
			OUTout.write(teste.getBytes());
			OUTin.read(response);
			System.out.println(new String(response));
		} catch (NullPointerException | IOException e2) {
			e2.printStackTrace();
		}*/
		while(true){

			/*try {

    			byte[] bytes = new byte[1024];

				INin.read(bytes);
				String rcvdMessage = new String(bytes, "UTF-8").trim();
				String cryptogram []=  rcvdMessage.split(":");

				if(cryptogram.length!=3) {
					System.out.println("erro 3 na mensagem"); 
					throw new IOException();
				} 

				byte[] Message = enc.base64SDecoder(cryptogram[0]);
				byte[] Hmac =    enc.base64SDecoder(cryptogram[1]);
				byte[] IV = 	 enc.base64SDecoder(cryptogram[2]);
				//System.out.println(Message.length+":"+Hmac.length+":"+IV.length);
				String m = null;
				for(String key : keys){
					byte[] decryptkey = (enc.base64SDecoder(key));

					System.out.println(BufferUtil.toHexString(decryptkey));
					m = "\""+ enc.decryptAES(decryptkey, IV, cryptogram[0]) + "\"";
					if(m != null){
						System.out.println(m);
						deviceKey= key;
						Hmac_key = enc.toSHA1(enc.base64SDecoder(key));
						System.out.println("HMAC KEY:" + BufferUtil.toHexString(Hmac_key));

						try {
							System.out.println("Calculated HMac:"+ enc.calculateHMAC(
														enc.decryptAESwithPadding(decryptkey, IV, cryptogram[0] ).getBytes(), Hmac_key));
							System.out.println("Message HMac:"+ BufferUtil.toHexString(Hmac));
						} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
							e.getMessage();
						}
						break;
					}
				}
				if (m == null)	System.out.println("erro 4 na mensagem");

				//new EncryptionUtil().calculateHMAC(Message, key);
				System.out.println(rcvdMessage);
//				//if(i > 0) System.out.println(state.trim());

				//Thread.sleep(3000);		// Each 3 seconds, polls the device

			}/*catch(SocketException e){
				// When the connection closes
				System.out.println("TODO: DEVICE CONN FAIL");
			}catch (IOException | InterruptedException  e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			} 
			 */
		}

	}
	//implement your methods here
	public void pollMsgToSend(String m) {
		try {
			msgToSend.put(m);
		} catch (InterruptedException e) {
			System.out.println("Interrompido enquanto queueing message para mandar");
			e.printStackTrace();
		}
	}

	public void login() throws IOException{

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

		//byte[] cryptmsg = enc.encryptAESwithPadding(enc.base64SDecoder(deviceKey), IV, enc.base64SEncoder(m.getBytes()));

		try {
			//String seskey = enc.base64SEncoder(cryptmsg) +":"+ enc.base64SEncoder(enc.calculateHMACb(m.getBytes(), Hmac_key)) +":"+ enc.base64SEncoder(IV);
			//INout.write(seskey.getBytes());
			INout.write(m.getBytes());
			
		} catch (NullPointerException e) {
			throw new IOException(e.getMessage());
		}
		bytes = new byte[1028];
		INin.read(bytes);
		rcvdMessage = new String(bytes, "UTF-8").trim();
		cryptogram =  rcvdMessage.split(":");
		msg = processMessage(cryptogram);
		devicedata = msg.split(",");
		this.challenge = enc.base64SDecoder(devicedata[1]);
		if(new String(enc.base64SDecoder(devicedata[0])).equals("NACK"))
			throw new IOException();

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
}

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
	byte[] Hmac_key;
	private LinkedBlockingQueue <String> msgToSend = new LinkedBlockingQueue <String>();
	
	private Set<String> keys;
	
    public String getDeviceState() {
    	OUTout.print("GETSTATUS");
    	byte[] bytes = new byte[1024];
    	try {
			OUTin.read(bytes);
			deviceState = new String(bytes, "UTF-8").trim();
			String temp = deviceState;
			//deviceState = "";
			return temp;
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return "Not able to get state of device";
		
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
    	EncryptionUtil enc = new EncryptionUtil();
    	

        //Read input and process here
    	//pollMsgToSend("GETSTATUS");pollMsgToSend("SWITCH");pollMsgToSend("GETSTATUS");
    	while(true){
    		
    		try {
   			
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
			}*/catch (IOException /*| InterruptedException */ e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			} 
    		
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
    	EncryptionUtil enc = new EncryptionUtil();
   
    		byte[] bytes = new byte[1024];
			
			INin.read(bytes);
			String rcvdMessage = new String(bytes, "UTF-8").trim();
			String cryptogram []=  rcvdMessage.split(":");
			
			String msg = processMessage(cryptogram);
			String devicedata[] = msg.split(",");
			byte sessionkey[] = enc.secureRandom(16);
			byte IV[] = enc.secureRandom(16);
			
			byte[] message = enc.base64Encoder(enc.encryptAESwithPadding(sessionkey, IV, enc.base64SEncoder(sessionkey)));
			try {
				String seskey = enc.base64SEncoder(message) +":"+ enc.base64SEncoder(enc.calculateHMACb(sessionkey, Hmac_key)) +":"+ enc.base64SEncoder(IV);
				INout.write(seskey.getBytes());
			} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
				throw new IOException(e.getMessage());
			}
			
			
    	
    }

    public String processMessage(String[] cryptogram) throws IOException{
    	EncryptionUtil enc = new EncryptionUtil();
    	
    	String msg = null;
    	
    	if(cryptogram.length!=3) {
			System.out.println("erro 1: Mensagem invalida"); 
			throw new IOException();
		} 
    	
		byte[] Hmac =    enc.base64SDecoder(cryptogram[1]);
		byte[] IV = 	 enc.base64SDecoder(cryptogram[2]);
		//System.out.println(Message.length+":"+Hmac.length+":"+IV.length);
		String m = null;
		for(String key : keys){
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
				} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
					e.getMessage();
				}
				break;
			}
		}
		if (m == null){	
			System.out.println("erro 2: Nao foi possivel decifrar a mensgem");
			throw new IOException();
		}
		
		return msg;
	
    }
    
    public String encryptMessage(String message, byte[] key){
    	
    	
    	
		return message;
    	
    }
    
}

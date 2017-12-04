package server.Controllers;

import java.io.IOException;

import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import utils.EncryptionUtil;

public class Helper extends Thread{
	private Socket socketOUT = null;
	private Socket socketIN = null;
	
	private DataInputStream OUTin= null;
	private PrintStream OUTout= null;
	private DataInputStream INin= null;
	private PrintStream INout= null;
	
	private volatile String deviceState = "";
	private LinkedBlockingQueue <String> msgToSend = new LinkedBlockingQueue <String>();
	
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
    	return "";
		
	}

	public Helper(Socket socket, int deviceListenPort) throws UnknownHostException, IOException {

        super("Helper");
        this.socketOUT = socket;
        
        socketIN = new Socket(socket.getInetAddress().getHostAddress(), deviceListenPort);
        
        OUTin = new DataInputStream(new BufferedInputStream(socketOUT.getInputStream()));
		OUTout = new PrintStream(socketOUT.getOutputStream());
		INin = new DataInputStream(new BufferedInputStream(socketIN.getInputStream()));
		INout = new PrintStream(socketIN.getOutputStream());
    }

    public void run(){
    	
        //Read input and process here
    	//pollMsgToSend("GETSTATUS");pollMsgToSend("SWITCH");pollMsgToSend("GETSTATUS");
    	while(true){
    		
    		try {
   			
    			byte[] bytes = new byte[1024];
    			
				INin.read(bytes);
				String rcvdMessage = new String(bytes, "UTF-8").trim();
				String cryptogram []=  rcvdMessage.split(":");
				if(cryptogram.length!=3) {System.out.println("erro na mensagem"); continue;} 
				
				byte[] Message = new EncryptionUtil().base64SDecoder(cryptogram[0]);
				byte[] Hmac = new EncryptionUtil().base64SDecoder(cryptogram[1]);
				byte[] IV = new EncryptionUtil().base64SDecoder(cryptogram[2]);
				//System.out.println(Message.length+":"+Hmac.length+":"+IV.length);
				
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
    
    public void login(){
    	
    }
}

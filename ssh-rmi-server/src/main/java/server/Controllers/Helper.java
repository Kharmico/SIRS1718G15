package server.Controllers;

import java.io.IOException;

import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class Helper extends Thread{
	private Socket socket = null;
	private DataInputStream in= null;
	private PrintStream out= null;
	private volatile String deviceState = "";
	private LinkedBlockingQueue <String> msgToSend = new LinkedBlockingQueue <String>();
	
    public String getDeviceState() {
    	out.print("GETSTATUS");
    	byte[] bytes = new byte[1024];
    	try {
			in.read(bytes);
			deviceState = new String(bytes, "UTF-8").trim();
			String temp = deviceState;
			//deviceState = "";
			return temp;
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return "";
		
	}

	public Helper(Socket socket) {

        super("Helper");
        this.socket = socket;

    }

    public void run(){
    	try {
    		// Get socket streams
    		in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    		out = new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
            //Read input and process here
    	//pollMsgToSend("GETSTATUS");pollMsgToSend("SWITCH");pollMsgToSend("GETSTATUS");
    	int mCnt = 0;
    	while(true){
    		
    		try {
    			
//    			String message = msgToSend.take();
//    			out.print(message);
//    			System.out.println("Mensagem n" +(mCnt++) +" enviada");
    			
    			byte[] bytes = new byte[1024];
    			
				/*int i =*/ in.read(bytes);
				String rcvdMessage = new String(bytes, "UTF-8").trim();
				System.out.println(rcvdMessage);
//				//if(i > 0) System.out.println(state.trim());
				
				Thread.sleep(3000);		// Each 3 seconds, polls the device
				
			}/*catch(SocketException e){
				// When the connection closes
				System.out.println("TODO: DEVICE CONN FAIL");
			}*/catch (IOException | InterruptedException e) {
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

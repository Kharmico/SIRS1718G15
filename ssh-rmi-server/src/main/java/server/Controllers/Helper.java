package server.Controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.nio.CharBuffer;

public class Helper extends Thread{
	private Socket socket = null;
	public DataInputStream in= null;
	public PrintStream out= null;
	public String state = "";
	
    public Helper(Socket socket) {

        super("Helper");
        this.socket = socket;

    }

    public void run(){
    	try {
			//in = new InputStreamReader(socket.getInputStream());
    		in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    		out = new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
            //Read input and process here
    	while(true){
    		
    		try {
    			out.print("GETSTATUS");
    			
    			byte[] bytes = new byte[1024];
    			
				int i = in.read(bytes);
				state = new String(bytes, "UTF-8");
				//if(i > 0) System.out.println(state.trim());
				
				Thread.sleep(1000);		// Each 3 seconds, polls the device
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				continue;
			}
    		
    	}
    
    }
            //implement your methods here
}

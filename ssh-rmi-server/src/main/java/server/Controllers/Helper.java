package server.Controllers;

import java.net.Socket;

public class Helper extends Thread{
	private Socket socket = null;

    public Helper(Socket socket) {

        super("Helper");
        this.socket = socket;

    }

    public void run(){
            //Read input and process here
    }
            //implement your methods here
}

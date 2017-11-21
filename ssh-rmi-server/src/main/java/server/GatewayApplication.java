package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import server.Controllers.GatewayController;
import server.Services.GatewayService;

public class GatewayApplication {

	public static void main(String[] args){
		String name = args[0];
		int registryPort = Integer.parseInt(args[1]);
		
        System.out.println("Main OK");
        
        try{
            GatewayController controller = new GatewayController();
            System.out.println("After create");
            
            Registry reg = LocateRegistry.createRegistry(registryPort);
			reg.rebind(name, controller);			
           
            System.out.println("Gateway Controller ready");

            System.out.println("Awaiting connections");
            System.out.println("Write \"exit\" to shutdown");
            commandLineThread.start();
            cycle(controller);
            
        }catch(Exception e) {
            System.out.println("Gateway Controller main " + e.getMessage());
        }

	}
	
	public  static void cycle(GatewayController stub) {
		while(true){
			//do stuff
		}
	}
	

	
	private static Thread commandLineThread = new Thread() {
        public void run() {
        	Scanner scanner = new Scanner(System.in);

            while(true) {
            	switch(scanner.nextLine()) {
            		case "exit":
        				System.exit(MAX_PRIORITY);
            			break;
            		default:
            			System.out.println("Unrecognizable command");
            			break;
            	
            	}
            }
        }
    }; 

}

package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.Controllers.GatewayController;
import server.Services.GatewayService;

public class GatewayApplication {

	public static void main(String[] args){
		String name = args[0];
		int registryPort = Integer.parseInt(args[1]);
		
        System.out.println("Main OK");
        
        try{
            GatewayService _ttt = new GatewayController();
            System.out.println("After create");
            
            Registry reg = LocateRegistry.createRegistry(registryPort);
			reg.rebind(name, _ttt);			
           
            System.out.println("TTT server ready");

            System.out.println("Awaiting connections");
            System.out.println("Press enter to shutdown");
            System.in.read();
            
        }catch(Exception e) {
            System.out.println("TTT server main " + e.getMessage());
        }

	}

}

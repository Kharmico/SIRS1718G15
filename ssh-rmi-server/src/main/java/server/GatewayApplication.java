package server;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Scanner;

import server.Controllers.GatewayController;
import server.Controllers.Helper;
import server.Services.GatewayService;

public class GatewayApplication {
	public static final int SERVERPORT = 8080;

	public static void main(String[] args){
		System.out.println(args[0] + " " + args[1]);
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
			new commandLineThread(controller).start();
			cycle(controller);

		}catch(Exception e) {
			System.out.println("Gateway Controller main " + e.getMessage());
			e.printStackTrace();
		}

	}

	public  static void cycle(GatewayController stub) throws IOException, InterruptedException {
		int devListenPort = stub.createListeningSocket(SERVERPORT);
		System.out.println("Listening new devices registration at port " + devListenPort);
		stub.startListeningDevices();
		System.out.println("sonic");
		while(true){
			//do stuff
			Thread.sleep(3000);
			/*Map<String, Helper> devConnections = stub.devConnections;
			if(devConnections == null) continue;
			for ( Map.Entry<String, Helper> e : devConnections.entrySet()){
				String state = e.getValue().getDeviceState();
				if ( state == null ) state = "Null";
				System.out.println(e.getKey() +":"+ state);
			}*/

		}
	}



	private static class commandLineThread extends Thread {
		private final GatewayController stub;
		public commandLineThread(GatewayController stub) {
			this.stub=stub;
		}
		public void run() {
			Scanner scanner = new Scanner(System.in);

			while(true) {
				String cmd = scanner.nextLine();

				if(cmd.equals("getstatus")) {
					Map<String, Helper> devConnections = stub.devConnections;
					if(devConnections == null) continue;
					for ( Map.Entry<String, Helper> e : devConnections.entrySet()){
						String state = e.getValue().getDeviceState();
						if ( state == null ) state = "Null";
						System.out.println(e.getKey() +":"+ state);
					}
				}
				else if(cmd.equals("exit")){
					System.exit(MAX_PRIORITY);
				}
				else if(cmd.contains("addkey")){
					String command[] = cmd.split(" ");
					stub.addKey(command[1]);
				}
				else if(cmd.contains("listkeys")){
					for(String s: stub.b64keys)
						System.out.println(s);
				}
				else if(cmd.contains("removekey")){
					String command[] = cmd.split(" ");
					stub.removeKey(command[1]);
				}
				else{
					System.out.println("Unrecognizable command");
				}

			}
		}
	}
} 



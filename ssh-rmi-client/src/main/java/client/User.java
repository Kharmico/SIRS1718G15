package client;

import java.rmi.*;
import java.util.Scanner;

import server.Services.GatewayService;

public class User {
	GatewayService ttt;
	Scanner keyboardSc;
	int winner = 0;
	int player = 1;
	
	public User(GatewayService stub) throws RemoteException {
		ttt = stub;
		keyboardSc = new Scanner(System.in);
	}

	public int readPlay() {
		int play;
		do {
			System.out.printf("\nPlayer %d, please enter the number of the square "
							+ "where you want to place your %c (or 0 to refresh the board): \n",
							player, (player == 1) ? 'X' : 'O');
			play = keyboardSc.nextInt();
		} while (play > 10 || play < 0);
		return play;
	}

	public void playGame() throws RemoteException {
		int play;
		boolean playAccepted;

		do {
			player = ++player % 2;
			do {
				System.out.println(ttt.currentBoard());
				play = readPlay();
				if (play > 0 && play < 10) {
					playAccepted = ttt.play( --play / 3, play % 3, player);
					if (!playAccepted)
						System.out.println("Invalid play! Try again.");
				} 
				else if(play == 10){
					ttt.reiniciar();
					playAccepted = false;
				}else
					playAccepted = false;
			} while (!playAccepted);
			winner = ttt.checkWinner();
		} while (winner == -1);
	}

	public void congratulate() {
		int play;
		if (winner == 2)
			System.out.printf("\nHow boring, it is a draw\n");
		else
			System.out.printf(
					"\nCongratulations, player %d, YOU ARE THE WINNER!\n",
					winner);
		
		do {
			System.out.printf("10 para reiniciar; \n");
			play = keyboardSc.nextInt();
		} while (play != 10);
	}
	
	
    public static void main(String[] args) throws Exception {

    	GatewayService server = null;
		try {
			server = (GatewayService) Naming.lookup(args[0]);
			System.out.println("Found server");

			while(true){
				User g = new User(server);
				
				g.playGame();
				g.congratulate();
			}
			
			
			} catch (Exception e) {
				System.out.println("Lookup: " + e.getMessage());
		}

    }
}

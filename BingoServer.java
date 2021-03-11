import java.net.*;
import java.io.*;
import java.lang.Thread;
import java.util.Enumeration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class BingoServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
		BingoRoom bri = new BingoRoom();
                

		// verify we have a port #
		if ( args.length != 1 ) {  
			System.out.println("Error: No port # provided!");
			System.exit(0);
		}
		
		// display the IP address(es) of the server
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration ee = n.getInetAddresses();
			while (ee.hasMoreElements())
			{
				InetAddress i = (InetAddress) ee.nextElement();
				// don't worry about link local or loopback addrs
				if ( i.isLinkLocalAddress() || i.isLoopbackAddress() )
					continue;
				System.out.println( "Local IP Addr: " + i.getHostAddress());
			}
		}

        try {
            serverSocket = new ServerSocket(12345);
        } catch (IOException ex) {
            System.err.println("Could not listen on port 12345" ); 
            System.exit(-1);
        }
        new StartUp(bri).start(); //start the loop that waits for enough players to play a game of bingo
        while (listening)
		{
			// start a client thread
            new BingoClientThread(serverSocket.accept(), bri).start(); 
            
            
                
			// debugging output
			System.out.println("Started new thread");
		}

        serverSocket.close();
    }
}

class BingoRoom {
	ArrayList<BingoClientThread> clientList = new ArrayList<>(); 
        boolean won = false;
        int gameSize;

	// have a new client
	public synchronized void newClient( BingoClientThread b ) { 
		System.out.println("Adding client");
		clientList.add( b );
	}

	// handle a client who is quitting
	public synchronized void removeClient( BingoClientThread b ) {
		// remove the client object from the list of clients
                if (b.inGame == true) //reduce the number of in game players by one
                    gameSize--;
		clientList.remove( b );
                
                
	}
	
	// send a message to everyone in game
	public synchronized void notifyGame( String message ) {
		System.out.println("Sending to clients in game" );
		for( int i=0; i< clientList.size(); i++ ) {
                    if (clientList.get(i).inGame == true) {
			clientList.get(i).notifyClient( message );
                    }
		}
                
	}
        //send a message to everyone
        public synchronized void notifyAll( String message ) {
		System.out.println("Sending to " + clientList.size() + " clients" );
		for( int i=0; i< clientList.size(); i++ ) {
			clientList.get(i).notifyClient( message );
		}
                
	}
        
        void game() {
            Random rand = new Random();
            String message = "";
            
        
            for( int i=0; i < clientList.size(); i++ ) {
                if (clientList.get(i).inGame == true){ //make sure the clients joined before the game began
                    for (int j = 0; j < 5; j++) {
                        for (int k = 0; k < 5; k++) {
                            message += rand.nextInt(99) + "    "; //send 5 lines of 5 numbers to make a 5x5 bingo card for each client
                        }
                        try {
                        Thread.sleep(100);
                       }   catch (InterruptedException ie) { //wait for clients to check if they have bingo
                           ie.printStackTrace();
                      }
                        clientList.get(i).notifyClient(message);
                        message = "";
                    }
                }
            }
        
            while (true && !won) {
                
                
                if (gameSize < 2) { //if too many players drop out in game end the game
                    message = "not enough players to continue";
                    notifyGame(message);
                    break;
                }
                    
                message = "The number is " + rand.nextInt(99); //send a random number for bingo
                notifyGame(message);
                
                try {
                    Thread.sleep(30000);
                }   catch (InterruptedException ie) { //wait for clients to check if they have bingo
                        ie.printStackTrace();
                    }
            }
            
            for (int i=0; i < clientList.size(); i++) { //remove clients from game
                clientList.get(i).inGame = false;
            }
            notifyAll("a new game will begin shortly");
            won = false;
            gameSize = 0;
        }
}


class StartUp extends Thread {
    BingoRoom bingoRoom;
    StartUp(BingoRoom br) {
        bingoRoom = br;
    }
    public void run() {
            while (true) {
                try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ie) { //wait so that new clients can put in their name before the game starts
                        ie.printStackTrace();
                    }
                if (bingoRoom.clientList.size() >= 2) { //if there are at least two clients connected make a game
                    
                    for (int i = 0; i < bingoRoom.clientList.size(); i++){ // let system know the clients who have already joined are in the game
                        bingoRoom.clientList.get(i).inGame = true;
                        bingoRoom.gameSize++;
                    }
                    
                    bingoRoom.notifyAll("a new game is beginning");
                    
                    
                    
                    bingoRoom.game();
                }
            }
        }
    
}
class BingoClientThread extends Thread {
        boolean inGame = false;
	BingoRoom bingoRoom;
	private Socket socket = null;
	InputStream in;
	OutputStream out;
	
	public BingoClientThread( Socket s, BingoRoom br ) {
		socket = s;
		bingoRoom = br;
	}
	
	// send a message to this client
	public void notifyClient( String message ) {
		try {
			// send it to the client
			byte[] userdata = message.getBytes();
			out.write( userdata );
		} catch (IOException e) { 
			closeClient();
		}
	}
	
	// close this client
	public void closeClient() {
		try {
			// forget this client
			socket.close();
			bingoRoom.removeClient(this);
		} catch (IOException e) {
			// ignore now
		}
	}
	
	// handle incoming data from this client
    public void run() {
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
			
			// ready to handle messages, so register with the chat room
			bingoRoom.newClient( this );
			
			while ( true )
			{
				byte[] userdata = new byte[200];

				// read up to 200 chars from the input
				int chars_read = in.read( userdata, 0, 200 );

				// end of file?
				if ( chars_read < 0 ) 
					break;					// yes, at EOF
					
				// convert to a string
				String str = new String( userdata, 0, chars_read );
				System.out.println("Received: " + str);
				
				// client leaving?
				if ( str.substring( 0, 5 ).equals("/quit") ) {
					// client is leaving
                                        bingoRoom.notifyAll( str.substring(6) + " is leaving");
					
					// confirm the quit
					notifyClient("/quit");
					
					closeClient();
					
					break;
				}
                                if (str.substring( 0, 5 ).equals("BINGO") && inGame) { //if the message received is BINGO then the client has won
					bingoRoom.notifyGame( str.substring(5) + " has won");
					bingoRoom.won = true;
					
					
				}
                                if (str.substring( 0, 8 ).equals("username")) {
					bingoRoom.notifyAll( str.substring(8) + " has joined");
                                }
			}
			
			out.close();
			in.close();
			socket.close(); 
		} catch ( SocketException e ) {				
			closeClient();
		} catch (IOException e) {
			e.printStackTrace(); 	
		}
		System.out.println("Thread exiting");
	}
	
}


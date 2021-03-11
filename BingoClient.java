import java.io.*;
import java.net.*;

public class BingoClient {
    public static void main(String[] args) throws IOException {

        Socket echoSocket = null;
        PrintWriter out = null;

        try {
            echoSocket = new Socket(args[0], 12345 );
            out = new PrintWriter(echoSocket.getOutputStream(), true); 

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + args[0]); 
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + args[0] );
            System.exit(1);
        }
		TcpClientDataThread inThread = new TcpClientDataThread(echoSocket);
		inThread.start();
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		// get the username
		System.out.print("Type your username: ");
		String username = stdIn.readLine();
		
		// send data without CR/LF
		OutputStream rawout = echoSocket.getOutputStream();

		byte[] b = new byte[200];
                rawout.write(("username" + username).getBytes() );
		System.out.println("when a game starts type BINGO to win or QUIT: ");

		// process one line of text
		while ( true ) {
			userInput = stdIn.readLine();
			if ( userInput == null || userInput.equals("QUIT") ) {
				// tell server we are quitting
				String quitMessage = "/quit " + username;
				rawout.write( quitMessage.getBytes() );
				break;
			}
                        if ( userInput.equals("BINGO") ) {
				// tell server we are have bingo
				String message = "BINGO" + username;
				rawout.write( message.getBytes() );
                        }
			
			
		}

		stdIn.close();
		
		// wait for input thread to stop
		try {
			// wait for any incoming messages
			inThread.join(); //Q: whats this?
			
			// close the socket
			echoSocket.close();
		} catch ( InterruptedException e ) {
			// ignore
		}
    }
}

// handle data coming in from the server
class TcpClientDataThread extends Thread {
	Socket echoSocket = null;
	InputStream rawin;
	
	public TcpClientDataThread( Socket s ) {
		// get the socket and corresponding input stream
		echoSocket = s;
		try {
			rawin = echoSocket.getInputStream(); 
		} catch ( IOException e ) {
			System.out.println("i/O exception on socket");
		}
	}
	
	public void run() {
		byte[] b = new byte[200];
		
		while( true ) {
			try {
				// get a message from the server
				int rlen = rawin.read( b, 0, 200 );
				
				// make it into a string
				String s = new String( b, 0, rlen );
				
				// is it a quit confirmation?
				if ( s.equals("/quit") ) {
					// yes, confirming quit
					echoSocket.close(); 
					return;
				}
				
				// display the message
				System.out.println(s);
			} catch ( IOException e ) {
				System.out.println("i/O exception on socket");
				return;
			}
		}
	}
}

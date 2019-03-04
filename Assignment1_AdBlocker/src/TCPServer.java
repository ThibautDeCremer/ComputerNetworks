import java.io.*;  import java.net.*;

public class TCPServer {
	public static void main(String argv[]) throws Exception {
		System.out.println("Running...");
		
		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //BufferedReader receives characters and buffers them to allow more efficient reading practices | InputStreamReader converts bytes into chars | system.in is an input mechanism
		ServerSocket s = new ServerSocket(800);
		while(true) {
			Socket connectionSocket = s.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			String clientSentence = inFromClient.readLine();  
			System.out.println("Received: " + clientSentence);
			connectionSocket.close();
			s.close();
		}
		//System.out.println("Done");
	}
}

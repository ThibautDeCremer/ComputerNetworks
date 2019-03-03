import java.io.*;  import java.net.*;
public class TCPClient {
	public static void main(String argv[]) throws Exception {
		System.out.println("Running...");
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //BufferedReader receives characters and buffers them to allow more efficient reading practices | InputStreamReader converts bytes into chars | system.in is an input mechanism
		Socket clientSocket = new Socket("localhost", 6789); //Socket creation
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new  InputStreamReader(clientSocket.getInputStream()));
		String sentence = inFromUser.readLine(); //Read command line for string to send
		outToServer.writeBytes(sentence + '\n'); //Send string to server
		String modifiedSentence = inFromServer.readLine(); //Read response
		System.out.println("FROM SERVER: " + modifiedSentence); //Print server response
		clientSocket.close(); //Close the socket
		System.out.println("Done");
	}
}

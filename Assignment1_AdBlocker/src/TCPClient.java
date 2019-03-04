import java.io.*;  import java.net.*; import java.util.*;

public class TCPClient {
	public static void main(String argv[]) throws Exception {
		System.out.println("Running...");
		
		//Scanner in = new Scanner(System.in);
		//System.out.println("Enter a string");
		//String string = in.nextLine();
		//System.out.println(string);
		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //BufferedReader receives characters and buffers them to allow more efficient reading practices | InputStreamReader converts bytes into chars | system.in is an input mechanism
		Socket s = new Socket(InetAddress.getByName("www.example.com"), 80);
		PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
		pw.println("GET / HTTP/1.1");
	    pw.println("Host: example.com");
		pw.println("Connection: Close"); //Add this when it's the last communication
		pw.println();
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String t;
		while((t = br.readLine()) != null) System.out.println(t);
		br.close();
		s.close();
		System.out.println("Done");
	}
}

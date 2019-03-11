import java.io.*;  import java.net.*; import java.util.*;

public class TCPClient {
	public static void main(String argv[]) throws Exception {
		System.out.println("Running...");
		
		//Scanner in = new Scanner(System.in);
		//System.out.println("Enter a string");
		//String string = in.nextLine();
		//System.out.println(string);
		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //BufferedReader receives characters and buffers them to allow more efficient reading practices | InputStreamReader converts bytes into chars | system.in is an input mechanism
		Socket s = new Socket(InetAddress.getByName("localhost"), 5111);
		PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
		String message = "This is a testMessage";
		pw.println("PUT /test.txt HTTP/1.1");
	    pw.println("Host: example.com");
		pw.println("Connection: Close");
		pw.println("Content-length: "+message.length());
		pw.println("Content-type: text/html");//Add this when it's the last communication
		pw.println("");
		pw.println(message);
		pw.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String t;
		while((t = br.readLine()) != null) System.out.println(t);
		br.close();
		s.close();
		System.out.println("Done");
	}
}

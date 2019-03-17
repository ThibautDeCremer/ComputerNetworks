import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TestCase {
	public static void main(String[] args) throws IOException, WrongRequestException {
		//ServerSocket m_ServerSocket = new ServerSocket(6001);
		try {
			POST();
			System.out.println("Done");
		} catch (Exception e) {
			//m_ServerSocket.close();
			e.printStackTrace();
		}
	}
	
	private static void POST() throws IOException {
		String host = null;
		String path = "/";
		
		if (args[1].contains("/")) // if-else statement to retrieve the host and path
		{
			int index = args[1].indexOf("/");
			int l = args[1].length();
			
			host = args[1].substring(0, index);
			path = args[1].substring(index, l);
		}
		
		else
			host = args[1];
		
		//host="localhost";		//TODO:newPOST change host and path to the correct things
		//path="/tempFile.txt";
		System.out.println(args[0]);
		System.out.println(host);
		System.out.println(path);
		
		System.out.println("Enter something to upload in server: ");
		Scanner scanner = new Scanner(System.in);
		String write = scanner.nextLine();
		
		Socket sock = new Socket(InetAddress.getByName(host),6111);
		BufferedWriter wr = new BufferedWriter( new OutputStreamWriter(sock.getOutputStream(),"UTF8"));
		//String write = URLEncoder.encode(input,"UTF-8"); // www works with UTF-8
		
		wr.write(args[0]+" " + path + " HTTP/1.1\r\n");
		wr.write("Host: " + host + "\r\n");
		wr.write("Content-Type: text/html\r\n"); //application/x-www-form-urlencoded\r\n
		wr.write("Content-Length: " + write.length() + "\r\n");
		wr.write("Connection: Close\r\n");
		wr.write("\r\n");
		wr.write(write+"\r\n"); //TODO:newPOST Server needs a \r\n after message
		wr.flush();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		String t;
		
		while(((t = br.readLine()) != null))
		{
			System.out.println(t);
		}
		
		br.close();
		scanner.close();
		wr.close();
		sock.close();
		System.out.println("Done");
		return;
	}
	
	private static void printRequest(Socket clientSocket) throws IOException {
		BufferedReader reader  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String t=reader.readLine();
    	while(!t.equals("")) {
    		System.out.println(t);
    		/*
    		String[] temp = t.split(":");
    		map.put(temp[0].trim(), temp[1].trim());
    		*/
    		t = reader.readLine();
    	}
    	//reader.close();
	}
	
	private static void streamImage(Socket clientSocket) throws IOException {
		OutputStream os = clientSocket.getOutputStream();
		File file=new File(System.getProperty("user.dir"),"/tempfile.txt");
		byte[] fileContent = Files.readAllBytes(file.toPath());
		String responseHeader = "HTTP/1.1 200 OK \r\n"; 
		String extra = "Content-Length: " + fileContent.length + "\r\n" + 
				"Content-Type: image/jpeg \r\n";
		//ObjectOutputStream output = new ObjectOutputStream(os);
		//output.write(arg0);
		responseHeader+=extra;
		os.write((responseHeader+"\r\n").getBytes());
		os.write(fileContent);
		os.flush();
	}
}

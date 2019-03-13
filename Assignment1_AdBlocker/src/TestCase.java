import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TestCase {

	public static void main(String[] args) throws IOException, WrongRequestException {
		ServerSocket m_ServerSocket = new ServerSocket(6001);
		try {
			Socket clientSocket = m_ServerSocket.accept();
			printRequest(clientSocket);
			streamImage(clientSocket);
			//printRequest(clientSocket);
			m_ServerSocket.close();
			System.out.println("Done");
		} catch (Exception e) {
			m_ServerSocket.close();
			e.printStackTrace();
		}
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

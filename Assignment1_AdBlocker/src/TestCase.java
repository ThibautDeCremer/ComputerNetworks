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
			OutputStream os = clientSocket.getOutputStream();
			File file=new File(System.getProperty("user.dir"),"robot.jpg");
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
			m_ServerSocket.close();
			System.out.println("Done");
		} catch (Exception e) {
			m_ServerSocket.close();
			e.printStackTrace();
		}
	}
}

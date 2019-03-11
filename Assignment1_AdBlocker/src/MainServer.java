import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class MainServer {
	public static void main(String[] args) throws Exception {
	
	System.out.println("Running Server");
	ServerSocket m_ServerSocket = new ServerSocket(5111);
    int id = 0;
    
    try {
		while (true) {
		  Socket clientSocket = m_ServerSocket.accept();
		  HTTPRequestHandlerThread cliThread = new HTTPRequestHandlerThread(clientSocket, id++);
		  cliThread.start();
		}
	} catch (Exception e) {
		m_ServerSocket.close();
		e.printStackTrace();
	}
	
  }
  
  
}
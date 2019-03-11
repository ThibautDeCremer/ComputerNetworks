import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
  
// Server class 
public class mainServerThread  
{ 
    public static void main(String[] args) throws IOException  
    { 
        // server is listening on port 5056 
        ServerSocket ss = new ServerSocket(9000); 
          
        // running infinite loop for getting 
        // client request 
        boolean isRunning=true;
        while (isRunning)  
        { 
            Socket s = null; 
              
            try 
            { 
                // socket object to receive incoming client requests 
                s = ss.accept(); 
                  
                System.out.println("A new client is connected : " + s); 
                  
                // obtaining input and out streams 
                  
                System.out.println("Assigning new thread for this client"); 
  
                // create a new thread object 
                Thread t = new Thread(new HTTPRequestHandler(s, "Multithreaded Server")); 
  
                // Invoking the start() method 
                t.start(); 
                isRunning=false;
                  
            } 
            catch (Exception e){ 
                s.close(); 
                e.printStackTrace(); 
            } 
            ss.close();
        } 
    } 
} 

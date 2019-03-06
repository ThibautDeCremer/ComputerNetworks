import java.io.*;  import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**

 */
public class HTTPRequestHandler implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;

    public HTTPRequestHandler(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
    }

    public void run(){
        try {
            BufferedReader input  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            long time = System.currentTimeMillis();
            //String message = (String) input.readObject();
            try {
				Map<String, String> parsedRequest = this.ParseRequestWithReader(input);
				String[] response = null; //has 2 elements [0]=headers+response code | [1]=body
				if (parsedRequest.keySet().contains("if-modified-since")) response= this.handleIfModifiedSince(parsedRequest);
				switch (parsedRequest.get("type")) {
					case "GET": response = this.GET(parsedRequest);
					case "HEAD": response = this.HEAD(parsedRequest);
					case "PUT": response = this.PUT(parsedRequest);
					case "POST": response = this.POST(parsedRequest);
				}
				// 501 not implemented
				
				//on 200 and GET open requested file and put html code in the body
				//implement not closing on Connection: keep-alive
				response[0]="HTTP/1.1 200 OK"
								+ this.getDateHeader()
								+ "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT"
								+ "Content-Length: "
								+ "Content-Type: text/html"
								+ "Connection: Closed";
				if (parsedRequest.get("Connection")=="Close") {
					response[0]+="Connection: Closed";
					//close connection		
				} else {
					//keep connection open
				}
					
			} catch (WrongRequestException e) {
				if (e.getReason()=="invalid Request Format") {
					String body = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>400 Bad Request</title></head><body><h1>Bad Request</h1></body></html>";
	 				output.write(("HTTP/1.1 400 Bad Request \r\n"
							+ this.getDateHeader()
	 						+ "Content-Length: "+ Integer.toString(body.length())
	 						+ "Content-Type: text/html; charset=iso-8859-1"
	 						+ "Connection: Closed").getBytes());
				} else {
					String body = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 1.1//EN\">\r\n" + 
							"<HTML><HEAD>\r\n" + 
							"<TITLE>404 Not Found</TITLE>\r\n" + 
							"</HEAD><BODY>\r\n" + 
							"<H1>Not Found</H1>\r\n" + 
							"The requested URL was not found on this server.\r\n" + 
							"<HR>\r\n" +  
							"</BODY></HTML>";
	 				output.write(("HTTP/1.1 404 Not Found \r\n"
							+ this.getDateHeader()
	 						+ "Content-Length: "+ Integer.toString(body.length())
	 						+ "Content-Type: text/html; charset=iso-8859-1"
	 						+ "Connection: Closed").getBytes());
				}
			}
            output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable: " +
            		this.serverText + " - " + time + "").getBytes());
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        } catch (IOException /**| ClassNotFoundException*/ e) {
            //This is the 500 response code
        }
    }

	private String[] POST(Map<String, String> parsedRequest) {
		// TODO Auto-generated method stub
		
	}

	private String[] PUT(Map<String, String> parsedRequest) {
		// TODO Auto-generated method stub
		
	}

	private String[] HEAD(Map<String, String> parsedRequest) {
		// TODO Auto-generated method stub
		
	}

	private String[] GET(Map<String, String> parsedRequest) {
		// TODO Auto-generated method stub
		
	}

	private String[] handleIfModifiedSince(Map<String, String> parsedRequest) {
		// TODO Auto-generated method stub
		
	}

	private String generateResponseCode(Map<String, String> parsedRequest) throws WrongRequestException {
		String responseCode="200";
    	if (!parsedRequest.keySet().contains("Host")
				&& parsedRequest.get("version")=="1.1") throw new WrongRequestException("invalid Request Format");
		if (!this.isValidPath(parsedRequest.get("path"))
				&& parsedRequest.get("type")!="PUT") responseCode="404";
		if (parsedRequest.keySet().contains("if-modified-since")) responseCode="304";
    	return responseCode;
	}

	private String getDateHeader() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateHeader = "Date: " + format.format(new Date()) + " GMT";

        return dateHeader;
	}

	public Map<String,String> ParseRequestWithReader(BufferedReader reader) throws IOException, WrongRequestException{
    	Map<String, String> map = new HashMap<String, String>();
    	String currLine = reader.readLine();
    	String[] currCommand = currLine.split("\\s+");
    	if (currCommand.length!=3
    			|| !this.isValidMethod(currCommand[0])
    			|| !this.isValidVersion(currCommand[2])) throw new WrongRequestException("invalid Request Format");
    	if (!this.isValidPath(currCommand[1])) throw new WrongRequestException("404 not found");
    	map.put("type", currCommand[0]);
    	map.put("path", currCommand[1]);
    	map.put("version", currCommand[2]);
    	String t=null;
    	while((t = reader.readLine()) != "");
    		String[] temp = t.split(":");
    		map.put(temp[0].trim(), temp[1].trim());
    	String body = "";
    	while((t = reader.readLine()) != null) body+=t;
    	map.put("body", body);
    	return map;
    }
    
    private boolean isValidVersion(String version) {
		String[] temp=version.split("/");
		String[] ver=temp[1].split(".");
    	return temp[0]=="HTTP"
    			&& ver[0]=="1"
    			&& (ver[1]=="1" || ver[1]=="0");
	}

	private boolean isValidPath(String path) {
    	File tmpDir = new File(path);
    	return tmpDir.exists();
	}

	private boolean isValidMethod(String method) {
		return method=="GET"
				|| method=="HEAD"
				|| method=="POST"
				|| method=="PUT";
	}

	public Map<String,String> ParseRequest(String HTTPRequest){
    	Map<String, String> map = new HashMap<String, String>();
    	int firstNonEmptyIndex = 0;
    	char lastChar=' ';
    	for (int i=0;i<HTTPRequest.length();i++) {
    		char currChar = HTTPRequest.charAt(i);
    		
    		//map.size() >= 3 enkel nog headers
    		if (currChar=='\\' && map.size()>=3) {
    			String headerLine=HTTPRequest.substring(firstNonEmptyIndex, i);
				String[] temp = headerLine.split(": ");
				map.put(temp[0],temp[1]);
    			if (HTTPRequest.substring(i, i+2)=="\\r") {
    				i+=3;
    			} else {
    				i+=1;
    			}
    			firstNonEmptyIndex=i+1;
    			//Check of de nieuwe lijn ook geen \n of \r\n bevat => dan moet met headers zoeken gestopt worden
    		} else if (currChar!=' ') {
    			if(lastChar==' ') {
    				firstNonEmptyIndex = i;
    			}
    		} else { //currChar==' '
    			if (lastChar!=' ') {
    				switch(map.size()) {
    				case 0:
    					map.put("type", HTTPRequest.substring(firstNonEmptyIndex, i+1));
    				case 1:
    					map.put("path", HTTPRequest.substring(firstNonEmptyIndex, i+1));
    				case 2:
    					map.put("HTTP-version", HTTPRequest.substring(firstNonEmptyIndex, i+1));
    				}
    			}
    			
    		}
    		lastChar=currChar;
    	}
    	return map;
    }
    	
}
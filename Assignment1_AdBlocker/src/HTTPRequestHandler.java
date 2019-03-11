import java.io.*;  import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**

 */
public class HTTPRequestHandler implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;
    protected String notImplemented = "501 Not Implemented";

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
				String[] response = {"",""}; //has 2 elements [0]=headers+response code | [1]=body
				if (parsedRequest.keySet().contains("if-modified-since")) response=this.handleIfModifiedSince(parsedRequest);
				switch (parsedRequest.get("type")) {
					case "GET": response = this.GET(parsedRequest);
							break;
					case "HEAD": response = this.HEAD(parsedRequest);
							break;
					case "PUT": response = this.PUT(parsedRequest);
							break;
					case "POST": response = this.POST(parsedRequest);
							break;
					default: response[0] = this.notImplemented;
							break;
				}
				boolean closeSocket = true;
				//implement not closing on Connection: keep-alive
				if (parsedRequest.get("Connection").equals("Keep-Alive")) {
					closeSocket = false;
				} else {
					response[0]+="Connection: Closed";
				} 
				output.write((response[0]+response[1]).getBytes());
				output.close();
 	            input.close();
 	            System.out.println(closeSocket);
 	            if (closeSocket) {this.clientSocket.close();};
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
	 				output.close();
	 	            input.close();
	 	            this.clientSocket.close();
				}
			}
            System.out.println("Request processed: " + time);
        } catch (IOException | ParseException/**| ClassNotFoundException*/ e) {
            //This is the 500 response code
        }
    }

	private String[] POST(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
		String path = parsedRequest.get("path");
		String fileName = this.determineNewfileName(path);
		String content = parsedRequest.get("body");
		String[] response = {"",""};
		if (this.FileExists(fileName)) {
			this.appendToFile(fileName, content);
			response[0] = "HTTP/1.1 200 OK \r\n"
					+ this.getDateHeader() + "\r\n"
					+ "Content-Location: " + path + fileName + "\r\n";
		} else {
			this.createNewFile(fileName, content);
			response[0] = "HTTP/1.1 201 Created \r\n"
					+ this.getDateHeader() + "\r\n"
					+ "Content-Location: " + path + fileName + "\r\n";
		}
		response[1] = "\r\n";
		return response;
	}

	/**
	 * creates a new text file or replaces a representation of the target resource with the request payload
	 * @param parsedRequest
	 * @return
	 * @throws WrongRequestException
	 * @throws IOException 
	 */
	private String[] PUT(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
		String path = parsedRequest.get("path");
		String fileName = this.determineNewfileName(path);
		String content = parsedRequest.get("body");
		String[] response = {"",""};
		if (this.FileExists(fileName)) {
			this.overWriteFile(fileName, content);
			response[0] = "HTTP/1.1 200 OK \r\n"
					+ this.getDateHeader() + "\r\n"
					+ "Content-Location: " + path + fileName + "\r\n";
		} else {
			this.createNewFile(fileName, content);
			response[0] = "HTTP/1.1 201 Created \r\n"
					+ this.getDateHeader() + "\r\n"
					+ "Content-Location: " + path + fileName + "\r\n";
		}
		response[1] = "\r\n";
		return response;
	}
	
	private String determineNewfileName(String path) throws WrongRequestException {
		int indexOfLastSlash = path.lastIndexOf("/");
		String newFileName = path.substring(indexOfLastSlash,path.substring(indexOfLastSlash).indexOf(".")) + ".txt";
		if (this.isValidPath("/"+newFileName)) throw new WrongRequestException("invalid Request Format");
		return newFileName;
	}
	
	/**
	 * creates a file and 
	 * @param newFileName The name for the new file
	 * @return true if and only if the file had to be created, else false
	 * @throws IOException 
	 */
	private boolean putFile(String newFileName, String content) throws IOException {
		boolean createdFile = false;
		File file = new File(System.getProperty("user.dir"), newFileName);
		if (!file.exists()) {
			file.createNewFile();
			createdFile=true;
		}
		//to this file can then be written using any previously seen methods
		FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
		fr.write("content");
		fr.close();
		return createdFile;
	}
	
	private boolean FileExists(String fileName) {
		return (new File(System.getProperty("user.dir"), fileName)).exists();
	}
	
	private void createNewFile(String newFileName, String content) throws IOException {
		File file = new File(System.getProperty("user.dir"), "tempfile.txt");
		if (!file.exists()) file.createNewFile();
		//to this file can then be written using any previously seen methods
		FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
		fr.write("data");
		fr.close();
	}
	
	private void appendToFile(String fileName, String content) throws IOException {
		File file = new File(System.getProperty("user.dir"), fileName);
		FileWriter fr = new FileWriter(file, true); //false means that if file exists that has to be overwritten
		fr.write(content);
		fr.close();
	}
	
	private void overWriteFile(String fileName, String content) throws IOException {
		File file = new File(System.getProperty("user.dir"), fileName);
		FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
		fr.write(content);
		fr.close();
	}

	private String[] HEAD(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
		String path = parsedRequest.get("path");
		if (!this.isValidPath(path)) throw new WrongRequestException("404 not found");
		String[] response = {"",""};
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(path));
	    String str;
	    while ((str = in.readLine()) != null) {
	        contentBuilder.append(str);
	    }
	    in.close();
		String content = contentBuilder.toString();
		response[0] = "HTTP/1.1 200 OK \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Last-Modified: "+ this.getLastModifiedHeader(path) +"\r\n"
						+ "Content-Length: " + content.length() + "\r\n"
						+ "Content-Type: text/html \r\n";
		response[1] = "\r\n";
		return response;
	}

	private String[] GET(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
		System.out.println("Starting GET");
		String path = parsedRequest.get("path");
		if (path.equals("\\")
				|| path.equals("/")) path="index.html";
		System.out.println(this.isValidPath(path));
		if (!this.isValidPath(path)) throw new WrongRequestException("404 not found");
		String[] response = {"",""};
		System.out.println(path);
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(new File(System.getProperty("user.dir"),path)));
	    String str;
	    while ((str = in.readLine()) != null) {
	    	contentBuilder.append(str);
	    }
	    in.close();
		String content = contentBuilder.toString();
		response[0] = "HTTP/1.1 200 OK \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Last-Modified: "+ this.getLastModifiedHeader(path) +"\r\n"
						+ "Content-Length: " + content.length() + "\r\n"
						+ "Content-Type: text/html \r\n";
		response[1] = "\r\n"
						+ content;
		System.out.println(Arrays.toString(response));
		return response;
	}

	private String getLastModifiedHeader(String path) {
		File fileAtPath = new File(System.getProperty("user.dir"),path);
		SimpleDateFormat sdf=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		String header = sdf.format(fileAtPath.lastModified());
		return header;
	}

	private String[] handleIfModifiedSince(Map<String, String> parsedRequest) throws ParseException, WrongRequestException, IOException {
	    Date date1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(parsedRequest.get("If-Modified-Since")); 
	    File fileAtPath = new File(parsedRequest.get("path"));
	    SimpleDateFormat sdf=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		String fileModificationDate = sdf.format(fileAtPath.lastModified());
		Date date2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(fileModificationDate); 
		String[] response = {};
		if (date1.compareTo(date2)<0) {
			response = this.GET(parsedRequest);
		} else {
			//file hasn't changed, return a 304 code
			response[0] = "HTTP/1.1 304 Not Modified \r\n"
					+ this.getDateHeader() + "\r\n";
			response[1] = "\r\n";
		}
		return response;
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
    	System.out.println("commencing parser");
		Map<String, String> map = new HashMap<String, String>();
    	String currLine = reader.readLine();
    	String[] currCommand = currLine.split("\\s+");
    	if (currCommand.length!=3
    			|| !this.isValidMethod(currCommand[0])
    			|| !this.isValidVersion(currCommand[2])) throw new WrongRequestException("invalid Request Format");
    	map.put("type", currCommand[0]);
    	map.put("path", currCommand[1]);
    	map.put("version", currCommand[2]);
    	String t=reader.readLine();
    	while(!t.equals("")) {
    		String[] temp = t.split(":");
    		map.put(temp[0].trim(), temp[1].trim());
    		t = reader.readLine();
    	}
    	if (!map.keySet().contains("Host")) throw new WrongRequestException("invalid Request Format");
    	if (! (map.get("type").equals("GET")
    			|| map.get("type").equals("HEAD"))) {
    		String body = "";
        	System.out.println("test");
        	System.out.println(reader.readLine());
        	System.out.println("test");
        	while((t = reader.readLine()) != null) body+=t+"\r\n";
        	map.put("body", body);
    	}
    	System.out.println(map.toString());
    	return map;
    }
    
    private boolean isValidVersion(String version) {
    	String[] temp=version.split("/");	
    	return temp[0].equals("HTTP")
    			&& (temp[1].equals("1.1")||temp[1].equals("1.0"));
	}

	private boolean isValidPath(String path) {
    	File tmpDir = new File(System.getProperty("user.dir"),path);
    	return tmpDir.exists()
    			|| path=="/";
	}

	private boolean isValidMethod(String method) {
		return method.equals("GET")
				|| method.equals("HEAD")
				|| method.equals("POST")
				|| method.equals("PUT");
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
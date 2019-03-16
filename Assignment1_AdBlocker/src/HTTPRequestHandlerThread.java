import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

class HTTPRequestHandlerThread extends Thread {
	  Socket clientSocket;
	  int clientID = -1;
	  boolean running = true;

	  HTTPRequestHandlerThread(Socket s, int i) {
	    clientSocket = s;
	    clientID = i;
	  }

	  public void run() {
	    System.out.println("Accepted Client : ID - " + clientID + " : Address - "
	        + clientSocket.getInetAddress().getHostName());
        try {
            BufferedReader input  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream output = clientSocket.getOutputStream();
            long time = System.currentTimeMillis();
            //String message = (String) input.readObject();
            try {
				Map<String, String> parsedRequest = this.ParseRequestWithReader(input);
				//input.close();
				String[] response = {"",""}; //has 2 elements [0]=headers+response code | [1]=body
				//this.GETbytesIncludingResponse(parsedRequest,output);
				Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
				if (parsedRequest.keySet().contains("if-modified-since")) responseBytes=this.handleIfModifiedSinceBytes(parsedRequest);
				switch (parsedRequest.get("type")) {
					case "GET": responseBytes = this.GETbytes(parsedRequest);
							break;
					case "HEAD": responseBytes = this.HEADbytes(parsedRequest);
							break;
					case "PUT": responseBytes = this.PUTbytes(parsedRequest);
							break;
					case "POST": responseBytes = this.POSTbytes(parsedRequest);
							break;
					default: responseBytes.put("headers", "501 Not Implemented\r\n\r\n".getBytes());
							 responseBytes.put("body", null);
							break;
				}
				boolean closeSocket = true;
				//implement not closing on Connection: keep-alive
				if (parsedRequest.get("Connection").equals("Keep-Alive")) {
					closeSocket = false;
				} else {
					response[0]+="Connection: Closed";
				}
				output.write(responseBytes.get("headers"));
				byte[] body=responseBytes.get("body");
				if (!body.equals(null)) output.write(body);
				output.flush();
				output.close();
				
				//this.clientSocket.close();
				
				if (closeSocket) {
 	            	this.clientSocket.close();
 	            } else {
 	            	HTTPRequestHandlerThread cliThread = new HTTPRequestHandlerThread(clientSocket, clientID+1);
 	            	cliThread.start();
 	            }//*/
			} catch (WrongRequestException e) {
				System.out.println("invalid Request detected");
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
        } catch (IOException | ParseException /*| ClassNotFoundException*/ e) {
            //This is the 500 response code
        	
        }
	  }

		
		
		private Map<String, byte[]> POSTbytes(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			String path = parsedRequest.get("path");
			String fileName = this.determineNewfileName(path);
			String content = parsedRequest.get("body");
			Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
			String response;
			if (this.FileExists(fileName)) {
				this.appendToFile(fileName, content);
				response = "HTTP/1.1 200 OK \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			} else {
				this.createNewFile(fileName, content);
				response = "HTTP/1.1 201 Created \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			}
			response+="\r\n";
			responseBytes.put("headers", response.getBytes());
			responseBytes.put("body", null);
			return responseBytes;
		}
		
		private Map<String, byte[]> PUTbytes(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			String path = parsedRequest.get("path");
			String fileName = this.determineNewfileName(path);
			String content = parsedRequest.get("body");
			Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
			String response;
			if (this.FileExists(fileName)) {
				this.overWriteFile(fileName, content);
				response = "HTTP/1.1 200 OK \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			} else {
				this.createNewFile(fileName, content);
				response = "HTTP/1.1 201 Created \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			}
			response+="\r\n";
			responseBytes.put("headers", response.getBytes());
			responseBytes.put("body", null);
			return responseBytes;
		}
		
		private String determineNewfileName(String path) throws WrongRequestException {
			int indexOfLastSlash = path.lastIndexOf("/");
			try {
				String newFileName = path.substring(indexOfLastSlash,path.substring(indexOfLastSlash).indexOf(".")) + ".txt";
				return newFileName;
			} catch (Exception e) {
				throw new WrongRequestException("invalid Request Format");
			}
		}
		
		private boolean FileExists(String fileName) {
			return (new File(System.getProperty("user.dir"), fileName)).exists();
		}
		
		private void createNewFile(String newFileName, String content) throws IOException {
			File file = new File(System.getProperty("user.dir"), newFileName);
			if (!file.exists()) file.createNewFile();
			//to this file can then be written using any previously seen methods
			FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
			fr.write(content);
			fr.close();
		}
		
		private void appendToFile(String fileName, String content) throws IOException {
			File file = new File(System.getProperty("user.dir"), fileName);
			FileWriter fr = new FileWriter(file, true); //true means that if file exists that has to be overwritten
			fr.write(content);
			fr.close();
		}
		
		private void overWriteFile(String fileName, String content) throws IOException {
			File file = new File(System.getProperty("user.dir"), fileName);
			FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
			fr.write(content);
			fr.close();
		}
		
		private Map<String, byte[]> HEADbytes(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			String path = parsedRequest.get("path");
			if (!this.isValidPath(path)) throw new WrongRequestException("404 not found");
			Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
			StringBuilder contentBuilder = new StringBuilder();
			BufferedReader in = new BufferedReader(new FileReader(path));
		    String str;
		    while ((str = in.readLine()) != null) {
		        contentBuilder.append(str);
		    }
		    in.close();
			byte[] content = contentBuilder.toString().getBytes();
			String response = "HTTP/1.1 200 OK \r\n"
							+ this.getDateHeader() + "\r\n"
							+ "Last-Modified: "+ this.getLastModifiedHeader(path) +"\r\n"
							+ "Content-Length: " + content.length + "\r\n"
							+ "Content-Type: text/html \r\n"
							+ "\r\n";
			responseBytes.put("body", content);
			responseBytes.put("headers", response.getBytes());
			return responseBytes;
		}
		
		private Map<String, byte[]> GETbytes(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			//System.out.println("Starting GET");
			Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
			String path = parsedRequest.get("path");
			if (path.equals("/")) path="index.html";
			if (!this.isValidPath(path)) throw new WrongRequestException("404 not found");
			byte[] contentBytes;
			String extra;
			if (this.getExtension(path).equals("jpg")) { //A jpg is requested
				File file=new File(System.getProperty("user.dir"),path);
				contentBytes = Files.readAllBytes(file.toPath());
				extra = "Content-Type: image/jpeg\r\n"
										+ "\r\n";
			} else { //A text/html file is requested
				File file = new File(System.getProperty("user.dir"),path);
				contentBytes = Files.readAllBytes(file.toPath());
				extra = "Content-Type: text/html\r\n"
										+ "\r\n";
			}
			responseBytes.put("body",contentBytes);
			String response = "HTTP/1.1 200 OK\r\n"
							+ this.getDateHeader() + "\r\n"
							+ "Last-Modified: "+ this.getLastModifiedHeader(path) +"\r\n"
							+ "Content-Length: " + contentBytes.length + "\r\n"
							+ extra;
			
			//System.out.println(response);
			responseBytes.put("headers",response.getBytes());
			return responseBytes;
		}

		private String getExtension(String path) {
			int dotIndex = path.indexOf(".");
			return path.substring(dotIndex+1);
		}

		private String getLastModifiedHeader(String path) {
			File fileAtPath = new File(System.getProperty("user.dir"),path);
			SimpleDateFormat sdf=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			String header = sdf.format(fileAtPath.lastModified());
			return header;
		}
		
		private Map<String,byte[]> handleIfModifiedSinceBytes(Map<String, String> parsedRequest) throws ParseException, WrongRequestException, IOException {
		    Date date1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(parsedRequest.get("If-Modified-Since")); 
		    File fileAtPath = new File(parsedRequest.get("path"));
		    SimpleDateFormat sdf=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			String fileModificationDate = sdf.format(fileAtPath.lastModified());
			Date date2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(fileModificationDate); 
			Map<String,byte[]> responseBytes = new HashMap<String, byte[]>();
			if (date1.compareTo(date2)<0) {
				responseBytes = this.GETbytes(parsedRequest);
			} else {
				//file hasn't changed, return a 304 code
				String response = "HTTP/1.1 304 Not Modified \r\n"
											+ this.getDateHeader() + "\r\n"
											+ "\r\n";
				responseBytes.put("headers", response.getBytes());
				responseBytes.put("body",null);
			}
			return responseBytes;
		}

		private String getDateHeader() {
	        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	        format.setTimeZone(TimeZone.getTimeZone("GMT"));
	        String dateHeader = "Date: " + format.format(new Date()) + " GMT";

	        return dateHeader;
		}

		public Map<String,String> ParseRequestWithReader(BufferedReader reader) throws IOException, WrongRequestException{
	    	//System.out.println("commencing parser");
			Map<String, String> map = new HashMap<String, String>();
	    	String currLine = reader.readLine();
	    	//System.out.println(currLine);
	    	String[] currCommand = currLine.split("\\s+");
	    	if (currCommand.length!=3
	    			|| !this.isValidMethod(currCommand[0])
	    			|| !this.isValidVersion(currCommand[2])) throw new WrongRequestException("invalid Request Format");
	    	map.put("type", currCommand[0]);
	    	map.put("path", currCommand[1]);
	    	map.put("version", currCommand[2]);
	    	String t=reader.readLine();
	    	while(!t.equals("")) {
	    		//System.out.println(t);
	    		String[] temp = t.split(":");
	    		map.put(temp[0].trim(), temp[1].trim());
	    		t = reader.readLine();
	    	}
	    	if (!map.keySet().contains("Host")) throw new WrongRequestException("invalid Request Format");
	    	if (! (map.get("type").equals("GET")
	    			|| map.get("type").equals("HEAD"))) {
	    		String body = "";
	        	//System.out.println("test");
	        	//System.out.println(reader.readLine());
	        	//System.out.println("test");
	    		//System.out.println(map.get("Content-length"));
	    		int length = Integer.parseInt(map.get("Content-Length"));
	    		while(length>0)  {
	    		t=reader.readLine();
	    		body+=t+"\r\n";
	    		length-=(t.length()+2);
	    		//System.out.println(length);
	    		}
	        	map.put("body", body);
	    	}
	    	System.out.println(map.toString());
	    	return map;
	    }
	    
	    private boolean isValidVersion(String version) {
	    	return version.equals("HTTP/1.1")
	    			|| version.equals("HTTP/1.1");
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
		
		/*
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
	    
	    private void GETbytesIncludingResponse(Map<String, String> parsedRequest, OutputStream output) throws IOException {
			OutputStream os = clientSocket.getOutputStream();
			File file=new File(System.getProperty("user.dir"),"/tempfile.txt");
			byte[] fileContent = Files.readAllBytes(file.toPath());
			String responseHeader = "HTTP/1.1 200 OK \r\n"; 
			String extra = "Content-Length: " + fileContent.length + "\r\n" + 
					"Content-Type: text/html \r\n";
			//ObjectOutputStream output = new ObjectOutputStream(os);
			//output.write(arg0);
			responseHeader+=extra;
			os.write((responseHeader+"\r\n").getBytes());
			os.write(fileContent);
			os.flush();
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
						+ "Content-Location: " + fileName + "\r\n";
			} else {
				this.createNewFile(fileName, content);
				response[0] = "HTTP/1.1 201 Created \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			}
			response[1] = "\r\n";
			return response;
		}
	    
	    private void GETbytesIncludingResponse(Map<String, String> parsedRequest, OutputStream output) throws IOException {
			OutputStream os = clientSocket.getOutputStream();
			File file=new File(System.getProperty("user.dir"),"/tempfile.txt");
			byte[] fileContent = Files.readAllBytes(file.toPath());
			String responseHeader = "HTTP/1.1 200 OK \r\n"; 
			String extra = "Content-Length: " + fileContent.length + "\r\n" + 
					"Content-Type: text/html \r\n";
			//ObjectOutputStream output = new ObjectOutputStream(os);
			//output.write(arg0);
			responseHeader+=extra;
			os.write((responseHeader+"\r\n").getBytes());
			os.write(fileContent);
			os.flush();
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
						+ "Content-Location: " + fileName + "\r\n";
			} else {
				this.createNewFile(fileName, content);
				response[0] = "HTTP/1.1 201 Created \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			}
			response[1] = "\r\n";
			return response;
		}
		
		
		 * creates a new text file or replaces a representation of the target resource with the request payload
		 * @param parsedRequest
		 * @return
		 * @throws WrongRequestException
		 * @throws IOException 
		
		private String[] PUT(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			String path = parsedRequest.get("path");
			String fileName = this.determineNewfileName(path);
			String content = parsedRequest.get("body");
			String[] response = {"",""};
			if (this.FileExists(fileName)) {
				this.overWriteFile(fileName, content);
				response[0] = "HTTP/1.1 200 OK \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			} else {
				this.createNewFile(fileName, content);
				response[0] = "HTTP/1.1 201 Created \r\n"
						+ this.getDateHeader() + "\r\n"
						+ "Content-Location: " + fileName + "\r\n";
			}
			response[1] = "\r\n";
			return response;
		}
	    
	    
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
		
		private String[] GET(Map<String, String> parsedRequest) throws WrongRequestException, IOException {
			//System.out.println("Starting GET");
			String path = parsedRequest.get("path");
			if (path.equals("/")) path="index.html";
			if (!this.isValidPath(path)) throw new WrongRequestException("404 not found");
			String[] response = {"",""};
			//System.out.println(path);
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
			//System.out.println(Arrays.toString(response));
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
	    
	    **/
	}

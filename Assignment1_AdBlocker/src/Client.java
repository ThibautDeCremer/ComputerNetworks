/**
 * Client program for a HTTP client-server application, this program has primitive ad blocker functionalities.
 * 
 * @author Thibaut De Cremer and Niels Verdijck
 */

import java.net.*;
import java.util.Scanner;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class Client
{
	/**
	 * Main running program of client.
	 * 
	 * @param args
	 * 		Array with input. The input array should at least contain 2 elements, it may at most contain 3 elements.
	 * 		- First element in array is the HTTP command (GET, HEAD, PUT or POST).
	 * 		- Second element in array is the host name of the server (+path).
	 * 		- Third element in array is the number of the port. If no port is specified, default port 80 is used.
	 * 
	 * @throws Exception
	 * 		When an I/O error occurs when storing the incoming HTML file.
	 */
	public static void main(String[] args) throws Exception
	{
		if ((args.length < 2) || (args.length > 3)) // length of the command should be 2 or 3: command + host(and path) (+port)
		{
			System.out.println("Not a valid command");
			System.out.println("Done");
			return;
		}
		
		int port = 80; // default port
		
		if (args.length == 3) // extract the port from the input argument
		{
			try
			{
				port = Integer.parseInt(args[2],10);
			}
			catch (NumberFormatException e) // if the third part of the input array is not a number, a NumberFormatException is thrown.
			{
				System.out.println("The port should be the third part of the input string");
				System.out.println("Done");
				return;
			}
		}
		
		/**
		 * part of the method that deals with HEAD requests.
		 */
		if (args[0].equals("HEAD"))
		{
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
			
			Socket sock = new Socket(InetAddress.getByName(host),port); // open socket
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			pw.println("HEAD " + path +" HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
			pw.println("Host: " + host);
			pw.println("Connection: Close"); // close connection after making request
			pw.println(); // always end with blank line
			InputStream inSt = sock.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inSt));
			String incoming = "";
			String r = br.readLine() + "\r\n";
			incoming += r;
			
			while(! r.equals("")) // loop until first blank line, that is the end of the header
			{
				r = br.readLine();
				incoming += r + "\r\n";
			}
			
			System.out.println(incoming);
			br.close();
			sock.close();
			System.out.println("Done");
		}
		
		/**
		 * Part of the method that deals with GET requests. (WORKS)
		 */
		else if (args[0].equals("GET"))
		{
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
			
			Socket sock = new Socket(InetAddress.getByName(host),port); // open socket
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			String req = "GET " + path +" HTTP/1.1\r\n";
			req += "Host: " + host + "\r\n";
			req += "User-Agent: niels\r\n";
			req += "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n";
			req += "Accept-Language: nl,en-US;q=0.7,en;q=0.3\r\n";
			req += "Connection: keep-alive\r\n";
			req += "Upgrade-Insecure-Requests: 1\r\n"; // send the entire header of the request in one go (speed up of request)
			pw.println(req);
			
			InputStream inSt = sock.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inSt));
			String header = "";
			String r = br.readLine() + "\r\n";
			header += r;
			
			while(!r.equals("")) // parse header to know how the content is transfered (chunked or content-length)
			{
				r = br.readLine();
				header+= r + "\r\n";
			}
			
			System.out.println(header);
			String html = "";
			
			/**
			 * GET requests that sends its data with content-length
			 */
			if (header.contains("Content-Length"))
			{
				String ConLen = "Content-Length";
				int cijfer = header.indexOf(ConLen);
				int endIn = header.indexOf("\r\n", cijfer);
				int leng = Integer.parseInt(header.substring(cijfer+(ConLen.length())+2,endIn));
				
				char[] cbuf = new char[leng];
				br.read(cbuf, 0, leng); // iterative method that reads until all characters are read.
				html = new String(cbuf);
			}
			
			/**
			 * GET request that sends its data in chunks.
			 */
			else if (header.contains("chunked"))
			{
				String h = br.readLine();
				int counter = Integer.parseInt(h,16);
				Boolean fullTransfer = false; // check whether or not there has been a full transfer of data.
				char[] cbuf = new char[1] ;
				while(!fullTransfer)
				//while (counter > 0)
				{
					/**
					 * If I run this code (in comments), it doesn't work.
					 * But it works while debugging with a breakpoint before char[] cbuf = new ...
					 */
					/*char[] cbuf = new char[counter];
					br.read(cbuf, 0, counter);
					String t = new String(cbuf);
					html += t;
					h = br.readLine(); // empty line
					h = br.readLine();
					try
					{
						counter = Integer.parseInt(h,16);
					}
					catch (NumberFormatException e)
					{
						h = br.readLine();
						counter = Integer.parseInt(h,16);
					}*/
					while(counter > 0)
					{
						br.read(cbuf, 0, 1);
						System.out.print(Character.toString(cbuf[0]));
						html += Character.toString(cbuf[0]);
						counter --;
					}
					
					String k = br.readLine();
					k = br.readLine();
					System.out.println(k);
					counter = Integer.parseInt(k,16);
					if (counter == 0)
					{
						fullTransfer = true;
						html += "\r\n";
					}
				}
			}
			
			else // we only accept transfer of data specified with content-Length or chunked transfer
			{
				System.out.println("Incoming content not with content-length or chunked");
				System.out.println("Done");
			}
									
			if (scanImages(html))
			{
				html = processImages(html, host, path, port, sock); // invoke method that deals with all the images.
			}
			
			FileWriter fw = new FileWriter("AdBlock.html"); // write html to locally stored file named AdBlock.html
			BufferedWriter bw = new BufferedWriter(fw);
			try
			{
				bw.write(html);
				bw.newLine();
				bw.close();
			}
			catch (Exception e)
			{
				sock.close();
				throw e;
			}
			
			System.out.println(html);
			br.close();
			sock.close();
			System.out.println("Done"); // needed to see whether or not the program is still running
			return;
		}
		
		else if ((args[0].equals("PUT")) || (args[0].equals("POST")))
		{
			String host = null;
			String path = "/newFile.txt";
			
			if (args[1].contains("/")) // if-else statement to retrieve the host and path
			{
				int index = args[1].indexOf("/");
				int l = args[1].length();
				
				host = args[1].substring(0, index);
				path = args[1].substring(index, l);
				if(path.substring(path.length()-4, path.length()) != ".txt")
					path += ".txt";
			}
			
			else
				host = args[1];
			
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
			wr.write(write+"\r\n");
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
		
		else // command given does not use HTTP commands from assignment
		{
			System.out.println("Invalid command. Please use: GET, HEAD, PUT or POST");
			System.out.println("Done");
			return;
		}
	}
	
	/**
	 * Method to scan the HTML file for image files. Method scans for images using string scanner.
	 * 
	 * @param t
	 * 		HTML string to check for image files.
	 * 
	 * @return
	 * 		Returns whether or not the given string has at least one occurrence of "<img ".
	 */
	private static boolean scanImages(String t)
	{		
		if (t == null)
			return false;

		else
			return t.contains("<img ");
	}
	
	/**
	 * Method that searches the first index of a byte array in the larger byte array.
	 * 
	 * @param byteArray
	 * 		Byte array to search in.
	 * @param ba
	 * 		Byte array to search in the larger byte array, length must be at least 2.
	 * 
	 * @return
	 * 		Index of the first occurrence. -1 is returned if no such index can be found or the length of ba is smaller than 2 or bigger than byteArray.
	 */
	private static int byteSearch(byte[] byteArray, byte[] ba)
	{
		int L = byteArray.length;
		int l = ba.length;
		
		if ((l < 2) || (L < l)) // The length of byteArray must not be smaller than the length of ba. For simplicity, the length of ba should be at least 2 (always true for this client)
			return -1;
		
		for(int i=0; i < (L-l+1); i++)
		{
			if(byteArray[i] == ba[0]) // search for first hit
			{
				for(int j=1;j<l;j++)
				{
					if((byteArray[i+j] == ba[j]) && (j == l-1)) // if first occurrence is found, return the index
						return i;
					else if(byteArray[i+j] != ba[j])
						break;
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Method to deal with  all images in the HTML file, retrieve them and store them locally. This method also implements the ad block.
	 * 
	 * @param allImages
	 * 		String containing all HTML lines that reference images.
	 * 
	 * @return
	 * 		Modified HTMl string if the original contained advertisements.
	 * 
	 * @throws Exception
	 * 		If no header is found in the incoming byte array, an exception is thrown
	 */
	private static String processImages(String allImages, String host, String path, int port, Socket socket) throws Exception
	{
		InputStream is = socket.getInputStream();
		PrintWriter pr = new PrintWriter(socket.getOutputStream(),true);

		int index = allImages.indexOf("<img ", 0); // function returns -1 if no index can be found
		
		while(index != -1)
		{
			int beginIndex = allImages.indexOf("src=", index)+5; // index where image reference starts
			int endIndex = allImages.indexOf("\"", beginIndex); // index where image reference ends
			String image = allImages.substring(beginIndex, endIndex);
			if (image.contains("ad")) // if the found embedded image is an add, replace it with something else.
			{
				image = "ReplacementPicture.png"; // this picture is stored locally
				allImages = allImages.replace(allImages.substring(beginIndex, endIndex),image); // insert replacement picture where advertisement is in HTML string
			}
			
			/**
			 * Not needed in the HTML file of webpage. These lines of code will retrieve an image from another host.
			 */
			if (image.substring(0, 4) == "http")
			{
				String im = image.substring(7,image.length()); // http://.........
				int ind = im.indexOf("/");
				host = im.substring(0, ind);
				image = im.substring(ind,im.length());
			}

			if (image != "ReplacementPicture.png") // if this image is not an advertisement, retrieve it.
			{
				String r = "GET " + path + image + " HTTP/1.1\r\n";
				r += "Host: " + host + "\r\n";
				r += "User-Agent: niels\r\n";
				r += "Accept: image/webp,*/*\r\n";
				r += "Accept-Language: nl,en-US;q=0.7,en;q=0.3\r\n";
				r += "Accept-Encoding: gzip, deflate\r\n";
				r += "Referer: http://" + host+path + "\r\n";
				r += "Connection: keep-alive\r\n";
				pr.println(r); // send request in one go the speed up the request a bit.
				
				byte[] endOfHeader = {13,10,13,10}; // equals \r\n in bytes -> indicates end of header
				int c = 0;
				byte[] b = new byte[1]; // byte buffer for the next incoming byte
				byte[] he = new byte[0]; // byte array storing the header
				
				while(c!=-1) // first try to parse the header to know how big the image is
				{
					c = is.read(b, 0, 1); // read a byte at a time
					
					if (c != -1)
					{
						byte[] ba = new byte[he.length+b.length];
						System.arraycopy(he, 0, ba, 0, he.length);
						System.arraycopy(b, 0, ba, he.length, b.length);
						he = ba;
						
						if (byteSearch(he,endOfHeader) != -1)
							c = -1;
					}
				}
				
				String headr = new String(he, 0, he.length); // decode header from bytes to string
				String ConLen = "Content-Length";
				int cijfer = headr.indexOf(ConLen);
				int endIn = headr.indexOf("\r\n", cijfer);
				int leng = Integer.parseInt(headr.substring(cijfer+(ConLen.length())+2,endIn)); // stores the length of the image
				
				int f = 0;
				int che = 0; // check for the amount of read bytes of the image
				byte[] tf = new byte[leng]; // byte buffer for the next bytes of the image
				byte[] im = new byte[leng]; // byte array containing the image
				while ((che < leng) && (f != -1)) // loop until whole image is retrieved
				{
					f = is.read(tf, 0, leng-che);
					
					if (f != -1)
					{
						System.arraycopy(tf, 0, im, che, f);
						che += f;
					}
				}
				
				ByteArrayInputStream ins = new ByteArrayInputStream(im);
				BufferedImage bi = ImageIO.read(ins); // convert byte array to buffered image, needed for next line
				ImageIO.write(bi, "jpg", new File(image)); // store image locally
			}
			index = allImages.indexOf("<img ", endIndex); // search for the next image in the html file
		}
		
		return allImages;
	}
}

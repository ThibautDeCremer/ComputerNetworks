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

/**
 * TODO:
 * 	- GET method for images -> created BufferedImage is equals null -> NullPointerException (but the code is fine, the bytes are not)
 * 	- I have not yet tested the PUT and POST functions (how to make request to server?)
 */

public class Client
{
	/**
	 * Main running program of client.
	 * 
	 * @param args
	 * 		Array with input. The input array should at least contain 2 elements.
	 * 		- First element in array is the HTTP command
	 * 		- Second element in array is the host name of the server
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		if ((args.length < 2) || (args.length > 3)) // length of the command should be 2 or 3: command and host(+path) (+port)
		{
			System.out.println("Not a valid command");
			System.out.println("Done");
			return;
		}
		
		int port = 80;
		
		if (args.length == 3)
		{
			try
			{
				port = Integer.parseInt(args[2],10);
			}
			catch (NumberFormatException e)
			{
				System.out.println("The port should be the 3 part of the input string");
				System.out.println("Done");
				return;
			}
		}
		
		/**
		 * part of the method that deals with HEAD requests. (WORKS)
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
			
			Socket sock = new Socket(InetAddress.getByName(host),port); // open socket with default port 80
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
			
			while(! r.equals(""))
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
			
			Socket sock = new Socket(InetAddress.getByName(host),port); // open socket with default port 80
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			String req = "GET " + path +" HTTP/1.1\r\n";
			req += "Host: " + host + "\r\n";
			req += "User-Agent: niels\r\n";
			req += "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n";
			req += "Accept-Language: nl,en-US;q=0.7,en;q=0.3\r\n";
			req += "Connection: keep-alive\r\n";
			req += "Upgrade-Insecure-Requests: 1\r\n";
			pw.println(req);
			
			InputStream inSt = sock.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inSt));
			String header = "";
			String r = br.readLine() + "\r\n";
			header += r;
			
			while(!r.equals("")) // parse header
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
				br.read(cbuf, 0, leng);
				html = new String(cbuf);
			}
			
			/**
			 * GET request that sends its data in chunks.
			 */
			else if (header.contains("chunked"))
			{
				String h = br.readLine();
				int counter = Integer.parseInt(h,16);
				//Boolean fullTransfer = false; // check whether or not there has been a full transfer of data.
				//char[] cbuf = new char[1] ;
				//while(!fullTransfer)
				while (counter > 0)
				{
					/**
					 * If I run this code, it doesn't work.
					 * But it works while debugging with a breakpoint before char[] cbuf = new ...
					 */
					char[] cbuf = new char[counter];
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
					}
					/*while(counter > 0)
					{
						//String s = br.readLine();
						br.read(cbuf, 0, 1);
						System.out.print(Character.toString(cbuf[0]));
						html += Character.toString(cbuf[0]);
						counter --;
						//System.out.println(counter);
						//if (counter > 0)
							//html += "\r\n";
						//System.out.println(html.length());
					}
					
					String k = br.readLine();
					k = br.readLine();
					System.out.println(k);
					counter = Integer.parseInt(k,16);
					if (counter == 0)
					{
						fullTransfer = true;
						html += "\r\n";
					}*/
				}
			}
			
			else // we only accept transfer of data specified with content-Length or chunked transfer
			{
				System.out.println("Incoming content not with content-length or chunked");
				System.out.println("Done");
			}
									
			if (scanImages(html))
			{
				html = processImages(html, host, path, port, sock);
			}
			
			FileWriter fw = new FileWriter("AdBlock.html"); // write to file
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
		
		else if ((args[0].equals("PUT")) || (args[0].equals("POST"))) // TODO: voeg content-length toe aan upload
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
			
			Socket sock = new Socket(InetAddress.getByName(host),6111);
			BufferedWriter wr = new BufferedWriter( new OutputStreamWriter(sock.getOutputStream(),"UTF8"));
			Boolean bool = true;
			while (bool)
			{
				System.out.println("Enter something to upload in server: ");
				Scanner scanner = new Scanner(System.in);
				String input = scanner.nextLine();
				String write = URLEncoder.encode(input,"UTF-8"); // www works with UTF-8
				wr.write(args[0] + " " + path + "HTTP/1.1");
				wr.write("Host: " + host);
				wr.write("Content-Type: text/html"); //application/x-www-form-urlencoded\r\n
				wr.write("Content-Length: " + write.length() + "\r\n");
				wr.write("\r\n");
				wr.write(write);
				wr.flush();
				
				BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String t;
				
				while(((t = br.readLine()) != null))
				{
					System.out.println(t);
				}
				
				br.close();
				scanner.close();
				
				if (input.equals(""))
					bool = false;
			}
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
	 * Method to deal with  all images in the HTML file. This method also implements the ad block.
	 * 
	 * @param allImages
	 * 		String containing all HTML lines that reference images.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private static String processImages(String allImages, String host, String path, int port, Socket socket) throws UnknownHostException, IOException
	{
		//Socket s = new Socket(InetAddress.getByName(host),port);
		InputStream is = socket.getInputStream();
		BufferedReader bw = new BufferedReader(new InputStreamReader(is));
		PrintWriter pr = new PrintWriter(socket.getOutputStream(),true);

		int index = allImages.indexOf("<img ", 0); // function returns -1 if no index can be found
		
		while(index != -1)
		{
			int beginIndex = allImages.indexOf("src=", index)+5; // index where image reference starts
			int endIndex = allImages.indexOf("\"", beginIndex); // index where image reference ends
			String image = allImages.substring(beginIndex, endIndex);
			if (image.contains("ad")) // if the found embedded image is an add, replace it with something else.
			{
				image = "ReplacementPicture.png";
				allImages = allImages.replace(allImages.substring(beginIndex, endIndex),image);
			}
			// GET operation to retrieve the image and store it locally -> perhaps use same function as in the main
			
			/**
			 * Not needed in the html file of webpage.
			 */
			if (image.substring(0, 4) == "http")
			{
				String im = image.substring(7,image.length()); // http://.........
				int ind = im.indexOf("/");
				host = im.substring(0, ind);
				image = im.substring(ind,im.length());
			}
			//System.out.println("TEST");

			if (image != "ReplacementPicture.png")
			{
				String r = "GET " + path + image + " HTTP/1.1\r\n";
				r += "Host: " + host + "\r\n";
				r += "User-Agent: niels\r\n";
				r += "Accept: image/webp,*/*\r\n";
				r += "Accept-Language: nl,en-US;q=0.7,en;q=0.3\r\n";
				r += "Accept-Encoding: gzip, deflate\r\n";
				r += "Referer: http://" + host+path + "\r\n";
				r += "Connection: keep-alive\r\n";
				pr.println(r);
				
				String h = bw.readLine(); // parsing the header of the responds WORKS.
				byte[] he = h.getBytes();
				int count = he.length;
				String header = h + "\r\n";
				while(!h.equals("")) // parse header
				{
					h = bw.readLine();
					he = h.getBytes();
					count += he.length;
					header += h + "\r\n";
				}
								
				String ConLen = "Content-Length";
				int cijfer = header.indexOf(ConLen);
				int endIn = header.indexOf("\r\n", cijfer);
				int leng = Integer.parseInt(header.substring(cijfer+(ConLen.length())+2,endIn));
				byte[] im = new byte[leng];
				is.read(im,0,leng);
				ByteArrayInputStream ins = new ByteArrayInputStream(im); // TODO: figure out why this won't work with our images, most likely are the bytes in im not from the actual image
				BufferedImage bi = ImageIO.read(ins);
				ImageIO.write(bi, "jpg", new File(image));
				
				/*BufferedImage im = ImageIO.read(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(im, "jpg", baos);
				baos.flush();
				byte[] ba = baos.toByteArray();
				System.out.println(ba);
				baos.close();
				
				InputStream is2 = new ByteArrayInputStream(ba);
				BufferedImage bi = ImageIO.read(is2);
				ImageIO.write(bi, "jpg", new File(image));*/
				
				/*DataInputStream in = new DataInputStream(socket.getInputStream());
				byte[] b = new byte[1];
				int check = is.read(b); // read one byte at a time.
				byte[] im = new byte[0];
				
				while (check == 1) // check should be -1 when the transfer is finished
				{
					byte[] inRes = new byte[im.length+1];
					System.arraycopy(im, 0, inRes, 0, im.length);
					System.arraycopy(b, 0, inRes, im.length, b.length);
					im = inRes;
					check = in.read(b);
				}*/
				
				/**
				 * alternative 1 (works, but not to good).
				 */
				/*InputStream is = s.getInputStream(); // TODO: as long as the image is not to large, this works hopefully
				byte[] im = is.readAllBytes();*/
				
				/**
				 * Alternative 2 (buffered image is null -> exception)
				 */
				/*File newIm = new File(image);
				InputStream is = new ByteArrayInputStream(im);
				Image i = ImageIO.read(is);
				ImageIO.write((RenderedImage) i, "jpg", newIm);*/
			}
			
			index = allImages.indexOf("<img ", endIndex);
		}
		
		bw.close();
		//s.close();
		return allImages;
	}
}

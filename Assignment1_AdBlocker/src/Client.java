/**
 * Client program for a HTTP client-server application, this program has ad blocker functionalities.
 * 
 * @author Thibaut De Cremer and Niels Verdijck
 */

import java.net.*;
import java.util.Scanner;
import java.io.*;

/**
 * TODO:
 * 	- GET image in processImages ok?
 * 	- store HTML locally and store found image files locally -> how?
 * 	- I have not yet tested the PUT and POST functions
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
		if (args.length != 2) // length of the command should be 2: command and host(+path)
		{
			System.out.println("Not a valid command");
			System.out.println("Done");
			return;
		}
		
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
			
			Socket sock = new Socket(InetAddress.getByName(host),80); // open socket with default port 80
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			pw.println("HEAD " + path +" HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
			pw.println("Host: " + host);
			pw.println("Connection: Close"); // close connection after making request
			pw.println(); // always end with blank line
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
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
			
			Socket sock = new Socket(InetAddress.getByName(host),80); // open socket with default port 80
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			pw.println("GET " + path +" HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
			pw.println("Host: " + host);
			pw.println("Connection: Keep-Alive"); // keep connection alive after making request
			pw.println(); // always end with blank line
			
			/*// test
			InputStream so = sock.getInputStream();
			byte[] b = new byte[4000];
			int i = so.read(b);
			// test*/
			
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
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
			
			if (header.contains("Content-Length"))
			{
				String ConLen = "Content-Length";
				int cijfer = header.indexOf(ConLen);
				int endIn = header.indexOf("\r\n", cijfer);
				int leng = Integer.parseInt(header.substring(cijfer+(ConLen.length())+2,endIn));
				int test = 0;
				
				// keep reading until all content is retrieved (Content-Length is given)
				while (test < leng)
				{
					String t = br.readLine();
					
					if (t == null) // TODO: check why final print of test does not equal content-length, last <html> tag is printed (de-cremer.be/webpage/)
						break; // test with example.com not successful, test is at most 1220 in stead of 1270
					
					test += t.length();
					System.out.println(test);
					html = html + t + "\r\n";
					//leng -= (t.length()+2); // \r\n is only 2 characters
				}
				System.out.println(test);
			}
			
			else if (header.contains("chunked"))
			{
				/*String h = br.readLine(); // NOT GOOD, to simple
				html += h + "\r\n";
				System.out.println(html.length());
				System.out.println(h);
				
				while(h != "0")
				{
					h = br.readLine();
					html += h + "\r\n";
					System.out.println(html.length());
					System.out.println(h);
				}*/
				
				
				String h = br.readLine();
				System.out.println(h);
				int counter = Integer.parseInt(h,16);
				System.out.println(counter);
				Boolean fullTransfer = false;
				
				while(!fullTransfer)
				{
					while(counter >= -1)
					{
						String s = br.readLine();
						System.out.println(s);
						html += s;
						counter -= (s.length()+2);
						System.out.println(counter);
						if (counter > 0)
							html += "\r\n";
						System.out.println(html.length());
					}
					
					String k = br.readLine();
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
			
			/*while(! connectionLength)
			{
				pw.println(args[0] +  " / HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
				pw.println("Host: " + args[1]);
				String r;
				r = br.readLine();
				incoming = incoming + r + "\r\n";
								
				while(r.equals("")) // parse header
				{
					r = br.readLine();
					incoming+= r + "\r\n";
					if ((r == null)|| r.contains("Content-Length"))
						connectionLength = true;
				}
				
				if (incoming.contains("Content-Length") || (r == null))
					connectionLength = true;

			}*/
			
			if (scanImages(html))
			{
				html = processImages(html, host, path, sock);
			}
			
			FileWriter fw = new FileWriter("AdBlock.html");
			BufferedWriter bw = new BufferedWriter(fw);
			try
			{
				bw.write(html);
				bw.newLine();
				bw.close();
			}
			catch (Exception e)
			{
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
			
			Socket sock = new Socket(InetAddress.getByName(host),80);
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
	private static String processImages(String allImages, String host, String path, Socket socket) throws UnknownHostException, IOException
	{
		PrintWriter pr = new PrintWriter(socket.getOutputStream(),true);

		int index = allImages.indexOf("<img ", 0); // function returns -1 if no index can be found
		
		while(index != -1)
		{
			int beginIndex = allImages.indexOf("src=", index)+5; // index where image reference starts
			int endIndex = allImages.indexOf("\"", beginIndex); // index where image reference ends
			String image = allImages.substring(beginIndex, endIndex);
			if (image.contains("ad")) // if the found embedded image is an add, replace it with something else.
			{
				image = "ReplacementPicture.png"; //TODO: find how to get this picture
				allImages = allImages.replace(allImages.substring(beginIndex, endIndex),image);
			}
			// GET operation to retrieve the image and store it locally -> perhaps use same function as in the main
			
			if (image.substring(0, 4) == "http")
			{
				String im = image.substring(7,image.length()); // http://.........
				int ind = im.indexOf("/");
				host = im.substring(0, ind);
				image = im.substring(ind,im.length());
			}
			System.out.println("TEST");

			pr.println("GET " + path + "/" + image + " HTTP/1.1");
			pr.println("Host: " + host);
			pr.println("Connection: Keep-Alive");
			pr.println();
			
			DataInputStream in = new DataInputStream(socket.getInputStream());
			int len = in.readInt(); // length of the incoming image
			byte[] im = new byte[len];
			if (len > 0)
			{
				in.readFully(im,0,im.length); // read the entire image
			}
			try (FileOutputStream fos = new FileOutputStream("pathname")) // TODO: replace pathname
			{
				 fos.write(im);
			}
			// not allowed : BufferedImage img = ImageIO.read(socket.getInputStream());
			
			index = allImages.indexOf("<img ", endIndex);
		}
		
		return allImages;
	}
}

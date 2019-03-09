/**
 * Client program for a HTTP client-server application, this program has ad blocker functionalities.
 * 
 * @author Thibaut De Cremer and Niels Verdijck
 */

import java.net.*;
import java.util.Scanner;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * to do:
 * 	- GET image in processImages ok?
 * 	- store HTML locally and store found image files locally -> how?
 * 	- is ad blocker correct? (the implementation is a bit naive)
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
		if (args.length != 2) // length of the command should be 2: command and host
		{
			System.out.println("Not a valid command");
			System.out.println("Done");
			return;
		}
		
		if ((args[0].equals("GET")) || (args[0].equals("HEAD")))
		{
			Socket sock = new Socket(InetAddress.getByName(args[1]),80); // open socket with default port 80
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			pw.println(args[0] +  " / HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
			pw.println("Host: " + args[1]);
			pw.println("Connection: Close"); // close connection after making request
			pw.println(); // always end with blank line
			Boolean connectionLength = false;
			String imagefiles = "";
			String incomming = br.readLine() + "\r\n";
			if (scanImages(incomming))
				imagefiles = imagefiles + incomming + "\r\n";
			while(! connectionLength)
			{
				pw.println(args[0] +  " / HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
				pw.println("Host: " + args[1]);
				String r;
				r = br.readLine();
				incomming = incomming + r + "\r\n";
				if (r.contains("Content-Length") || (r == null))
					connectionLength = true;
				while(r != null)
				{
					r = br.readLine();
					incomming+= r + "\r\n";
					if ((r == null)|| r.contains("Content-Length"))
						connectionLength = true;

				}
			}
			incomming = incomming.replace("null", "");
			processImages(imagefiles);
			System.out.println(incomming);
			br.close();
			sock.close();
			System.out.println("Done"); // needed to see whether or not the program is still running
			return;
		}
		
		else if ((args[0].equals("PUT")) || (args[0].equals("POST")))
		{
			Socket sock = new Socket(InetAddress.getByName(args[1]),80);
			BufferedWriter wr = new BufferedWriter( new OutputStreamWriter(sock.getOutputStream(),"UTF8"));
			Boolean bool = true;
			while (bool)
			{
				System.out.println("Enter something to upload in server: ");
				Scanner scanner = new Scanner(System.in);
				String input = scanner.nextLine();
				String write = URLEncoder.encode(input,"UTF-8");
				wr.write(args[0] + " / HTTP/1.1\r\n");
				wr.write("Content-Length: " + input.length());
				wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
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
	private static void processImages(String allImages) throws UnknownHostException, IOException
	{
		int index = allImages.indexOf("<img ", 0); // function returns -1 if no index can be found
		
		while(index != -1)
		{
			int beginIndex = allImages.indexOf("src=", index)+5; // index where image reference starts
			int endIndex = allImages.indexOf("\"", beginIndex); // index where image reference ends
			String image = allImages.substring(beginIndex, endIndex);
			if (image.contains("ad")) // if the found embedded image is an add, replace it with something else.
				image = "ReplacementPicture.png";
			// GET operation to retrieve the image and store it locally -> perhaps use same function as in the main
			Socket s = new Socket(InetAddress.getByName(image),80);
			PrintWriter pr = new PrintWriter(s.getOutputStream(),true);
			pr.println("GET / HTTP/1.1");
			pr.println("Host: " + image);
			pr.println("Connection: Close");
			pr.println();
			BufferedImage img = ImageIO.read(s.getInputStream()); // now need to store this
			s.close();
			
			index = allImages.indexOf("<img ", endIndex);
		}
	}
}

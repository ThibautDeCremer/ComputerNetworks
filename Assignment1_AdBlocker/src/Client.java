/**
 * Client program for a HTTP client-server application.
 * 
 * @author Thibaut De Cremer and Niels Verdijck
 */

import java.net.*;
import java.util.Scanner;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;

import java.io.*;

/**
 * to do:
 * 	- store HTML locally -> how?
 *	- scan HTML file and check for embedded objects
 *		-> use GET command for found embedded objects (only retrieve image files, store them locally) 
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
			String incomming = "";
			Socket sock = new Socket(InetAddress.getByName(args[1]),80); // open socket with default port 80
			PrintWriter pw = new PrintWriter(sock.getOutputStream(),true);
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			pw.println(args[0] +  " / HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
			pw.println("Host: " + args[1]);
			pw.println("Connection: Close"); // close connection after making request
			pw.println(); // always end with blank line
			Boolean connectionLength = false;
			String t = br.readLine();
			while(! connectionLength)
			{
				pw.println(args[0] +  " / HTTP/1.1"); // as specified in the assignment, client program should support HTTP version 1.1
				pw.println("Host: " + args[1]);
				String r;
				r = br.readLine();
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
			System.out.println(incomming);
//			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//			String t;
//			while((t = br.readLine()) != null)
//			{
//				System.out.println(t);
//				Attribute[] images = scanImages(t); // HELP
//			}
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
				Scanner sc = new Scanner(System.in);
				String input = sc.nextLine();
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
	 * Method to scan the HTML file for image files. Image files have following HTML attributes: ismap, poster, usemap
	 */
	private static Attribute[] scanImages(String t)
	{
		Attribute[] result = null;

		Attribute[] allAttributes = HTML.getAllAttributeKeys();
		return result;
	}
}

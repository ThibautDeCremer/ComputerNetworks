import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class TEST
{
	static String r = "4\r\nWiki\r\n5\r\npedia\r\nE\r\n in\r\n\r\nchunks.\r\n0\r\n\r\n";
	static BufferedReader re = new BufferedReader(new StringReader(r));
	
	public static void main(String args[]) throws IOException
	{
		String h = re.readLine();
		System.out.println(h);
		int counter = Integer.parseInt(h,16);
		System.out.println(counter);
		Boolean fullTransfer = false;
		String html = "";
	
		while(!fullTransfer)
		{
			while(counter >= 0)
			{
				String s = re.readLine();
				System.out.println(s);
				html += s;
				counter -= (s.length()+2);
				System.out.println(counter);
				if (counter > 0)
					html += "\r\n";
				System.out.println(html.length());
			}
		
			String k = re.readLine();
			System.out.println(k);
			counter = Integer.parseInt(k,16);
			if (counter == 0)
			{
				fullTransfer = true;
				html += "\r\n";
			}
		}
		System.out.println(r.getBytes().length);
		System.out.println(r.length());
		System.out.println(html);
	}
}

import java.io.*;

public class TestCase {

	public static void main(String[] args) throws IOException {
		File file = new File(System.getProperty("user.dir"), "tempfile.txt");
		if (!file.exists()) file.createNewFile();
		//to this file can then be written using any previously seen methods
		FileWriter fr = new FileWriter(file, false); //false means that if file exists that has to be overwritten
		fr.write("data");
		fr.close();
		System.out.println("Done");
	}
}

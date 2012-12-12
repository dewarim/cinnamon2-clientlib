package safran;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Very basic connectivity test client: connects to the server using the settings
 * in file config.properties and then disconnects again.
 * This class may grow into a command line client one day.
 */
public class CLI {

	/**
	 * @param args command line arguments
	 * @throws IOException if properties cannot be read.
	 */
	public static void main(String[] args) throws IOException {		
		Properties config = new Properties();
		config.load(new BufferedInputStream(new FileInputStream("config.properties")));

		Client client = new Client(config);
		
		boolean is_connected = client.connect();
		if (!is_connected) {
			System.out.println("Failed to connect to server.");
			System.exit(0);
		}
		System.out.println(client.sessionTicket);
		
		client.disconnect();
	}

}

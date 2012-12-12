package safran.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;

import java.util.Properties;

/**
 * Tests if the command sendPasswordMail succeeds. This test is considered successful,
 *  if it returns an XML string without error. We do not check the admin's mailbox.  
 */
public class ResetPasswordTest extends BaseTest{	
	
	public ResetPasswordTest(){
		super();
	}
	
	public ResetPasswordTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

	
	/**
	 * Calls client.sendPasswordMail. This test will succeed unless an error message is returned.
	 */
	@Test
	public void sendPasswordMailTest(){
		client.sendPasswordMail("cmn_test", "admin");
	}

	
	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

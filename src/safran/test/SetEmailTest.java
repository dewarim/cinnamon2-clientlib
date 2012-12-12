package safran.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

import java.util.Properties;

/**
 * Tests if the command setEmail succeeds. This test is considered successful,
 *  if it returns an XML string without error. We do not check the admin's mailbox.  
 */
public class SetEmailTest extends BaseTest{	
	
	public SetEmailTest(){
		super();
	}
	
	public SetEmailTest(Properties config, Client client){
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
	public void setEmailTest(){
		String userXml = client.getUserByName("admin");
		String adminEmail = ParamParser.parseXmlToDocument(userXml, "").selectSingleNode("//email").getText();
		client.setEmail(adminEmail);
	}

	
	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

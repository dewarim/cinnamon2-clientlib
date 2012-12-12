package safran.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import safran.test.BaseTest;

import java.util.Properties;

/**
 * Reported bug: in case of wrong login name or missing password, a non-standard
 * error message is created (which cannot be accessed with the XPath statement "/error/message".
 */
public class Bug1994 extends BaseTest {

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public Bug1994(){
		super();
	}

	public Bug1994(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp(true);
	}
	
	@Test
	public void loginTest(){
	    Client nobody = new Client(config);
//        nobody.setUsername("Wrong Client");
//        nobody.setClientPassword("...");
        nobody.setUsername("a");
        nobody.setClientPassword("b");
        try{
            nobody.connect();
        }
        catch (Exception e){
            String oldMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<error>No entity found for query</error>";
            String errorMessage = e.getMessage();
            assert ! errorMessage.equals(oldMessage) : "Found wrongly formatted error message";
            String expectedMessage ="<error><message>error.user_not_found</message><code>error.user_not_found</code><parameters/></error>";
            assert errorMessage.contains(expectedMessage) :
                    "Error message is not what I expected: \n"+expectedMessage+"\n but received: \n"+errorMessage;
        }
        nobody.setUsername("admin");
        try{
            nobody.connect();
        }
        catch (Exception e){
            String error = e.getMessage();
            String expectedMessage = "error.wrong.password";
            assert error.contains(expectedMessage) : "Received message does not contain "+expectedMessage+" but: \n"+error;
        }
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

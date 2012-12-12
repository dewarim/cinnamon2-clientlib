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
 * Reported bug: connection attempt with invalid repository name will result in a
 * nondescript NullPointerException instead of a better error message.
 */
public class Bug1995 extends BaseTest {

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public Bug1995(){
		super();
	}

	public Bug1995(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp(true);
	}
	
	@Test
	public void invalidRepositoryTest(){
	    Client nobody = new Client(config);
        nobody.setUsername("nix");
        nobody.setClientPassword("nop");
        nobody.setRepository("does_not_exist");
        try{
            nobody.connect();
        }
        catch (Exception e){
            String oldMessage = "<code>java.lang.NullPointerException</code>";
            String errorMessage = e.getMessage();
            assert ! errorMessage.contains(oldMessage) : "Found NPE instead of improved error message.";
            String expectedMessage ="<code>error.unknown.repository</code>";
            assert errorMessage.contains(expectedMessage) :
                    "Error message is not what I expected: \n"+expectedMessage+"\n but received: \n"+errorMessage;
        }
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

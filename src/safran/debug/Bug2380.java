package safran.debug;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import safran.test.BaseTest;
import utils.ParamParser;

import java.util.Properties;

/**
 * Reported bug: login from Windows client fails with message about failed
 * parsing of server response.
 * Server log only shows "getUsers" as last (completed) action, no exception message.
 *
 * Solution: compute response content length from bytes, not number of Unicode points.
 */
public class Bug2380 extends BaseTest {

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public Bug2380(){
		super();
	}

	public Bug2380(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
	}
	
	@Test
	public void getUsersTest(){
        try{
            client = fetchNewClient("config.properties");
            client.connect();
            String xml  = client.getUsers();
            log.debug("length: "+xml.length());
            log.debug("getUsers returned: \n"+xml);
            // BOM check:
//            String firstBytes = xml.substring(0,3);
//            log.debug("first bytes: "+firstBytes);
            ParamParser.parseXmlToDocument(xml, "Failed to parse xml response from getUsers");
        }
        catch (Exception e){
            log.debug("Failed to call getUsers: ",e);
            assert false: e.getLocalizedMessage();
        }
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

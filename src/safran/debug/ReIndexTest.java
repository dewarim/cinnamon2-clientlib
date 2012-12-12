package safran.debug;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.test.BaseTest;

/**
 * This test issues a reindex command to the repository specified in the config.properties.
 * It is intended for testing IndexServer-performance and functionality.
 *
 */
public class ReIndexTest extends BaseTest{

	
	@BeforeClass
	public void setUp(){
		super.setUp(true);	
	}
	
	@Test // should probably have its own test class.
	public void reindexTest() {
		String xml = client.reindex("objects");
		log.debug(xml);
		assert xml.contains("reindexResult"): "missing 'reindexResult'-Tag - received:\n"+xml;
	}
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}
	
}

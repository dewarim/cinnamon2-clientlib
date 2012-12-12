package safran.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;

import java.util.Properties;

public class ChangeTriggerTest extends BaseTest{
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ChangeTriggerTest(){
		super();
	}
	
	public ChangeTriggerTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void changeTriggerTest(){
		Long testFolderId = client.createFolder("testFolder", 0L);
		Long id = client.create("", "ChangeTriggerTestObject", testFolderId);
		OSD_Test.createTestFile();
		client.lock(id);
		client.setContent(OSD_Test.testData, "xml", id);
		String meta = client.getMeta(id);
		assert meta.contains("<testTrigger/>") : "testTrigger did not change metadata:\n"+meta;

		SearchTest st = new SearchTest(config,client);
		String query = st.loadQuery("changeTriggerQuery.xml");
		String xml = client.searchObjects(query);
		assert xml.contains(id.toString()) :
			"search for changed testTrigger Object did not return the expected result.\n"+xml;

        String setMeta = client.setMetaGetRawResponse(id, "<meta><testTriggerSetMeta/></meta>");
        log.debug(setMeta);
        assert setMeta.contains("<warning>") : "testTrigger did not add warning to setMeta-response.";
        client.unlock(id);
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

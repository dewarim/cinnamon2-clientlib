package safran.test;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

import java.util.List;
import java.util.Properties;

public class FolderTypeTest extends BaseTest {

    private Long id;

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

    public FolderTypeTest(){        
    }

    public FolderTypeTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}

	@Test()
	public void createFolderTypeTest() {
		id = client.createFolderType("foo", "TestType");
		assert id > 0 : "Got invalid FolderType ID " + id;
	}

	@Test(dependsOnMethods = {"createFolderTypeTest"})
	public void getFolderTypesTest(){
		String result = client.getFolderTypes();
		log.debug("folderTypes: "+result);
		assert result.contains("foo") : "folderTypes did not contain TestType 'foo'";
		Document doc = ParamParser.parseXmlToDocument(result, null);
		List<?> types = doc.selectNodes("//folderType");
		assert types.size() == 2 : "Received "+types.size()+" FolderTypes instead of 2.\n"+result;
	}
	
	@Test(dependsOnMethods = {"createFolderTypeTest", "getFolderTypesTest"})
	public void deleteFolderTypeTest() {
		boolean result = client.deleteFolderType(id);
		assert result : "Could not delete FolderType " + id;
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

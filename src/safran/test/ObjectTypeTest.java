package safran.test;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ParamParser;

import java.util.List;

public class ObjectTypeTest extends BaseTest {

	private Long id;

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

	@Test()
	public void createObjectTypeTest() {
		id = client.createObjectType("foo", "TestType");
		assert id > 0 : "Got invalid ObjectType ID " + id;
	}

	@Test(dependsOnMethods = {"createObjectTypeTest"})
	public void getObjTypesTest(){
		String result = client.getObjTypes();
		log.debug("objectTypes: "+result);
		assert result.contains("foo") : "objTypes did not contain TestType 'foo'";
		Document doc = ParamParser.parseXmlToDocument(result, null);
		List<?> types = doc.selectNodes("//objectType");
		assert types.size() == 2 : "Received "+types.size()+" ObjectTypes instad of 2.\n"+result;
		assert result.contains("<sysName>foo</sysName>") : "could not find sysName 'foo'";
	}
	
	@Test(dependsOnMethods = {"createObjectTypeTest", "getObjTypesTest"})
	public void deleteObjectTypeTest() {
		boolean result = client.deleteObjectType(id);
		assert result : "Could not delete ObjectType " + id;
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

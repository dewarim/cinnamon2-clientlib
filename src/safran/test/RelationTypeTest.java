package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ParamParser;

public class RelationTypeTest extends BaseTest {
	private long relationTypeID;
	private String relationTypeName;
	
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void createRelationTypeTest() {
		relationTypeName = "testrelation";
		relationTypeID = client.createRelationType(relationTypeName, "RelationType Entry for testing", true, false, null, false, true);
		assert relationTypeID > 0 : "Could not create RelationType.";
	}

	@Test(dependsOnMethods={"createRelationTypeTest"})
	public void getRelationTypeTest() {
		String ret = client.getRelationTypes();
		log.debug("getRelationTypes: "+ret);
		assert ret.contains(relationTypeName) : "response of getRelationTypes did not contain "+relationTypeName;
        Document doc = ParamParser.parseXmlToDocument(ret);
        Node rightCopy = doc.selectSingleNode("/relationTypes/relationType[name='testrelation']/cloneOnRightCopy");
        assert rightCopy.getText().equals("true") : "testrelation does not have correct cloneOnRightCopy flag.\n"+ret;
        Node leftCopy = doc.selectSingleNode("/relationTypes/relationType[name='testrelation']/cloneOnLeftCopy");
        assert leftCopy.getText().equals("false") : "testrelation does not have correct cloneOnLeftCopy flag.\n"+ret;
	}
	
	@Test(dependsOnMethods={"createRelationTypeTest", "getRelationTypeTest"})
	public void deleteRelationTypeTest() {
		boolean ret = client.deleteRelationType(relationTypeID);
		assert ret : "could not delete relationType " + relationTypeID;	
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

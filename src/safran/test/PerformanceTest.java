package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.PermissionName;
import utils.ParamParser;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance Tests (using searchObjects-API).
 *
 */
public class PerformanceTest extends BaseTest{

	Long folderId = 0L;
	
	@BeforeClass
	public void setUp(){
		super.setUp(true);
		
		// clear the index on the test system
		client.clearIndex("server.Folder");
		client.clearIndex("server.data.ObjectSystemData");
		
		folderId = client.createFolder("testFolder", 0L);

		assert folderId != 0L;
		
	}
	
	/*
	 * Testing performance for 1000 objects created by admin
	 */
	@Test()
	public void performanceAdminTest(){
		Integer max = 100;
		
		Date startTime = new Date();
		createTestObjects(client, max);
		String xml = timeQuery(client, "searchObjects.xml");
		assert xml.contains("<name>IndexItemTestObject1</name>");
		Date endTime = new Date();
		Long diff = (endTime.getTime() - startTime.getTime())/1000; 
		log.debug("Time needed to create and search for 1000 objects: "+diff+" seconds.");

//		log.debug(ParamParser.prettyPrint(xml));
		
		// check that the OSDs are serialized correctly: (Bug #1524)
		log.debug("checkResultSize");
		checkResultSize(xml, "//modifier", max);
		checkResultSize(xml, "//owner", max +1); // owner node also appears in folder/owner element.
		checkResultSize(xml, "//creator", max);

	}
	
	
	
	/*
	 * Testing performance for 1000 objects created by dummy user.
	 */
//	@Test()
	public void performanceUserTest(){
		log.debug("creating test objects");
		Long userId = client.createUser("foo", "foo", "foo", "foo", "foo");
		AclTest at = new AclTest(config, client);
		at.createAclTest();
		Long aclId = at.getAclId();
		
		// add user to acl
		// 1. find group of this user.
		String xml = client.getGroupsOfUser(userId, false);
		String gid = parseXmlResponse(xml).selectSingleNode("//id").getText();
		Long groupId = Long.parseLong(gid);
		// 2. add user's own group to the test acl.
		xml = client.addGroupToAcl(groupId, aclId);
		log.debug(xml);
		String aeid = parseXmlResponse(xml).selectSingleNode("//id").getText();
		Long aclEntryId = Long.parseLong(aeid);		
		AclIntegrationTest ait = new AclIntegrationTest(config, client);
		ait.setAdminClient(client);
		ait.setPermissions(ParamParser.parseXmlToDocument(client.listPermissions(), null));
		ait.addToAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);
		ait.addToAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		ait.addToAclEntry(PermissionName.BROWSE_OBJECT, aclEntryId);
		Map<String,String> folderMeta = new HashMap<String,String>();
		folderMeta.put("aclid", aclId.toString());
		client.updateFolder(folderId, folderMeta);		
		Client userClient = new Client(client.getConfigurationProperties());
		userClient.setUsername("foo");
		userClient.setClientPassword("foo");
		boolean r =  userClient.connect();
        assert r;
		Integer max = 1000;
		Date startTime = new Date();
		createTestObjects(userClient, max);
		
		xml = timeQuery(userClient, "searchObjects.xml");
		
		Date endTime = new Date();
		Long diff = (endTime.getTime() - startTime.getTime())/1000; 
		log.debug("Time needed to create and search for "+max+" objects: "+diff+" seconds.");
		
		assert xml.contains("<name>IndexItemTestObject1</name>");
		
//		log.debug(ParamParser.prettyPrint(xml));
		
		// check that the OSDs are serialized correctly: (Bug #1524)
		log.debug("checkResultSize for :\n"+xml);
		checkResultSize(xml, "//modifier", max);
		checkResultSize(xml, "//owner", max+1); // adjusted for test folder.
		checkResultSize(xml, "//creator", max);

	}
	
	void createTestObjects(Client client, Integer max){
		log.debug("creating "+ max + " test objects");
        String query = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><BooleanQuery><Clause occurs=\"must\"><TermQuery fieldName=\"objecttype\">00000000000000000001</TermQuery></Clause><Clause occurs=\"must\"><TermQuery fieldName=\"name\">IndexItemTestObject1</TermQuery></Clause><Clause occurs=\"must\"><TermQuery fieldName=\"latesthead\">true</TermQuery></Clause></BooleanQuery>";

		for(int x = 0;x < max ;x++){
			if(x % 50 == 0){
				log.debug(String.valueOf(x));
                client.searchObjects(query);
			}
			Long id = client.create("<meta><name>Alice#"+x+"</name><testing>foo</testing></meta>",
					"IndexItemTestObject1", "testdata/cinnamon-screen.jpg", "jpeg","image/jpeg", folderId);
            // testing correct download:
//            File content = client.getContentAsFile(id);
//            assert content.length() == 54692 : "Content is not 54692 bytes as expected: "+content.length();
        }
	}
	
	/**
	 * Execute a query, log the execution time and return the response.
     * @param timeClient client to use for calling the search API
     * @param queryFilename filename from which to load the query.
	 * @return the server's response.
	 */
	String timeQuery(Client timeClient, String queryFilename){
		String query = loadQuery(queryFilename);
		Date start = new Date();
		String xml = timeClient.searchObjects(query);
		Date end = new Date();
		Long elapsedTime = end.getTime() - start.getTime();
		log.debug("elapsed time: "+elapsedTime);
		return xml;
	}
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}
	
	/**
	 * Load the content of the query file which has to be located in the folder testData/lucene.
	 * @param filename the filename of the query you want to load.
	 * @return the content of the given file.
	 */
	String loadQuery(String filename){
		String query;
		try{
			query = utils.ContentReader.readFileAsString("testdata/lucene/" + filename);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return query;
	}

	@SuppressWarnings("unchecked")
	void checkResultSize(String xml, String xpath, Integer size){
		Document doc = ParamParser.parseXmlToDocument(xml, null);

		List<Node> nodes = doc.selectNodes(xpath);		
		log.debug("nodes.size: "+nodes.size());
		assert nodes.size() == size :
			String.format("Wrong number of items in search results for node '%s' : not %d but %d.",
					xpath, size, nodes.size());
	}
	
}

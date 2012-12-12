package safran.test;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ParamParser;

public class GroupTest extends BaseTest{	

    Long folderId = 0L;
	Long userId = 0L;
	Long groupId = 0L;
	String groupName = "testGroup"+Math.random();
	String groupDescription = "This is a TestGroup for automated testing";
	
	@BeforeClass
	public void setUp(){
		super.setUp();
			
	}

	// ------------ Start Testing -------------------
	
	@Test
	public void createGroupTest() {
		Document doc;
		try {
			doc = DocumentHelper.parseText(client.createGroup(groupName, groupDescription, 0L));
			log.debug(doc.asXML());
			Node node = doc.selectSingleNode("//cinnamon/groupid");
			groupId = Long.parseLong(node.getText());
			assert groupId > 0 : "received invalid groupId - createGroup failed.";
			
		} catch (Exception e) {
			e.printStackTrace();
			assert false : "reading xml document from server failed.";
		}
	}
	
	@Test(dependsOnMethods = {"createGroupTest"})
	public void addUserToGroupTest(){
		userId = client.createUser("Testuser", "Max Mustermann", "Foo", "Bar", "none");		
		String result = client.addUserToGroup(userId, groupId);
		Document doc = ParamParser.parseXmlToDocument(result, null);
		assert doc.selectSingleNode("/cinnamon/addUserToGroup[@result='true']") != null :
			"addUserToGroup failed with:\n"+result;
	}
	
	@Test(dependsOnMethods = {"addUserToGroupTest", "listGroups"})
	public void removeUserFromGroupTest(){
		String xml = client.removeUserFromGroup(userId, groupId);
		log.debug("removeUserFromGroup:\n"+xml);
		Document doc = ParamParser.parseXmlToDocument(xml, null);
		assert doc.selectSingleNode("/cinnamon/removeUserFromGroup[@result='true']") != null :
			"removeUserFromGroup failed with:\n"+xml;
	}
	
	
	@Test (dependsOnMethods = {"createGroupTest", "addUserToGroupTest"})
	public void listGroups(){
		Document doc = null;
		try {			
			doc = DocumentHelper.parseText(client.listGroups(0L));
		} catch (Exception e) {
			e.printStackTrace();
			assert false : "reading xml document from server failed.";
		}
		
		assert doc != null : "received document";
		assert doc.getRootElement().getName().equals("groups") : "received strange xml:\n"+doc.asXML();

        assert doc.selectSingleNode("//subGroups") != null : "Could not find subGroups element.";
        assert doc.selectSingleNode("//users") != null : "Could not find users element.";
        assert doc.selectSingleNode("//users/user[name='Foo']") != null :
                "Could not find test user as group member\n"+doc.asXML();
	}
	
	@Test (dependsOnMethods = {"createGroupTest"})	
	public void listGroupsWithIDTest() {
		Document doc = null;
		long subGroupId = 0;
		try {			
			doc = DocumentHelper.parseText(client.createGroup("subgroup", "subgroup for listGroupsWithIDTest()", groupId));
			Node node = doc.selectSingleNode("//cinnamon/groupid");
			subGroupId = Long.parseLong(node.getText());
			assert subGroupId > 0 : "received invalid groupId - createGroup failed.";
			
		} catch (Exception e) {
			e.printStackTrace();
			assert false : "reading xml document from server failed.";
		}

		try {
			try {			
				doc = DocumentHelper.parseText(client.listGroups(groupId));
			} catch (Exception e) {
				e.printStackTrace();
				assert false : "reading xml document from server failed.";
			}
			
			assert doc != null : "received document";
			assert doc.getRootElement().getName().equals("groups") : "received strange xml:\n"+doc.asXML();
			// TODO: test if this is really the group list.		
	
		} finally {
			boolean result = false;
			try {			
				doc = DocumentHelper.parseText(client.deleteGroup(subGroupId));
				Node node = doc.selectSingleNode("//cinnamon/result/value");
				result = Boolean.parseBoolean(node.getText());
			} catch (Exception e) {
				e.printStackTrace();
				assert false : "reading xml document from server failed.";
			}
			
			assert result : "delete testGroup failed.";
		}
	}
	
	@Test(dependsOnMethods = {"removeUserFromGroupTest", "listGroupsWithIDTest"})
	public void deleteGroupTest(){		
		Document doc;
		Boolean result = false;
		try {			
			doc = DocumentHelper.parseText(client.deleteGroup(groupId));
			Node node = doc.selectSingleNode("//cinnamon/result/value");
			result = Boolean.parseBoolean(node.getText());
		} catch (Exception e) {
			e.printStackTrace();
			assert false : "reading xml document from server failed.";
		}
		
		assert result : "delete testGroup failed.";
	}
	

	@Test
	public void failDeleteGroupTestInvalidID(){
		try{
			String result = client.deleteGroup(-1L);
			assert false :"no error while trying to delete missing group:\n"+result;
		}
		catch (Exception e) {
			checkExceptionMessage(e, "failDeleteGroupTestInvalidId","<code>error.group.not.found</code>");
		}
	}

	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}
	
	
}


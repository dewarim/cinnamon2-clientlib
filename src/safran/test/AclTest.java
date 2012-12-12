package safran.test;


import org.custommonkey.xmlunit.Diff;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import safran.Client;
import utils.ParamParser;

import java.io.IOException;
import java.util.Properties;

public class AclTest extends BaseTest{	
	
	Long folderId = 0L;
	Long aclId = 0L;
	String aclName = "testacl"+Math.random();
	private Long groupID;
	String defaultAclId;

	// ------------ Start Testing -------------------
	
	public AclTest() {
		super();
	}
	
	public AclTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}

	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	/**
	 * Tests if an Acl can be created on the server.
	 * Sets aclId. 
	 */
	@Test
	public void createAclTest(){
		assert client.getSessionTicket() != null : "no valid sessionTicket in client!";
		String description = "Test acl description";
		log.debug(String.format("calling createAcl with name=%s and description=%s",aclName,description));
		String aclResponse = client.createAcl(aclName, description);
		Document aclDoc = parseXmlResponse(aclResponse);
		aclId = Long.parseLong(aclDoc.selectSingleNode("/acls/acl/id").getText());
		assert aclId > 0 : "Could not create testacl.";		
	}
	
	@Test (dependsOnMethods={"createAclTest"})	
	public void addGroupToAcl() throws DocumentException {
		Document doc = DocumentHelper.parseText( client.createGroup("group" + Math.random(), "group for addGroupToAcl()", 0L));
		Node node = doc.selectSingleNode("//cinnamon/groupid");
		groupID = Long.parseLong(node.getText());
		assert groupID > 0 : String.format("received invalid groupID (%s) - createGroup failed.", node.getText());
		
		doc = DocumentHelper.parseText(client.addGroupToAcl(groupID, aclId));
		node = doc.selectSingleNode("//aclEntries/aclEntry/id");
		assert node != null : String.format("failed to add group (%d) to ACL (%d);\n%s", groupID, aclId, doc.asXML());
	}
	
	@Test (dependsOnMethods={"addGroupToAcl"})	
	public void removeGroupFromAcl() throws DocumentException{
		String result = client.removeGroupFromAcl(groupID, aclId);		
		assert result.contains("group.removed_from_acl") : 
			String.format("failed to remove group (%d) from ACL (%d)", groupID, aclId);
	}
	
	@Test(dependsOnMethods = {"createAclTest"})
	public void getAclsTest() throws DocumentException{
		String xml = client.getAcls();
		Document doc = DocumentHelper.parseText(xml);
		/*
		 * We cheat a little to get the default Acl's id:
		 */
		Node defaultAcl = doc.selectSingleNode("//id[ parent::acl[child::name[text() = 'Default ACL']]]");

		defaultAclId = defaultAcl.getText();		
		
		Diff myDiff = null;
		try {
			myDiff = new Diff("<acls>" +
					"<acl><id>"+defaultAclId+"</id><name>Default ACL</name><description>Default ACL</description></acl>"+
					"<acl><id>"+aclId+"</id><name>"+
					aclName+"</name><description>Test acl description</description></acl></acls>", xml);
		} catch (SAXException e) {
			e.printStackTrace();
			assert false : "listTest failed in xml-diff\n"+xml;
		} catch (IOException e) {
			e.printStackTrace();
			assert false : "listTest failed in xml-diff";
		}		
		assert myDiff.similar() : "XML-Diff failed for getAcls: \n"+xml+"\n"+myDiff.toString();
	}
	
	@Test (dependsOnMethods={"createAclTest", "getAclsTest"})	
	public void listAclMembersTest(){
		String xml = client.listAclMembers(Long.parseLong(defaultAclId));
		log.debug("xml:\n"+xml);
		Diff myDiff = null;
		try {
			myDiff = new Diff("<users><user><id>"+getAdminId()+
					"</id><name>admin</name><fullname>Administrator</fullname>" +
					"<description>Cinnamon Administrator</description><activated>" +
					"true</activated><isSuperuser>true</isSuperuser></user></users>"
					, xml);
		} catch (SAXException e) {
			e.printStackTrace();
			assert false : "failed in xml-diff\n"+xml;
		} catch (IOException e) {
			e.printStackTrace();
			assert false : "failed in xml-diff";
		}		
		assert myDiff.similar() : "XML-Diff failed for listAclMembers: \n"+xml+"\n"+myDiff.toString();
	}

	String getAdminId(){
		Document users = null;
		try {
			users = ParamParser.parseXmlToDocument(client.getUsers());
		} catch (Exception e) {
			log.debug("",e);
			assert false : "getUsers failed (for fetching adminId)";
		}
		Node admin = users.selectSingleNode("//user[child::name[text()='admin']]/id");
		return admin.getText();
	}
	
	@Test (dependsOnMethods={"createAclTest", "getAclsTest"})	
	public void getUsersAcls(){
		String xmlStr = client.getUsersAcls(Long.parseLong(getAdminId()));
		Document xml = ParamParser.parseXmlToDocument(xmlStr, null);
		
		Diff myDiff = null;
		try {
			myDiff = new Diff("<acls><acl><id>"+
					defaultAclId+
				"</id><name>Default ACL</name><description>Default ACL</description></acl></acls>"
					, xml.asXML());
		} catch (SAXException e) {
			e.printStackTrace();
			assert false : "getUsersAcls failed in xml-diff\n"+xml;
		} catch (IOException e) {
			e.printStackTrace();
			assert false : "getUserAcls failed in xml-diff";
		}		
		assert myDiff.similar() : "XML-Diff failed for getAcls: \n"+xml+"\n"+myDiff.toString();
		
	}
	
	@Test (dependsOnMethods={"createAclTest", "removeGroupFromAcl"})	
	public void deleteAclTest(){
		boolean result = client.deleteAcl(aclId);
		assert result : "Could not delete ACL.";
	}
	
	

	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	/**
	 * @return the aclId
	 */
	public Long getAclId() {
		return aclId;
	}

	/**
	 * @return the aclName
	 */
	public String getAclName() {
		return aclName;
	}

	/**
	 * @param aclName the aclName to set
	 */
	public void setAclName(String aclName) {
		this.aclName = aclName;
	}
	
	
}


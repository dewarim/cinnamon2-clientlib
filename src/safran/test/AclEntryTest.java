package safran.test;


import org.dom4j.Document;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.PermissionName;

import java.util.Properties;

public class AclEntryTest extends BaseTest{	
	
	Long aclId = 0L;
	Long aclEntryId = 0L;
	String aclName = "testacl"+Math.random();

	// ------------ Start Testing -------------------
	
	public AclEntryTest() {
		super();
	}
	
	public AclEntryTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}

	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void listAclEntryTest(){
		// create a test-acl and an aclEntry:
		AclIntegrationTest ait = new AclIntegrationTest(config, client);
		ait.setAdminClient(client);
		ait.createPrerequisites();
		aclEntryId = ait.getAclEntryId();
		
		// add a browse-object PermissionName to test AE
		ait.addToAclEntry(PermissionName.BROWSE_OBJECT, aclEntryId);
		aclId = ait.getAclId();
		String aeList = client.listAclEntries(aclId, "aclid");
		log.debug("aeList: "+aeList);
		assert aeList.contains("<name>_browse</name>") : "missing browse-PermissionName on AclEntry";
			
		Long groupId = ait.getGroupId();
		String aeg = client.listAclEntries(groupId, "groupid");
		assert aeg.contains(aclEntryId.toString()) :
			"listAclEntries-response did not return aclEntryId";
		assert aeg.contains(groupId.toString()) :
			"listAclEntries-response did not return groupId";
	}
	
	@Test(dependsOnMethods={"listAclEntryTest"})
	public void getAclEntryTest(){
		try{
			client.getAclEntry(0L);
			assert false : "getAclentry found non existent entry.";
		}
		catch (Exception e) {
			checkExceptionMessage(e, "getAclEntry", "error.object.not.found");
		}
		
		String ae = client.getAclEntry(aclEntryId);
		log.debug("ae: "+ae);
		Document aeDoc = parseXmlResponse(ae);
		String id = aeDoc.selectSingleNode("/aclEntries/aclEntry/id").getText();
		assert id.equals(aclEntryId.toString()) : 
			String.format("expected: %s - got: %s", aclEntryId, aclId);
		String aclIdNodeText = aeDoc.selectSingleNode("/aclEntries/aclEntry/aclId").getText();
		assert aclIdNodeText.equals(aclId.toString()) :
			String.format("aclId %s in response != expected aclId %s.",
					aclIdNodeText, aclId.toString());
		assert ae.contains("<description>_browse.description</description>") : 
			"Could not find browse_PermissionName";
			
	}
	

	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		assert client.disconnect() : "Disconnect form Server failed!";
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


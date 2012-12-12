package safran.test;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

import java.util.Properties;

public class UserTest extends BaseTest{	
	
	int folderId = 0;
	Long userId = 0L;

	private String username = "max";
	private String password = "geheim";
	
	Client testUserClient;
	
	public UserTest(){
		super();
	}
	
	public UserTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

	
	//	 -------------- User related tests ---------------------
	/**
	 * Tests if the command createUser() succeeds. 
	 * Sets userId to the new user's id.
	 */
	@Test
	public void createUserTest(){
		userId = client.createUser("Testuser", "Max Müstermän", username, password, "none" );
		assert userId > 0 : "user creation failed!";        
	}
	
	@Test(dependsOnMethods = {"createUserTest"})
	public void newUserLoginTest(){
		// Try to login with new user:
		assert config.getProperty("default_repository") != null : "no default repository defined!";
		testUserClient = new Client(config.getProperty("server.url"),
				username, "geheim", config.getProperty("default_repository"));
		assert testUserClient.connect() : "Connection for new user failed.";
	}
	
	@Test()
	public void setPasswordTest(){
		String oldPassword = client.getPassword();
		try{
			client.setPassword("x");
			assert false : "Too short password has not been detected";
		}
		catch (Exception e) {
			// exception expected.
			log.debug("",e);
		}
		
		String xml = client.setPassword("sehr geheim");
		assert xml.contains("success.set.password") : "Failed to set password: \n"+xml;
		assert client.disconnect() : "Disconnect failed after setPassword";
		
		client.setClientPassword("sehr geheim"); // set password which is used in connect()
		assert client.connect() : "Connect failed after setPassword";
		
		// revert password:
		xml = client.setPassword(oldPassword);		
		assert xml.contains("success.set.password") : "Failed to set password: \n"+xml;
		client.setClientPassword(oldPassword);
	}
	
	@Test(dependsOnMethods = {"createUserTest", "newUserLoginTest"})
	public void createUserWithoutPermissionName(){
		// Try to create a new user without authorization:
		Long newId = null;
		try{
			newId = testUserClient.createUser("Testuser", "Marla Musterfrau", "Marla", "geheimer", "none");
		}
		catch (Exception e) {
			// do nothing. newId is already null.
			log.debug("",e);
		}
		assert newId == null : "Normal User managed to create another User!";
	}
 
	@Test(dependsOnMethods = {"createUserTest"})
	public void getUser() throws DocumentException {
		Document ret = ParamParser.parseXmlToDocument(client.getUser(userId));
		assert ret != null : "document is null";
		String id = ret.valueOf("//users/user/id");
		assert id.equals(userId.toString()) : String.format("wrong ID: '%s' - expected '%d'", id, userId);
		
		String name = ret.valueOf("//users/user/name");
		assert name.equals(username) : String.format("wrong name: '%s' - expected '%s'", name, username);
		String fullname = ret.valueOf("//users/user/fullname");
		assert fullname.equals("Max Müstermän") : String.format("wrong fullname: '%s' - expected 'Max Müstermän'", fullname);
		String description = ret.valueOf("//users/user/description");
		assert description.contains("Testuser") : String.format("wrong description: '%s' - expected 'Testuser'", description);
		String isSuperuser = ret.valueOf("//users/user/isSuperuser");
		assert isSuperuser.equals("false"): "testUser is a superuser.";

        String sudoer = ret.valueOf("//users/user/sudoer");
        assert sudoer.equals("false"): "testUser is sudoer.";
        String sudoable = ret.valueOf("//users/user/sudoable");
        assert sudoable.equals("false"): "testUser is sudoable.";
	}	
	
	@Test
	public void getUsersTest(){
		Document userList = null;
		try{            
			userList = ParamParser.parseXmlToDocument(client.getUsers());
		}
		catch (DocumentException e) {
			assert false: "Encountered DocumentException: " + e.getLocalizedMessage();
		}
		assert userList.asXML().length() > 0 : "received empty response";
		assert userList.asXML().contains(">admin<") : "Could not find admin account in list.";
	}
	
	@Test(dependsOnMethods = {"createUserTest"})
	public void getUserByNameTest(){
		String xml = client.getUserByName(username);
		Document doc = ParamParser.parseXmlToDocument(xml, "error.parse.xml");
		String id = doc.selectSingleNode("/users/user/id").getText();
		assert id.equals(userId.toString()) : "getUserByName did not return the correct id. Received: \n"+xml;	
	}
	
	@Test(dependsOnMethods = {"createUserTest", "getUserByNameTest"})
	public void superuserTest(){
		String admin = client.getUserByName("admin");
		Document doc = ParamParser.parseXmlToDocument(admin, null);
		String isSuperuser = doc.valueOf("//users/user/isSuperuser");
		assert isSuperuser.equals("true"): "admin is a not superuser.";
	}
	
	@Test(dependsOnMethods = {"createUserTest", "getUserByNameTest"})
	public void getUsersPermissionsTest(){
		// find default_acl:
		String acls = client.getAcls();
		Document doc = ParamParser.parseXmlToDocument(acls, null);
		log.debug("getUserPermissionsTest"+doc.asXML());
		Node acl = doc.selectSingleNode("/acls/acl[name='Default ACL']");
		Long aclId = ParamParser.parseLong(acl.selectSingleNode("id").getText(), null);
		
		// 1. a new user should not have any PermissionNames.
		String xml = client.getUsersPermissions(userId, aclId);
		log.debug(xml);
		assert xml.contains("<permissions/>"): "expected empty PermissionNames element. received: \n"+xml;

		// 2. an admin user should have all PermissionNames.
        // changed: an admin does not need any permissions, so they are not assigned during server auto-initialization.
//		String admin = client.getUserByName("admin");
//		doc = ParamParser.parseXmlToDocument(admin, "error.parse.xml");
//		Long id = ParamParser.parseLong(doc.selectSingleNode("/users/user/id").getText(),"Coud not find id.");
//		xml = client.getUsersPermissions(id, aclId);
//		assert xml.contains("<permissions>") && xml.contains("<name>_set_acl</name>"): "getUserPermissionNames for admin returned: \n"+xml;
		
	}
	
	@Test(dependsOnMethods = {"getUsersPermissionsTest", "getUser"})
	public void deleteUserTest() {
		//	Try to delete user without authorization:
		Client c = new Client(config.getProperty("server.url"), username, "geheim",
				config.getProperty("default_repository"));
		assert c.connect() : "Connection for deleteUserTest failed.";
		
		boolean result;
		try{
			// deleteUser will throw an exception if the result is an error.
			result = c.deleteUser(userId);
		}
		catch (Exception e) {
			log.debug("",e);
			result = false;
		}
		assert !result : "Normal user managed to delete himself!";
		
		// delete with admin rights:
		result = client.deleteUser(userId);
		assert result : "Deleteuser failed!";
	}
	
	// TODO: test getGroupsOfUser (recursively = true || false)
	
	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}


	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}	
	
}

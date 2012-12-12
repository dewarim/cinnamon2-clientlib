package safran.test;

import org.custommonkey.xmlunit.Diff;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import safran.Client;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class PermissionTest extends BaseTest{
	private long permissionId;
	private String permissionName = "test_permission_"+ Math.random();
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public PermissionTest(){
		super();
	}
	
	public PermissionTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void createPermissionTest(){
		String xml = client.createPermission(permissionName, "testPermission-Description");
		Document doc = parseXmlResponse(xml);
		Node pNode = doc.selectSingleNode("//id");
		String pId = pNode.getText();
		permissionId = Long.parseLong(pId);
		String newPermission =
			String.format("<permissions><permission><id>%s</id><name>%s</name><sysName>%s</sysName><description>%s</description></permission></permissions>",
					pId, permissionName, permissionName, "testPermission-Description");
		
		Diff myDiff = null;
		try {
			myDiff = new Diff(newPermission, doc.asXML());
		} catch (SAXException e) {
			e.printStackTrace();
			assert false : "createPermission failed in xml-diff\n"+doc;
		} catch (IOException e) {
			e.printStackTrace();
			assert false : "createPermission failed in xml-diff";
		}		
		assert myDiff.similar() : "XML-Diff failed: \n"+doc.asXML()+"\n"+myDiff.toString();				
	}

	@SuppressWarnings("unchecked")
	@Test(dependsOnMethods={"createPermissionTest"})
	public void listPermissionsTest() throws DocumentException {
		String xml = client.listPermissions();
		Document doc = DocumentHelper.parseText(xml);
		List<Node> permissions = doc.selectNodes("/permissions/permission");
        for(Node n : permissions){
            log.debug(n.selectSingleNode("name").asXML());
        }
		assert permissions.size() == 20 : "found "+permissions.size()+" permissions instead of 19";
	}
	
	@Test(dependsOnMethods={"createPermissionTest"})
	public void getPermissionTest() throws DocumentException {
		String xml =  client.getPermission(permissionId);
		Document doc = DocumentHelper.parseText(xml);
		// getPermission should return the same as createPermission.
		String permission =
			String.format("<permissions><permission><id>%s</id><name>%s</name><sysName>%s</sysName><description>%s</description></permission></permissions>",
					permissionId, permissionName, permissionName, "testPermission-Description");
		Diff myDiff = null;
		try {
			myDiff = new Diff(permission, doc.asXML());
		} catch (SAXException e) {
			e.printStackTrace();
			assert false : "getPermission failed in xml-diff\n"+doc;
		} catch (IOException e) {
			e.printStackTrace();
			assert false : "getPermission failed in xml-diff";
		}		
		assert myDiff.similar() : "XML-Diff failed: \n"+doc.asXML()+"\n"+myDiff.toString();	
	}
	
	@Test(dependsOnMethods={"createPermissionTest", "getPermissionTest", "listPermissionsTest"})
	public void deletePermissionTest() throws DocumentException {
		String xml = client.deletePermission(permissionId);
		Document doc =  parseXmlResponse(xml);
		String expectedResult = 
			String.format("<success>Permission with id %s was successfully deleted.</success>",
					permissionId);
		assert doc.asXML().contains(expectedResult) : "Did not receive expected result "+
			expectedResult + ", instead I got: " + doc.asXML();
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	/**
	 * @return the permissionId
	 */
	public long getPermissionId() {
		return permissionId;
	}

	/**
	 * @param permissionId the permissionId to set
	 */
	public void setPermissionId(long permissionId) {
		this.permissionId = permissionId;
	}

	
	
}

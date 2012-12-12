package safran.debug;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import safran.test.BaseTest;

import java.util.Properties;

/*
 * Includes some tests from folderTest to setup everything.
 */

public class Bug1150 extends BaseTest{	
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	Long folderId = 0L;
	Long objId = 0L;
	Long deep1 = 0L;
	Long deep2 = 0L;
	Long rel1 = 0L;
	Long rel2 = 0L;
	
	public Bug1150() {
		super();
	}

	public Bug1150(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void createFolderTest(){
		folderId = client.createFolder("testfolder"+Math.random(), 0L);
		assert folderId > 0 : "Could not create folder!";		
	}
	
	@Test(dependsOnMethods = {"createFolderTest"})
	public void createObjectsTest(){
		objId = client.create("", "test", folderId);
		deep1 = client.create("", "rel1", folderId);
		deep2 = client.create("", "rel2", folderId);
	}
	
	@Test(dependsOnMethods = {"createObjectsTest"})
	public void createRelationsTest(){		
		client.createRelationType("_default_relation", "-", false,	true, null);
		
		// create 2 relations:
		String xml = client.createRelation("_default_relation", objId, deep1);
		rel1 = Client.parseLongNode(xml, "/relations/relation/id");		
		xml = client.createRelation("_default_relation", objId, deep2);
		rel2 = Client.parseLongNode(xml, "/relations/relation/id");
		assert rel1 != 0 && rel2 != 0;
		xml = client.getRelations(null, objId, null);
		assert xml.contains("<id>"+rel1+"</id>") : "Could not find relation #1 with getRelations";
		assert xml.contains("<id>"+rel2+"</id>") : "Could not find relation #2 with getRelations";
		
	}
	
	@Test(dependsOnMethods ={"createRelationsTest"})
	public void createAndDeleteVersionTest(){
		Long newVersion = Long.parseLong(client.version(objId));
		client.createRelation("_default_relation", newVersion, deep1);
		client.createRelation("_default_relation", newVersion, deep2);
		assert newVersion > 0;
		
		// check relations on new version:
		String xml = client.getRelations( null, newVersion, null);
		assert xml.contains("<rightId>"+deep1+"</rightId>") : "Could not find relation #1 with getRelations:\n"+xml;
		assert xml.contains("<rightId>"+deep2+"</rightId>") : "Could not find relation #2 with getRelations";
		
		// delete new version:
		assert client.delete(newVersion);
		
		// check relations on old version:
		xml = client.getRelations(null, objId, null);
		assert xml.contains("<id>"+rel1+"</id>") : "Could not find relation #1 with getRelations:\n"+xml;
		assert xml.contains("<id>"+rel2+"</id>") : "Could not find relation #2 with getRelations:\n"+xml;
	}
	
	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";		
	}

	/**
	 * @return the folderId
	 */
	public Long getFolderId() {
		return folderId;
	}

	/**
	 * @param folderId the folderId to set
	 */
	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	/**
	 * @return the objId
	 */
	public Long getObjId() {
		return objId;
	}
	
}


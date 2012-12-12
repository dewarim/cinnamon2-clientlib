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
 * Bug: corrupted Unicode in serialization of Folder/Object names.
 */

public class Bug1184 extends BaseTest{	
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	Long folderId = 0L;
	Long objId = 0L;

	public Bug1184() {
		super();
	}

	public Bug1184(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

	@Test
	public void createFolderTest(){
		folderId = client.createFolder("testfölder", 0L);
		assert folderId > 0 : "Could not create folder!";
		String resp = client.getFolder(folderId);
		assert resp.contains("testfölder") : "getFolder did not return expected foldername.:\n"+resp;
	}
	
	@Test(dependsOnMethods = {"createFolderTest"})
	public void createObjectsTest(){
		objId = client.create("", "täst", folderId);
		String resp = client.getObject(objId);
		assert resp.contains("täst"):"getObject did not return expected objectname.\n"+resp;
	}

	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		assert client.delete(objId);
		assert client.deleteFolder(folderId);
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


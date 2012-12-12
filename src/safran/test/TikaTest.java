package safran.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/*
 * Upload data and check if the Tika-module in Cinnamon has processed the content.
 */

public class TikaTest extends BaseTest {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	Long folderId = 0L;
	Long objId = 0L;

	static final File testData =  new File("_testData_for_upload.xml");
    static final String testDataContent = "<xml><data>This is just a test string</data></xml>";
	private String name;
	
	public TikaTest() {
		super();
	}

	public TikaTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
		createTestFile();
	}

	public static File createTestFile(){
		if (! testData.exists()) {
			// create testData if neccessary
			try {
				FileWriter f = new FileWriter(testData);
				f.write(testDataContent);
				f.close();
			} catch (IOException e) {
				System.out.println("Could not create testData for upload.");
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		testData.deleteOnExit();
        return testData;
	}
	
	
	@Test
	public void createFolderTest(){
		/* using testfolder_$double so we do not have to delete
		 rows by hand for next test-run (in the case we do not reach
		 deleteFolder...
		*/
		folderId = client.createFolder("testfolder"+Math.random(), 0L);
		assert folderId > 0 : "Could not create folder!";		
	}
	

	
	//--------------- create and delete empty obj:
	@Test(dependsOnMethods = {"createFolderTest"})
	public void createTestSimple() {
		name = "createTestObject";
		objId = client.create("", name, folderId);
		assert objId > 0 : "Got invalid objId "+objId;
	}


	@Test(dependsOnMethods = {"createTestSimple"})
	public void setContentTest() {
        client.lock(objId);
		boolean result = client.setContent(testData, "xml",objId);
        client.unlock(objId);
		assert result : "Could not setContent to testData";
	}

    @Test(dependsOnMethods = {"setContentTest"})
	public void checkTikaExtract(){
		String tikaMeta = client.getMeta( objId );
		assert tikaMeta.contains("This is just a test string"):
                "metadata does not seem to contain tika-metadata:\n"+tikaMeta;
	} 
    
    @Test(dependsOnMethods = {"createTestSimple"})
	public void checkTikaWordExtract(){
        client.lock(objId);
        boolean result = client.setContent(new File("testdata/tika_word.doc"), "doc",objId);
        client.unlock(objId);
		String tikaMeta = client.getMeta( objId );
        log.debug("metadata:\n"+tikaMeta);
		assert tikaMeta.contains("Test Word Document for Tika Test"):
                "metadata does not seem to contain tika-metadata:\n"+tikaMeta;
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


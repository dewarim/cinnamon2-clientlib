package safran.test;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public class ClientTest extends BaseTest {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	final String METADATA_TEST_STRING = "<meta><metaset type='test'><test text='teststring&gt;&lt;?Title ≤?'/></metaset></meta>";
	Long folderId = 0L;
	Long objId = 0L;
	Long uploadId = 0L;
	long formatId = 0;
	long relationTypeId = 0;
	long aclid = 0;

	final File testdata =  new File("_testData_for_upload.xml");
	
	@BeforeClass
	public void setUp(){
		super.setUp();
		
		OSD_Test.createTestFile();
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
		objId = client.create("", "createTestObject", folderId);
		assert objId > 0 : "Got invalid objId "+objId;
	}

	@Test()
	public void emptySessionTicket() {
		Part[] parts = {
				new StringPart("command", "deletefolder"),			
				new StringPart("id", folderId.toString()),
				new StringPart("ticket", ""),	      
		};
		try {
			client.executeMethod(parts);
		}
		catch (Exception e) {
			assert e.getMessage().contains("ticket is too short"):
				"Instead of expected error message, I got:\n"+e.getMessage();
		}
	}

	@Test()
	public void wrongSessionInvalidTicketId() {
		Part[] parts = {
				new StringPart("command", "deletefolder"),			
				new StringPart("id", folderId.toString()),
				new StringPart("ticket", "123456789012345678901234567890foo@cmn_test"),	      
		};
		try {
            Integer reconnects = client.getReconnects();
			client.executeMethod(parts);
            if(reconnects.equals(client.getReconnects())){
			    assert false : "Invalid ticket accepted";
            }
		}
		catch (Exception e) {
			// none expected   ?
		}
	}

	@Test()
	public void wrongSessionTicketInvalidRepository() {
		Part[] parts = {
				new StringPart("command", "deletefolder"),			
				new StringPart("id", folderId.toString()),
				new StringPart("ticket", "1234567890123456789012345678901234567890foo@bar"),
		};
		try {
			client.executeMethod(parts);
		}
		catch (Exception e) {
			assert e.getMessage().contains("invalid repository 'bar'"):
				"Instead of expected error message, I got:\n"+e.getMessage();
		}
	}

	@Test()
	public void wrongSessionXmlTicketId() {
		Part[] parts = {
				new StringPart("command", "deletefolder"),			
				new StringPart("id", folderId.toString()),
				new StringPart("ticket", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><connection><ticket>6861b687-ab89-416f-a418-1c5b90d0e826@cmn_test</ticket></connection>"),
		};
		try {
			client.executeMethod(parts);
		}
		catch (Exception e) {
			assert e.getMessage().contains("invalid repository"):
				"Instead of expected error message, I got:\n"+e.getMessage();
		}
	}

	
	@Test(dependsOnMethods = {"createTestSimple"})
	public void lockTest() {
		boolean result = client.lock(objId);
		assert result : "Could not acquire lock on Object";
	}
	
	@Test(dependsOnMethods = {"lockTest"})
	public void getObjectsTest() {
		String list = client.getObjects(folderId, "head");
		assert list.indexOf("createTestObject") > 0 : "getObjects failed and returned: '"+list + "'";
		// TODO: call with empty versions param and with branch (as soon as there is a branched obj...)
	} 
	
	@Test(dependsOnMethods = {"lockTest"})
	public void setContentTest() {
		boolean result = client.setContent(testdata, "xml",objId);
		assert result : "Could not setContent to testData";
	}
			
	@Test(dependsOnMethods ={"setMetaTest"})
	public void getMetaTest(){
		String metadata = client.getMeta(objId);
		System.out.println("Metadata: "+metadata);
		assert metadata.contains("<test text=\"teststring&gt;&lt;?Title ≤?\"/></metaset></meta>") : "Could not retrieve correct Metadata";
	}
	
	@Test(dependsOnMethods = {"lockTest"})
	public void setMetaTest(){
		Boolean result = client.setMeta(objId, METADATA_TEST_STRING );
		assert result : "Could not setMeta";
	}
	
	@Test(dependsOnMethods = {"setContentTest"})
	public void getContentTest(){
		String content = client.getContent( objId );
		assert content.equals("<xml><data>This is just a test string</data></xml>"): "getcontent failed.";
	}
		
	@Test(dependsOnMethods = {"getContentTest"})
	public void unlockTest(){
		boolean result = client.unlock( objId );
		assert result : "Unlock object failed.";
	}
	
	// @Test(dependsOnMethods = {"unlockTest"})
	public void getFolderTest() {
		String result = client.getFolder(folderId);
		assert result.contains(
				String.format("<id>%d</id>", folderId)) : "Missing folder content.";	
	}
	
	@Test(dependsOnMethods = {"getContentTest", "getObjectsTest", "getMetaTest", "unlockTest"})
	public void deleteTestSimple(){
		boolean result = client.delete( objId );
		assert result : "Delete object failed.";
	}
		
	//--------------- create and delete obj with file upload:
	@Test(dependsOnMethods = {"createFolderTest"})
	public void createTest() {
		System.out.println("Uploading: "+testdata.getName());
		uploadId = client.create("", "MyFileUpload", testdata.getName(), "xml","text/xml", folderId);
		log.debug("uploadId: "+uploadId);
		assert uploadId > 0 : "Create object with fileupload failed.";	
	}
	
	@Test(dependsOnMethods = {"createTest"})
	public void deleteTest() {
		boolean result = client.delete(uploadId);
		assert result : "Could not delete uploaded file.";
		
		try{
			String xml = client.getContent(uploadId);
			assert false : "Object has not been deleted.\n"+xml;
		}
		catch (Exception e) {
			checkExceptionMessage(e, "deleteTest", "error.object.not.found");
		}
	}
	
	// deleteFolderTest currently works only with fresh folders without content
	@Test
	public void deleteFolderTest(){
		Long folderid = client.createFolder("testfolder"+Math.random(), 0L);
		boolean result = client.deleteFolder(folderid);
		assert result : "Could not delete folder!";
	}	
	
	// Tests for Format (CRUD)
	@Test
	public void createFormatTest() {
		formatId = client.createFormat("TestFormat",  "test",  "text/test", "TestFormat für ClientTests");
		assert formatId > 0 : "createFormat failed!";
	}
	
	@Test(dependsOnMethods ={"createFormatTest"})
	public void deleteFormatTest() {
		boolean result = client.deleteFormat(formatId);
		assert result : "could not delete Test-Format!";
	}
	
	@Test
	public void getFormatsTest() {
		String result = client.getFormats();
//		System.out.println(result);
		assert result.contains("<name>xml</name>"): "Empty format list returned:"+result;
		result = client.getFormats(null,"xml");
		assert result.contains("<contentType>application/xml</contentType>"): "Format with name xml not found.";
		// note: the XML-Format is currently the only one created by initializeDatabase. 
	}

	// Relations and RelationTypes
	@Test
	public void createRelationTypeTest() {
		relationTypeId = client.createRelationType("testrelation", "RelationType Entry for testing", true, false, null);
		assert relationTypeId > 0 : "Could not create RelationType.";
	}
	
	@Test (dependsOnMethods={"createRelationTypeTest"})
	public void deleteRelationTypeTest() {
		boolean result = client.deleteRelationType(relationTypeId);
		assert result : "Could not delete RelationType.";
	}
	
	@Test
	public void createAclTest() {
		AclTest at = new AclTest(config, client);
		at.createAclTest();
		aclid = at.getAclId();
		assert aclid > 0 : "Could not create testacl.";
	}
	
	@Test (dependsOnMethods={"createAclTest"})	
	public void deleteAclTest(){
		boolean result = client.deleteAcl(aclid);
		assert result : "Could not delete ACL.";
	}
	
	// ------ test delete error handling ---------

	/**
	 * Note: The fail*-Tests depend on a non-destructive persistence-unit (that is, one without
	 * "create" as its update-strategy. Otherwise, the sessions will be wiped from the database.
	 * 
	 */
	@Test
	public void failDeleteNonExistentOSDTest() {
		long fID = client.createFolder("testfolder" + Math.random(), 0L);
		assert fID > 0 : "Could not create folder!";

		boolean result;
		try {
			System.out.println("Uploading: " + testdata.getName());
			Long uID = client.create("", "MyFileUpload", testdata.getName(), "xml", "text/xml", fID);
			assert uID > 0 : "Create object with fileupload failed.";	

			// try to delete a nonexistent object
			try {
				result = client.delete(-1L);
				assert !result : "Could not delete uploaded file.";
			}
			catch (Exception e) {
				checkExceptionMessage(e, "failDeleteNonExistentOSDTest", "error.object.not.found");
			}
			finally { 
				result = client.delete(uID);
				assert result : "Could not delete uploaded file.";
			}
		} finally {

			result = client.deleteFolder(fID);
			assert result : "Could not delete folder!";
		}
	}	

	@Test
	public void failDeleteOSDWithPredecessorTest() {
		long fID = client.createFolder("testfolder" + Math.random(), 0L);
		assert fID > 0 : "Could not create folder!";

		boolean result;
		try {
			System.out.println("Uploading: " + testdata.getName());
			Long uID = client.create("", "MyFileUpload", testdata.getName(), "xml","text/xml", fID);
			assert uID > 0 : "Create object with fileupload failed.";	

			Long nextVersionID = client.versionAsLong(uID);
			assert nextVersionID > 0 : "Could not create next version.";
			try {
				result = client.delete(uID);
				assert !result : "uploaded file has been deleted (but shouldn't).";
			}
			catch (Exception e) {
				checkExceptionMessage(e, "failDeleteOSDWithPredecessorTest", "error.delete.has_descendants");
			}
			finally {
				result = client.delete(nextVersionID);
				assert result : "Could not delete uploaded file.";
				result = client.delete(uID);
				assert result : "Could not delete uploaded file.";
				try { 
					String content = client.getContent(uID);
					assert content.isEmpty():  "Content file has not been deleted.";
				}
				catch (Exception e) {
					checkExceptionMessage(e, "failDeleteOSDWithPredecessorTest", "error.object.not.found");
				}
			}
		} finally {
			result = client.deleteFolder(fID);
			assert result : "Could not delete folder!";
		}
	}
	
	// ------------ End of Testing -------------------
	
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}
	
	
}

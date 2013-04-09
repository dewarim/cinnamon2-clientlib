package safran.test;


import org.dom4j.*;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/*
 * Includes some tests from folderTest to setup everything.
 */

public class OSD_Test extends BaseTest {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	Long folderId = 0L;
	Long objId = 0L;
	Long newObjId = 0L;
	Long copyObjId = 0L;
	Long secondFolder = 0L;

	static final File testData =  new File("_testData_for_upload.xml");
    static final String testDataContent = "<xml><data>This is just a test string</data></xml>";
	private String name;
	
	public OSD_Test() {
		super();
	}

	public OSD_Test(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp(false);
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
	
	@Test(dependsOnMethods = {"createTestSimple", "setSysMetaTest"})
	public void getObject() throws DocumentException{
		String xml =  client.getObject(objId);
		Document ret = ParamParser.parseXmlToDocument(xml, null);
		assert ret != null : "document is null";
		String id = ret.valueOf("//objects/object/id");
		assert id.equals(objId.toString()) : String.format("wrong ID: '%s' - expected '%d'", id, objId);
		
		String res = ret.valueOf("//objects/object/name");
		assert res.equals("a rose by any other name") : String.format("wrong name: '%s' - expected 'a rose by any other name'", res);

		// find admin user id:
        String userXml = client.getUserByName("admin");
        Long adminId = Client.parseLongNode(userXml, "/users/user/id");

		Long ownerId =  Client.parseLongNode(xml, "//objects/object/owner/id");
		assert ownerId.equals(adminId) : String.format("wrong ownerId: '%d' - expected '%d'", adminId, ownerId);

		// fetch default-ACL:
	    String defaultAclId = getDefaultAclId();

		res = ret.valueOf("//objects/object/aclId");		
		assert res.equals(defaultAclId) : String.format("wrong aclId: '%s' - expected '%s'", res, defaultAclId);
		res = ret.valueOf("//objects/object/parentId");
		assert res.equals(folderId.toString()) : String.format("wrong parent: '%s' - expected '%s'", res, folderId);
		res = ret.valueOf("//objects/object/appName"); 
		assert res.equals("TestApp") : String.format("wrong appName: '%s' - expected 'TestApp'", res);
		res = ret.valueOf("//objects/object/procstate");
		assert res.equals("_tested") : String.format("wrong procstate: '%s' - expected '_tested'", res);
	}

    public String getDefaultAclId(){
        Document doc = ParamParser.parseXmlToDocument(client.getAcls(), null);
        // log.debug("getDefaultAclId:\n"+doc.asXML());
		Node defaultAcl = doc.selectSingleNode("//id[ parent::acl[child::sysName[text() = '_default_acl']]]");
        assert defaultAcl != null : "Did not find the expected default_acl node. XML: \n"+doc.asXML();
		return defaultAcl.getText();
    }

	@Test
	public void getInvalidObject()  {		
		try{
			@SuppressWarnings("unused")
			String xml = client.getObject(-1L);
			// this should result in an object_not_found exception.
			assert false : "client.getObject(-1) did not throw the expected exception.";			
		}
		catch(Exception ex){
			Document ret = ParamParser.parseXmlToDocument(ex.getMessage(), null);
			assert ret != null : "error message is null";
			Node error = ret.selectSingleNode("//error/message[string() = 'error.object.not.found']");
			assert error != null : "didn't receive expected result but:\n"+ret.asXML();
		}
	}
	
	@Test(dependsOnMethods = {"createTestSimple"})
	public void inheritParentAclTest(){
		Long subFolderId = client.createFolder("subTestFolder", folderId);
		String xml =  client.createAcl("subFolderTestAcl", "-");
		Long myAclId = Client.parseLongNode(xml, "/acls/acl/id");
		Map<String,String> fields = new HashMap<String,String>();
		fields.put("aclid", myAclId.toString());
		assert client.updateFolder(subFolderId, fields);		
		Long objId = client.create("","inheritParentAclTestObject", subFolderId );
		xml = client.getObject(objId);
		log.debug(xml);
		Long objAclId = Client.parseLongNode(xml, "/objects/object/aclId");
		assert objAclId.equals(myAclId) : String.format("Object's aclId '%d' is != the expected id '%d'.\n%s",
				objAclId, myAclId, xml);
		
		// on copy(), the new object should get the target folder's ACL
		Long sub2 = client.createFolder("subTestFolder2", folderId);
		Long copyId = client.copy(objId, sub2);
		xml = client.getObject(copyId);
		Long copyAclId = Client.parseLongNode(xml, "/objects/object/aclId");
		xml = client.getFolder(sub2);
		log.debug(xml);
		Long targetFolderAclId = Client.parseLongNode(xml, "/folders/folder/aclId");
		assert copyAclId.equals(targetFolderAclId): "Copied object does not have target folder's ACL.";
	}
	
	@Test(dependsOnMethods = {"createTestSimple", "lockTest"})
	public void setMetaTest() {
		boolean result = client.setMeta(objId, "<meta>foo</meta>");
		assert result : "SetMeta failed.";
		try{
			client.setMeta(objId, "no-ooxml");
			assert false : "Succeeded in setting non-xml metadata on object";
		}
		catch (Exception e) {
			assert e.getMessage().contains("<message>error.param.metadata</message>"):
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
		assert list.indexOf("createTestObject") > 0 : "getObjects failed and returned: "+list;
		// TODO: call with empty versions param and with branch (as soon as there is a branched obj...)
	} 

	@Test
	public void getObjectsHeadTest() {
		String versionname = "headtest";
		Long id = client.create("", versionname, folderId);
		assert id > 0 : String.format("Got invalid id '%d'", id);
		
		try {
			// create multiple versions of the test object
			Long version2Id = client.versionAsLong(id);
			assert version2Id != null : "no version 2 has been created";
			assert ! version2Id.equals(objId) : "no version 2 has been created";
	
			try {
				Long version3Id = client.versionAsLong(version2Id);
				assert version3Id != null : "no version 3 has been created";
				assert ! version3Id.equals(objId) : "no version 3  has been created";
				assert ! version3Id.equals(version2Id) : "no version 3 has been created";

				try {
					String list = client.getObjects(folderId, "head");
					int objectIndex = list.indexOf(versionname);
					assert !list.equals("") : "Found no head version of object";
					assert objectIndex > 0 : String.format(
							"getObjects failed and returned: '%s'", list); // 
					// TODO: call with empty versions param and with branch (as soon as there is a branched obj...)
					assert list.indexOf(versionname, objectIndex + 1) == -1 : "only _one_ object can be HEAD";
				} finally {
					boolean result = client.delete(version3Id);
					assert result : "could not delete object!";
				}
			} finally {
				boolean result = client.delete(version2Id);
				assert result : "could not delete object!";
			}
		}
		finally {
			boolean result = client.delete(id);
			assert result : "could not delete object!";
		}
	} 

//	@Test(dependsOnMethods = {"createTestSimple"})
	// is expected to fail as findObjectByName is not implemented.
	public void findObjectByNameTest(){
		String xml = client.findObjectByName(name);
		Document ret = parseXmlResponse(xml);
		assert ret != null : "document is null";
		String id = ret.valueOf("//objects/object/id");
		assert id.equals(objId.toString()) : String.format("wrong ID: '%s' - expected '%d'\n%s", id, objId, xml);
	}

//	@Test(dependsOnMethods = {"createTestSimple"})
	// is expected to fail as findObjectByName is not implemented.
	public void findObjectByInvalidName() {
		String xml = client.findObjectByName("invalid");
		Document ret = parseXmlResponse(xml);
		assert ret != null : "document is null";
		List<?> ids = ret.selectNodes("//objects/nil");
		assert ids.size() == 1 : "didn't receive expected result.";
	}
	
//	@Test(dependsOnMethods = {"createTestSimple"})
	// is expected to fail as findObjectByName is not implemented.
	public void findObjectByAmbiguousName(){
		Long objectID = client.create("", name, folderId);
		assert objectID > 0 : String.format("Got invalid objectID (%s)", objectID);
		
		String xml = client.findObjectByName("invalid");
		Document ret = parseXmlResponse(xml);
		assert ret != null : "document is null";
		
		List<?> ids = ret.selectNodes("//objects/nil");
		assert ids.size() == 1 : "didn't receive expected result.";
		
		// clean up
		boolean result = client.delete(objectID);
		assert result : "couldn't remove object";
	}
	
	@Test
	public void getObjectsById(){
		Long o1 = client.create("", "x1", folderId);
		Long o2 = client.create("", "x2", folderId);
		Long o3 = client.create("", "x3", folderId);
		
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("ids");
		root.addElement("id").addText(o1.toString());
		root.addElement("id").addText(o2.toString());
		root.addElement("id").addText(o3.toString());
		
		String query = String.format("<ids><id>%d</id><id>%d</id><id>%d</id></ids>",o1,o2,o3);
		String result = client.getObjectsById(query);
		Document redoc = parseXmlResponse(result);
		String xpath = String.format("/objects/object[id='%d']",o1);
		Node object = redoc.selectSingleNode(xpath);
		Node id = object.selectSingleNode("//id");
		String id1 = id.getText();
		assert id1.equals(o1.toString()) : "could not found object1 in output";
		String xpath2 = String.format("/objects/object[id='%d']",o2);
		assert redoc.selectSingleNode(xpath2) != null : "could not find obj2-id in output";
		String xpath3 = String.format("/objects/object[id='%d']",o3);
		assert redoc.selectSingleNode(xpath3) != null : "could not find obj3-id in output";
	}
	
//	@Test
	// is expected to fail because findObjectByName is not implemented.
	@SuppressWarnings("unchecked")
	public void findObjectByNameMultipleVersions() {
		String versionname = "headtest";
		Long id = client.create("", versionname, folderId);
		assert id > 0 : String.format("Got invalid id '%d'", id);
		try {
			// create multiple versions of the test object
			Long version2Id = client.versionAsLong(id);
			assert version2Id != null : "no version 2 has been created";
			assert ! version2Id.equals(objId) : "no version 2 has been created";
			try {
				Long version3Id = client.versionAsLong(version2Id);
				assert version3Id != null : "no version 3 has been created";
				assert ! version3Id.equals(objId) : "no version 3  has been created";
				assert ! version3Id.equals(version2Id) : "no version 3 has been created";
				try {
					String xml = client.findObjectByName(versionname);
					Document ret = parseXmlResponse(xml);
					assert ret != null : "document is null";
					
					List<DefaultElement> ids = ret.selectNodes("//objects/object/id");
					assert ids.size() == 1 : "didn't receive expected result.";
					String a = ids.get(0).valueOf("//objects/object/id");
					assert a.equals(version3Id.toString()) : String.format("wrong ID: '%s' - expected '%d'", a, version3Id);
				} finally {
					boolean result = client.delete(version3Id);
					assert result : "could not delete object!";
				}
			} finally {
				boolean result = client.delete(version2Id);
				assert result : "could not delete object!";
			}
		} finally {
			boolean result = client.delete(id);
			assert result : "could not delete object!";
		}
	} 
	
	@Test(dependsOnMethods = {"lockTest"})
	public void setContentTest() {
		boolean result = client.setContent(testData, "xml",objId);
		assert result : "Could not setContent to testData";
	}
	
	@Test(dependsOnMethods = {"setContentTest"})
	public void getContentTest(){
		String content = client.getContent( objId );
		assert content.contains("<xml><data>This is just a test string</data></xml>"): "getcontent failed.";
	}	
	
	@Test(dependsOnMethods = {"getContentTest"})
	public void copyTest() {
		secondFolder = client.createFolder("secondTestFolder"+Math.random(), folderId);
		copyObjId = client.copy(objId, secondFolder);
		assert copyObjId > 0L : "copy object failed!";
		assert ! copyObjId.equals(objId) : "Id of source and copied object are identical!";
		String content = client.getContent(objId);
		assert content.equals("<xml><data>This is just a test string</data></xml>") : "Source object was changed during copy";
		
		content = client.getContent(copyObjId);
		assert content.equals("<xml><data>This is just a test string</data></xml>") : "New Object is not an exact copy.";
		String copy = client.getObject(copyObjId);
		Document copyAsXml = ParamParser.parseXmlToDocument(copy, null);
		assert copyAsXml.selectSingleNode("//owner/id") != null : "copied object has no owner";
		log.debug("copy::\n"+copy);
		
		client.unlock(objId); // object must be unlocked for versioning.
		Long secondVersion= client.versionAsLong(objId);
		client.lock(objId);
		Long secondCopy = client.copy(secondVersion, secondFolder);
		copyAsXml = ParamParser.parseXmlToDocument(client.getObject(secondCopy), null);
		assert copyAsXml.selectSingleNode("//version").getText().equals("1") : "object version of copy is not 1";
		// TODO: implement client.getsysmeta(parentid) to check if copy is in the right place
		// (at one time, the object was moved and the copy put in the original object's place.)
		assert client.delete(secondVersion);
		log.debug("secondCopy::"+secondCopy);
		assert client.getObject(secondCopy).contains("<rootId>"+secondCopy+"</rootId>") 
		: "secondCopy contains invalid rootId.";
		assert client.delete(secondCopy);
	}
	
	@Test(dependsOnMethods = {"copyTest"})
	public void deleteCopyTest() {
		boolean result = client.delete(copyObjId);
		assert result : "could not delete copy!";
		String obj = client.getObjects(folderId);
		assert (obj.trim().length() > 0 && obj.contains(objId.toString())) : "original "+objId +" was deleted, too?!";
	}

    @Test(dependsOnMethods = {"setContentTest"})
	public void copyAllVersionsTest() {
		Long sourceFolder = client.createFolder("copyAllVersions_source", 0L);
        Long targetFolder = client.createFolder("copyAllVersions_target", 0L);

        // create a source object and 2 versions.
        Long original = client.create("", "sourceObject", sourceFolder);
        client.lock(original);
        client.setContent(testData, "xml", original);
        client.unlock(original);
        Long v2 = client.versionAsLong(original);
        client.lock(v2);
        client.setContent(testData, "xml", v2);
        client.unlock(v2);
        Long v3 = client.versionAsLong(v2);

        // copy source to target folder:
        String copies = client.copyAllVersions(v2, targetFolder);
        String targetFolderContent = client.getObjects(targetFolder, "all");
        Document copiesDoc = ParamParser.parseXmlToDocument(copies);
        List<Node> copiesWithContent = copiesDoc.selectNodes("//object/contentsize[text()='50']");
        assert copiesWithContent.size() == 2 : "copyAllVersions did not create 2 objects with content:\n"+copies;
        List<Node> copiesWithXmlFormat = copiesDoc.selectNodes("//object/format/sysName[text()='xml']");
        assert copiesWithXmlFormat.size() == 2 : "copyAllVersions did not set format correctly:\n"+copies;
        log.debug("copies:\n{} ",copies);
        log.debug("targetFolderContent:\n{}",targetFolderContent);
        checkDetailedDiff("copyAllVersions", copies, targetFolderContent);
	}


	@Test(dependsOnMethods = {"setContentTest", "copyTest"})
	public void setSysMetaTest(){
		// TODO: getSysMeta now supports several more params. Those should be tested, too.
		
		String parameter = "objtype";
		String value = "_default_objtype";
		Boolean result = null;
		try{
			result = client.setSysMeta(0L, parameter, value);
		}
		catch (Exception e) {
			// exception expected.
			log.debug("An expected exception occured",e);
		}
		assert result == null: "setsysmeta does not fail without given id or folderid."; 

//		parameter = "objtype";
//		value = "_default_objtype"; // TODO: momentan noch der default_type. Später den Typ selber erzeugen (und anschließend wieder löschen)
		result = client.setSysMeta(objId, parameter, value);
		assert result : "setsysmeta set objtype failed.";
		String strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";

		result = client.setSysMeta(objId,parameter, value );
		assert result : "setSysMeta (objtype) failed.";
		
		parameter = "name";
		value = "a rose by any other name";
		result = client.setSysMeta(objId, 	parameter, value );
		assert result : "setSysMeta (name) failed.";
		strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";
		
		parameter = "appname";
		value = "TestApp";
		result = client.setSysMeta(objId, 	parameter, value );
		assert result : "setSysMeta (appname) failed.";
		strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";
		
		parameter = "procstate";
		value = "_tested";
		result = client.setSysMeta( objId, 	parameter, value );
		assert result : "setSysMeta (procstate) failed.";
		strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";

        Long ownerId = client.createUser("-", "The owner","owner.foo", "newOwner", "none");
		parameter = "owner";
		value = ownerId.toString();
		result = client.setSysMeta( objId, 	parameter, value );
		assert result : "setSysMeta (owner) failed.";
		strResult = client.getSysMeta(objId, null, parameter);
		value = "owner.foo";
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";

        String user = client.getUserByName(config.getProperty("server.username"));
        Long userId = Client.parseLongNode(user, "/users/user/id");
		client.setSysMeta(objId, parameter, userId.toString());
        client.deleteUser(ownerId);

        String defaultAclId = getDefaultAclId();
		// TODO: create our own TestACL.
		
		parameter = "acl_id";
		value = defaultAclId;
		
		result = client.setSysMeta( objId, 	parameter, value );
		assert result : "setSysMeta (acl_id) failed.";
		strResult = client.getSysMeta(objId, null, parameter);
//		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";
		
		parameter = "parentid";
		value = folderId.toString();
		result = client.setSysMeta(objId, parameter, value);
		assert result : "setSysMeta (parentid) failed.";		
		strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";

        String langList = client.listLanguages();
        Long langId = Client.parseLongNode(langList, "/languages/language[sysName='zxx']/id");

		parameter = "language_id";
		value = String.valueOf(langId);
		assert client.setSysMeta(objId, parameter, value);
		strResult = client.getSysMeta(objId, null, parameter);
		assert value.equals(strResult) : "expected '" + value + "' but got '" + strResult + "'";
	}	

    @Test
    public void deleteWithRelationsTest(){
        /*
        Plan:
         1. delete an object with a leftprotected relation (expected: error)
         2. delete an object with a rightprotected relation (expected: error)
         3. delete an object with a left- rightprotected relation (expected: error)
         4. delete an object with an unprotected relation (expected: works)
         */
        Long related = client.create("<meta/>","related test object",folderId );
        client.createRelationType("unprotected", "-", false, false, null);
        client.createRelationType("leftprotected", "-", true, false, null);
        client.createRelationType("rightprotected", "-", false, true, null);
        client.createRelationType("bothProtected", "-", true, true, null);

        Long deleteMe = client.create("<meta/>", "to be deleted", folderId);
        Long leftprotected
                = Client.parseLongNode(client.createRelation("leftprotected", deleteMe, related), "//id");
        deleteRelatedObject(deleteMe);
        client.deleteRelation(leftprotected);

        Long rightprotected
                = Client.parseLongNode(client.createRelation("rightprotected", related, deleteMe), "//id");
        deleteRelatedObject(deleteMe);
        client.deleteRelation(rightprotected);

        Long bothProtected
                = Client.parseLongNode(client.createRelation("bothProtected", deleteMe, related), "//id");
        deleteRelatedObject(deleteMe);
        client.deleteRelation(bothProtected);

        Long unprotectedRelation
                = Client.parseLongNode(client.createRelation("unprotected", deleteMe, related), "//id");
        client.delete(deleteMe);
    }

    void deleteRelatedObject(Long id){
        try{
             client.delete(id);
        }
        catch(Exception ex){
            assert ex.getMessage().contains("cannot be deleted,") : "unexpected error: "+ex.getMessage();
        }
    }

	@Test(dependsOnMethods = {"getContentTest", "setSysMetaTest"})
	public void unlockTest(){
		boolean result = client.unlock( objId );
		assert result : "Unlock object failed.";
	}
	
	@Test(dependsOnMethods = {"unlockTest", "setSysMetaTest"})
	public void versionTest() {
		assert objId > 0 : "No valid objId given!";
		log.debug("create new version of object '"+objId+"'");
		newObjId = client.versionAsLong(objId);
		assert newObjId > 0 : "Could not create new version of testobject.";

        client.lock(objId);
        client.setContent(testData, "xml", objId);
        client.unlock(objId);

		String newObj = client.getObject(newObjId);
		assert newObj.contains("<version>2</version>") : "newly versioned object has wrong version number.";
		assert newObj.contains("<latestHead>true</latestHead>") : "new Object is not latestHead";
		log.debug("###***\n"+newObj);
		assert newObj.contains("<latestBranch>true</latestBranch>") : "new Object is not latestBranch";
		Long secondVersionId = client.versionAsLong(newObjId);
		String secondVersion = client.getObject(secondVersionId);
		assert secondVersion.contains("<version>3</version>") : "2nd new Object has wrong version number";
		assert secondVersion.contains("<latestHead>true</latestHead>") : "2nd new Object is not latestHead";
		assert secondVersion.contains("<latestBranch>true</latestBranch>"):" 2nd new Object is not latestBranch";
		newObj = client.getObject(newObjId);
		assert newObj.contains("<latestHead>false</latestHead>") : "newObj is wrongly reported as latestHead";
		assert newObj.contains("<latestBranch>false</latestBranch>") : "newObj is wrongly reported as latestBranch";
		assert newObj.contains("<isoCode>zxx</isoCode></language>") : "expected language 'und'"; // is set to zxx by setSysMetaTest
		assert ! newObj.contains("<sysName>xml</sysName>") : "object-2 has format - version should not copy format";
        assert ! newObj.contains("<contentsize>") : "object-2 has content - version should not copy content";
        
		log.debug("check branch / latesthead");
		log.debug(client.getObject(newObjId));
		
		client.delete(secondVersionId);
		newObj = client.getObject(newObjId);
		assert newObj.contains("<latestHead>true</latestHead>");
		assert newObj.contains("<latestBranch>true</latestBranch>");
		
		log.debug("deleted secondVersion");
		log.debug(client.getObject(newObjId));
//		log.debug("delete object with descendants (should fail:)");
//		assert ! client.delete(objId) : "Managed to delete object with descendants.";
	}

	@Test(dependsOnMethods = {"unlockTest", "setContentTest", "versionTest"})
	public void versionWithFormatTest() {
		String formatName = "test" + Math.random();
		long testFormatId = client.createFormat(formatName, "tst", "test/test", "test format");
		assert testFormatId > 0 : "could not create format";
		
		try {
			assert objId > 0 : "No valid objId given!";
			String obj = client.getObject(objId);
			
			String xmlFormat = client.getFormats(null, "xml");
			Document doc = ParamParser.parseXmlToDocument(xmlFormat, null);
			// There should be just one format in this test environment:
			String xmlFormatId = doc.selectSingleNode("/formats/format/id").getText(); 
			assert obj.contains(String.format("<format><id>%s</id>", xmlFormatId))
				: String.format("could not find formatId %s in server result %s", xmlFormatId,xmlFormat);
			
			long newObjId = client.versionAsLong(objId, formatName );
			assert newObjId > 0 : "Could not create new version of testobject.";
			try {
				String newObj = client.getObject(newObjId);
				assert newObj.contains(String.format("<format><id>%s</id>", testFormatId));

				String newFormatName = "test" + Math.random();
				long newFormatId = client.createFormat(newFormatName, "tst", "test/test", "test format");
				assert newFormatId > 0 : "could not create format";
				try {
					long versionedObjIdWithNewFormat = client.versionAsLong(objId, newFormatName );
					try {
						assert versionedObjIdWithNewFormat > 0 : "Could not create new version of testobject.";
						String versionedObjWithNewFormat = client.getObject(versionedObjIdWithNewFormat);
						assert versionedObjWithNewFormat.contains(String.format("<format><id>%s</id>", newFormatId));
					} finally {
						assert client.delete(versionedObjIdWithNewFormat) : "could not delete versioned object " + newObjId;
					}
				} finally {
					assert client.deleteFormat(newFormatId) : "could not delete new format";
				}
				
			} finally {
				assert client.delete(newObjId) : "could not delete versioned object " + newObjId;
			}
		} finally {
			assert client.deleteFormat(testFormatId) : "could not delete format";
		}
	}

	@Test(dependsOnMethods = {"createFolderTest", "createTestSimple", "setSysMetaTest"})
	public void createOSDWithDefaultObjType() {
		String objType = "_default_objtype";
		Long objectID = client.create("", "createTestObject", folderId, objType);
		assert objectID > 0 : "Got invalid objId " + objectID;
		
		try {
			String result = client.getSysMeta(objectID, null, "objtype");
			assert result.equals(objType) : String.format(
					"Returned objtype (%s) differs from expected objtype (%s)",
					result, objType);
		} finally {
			boolean res = client.delete(objectID);
			assert res : String.format("Could not delete OSD '%s'", objectID);
		}
	}

	@Test(dependsOnMethods = {"createFolderTest", "createTestSimple", "setSysMetaTest", "createOSDWithDefaultObjType"})
	public void createOSDWithFooObjType() {
		String objType = "foo";
		Long objTypeID = client.createObjectType(objType, "Test Type");
		assert objTypeID > 0 : String.format("Could not create object type '%s'.", objType);
		
		try {
			Long objectID = client.create("", "createTestObject", folderId,
					objType);
			assert objectID > 0 : "Got invalid objId " + objectID;
			try {
				String result = client.getSysMeta(objectID, null, "objtype");
				assert result.equals(objType) : String.format("Returned objtype (%s) differs from expected objtype (%s)", result, objType);
			} finally {
				boolean res = client.delete(objectID);
				assert res : String.format("Could not delete OSD '%s'", objectID);
			}
		} finally {
			boolean res = client.deleteObjectType(objTypeID);
			assert res : String.format("Could not delete object type '%s'.", objTypeID);
		}
	}

//	List<ObjectSystemData> findAllByName(String name);
//	List<ObjectSystemData> findAllByNameAndParentID(String name, Long parentID);
//	List<ObjectSystemData> findAllByNameAndParentIDAndLatestHead(String name, Long parentID, boolean latestHead);
//	List<ObjectSystemData> findAllByNameAndParentIDAndLatestBranch(String name, Long parentID, boolean latestBranch);
//	List<ObjectSystemData> findAllByNameAndLatestHead(String name, boolean latestHead);
//	List<ObjectSystemData> findAllByNameAndLatestBranch(String name, boolean latestBranch);
	
//	@Test(dependsOnMethods = {"createTestSimple"})
//	public void findAllObjectsByName() throws DocumentException {
//	}
	
//	@Test
//	public void findAllObjectsByNameAndAllVersions() throws DocumentException {
//		throw new DocumentException();
//	}

//	@Test
//	public void findAllObjectsByNameAndHeadVersions() throws DocumentException {
//		throw new DocumentException();
//	}

//	@Test
//	public void findAllObjectsByNameAndBranchVersions() throws DocumentException {
//		throw new DocumentException();
//	}
	
//	@Test(dependsOnMethods = {"createTestSimple"})
//	public void findAllObjectsByNameAndParentID() throws DocumentException {
//		throw new DocumentException();
//	}

//	@Test(dependsOnMethods = {"createTestSimple"})
//	public void findAllObjectsByNameAndParentIDAndAllVersions() throws DocumentException {
//		throw new DocumentException();
//	}

//	@Test
//	public void findAllObjectsByNameAndParentIDAndHeadVersions() throws DocumentException {
//		throw new DocumentException();
//	}

//	@Test
//	public void findAllObjectsByNameAndParentIDAndBranchVersions() throws DocumentException {
//		throw new DocumentException();
//	}

	@Test(dependsOnMethods = {"unlockTest", "deleteCopyTest", "versionTest", "versionWithFormatTest"})
	public void deleteTest() {
		boolean newVersionDeleted = client.delete(newObjId);
		assert newVersionDeleted : "could not delete newVersion";
		boolean result = client.delete(objId);
		assert result : "could not delete copy!";
	}


	// TODO: test für nichtexistenten Objtype
	
	// ----- cleaning up -------
	
	// deleteFolderTest currently works only with fresh folders without content
	@Test(dependsOnMethods = {"unlockTest", "deleteTest"})
	public void deleteFolderTest(){
		Long folderid = client.createFolder("testfolder"+Math.random(), 0L);
		boolean result = client.deleteFolder(folderid);
		assert result : "Could not delete folder!";
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


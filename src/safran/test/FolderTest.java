package safran.test;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.PermissionName;
import utils.ParamParser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FolderTest extends BaseTest{

	Long folderId = 0L;
	String foldername = "testFolder";
	Long subFolderId = 0L;
	
	public FolderTest() {
		super();
	}
	
	public FolderTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

	
	@Test
	public void createFolderTest(){
		folderId = client.createFolder(foldername, 0L);
		assert folderId > 0 : "Could not create folder!";
	}
		
	@Test(dependsOnMethods = {"createFolderTest"})
	public void createDuplicateFolderTest(){
		Long folderId2 = null;
		try{
			folderId2 = client.createFolder(foldername, 0L);
		}
		catch (Exception e) {
			// do nothing, we expect an exception.
			log.debug("",e);
		}
		assert folderId2 == null: "Successfully created duplicate folder. ";
	}
	
	@Test(dependsOnMethods = {"createFolderTest"})
	public void getFolderTest() {
		String result = client.getFolder(folderId);
		String expected = String.format("<id>%d</id><name>%s</name>", folderId, foldername);
		assert result.contains(expected) : "Missing folder content:"+result;
		Long folderId2 = client.createFolder("test1", folderId);
		result = client.getFolder(folderId2);
//		assert false:result;
		assert result.contains(expected) : "subfolder does not contain parent folder:\n"+result;
	}

	@Test(dependsOnMethods = {"createFolderTest"})
	public void getFolderByPathTest() {
		String result = client.getFolderByPath("root/"+foldername);
		log.debug("getFolderByPath:"+result);
		String expected = String.format("<id>%d</id><name>%s</name>", folderId, foldername);
		assert result.contains(expected) : "getFolderByPath failed with result: "+result;
		assert result.contains("<name>root</name>"):"Could not find root-folder in output.:\n"+result;
	}
	
	// deleteFolderTest currently works only with fresh folders without content
	@Test(dependsOnMethods = {"getFolderTest", "getFolderByPathTest", "copyFolderTest"})
	public void deleteFolderTest(){
		Long deleteMe = client.createFolder("testfolder"+Math.random(), 0L);
		boolean result = client.deleteFolder(deleteMe);
		assert result : "Could not delete folder!";
	}	

	@Test(dependsOnMethods = {"createFolderTest"})
	public void getSubfolderTest() {
		String subfolderName = "testSubFolder"+Math.random();
		Long folderId = client.createFolder("testFolder"+Math.random(), 0L);
		subFolderId = client.createFolder(subfolderName, folderId);
		
		String result = client.getSubfolders(folderId);
		String expected = String.format("<id>%d</id><name>%s</name>", subFolderId, subfolderName);
		assert result.contains(expected);
	}

    @Test(dependsOnMethods = {"createFolderTest"})
	public void getFoldersById(){
		Long f1 = client.createFolder("f1", folderId);
		Long f2 = client.createFolder("f2", folderId);
		Long f3 = client.createFolder("f3", folderId);

		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("ids");
		root.addElement("id").addText(f1.toString());
		root.addElement("id").addText(f2.toString());
		root.addElement("id").addText(f3.toString());

		String query = String.format("<ids><id>%d</id><id>%d</id><id>%d</id></ids>",f1,f2,f3);
		String result = client.getFoldersById(query);
		Document redoc = parseXmlResponse(result);
		String xpath = String.format("/folders/folder[id='%d']",f1);
		Node object = redoc.selectSingleNode(xpath);
		Node id = object.selectSingleNode("//id");
		String id1 = id.getText();
		assert id1.equals(f1.toString()) : "could not found folder1 in output";
		String xpath2 = String.format("/folders/folder[id='%d']",f2);
		assert redoc.selectSingleNode(xpath2) != null : "could not find folder2-id in output";
		String xpath3 = String.format("/folders/folder[id='%d']",f3);
		assert redoc.selectSingleNode(xpath3) != null : "could not find folder3-id in output";
	}

    @Test(dependsOnMethods =  {"getSubfolderTest"})
    public void hasChildrenTest(){
        Long folderId = client.createFolder("testfolder"+Math.random(), 0L);
        String folderXml = client.getFolder(folderId);
        assert folderXml.contains("<hasChildren>false</hasChildren>") :
                "hasChildren on empty folder failed.\n"+folderXml;

		subFolderId = client.createFolder("subTestFolder", folderId);
        folderXml = client.getFolder(folderId);
        assert folderXml.contains("<hasChildren>true</hasChildren>") :
                "hasChildren on non-empty folder failed.\n"+folderXml;          
    }

	@Test(dependsOnMethods = {"createFolderTest", "getFolderTest"})
	public void updateFolderTest() {
		String updatedFolderName = "updatedFolder";
		Map<String, String> fields = new HashMap<String, String>();
		fields.put("name", updatedFolderName);
		Boolean result = client.updateFolder(folderId, fields);
		assert result : "could not update folder.";

        // #1716: prevent setting folder.parent = folder.id
        fields.clear();
        fields.put("parentid", folderId.toString());
        try{
            result = client.updateFolder(folderId, fields);
            assert ! result : "managed to set folder's parent to itself.";
        }
        catch (Exception ex){
            assert ex.getMessage().contains("error.illegal_parent_id") : "unexpected exception:"+ex.getLocalizedMessage();
        }
        // end #1716 test

		String xml =  client.createAcl("test_acl_#1", "-");	
		Document doc = ParamParser.parseXmlToDocument(xml, null);
		Long aclId = ParamParser.parseLong(doc.selectSingleNode("/acls/acl/id").getText(), null);
		fields.clear();
		fields.put("aclid", aclId.toString());
		result = client.updateFolder(folderId, fields);
		assert result : "updateFolder (folder-aclid) failed.";

        subFolderId = client.createFolder("testFolder---"+folderId, 0L);
		fields.clear();
		fields.put("parentid", subFolderId.toString());
        result = client.updateFolder(folderId, fields);
		assert result : "updateFolder(folder-parentid) failed.";

        // # 1722 folder may be moved inside one of its own subfolders
        Long subFolderId2 = client.createFolder("testFolder---"+subFolderId, 0L);
        Long subFolderId3 = client.createFolder("testFolder---"+subFolderId2, subFolderId2);
        fields.put("parentid", subFolderId3.toString());
        try{
            client.updateFolder(subFolderId2, fields);
            assert false : "Managed to move a folder into its child folder.";
        }
        catch (Exception e){
            assert e.getMessage().contains("error.illegal_parent_id") : "Received unexpected exception: "+e.getMessage();
        }
        // end #1722-test

		String strResult = client.getFolder(folderId);
		String expected = String.format("<id>%d</id><name>%s</name>",
				folderId, updatedFolderName);
		assert strResult.contains(expected) : "Update failed.";
		
		// set folder to previous values
		fields = new HashMap<String, String>();
		fields.put("name", foldername);		
		result = client.updateFolder(folderId, fields);
		assert result : "could not reset folder.";
		
		fields.clear();
		fields.put("metadata", "<meta>test</meta>");
		result = client.updateFolder(folderId, fields);
		assert result : "Could not set metadata on folder.";
		String meta = client.getFolderMeta(folderId);
		assert meta.contains("<meta>test</meta>"): "Instead of the metadata I received: \n"+meta;
		
		// submit broken metadata (ie, not XML)
		fields.clear();
		fields.put("metadata", "not-xml");
		try{
			client.updateFolder(folderId, fields);
			assert false : "Succeeded in setting non-xml metadata on folder";
		}
		catch (Exception e) {
			assert e.getMessage().contains("<code>error.param.metadata</code>"):
				"Instead of expected error message, I got:\n"+e.getMessage();
		}
	}

    @Test(dependsOnMethods = {"updateFolderTest"})
    public void copyFolderTest(){
        Long parentId = client.createFolder("copyFolderTest", 0L);
        //----------------------------------------------------------------
        // copy empty folder
        //----------------------------------------------------------------
        Long sourceId = client.createFolder("sourceFolder", parentId);
        Long targetId = client.createFolder("targetFolder", parentId);
        String result = client.copyFolder(sourceId, targetId, "head", false);
        log.debug("copy empty folder result: "+result);
        Document doc = ParamParser.parseXmlToDocument(result);
        Node errors = doc.selectSingleNode("//errors/folders/folder");
        assert errors == null : "Copy empty folder failed. "+result;

        // target folder should contain a copy of source:
        result = client.getSubfolders(targetId);
        doc = ParamParser.parseXmlToDocument(result);
        Node folder = doc.selectSingleNode("/folders/folder[name='sourceFolder']");
        assert folder != null : "Could not find copy of sourceFolder in targetFolder:\n"+result;

        // ---------------------------------------------------------------
        // copy folder with a folder inside
        // ---------------------------------------------------------------
        Long insideFolder = client.createFolder("insideFolder", sourceId);
        Long target2 = client.createFolder("target2", parentId); // less work than deleting test folders.
        result = client.copyFolder(sourceId, target2, "head", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkFolderCount(doc, 2);

        result = client.getFolderByPath("/copyFolderTest/target2/sourceFolder/insideFolder");
        doc =   ParamParser.parseXmlToDocument(result);
        assert doc.selectSingleNode("/folders/folder[name='insideFolder']") != null :
                "copyFolder did not result in expected folder structure.\n"+result;

        //----------------------------------------------------------------
        // copy folder with a folder and an object inside
        //----------------------------------------------------------------
        Long sourceObject = client.create("", "sourceObject", sourceId);
        Long target3 = client.createFolder("target3", parentId);
        result = client.copyFolder(sourceId, target3, "head", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkFolderCount(doc, 2);
        checkObjectCount(doc, 1);

        //----------------------------------------------------------------
        // copy folder with a folder and an object with
        // two versions inside
        //----------------------------------------------------------------
        Long version2 = client.versionAsLong(sourceObject);
        // state: there is now v1 + v2 in source folder
        Long target4 = client.createFolder("target4", parentId);
        result = client.copyFolder(sourceId, target4, "head", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc,1); // only copy head
        Long target5 = client.createFolder("target5", parentId);
        result = client.copyFolder(sourceId, target5, "all", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 2); // should contain both versions

        //----------------------------------------------------------------
        // copy folder with a folder and an object with
        // two versions and a branch inside
        //----------------------------------------------------------------
        client.version(sourceObject);
         // state: there is now v1 + v1.1 + v2 in source folder
        Long target6 = client.createFolder("target6", parentId);
        result = client.copyFolder(sourceId, target6, "head", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 1);
        Long target7 = client.createFolder("target7", parentId);
        result = client.copyFolder(sourceId, target7, "all", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 3);
        Long target8 = client.createFolder("target8", parentId);
        result = client.copyFolder(sourceId, target8, "branch", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 1); // copy only the latestBranch.

        //----------------------------------------------------------------
        // copy folder with a folder and an object with
        // two versions and a branch inside - and the root object in
        // another folder.
        //-----------------------------------------------------------------
        client.version(version2);
        // state: there is now v1 + v1.1 + v2 + v3 in source folder
        // move root to "copyFolderTest":
        client.setSysMeta(sourceObject, "parentid", parentId.toString());
        // state: there is now v1.1 + v2 + v3 in source folder and v1 is in parent of source folder.
        Long target9 = client.createFolder("target9", parentId);
        result = client.copyFolder(sourceId, target9, "all", false);
        doc = ParamParser.parseXmlToDocument(result);
        // target folder should contain all versions, including the root
        // object from another folder.
        checkObjectCount(doc, 4);

        //----------------------------------------------------------------
        // copy folder with an OSD which has content
        //----------------------------------------------------------------
        Long contentFolder = client.createFolder("contentFolder", sourceId);
        Long contentObject = client.create("", "hasContent", contentFolder);
        client.lock(contentObject);
        File testContentFile = OSD_Test.createTestFile();
        assert client.setContent(testContentFile, "xml", contentObject);
        client.unlock(contentObject);

        // check if content is really available:
        String content = client.getContent(contentObject);
        assert content.trim().equals(OSD_Test.testDataContent);

        Long target10 = client.createFolder("target10_with_content", parentId);
        result = client.copyFolder(contentFolder, target10, "head", false );
        doc = ParamParser.parseXmlToDocument(result);
        String contentCopyId = doc.selectSingleNode("//objects/id").getText();
        content = client.getContent(ParamParser.parseLong(contentCopyId, null)).trim();
        assert content.equals(OSD_Test.testDataContent) : "copy of object does not contain expected testData.\n"+content;

        //----------------------------------------------------------------
        // copy folder with an OSD which has custom metadata
        //----------------------------------------------------------------
        String metaContent = "<meta tested='true'/>";
        client.lock(contentObject);
        client.setMeta(contentObject, metaContent);
        client.unlock(contentObject);
        Long target11 = client.createFolder("target11_with_metadata", parentId);
        result = client.copyFolder(contentFolder, target11, "all", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 1);
        String metaCopyId = doc.selectSingleNode("//objects/id").getText();
        String metaCopy = client.getMeta(ParamParser.parseLong(metaCopyId, null));
        assert metaCopy.equals(metaContent) : "custom metadata was not copied correctly by copyFolder";

        //----------------------------------------------------------------
        // copy folder with an OSD which is not readable (because of ACL)
        //----------------------------------------------------------------
        Long johnDoe = client.createUser("Normal test user", "The Norm", "norm", "norm", "none");
        Client johnClient = new Client(config.getProperty("server.url"), "norm", "norm",
                config.getProperty("default_repository"));        
        Long target12 = client.createFolder("target12_for_normal_user", parentId);
        log.debug(String.format("%d %d", contentFolder, target12));
        johnClient.connect();

        // first should run without anything happening.
        result = johnClient.copyFolder(contentFolder, target12, "all", false);
        log.debug("normal user copyFolder result: "+result);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc, 0);
        checkFolderCount(doc, 0);
        checkErrorCount(doc, 1); // should have 1 for the forbidden folder

        Long aclUserGroup = Client.parseLongNode(client.getGroupsOfUser(johnDoe, false), "//id");
        Long defaultAcl = Client.parseLongNode(client.getFolder(contentFolder), "//aclId");
        Long defaultGroupEntry = Client.parseLongNode(client.addGroupToAcl(aclUserGroup, defaultAcl), "//id");
        Long browsePermissionNameId = getPermissionNameId(PermissionName.BROWSE_FOLDER);
        client.addPermissionToAclEntry(browsePermissionNameId, defaultGroupEntry);
        Long createInsideFolder = getPermissionNameId(PermissionName.CREATE_OBJECT);
        client.addPermissionToAclEntry(createInsideFolder, defaultGroupEntry);
        Long createFolder = getPermissionNameId(PermissionName.CREATE_FOLDER);
        client.addPermissionToAclEntry(createFolder, defaultGroupEntry);

        // copying should result in one error message because the contentObject could not be copied.
        result = johnClient.copyFolder(contentFolder, target12, "all", false);
        log.debug("normal user copyFolder result: "+result);
        doc = ParamParser.parseXmlToDocument(result);
        checkFolderCount(doc, 1); // should be possible now to copy the folder.
        checkObjectCount(doc, 0); // no copy without read_object PermissionNames.
        checkErrorCount(doc, 1);

        Long sourceFolder = client.createFolder("non-readable-root-test-source", parentId);
        Long targetFolder = client.createFolder("non-readable-root-test-target", parentId);
        Long unreadableRoot = client.create("", "unreadableRoot", sourceFolder);
        client.version(unreadableRoot);
        String blockingAcl = client.createAcl("sun blocker", "Acl which prevents user from reading root OSD");
        Long blockingAclId = Client.parseLongNode(blockingAcl, "//acl/id");
        client.setSysMeta(unreadableRoot, "acl_id", blockingAclId.toString());
        result = johnClient.copyFolder(sourceFolder, targetFolder, "all", false);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc,0); // cannot copy object without root.
        checkFolderCount(doc,1); // should copy empty folder
        checkErrorCount(doc,2); // unreadable objects should create errors

        // use the setup from above, but with a readable OSD and croakOnError
        Long anotherObject = client.create("", "croakOnErrorTest", sourceFolder);
        Long anotherFolder = client.createFolder("targetFolder-for-croakOnErrorTest", parentId);
        result = johnClient.copyFolder(sourceFolder,anotherFolder,"all", true);
        doc = ParamParser.parseXmlToDocument(result);
        checkObjectCount(doc,0); // the unreadable OSD will stop copyFolder before the readable one can be copied
        checkFolderCount(doc,1); // folder will be copied before objects make process croak
        checkErrorCount(doc,1); // should stop on first error
    }

    void checkFolderCount(Document doc, Integer expected){
        @SuppressWarnings("unchecked")
        List<Node> countFolders = doc.selectNodes("//copyResult/folders/id");
        log.debug("check folder count - expecting: "+expected);
        assert countFolders.size() == expected :
                "copying "+expected+" folder(s) did not result in the same number of new folders.\n"+doc.asXML();
    }

    void checkErrorCount(Document doc, Integer expected){
        @SuppressWarnings("unchecked")
        List<Node> countErrors= doc.selectNodes("/copyResult/errors//id");
        log.debug("check error count - expecting: "+expected);
        assert countErrors.size() == expected :
                "copying "+expected+" folder(s) did not result in the expected number of error messages.\n"+doc.asXML();
    }


    /*
<copyResult><folders/><objects/><errors><folders><folder><id>163</id>
<message>error.PermissionName.missing._browse_folder</message></folder></folders><objects/></errors></copyResult>

     */

    void checkObjectCount(Document doc, Integer expected){
        @SuppressWarnings("unchecked")
        List<Node> countObjects = doc.selectNodes("//copyResult/objects/id");
        log.debug("check object count - expecting: "+expected);
        assert countObjects.size() == expected :
                "copying "+expected+" object(s) did not result in the same number of new objects.\n"+doc.asXML();
    }

    @Test
    public void zipFolderTest(){
        //----------------------------------------------------------------
        // copy folder with an OSD which has content
        //----------------------------------------------------------------
        Long zipFolder = client.createFolder("zipFolder", 0L);
        Long contentFolder = client.createFolder("contentFolder", zipFolder);
        Long contentObject = client.create("", "hasContent", contentFolder);
        client.lock(contentObject);
        File testContentFile = OSD_Test.createTestFile();
        assert client.setContent(testContentFile, "xml", contentObject);
        client.unlock(contentObject);
        File zippedFolder = client.zipFolder(zipFolder);
        assert zippedFolder.length() > 0 : "Received zip archive with length 0.";
        
        Long targetFolder = client.createFolder("zipOutput", 0L);
        Long zipFolderToObject = client.zipFolderToObject(zipFolder, null, null, 
                targetFolder, null, "<meta><metaset type=\"testmetaset\">nothing</metaset></meta>");
        log.debug("zipFolderToObject result id: "+zipFolderToObject);
        File zipFromObject = client.getContentAsFile(zipFolderToObject);
        log.debug("first zip: "+zippedFolder.getAbsolutePath());
        log.debug("second zip: "+zipFromObject.getAbsolutePath());
        assert zippedFolder.length() - zipFromObject.length() == 0 : "directly downloaded zip and zip from object differ in size";
        // TODO: read zip file's content to determine if it is correct.
        // TODO: improve this test by using latestHead/latestBranch params on zipFolder
        // TODO: test with objects that are not visible to a normal user (acl prevents browse folder / object)
        // (those 3 points should work ok as far as manual testing and code review indicates)        
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
	
}


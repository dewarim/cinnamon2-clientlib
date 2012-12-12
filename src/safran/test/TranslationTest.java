package safran.test;

import org.custommonkey.xmlunit.Diff;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import safran.Client;
import utils.ParamParser;

import java.io.IOException;
import java.util.Properties;

/**
 * Test cases:
 * <ul>
 * <li>call without invalid parameters:
 * <ol>
 * <li>source_id</li>
 * <li>object_relation_type_id</li>
 * <li>root_relation_type_id</li>
 * <li>attribute [results in null result, which is interpreted as missing object tree; no test]</li>
 * <li>attribute_value [results in null result, which is interpreted as missing object tree; no test]</li>
 * <li>target_folder_id</li>
 * </ol>
 * </li>
 * <li>call with a source tree of size 1 and no target tree
 * (with/without existing translation metadata)</li>
 * <li>call with a source tree of size &gt; 1 and no target tree</li>
 * <li>call with a source tree of size 1 and a valid target tree</li>
 * <li>call with a source tree of size &gt; 1 and a valid target tree</li>
 * <li>call with a source tree with a branch</li>
 * <li>call with a source tree with several objects and an incomplete target tree</li>
 * <li>call with a source tree with branches and an (in)complete target tree</li>
 * <li>call with a source tree and a target tree with another relation of
 * a different type (tested implicitly by existingMetadata)</li>
 * <p/>
 * </ul>
 */
public class TranslationTest extends BaseTest {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    Long folderId = 0L;
    Long objectRelationTypeId = 0L;
    Long rootRelationTypeId = 0L;
    final String existingMetadata = "<meta><metaset type='translation_extension' id='213'><target><relation_type_id>0</relation_type_id>" +
            "<attribute_value>*</attribute_value></target></metaset></meta>";


    public TranslationTest() {
        super();
    }

    public TranslationTest(Properties config, Client client) {
        this.config = config;
        this.client = client;
    }

    @BeforeClass
    public void setUp() {
        super.setUp();
        OSD_Test.createTestFile();
        OSD_Test osdT = new OSD_Test(config, client);
        osdT.createFolderTest();
        folderId = osdT.getFolderId();

        // create test object relation
        objectRelationTypeId = client.createRelationType("objectTranslationRelationType", "-",
                true, false, null);

        // create test root relation
        rootRelationTypeId = client.createRelationType("rootTranslationRelationType", "-",
                true, false, null);
    }

    @Test
    public void invalidSourceIdTest() {
        try {
            client.createTranslation("/meta/object/id", "0", 0L, rootRelationTypeId, objectRelationTypeId);
            assert false : "createTranslation succeeded without valid sourceId";
        } catch (Exception e) {
            assert e.getMessage().contains("error.object.not.found") :
                    "received unexpected error message\n" + e.getMessage();
        }
    }

    @Test
    public void invalidRootRelationTypeTest() {
        try {
            Long sourceId = client.create("", "sourceTestObject", folderId);
            log.debug("created object with id " + sourceId);
            client.createTranslation("/meta/object/id", sourceId.toString(),
                    sourceId, 0L, objectRelationTypeId);
            log.debug("created Translation");
            assert false : "createTranslation succeeded without valid rootRelationTypeId";

        } catch (Exception e) {
            assert e.getMessage().contains("error.param.object_relation_type_id") :
                    "received unexpected error message\n" + e.getMessage();
        }
    }

    @Test
    public void invalidObjectRelationTypeTest() {
        try {
            Long sourceId = client.create("", "sourceTestObject", folderId);
            log.debug("created object with id " + sourceId);
            client.createTranslation("/meta/object/id", sourceId.toString(),
                    sourceId, rootRelationTypeId, 0L);
            log.debug("created Translation.");
            assert false : "createTranslation succeeded without valid objectRelationTypeId";
        } catch (Exception e) {
            assert e.getMessage().contains("error.root_relation_type.not_found") :
                    "received unexpected error message\n" + e.getMessage();
        }
    }

    @Test
    public void invalidTargetFolderTest() {
        try {
            Long sourceId = client.create("", "sourceTestObject", folderId);
            client.createTranslation("/meta/object/id", sourceId.toString(),
                    sourceId, objectRelationTypeId, rootRelationTypeId, 0L);
            assert false : "createTranslation succeeded without valid target_folder_id";
        } catch (Exception e) {
            assert e.getMessage().contains("error.target_folder.not_found") :
                    "received unexpected error message\n" + e.getMessage();
        }
    }

    @Test
    public void sourceTreeOf1AndNoTargetTest() {
        // without existing translation metadata
        Long sourceId = client.create("", "sourceTestObject", folderId);
        Long targetId = client.createTranslation("-", "-", sourceId, objectRelationTypeId, rootRelationTypeId);
        String xml = client.getMeta(targetId);

        String newMetadata = getTranslationMetadataString(objectRelationTypeId, "-");
        checkDiff("createTranslation", newMetadata, xml);
        client.delete(targetId);

        // with existing translation metadata
        Long withMetadata = client.create(existingMetadata,
                "sourceWithTranslationMetadata", folderId);
        targetId = client.createTranslation("-", "-", withMetadata, objectRelationTypeId, rootRelationTypeId);
        newMetadata = combineNewAndExistingMetadata(objectRelationTypeId, "-");
        xml = client.getMeta(targetId);
        checkDiff("createTranslation", newMetadata, xml);
        client.delete(targetId);
    }

    @Test
    public void sourceTreeOf1AndExistingTargetTest() {
        // without existing translation metadata
        String name = "sourceTestObject";
        Long sourceId = client.create("", name, folderId);
        client.createTranslation("/sysMeta/object/name", name, sourceId,
                objectRelationTypeId, rootRelationTypeId);

        try {
            client.createTranslation("/sysMeta/object/name", name,
                    sourceId, objectRelationTypeId, rootRelationTypeId);
            assert false : "Managed to create a translation object on top of an existing one!";
        } catch (Exception e) {
            assert e.getMessage().contains("error.translation_exists") : "Unexpected error message: " + e.getMessage();
        }

    }

    @Test
    public void sourceTreeOf2AndExistingTargetTest() {
        //call with a source tree of size > 1 and a valid target tree
        String name = "sourceTestObject";
        Long sourceId = client.create("", name, folderId);
        Long version2 = client.versionAsLong(sourceId);
        client.createTranslation("/sysMeta/object/name", name, version2,
                objectRelationTypeId, rootRelationTypeId);

        try {
            client.createTranslation("/sysMeta/object/name", name,
                    version2, objectRelationTypeId, rootRelationTypeId);
            assert false : "Managed to create a translation object on top of an existing one!";
        } catch (Exception e) {
            assert e.getMessage().contains("error.translation_exists") : "Unexpected error message: " + e.getMessage();
        }
    }

    @Test
    public void targetFolderTest() {
        // call with a source tree with several objects and an incomplete target tree
        String name = "sourceTestObject";
        Long targetFolderId = client.createFolder("testTargetFolder", folderId);
        Long sourceId = client.create("", name, folderId);
        Long targetId = client.createTranslation("/meta", "-", sourceId, objectRelationTypeId,
                rootRelationTypeId, targetFolderId);
        String meta = client.getObject(targetId);
        assert meta.contains("<parentId>" + targetFolderId + "</parentId>") :
                String.format("Translation object was not created in correct folder %d. \n%s",
                        targetFolderId, meta);
    }

    @Test
    public void growingTreeTargetTest() {
        // call with a source tree with several objects and an incomplete target tree
        String name = "sourceTestObject";
        Long o1 = client.create("", name, folderId);
        OSD_Test.createTestFile();
        client.lockAndSetContent(OSD_Test.testData, "xml", o1);
        client.lockAndSetMeta(o1, "<meta><metaset type='test'>Boo!</metaset></meta>");

        Long o2 = client.versionAsLong(o1);
        client.lockAndSetContent(OSD_Test.testData, "xml", o2);
        client.lockAndSetMeta(o2, "<meta><metaset type='test'>Foo!</metaset></meta>");
        Long targetO2 = client.createTranslation("/sysMeta", name, o2,
                objectRelationTypeId, rootRelationTypeId);

        /*
        The newly created translation-tree must not have any content or format or custom metadata -
        only the target node receives the original content for later translation, along with the metadata.
         */
        // check translation root object:
        String translationRoot = client.getRootObject(targetO2);
        Document translationRootDoc = ParamParser.parseXmlToDocument(translationRoot);
        assert translationRootDoc.selectSingleNode("//object/format/sysName") == null : "new translationRoot must not have a format.";
        assert translationRootDoc.selectSingleNode("//object/contentsize[text()='50']") == null : "new translationRoot must not have content.";
        assert !client.getMeta(client.getRootObjectId(targetO2)).contains("Boo!") : "new translationRoot must not have custom metadata.";

        // check target node (version2):
        String targetO2Xml = client.getObject(targetO2);
        log.debug("targetO2Xml:\n" + targetO2Xml);
        Document translationTargetO2 = ParamParser.parseXmlToDocument(targetO2Xml);
        assert translationTargetO2.selectSingleNode("//object/format/sysName[text()='xml']") != null : "target node needs to copy format of source";
        assert translationTargetO2.selectSingleNode("//object/contentsize[text()='50']") != null : "target node needs content of source node.";
        String targetO2Meta = client.getMeta(targetO2);
        assert targetO2Meta.contains("Foo!") : "new object needs its custom metadata, not:\n"+targetO2Meta;

        Long o3 = client.versionAsLong(o2);
        client.lockAndSetMeta(o3, "<meta><metaset type='test'>Foo!</metaset></meta>"); // so the metadata is potentially identical to the version 2 of this object.
        Long targetO3 = client.createTranslation("/sysMeta", name, o3,
                objectRelationTypeId, rootRelationTypeId);
        String metaO2 = client.getMeta(targetO2);
        String metaO3 = client.getMeta(targetO3);
        // TODO: check diff without choking on different order of elements.
//        checkDiff("growingTreeTargetTest", metaO2,metaO3);

        Long targetO1 = client.createTranslation("/sysMeta", name, o1,
                objectRelationTypeId, rootRelationTypeId);
        String metaO1 = client.getMeta(targetO1).replace("Boo!", "Foo!"); // so metadata should be identical again.
//        checkDiff("growingTreeTargetTest",metaO1,metaO2);

        // now try to create a translation of a new branch
        Long o2v1 = client.versionAsLong(o2);
        log.debug("o2v1:\n" + client.getObject(o2v1));
        log.debug("o2v1-meta: " + client.getMeta(o2v1));
        Long targetO2v1 = client.createTranslation("/sysMeta/object/name",
                name, o2v1, objectRelationTypeId, rootRelationTypeId);
        String metaO2v1 = client.getMeta(targetO2v1);
//        checkDiff("growingTreeTargetTest",metaO2v1,metaO2);

        // test: complete target tree with branch exists:
        try {
            client.createTranslation("/sysMeta/object/name", name,
                    o2v1, objectRelationTypeId, rootRelationTypeId);
            assert false : "Managed to create a translation object on top of an existing one!";
        } catch (Exception e) {
            assert e.getMessage().contains("error.translation_exists") : "Unexpected error message: " + e.getMessage();
        }

        // check that older version has correct latesthead/branch value set:
        String oldTarget = client.getObject(targetO2);
        log.debug("rootObject: "+oldTarget);
        assert oldTarget.contains("<latestHead>false</latestHead>") : "LatestHead on targetO2.root "+targetO2+" was not updated!";
        assert oldTarget.contains("<latestBranch>false</latestBranch>") : "LatestBranch on targetO2.root "+targetO2+" was not updated!";
    }

    @Test
    public void checkTranslationTest() {
        /*
           * There are 4 possible cases:
           * 1. empty translation node
           * 2. only tree id
           * 3. full tree id + object id and translated=true
           * 4. full tree id + object id and translated=false
           */

        // call without translation tree:
        String name = "sourceTestObject";
        Long o1 = client.create("", name, folderId);
        String xml = client.checkTranslation("/sysMeta/object/name", name, o1,
                objectRelationTypeId, rootRelationTypeId);
        // should have: empty translation node.
        checkDiff("checkTranslationTest", "<translation/>", xml);

        Long targetId = client.createTranslation("/sysMeta/object/name", name, o1,
                objectRelationTypeId, rootRelationTypeId);
        xml = client.checkTranslation("/sysMeta/object/name", name, o1,
                objectRelationTypeId, rootRelationTypeId);

        // currently, we got a complete tree with translation-ready node:
        String expected = String.format("<translation><tree_root_id>%d</tree_root_id>" +
                "<target_object_id translated='true'>%d</target_object_id></translation>",
                targetId, targetId);
        // should have: tree_root_id, target_object_id, translated=true:
        checkDiff("checkTranslationTest", expected, xml);

        /*
           * Create another object in the source tree to get an incomplete
           * target tree. This should result in an incomplete target tree:
           */
        Long o2 = client.versionAsLong(o1);
        xml = client.checkTranslation("/sysMeta/object/name", name, o2,
                objectRelationTypeId, rootRelationTypeId);
        expected = String.format("<translation><tree_root_id>%d</tree_root_id></translation>",
                targetId);
        // should have: only tree_root_id:
        checkDiff("checkTranslationTest", expected, xml);
//		log.debug("o2:\n"+client.getObject(o2));
        /*
           * Create a third version, translate it, check the untranslated middle node:
           */
        Long o3 = client.versionAsLong(o2);
        Long o3TransId = client.createTranslation("/sysMeta/object/name", name, o3,
                objectRelationTypeId, rootRelationTypeId);

        // get o3Trans predecessor:
        String o3Trans = client.getObject(o3TransId);
//		log.debug("o3Trans:\n"+o3Trans);
        Document doc = ParamParser.parseXmlToDocument(o3Trans, null);
        String o2Trans = doc.selectSingleNode("//predecessorId").getText();
//		assert false : doc.selectSingleNode("//predecessorId").asXML();
        log.debug("predecessorId: " + doc.selectSingleNode("//predecessorId").asXML());
        xml = client.checkTranslation("/sysMeta/object/name", name, o2,
                objectRelationTypeId, rootRelationTypeId);
        // untranslated middle node should have @translated == false
        expected = String.format("<translation><tree_root_id>%d</tree_root_id>" +
                "<target_object_id translated='false'>%s</target_object_id></translation>",
                targetId, o2Trans);
        // should have: tree_root_id, target_object_id, translated=false:
        checkDiff("checkTranslationTest", expected, xml);
    }

    @Test
    public void sourceTreeAndBranchNoTargetTest() {
        // without existing translation metadata
        String name = "sourceTestObject";
        Long sourceId = client.create("", name, folderId);
        client.versionAsLong(sourceId);
        Long branchId = client.versionAsLong(sourceId);
        Long targetBranchId = client.createTranslation("/sysMeta/object/name", name, branchId,
                objectRelationTypeId, rootRelationTypeId);

        try {
            client.createTranslation("/sysMeta/object/name", name,
                    branchId, objectRelationTypeId, rootRelationTypeId);
            assert false : "Managed to create a translation object on top of an existing one!";
        } catch (Exception e) {
            assert e.getMessage().contains("error.translation_exists") : "Unexpected error message: " + e.getMessage();
        }

        String meta = client.getMeta(targetBranchId);
        checkDiff("sourceTreeAndBranchNoTargetTest",
                getTranslationMetadataString(objectRelationTypeId, name), meta);

    }

    @Test(dependsOnMethods = {"sourceTreeOf1AndNoTargetTest"})
    public void sourceTreeWith2ObjectsAndNoTargetTest() {
        String methodName = "sourceTreeWith2ObjectsAndNoTargetTest";
        // source object
        Long sourceId = client.create("", "sourceTestObject", folderId);
        Long version2 = client.versionAsLong(sourceId);
        Long targetId = client.createTranslation("-", "-", version2, objectRelationTypeId, rootRelationTypeId);

        // without metadata:
        String meta = client.getMeta(targetId);
        String expected = getTranslationMetadataString(objectRelationTypeId, "-");
        checkDiff(methodName, expected, meta);
        client.deleteAllVersions(targetId);

        // with metadata
        /*
         * Expected: version1 has extended (existing + new target node),
         * 			 version2 has normal metadata (only target node).
         */
        Long version1 = client.create(existingMetadata,
                "sourceWithTranslationMetadata", folderId);
        version2 = client.versionAsLong(version1);
        Long targetId2 = client.createTranslation("-", "-", version2, objectRelationTypeId, rootRelationTypeId);

        // check version 2:
        meta = client.getMeta(targetId2);
        checkDiff(methodName,
                combineNewAndExistingMetadata(objectRelationTypeId, "-"), meta);

        // check version 1:
        String target2XML = client.getObject(targetId2);
        Long targetRootId = Client.parseLongNode(target2XML, "/objects/object/rootId");
        meta = client.getMeta(targetRootId);
        checkDiff(methodName, getTranslationMetadataString(objectRelationTypeId, "-"),
                meta);

        /*
         * DeleteAllVersions will fail due to protected relations.
         * If you add another test which depends on the removal of version2 & targetId2,
         * please remove the relations first.
         */
//		client.deleteAllVersions(version2);
//		client.deleteAllVersions(targetId2);
    }

    /*
     * This test the fix for a bug where a translation of version 2 did not remove the
     * latestHead / latestBranch flag on version 1 of a translation.
     */
    @Test
    public void versionAndLatestHead() {
        String name = "VersionAndLatestHead";
        Long sourceId = client.create("", name, folderId);
        Long targetId1 = client.createTranslation("/sysMeta/object/name", name, sourceId, objectRelationTypeId, rootRelationTypeId);
        Long version2 = client.versionAsLong(sourceId);
        Long targetId2 = client.createTranslation("/sysMeta/object/name", name, version2, objectRelationTypeId, rootRelationTypeId);

        String osd1 = client.getObject(targetId1);
        assert osd1.contains("<latestBranch>false</latestBranch>") : "Version 1 is still latestBranch.";
        assert osd1.contains("<latestHead>false</latestHead>") : "Version 1 is still latestHead";

        String osd2 = client.getObject(targetId2);
        assert osd2.contains("<latestBranch>true</latestBranch>") : "Version 2 is not latestBranch.";
        assert osd2.contains("<latestHead>true</latestHead>") : "Version 2 is not latestHead";
    }

    @AfterClass
    public void disconnectTest() {
        boolean result = client.disconnect();
        assert result : "Disconnect form Server failed!";
    }

    public void checkDiff(String methodName, String expectedData, String actualData) {
        actualData = actualData.replaceAll("type=['\"](.+?)['\"] id=['\"]\\d+['\"]", "type='$1' id='xxx'");
        expectedData = expectedData.replaceAll("type=['\"](.+?)['\"] id=['\"]\\d+['\"]", "type='$1' id='xxx'");

        Diff myDiff = null;
        try {
            myDiff = new Diff(expectedData, actualData);
        } catch (SAXException e) {
            e.printStackTrace();
            assert false : methodName + " failed in xml-diff\n" + actualData;
        } catch (IOException e) {
            e.printStackTrace();
            assert false : methodName + " failed in xml-diff";
        }
        assert myDiff.similar() : "XML-Diff failed: \nreceived:\n" + actualData
                + "\nexpected:\n" + expectedData + "\n" + myDiff.toString();
    }

    String getTranslationMetadataString(Long objectRelationTypeId, String attributeValue) {
        return String.format("<meta><metaset type='translation_extension' id='xxx'><target><relation_type_id>%d</relation_type_id>"
                + "<attribute_value>%s</attribute_value></target></metaset></meta>",
                objectRelationTypeId, attributeValue);
    }

    String combineNewAndExistingMetadata(Long objectRelationTypeId, String attributeValue) {
        return existingMetadata.replace("</target>", String.format("</target><target><relation_type_id>%d</relation_type_id>"
                + "<attribute_value>%s</attribute_value></target>",
                objectRelationTypeId, attributeValue));
    }
}

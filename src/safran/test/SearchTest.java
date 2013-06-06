package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.PermissionName;
import utils.ParamParser;

import java.awt.geom.Dimension2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Tests for the index and search components, currently implemented via Lucene.
 */
public class SearchTest extends BaseTest {

    Long pgDoc = 0L;
    Long msDoc = 0L;
    Long folderId = 0L;
    Long formatId = 0L;
    Long insideFolder = 0L;

    @BeforeClass
    public void setUp() {
        super.setUp(false);

        // clear the index on the test system
        client.clearIndex("server.Folder");
        client.clearIndex("server.data.ObjectSystemData");


        formatId = client.createFormat("testFormat", "xml", "text/xml", "an XML test format");
        folderId = client.createFolder("testFolder", 0L);

        // create testfolder for indexfolderpath
        Long isf = client.createFolder("insideFolder1", folderId);
        insideFolder = client.createFolder("insideFolder2", isf);

        assert folderId != 0L;
        
		/*
		 * Upload 2 documents which should be indexed.
		 */
        pgDoc = client.create("<meta><name>Alice</name></meta>", "IndexItemTestObject1",
                "testdata/lucene/index_test_postgresql.xml", "testFormat", "text/xml", folderId);
        assert pgDoc != 0L;

        msDoc = client.create("<meta><name>Bob</name></meta>", "IndexItemTestObject2",
                "testdata/lucene/index_test_sql2005.xml", "testFormat", "text/xml", folderId);
        assert msDoc != 0L;

    }

    public SearchTest() {
    }

    public SearchTest(Properties config, Client client) {
        this.config = config;
        this.client = client;
    }

    @Test
    public void listIndexItemsTest() {
        String xml = client.listIndexItems();
        log.debug(xml);
        assert xml.contains("<name>index.name</name>") : "missing default index 'index.name' - received:\n" + xml;
    }

    //	@Test // should probably have its own test class.
    public void reindexTest() {
        String xml = client.reindex();
        log.debug(xml);
        assert xml.contains("success.reindex") : "missing 'reindexResult'-Tag - received:\n" + xml;
    }

    @Test()
    public void searchFolderPathTest() {
        
        String query = loadQuery("searchFolderPath.xml");
        String xml = client.searchFolders(query);
        assert xml.contains("<name>insideFolder2</name>") : "Did not find insideFolder1:\n" + xml;
    }

    /*
     * Testing if searchFolders works at all.
     */
    @Test()
    public void searchFoldersTest() {
        
        String query = loadQuery("searchFolders.xml");
        String xml = client.searchFolders(query);
        assert xml.contains("<name>testFolder</name>") : "Did not find testFolder:\n" + xml;
        assert xml.contains("total-results=\"1\"") : "Test result dooes not contain total-results:\n" + xml;
    }

    /*
     * Testing if searchObjects works at all.
     */
    @Test()
    public void searchObjectsTest() {
        
        String query = loadQuery("searchObjects.xml");
        String xml = client.searchObjects(query);
        assert xml.contains("<name>IndexItemTestObject1</name>") : "Did not find testObject:\n" + xml;
        assert xml.contains("total-results=\"1\"") : "Test result dooes not contain total-results:\n" + xml;
    }

    @Test()
    @SuppressWarnings("unchecked")
    public void searchDocumentsTest() {
		/*
		 * Search just for Apples:
		 */
        
        String query = loadQuery("lucene_term_query.xml");
        String xml = client.searchObjects(query);

        log.debug("current msDoc: " + client.getContent(msDoc));


        log.debug("results of term-query:\n" + xml);
        assert xml.contains(msDoc.toString()) : "no search results found.:\n" + xml;
		/*
		 * Search for a document which contains "Apples" but not "Oranges"
		 */
        query = loadQuery("lucene_query_name.xml");
        xml = client.searchObjects(query);
        assert xml.contains(msDoc.toString()) :
                String.format("search result does not include msDoc.id (%d) - received:\n%s", msDoc, xml);

        query = loadQuery("query_both_documents.xml");
        xml = client.searchObjects(query);
        checkResultSize(xml, 2);

        /*
         * check that parentFolders are returned.
         */
        Document doc = ParamParser.parseXmlToDocument(xml);
        List<Node> parentFolders = doc.selectNodes("//parentFolders/folder");
        assert parentFolders.size() == 1 : "Found incorrect number of parentFolders.";
        assert doc.selectNodes("//name[text()=testFolder]") != null : "Folder testFolder is not included in parentFolder.";
        // copy folder to create more results and check if parentFolders also work with multiple paths:
        Long targetFolder = client.createFolder("targetFolder", 0L);
        String copyFolderResult = client.copyFolder(folderId, targetFolder, "all", true);
        log.debug("copyFolderResult:\n" + copyFolderResult);
        xml = client.searchObjects(query);
        log.debug("search with 4 objects:\n" + xml);
        doc = ParamParser.parseXmlToDocument(xml);
        // should find 4 objects now.
        checkResultSize(xml, 4);
        parentFolders = doc.selectNodes("//parentFolders/folder");
        assert parentFolders.size() == 3 : "Found incorrect number of parentFolders: " + parentFolders.size() + "\n" + xml;
        // cleanup extra copies (else the results in further tests will be screwed up)
        List<Node> copies = doc.selectNodes(String.format("//object[not(parentId/text() = '%d')]", folderId));
        assert copies.size() == 2 : "Xpath did not find 2 results in xml, but " + copies.size();
        for (Node n : copies) {
            client.delete(Long.parseLong(n.selectSingleNode("id").getText()));
        }
        List<Node> copiedFolders =
                ParamParser.parseXmlToDocument(copyFolderResult).selectNodes("//folders/id");
        assert copiedFolders.size() == 3 : "Wrong number of folders reported: " + copiedFolders.size() + " doc: \n" + copyFolderResult;
        Collections.sort(copiedFolders, new Comparator<Node>() {
            @Override
            public int compare(Node node, Node node1) {
                Long n1 = Long.parseLong(node.getText());
                Long n2 = Long.parseLong(node1.getText());
                return n2.compareTo(n1); // sort last to first so we do delete the folders on the deepest level first.
            }
            // equals not implemented here, as node.equals is enough (we trust the server not to return two identical ids).
        });
        // note: the easier way would have been to convert to Long first.
        for (Node n : copiedFolders) {
            log.debug("deleteFolder: " + n.getText());
            client.deleteFolder(Long.parseLong(n.getText()));
        }

    }

    @Test()
    public void searchDocumentsPagedTest() {
		/*
		 * Limit search results by using page_size and page parameters:
		 */
        
        String query = loadQuery("query_both_documents.xml");
        String xml = client.searchObjectsPaged(query, 1, 1);
        assert xml.contains(pgDoc.toString()) : "paged search did not return pgDoc.id.\n" + xml;
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchMetadata() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Alice</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("metadata_query.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchMetadataWithCondition() {
        String query = loadQuery("metadata_conditional.xml");
        String xml = client.searchObjects(query);
        checkResultSize(xml, 0);

        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Smaug</name></metaset></meta>");
        client.unlock(pgDoc);
        xml = client.searchObjects(query);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchMetadataWithSpanQuery() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Alice in Wonderland</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("span_query.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchMetadataFoxSpanQuery() {
        // the difference to Alice in Wonderland: this sentence has no stop-word in the middle.
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>The sick brown fox dies alone.</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("span_query_fox.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchMetadataFoxTermsQuery() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>The sick brown fox dies alone.</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("terms_fox_query.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchUserQueryQuotedString() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Cinnamon meets Lucene - happy?.</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("user_query_quoted_string.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchMetadataUmlauts() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Ülüce</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("metadata_query_umlaut.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchAttributes() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><test attrib='a special attribute'/></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("attribute_query.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchDecimal() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><test><dec>12.34</dec></test><test><dec>43.21</dec></test></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("search_decimal.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchDecimalNegativeResult() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><test><dec>-65.43</dec></test><test><dec>43.21</dec></test><test><dec>12.34</dec></test></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("search_negative_decimal.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchDecimalPi() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><test><dec>3.1415926535</dec></test><test><dec>43.21</dec></test><test><dec>12.34</dec></test></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("search_decimal_pi.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchSysMetadataTest() {
        
        String query = loadQuery("sysmeta_query.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void updateAndSearchFolderMetadataTest() {
//		client.clearIndex("server.Folder");
//		client.reindex("folders");
        HashMap<String, String> fields = new HashMap<String, String>();
        fields.put("metadata", "<meta><metaset type='test'><description>a funny folder</description></metaset></meta>");
        assert client.updateFolder(folderId, fields);
        
        String query = loadQuery("folder_metadata_query.xml");
        String xml = client.searchFolders(query);
        assert xml.contains(folderId.toString()) : "foldersearch does not include folderId. Received:\n" + xml;
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchWildCardQuery() {
//		client.clearIndex("server.Folder");
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Cinnamon meets Lucene - happy?.</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("wildcard_querybuilder.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test()
    public void searchSimple() {
        String query = "apples build file";
        String xml = client.searchSimple(query, 10, 0);
        assert xml.contains("<name>IndexItemTestObject1</name>") && xml.contains("<name>IndexItemTestObject2</name>") :
                String.format("search result does not include both documents as XML - received:\n%s", pgDoc, xml);
        assert xml.contains("total-results=\"2\"") : "Should have 2 total results";

        log.debug("searchSimple: " + xml);
        String folderSearchResult = client.searchSimple("testFolder", 10, 0);
        assert folderSearchResult.contains("<name>testFolder</name>") :
                "SearchSimple did not find testFolder, but: " + folderSearchResult;
    }

    @Test()
    public void innerWildCardQuery() {
        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><name>Cinnamon meets Lucene - happy?.</name></metaset></meta>");
        client.unlock(pgDoc);
        
        String query = loadQuery("inner_wildcard.xml");
        String xml = client.searchObjects(query);
        assert xml.contains(pgDoc.toString()) :
                String.format("search result does not include pgDoc.id (%d) - received:\n%s", pgDoc, xml);
    }

    @Test(dependsOnMethods = {"searchAttributes"})
    public void searchWithoutPermissionName() {
		/*
		 * Plan: create a user without PermissionNames and let
		 * him try to search for something.
		 * Expected: should return no documents without PermissionName.
		 */

        client.lock(pgDoc);
        assert client.setMeta(pgDoc, "<meta><metaset type='test'><test attrib='a special attribute'/></metaset></meta>");
        client.unlock(pgDoc);
        
        @SuppressWarnings("unused")
        Long userId = client.createUser("testUser", "testi", "testme", "admin", "none");
        Properties props = new Properties(config);
        props.put("server.username", "testme");
        Client normalClient = new Client(props);
        Boolean connected = normalClient.connect();
        assert connected;

        // once for folders:
        String query = loadQuery("folder_metadata_query.xml");
        String xml = normalClient.searchFolders(query);
        checkResultSize(xml, 0);

        // once for objects:
        query = loadQuery("attribute_query.xml");
        xml = normalClient.searchObjects(query);
        checkResultSize(xml, 0);
        xml = client.searchObjects(query);
        checkResultSize(xml, 1);

        // now with valid acl:		
        String pgObj = client.getObject(pgDoc);
        Long aclId = Long.valueOf(ParamParser.parseXmlToDocument(pgObj, null).valueOf("//aclId"));
        String aclEntries = client.listAclEntries(aclId, "aclid");
        String aclEntry = ParamParser.parseXmlToDocument(aclEntries, null).valueOf("//aclEntry/id");
        Long aclEntryId = Long.valueOf(aclEntry);
        AclIntegrationTest ait = new AclIntegrationTest(config, client);
        ait.setPermissions(parseXmlResponse(client.listPermissions()));
        ait.setAdminClient(client);
        ait.addToAclEntry(PermissionName.BROWSE_OBJECT, aclEntryId);
        log.debug("AttributeQuery:\n " + query);
        xml = client.searchObjects(query);
        checkResultSize(xml, 1);
    }

    @Test()
    public void searchContentsize() {
        // find by contentsize 	[Integer]
        
        String query = loadQuery("search_contentsize.xml");
        String xml = client.searchObjects(query);
        checkResultSize(xml, 2);

        try {
            // create a small file which should not be found via contentsize > 999 Bytes.
            File testdata = File.createTempFile("cinnamonClient", null);
            FileWriter f = new FileWriter(testdata);
            f.write("<xml><data>This is just a test string</data></xml>");
            f.close();
            Long obj1 = client.create("", "evilIggy", testdata.getAbsolutePath(),
                    "testFormat", "text/xml", folderId);
            xml = client.searchObjects(query);
            checkResultSize(xml, 2);
            assert client.delete(obj1); // delete object because it could mess up other tests.
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Exception:" + e.getLocalizedMessage();
        }

    }

    @Test()
    public void searchCreated() {
        // find by created [Date]
        
        String query = loadQuery("search_created.xml");
        String xml = client.searchObjects(query);
        checkResultSize(xml, 2);

        // create a new object: search result should find 3 items.
        Long obj1 = client.create("", "Fang", folderId);
        xml = client.searchObjects(query);
        checkResultSize(xml, 3);

        query = loadQuery("search_created_by_valid_time.xml");
        xml = client.searchObjects(query);
        checkResultSize(xml, 3);

        query = loadQuery("search_created_by_invalid_time.xml");
        xml = client.searchObjects(query);
        checkResultSize(xml, 0);

        assert client.delete(obj1);
    }


    @Test()
    public void searchLatestHead() {
        // find by latestHead	[Boolean]
        
        String query = loadQuery("search_latestHead.xml");
        String xml = client.searchObjects(query);
        checkResultSize(xml, 2);

        // create a new object: search should find 3 items.
        Long obj1 = client.create("", "Farmer Maggot", folderId);
        xml = client.searchObjects(query);
        checkResultSize(xml, 3);

        // create a new version: search should find 3, not 4 items.
        Long obj2 = Long.parseLong(client.version(obj1));
        xml = client.searchObjects(query);
        checkResultSize(xml, 3);

        assert client.delete(obj2);
        assert client.delete(obj1);
    }

    @Test()
    public void regexQueryTest() {
        
        String query = loadQuery("regex_querybuilder.xml");
        String xml = client.searchFolders(query);
        assert xml.contains("<name>insideFolder2</name>") : "Did not find insideFolder2:\n" + xml;
        checkResultSize(xml, 1);
    }

    @AfterClass
    public void disconnectTest() {
		
		/*
		 * Due to a limitation of the eclipse testNG plugin,
		 * the code of searchDeletedItems was placed here:
		 */
        
        String query = loadQuery("lucene_query_name.xml"); // TODO: write a query that actually finds both docs!
        assert client.delete(pgDoc);
        assert client.delete(msDoc);
        String xml = client.searchObjects(query);
        assert !xml.contains("<id>" + msDoc.toString() + "</id>") &&
                !xml.contains("<id>" + pgDoc.toString() + "</id>") :
                "found deleted docs in index!\n" + xml;


        boolean result = client.disconnect();
        assert result : "Disconnect form Server failed!";
    }

    /**
     * Load the content of the query file which has to be located in the folder testdata/lucene.
     *
     * @param filename name of the query you want to load.
     * @return the content of the given file.
     */
    String loadQuery(String filename) {
        String query;
        try {
            query = utils.ContentReader.readFileAsString("testdata/lucene/" + filename);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    void checkResultSize(String xml, Integer size) {
        Document doc = ParamParser.parseXmlToDocument(xml, null);

        List<Node> nodes = doc.selectNodes("//objects/object|//folders/folder");
        assert nodes.size() == size :
                String.format("Wrong number of items in search results: not %d but %d. Received: \n" + xml,
                        size, nodes.size(), xml);
    }


    void waitForIndexServer() {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

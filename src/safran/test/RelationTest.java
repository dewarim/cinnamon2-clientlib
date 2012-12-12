package safran.test;

import org.dom4j.Document;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

public class RelationTest extends BaseTest{
	private Long relationID;
	private Long leftID;
	private Long rightID;
	private Long folderId = 0L;
	private long relationTypeID;
	private String relationTypeName;

	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void createRelationTest() {
		folderId = client.createFolder("testfolder", 0L);
		leftID = client.create("", "Left ID", folderId);
		rightID = client.create("", "Right ID", folderId);

		relationTypeName = "testrelation"+ Math.random();
		relationTypeID = client.createRelationType(relationTypeName, "RelationType Entry for testing", true, false, null);
		assert relationTypeID > 0 : "Could not create RelationType.";

		Document result = ParamParser.parseXmlToDocument(client.createRelation(relationTypeName, leftID, rightID), null);
		log.debug(result.asXML());
		String id = result.selectSingleNode("/relations/relation/id").getText();
		relationID = Long.parseLong(id);
		assert relationID > 0 : "Could not create testrelation.";
		log.debug(result.asXML());
	}

    @Test
    public void relationResolverTest(){
        Long resolverFolder = client.createFolder("resolverTest", 0L);
        Long leftId = client.create("", "Left ID", resolverFolder);
		Long rightId = client.create("", "Right ID", resolverFolder);
        Long relationTypeId = client.createRelationType("fixedResolverRelationType", "test-resolver-relation", true, true, null,
                "FixedRelationResolver", "LatestHeadResolver", false, false );
        Document result = ParamParser.parseXmlToDocument(client.createRelation("fixedResolverRelationType", leftId, rightId), null);
		log.debug(result.asXML());
		String id = result.selectSingleNode("/relations/relation/id").getText();
		Long relationId = Long.parseLong(id);
        Long rightVersion2 = client.versionAsLong(rightId);
        log.debug("new version: "+rightVersion2);
        /*
        After increasing the version number, the relation should point to the new object because of LatestHeadResolver.
         */
        String xml = client.getRelations(null, leftId, null);
        result = ParamParser.parseXmlToDocument(xml);
        assert result.selectNodes("/relations/relation").size() == 1 : "getRelations did not return one expected relation.\n"+xml;
        String rightOsd = result.selectSingleNode("//rightId").getText();
        assert rightOsd.equals(rightVersion2.toString()) : String.format("id %s did not match expected: %s\n%s", rightId, rightVersion2, xml);

        // when left version changes, relation should stay the same (because it is FixedRelationResolver)
        Long leftVersion = client.versionAsLong(leftId);
        xml = client.getRelations(null, null, rightVersion2);
        result = ParamParser.parseXmlToDocument(xml);
        assert result.selectNodes("/relations/relation").size() == 1 : "getRelations did not return one expected relation.\n"+xml;
        String leftOsd = result.selectSingleNode("//leftId").getText();
        assert leftOsd.equals(leftId.toString()) : String.format("id %s did not match expected: %s\n%s", leftOsd, leftId, xml);
        client.deleteRelation(relationId); // cleanup

        // test of LatestBranchResolver.
        /*
         * From the start, this should have left.v1 <=> right.v2 (as v2 is the latest branch).
         * After version(right.v1), the relation should be left.v1 <=> right.v1-1
         */
        Long fixedBranchRelationId = client.createRelationType("fixed_branch_RelationType", "test-fixed-and-branch-relation", true, true, null,
                "FixedRelationResolver", "LatestBranchResolver", false, false);
        result = ParamParser.parseXmlToDocument(client.createRelation("fixed_branch_RelationType", leftId, rightId), null);
		log.debug(result.asXML());
        assert result.selectNodes("/relations/relation").size() == 1 : "getRelations did not return one expected relation.\n"+xml;
        String rightBranch = result.selectSingleNode("//rightId").getText();
        assert rightBranch.equals(rightVersion2.toString()) : "Relation is not correct.\n"+result.asXML();
        client.versionAsLong(rightId); // create new latestBranch
        xml = client.getRelations(null, leftId, null);
        result = ParamParser.parseXmlToDocument(xml);
        String rightBranch1_1 = result.selectSingleNode("//rightId").getText();
        assert ! rightBranch.equals(rightBranch1_1) : "right part of relation should be changed after creating new version 1-1";
    }


	@Test(dependsOnMethods={"createRelationTest"})
	public void getRelationsTest() {
		/*
		 *  create a dummy relation which should not show up in result set: 
		 */
		Long rid = client.create("", "rid", folderId);
		Long lid = client.create("", "lid", folderId);
		client.createRelationType(relationTypeName+"-2", "TestRelation #2", true, false, null);
		client.createRelation(relationTypeName+"-2", lid, rid);
				
		String a = String.format("<relation><id>%d</id><leftId>%d</leftId><rightId>%d</rightId><type>%s</type><meta/></relation>",
					relationID, leftID, rightID, relationTypeName);
		
		String ret = client.getRelations(relationTypeName, null, null);
		assert ret.contains(a) : "expected '" + a + "' but got '" + ret + "'";

		ret = client.getRelations( null, leftID, null);
		assert ret.contains(a) : "expected '" + a + "' but got '" + ret + "'";

		ret = client.getRelations( null, null, rightID);
		assert ret.contains(a) : "expected '" + a + "' but got '" + ret + "'";

		try{
			// #1131: timeout if leftid or rightid does not exist...
			ret = client.getRelations( null, 1222222222L, -1L);
			assert false : "getRelations did not return an error on wrong input.";
		}
		catch (Exception e) {
			log.debug("ret:"+ret);
		}
	}

	@Test(dependsOnMethods={"createRelationTest", "getRelationsTest"})
	public void deleteRelationTest() {
		boolean ret = client.deleteRelation(relationID);
		assert ret : "could not delete relation " + relationID;

		ret = client.deleteRelationType(relationTypeID);
		assert ret : "could not delete relation type ID " + relationTypeID;

		ret = client.delete(rightID);
		assert ret : "could not delete right ID " + rightID;
		
		ret = client.delete(leftID);
		assert ret : "could not delete left ID " + leftID;
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

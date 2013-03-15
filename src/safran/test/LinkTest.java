package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ParamParser;

import java.io.IOException;

public class LinkTest extends BaseTest {
    private Long osd;
    private Long folder;
    private Long osdLink;
    private Long folderLink;
    private Long user;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public void setUp() {
        try {
            client = createClient("config.properties");
            boolean isConnected = client.connect();
            assert isConnected : "Client failed to connect to Cinnamon server.";

            // cleanup old runs:
//            Long f = Client.parseLongNode(client.getFolderByPath("/linkTestFolder"), "/folders/folder/id" );
//            client.deleteFolder(f);
        }
        catch (IOException e) {
            log.error("failed to create a client object: ", e);
            assert false : "Could not create a client object: " + e.getMessage();
        }
    }

    @Test
    public void createLinkTest() {
        folder = client.createFolder("linkTestFolder", 0L);
        osd = client.create("<meta/>", "linkTarget", folder);
        try {
            Long user = Client.parseLongNode(client.getUserByName("admin"), "/users/user/id");
            Long acl = Client.parseLongNode(client.getAcls(), "/acls/acl[sysName='_default_acl']/id");
            // link to osd:
            String linkResult = client.createLink(osd, user, acl, folder, "FIXED", "OBJECT");
            log.debug("linkResult:\n" + linkResult);
            Long targetId = Client.parseLongNode(linkResult, "/link/object/reference/id");
            assert targetId.equals(osd) : "Linked OSD is != link target.";
            osdLink = Client.parseLongNode(linkResult, "/link/object/reference/linkId");

            // link to folder:
            String folderLinkResult = client.createLink(folder, user, acl, folder, "FIXED", "FOLDER");
            log.debug("folderLinkResult:\n" + folderLinkResult);
            Long folderTargetId = Client.parseLongNode(folderLinkResult, "/link/folder/reference/id");
            assert folderTargetId.equals(folder) : "Linked folder is != folder link target";
            folderLink = Client.parseLongNode(folderLinkResult, "/link/folder/reference/linkId");

        }
        catch (Exception e) {
            log.debug("failed createLinkTest:", e);
            assert false : "got error: " + e.getMessage();
        }
    }

    @Test(dependsOnMethods = {"createLinkTest"})
    public void getLinkTest() {
        try {
            // get folder link
            String subfolders = client.getSubfolders(folder);
            log.debug("getSubfolders response:\n" + subfolders);
            Long refId = Client.parseLongNode(subfolders, "/folders/folder/reference/linkId");
            assert refId.equals(folderLink) : "Reference linkId does not match folderLink";

            // get osd link
            String objects = client.getObjects(folder);
            log.debug("getObjects response:\n" + objects);
            Long osdRef = Client.parseLongNode(objects, "/objects/object/reference/linkId");
            assert osdRef.equals(osdLink) : "Reference linkId does not match objectLink.";
            
            // TODO: test with getObjectsWithMetadata
            
        }
        catch (Exception e) {
            log.debug("failed getLinkTest:", e);
            assert false : "got error: " + e.getMessage();
        }
    }

    @Test(dependsOnMethods = {"getLinkTest"})
    public void updateLinkTest() {
        try {
            // update acl
            String aclQuery = client.getAcls();
            log.debug("result of getAcls:"+aclQuery);
            Long reviewAcl = Client.parseLongNode(aclQuery, "/acls/acl[sysName='review.acl']/id");
            String updateResult = client.updateLink(folderLink, null, reviewAcl, null, null);
            assert reviewAcl.equals(Client.parseLongNode(updateResult, "/link/folder/reference/aclId")) :
                    "Failed to update link with new ACL.";

            // update parent
            Long testFolder = null;
            try {
                testFolder = client.createFolder("parentUpdate", folder);
                String folderUpdate = client.updateLink(folderLink, null, null, testFolder, null);
                assert testFolder.equals(Client.parseLongNode(folderUpdate, "/link/folder/reference/parentId")) :
                        "Failed to update link with new parentId";
                client.updateLink(folderLink, null, null, folder, null);
                client.deleteFolder(testFolder);
            }
            catch (Exception e) {
                if (testFolder != null) {
                    client.deleteFolder(testFolder);
                }
                assert false : "Failed to update link with new parentId";
            }

            // update owner
            Long user = null;
            try {
                user = client.createUser("nobody", "linkTest testUser", "testUser", "test123", "none");
                String userUpdate = client.updateLink(folderLink, user, null, null, null);
                assert user.equals(Client.parseLongNode(userUpdate, "/link/folder/reference/ownerId")) :
                        "Failed to update link with new ownerId";
                client.deleteUser(user); // TODO: deleteUser() is not working correctly, as users are deactivated instead of deleted.
            }
            catch (Exception e) {
                if (user != null) {
                    client.deleteUser(user);
                }
                log.debug("updateLink(ownerId) failed",e);
                assert false : "Failed to update link with new ownerId";
            }

            // update resolver
            String resolverUpdate = client.updateLink(osdLink, null, null, null, "LATEST_HEAD");
            Node resolverNode = ParamParser.parseXmlToDocument(resolverUpdate).selectSingleNode("/link/object/reference/resolver");
            assert "LATEST_HEAD".equals(resolverNode.getText()):"Failed to update link with new resolver type.";

        }
        catch (Exception e) {
            log.debug("failed updateLinkTest:", e);
            assert false : "got error: " + e.getMessage();
        }
    }

    @Test(dependsOnMethods = {"updateLinkTest"})
    public void deleteLinkTest() {
        try {
            assert client.deleteLink(osdLink) : "Failed to delete link to OSD";
            assert client.deleteLink(folderLink) : "Failed to delete link to Folder";
        }
        catch (Exception e) {
            log.debug("failed createLinkTest:", e);
            assert false : "got error: " + e.getMessage();
        }
    }

    @AfterClass
    public void disconnectTest() {
        client.delete(osd);
        client.deleteFolder(folder);
        boolean result = client.disconnect();
        assert result : "Disconnect form Server failed!";
    }

}

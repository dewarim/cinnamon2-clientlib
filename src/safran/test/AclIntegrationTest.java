package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.PermissionName;
import utils.ParamParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This test is for testing the complex relationship of permissions, acls and the
 * rest of the Cinnamon API. For example, if a user has no write permission to a folder,
 * does the rights management successfully prevent her from copying new files 
 * to this folder?
 *    
 * @author Ingo Wiarda
 *
 */

public class AclIntegrationTest extends BaseTest {

    class Permission{

        String name;
        Long id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }


	private transient Logger log = LoggerFactory.getLogger(this.getClass());
	
	Long folderId 	= 0L;
	Long objId 		= 0L;
	Long userId 	= 0L;
	Long aclId 		= 0L;
	Long groupId	= 0L;
	Long aclEntryId	= 0L;
	Long copyId		= 0L;
	Long subFolderId= 0L;

	String metadata =  "<xml><p>foo!</p></xml>";
	Document permissions;
	Client adminClient;
	
	public AclIntegrationTest() {
		super();
	}
	
	public AclIntegrationTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	
	@BeforeClass
	public void setUp() {
		super.setUp();
		createPrerequisites();
	}

	// ------------ Start Testing -------------------
	

	/*
	 * Test plan:
	 * create test-user without admin privileges.
	 *
	 * create Acl with "no PermissionNames"
	 * create folder
	 * create test object inside folder
	 * set folder-ACL and object-ACL to NP-ACL.
	 *
	 * Both the folder and the object should be completely inaccessible for
	 * the test user at this moment.
	 *  
	 * try:
	 * 	* create object inside folder
	 *  * version test object
	 *  * copy test object
	 *  * delete object
	 *  * create folder inside test folder
	 *  * setMeta
	 *  * getMeta
	 *  * setSysMeta
	 *  * getSysMeta
	 *  * updateFolder
	 *  * deleteFolder
	 *  * lock
	 *  * unlock
	 *  * setContent
	 *  (*)queryObjects, 
	 *  getObjects (currently protected via addObjectResultSetToOutput and 
	 *  	PermissionName.BROWSE_OBJECT)
	 *  queryCustomTable (currently not tested, code was visually verified ;) )
	 *  * getContent
	 * 
	 * now add one PermissionName after the other and re-check whether
	 * the above actions are allowed / forbidden.
	 * 
	 * Optional:
	 * check EVERYONE and OWNER PermissionNames.
	 * 
	 * - test if custom PermissionNames work: create a non-default PermissionName
	 * and re-check.
	 * 
	 */
	
	void createPrerequisites() {
        // save admin client so we can add Permissions easily later on:
		adminClient = client;

        // setup test user
        Map<String,String> uf = new HashMap<String,String>();
        uf.put("name", "testUser");
        uf.put("pwd", "not.relevant");
        uf.put("description", "nondescript");
        uf.put("fullname", "L.Longus");
        uf.put("sudoable", "true");

        // add user to acl
        userId= adminClient.createUser(uf);
		assert userId > 0 : "create test user failed";

		AclTest at = new AclTest(config, client);
		at.createAclTest();
		aclId = at.getAclId();
		assert aclId > 0 : "create test acl failed";

		// 1. find group of this user.
		String xml = client.getGroupsOfUser(userId, false);
		String gid = parseXmlResponse(xml).selectSingleNode("//id").getText();
		groupId = Long.parseLong(gid);

		// 2. add user's own group to the test acl.
		xml = client.addGroupToAcl(groupId, aclId);
		log.debug(xml);
		String aeid = parseXmlResponse(xml).selectSingleNode("//id").getText();
		aclEntryId = Long.parseLong(aeid);

		FolderTest ft = new FolderTest(config, client);
		ft.createFolderTest();
		folderId = ft.getFolderId();
		assert folderId > 0 : "create test folder failed";

		// set test-folder's acl to test-acl:
		Map<String, String> fields = new HashMap<String, String>();
		fields.put("aclid", aclId.toString());
		assert client.updateFolder(folderId, fields) : "set folder to test acl failed";

		objId = createTestObject();
		assert objId > 0 : "create test object";

		assert client.lock(objId) : "Failed to obtain lock for test object.";
		Boolean result = client.setSysMeta(objId, "acl_id", aclId.toString());
		assert result : "set object to test acl failed";
		assert client.unlock(objId) : "Failed to release lock for test object";

		// create a new client which uses the test user account:
		client = new Client(config.getProperty("server.url"), "testUser",
				"not.relevant", config.getProperty("default_repository"));
		assert client.connect() : "connect to server with test user failed";

		// get permissions:
		permissions = parseXmlResponse(client.listPermissions());
	}

	@Test(dependsOnMethods = { "lockAndUnlockTest" })
	public void setContentTest() {
		OSD_Test.createTestFile();
		addToAclEntry(PermissionName.LOCK, aclEntryId);
		try {
			assert client.lock(objId);

			try {
				client.setContent(OSD_Test.testData, "xml", objId);
				assert false : "setContent succeeded without permission";
			} catch (Exception e) {
				checkExceptionMessage(e, "setContent", "error.missing.permission._write_object_content");
			}

			addToAclEntry(PermissionName.WRITE_OBJECT_CONTENT, aclEntryId);

			try {
				Boolean result = client.setContent(safran.test.OSD_Test.testData, "xml",
						objId);
				assert result : "setContent() fails with permission.";
				assert client.unlock(objId);
			} finally {
				removeFromAclEntry(PermissionName.WRITE_OBJECT_CONTENT, aclEntryId);
			}
		} finally {
			removeFromAclEntry(PermissionName.LOCK, aclEntryId);
		}
	}


	@Test(dependsOnMethods = { "setContentTest"/* , "copyTest" */})
	public void getContentTest() {
		try {
			client.getContent(objId);
			assert false : "getContent succeeded without permission";
		} catch (Exception e) {
			checkExceptionMessage(e, "getContentTest", "error.missing.permission._read_object_content");
		}

		addToAclEntry(PermissionName.READ_OBJECT_CONTENT, aclEntryId);
		try {
			String content = client.getContent(objId);
			assert content
					.contains("<xml><data>This is just a test string</data></xml>") : "getContent() fails with PermissionName.";
		} finally {
			removeFromAclEntry(PermissionName.READ_OBJECT_CONTENT, aclEntryId);
		}
	}

	@Test
	public void lockAndUnlockTest() {
		try {
			// try to create a object with limited user:
			client.lock(objId);
			assert false : "lock succeeded without permission";
		} catch (Exception e) {
			checkExceptionMessage(e, "lockAndUnlockTest",
					"error.missing.permission._lock");
		}

		addToAclEntry(PermissionName.LOCK, aclEntryId);
		try {
			assert client.lock(objId) : "lock failed despite permission.";
			assert client.unlock(objId) : "unlock failed despite permission.";
			/*
			 * there is no "unlock locked object without permission" test.
			 			 * Permission to lock includes permission to unlock.
*/
		} finally {
			removeFromAclEntry(PermissionName.LOCK, aclEntryId);
		}
	}

	@Test
	public void writeToFolderTest() {
		try {
			// try to create a object with limited user:
			Long testId = client.create("", "permissionTest", folderId);
			assert false : String.format(
					"Created object %s without permission.", testId);
		} catch (Exception e) {
			checkExceptionMessage(e, "writeToFolder", "error.missing.permission._create_inside_folder");
		}
		addToAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);

		try {
			Long testId = client.create("", "permissionTest", folderId);
			assert testId > 0 : "Created object with permission failed.";
		} finally {
			removeFromAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);
		}
	}

    @Test
    public void sudoTest(){
        // try sudo as test user for itself:  must fail because test user is not a sudoer..
        try{
            String mySudo = client.sudo(userId);
        }
        catch (Exception e){
            checkExceptionMessage(e, "sudoNoPermission", "error.sudo.forbidden");
        }

        // try sudo as admin for admin - must fail because admin is not sudoable.
        try{
            Long adminId = Client.parseLongNode(adminClient.getUserByName("admin"), "//users/user/id");
            String mySudo = adminClient.sudo(adminId);
            assert false: "Managed to 'sudo admin' - but admin is missing sudoable flag.";
        }
        catch (Exception e){
            checkExceptionMessage(e, "sudoToAdminRole", "error.sudo.misuse");
        }

        // try sudo as admin user for non-existent user - must fail.
        try{
            String mySudo = adminClient.sudo(0L);
            assert false: "Managed to sudo with null-user.\n"+mySudo;
        }
        catch (Exception e){
            checkExceptionMessage(e, "sudoWithUser0", "error.user.not_found");
        }

        // try sudo as admin with anotherTestUser, who is sudoable:
        String xml = adminClient.sudo(userId);
        String ticket = ParamParser.parseXmlToDocument(xml).selectSingleNode("/sudoTicket").getText();
        assert ticket != null : "Did not receive a correct ticket: " + xml;

        // check that ticket != adminClient.ticket
        assert !ticket.equals(adminClient.getSessionTicket()) : "Ticket == adminTicket.";

        // check ticket allows create test object with owner=anotherUser.

        addToAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);

        String adminTicket = adminClient.getSessionTicket();
        adminClient.setSessionTicket(ticket);
        try {
            Long sudoObjectId = adminClient.create("<meta/>", "sudo-test-object", folderId);
            adminClient.setSessionTicket(adminTicket);
            String sudoObject = adminClient.getObject(sudoObjectId);
            String owner = ParamParser.parseXmlToDocument(sudoObject)
                    .selectSingleNode("//objects/object/owner/id").getText();
            assert owner.equals(userId.toString()) : "sudo did not work correctly: ownerId != userId.";
        } finally {
            adminClient.setSessionTicket(adminTicket);
            removeFromAclEntry(PermissionName.CREATE_OBJECT, aclEntryId); // restore previous functionality
        }
    }

    @Test
	public void versionObjectTest() {
		try {
			client.versionAsLong(objId);
			assert false : "Created a new version without permission.";
		} catch (Exception e) {
			checkExceptionMessage(e, "versionObjectTest",
					"error.missing.permission._version");
		}

		addToAclEntry(PermissionName.VERSION_OBJECT, aclEntryId);
		try {
			Long newVersion = client.versionAsLong(objId);
			assert newVersion > 0 : "Created a new version failed in spite of valid permission.";
		} finally {
			removeFromAclEntry(PermissionName.VERSION_OBJECT, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "writeToFolderTest" })
	public void copyTest() {
		try {
			client.copy(objId, folderId);
			assert false : "Server allowed copy(...) without permission";
		} catch (Exception e) {
			// it is not enough that copy() throws an exception, it has to be
			// the right one:
			checkExceptionMessage(e, "copyTest", "error.missing.permission._read_object_content");
		}

		addToAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);
		addToAclEntry(PermissionName.READ_OBJECT_CONTENT, aclEntryId);
		addToAclEntry(PermissionName.READ_OBJECT_CUSTOM_METADATA, aclEntryId);
		addToAclEntry(PermissionName.READ_OBJECT_SYS_METADATA, aclEntryId);

		try {
			copyId = client.copy(objId, folderId);
			assert copyId > 0 : "Copy failed despite valid permissions.";
		} finally {
			// removing permissions so that the more specific tests may set
			// them:
			removeFromAclEntry(PermissionName.READ_OBJECT_CONTENT, aclEntryId);
			removeFromAclEntry(PermissionName.READ_OBJECT_CUSTOM_METADATA,
					aclEntryId);
			removeFromAclEntry(PermissionName.READ_OBJECT_SYS_METADATA, aclEntryId);
			removeFromAclEntry(PermissionName.CREATE_OBJECT, aclEntryId);
		}

		assert adminClient.lock(copyId);
		Boolean result = adminClient.setSysMeta(copyId, "acl_id", aclId
				.toString());
		assert result : "Failed to set ACL of copy to test-acl.";
		assert adminClient.unlock(copyId);
	}

	@Test(dependsOnMethods = { "lockAndUnlockTest" })
	public void setMetaTest() {
		addToAclEntry(PermissionName.LOCK, aclEntryId);
		try {
			assert client.lock(objId);
			try {
				client.setMeta(objId, metadata);
				assert false : "Server allowed setMeta(...) without permission";
			} catch (Exception e) {
				checkExceptionMessage(e, "setMetaTest", "error.missing.permission._write_object_custom_metadata");
			}

			addToAclEntry(PermissionName.WRITE_OBJECT_CUSTOM_METADATA, aclEntryId);
			try {
				Boolean result = client.setMeta(objId, metadata);
				assert result : "Failed to set metadata.";
				assert client.unlock(objId);
			} finally {
				removeFromAclEntry(PermissionName.WRITE_OBJECT_CUSTOM_METADATA,
						aclEntryId);
			}
		} finally {
			removeFromAclEntry(PermissionName.LOCK, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "setMetaTest"})
	public void getMetaTest() {
		try {
			client.getMeta(objId);
			assert false : "Server allowed getMeta(...) without permission";
		} catch (Exception e) {
			checkExceptionMessage(e, "getMetaTest", "error.missing.permission._read_object_custom_metadata");
		}

		addToAclEntry(PermissionName.READ_OBJECT_CUSTOM_METADATA, aclEntryId);
		try {
			String meta = client.getMeta(objId);
			assert meta.equals(metadata) : "Failed to read the metadata:" + meta;
		} finally {
			removeFromAclEntry(PermissionName.READ_OBJECT_CUSTOM_METADATA,
					aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "lockAndUnlockTest" })
	public void setSysMetaTest() {
		addToAclEntry(PermissionName.LOCK, aclEntryId);
		try {
			assert client.lock(objId);

			/*
			 * We will just try to set the procstate and leave the actual
			 * setSysMeta-Testing to OSD_Test. (Currently, there is only one
			 * Permission for setSysMeta. You may have to expand this test if
			 * the permission system is extended to specific permissions for the
			 * individual fields of the system metadata.
			 */

			// set parentId
			// set owner
			// set procstate
			/*
			 * Note: At the moment, procstate is simply a string. Update this
			 * test if procstate becomes a reference to a procstate_type-table.
			 */

			try {
				client.setSysMeta(objId, "procstate", "tested");
				assert false : "Server allowed setSysMeta(procstate) without permission";
			} catch (Exception e) {
				checkExceptionMessage(e, "setSysMetaTest",
						"error.missing.permission._write_object_sysmeta");
			}

			addToAclEntry(PermissionName.WRITE_OBJECT_SYS_METADATA, aclEntryId);
			try {
				Boolean result = client
						.setSysMeta(objId, "procstate", "tested");
				assert result : "SetSysMeta(procstate) was forbidden despite permission.";
			} finally {
				removeFromAclEntry(PermissionName.WRITE_OBJECT_SYS_METADATA,
						aclEntryId);
			}

			// set permissionid (== set acl)

			// set objtype

			assert client.unlock(objId);
		} finally {
			removeFromAclEntry(PermissionName.LOCK, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "setSysMetaTest" })
	public void getSysMetaTest() {
		try {
			client.getSysMeta(objId, null, "procstate");
			assert false : "getSysMeta(procstate) returned data without permission";
		} catch (Exception e) {
			checkExceptionMessage(e, "getSysMetaTest", "error.missing.permission._read_object_sysmeta");
		}

		addToAclEntry(PermissionName.READ_OBJECT_SYS_METADATA, aclEntryId);
		try {
			String procstate = client.getSysMeta(objId, null, "procstate");
			assert procstate.equals("tested") : "getSysMeta(procstate) failed to return data.";
		} finally {
			removeFromAclEntry(PermissionName.READ_OBJECT_SYS_METADATA, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "copyTest" })
	public void deleteTest() {
		// note: the copy should have the same acl as the original.
		try {
			client.delete(copyId);
			assert false : "Server allowed delete() without permission";
		} catch (Exception e) {
			checkExceptionMessage(e, "deleteTest", "error.missing.permission._delete_object");
		}

		addToAclEntry(PermissionName.DELETE_OBJECT, aclEntryId);
		try {
			Boolean result = client.delete(copyId);
			assert result : "Delete object failed despite permission.";
		} finally {
			removeFromAclEntry(PermissionName.DELETE_OBJECT, aclEntryId);
		}
	}

	// ---------------------------- Folder Tests -----------------------------

	@Test()
	public void createFolderTest() {
		try {
			client.createFolder("testFolder", folderId);
			assert false : "Managed to create a folder without permission.";
		} catch (Exception e) {
			// it is not enough that copy() throws an exception, it has to be
			// the right one:
			checkExceptionMessage(e, "createFolderTest", "error.missing.permission._create_folder");
		}

		addToAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);
		try {
			subFolderId = client.createFolder("testFolder", folderId);
			assert subFolderId > 0 : "Create folder with permission failed.";

			Map<String, String> fields = new HashMap<String, String>();
			fields.put("aclid", aclId.toString());
			Boolean result = adminClient.updateFolder(subFolderId, fields);
			assert result : "Could not set test-acl on subFolder.";
		} finally {
			removeFromAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "createFolderTest" })
	public void updateFolderTest() {
		Map<String, String> fields = new HashMap<String, String>();
		fields.put("aclid", aclId.toString());
		// move folder

		addToAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);
		try {
			Long sourceFolderId = client.createFolder("sourceFolder", folderId);
			adminClient.updateFolder(sourceFolderId, fields); // set testAcl
			Long targetFolderId = client.createFolder("targetFolder", folderId);
			adminClient.updateFolder(sourceFolderId, fields); // set testAcl

			fields.clear();
			fields.put("parentid", targetFolderId.toString());

			try {
				client.updateFolder(sourceFolderId, fields);
				assert false : "Update folder (move) succeeded without permission.";
			} catch (Exception e) {
				checkExceptionMessage(e, "updateFolderTest",
						"error.missing.permission._move");
			}

			addToAclEntry(PermissionName.MOVE, aclEntryId);
			try {
				assert client.updateFolder(sourceFolderId, fields) : "Update folder (move) failed despite PermissionName.";
			} finally {
				removeFromAclEntry(PermissionName.MOVE, aclEntryId);
			}

			// set name and metadata
			fields.clear();
			fields.put("name", "newSourceFolderName");
			fields.put("metadata", "<xml>foo</xml>");

			try {
				client.updateFolder(sourceFolderId, fields);
				assert false : "Update folder (edit) succeeded without permission.";
			} catch (Exception e) {
				checkExceptionMessage(e, "updateFolderTest",
						"error.missing.permission._edit_folder");
			}

			addToAclEntry(PermissionName.EDIT_FOLDER, aclEntryId);
			try {
				assert client.updateFolder(sourceFolderId, fields) : "Update folder (move) failed despite PermissionName.";
			} finally {
				removeFromAclEntry(PermissionName.EDIT_FOLDER, aclEntryId);
			}

			// set acl
			AclTest at = new AclTest(config, adminClient);
			at.setAclName("yaacl");
			at.createAclTest();
			Long anotherAclId = at.getAclId();

			fields.clear();
			fields.put("aclid", anotherAclId.toString());

			try {
				client.updateFolder(sourceFolderId, fields);
				assert false : "Update folder (acl) succeeded without permission.";
			} catch (Exception e) {
				checkExceptionMessage(e, "updateFolderTest",
						"error.missing.permission._set_acl");
			}

			addToAclEntry(PermissionName.SET_ACL, aclEntryId);
			try {
				assert client.updateFolder(sourceFolderId, fields) : "Update folder (acl) failed despite PermissionName.";
			} finally {
				removeFromAclEntry(PermissionName.SET_ACL, aclEntryId); // we may
				// need this
				// unset for
				// setSysMeta(object)
			}
		} finally {
			removeFromAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);
		}
	}

	@Test
	public void browseFolderTest() {
		try {
			client.getFolder(folderId);
			assert false : "Managed to browse folder without permission.";
		} catch (Exception e) {
			checkExceptionMessage(e, "browseFolderTest", "error.missing.permission._browse_folder");
		}
		addToAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		try {
			assert client.getFolder(folderId) != null : "Failed to retieve folder.";
		} finally {
			removeFromAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		}
	}

	@Test(dependsOnMethods = { "updateFolderTest", "browseFolderTest" })
	public void ownerAclTest() {
		// 1. find the owner-group
		String xml = client.listGroups(null);
		log.debug("ownerAclTest:listGroups:\n" + xml);
		Long gid = Client.parseLongNode(xml, "/groups/group[name='_owner']/id");

		// 2. add group to our test-ACL
		xml = adminClient.addGroupToAcl(gid, aclId);
		Long ownerAE = Client.parseLongNode(xml, "/aclEntries/aclEntry/id");

		// 3. create folder:
		addToAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		addToAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);

		try {
			log.debug("folder:\n" + client.getFolder(folderId));
			Long newFolder = client.createFolder("a test Folder", folderId,	aclId);
			log.debug("newFolder:\n" + client.getFolder(newFolder));

			// remove EDIT-Folder-Permission from standard AE
			// removeFromAclEntry(Permission.EDIT_FOLDER, aclEntryId);

			// try to edit folder without permission
			Map<String, String> fields = new HashMap<String, String>();
			fields.put("name", "fooFolder");
			try {
				client.updateFolder(newFolder, fields);
				assert false : "Update folder (name) succeeded without permission.";
			} catch (Exception e) {
				checkExceptionMessage(e, "ownerAclTest", "error.missing.permission._edit_folder");
			}

			// add required permission to owner-ACL-Entry
			addToAclEntry(PermissionName.EDIT_FOLDER, ownerAE);

			try {
				// try again - should succeed.
				assert client.updateFolder(newFolder, fields) : "failed to update folder via owner_acl";
			} finally {
				removeFromAclEntry(PermissionName.EDIT_FOLDER, ownerAE);
			}
		} finally {
			removeFromAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
			removeFromAclEntry(PermissionName.CREATE_FOLDER, aclEntryId);
		}
	}

	@Test
	public void createInstanceTest(){
		WorkflowTest wt = new WorkflowTest(config, adminClient);
		wt.workflowSetup();
		Long templateId = wt.getTemplateId();
		
		// set our test-acl on workflow-template:
		adminClient.lockAndSetSysMeta(templateId, "acl_id", aclId.toString());
		
		try{
			client.createWorkflow(templateId);
			assert false : "client was able to create a workflow without permission.";
		}
		catch(Exception e){
			checkExceptionMessage(e, "createInstanceTest", "error.missing.permission._create_instance");
		}
		
		// add createInstance-Permission
		addToAclEntry(PermissionName.CREATE_INSTANCE, aclEntryId);
		String workflow = client.createWorkflow(templateId);
		assert workflow.contains("<workflowId>") : "no id found in createWorkflow response.\n"+workflow;
	}
	
	@Test(dependsOnMethods = { "updateFolderTest", "ownerAclTest" })
	public void deleteFolderTest() {
		try {
			client.deleteFolder(subFolderId);
			assert false : "Delete folder succeeded without permission.";
		} catch (Exception e) {
			checkExceptionMessage(e, "deleteFolderTest", "error.missing.permission._delete_folder");
		}
		log.debug("listAclEntries:\n" + client.listAclEntries(aclId, "aclid"));
		addToAclEntry(PermissionName.DELETE_FOLDER, aclEntryId);
		addToAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		try {
			log.debug("listAclEntries after set delete_folder:\n"
					+ client.listAclEntries(aclId, "aclid"));
			log.debug(client.getFolder(subFolderId));
			Boolean result = client.deleteFolder(subFolderId);
			assert result : "Delete folder with permission failed.";
		} finally {
			removeFromAclEntry(PermissionName.DELETE_FOLDER, aclEntryId);
			removeFromAclEntry(PermissionName.BROWSE_FOLDER, aclEntryId);
		}
	}

	// ------------ End of Testing -------------------

	// As long as we create a new on db each run, we can skip this:
	@AfterClass
	public void disconnectTest() {
		assert client != null : "check for valid client";
		assert adminClient != null;

		assert adminClient.disconnect() : "Disconnect failed for admin.";
		assert client.disconnect() : "Disconnect failed for test user.";
	}

	// -------------------- internal methods ---------------

	private Long createTestObject() {
		OSD_Test ot = new OSD_Test(config, client);
		ot.setFolderId(folderId);
		ot.createTestSimple();
		return ot.getObjId();
	}

	void addToAclEntry(String permName, Long aclEntryId) {
		Permission permission = findPermission(permName);
		assert permission != null;
		String xml = adminClient.addPermissionToAclEntry(permission.getId(),
                aclEntryId);
		assert xml.contains("<success>success.add.permission</success>") : "addPermissionToAE failed.";
	}

	void removeFromAclEntry(String permName, Long aclEntryId) {
		Permission permission = findPermission(permName);
		assert permission != null;
		String xml = adminClient.removePermissionFromAclEntry(permission
                .getId(), aclEntryId);
		log.debug("message: '" + xml + "'");
		assert xml.contains("<success>success.remove.permission</success>") : "removePermissionFromAE failed.";
	}

	Permission findPermission(String name) {
		Node permission = permissions.selectSingleNode(String.format(
				"//permission[name='%s']", name));
		log.debug("node:" + permission.asXML());

        Permission p = new Permission();
        String id = permission.selectSingleNode("id").getText();
        p.setId(ParamParser.parseLong(id, null));
        p.setName(name);
        return p;
	}

	/**
	 * @return the aclEntryId
	 */
	public Long getAclEntryId() {
		return aclEntryId;
	}

	/**
	 * @param aclEntryId
	 *            the aclEntryId to set
	 */
	public void setAclEntryId(Long aclEntryId) {
		this.aclEntryId = aclEntryId;
	}

	/**
	 * @return the aclId
	 */
	public Long getAclId() {
		return aclId;
	}

	/**
	 * @param aclId
	 *            the aclId to set
	 */
	public void setAclId(Long aclId) {
		this.aclId = aclId;
	}

	/**
	 * @return the adminClient
	 */
	public Client getAdminClient() {
		return adminClient;
	}

	/**
	 * @param adminClient
	 *            the adminClient to set
	 */
	public void setAdminClient(Client adminClient) {
		this.adminClient = adminClient;
	}

	/**
	 * @return the groupId
	 */
	public Long getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId
	 *            the groupId to set
	 */
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param permissions
	 *            the permissions to set
	 */
	public void setPermissions(Document permissions) {
		this.permissions = permissions;
	}

}


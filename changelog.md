2.3.0-beta
+ Fixed: connect() now accepts session tickets larger than 64 chars 
  (only relevant to those using database names longer than 24 characters...)

2.1.1
+ API changed: getUsers() now returns String instead of Doc.
+ UserTest now checks if user with Umlaut can be created properly.

2.1.0
+ Added methods to use zipFolder command.

2.0.5
+ Updated RelationTypeTest and Client for cloneOn{Left,Right}Copy

2.0.4
+ Fixed WorkflowTest and workflow related client methods.

2.0.3_RC1
+ Added call to search API method (previously, had only searchObjects, searchFolders, searchSimple)

2.0.2
+ Fixed SessionTest (forkSession did not check for oldTicket != newTicket)
+ Improved UserTest (check for sudoable / sudoer in serialzed user object)
+ Client changed API: create() now requires additional parameter contentType
+ Client now uses MultiThreadedHttpConnectionManager so it can be better used in multithreaded applications.
+ Added copy-constructor which creates a new Client with forked session ticket.

2.0.1
+ Added searchSimple method
+ Added example safran.xml

1.0.9_RC1
+ Fixed bug in getContentAsFile which tended to corrupt downloads ... sometimes.
+ Removed API method getExtension as it no longer exists on the server.
+ Added publishStackTrace method for easier upload of Java exceptions.
+ Added sudo API method

1.0.8
+ Added test for bug #1994.
+ Removed dependency entitylibs.jar & utils.jar
+ Added dependency cinnamonBase.jar
+ Created separate build script for safran.jar

1.0.7
+ fixed bug in client.setMeta (would return false even on success).
+ removed executeXmlQuery and queryFolders for security reasons (and because they were not used).

1.0.6
+ improved FolderTest.copyFolder
+ improved test for copyAllVersions
+ setContent throws RuntimeException if FileNotFound.
+ new: lockAndSetContent(file, format, id)
+ new: getRootObject(id)
+ new: getRootObjectId(id)
+ improved TranslationTest
+ fixed: two createFolder-methods relied on an old server-API.  

1.0.5
-

1.0.4
-

1.0.3
+ added searchObjectsPaged and searchFoldersPaged for searches with paged results.

1.0.2
+ added setPassword + test for basic functionality
+ added setEmail + test for basic functionality

1.0.1
+ Added test for Permission.CREATE_INSTANCE, which is needed for createWorkflow. 
+ Added lockAndSetSysMeta() which locks an object before it tries to change the sysmeta
 (and unlocks it afterward) 
+ Added test for task deadlines.
+ Added test for workflow deadlines.

1.0.0
+ Updated Lucene to version 3.0.1
+ Improved ChangeTriggerTest.
+ reworking WorkflowEngine.

0.7.1
+ fixed FolderTest
+ fixed OSD_Test.versionWithFormatTest 
+ changed client to work with the first batch of new XML server responses.
 
0.7.0
Fixed javaDoc.
Added PerformanceTest.

0.6.9
Added TransformationTest
Added transformObject() and transformObjectToFile() as API methods.
Added getContentAsFile() as API method for downloading binary content.

0.6.8
-

0.6.7
-

0.6.6
Added ChangeTriggerTest
createTranslation now accepts optional targetFolderId parameter.
Added support for API method checkTranslation.

0.6.5
Added TestTemplate and TranslationTest. 
Extended client with createTranslation and internal checkSuccess.

0.6.4
Fixed several broken tests.
Added test for browseFolder-Permission. 

0.6.3
Added test for folderpath-indexer.
Added test for regex-query-builder.

0.6.2
Upgrade to Lucene 3

0.6.0 / 0.6.1
-

0.5.9
Fixed a bug in Client class: name parameter was missing from parameter list to create(...)
Renamed IndexItemTest to SearchTest
Added basic tests for searchFolders and searchObjects API methods.

0.5.8
Added FolderTypeTest + API.
Added clearIndex method to API
Upgraded to Lucene 2.9.0 (mostly)
Added ReIndexTest

Extended IndexItemTest. 
IndexItemTest now checks for correct filtering of search results via BROWSE-permission.
Added API method client.search(String lucene_xml_query).

0.5.7
Added IndexItemTest
Added method queryFolders which uses XML-queries.
Added method createFolder which includes ownerId in parameter list.
The createFolder methods without explicit ownerId-param will use the current user's id.
all StringParts submitted by the Client are now expected to be in UTF-8.
added test for Bug 1184 (broken Umlauts).

0.5.6
FolderTest now checks for root-folder in getFolder/getFolderByPath-Response.
Added test for bug #1150. 
Changed API: client.getRelations(name, leftId, rightId) does no longer expect an id as the first parameter.
Added tests for addUserToGroup and removeUserFromGroup
Removed getFolderById - use getFolder instead; params are the same.

Added some more tests to test/testng.xml. 
	*** NOTE ***
	on some systems, running all tests may cause Tomcat to run out of
	PermGenSpace and require a restart. This is caused by Tomcat reloading Cinnamon
	with all its classes repeatedly. A workaround is to increase PermGenSpace and
	adding -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled as JavaOpts.
	 
Added createFolder(name,parentId,aclId)
Added test for check of folder-owner-acl.
getRelations now returns a XML response.

0.5.5
changed: getObjTypes now returns XML list; updated ObjectTypeTest.
added client method & test for getFolderMeta.
added OSD_Test.inheritParentAclTest
added getUserByName(name)
added getUsersPermissions(userId,aclId)
added getFolder method
new test: Xml2SqlTest for new method exectuteXmlQuery
renamed queryObjectsXML to queryObjects: all methods returning <object> elements 
now use the same format. 

0.5.4
New API method: queryObjectsXML

0.5.2
Refactored Client, updated Tests, broke API. Luckily this Client lib is currently only used for
testing purposes, so the API changes are not that critical ;)
The Client will now rethrow Server error messages as RuntimeExceptions. Previous behavior was to
convert them into true/false or 0L-ids and hide the actual error message.

0.5.1
first version, compatible to Cinnamon Server version 0.5.1

Fixed bug in version-command, which would throw a NFE if version returned an empty string.
added removePermissionFromAclEntry(permId, aeId)
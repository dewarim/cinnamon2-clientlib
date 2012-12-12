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

import java.util.Properties;

public class LifecycleTest extends BaseTest{

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

    Long testLifecycleId;
    Long authoringState;
    Long publishedState;
    Long reviewState;
    Long osd;

	public LifecycleTest(){
		super();
	}

	public LifecycleTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}

	@BeforeClass
	public void setUp(){
		super.setUp();
        client.initializeComponent("initializelifecycles");
        Long folderId = client.createFolder("lifecycletestfolder", 0L);
        osd = client.create("<meta/>", "LifecycleTestObject", folderId);
	}

	@Test
	public void listLifecycleTest(){
        Document lifeCycleList = ParamParser.parseXmlToDocument(client.listLifeCycles());
        log.debug(lifeCycleList.asXML());
        Node lcNode =  lifeCycleList.selectSingleNode("//lifecycle[name='Test Lifecycle']");
        assert lcNode != null : "did not receive expected lifecycle." + lifeCycleList.asXML();
        testLifecycleId = Long.parseLong(lcNode.selectSingleNode("id").getText());
        authoringState = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='authoring']/id").getText());
        publishedState = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='published']/id").getText());
        reviewState    = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='review']/id").getText());
	}

    @Test(dependsOnMethods = {"listLifecycleTest"})
    public void attachAndDetachLifecycleTest(){
        assert testLifecycleId > 0L;
        assert osd > 0L;
        client.lock(osd);
        String result = client.attachLifeCycle(osd, testLifecycleId);
        assert result.contains("success") : "Failed to attach lifecycle:\n"+result;

        Document osdDoc = ParamParser.parseXmlToDocument(client.getObject(osd));
        log.debug(osdDoc.asXML());
        assert osdDoc.selectSingleNode("//lifeCycleState").getText().equals(authoringState.toString()) :
                "OSD does not have authoring state."+osdDoc.asXML();

        result = client.detachLifeCycle(osd);
        assert result.contains("success") : "Failed to detach Lifecycle:\n"+result;

        osdDoc = ParamParser.parseXmlToDocument(client.getObject(osd));
        assert osdDoc.selectSingleNode("//lifeCycleState").getText().length() == 0 :
                "OSD still has lifecyclestate."+osdDoc.asXML();

        try{
            result = client.attachLifeCycle(osd, testLifecycleId, publishedState);
            assert false;
        }
        catch (Exception ex){
            assert ex.getMessage().contains("error.enter.lifecycle"):
                    "Managed to attach lifecycle with published state:\n"+result;
        }
        client.unlock(osd);
    }


    @Test(dependsOnMethods = {"attachAndDetachLifecycleTest"})
    public void getNextStatesTest(){
        client.lock(osd);
        String result = client.attachLifeCycle(osd, testLifecycleId, authoringState);
        assert result.contains("success");

        result = client.getNextStates(osd);
        assert result.contains("review") : "getNextStates does not offer review state.\n"+result;
        assert ! result.contains("authoring") : "getNextStates does offer already active authoring state.\n"+result;
        assert ! result.contains("published") : "getNextStates does offer forbidden published state.\n"+result;

        result = client.detachLifeCycle(osd);
        assert result.contains("success");

        try{
            result = client.getNextStates(osd);
            assert false : "getNextStates did not throw an error on OSD without lifecycle."+result;
        }
        catch (Exception ex){
            assert ex.getMessage().contains("error.no_lifecycle_set") : "Unexpected error: "+result;
        }
        client.unlock(osd);
    }

    @Test(dependsOnMethods = {"getNextStatesTest"})
    public void changeLifecycleTest(){
        client.lock(osd);
        String result = client.attachLifeCycle(osd, testLifecycleId, authoringState);
        assert result.contains("success");
        result = client.changeLifeCycleState(osd, reviewState);
        assert result.contains("success") : "Failed to change lifecycle to review state.";
        result = client.changeLifeCycleState(osd, publishedState);
        assert result.contains("success") : "Failed to change lifecycle to published state:\n"+result;

        Document osdDoc = ParamParser.parseXmlToDocument(client.getObject(osd));
        assert osdDoc.selectSingleNode("//lifeCycleState").getText().equals(publishedState.toString()) :
                "OSD has wrong lifecyclestate."+osdDoc.asXML();

        try{
            result = client.changeLifeCycleState(osd, authoringState);
            assert false;
        }
        catch (Exception ex){
            assert ex.getMessage().contains("error.enter.lifecycle"):
                    "Managed to change lifecycle from published to authoring state:\n"+result;
        }
        client.unlock(osd);
    }

    /**
     * Test for the ChangeAclState.class, which sets the ACL of an object upon state change.
     */
    @Test
    public void changeAclStateTest(){
        String osdXml = client.getObject(osd);
        Long currentAclId = Client.parseLongNode(osdXml, "//aclId");
        Long lcId = getLifeCycleIdByName("TestAclLc");
        client.lock(osd);
        String result = client.attachLifeCycle(osd, lcId);
        assert result.contains("success") : "Failed to attach TestAclLc to osd.\n"+result;

        Document lifeCycleList = ParamParser.parseXmlToDocument(client.listLifeCycles());
        Node lcNode =  lifeCycleList.selectSingleNode("//lifecycle[name='TestAclLc']");
        Long otherStateId = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='otherAclState']/id").getText());
        String changeStateResult = client.changeLifeCycleState(osd, otherStateId);
        assert changeStateResult.contains("success") : "Failed to change LifeCycleState. ";
        osdXml = client.getObject(osd);
        Long newAclId = Client.parseLongNode(osdXml, "//aclId");

        assert ! newAclId.equals(currentAclId) : "AclId did not change after LifeCycleState changed.";
        client.unlock(osd);
    }

    Long getLifeCycleIdByName(String name){
        Document lifeCycleList = ParamParser.parseXmlToDocument(client.listLifeCycles());
        log.debug(lifeCycleList.asXML());
        Node lcNode =  lifeCycleList.selectSingleNode("//lifecycle[name='"+name+"']");
        assert lcNode != null : "did not receive expected lifecycle "+name+"\n" + lifeCycleList.asXML();
        return Long.parseLong(lcNode.selectSingleNode("id").getText());
    }

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}


}
package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import server.global.Constants;
import utils.ParamParser;

import java.util.Properties;

public class RenderServerConnectorTest extends BaseTest{

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public RenderServerConnectorTest(){
		super();

	}

	public RenderServerConnectorTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
        createRenderServerLifeCycle();
	}

    void createRenderServerLifeCycle(){
        // create Lifecycle.
        Long renderLc = client.createLifeCycle(Constants.RENDER_SERVER_LIFECYCLE);

        // create LifeCycleState
        // new is last element in lcList so it becomes the default for the LC:
        String[] lcList = {"rendering", "finished", "failed", "new"};
        for(String name : lcList){
            Long id = client.addLifeCycleState(renderLc, name, null, true, "server.lifecycle.state.NopState", null);
            log.debug("Created lifeCycleState "+name+" with id "+id+".");
        }
    }


	@Test
	public void startRenderTaskTest(){
        Long testFolder = client.createFolder("TestFoolDer", 0L);
	    Long id = client.startRenderTask(testFolder, "<?xml version='1.0' encoding='UTF-8'?><metaset type='render_input'><test /><renderTaskName>foo</renderTaskName></metaset>");
        String metadata = client.getMeta(id);
        Document doc = ParamParser.parseXmlToDocument(metadata);
        Node testNode = doc.selectSingleNode("/meta/metaset[@type='render_input']/test");
        assert testNode != null : "Metadata does not contain <test/> element: \n"+metadata;

        String objectType = client.getSysMeta(id, null, "objtype");
        assert objectType.equals(Constants.OBJECT_TYPE_RENDER_TASK) :
                "ObjectType is not "+Constants.OBJECT_TYPE_RENDER_TASK+" but: "+objectType;
	}


    // unfinished.
//    @Test(dependsOnMethods={"startRenderTaskTest"})
//    public void multipleObjectsTest(){
//        Long folderId = client.createFolder("FullRenderFolder", 0L);
//        String metadata = "<metaset type=\"render_input\"><sourceId>0</sourceId><renderTaskName>foo2pdf</renderTaskName></metaset>";
//        for(int x = 0; x < 20; x++){
//            Long id = client.startRenderTask(folderId, metadata);
//        }
//        // do nothing.
//    }

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

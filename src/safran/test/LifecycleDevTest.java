package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import safran.setup.BasicDatabaseSetup;
import safran.setup.DatabaseSetup;
import utils.ParamParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class LifecycleDevTest extends BaseTest {

    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(this.getClass());

    Long testLifecycleId;
    Long authoringState;
    Long publishedState;
    Long reviewState;
    Long osd;

    public LifecycleDevTest() {
        super();
    }

    public LifecycleDevTest(Properties config, Client client) {
        this.config = config;
        this.client = client;
    }

    @BeforeClass
    public void setUp() {
        try {
            String propertiesFilename = "config.properties";
            assert new File(propertiesFilename).exists() : "config.properties was not found.";
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                    propertiesFilename));
            config.load(bis);
            bis.close();

            String[] properties = {"server.url", "server.username",
                    "server.password", "default_repository"};
            for (String p : properties) {
                log.debug(String.format("testing property: %s==%s", p, config
                        .getProperty(p)));
                assert config.getProperty(p) != null : String.format(
                        "Property %s is not set.", p);
            }

            client = new Client(config);

            assert client.connect() : "Could not connect to server.";
            assert client.getSessionTicket() != null : "SessionTicket is null.";

            Long folderId = client.createFolder("lifecycletestfolder" + Math.random(), 0L);
            osd = client.create("<meta/>", "LifecycleTestObject", folderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void listLifecycleTest() {
        Document lifeCycleList = ParamParser.parseXmlToDocument(client.listLifeCycles());
        log.debug(lifeCycleList.asXML());
        Node lcNode = lifeCycleList.selectSingleNode("//lifecycle[name='review.lc']");
        assert lcNode != null : "did not receive expected lifecycle." + lifeCycleList.asXML();
        testLifecycleId = Long.parseLong(lcNode.selectSingleNode("id").getText());
        authoringState = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='authoring.lcs']/id").getText());
        reviewState = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='review.lcs']/id").getText());
        publishedState = Long.parseLong(lcNode.selectSingleNode("//lifecycleState[name='released.lcs']/id").getText());
    }

    /*
    manual SQL setup:
       update change_triggers set config='<config><logEverything>false</logEverything></config>' where id=6;
       update change_triggers set config='<config><logEverything>true</logEverything></config>' where id=6;
       update change_triggers set config='<config><lifecycles><lifecycle id=''99999''/></lifecycles></config>' where id=6;
       update change_triggers set config='<config><lifecycles><lifecycle id=''840''/></lifecycles></config>' where id=6;
       update change_triggers set config='<config><lifecycles><lifecycle id=''840''><stateId>845</stateId></lifecycle></lifecycles></config>' where id=6;
    
     */

    @Test(dependsOnMethods = {"listLifecycleTest"})
    public void attachLifecycleTest() {
        assert testLifecycleId > 0L;
        assert osd > 0L;
        client.lock(osd);
        String result = client.attachLifeCycle(osd, testLifecycleId);
        assert result.contains("success") : "Failed to attach lifecycle:\n" + result;

        Document osdDoc = ParamParser.parseXmlToDocument(client.getObject(osd));
        log.debug(osdDoc.asXML());
        assert osdDoc.selectSingleNode("//lifeCycleState").getText().equals(authoringState.toString()) :
                "OSD does not have authoring state." + osdDoc.asXML();
        result = client.changeLifeCycleState(osd, reviewState);
        assert result.contains("success") : "Failed to change lifecycle to review state:\n" + result;
        result = client.changeLifeCycleState(osd, publishedState);
        assert result.contains("success") : "Failed to change lifecycle to published state:\n" + result;
        client.unlock(osd);
    }


    Long getLifeCycleIdByName(String name) {
        Document lifeCycleList = ParamParser.parseXmlToDocument(client.listLifeCycles());
        log.debug(lifeCycleList.asXML());
        Node lcNode = lifeCycleList.selectSingleNode("//lifecycle[name='" + name + "']");
        assert lcNode != null : "did not receive expected lifecycle " + name + "\n" + lifeCycleList.asXML();
        return Long.parseLong(lcNode.selectSingleNode("id").getText());
    }

    @AfterClass
    public void disconnectTest() {
        boolean result = client.disconnect();
        assert result : "Disconnect form Server failed!";
    }


}
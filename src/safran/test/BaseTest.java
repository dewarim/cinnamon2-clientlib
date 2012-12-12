package safran.test;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.xml.sax.SAXException;
import safran.Client;
import safran.setup.BasicDatabaseSetup;
import safran.setup.DatabaseSetup;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class BaseTest {

	protected transient Logger log = LoggerFactory.getLogger(this.getClass());
	protected Client client;
	protected Properties	config	= new Properties();

    Document permissionNames;

	public void setUp(){
		doSetup(true, "config.properties");
	}
	
	public void setUp(Boolean initializeDatabase){
		doSetup(initializeDatabase, "config.properties");
	}
	
	public void setUp(Boolean initializeDatabase, String propertiesFilename){
		doSetup(initializeDatabase, propertiesFilename);
	}	

	public void doSetup(Boolean initializeDatabase, String propertiesFilename){
		try {
            client = createClient(propertiesFilename);
			
			DatabaseSetup setup = new BasicDatabaseSetup(client, config);
			if(initializeDatabase){
				assert setup.initializeDatabase() : "Could not initialize database";
			}
			else{
				setup.reloadCinnamon();
			}
			assert client.connect() : "Could not connect to server.";
			assert client.getSessionTicket() != null : "SessionTicket is null.";
			log.debug("finished setUp");
			// note: if you finish setUp but sessionTicket later on is still
			// null,
			// check whether you have enabled assertions (java -ea).

			XMLUnit.setIgnoreWhitespace(true);
		} catch (FileNotFoundException e) {
			log.debug("", e);
			assert false : "encountered FileNotFound-Exception.";
		} catch (IOException e) {
			log.debug("", e);
			assert false : "encountered IOException.";
		}
	}
	
    public Client createClient(String propertiesFilename) throws IOException{
        assert new File(propertiesFilename).exists() : "config.properties was not found.";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                propertiesFilename));
        config.load(bis);
        bis.close();

        String[] properties = { "server.url", "server.username",
                "server.password", "default_repository" };
        for (String p : properties) {
            log.debug(String.format("testing property: %s==%s", p, config
                    .getProperty(p)));
            assert config.getProperty(p) != null : String.format(
                    "Property %s is not set.", p);
        }
        return new Client(config);
    }
    
	public BaseTest() {
		super();
	}

	public Document parseXmlResponse(String result){
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(result);
		} catch (Exception e) {
			assert false : "failed to parse xml-Response:\n "+result;
		}
		return doc;
	}
	
	/**
	 * @return the config
	 */
	public Properties getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(Properties config) {
		this.config = config;
	}

	/**
	 * @return the client
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * @param client the client to set
	 */
	public void setClient(Client client) {
		this.client = client;
	}

	/**
	 * Check if an Exception contains the expected message or something else.
	 * @param e - the Exception to test
	 * @param testName - the name of the test which failed
	 * @param message - the expected message
	 */
	public void checkExceptionMessage(Exception e, String testName, String message){
		assert e.getMessage().contains(message) :
			String.format("Test %s generated an unexpected exception. Expected: %s\nReceived:%s",
					testName, message, e.getMessage());
	}
	
	public static void main(String[] args){
		new BaseTest().doSetup(true, "config.properties");
	}

    public void checkDiff(String methodName, String expectedData, String actualData) {
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
        assert myDiff.similar() : "XML-Diff failed: \n received:\n" + actualData
                + "\n expected:\n" + expectedData + "\n" + myDiff.toString();
    }

    public void checkDetailedDiff(String methodName, String expectedData, String actualData) {
        DetailedDiff myDiff = null;
        try {
            myDiff = new DetailedDiff(new Diff(expectedData, actualData));
        } catch (SAXException e) {
            e.printStackTrace();
            assert false : methodName + " failed in xml-diff\n" + actualData;
        } catch (IOException e) {
            e.printStackTrace();
            assert false : methodName + " failed in xml-diff";
        }
        List allDifferences = myDiff.getAllDifferences();
        Assert.assertEquals(0, allDifferences.size(), myDiff.toString());
    }

    Long getPermissionNameId(String name){
        if(permissionNames == null){
            permissionNames = parseXmlResponse(client.listPermissions());
        }
        String xpath = String.format("/permissions/permission[sysName='%s']/id", name);
        return Long.parseLong(permissionNames.selectSingleNode(xpath).getText());
    }
    
    public Client fetchNewClient(String propertiesFilename) throws IOException{
        assert new File(propertiesFilename).exists() : "config.properties was not found.";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                propertiesFilename));
        config.load(bis);
        bis.close();

        String[] properties = { "server.url", "server.username",
                "server.password", "default_repository" };
        for (String p : properties) {
            log.debug(String.format("testing property: %s==%s", p, config
                    .getProperty(p)));
            assert config.getProperty(p) != null : String.format(
                    "Property %s is not set.", p);
        }

        return new Client(config);
    }
    
}
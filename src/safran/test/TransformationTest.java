package safran.test;

import org.dom4j.Document;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;
import utils.ContentReader;
import utils.ParamParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Testing the TransformationEngine.
 * Test cases:
 * <h2>listTransformations()</h2>
 * Retrieve a list of transformation entities.
 * <h2>Transformation actions</h2>
 * <h3>Parameter tests</h3> 
 * <ul>
 * 	<li>invalid object id</li>
 * 	<li>invalid transformer_id</li>
 * 	<li>PermissionNames</li>
 * </ul>
 * <h3>transformObject</h3>
 * 	<ul>
 * 		<li>check new content</li>
 * 		<li>check new format</li>
 *  </ul>
 * <h3>transformObjectToFile</h3>
 * 	<ul>
 * 		<li>check file response</li>
 * </ul>
 * 
 */
public class TransformationTest extends BaseTest{
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	Long folderId = 0L;
	Long transformerId = 0L;
	Long sourceId = 0L;
	File testData = null;
	
	public TransformationTest(){
		super();
	}
	
	public TransformationTest(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
		OSD_Test osdT = new OSD_Test(config, client);
		osdT.createFolderTest();
		folderId = osdT.getFolderId();
		
		try {
			testData = File.createTempFile("transformationTest", ".xml");
			FileWriter f = new FileWriter(testData);
			f.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><body><h1>TransformationTest</h1><p>This is just a test string</p></body></html>");
			f.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sourceId = client.create("", "testObject", testData.getAbsolutePath(), "xml", "text/xml", folderId);
	}
	
	@Test
	public void listTransformers(){
		String xml = client.listTransformers();
		Document doc = ParamParser.parseXmlToDocument(xml, null);
		assert doc.selectNodes("/transformers/transformer").size() == 1 :
			"Expected: 1 Transformer; Received:\n+xml"; 
		Node transNode = doc.selectSingleNode("/transformers/transformer/id");
		assert transNode != null : "No Transformer found!";
		transformerId = Long.parseLong(transNode.getText());
		assert transformerId > 0L : "TransformerId is not a positve number!";
	}
	
	@Test(dependsOnMethods={"listTransformers"})
	public void invalidOSDTest(){
		try{
			client.transformObject(0L, transformerId, "");
			assert false : "invalidOSDTest succeeded without valid sourceId";
		}
		catch (Exception e) {
			assert e.getMessage().contains("error.object.not.found") :
				"received unexpected error message\n"+e.getMessage();
		}		
	}
	
	@Test(dependsOnMethods={"listTransformers"})
	public void invalidTransformerIdTest(){		
		try{
			client.transformObject(sourceId, 0L, "-");
			assert false : "invalidTransformerId succeeded without valid transformerId";
		}
		catch (Exception e) {
			assert e.getMessage().contains("error.transformer.not_found") : 
				"received unexpected error message\n"+e.getMessage();
		}		
	}
	
	@Test(dependsOnMethods={"listTransformers"})
	public void missingPermissionNamesTest(){
		try{
			client.createUser("-", "Mr. Foo", "foo", "bar", "none");
			Client c = new Client(config.getProperty("server.url"), "foo", "bar",
					config.getProperty("default_repository"));
			assert c.connect();
//			String result = 
				c.transformObject(sourceId, transformerId, "-");
//			log.debug("missingPermissionNamesTest:\n"+result);
			assert false : "missingPermissionNamesTest succeeded without valid PermissionNames.";
		}
		catch (Exception e) {
			assert e.getMessage().contains("error.missing.PermissionName._write_object_content") :
				"received unexpected error message\n"+e.getMessage();
		}		
	}
	
	@Test
	public void pdfContentTest(){
		client.transformObject(sourceId, transformerId, "-");
		String xml = client.getContent(sourceId);
		assert xml.contains("%PDF-1.4") : "Missing PDF-1.4 stamp:\n"+xml;
	}
	
	@Test
	public void transformToFileTest(){
		client.lock(sourceId);
		client.setContent(testData, "xml", sourceId);
		client.unlock(sourceId);
		File pdf = client.transformObjectToFile(sourceId, transformerId, "-");
		String xml = "";
		try {
			xml = ContentReader.readFileAsString(pdf.getAbsolutePath());
		} catch (IOException e) {
			assert false : "Reading content of PDF failed.";
		}
		assert  xml.contains("%PDF-1.4") : "Missing PDF-1.4 stamp:\n"+xml;
	}
		
	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

}

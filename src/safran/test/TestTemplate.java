package safran.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;

import java.util.Properties;

public class TestTemplate extends BaseTest{
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public TestTemplate(){
		super();
	}
	
	public TestTemplate(Properties config, Client client){
		this.config = config;
		this.client = client;
	}
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}
	
	@Test
	public void fooTest(){
	
	}

	@AfterClass
	public void disconnectTest() {
		boolean result = client.disconnect();
		assert result : "Disconnect form Server failed!";
	}

	
}

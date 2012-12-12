package safran.setup;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import safran.Client;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * <p>
 * The BasicDatabaseSetup class allows you to connect to a Cinnamon server
 * and create an admin user and some other items needed to successfully create
 * new users and objects.
 * </p>
 * <p>
 * Note that the server checks whether there are already users in the
 * database and will not change the data in that case.
 * </p>
 * <p>This program is designed as a TestNG class, so you can use it with a test
 * suite to generate a valid database on start.</p> 
 * @author Ingo Wiarda
 *
 */
public class BasicDatabaseSetup implements DatabaseSetup{	
	
	String serverUrl;
	Client client;
	Properties config = new Properties();
	transient Logger log = LoggerFactory.getLogger(this.getClass());
	
	public BasicDatabaseSetup(){
		
	}
	
	public BasicDatabaseSetup(Client client, Properties config){
		this.client = client;
		this.config = config;
	}
	
	@BeforeClass
	public void setUp() throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream("config.properties"));
		config.load(bis);
		bis.close();

		serverUrl = config.getProperty("server.url");
		client = new Client(serverUrl, config.getProperty("server.username"), config.getProperty("server.password"));		
	}

	@Test
	public void reloadCinnamon(){
		String server = config.getProperty("server");
		Integer port = Integer.valueOf(config.getProperty("port"));
		String tomcat_manager = config.getProperty("tomcat_manager");
		String tomcat_password = config.getProperty("tomcat_password");
		String path_to_servlet = config.getProperty("path_to_servlet");
		String tomcat_manager_url = config.getProperty("tomcat_manager_reload");
		
		HttpClient httpClient = new HttpClient();
		HttpMethod query = new GetMethod(String.format("http://%s:%s%s?path=%s",
				server, port, tomcat_manager_url, path_to_servlet));
				 
		int resp;
		try{
	
			httpClient.getState().setCredentials(
					new AuthScope(server,port), 
					new UsernamePasswordCredentials(tomcat_manager, tomcat_password));
			
			resp = httpClient.executeMethod(query);
			log.debug("ResponseValue from httpclient: "+resp);		
			String response = query.getResponseBodyAsString().trim();			
			//			log.debug("Server Response: "+response);
			Boolean result = response.contains("OK - Reloaded application at context path "+path_to_servlet);
			assert result : "Failed to reload Cinnamon:\n"+response;
		}
		catch(IOException e){
			log.debug("",e);			
		}
		finally{
			query.releaseConnection();
		}
	}
	
	@Test(dependsOnMethods = {"reloadCinnamon"})
	public void initializeDatabaseTest() {
        // commented out because we are now using auto-initialize in cmn_test.
        // to enable auto-initialization, see:
        // http://cinnamon-cms.de/cinnamonserver/administration/cinnamon_config.xml
//		Boolean result = client.initializeDatabase(config.getProperty("default_repository"));
//		assert result : "Could not initialize Database";

//		result = client.initializeDatabase(config.getProperty("default_repository"));
//		assert ! result : "Database initialization reported success on second run";
	}
	
	/**
	 * Reload Cinnamon and initialize using the current client and configuration.
	 * @return true on success, false on error
	 */
	public boolean initializeDatabase(){
		try{
			reloadCinnamon();
			initializeDatabaseTest();
		}
		catch (Exception ex) {
			log.debug("",ex);
			return false;
		}
		return true;
	}
	
}

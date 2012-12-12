package safran.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ContentReader;
import utils.ParamParser;

import java.io.File;

public class SessionTest extends BaseTest{
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@BeforeClass
	public void setUp(){
		super.setUp();
	}

    /**
     * This test requires you to set the session timeout in cinnamon_config.xml to < 10 seconds.*
     */
//	@Test
	public void sessionTimeoutTest(){
		try {
            /*
             * During setup, the client connects to the server.
             * We wait for 12 seconds, after which the session should be invalid.
             * The client tries a reconnect if it receives a "session expired" message.
             *
             */
			// wait for 10000 milliseconds.
			Thread.sleep(12000L);
			String msg = client.getObjTypes(); // any action could result in timeout.
			log.debug("timeoutMessage: "+msg);
			assert ! msg.contains("error.session.expired") : "Did receive session expired message:\n"+msg;
		} catch (Exception e) {
			assert false : "unexpected Exception: "+e.getMessage();
		}
		
	}

    /**
     * This test requires you to set the session timeout in cinnamon_config.xml to < 10 seconds.*
     */
	@Test
	public void sessionTimeoutOnDownloadTest(){
		try {
            /*
             * During setup, the client connects to the server.
             * We wait for 12 seconds, after which the session should be invalid.
             * The client tries a reconnect if it receives a "session expired" message.
             *
             */

            File upload = OSD_Test.createTestFile();
            Long id = client.create("", "sessionUpload", 0L);
            client.lock(id);
            client.setContent(upload, "xml", id);
            client.unlock(id);

			// wait for 10000 milliseconds.
			Thread.sleep(12000L);
			File file = client.getContentAsFile(id); // any action could result in timeout.
            String msg = ContentReader.readFileAsString(file.getAbsolutePath());
			log.debug("downloadedFile: "+msg);
			assert ! msg.contains("error.session.expired") : "Did receive session expired message:\n"+msg;
		} catch (Exception e) {
			assert false : "unexpected Exception: "+e.getMessage();
		}

	}

    @Test
    public void forkSessionTest(){
        String oldTicket = client.getSessionTicket();
        String ticket = client.forkSession();
        assert ! oldTicket.equals(ticket) : "oldTicket == newTicket!";
        String groupsBefore = client.listGroups(null);
        assert ticket != null &&
               ticket.length() > 0 && ticket.length() < 65 : "Received invalid session ticket in forkSessionTest.";

        client.setSessionTicket(ticket);
        String groupsAfter = client.listGroups(null);
        assert groupsBefore.equals(groupsAfter) :
                "Something's wrong - received different responses to listGroups after forking session.";
        client.disconnect();
        client.setSessionTicket(oldTicket);
    }

}

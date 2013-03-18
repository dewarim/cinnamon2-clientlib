package safran.test

import safran.Client
import spock.lang.Shared
import spock.lang.Specification

/**
 * Basic Spock specification for client tests.
 * Setup and connect a client, look up the test folder in /system/test
 * and create a temporary test file with XML content.
 */
class BaseClientSpecification extends Specification{
    
    @Shared
    Client client
    @Shared
    Long testFolder
    @Shared
    File testFile
    @Shared
    def content = "<xml><data>content</data></xml>"

    def setupSpec(){
        def config = new Properties()
        config.load(new File('config.properties').newReader())
        client = new Client(config)
        client.connect()
        testFolder = Client.parseLongNode(client.getFolderByPath('system/test'), '/folders/folder/id')
        testFile = File.createTempFile('Cinnamon3-test-file', 'xml')
        testFile.write(content)
    }
    
    def cleanupSpec(){
        client.disconnect()
    }
    
    
}

package safran.test

import safran.Client
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

/**
 * Testing API-methods for OSD content CRUD 
 */
@Stepwise
class OsdContentSpec extends BaseClientSpecification{

    @Shared
    Long osd    
    
    def "create empty test object"(){
        when:
        osd = client.create('<meta/>', 'empty osd', testFolder)
        
        then:
        osd > 0
    }
    
    def "lock and unlock"(){
        when:        
        client.unlock(osd)
        
        then:
        // unlocking an already unlocked object should fail. 
        thrown RuntimeException
        
        when:
        client.lock(osd)
        
        then:
        client.unlock(osd)
    }
    
    def "set content"(){
        given:
        client.lock(osd)
        
        when:
        client.setContent(testFile, 'xml', osd)
        
        then:
        client.getContent(osd) == content
        
        cleanup:
        client.unlock(osd)
    }
    
    def "delete content"(){
        given:
        client.lock(osd)
        
        when:
        client.setContent(null, null, osd)
        
        then:
        def osdXml = client.getObject(osd)
        def xml = new XmlSlurper().parseText(osdXml)
        xml.object.contentsize.text().length() == 0
        xml.object.format.name.text().length() == 0
        
        cleanup:
        client.unlock(osd)
    }
}

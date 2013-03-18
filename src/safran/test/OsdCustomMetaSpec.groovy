package safran.test

import safran.Client
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

/**
 * Testing API-methods for OSD content CRUD 
 */
@Stepwise
class OsdCustomMetaSpec extends BaseClientSpecification{
    
    @Shared
    Long osd
    
    def customMeta = '<meta><metaset type="test">TestData</metaset></meta>'

    def "create empty test object"(){
        when:
        osd = client.create('<meta/>', 'empty osd', testFolder)

        then:
        osd > 0
    }
    
    def "add metadata"(){
        given:
        client.lock(osd)
        
        when:
        client.setMeta(osd, customMeta)
        
        then:
        def result = client.getMeta(osd)
        def xml = new XmlSlurper().parseText(result)
        xml.metaset.'@type' == 'test'
        xml.metaset.text() == 'TestData'
        
        cleanup:
        client.unlock(osd)
    }
    
}

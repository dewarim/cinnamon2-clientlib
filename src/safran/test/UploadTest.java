package safran.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Upload Tests:
 * 1. giantUploadTest: try to upload a 250'000'000 Byte file.
 */
public class UploadTest extends BaseTest {

    Long folderId = 0L;

    @BeforeClass
    public void setUp() {
        super.setUp(true);

        folderId = client.createFolder("testFolder", 0L);
        assert folderId != 0L;

    }


    @Test()
    public void giantUploadTest() throws IOException {
        Date startTime = new Date();

        // create 256 MB file
        String gutPath = System.getProperty("java.io.tmpdir", "/tmp") + File.separator + "giantUploadTest.dat";
        File bigFile = new File(gutPath);
        if (!bigFile.exists()) {
            FileOutputStream fos = new FileOutputStream(bigFile);
            for (int x = 1; x < 65536 * 4; x++) {
                fos.write(x);
            }
            fos.close();
        }
        Long id = client.create("", gutPath, bigFile.getAbsolutePath(), "png", "image/png", folderId);
        assert id != 0L;

        Date endTime = new Date();
        Long diff = (endTime.getTime() - startTime.getTime()) / 1000;
        log.debug("Time needed to create and upload giant upload file: " + diff + " seconds.");

    }

    @AfterClass
    public void disconnectTest() {
        boolean result = client.disconnect();
        assert result : "Disconnect form Server failed!";
    }

}

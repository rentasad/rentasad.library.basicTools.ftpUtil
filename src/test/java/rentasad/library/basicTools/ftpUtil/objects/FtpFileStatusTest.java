package rentasad.library.basicTools.ftpUtil.objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Calendar;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

public class FtpFileStatusTest {

    @Test
    public void testGetAgeOfFtpFile() {
        FTPFile ftpFile = new FTPFile();
        File localFile = new File("test.txt");
        FtpFileStatus status = new FtpFileStatus(ftpFile, localFile);
        
        // Test with null timestamp
        assertNull(status.getAgeOfFtpFile());
        
        // Test with valid timestamp
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -10);
        status.setTimestampFtpFile(cal);
        
        Long age = status.getAgeOfFtpFile();
        assertNotNull(age);
        // Age should be around 10 minutes (600,000 ms), allow some margin
        assertTrue(age >= 590000 && age <= 610000);
    }

    @Test
    public void testGetAgeOfLocalFile() {
        FTPFile ftpFile = new FTPFile();
        File localFile = new File("test.txt");
        FtpFileStatus status = new FtpFileStatus(ftpFile, localFile);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5);
        status.setTimestampLocalFile(cal);
        
        Long age = status.getAgeOfLocalFile();
        assertNotNull(age);
        assertTrue(age >= 290000 && age <= 310000);
    }
    
    @Test
    public void testGetAgeOfLocalFileWithNullTimestamp() {
        FTPFile ftpFile = new FTPFile();
        File localFile = new File("test.txt");
        FtpFileStatus status = new FtpFileStatus(ftpFile, localFile);
        status.setTimestampLocalFile(null);
        assertThrows(NullPointerException.class, () -> {
            status.getAgeOfLocalFile();
        });
    }
}

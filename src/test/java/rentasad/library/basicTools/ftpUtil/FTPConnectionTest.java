package rentasad.library.basicTools.ftpUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.objects.FtpSettings;

public class FTPConnectionTest
{
	/**
	 * Here you should configure the TEST FTP-SERVER for unti testing
	 */
	private final String FTP_HOSTNAME = "127.0.01";
	private final String FTP_USERNAME = "user";
	private final String FTP_PASSWORD = "123";
	private final Integer FTP_PORT = 21;
	
	
    FTPConnection ftpConnection = null;;
    boolean startTest;

    @BeforeEach
    /**
     * Only test if test system is windows based
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        this.startTest = true;
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        if (this.ftpConnection != null)
        {
            this.ftpConnection.disconnect();
        }
    }

    @Test
    public void testGetFTPFileList() throws Exception
    {
        if (startTest)
        {
            /* Richtige FTP Zugangsdaten */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);

            assertNotNull(ftpConnection.getFTPFileList(), "Objekt wird Null zuraeckgegeben");

            // throw new RuntimeException("not yet implemented");
        }
    }

    @Test
    public void testGetFTPFileListWrongUsername() throws Exception
    {
        if (startTest)
        {
            /* Falscher Benutzername */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername("WRONG_USER");
            this.ftpConnection = new FTPConnection(settings);

            assertThrows(FtpLoginException.class, () -> {
                ftpConnection.getFTPFileList();
            });
        }
    }

    @Test
    public void testGetFTPFileListWrongPassword() throws Exception
    {
        if (startTest)
        {
            /* Falscher Benutzername */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword("WRONG_PASSWORD");
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);

            assertThrows(FtpLoginException.class, () -> {
                ftpConnection.getFTPFileList();
            });
        }
    }

    @Test
    public void testGetFTPFileListWrongHostname() throws Exception
    {
        if (startTest)
        {
            /* Falscher Benutzername */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost("wrong-hostname-that-does-not-exist.local");
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);
            assertThrows(UnknownHostException.class, () -> {
                ftpConnection.getFTPFileList();
            });
        }
    }

    @Test
    public void testConnect() throws SocketException, IOException, FtpLoginException
    {
        if (startTest)
        {
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);
            assertTrue(ftpConnection.connect(), "Verbindung fehlgeschlagen mit " + settings.getFtpHost());
            ftpConnection.disconnect();
        }
    }

    @Test
    public void testDownloadCorrectParameters() throws Exception
    {
        if (startTest)
        {
            /*
             * Richtige FTP Zugangsdaten
             * Test Download einer Datei
             */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);

            // Zuerst eine Datei hochladen, um sie dann herunterzuladen
            String localSourceFileName = "resources/unittest/crcValueFileTest.csv";
            String remoteResultFileName = "downloadTest.csv";
            this.ftpConnection.upload(localSourceFileName, remoteResultFileName);

            List<FTPFile> ftpFiles = ftpConnection.getFTPFileList();
            FTPFile file = null;
            for (FTPFile f : ftpFiles) {
                if (f.getName().equals(remoteResultFileName)) {
                    file = f;
                    break;
                }
            }
            assertNotNull(file);
            File localResultFile = File.createTempFile("download_", ".csv");
            assertTrue(this.ftpConnection.download(localResultFile.getAbsolutePath(), file), "Download fehlgeschlagen");
            localResultFile.delete();
            this.ftpConnection.deleteFileFromFtp(remoteResultFileName);
        }
    }

    @Test
    public void testDownloadNotExistentFile() throws Exception
    {
        if (startTest)
        {
            /*
             * Richtige FTP Zugangsdaten
             * Test Download einer Datei
             */
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);

            FTPFile file = new FTPFile();
            file.setName("WRONG.TXT");
            file.setSize(100);
            assertFalse(this.ftpConnection.download("resources/WRONG.TXT", file), "Falscher Raeckgabewert");
        }
    }

    @Test
    public void testUpload() throws Exception
    {
        if (startTest)
        {
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);
            String localFile = "resources/unittest/crcValueFileTest.csv";
            String remoteFile = "uploadTest.xml";
            assertTrue(this.ftpConnection.upload(localFile, remoteFile));
            assertTrue(this.ftpConnection.existFile(remoteFile));
            this.ftpConnection.deleteFileFromFtp(remoteFile);
        }
    }

    @Test
    public void testExistFile() throws Exception
    {
        if (startTest)
        {
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);
            ftpConnection.connect();
            String localFile = "resources/unittest/crcValueFileTest.csv";
            String filename = "existTest.csv";
            this.ftpConnection.upload(localFile, filename);
            assertTrue(this.ftpConnection.existFile(filename));
            this.ftpConnection.deleteFileFromFtp(filename);
            ftpConnection.disconnect();
        }
    }

    @Test
    public void testExistFileNotExistent() throws Exception
    {
        if (startTest)
        {
            FtpSettings settings = new FtpSettings();
            settings.setFtpPassword(FTP_PASSWORD);
            settings.setFtpPort(FTP_PORT);
            settings.setFtpHost(FTP_HOSTNAME);
            settings.setFtpUsername(FTP_USERNAME);
            this.ftpConnection = new FTPConnection(settings);
            assertFalse(this.ftpConnection.existFile("auftrag2.xml"));
        }
    }

    @Test
    public void testGetCRCFromInputStreamFileNotExist() throws Exception
    {
        if (startTest)
        {
            String fileName = "resources/unittest/nonexistent.file";
            assertThrows(FileNotFoundException.class, () -> {
                new FileInputStream(new File(fileName));
            });
        }
    }

    @Test
    public void testGetCRCFromInputStreamFileExist() throws Exception
    {
        if (startTest)
        {
            String fileName = "resources/unittest/crcValueFileTest.csv";
            FileInputStream fileInputStream = new FileInputStream(new File(fileName));
            Long crcLong = Long.valueOf(FTPConnection.getCRCFromInputStream(fileInputStream));
            assertNotNull(crcLong);
            assertTrue(crcLong > 0);
        }
    }

    @Test
    public void testGetShaHashsumCrcFromFtpFile() throws Exception
    {
        FtpSettings settings = new FtpSettings();
        settings.setFtpPassword(FTP_PASSWORD);
        settings.setFtpPort(FTP_PORT);
        settings.setFtpHost(FTP_HOSTNAME);
        settings.setFtpUsername(FTP_USERNAME);
        this.ftpConnection = new FTPConnection(settings);
        ftpConnection.connect();
        String localFile = "resources/unittest/crcValueFileTest.csv";
        String filename = "shaTest.csv";
        this.ftpConnection.upload(localFile, filename);
        String[] shaStringArray = this.ftpConnection.getShaHashsumFromFtpFile(filename);
        if (shaStringArray != null) {
            for (String sha : shaStringArray) {
                System.out.println(String.format("%s: %s", filename, sha));
            }
        }
        this.ftpConnection.deleteFileFromFtp(filename);
        ftpConnection.disconnect();

    }

    @Test
    public void testUploadWithCrc() throws Exception
    {
        FtpSettings settings = new FtpSettings();
        settings.setFtpPassword(FTP_PASSWORD);
        settings.setFtpPort(FTP_PORT);
        settings.setFtpHost(FTP_HOSTNAME);
        settings.setFtpUsername(FTP_USERNAME);
        this.ftpConnection = new FTPConnection(settings);
        ftpConnection.connect();
        String filename = "resources/unittest/crcValueFileTest.csv";
        File file = new File(filename);
        System.out.println(file.getAbsolutePath());
        String remoteResultFileName = "crcValueFileTest.csv";

        boolean result = this.ftpConnection.uploadWithCrc(filename, remoteResultFileName);
        assertTrue(result);

        ftpConnection.disconnect();
    }

    @Test
    public void testGetCrcFromRemoteFtpFile() throws Exception
    {
        FtpSettings settings = new FtpSettings();
        settings.setFtpPassword(FTP_PASSWORD);
        settings.setFtpPort(FTP_PORT);
        settings.setFtpHost(FTP_HOSTNAME);
        settings.setFtpUsername(FTP_USERNAME);
        this.ftpConnection = new FTPConnection(settings);
        ftpConnection.connect();

        String remoteResultFileName = "crcValueFileTest.csv";
        String remoteResultCrcFileName = "crcValueFileTest.csv.crc";
        FTPFile ftpFile = this.ftpConnection.getFtpFileHashTable().get(remoteResultFileName);
        FTPFile ftpFileCrc = this.ftpConnection.getFtpFileHashTable().get(remoteResultCrcFileName);
        Long result = this.ftpConnection.getCrcFromRemoteFtpFile(ftpFile, ftpFileCrc);
        System.out.println(result);
        assertTrue(result == 952757702);
        ftpConnection.deleteFileFromFtp(remoteResultFileName);
        ftpConnection.deleteFileFromFtp(remoteResultCrcFileName);
        ftpConnection.disconnect();
    }
}

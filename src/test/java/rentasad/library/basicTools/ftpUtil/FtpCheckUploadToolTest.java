package rentasad.library.basicTools.ftpUtil;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rentasad.library.basicTools.ftpUtil.objects.FtpFileStatus;
import rentasad.library.basicTools.ftpUtil.objects.IFTPKonfigurationSheetParameter;

import rentasad.library.basicTools.ftpUtil.objects.FtpSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FtpCheckUploadToolTest implements IFTPKonfigurationSheetParameter {

    @TempDir
    Path tempDir;

    @Test
    public void testGetFtpFileStatusCollectionWithInvalidConnection() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(PARAMETER_NAME_FTP_HOST, "non-existent-host.local");
        configMap.put(PARAMETER_NAME_FTP_USERNAME, "user");
        configMap.put(PARAMETER_NAME_FTP_PASSWORT, "pass");
        configMap.put(PARAMETER_NAME_LOCAL_ARCHIV_DIR, tempDir.toString());
        configMap.put(PARAMETER_NAME_FTP_START_DIR, "");
        configMap.put(PARAMETER_NAME_CRC_CHECK, "false");
        configMap.put(PARAMETER_NAME_FTP_SERVER_TIME_DIFFERENCE_MINUTES, "0");
        configMap.put(PARAMETER_NAME_CHECK_FILES_COUNT, "0");

        // Laut FtpCheckUploadTool.java:166-170 wird eine FtpUtilException geworfen, wenn eine IOException (z.B. UnknownHostException) auftritt.
        assertThrows(rentasad.library.basicTools.ftpUtil.Exceptions.FtpUtilException.class, () -> {
            FtpCheckUploadTool.getFtpFileStatusCollection(configMap);
        });
    }

    @Test
    public void testGetFtpFileStatusCollectionWithCorrectLogin() throws Exception {
        // 1. Testdatei vorbereiten
        String testFileName = "testUpload_" + System.currentTimeMillis() + ".txt";
        Path localTestFile = tempDir.resolve(testFileName);
        Files.writeString(localTestFile, "Dies ist eine Testdatei fuer FtpCheckUploadToolTest.");

        // 2. Datei auf FTP hochladen, damit etwas gefunden wird
        FtpSettings settings = new FtpSettings("127.0.0.1", "user", "123");
        FTPConnection ftpConnection = new FTPConnection(settings);
        try {
            assertTrue(ftpConnection.connect(), "FTP Connect fehlgeschlagen");
            assertTrue(ftpConnection.upload(localTestFile.toString(), testFileName), "Upload fehlgeschlagen");
        } finally {
            ftpConnection.disconnect();
        }

        try {
            Map<String, String> configMap = new HashMap<>();
            configMap.put(PARAMETER_NAME_FTP_HOST, "127.0.0.1");
            configMap.put(PARAMETER_NAME_FTP_USERNAME, "user");
            configMap.put(PARAMETER_NAME_FTP_PASSWORT, "123");
            configMap.put(PARAMETER_NAME_LOCAL_ARCHIV_DIR, tempDir.toString());
            configMap.put(PARAMETER_NAME_FTP_START_DIR, "");
            configMap.put(PARAMETER_NAME_CRC_CHECK, "false");
            configMap.put(PARAMETER_NAME_FTP_SERVER_TIME_DIFFERENCE_MINUTES, "0");
            configMap.put(PARAMETER_NAME_CHECK_FILES_COUNT, "1");
            configMap.put(PARAMETER_NAME_CHECK_FILES_BASED_NAME + "1", testFileName);
            configMap.put(PARAMETER_NAME_CHECK_FILES_MAX_AGE_IN_MINUTES + "1", "60");

            Collection<rentasad.library.basicTools.ftpUtil.objects.FtpFileStatus> result = FtpCheckUploadTool.getFtpFileStatusCollection(configMap);
            assertNotNull(result);
            assertEquals(1, result.size());

            rentasad.library.basicTools.ftpUtil.objects.FtpFileStatus status = result.iterator().next();
            assertEquals(testFileName, status.getFileName());
            assertTrue(status.isExistFTPFile(), "Datei sollte auf FTP existieren");
        } finally {
            // Cleanup auf FTP
            if (ftpConnection.connect()) {
                ftpConnection.deleteFileFromFtp(testFileName);
                ftpConnection.disconnect();
            }
        }
    }

    @Test
    public void testGetFtpFileStatusCollectionWithWrongLogin() throws Exception {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(PARAMETER_NAME_FTP_HOST, "127.0.0.1");
        configMap.put(PARAMETER_NAME_FTP_USERNAME, "wrongUser");
        configMap.put(PARAMETER_NAME_FTP_PASSWORT, "wrongPass");
        configMap.put(PARAMETER_NAME_LOCAL_ARCHIV_DIR, tempDir.toString());
        configMap.put(PARAMETER_NAME_FTP_START_DIR, "");
        configMap.put(PARAMETER_NAME_CRC_CHECK, "false");
        configMap.put(PARAMETER_NAME_FTP_SERVER_TIME_DIFFERENCE_MINUTES, "0");
        configMap.put(PARAMETER_NAME_CHECK_FILES_COUNT, "0");

        // Wenn ein Server läuft aber Login falsch -> FtpUtilException (wegen FtpLoginException)
        assertThrows(rentasad.library.basicTools.ftpUtil.Exceptions.FtpUtilException.class, () -> {
            FtpCheckUploadTool.getFtpFileStatusCollection(configMap);
        });
    }

    @Test
    public void testGetFtpFileStatusCollectionMissingParameters() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(PARAMETER_NAME_FTP_HOST, "localhost");
        configMap.put(PARAMETER_NAME_FTP_USERNAME, "user");
        // Fehlende Parameter führen wahrscheinlich zu NullPointer oder ähnlichem beim Parsen
        
        assertThrows(Exception.class, () -> {
            FtpCheckUploadTool.getFtpFileStatusCollection(configMap);
        });
    }

    @Test
    public void testGetFtpFileStatusCollectionInvalidFileCount() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(PARAMETER_NAME_FTP_HOST, "localhost");
        configMap.put(PARAMETER_NAME_FTP_USERNAME, "user");
        configMap.put(PARAMETER_NAME_FTP_PASSWORT, "pass");
        configMap.put(PARAMETER_NAME_LOCAL_ARCHIV_DIR, tempDir.toString());
        configMap.put(PARAMETER_NAME_FTP_START_DIR, "");
        configMap.put(PARAMETER_NAME_CRC_CHECK, "false");
        configMap.put(PARAMETER_NAME_FTP_SERVER_TIME_DIFFERENCE_MINUTES, "invalid"); // Sollte NumberFormatException werfen

        assertThrows(NumberFormatException.class, () -> {
            FtpCheckUploadTool.getFtpFileStatusCollection(configMap);
        });
    }
}

package rentasad.library.tools.sftp_util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SFtpWrapperTest {

    private static final String USER = "user";
    private static final String PASS = "123";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String WRITE_DIR = "upload/";

    private SFtpWrapper sftpWrapper;
    private File tempFile;

	@BeforeEach
    public void setUp() throws Exception {
        sftpWrapper = new SFtpWrapper(USER, PASS, HOST, PORT);
		String baseRemoteDir = sftpWrapper.getRemoteActualDir();
        if (!baseRemoteDir.endsWith("/")) {
            baseRemoteDir += "/";
        }
        tempFile = File.createTempFile("sftp-test-", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Dies ist ein Test-Inhalt für SFTP.");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (sftpWrapper != null) {
            sftpWrapper.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testRemoteDir() throws Exception {
        String dir = sftpWrapper.getRemoteActualDir();
        assertNotEquals("", dir, "[DEBUG_LOG] Remote Dir: " + dir);
        List<SFtpWrapper.FileData> list = sftpWrapper.getFileDataList(".");
        for (SFtpWrapper.FileData data : list) {
            assertNotEquals("", data.getName(), "[DEBUG_LOG] File: " + data.getName() + " (Dir: " + data.isDirectory() + ")");
        }
    }

    @Test
    public void testUploadAndDownload() throws Exception {
        String remotePath = WRITE_DIR + "test-upload.txt";
        
        // Upload
        sftpWrapper.uploadFile(tempFile.getAbsolutePath(), remotePath);
        
        // Check File Data
        SFtpWrapper.FileData fileData = sftpWrapper.getFileData(remotePath);
        assertNotNull(fileData, "FileData should not be null");
        assertTrue(fileData.getName().contains("test-upload.txt"), "File name mismatch: " + fileData.getName());
        assertTrue(fileData.getSize() > 0, "File size should be > 0");
        
        // Download
        File downloadedFile = File.createTempFile("sftp-download-", ".txt");
        try {
            sftpWrapper.downloadFile(remotePath, downloadedFile.getAbsolutePath());
            assertTrue(downloadedFile.exists(), "Downloaded file does not exist");
            assertEquals(tempFile.length(), downloadedFile.length(), "File size mismatch");
            
            String content = new String(Files.readAllBytes(downloadedFile.toPath()));
            assertEquals("Dies ist ein Test-Inhalt für SFTP.", content, "Content mismatch");
        } finally {
            downloadedFile.delete();
        }
        
        // Remove
        sftpWrapper.removeFile(remotePath);
    }

    @Test
    public void testGetFileDataList() throws Exception {
        String remotePath1 = WRITE_DIR + "test-list-1.txt";
        String remotePath2 = WRITE_DIR + "test-list-2.txt";
        
        sftpWrapper.uploadFile(tempFile.getAbsolutePath(), remotePath1);
        sftpWrapper.uploadFile(tempFile.getAbsolutePath(), remotePath2);
        
        try {
            List<SFtpWrapper.FileData> list = sftpWrapper.getFileDataList(WRITE_DIR);
            boolean found1 = false;
            boolean found2 = false;
            for (SFtpWrapper.FileData data : list) {
                if (data.getName().equals("test-list-1.txt")) found1 = true;
                if (data.getName().equals("test-list-2.txt")) found2 = true;
            }
            assertTrue(found1, "Datei 1 nicht gefunden in " + WRITE_DIR);
            assertTrue(found2, "Datei 2 nicht gefunden in " + WRITE_DIR);
        } finally {
            sftpWrapper.removeFile(remotePath1);
            sftpWrapper.removeFile(remotePath2);
        }
    }

    @Test
    public void testUngzipRemote() throws Exception {
        File gzipFile = File.createTempFile("test-", ".gz");
        String remotePath = WRITE_DIR + "test.gz";
        String content = "GZIP Content";
        try {
            try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(gzipFile))) {
                gzos.write(content.getBytes());
            }
            sftpWrapper.uploadFile(gzipFile.getAbsolutePath(), remotePath);

            File destFile = File.createTempFile("unzipped-", ".txt");
            try {
                sftpWrapper.ungzipRemote(remotePath, destFile.getAbsolutePath());
                String unzippedContent = new String(Files.readAllBytes(destFile.toPath()));
                assertEquals(content, unzippedContent);
            } finally {
                destFile.delete();
            }
        } finally {
            gzipFile.delete();
            sftpWrapper.removeFile(remotePath);
        }
    }

    @Test
    public void testUnzipRemote() throws Exception {
        File zipFile = File.createTempFile("test-", ".zip");
        String remotePath = WRITE_DIR + "test.zip";
        String entryName = "test.txt";
        String content = "ZIP Content";
        try {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(content.getBytes());
                zos.closeEntry();
            }
            sftpWrapper.uploadFile(zipFile.getAbsolutePath(), remotePath);

            File destDir = Files.createTempDirectory("unzipped-dir-").toFile();
            try {
                long count = sftpWrapper.unzipRemote(remotePath, destDir.getAbsolutePath());
                assertEquals(1, count);
                File extractedFile = new File(destDir, entryName);
                assertTrue(extractedFile.exists());
                String extractedContent = new String(Files.readAllBytes(extractedFile.toPath()));
                assertEquals(content, extractedContent);
            } finally {
                // cleanup destDir
                File[] files = destDir.listFiles();
                if (files != null) for (File f : files) f.delete();
                destDir.delete();
            }
        } finally {
            zipFile.delete();
            sftpWrapper.removeFile(remotePath);
        }
    }
}

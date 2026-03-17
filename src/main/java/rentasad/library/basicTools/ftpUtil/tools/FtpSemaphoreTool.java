package rentasad.library.basicTools.ftpUtil.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.FTPConnection;

/**
 * Utility class for managing semaphore (lock) files on an FTP server.
 */
public class FtpSemaphoreTool {

    private final FTPConnection ftpConnection;

    public FtpSemaphoreTool(FTPConnection ftpConnection) {
        this.ftpConnection = ftpConnection;
    }

    /**
     * Creates a semaphore in the FTP root directory.
     *
     * @return true if successful.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean createSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String semaphoreFileName = tempDir + File.separator + "LOCK";
        try (PrintWriter writer = new PrintWriter(semaphoreFileName, "UTF-8")) {
            writer.println("This is a semphore File");
            writer.println("Until this File is visibility the files of this folder will be updated");
        }
        File semaphoreFile = new File(semaphoreFileName);
        if (semaphoreFile.exists()) {
            System.out.println("UPLOAD SEMAPHORE");
            return ftpConnection.upload(semaphoreFileName, semaphoreFile.getName());
        } else {
            System.err.println("Fehler beim Erzeugen der Semaphore");
            return false;
        }
    }

    /**
     * Removes a semaphore from the FTP root directory.
     *
     * @return true if successful.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean removeSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException {
        if (ftpConnection.existFile("LOCK")) {
            System.out.println("REMOVE SEMAPHORE");
            return ftpConnection.deleteFileFromFtp("LOCK");
        } else {
            System.err.println("Semaphore existiert nicht auf FTP-Server");
            return false;
        }
    }

    /**
     * Checks if a semaphore exists in the FTP root directory.
     *
     * @return true if it exists.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean existSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException {
        return ftpConnection.existFile("LOCK");
    }
}

package rentasad.library.basicTools.ftpUtil.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import lombok.extern.java.Log;
import org.apache.commons.net.ftp.FTPFile;
import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.FTPConnection;
import rentasad.library.basicTools.ftpUtil.objects.FtpSemaphore;

/**
 * Utility class for managing semaphore (lock) files on an FTP server.
 */
@Log
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
            lombokLog.info("UPLOAD SEMAPHORE");
            return ftpConnection.upload(semaphoreFileName, semaphoreFile.getName());
        } else {
            lombokLog.severe("Fehler beim Erzeugen der Semaphore");
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
            lombokLog.info("REMOVE SEMAPHORE");
            return ftpConnection.deleteFileFromFtp("LOCK");
        } else {
            lombokLog.severe("Semaphore existiert nicht auf FTP-Server");
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

    /**
     * Retrieves detailed information about the semaphore from the FTP server.
     *
     * @return An FtpSemaphore object containing metadata.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public FtpSemaphore getSemaphoreInfo() throws IOException, FtpLoginException {
        ftpConnection.connect();
        FTPFile[] files = ftpConnection.getFtpClient().listFiles("LOCK");
        if (files != null && files.length > 0) {
            FTPFile lockFile = files[0];
            return new FtpSemaphore(lockFile.getName(), lockFile.getTimestamp().getTime(), lockFile.getSize(), true);
        } else {
            return new FtpSemaphore("LOCK", null, 0, false);
        }
    }

    /**
     * Checks if a semaphore exists and is older than the specified duration.
     *
     * @param durationInMilliseconds The age threshold in milliseconds.
     * @return true if the semaphore is older than the threshold.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean isSemaphoreOlderThan(long durationInMilliseconds) throws IOException, FtpLoginException {
        FtpSemaphore semaphore = getSemaphoreInfo();
        if (semaphore.isExists() && semaphore.getCreationDate() != null) {
            long age = System.currentTimeMillis() - semaphore.getCreationDate().getTime();
            return age > durationInMilliseconds;
        }
        return false;
    }

    /**
     * Clears the semaphore if it is older than the specified duration.
     *
     * @param durationInMilliseconds The age threshold in milliseconds.
     * @return true if the semaphore was cleared or did not exist.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean clearSemaphoreIfOlderThan(long durationInMilliseconds) throws IOException, FtpLoginException {
        if (isSemaphoreOlderThan(durationInMilliseconds)) {
            lombokLog.info("Semaphore is older than threshold. Removing...");
            return removeSemaphoreInFtpRootDirectory();
        }
        return !existSemaphoreInFtpRootDirectory();
    }
}

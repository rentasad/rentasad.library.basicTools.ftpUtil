package rentasad.library.basicTools.ftpUtil.objects;

import java.util.Date;

/**
 * Data object representing a semaphore (lock) file on an FTP server.
 */
public class FtpSemaphore {
    private final String fileName;
    private final Date creationDate;
    private final long fileSize;
    private final boolean exists;

    public FtpSemaphore(String fileName, Date creationDate, long fileSize, boolean exists) {
        this.fileName = fileName;
        this.creationDate = creationDate;
        this.fileSize = fileSize;
        this.exists = exists;
    }

    public String getFileName() {
        return fileName;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isExists() {
        return exists;
    }

    @Override
    public String toString() {
        return "FtpSemaphore{" +
                "fileName='" + fileName + '\'' +
                ", creationDate=" + creationDate +
                ", fileSize=" + fileSize +
                ", exists=" + exists +
                '}';
    }
}

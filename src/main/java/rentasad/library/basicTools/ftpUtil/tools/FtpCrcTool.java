package rentasad.library.basicTools.ftpUtil.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.CRC32;

import org.apache.commons.net.ftp.FTPFile;

import rentasad.library.basicTools.NumberTools;
import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.FTPConnection;

/**
 * Utility class for CRC-related operations on files.
 */
public class FtpCrcTool {

    private final FTPConnection ftpConnection;

    public FtpCrcTool(FTPConnection ftpConnection) {
        this.ftpConnection = ftpConnection;
    }

    /**
     * Retrieves CRC from a remote FTP file.
     *
     * @param ftpFile    The remote file.
     * @param ftpFileCrc The CRC file.
     * @return The CRC value.
     * @throws IOException       If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public Long getCrcFromRemoteFtpFile(final FTPFile ftpFile, final FTPFile ftpFileCrc) throws IOException, FtpLoginException {
        File localTempCrcFile = File.createTempFile("crc_", ".crc");
        boolean downloadSuccess = ftpConnection.download(localTempCrcFile.getAbsolutePath(), ftpFileCrc);
        if (downloadSuccess) {
            try (BufferedReader br = new BufferedReader(new FileReader(localTempCrcFile))) {
                String line;
                if ((line = br.readLine()) != null) {
                    if (NumberTools.isNumericLong(line)) {
                        Long crcLong = Long.valueOf(line);
                        localTempCrcFile.deleteOnExit();
                        return crcLong;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Computes a CRC32 checksum for a local file.
     *
     * @param fileName The path to the local file.
     * @return The computed CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    public static long getCRCFromLocalFile(String fileName) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(new File(fileName))) {
            return getCRCFromInputStream(fileInputStream);
        }
    }

    /**
     * Retrieves the timestamp of a local file.
     *
     * @param fileName The path to the local file.
     * @return The timestamp of the file.
     */
    public static Date getTimeStampFromLocalFile(String fileName) {
        File file = new File(fileName);
        return new Date(file.lastModified());
    }

    /**
     * Computes a CRC32 checksum from an input stream.
     * Note: This method closes the input stream.
     *
     * @param inputStream The input stream to read from.
     * @return The computed CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    public static long getCRCFromInputStream(InputStream inputStream) throws IOException {
        CRC32 crc32 = new CRC32();
        try {
            int counter;
            while ((counter = inputStream.read()) != -1) {
                crc32.update(counter);
            }
        } finally {
            inputStream.close();
        }
        return crc32.getValue();
    }
}

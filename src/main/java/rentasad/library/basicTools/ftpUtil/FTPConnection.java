package rentasad.library.basicTools.ftpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.objects.FtpSettings;
import rentasad.library.basicTools.ftpUtil.tools.FtpCrcTool;
import rentasad.library.basicTools.ftpUtil.tools.FtpSemaphoreTool;

/**
 * The FTPConnection class provides functionality for managing connections to an FTP server
 * and performing various file operations such as uploading, downloading, and managing files.
 * It encapsulates the settings for the FTP connection and uses an FTP client to execute the operations.
 *
 * Class Fields:
 * - messageLog: A log to store messages pertaining to FTP operations.
 * - debug: A flag to enable/disable debug mode.
 * - ftpSettings: Contains the settings required for the FTP connection.
 * - ftpClient: The underlying client used for communicating with the FTP server.
 * - verbose: Specifies if detailed output should be generated during FTP operations.
 * - showMessages: Indicates whether to show messages from the FTP operations.
 *
 * Public Methods:
 * - FTPConnection(FtpSettings ftpSettings): Constructor accepting FTP settings.
 * - long getFileSize(String filePath): Retrieves the size of a file from the FTP server.
 * - Long getCrcFromRemoteFtpFile(FTPFile ftpFile, FTPFile ftpFileCrc): Retrieves CRC from a remote FTP file (delegated to FtpCrcTool).
 * - String[] getShaHashsumFromFtpFile(String ftpFilename): Retrieves the SHA hash of a file on the FTP server.
 * - boolean createSemaphoreInFtpRootDirectory(): Creates a semaphore in the FTP root directory (delegated to FtpSemaphoreTool).
 * - boolean removeSemaphoreInFtpRootDirectory(): Removes a semaphore from the FTP root directory (delegated to FtpSemaphoreTool).
 * - boolean existSemaphoreInFtpRootDirectory(): Checks if a semaphore exists in the FTP root directory (delegated to FtpSemaphoreTool).
 * - boolean connect(): Establishes an FTP connection.
 * - void disconnect(): Closes the FTP connection.
 * - boolean deleteFileFromFtp(String fileName): Deletes a file from the FTP server.
 * - List<FTPFile> getFTPFileList(): Fetches a list of files from the FTP server.
 * - Hashtable<String, FTPFile> getFtpFileHashTable(): Retrieves a hashtable of file names and FTP files.
 * - boolean download(String localResultFile, FTPFile ftpFile): Downloads a file from the FTP server.
 * - boolean download(String localResultFilePath, FTPFile ftpFile, String remoteFtpPath): Downloads a file from a specified remote FTP path.
 * - boolean uploadWithCrc(String localSourceFileName, String remoteResultFileName): Uploads a file with a CRC32 checksum.
 * - boolean upload(String localSourceFileName, String remoteResultFileName): Uploads a file to the FTP server.
 * - boolean existFile(String remotefile): Checks if a file exists in the current FTP directory.
 * - static long getCRCFromLocalFile(String fileName): Computes a CRC32 checksum for a local file.
 * - static Date getTimeStampFromLocalFile(String fileName): Retrieves the timestamp of a local file.
 * - static long getCRCFromInputStream(InputStream inputStream): Computes a CRC32 checksum from an input stream.
 * - boolean changeDir(String dirString): Changes the current directory on the FTP server.
 * - boolean isVerbose(): Retrieves the verbose status.
 * - void setVerbose(boolean verbose): Sets the verbose status.
 *
 * Exceptions:
 * - IOException: Thrown for I/O-related errors during FTP operations.
 * - FtpLoginException: Thrown for login or authentication failures.
 * - SocketException: Thrown for socket-related errors.
 */
public class FTPConnection
{
    private ArrayList<String> messageLog = new ArrayList<String>();
    private boolean debug = false;
    private FtpSettings ftpSettings;
    private FTPClient ftpClient = new FTPClient();
    private boolean verbose = true;
    private boolean showMessages = true;
    private final FtpSemaphoreTool semaphoreTool;
    private final FtpCrcTool crcTool;

    public FTPConnection(
                         FtpSettings ftpSettings)
    {
        super();
        this.ftpSettings = ftpSettings;
        this.ftpClient.setControlKeepAliveTimeout(20);
        this.semaphoreTool = new FtpSemaphoreTool(this);
        this.crcTool = new FtpCrcTool(this);
    }

    public long getFileSize(String filePath) throws Exception
    {
        long fileSize = 0;
        FTPFile[] files = ftpClient.listFiles(filePath);

        if (files.length == 1 && files[0].isFile())
        {
            fileSize = files[0].getSize();
        }
        // Log.i(TAG, "File size = " + fileSize);
        return fileSize;
    }

    public Long getCrcFromRemoteFtpFile(final FTPFile ftpFile, final FTPFile ftpFileCrc) throws IOException, FtpLoginException
    {
        return crcTool.getCrcFromRemoteFtpFile(ftpFile, ftpFileCrc);
    }

    /**
     * 
     * Description:
     * 
     * @param ftpFilename
     * @return
     * @throws IOException
     *             Creation: 27.11.2018 by mst
     */
    public String[] getShaHashsumFromFtpFile(String ftpFilename) throws IOException
    {
        if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("XSHA1", ftpFilename)))
        {
            String[] reply = ftpClient.getReplyStrings();
            return reply;
        } else
            return null;
    }

    public boolean createSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.createSemaphoreInFtpRootDirectory();
    }

    public boolean removeSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.removeSemaphoreInFtpRootDirectory();
    }

    public boolean existSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.existSemaphoreInFtpRootDirectory();
    }

    /**
     * Prueft ob die FTP-Verbindung bereits besteht und baut sie gegebenenfalls
     * auf
     *
     * @return
     * @throws SocketException
     * @throws IOException
     * @throws FtpLoginException
     */
    public boolean connect() throws SocketException, IOException, FtpLoginException
    {

        // int replyCode = -9999;

        if (this.ftpClient.isConnected() == false)
        {

            this.ftpClient.connect(this.ftpSettings.getFtpHost(), this.ftpSettings.getFtpPort());
            if (this.showMessages)
            {
            	ftpClient.enterLocalPassiveMode();
                System.out.println(ftpClient.getReplyString());
                this.messageLog.add(ftpClient.getReplyString());
            }
            @SuppressWarnings("unused")
            boolean resultOk = true;
            resultOk &= ftpClient.login(this.ftpSettings.getFtpUsername(), ftpSettings.getFtpPassword());
            if (ftpClient.getReplyCode() == FTPReply.NOT_LOGGED_IN)
            {
                this.messageLog.add(ftpClient.getReplyString());
                throw new FtpLoginException(ftpClient.getReplyString());

            }
            if (this.showMessages)
            {
                System.out.println(ftpClient.getReplyString());
                this.messageLog.add(ftpClient.getReplyString());
            }

            // replyCode = this.ftpClient.getReplyCode();
        }
        return this.ftpClient.isConnected();
    }

    public void disconnect() throws IOException
    {

        if (this.ftpClient.isConnected() == true)
        {
            this.ftpClient.disconnect();
        }
    }

    /**
     *
     * Description: Loescht Datei vom FTP-Server
     *
     * @param fileName
     * @return
     *         Creation: 12.08.2015 by mst
     * @throws IOException
     * @throws FtpLoginException
     */
    public boolean deleteFileFromFtp(String fileName) throws IOException, FtpLoginException
    {
        // if (ftpClient.isConnected() == false)
        return this.ftpClient.deleteFile(fileName);
    }

    /**
     *
     * @return
     * @throws SocketException
     * @throws IOException
     * @throws FtpLoginException
     */
    public List<FTPFile> getFTPFileList() throws SocketException, IOException, FtpLoginException
    {

        final List<FTPFile> ftpFileList = new ArrayList<FTPFile>();

        // String username = this.ftpSettings.getFtpUsername();
        // String password = this.ftpSettings.getFtpPassword();

        // if (this.ftpClient.isConnected() == false)
        connect();
        if (this.ftpClient.isConnected())
        {
            FTPFile[] ftpFileArray = ftpClient.listFiles();
            for (int i = 0; i < ftpFileArray.length; i++)
            {
                ftpFileList.add(ftpFileArray[i]);
            }

        } else
        {
            System.err.println("Es kam keine FTP-Verbindung zustande");
        }
        return ftpFileList;
    }

    /**
     * Gibt HashTable mit FileNames und FTPFile zurueck
     *
     * @return Hashtable<String, FTPFile>
     * @throws SocketException
     * @throws IOException
     * @throws FtpLoginException
     */
    public Hashtable<String, FTPFile> getFtpFileHashTable() throws SocketException, IOException, FtpLoginException
    {
        Hashtable<String, FTPFile> ftpFileHashTable = new Hashtable<String, FTPFile>();
        List<FTPFile> ftpFileList = getFTPFileList();
        for (@SuppressWarnings("rawtypes")
        Iterator iterator = ftpFileList.iterator(); iterator.hasNext();)
        {
            FTPFile ftpFile = (FTPFile) iterator.next();
            if (debug)
                System.out.println("Name Key: " + ftpFile.getName());
            ftpFileHashTable.put(ftpFile.getName(), ftpFile);
        }
        return ftpFileHashTable;
    }

    public boolean download(final String localResultFile, final FTPFile ftpFile) throws IOException, FtpLoginException
    {
        return download(localResultFile, ftpFile, "");
    }

    public boolean download(final String localResultFilePath, final FTPFile ftpFile, String remoteFtpPath) throws IOException, FtpLoginException
    {
        ftpClient.setBufferSize(1048576);
        FileOutputStream fileOutputStream = null;
        boolean resultOk = true;

        // try
        // {
        this.connect();
        if (!(remoteFtpPath.equals("")) && (remoteFtpPath != null))
        {
            this.changeDir(remoteFtpPath);
        }
        fileOutputStream = new FileOutputStream(localResultFilePath);
        long ftpSize = ftpFile.getSize();
        resultOk &= ftpClient.retrieveFile(ftpFile.getName(), fileOutputStream);
        if (resultOk)
        {
            long localSize = new File(localResultFilePath).length();
            resultOk = (localSize == ftpSize);
        }
        if (showMessages)
        {
            System.out.println(ftpClient.getReplyString());
            this.messageLog.add(ftpClient.getReplyString());
        }
        // resultOk &= ftpClient.logout();
        // if (showMessages)
        // {
        // System.out.println(ftpClient.getReplyString());
        // }
        // }
        // finally
        // {
        // try
        // {
        // if (fileOutputStream != null)
        // {
        // fileOutputStream.close();
        // }
        // } catch (IOException e)
        // {/* nothing to do */
        // }
        // ftpClient.disconnect();
        // }

        return resultOk;

    }

    /**
     * 
     * Description: Generate CRC32 from File to upload, upload CRC Info File with upload file
     * 
     * @param localSourceFileName
     * @param remoteResultFileName
     * @return
     *         Creation: 27.11.2018 by mst
     * @throws IOException
     * @throws FtpLoginException
     */
    public boolean uploadWithCrc(String localSourceFileName, String remoteResultFileName) throws IOException, FtpLoginException
    {
        Long crc = FtpCrcTool.getCRCFromLocalFile(localSourceFileName);
        String crcFilename = localSourceFileName + ".crc";
        try (PrintWriter writer = new PrintWriter(crcFilename, "UTF-8"))
        {
            writer.println(crc.toString());
        }
        upload(crcFilename, remoteResultFileName + ".crc");
        return upload(localSourceFileName, remoteResultFileName);
    }

    /**
     * FTP-Client-Upload.
     *
     * @return true falls ok
     * @throws FtpLoginException
     */
    public boolean upload(String localSourceFileName, String remoteResultFileName) throws IOException, FtpLoginException
    {
        FileInputStream fis = null;
        boolean resultOk = true;

        try
        {
            connect();
            // ftpClient.setBufferSize(0);
            ftpClient.setBufferSize(1048576);// Beschleunigt FTP-Transfer um hohen Faktor
            fis = new FileInputStream(localSourceFileName);
            if (this.verbose)
                System.out.println("uebertrage Datei " + localSourceFileName + "...");

            resultOk &= ftpClient.storeFile(remoteResultFileName, fis);
            showMessages = false;
            if (showMessages)
            {
                System.out.println(ftpClient.getReplyString());
                this.messageLog.add(ftpClient.getReplyString());
            }

            // resultOk &= ftpClient.logout();
            // if (showMessages)
            // {
            // System.out.println(ftpClient.getReplyString());
            // }
        } finally
        {
            try
            {
                if (fis != null)
                {
                    fis.close();
                    // System.out.println(ftpClient.getReplyString());
                    this.messageLog.add(ftpClient.getReplyString());
                }
            } catch (IOException e)
            {/* nothing to do */
            }
        }

        return resultOk;
    }

    /**
     * Prueft das aktuelle FTP-Verzeichnis darauf ob eine Datei darin existiert.
     * Dies wird durchgefuehrt, indem eine Liste der vorhandenen Dateien erstellt
     * wird.
     * Diese wird anschliessend in einer Schleife durchsucht.
     *
     * @param remotefile
     * @return
     * @throws IOException
     * @throws FtpLoginException
     */
    public boolean existFile(String remotefile) throws IOException, FtpLoginException
    {
        this.connect();
        boolean existFile = false;
        FTPFile[] ftpFiles = this.ftpClient.listFiles();
        for (int i = 0; i < ftpFiles.length; i++)
        {
            System.out.println(ftpFiles[i].getName());
            String name = ftpFiles[i].getName();
            if (name.equals(remotefile))
            {
                existFile = true;
            }
        }

        return existFile;
    }

    /**
     * Gibt eine Checksumme von einem
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static long getCRCFromLocalFile(String fileName) throws IOException
    {
        return FtpCrcTool.getCRCFromLocalFile(fileName);
    }

    public static Date getTimeStampFromLocalFile(String fileName)
    {
        return FtpCrcTool.getTimeStampFromLocalFile(fileName);
    }

    /**
     *
     * @param inputStream
     * @return
     * @throws IOException
     */

    public static long getCRCFromInputStream(InputStream inputStream) throws IOException
    {
        return FtpCrcTool.getCRCFromInputStream(inputStream);
    }

    public boolean changeDir(String dirString) throws SocketException, IOException, FtpLoginException
    {
        if (connect())
        {
            if (ftpClient.printWorkingDirectory().equalsIgnoreCase(dirString))
            {
                return true;
            } else
            {
                return this.ftpClient.changeWorkingDirectory(dirString);
            }
        } else
        {
            return false;
        }

    }

    /**
     * @return the verbose
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * @param verbose
     *            the verbose to set
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

}

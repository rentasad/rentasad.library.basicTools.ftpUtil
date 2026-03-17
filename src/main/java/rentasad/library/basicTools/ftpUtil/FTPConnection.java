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

import lombok.extern.java.Log;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import rentasad.library.basicTools.ftpUtil.Exceptions.FtpLoginException;
import rentasad.library.basicTools.ftpUtil.objects.FtpSemaphore;
import rentasad.library.basicTools.ftpUtil.objects.FtpSettings;
import rentasad.library.basicTools.ftpUtil.tools.FtpCrcTool;
import rentasad.library.basicTools.ftpUtil.tools.FtpSemaphoreTool;

/**
 * Die Klasse FTPConnection bietet Funktionalitäten zum Verwalten von Verbindungen zu einem FTP-Server
 * und zum Ausführen verschiedener Dateioperationen wie Hochladen, Herunterladen und Verwalten von Dateien.
 * Sie kapselt die Einstellungen für die FTP-Verbindung und verwendet einen {@link FTPClient} zum Ausführen der Operationen.
 * <p>
 * Diese Klasse wurde refaktoriert, um CRC- und Semaphore-Logik in spezialisierte Tool-Klassen auszulagern:
 * <ul>
 *   <li>{@link FtpCrcTool} - Für CRC32-Checksummen-Operationen</li>
 *   <li>{@link FtpSemaphoreTool} - Für die Verwaltung von Lock-Dateien (Semaphoren)</li>
 * </ul>
 * <p>
 * Die Klasse wahrt Abwärtskompatibilität, indem sie die ursprünglichen Methoden beibehält und intern an die Tools delegiert.
 */
@Log
public class FTPConnection
{
    private final ArrayList<String> messageLog = new ArrayList<String>();
    private boolean debug = false;
    private final FtpSettings ftpSettings;
    private final FTPClient ftpClient = new FTPClient();
    private boolean verbose = true;
    private boolean showMessages = true;
    private final FtpSemaphoreTool semaphoreTool;
    private final FtpCrcTool crcTool;

    /**
     * @param ftpSettings Die Einstellungen für die FTP-Verbindung (Host, Port, User, Passwort).
     */
    public FTPConnection(
                         FtpSettings ftpSettings)
    {
        super();
        this.ftpSettings = ftpSettings;
        this.ftpClient.setControlKeepAliveTimeout(20);
        this.semaphoreTool = new FtpSemaphoreTool(this);
        this.crcTool = new FtpCrcTool(this);
    }

    /**
     * @return Der verwendete {@link FTPClient}
     */
    public FTPClient getFtpClient()
    {
        return ftpClient;
    }

    /**
     * Ermittelt die Dateigröße einer Datei auf dem FTP-Server.
     *
     * @param filePath Pfad zur Datei auf dem FTP-Server.
     * @return Die Dateigröße in Byte.
     * @throws Exception Wenn ein Fehler beim Abrufen der Dateiliste auftritt.
     */
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

    /**
     * Berechnet die CRC-Checksumme einer entfernten Datei auf dem FTP-Server.
     * Nutzt intern das {@link FtpCrcTool}.
     *
     * @param ftpFile Die Zieldatei.
     * @param ftpFileCrc Die zugehörige .crc Datei.
     * @return Die CRC-Checksumme als Long oder null, wenn ein Fehler auftritt.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public Long getCrcFromRemoteFtpFile(final FTPFile ftpFile, final FTPFile ftpFileCrc) throws IOException, FtpLoginException
    {
        return crcTool.getCrcFromRemoteFtpFile(ftpFile, ftpFileCrc);
    }

    /**
     * Ermittelt den SHA1-Hash einer Datei auf dem FTP-Server unter Verwendung des XSHA1 Befehls.
     *
     * @param ftpFilename Name der Datei auf dem Server.
     * @return Array von Antwort-Strings des Servers, die den Hash enthalten.
     * @throws IOException Bei Kommunikationsfehlern mit dem FTP-Server.
     */
    public String[] getShaHashsumFromFtpFile(String ftpFilename) throws IOException
    {
        if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("XSHA1", ftpFilename)))
        {
			return ftpClient.getReplyStrings();
        } else
            return null;
    }

    /**
     * Erstellt eine Semaphore-Datei (LOCK) im Root-Verzeichnis des FTP-Servers.
     * Nutzt intern das {@link FtpSemaphoreTool}.
     *
     * @return true, wenn die Datei erfolgreich erstellt wurde.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean createSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.createSemaphoreInFtpRootDirectory();
    }

    /**
     * Entfernt die Semaphore-Datei (LOCK) aus dem Root-Verzeichnis des FTP-Servers.
     *
     * @return true, wenn das Löschen erfolgreich war.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean removeSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.removeSemaphoreInFtpRootDirectory();
    }

    /**
     * Prüft, ob eine Semaphore-Datei (LOCK) im Root-Verzeichnis existiert.
     *
     * @return true, wenn sie existiert.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean existSemaphoreInFtpRootDirectory() throws IOException, FtpLoginException
    {
        return semaphoreTool.existSemaphoreInFtpRootDirectory();
    }

    /**
     * Retrieves detailed information about the semaphore (lock) from the FTP server.
     *
     * @return FtpSemaphore object with metadata.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public FtpSemaphore getSemaphoreDetails() throws IOException, FtpLoginException
    {
        return semaphoreTool.getSemaphoreInfo();
    }

    /**
     * Clears the semaphore if it is older than the specified duration.
     *
     * @param durationInMilliseconds The age threshold.
     * @return true if successful.
     * @throws IOException If an I/O error occurs.
     * @throws FtpLoginException If an FTP login error occurs.
     */
    public boolean clearSemaphoreIfOlderThan(long durationInMilliseconds) throws IOException, FtpLoginException
    {
        return semaphoreTool.clearSemaphoreIfOlderThan(durationInMilliseconds);
    }

    /**
     * Establishes a connection to the FTP server using the provided FTP settings.
     * If the connection is successfully established, the method returns true.
     * The connection process involves establishing a socket connection to the specified
     * FTP host and port, logging in with the provided username and password, and optionally
     * entering passive mode if enabled. Any response messages are logged if configured.
     *
     * @return true if the FTP client successfully connects to the server, false otherwise
     * @throws SocketException if a socket-related error occurs during the connection process
     * @throws IOException if an I/O error occurs while communicating with the FTP server
     * @throws FtpLoginException if login to the FTP server fails
     */
    public boolean connect() throws SocketException, IOException, FtpLoginException
    {

        // int replyCode = -9999;

        if (!this.ftpClient.isConnected())
        {

            this.ftpClient.connect(this.ftpSettings.getFtpHost(), this.ftpSettings.getFtpPort());
            if (this.showMessages)
            {
            	ftpClient.enterLocalPassiveMode();
                lombokLog.info(ftpClient.getReplyString());
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
                lombokLog.info(ftpClient.getReplyString());
                this.messageLog.add(ftpClient.getReplyString());
            }

            // replyCode = this.ftpClient.getReplyCode();
        }
        return this.ftpClient.isConnected();
    }

    /**
     * Trennt die Verbindung zum FTP-Server, falls diese noch besteht.
     *
     * @throws IOException Bei Fehlern während des Verbindungsabbruchs.
     */
    public void disconnect() throws IOException
    {

        if (this.ftpClient.isConnected())
        {
            this.ftpClient.disconnect();
        }
    }

    /**
     * Löscht eine Datei vom FTP-Server.
     *
     * @param fileName Der Name der zu löschenden Datei.
     * @return true, wenn das Löschen erfolgreich war.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean deleteFileFromFtp(String fileName) throws IOException, FtpLoginException
    {
        // if (ftpClient.isConnected() == false)
        return this.ftpClient.deleteFile(fileName);
    }

    /**
     * Ruft eine Liste aller Dateien im aktuellen Verzeichnis des FTP-Servers ab.
     * Stellt sicher, dass eine Verbindung besteht.
     *
     * @return Liste von {@link FTPFile}-Objekten.
     * @throws SocketException Bei Netzwerkfehlern.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
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
            
            lombokLog.severe("Es kam keine FTP-Verbindung zustande");
        }
        return ftpFileList;
    }

    /**
     * Gibt eine Hashtable mit Dateinamen als Key und {@link FTPFile}-Objekten als Value zurück.
     *
     * @return Hashtable<String, FTPFile>
     * @throws SocketException Bei Netzwerkfehlern.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
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
                lombokLog.info("Name Key: " + ftpFile.getName());
            ftpFileHashTable.put(ftpFile.getName(), ftpFile);
        }
        return ftpFileHashTable;
    }

    /**
     * Lädt eine Datei vom FTP-Server herunter in ein lokales Zielverzeichnis/Datei.
     *
     * @param localResultFile Pfad zur lokalen Zieldatei.
     * @param ftpFile Das zu ladende FTPFile-Objekt.
     * @return true, wenn der Download erfolgreich war.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean download(final String localResultFile, final FTPFile ftpFile) throws IOException, FtpLoginException
    {
        return download(localResultFile, ftpFile, "");
    }

    /**
     * Lädt eine Datei von einem spezifischen Pfad auf dem FTP-Server herunter.
     *
     * @param localResultFilePath Lokaler Pfad, an dem die Datei gespeichert werden soll.
     * @param ftpFile Das zu ladende FTPFile-Objekt.
     * @param remoteFtpPath Der Verzeichnispfad auf dem FTP-Server.
     * @return true, wenn der Download erfolgreich war.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean download(final String localResultFilePath, final FTPFile ftpFile, String remoteFtpPath) throws IOException, FtpLoginException
    {
        ftpClient.setBufferSize(1048576);
        FileOutputStream fileOutputStream = null;
        boolean resultOk = true;

        // try
        // {
        this.connect();
        if (!(remoteFtpPath.isEmpty()) && (remoteFtpPath != null))
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
            lombokLog.info(ftpClient.getReplyString());
            this.messageLog.add(ftpClient.getReplyString());
        }
        // resultOk &= ftpClient.logout();
        // if (showMessages)
        // {
        // lombokLog.info(ftpClient.getReplyString());
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
     * Lädt eine lokale Datei hoch und erstellt zusätzlich eine .crc Datei mit der CRC32-Checksumme.
     * Nutzt intern das {@link FtpCrcTool}.
     *
     * @param localSourceFileName Lokaler Dateiname.
     * @param remoteResultFileName Dateiname auf dem FTP-Server.
     * @return true, wenn beide Dateien erfolgreich hochgeladen wurden.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
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
     * Lädt eine Datei auf den FTP-Server hoch.
     *
     * @param localSourceFileName Lokaler Pfad zur Quelldatei.
     * @param remoteResultFileName Zielpfad/Name auf dem FTP-Server.
     * @return true, wenn der Upload erfolgreich war.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
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
                lombokLog.info("uebertrage Datei " + localSourceFileName + "...");

            resultOk &= ftpClient.storeFile(remoteResultFileName, fis);
            showMessages = false;
            if (showMessages)
            {
                lombokLog.info(ftpClient.getReplyString());
                this.messageLog.add(ftpClient.getReplyString());
            }

            // resultOk &= ftpClient.logout();
            // if (showMessages)
            // {
            // lombokLog.info(ftpClient.getReplyString());
            // }
        } finally
        {
            try
            {
                if (fis != null)
                {
                    fis.close();
                    // lombokLog.info(ftpClient.getReplyString());
                    this.messageLog.add(ftpClient.getReplyString());
                }
            } catch (IOException e)
            {/* nothing to do */
            }
        }

        return resultOk;
    }

    /**
     * Prüft das aktuelle FTP-Verzeichnis darauf, ob eine Datei darin existiert.
     * Dies wird durchgeführt, indem eine Liste der vorhandenen Dateien erstellt und durchsucht wird.
     *
     * @param remotefile Name der zu prüfenden Datei.
     * @return true, wenn die Datei existiert.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
    public boolean existFile(String remotefile) throws IOException, FtpLoginException
    {
        this.connect();
        boolean existFile = false;
        FTPFile[] ftpFiles = this.ftpClient.listFiles();
        for (int i = 0; i < ftpFiles.length; i++)
        {
            lombokLog.info(ftpFiles[i].getName());
            String name = ftpFiles[i].getName();
            if (name.equals(remotefile))
            {
                existFile = true;
            }
        }

        return existFile;
    }

    /**
     * Berechnet die CRC-Checksumme einer lokalen Datei.
     *
     * @param fileName Pfad zur lokalen Datei.
     * @return CRC-Checksumme als long.
     * @throws IOException Bei I/O-Fehlern.
     */
    public static long getCRCFromLocalFile(String fileName) throws IOException
    {
        return FtpCrcTool.getCRCFromLocalFile(fileName);
    }

    /**
     * Liefert den Zeitstempel einer lokalen Datei zurück.
     *
     * @param fileName Name der lokalen Datei.
     * @return Das Datum der letzten Änderung.
     */
    public static Date getTimeStampFromLocalFile(String fileName)
    {
        return FtpCrcTool.getTimeStampFromLocalFile(fileName);
    }

    /**
     * Berechnet die CRC-Checksumme eines InputStreams.
     *
     * @param inputStream Der zu lesende Stream.
     * @return CRC-Checksumme als long.
     * @throws IOException Bei I/O-Fehlern.
     */
    public static long getCRCFromInputStream(InputStream inputStream) throws IOException
    {
        return FtpCrcTool.getCRCFromInputStream(inputStream);
    }

    /**
     * Wechselt das aktuelle Arbeitsverzeichnis auf dem FTP-Server.
     *
     * @param dirString Das Zielverzeichnis.
     * @return true, wenn der Wechsel erfolgreich war oder man sich bereits im Verzeichnis befindet.
     * @throws SocketException Bei Netzwerkfehlern.
     * @throws IOException Bei I/O-Fehlern.
     * @throws FtpLoginException Bei Fehlern während des Logins.
     */
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

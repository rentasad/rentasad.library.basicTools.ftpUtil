package rentasad.library.tools.sftp_util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
/**
 * SFtpWrapper is a wrapper class for performing SFTP operations such as connecting to an SFTP server,
 * uploading and downloading files, and extracting files from ZIP and GZ archives.
 */
public class SFtpWrapper implements AutoCloseable
{
    private Session     session;
    private ChannelSftp channel;
    /**
     * 
     * @param benutzername
     * @param passwort
     * @param host
     * @param port
     * @throws IOException
     */
    public SFtpWrapper( String benutzername, String passwort, String host, int port ) throws IOException
    {
       try {
          session = (new JSch()).getSession( benutzername, host, port );
          session.setPassword( passwort );
          
          session.setConfig( "StrictHostKeyChecking", "no" );
          session.setConfig("max_input_buffer_size", "increased_size");
          session.connect();
       } catch( JSchException ex ) {
          throw new IOException( "Fehler beim SFTP-Connect mit '" + benutzername + "' an '" + host + "': ", ex );
       }
       try {
          channel = (ChannelSftp) session.openChannel( "sftp" );
          if( channel == null ) {
             close();
             throw new IOException( "Fehler beim Oeffnen des SFTP-Channel zur SFTP-Session mit '" + session.getUserName() + "' an '" + session.getHost() + "'. " );
          }
          channel.connect();
          
       } catch( JSchException ex ) {
          close();
          throw new IOException( "Fehler beim Oeffnen des SFTP-Channel zur SFTP-Session mit '" + session.getUserName() + "' an '" + session.getHost() + "': ", ex );
       }
    }


    @Override
    public void close()
    {
       try {
          if( channel != null ) {
             channel.disconnect();
             channel = null;
          }
       } finally {
          if( session != null ) {
             session.disconnect();
             session = null;
          }
       }
    }

    public String getLocalActualDir()
    {
       return channel.lpwd();
    }

    public String getRemoteActualDir() throws IOException
    {
       try {
          return channel.pwd();
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Retrieves the file data of a specified remote file.
     *
     * @param remoteFilePath The path of the remote file.
     * @return The file data of the remote file, or null if the file does not exist or if there was an error retrieving the file data.
     * @throws IOException If there was an I/O error while retrieving the file data.
     */
    public FileData getFileData( String remoteFilePath ) throws IOException
    {
       try {
          @SuppressWarnings("unchecked")
          List<ChannelSftp.LsEntry> lsEntryLst = channel.ls( remoteFilePath );
          if( lsEntryLst == null || lsEntryLst.size() != 1 ) {
             return null;
          }
          LsEntry lsEntry = lsEntryLst.get( 0 );
          FileData fd = new FileData();
          int i = remoteFilePath.lastIndexOf( '/' );
          fd.parentPath  = ( i < 0 ) ? "" : remoteFilePath.substring( 0, i );
          fd.isDirectory =  lsEntry.getAttrs().isDir();
          fd.isFile      = !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink();
          fd.name        = lsEntry.getFilename();
          fd.size        = lsEntry.getAttrs().getSize();
          fd.timestamp   = Calendar.getInstance();
          fd.timestamp.setTimeInMillis( 1000L * lsEntry.getAttrs().getMTime() );
          return fd;
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Retrieves a list of FileData objects representing the files in the specified remote directory.
     *
     * @param remoteDir The remote directory path.
     * @return A list of FileData objects representing the files in the remote directory.
     * @throws IOException If there was an I/O error retrieving the file data.
     */
    public List<FileData> getFileDataList( String remoteDir ) throws IOException
    {
       try {
          List<FileData> fileDataLst = new ArrayList<FileData>();
          @SuppressWarnings("unchecked")
          List<ChannelSftp.LsEntry> lsEntryLst = channel.ls( remoteDir );
          for( LsEntry lsEntry : lsEntryLst ) {
             FileData fd = new FileData();
             fd.parentPath  = remoteDir;
             fd.isDirectory =  lsEntry.getAttrs().isDir();
             fd.isFile      = !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink();
             fd.name        = lsEntry.getFilename();
             fd.size        = lsEntry.getAttrs().getSize();
             fd.timestamp   = Calendar.getInstance();
             fd.timestamp.setTimeInMillis( 1000L * lsEntry.getAttrs().getMTime() );
             fileDataLst.add( fd );
          }
          return fileDataLst;
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Creates a remote file by uploading data from an InputStream.
     *
     * @param is                 The InputStream containing the data to upload.
     * @param remoteDstFilePath  The destination path of the remote file.
     * @throws IOException       If there was an I/O error while uploading the file data.
     */
    public void createRemoteFile( InputStream is, String remoteDstFilePath ) throws IOException
    {
       try {
          channel.put( is, remoteDstFilePath );
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Uploads a file from the local source path to the remote destination path.
     *
     * @param localSrcFilePath The path of the local file to upload.
     * @param remoteDstFilePath The destination path of the remote file.
     * @throws IOException If there was an I/O error while uploading the file.
     */
    public void uploadFile( String localSrcFilePath, String remoteDstFilePath ) throws IOException
    {
       try {
          channel.put( localSrcFilePath, remoteDstFilePath );
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Downloads a file from the remote source path to the local destination path.
     *
     * @param remoteSrcFilePath The path of the remote file.
     * @param localDstFilePath The destination path of the local file.
     * @throws IOException If there was an I/O error while downloading the file.
     */
    public void downloadFile( String remoteSrcFilePath, String localDstFilePath ) throws IOException
    {
       try {
          channel.get( remoteSrcFilePath, localDstFilePath );
          
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }
    
    /**
     * Removes a file in the remote server.
     *
     * @param remoteFilePath The path of the file to be removed.
     * @throws IOException If there was an I/O error while removing the file.
     */
    public void removeFile(String remoteFilePath) throws IOException
    {
            try
            {
                channel.rm( remoteFilePath );
            } catch (SftpException e)
            {
                throw new IOException(e);
            }
            
    }

    /**
     * Ungzips a remote source zip file and saves the result to a local destination file.
     *
     * @param remoteSourceZipFile The path of the remote source zip file.
     * @param localDestFilePath The path of the local destination file.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public void ungzipRemote( String remoteSourceZipFile, String localDestFilePath ) throws IOException
    {
       try( InputStream instreamZipped = channel.get( remoteSourceZipFile ) ) {
          ungzipStream( instreamZipped, localDestFilePath );
       } catch( Exception ex ) {
          throw new IOException( "Fehler beim Unzip von " + remoteSourceZipFile + ",", ex );
       }
    }

    /**
     * Unzips a local source zip file and saves the result to a local destination file.
     *
     * @param localSourceZipFile The path of the local source zip file.
     * @param localDestFilePath The path of the local destination file.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public static void ungzipLocal( String localSourceZipFile, String localDestFilePath ) throws IOException
    {
       try( InputStream instreamZipped = new FileInputStream( localSourceZipFile ) ) {
          ungzipStream( instreamZipped, localDestFilePath );
       } catch( Exception ex ) {
          throw new IOException( "Fehler beim Unzip von " + localSourceZipFile + ",", ex );
       }
    }

    /**
     * Ungzips the input stream and writes the result to the local file.
     *
     * @param instreamZipped The input stream of the zipped data.
     * @param localDestFilePath The path of the local destination file.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public static void ungzipStream( InputStream instreamZipped, String localDestFilePath ) throws IOException
    {
       try( GZIPInputStream zin = new GZIPInputStream( new BufferedInputStream( instreamZipped ) ) ) {
          try( BufferedOutputStream os = new BufferedOutputStream( new FileOutputStream( localDestFilePath ) ) ) {
             int size;
             byte[] buffer = new byte[64 * 1024];
             while( (size = zin.read( buffer, 0, buffer.length )) > 0 ) {
                os.write( buffer, 0, size );
             }
          }
       }
    }

    /**
     * Unzips a remote source zip file and saves the result to a local destination directory.
     *
     * @param remoteSourceZipFile The path of the remote source zip file.
     * @param localDestDir The path of the local destination directory.
     * @return The number of entries unzipped.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public long unzipRemote( String remoteSourceZipFile, String localDestDir ) throws IOException
    {
       try( InputStream instreamZipped = channel.get( remoteSourceZipFile ) ) {
          return unzipStream( instreamZipped, localDestDir );
       } catch( Exception ex ) {
          throw new IOException( "Fehler beim Unzip von " + remoteSourceZipFile + ",", ex );
       }
    }

    /**
     * Unzips a local source zip file and saves the result to a local destination directory.
     *
     * @param localSourceZipFile The path of the local source zip file.
     * @param localDestDir The path of the local destination directory.
     * @return The number of entries unzipped.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public static long unzipLocal( String localSourceZipFile, String localDestDir ) throws IOException
    {
       try( InputStream instreamZipped = new FileInputStream( localSourceZipFile ) ) {
          return unzipStream( instreamZipped, localDestDir );
       } catch( Exception ex ) {
          throw new IOException( "Fehler beim Unzip von " + localSourceZipFile + ",", ex );
       }
    }

    /**
     * Unzips the input stream and saves the result to the local destination directory.
     *
     * @param instreamZipped The input stream of the zipped data.
     * @param localDestDir The path of the local destination directory.
     * @return The number of entries unzipped.
     * @throws IOException If there is an I/O error during unzipping.
     */
    public static long unzipStream( InputStream instreamZipped, String localDestDir ) throws IOException
    {
       long   anzahlEntries = 0;
       String remoteResultFilename = null;
       String destDir = ( localDestDir == null ) ? "" : localDestDir.trim();
       destDir = ( destDir.endsWith( "/" ) || destDir.endsWith( "\\" ) ) ? destDir : (destDir + File.separator);
       try( ZipInputStream zin = new ZipInputStream( new BufferedInputStream( instreamZipped ) ) ) {
          ZipEntry zipEntry;
          while( (zipEntry = zin.getNextEntry()) != null ) {
             remoteResultFilename = zipEntry.getName();
             if( remoteResultFilename != null && remoteResultFilename.startsWith( "/" ) && remoteResultFilename.length() > 1 ) {
                remoteResultFilename = remoteResultFilename.substring( 1 );
             }
             try( BufferedOutputStream os = new BufferedOutputStream( new FileOutputStream( destDir + remoteResultFilename ) ) ) {
                int size;
                byte[] buffer = new byte[64 * 1024];
                while( (size = zin.read( buffer, 0, buffer.length )) > 0 ) {
                   os.write( buffer, 0, size );
                }
             }
             zin.closeEntry();
             anzahlEntries++;
          }
       } catch( Exception ex ) {
          throw new IOException( "Fehler beim Unzip, letzter Zip-Entry " + remoteResultFilename + ",", ex );
       }
       return anzahlEntries;
    }

    /**
     * Downloads and unzips files from a remote source directory to a local destination directory.
     *
     * @param remoteSrcDir The path of the remote source directory.
     * @param localDstDir The path of the local destination directory.
     * @param filenameMustContain The string that the filenames must contain to be processed.
     * @param maxAlterInTagen The maximum age of files (in days) to be processed.
     * @throws IOException If there was an I/O error while downloading or unzipping the files.
     */
    public void downloadAndUnzip( String remoteSrcDir, String localDstDir, String filenameMustContain, int maxAlterInTagen ) throws IOException
    {
       Calendar cal = new GregorianCalendar();
       cal.add( Calendar.DAY_OF_MONTH, -1 * maxAlterInTagen );
       (new File( localDstDir )).mkdirs();
       List<FileData> fds = getFileDataList( remoteSrcDir );
       for( FileData fd : fds ) {
          if( fd.isFile && fd.name.contains( filenameMustContain ) && fd.timestamp.after( cal ) ) {
             String remoteSrcFilePath = fd.parentPath + "/" + fd.name;
             String localDstFilePath  = localDstDir + File.separator + fd.name;
             if( fd.name.toLowerCase().endsWith( ".zip" ) ) {
                unzipRemote( remoteSrcFilePath, localDstDir );
             } else if( fd.name.toLowerCase().endsWith( ".gz" ) ) {
                ungzipRemote( remoteSrcFilePath, localDstFilePath.substring( 0, localDstFilePath.length() - 3 ) );
             } else {
                downloadFile( remoteSrcFilePath, localDstFilePath );
             }
          }
       }
    }

    /**
     * Downloads and unzips files from a remote source directory to a local destination directory.
     *
     * @param remoteSrcDir       The path of the remote source directory.
     * @param localDstDir        The path of the local destination directory.
     * @param filenameMustContain    The string that the filenames must contain to be processed.
     * @param maxAlterInTagen    The maximum age of files (in days) to be processed.
     * @param benutzername       The username for the SFTP connection.
     * @param passwort           The password for the SFTP connection.
     * @param host               The hostname for the SFTP connection.
     * @param port               The port number for the SFTP connection.
     * @throws IOException      If there was an I/O error while downloading or unzipping the files.
     */
    public static void downloadAndUnzip( String remoteSrcDir, String localDstDir, String filenameMustContain, String maxAlterInTagen,
                                         String benutzername, String passwort, String host, String port ) throws IOException
    {
       try( SFtpWrapper sftpWrapper = new SFtpWrapper( benutzername, passwort, host, Integer.parseInt( port ) ) ) {
          SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
          System.out.println( "Remote in " + remoteSrcDir + ":" );
          List<FileData> fds = sftpWrapper.getFileDataList( remoteSrcDir );
          for( FileData fd : fds ) {
             if( fd.isFile ) {
                System.out.println( df.format( Long.valueOf( fd.timestamp.getTimeInMillis() ) ) + ", " + fd.size + " Bytes, " + fd.name );
             }
          }
          sftpWrapper.downloadAndUnzip( remoteSrcDir, localDstDir, filenameMustContain, Integer.parseInt( maxAlterInTagen ) );
          System.out.println( "Lokal in " + localDstDir + ":" );
          File[] fls = (new File( localDstDir )).listFiles();
          for( File fl : fls ) {
             System.out.println( df.format( Long.valueOf( fl.lastModified() ) ) + ", " + fl.length() + " Bytes, " + fl.getName() );
          }
       }
    }

    /**
     * Represents the data of a file in a file system.
     */
    public static class FileData
    {
       public boolean  isFile;
       public boolean  isDirectory;
       public String   parentPath;
       public String   name;
       public long     size;
       public Calendar timestamp;
    }

}

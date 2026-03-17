package rentasad.library.tools.sftp_util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Wrapper-Klasse für die Handhabung von SFTP-Dateiübertragungen und Verzeichnisoperationen.
 * Bietet Methoden zum Verbinden mit einem SFTP-Server, zum Übertragen von Dateien,
 * zum Abrufen von Dateidaten sowie zum Komprimieren oder Dekomprimieren von Dateien.
 * Implementiert {@link AutoCloseable}, um eine ordnungsgemäße Ressourcenbereinigung zu gewährleisten.
 */
@Log
public class SFtpWrapper implements AutoCloseable
{
    private Session     session;
    private ChannelSftp channel;

    /**
     * Innere Klasse zur Speicherung von Dateimetadaten.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FileData
    {
        /** Gibt an, ob es sich um eine Datei handelt. */
        boolean  isFile;
        /** Gibt an, ob es sich um ein Verzeichnis handelt. */
        boolean  isDirectory;
        /** Der Pfad des übergeordneten Verzeichnisses auf dem Server. */
        String   parentPath;
        /** Der Name der Datei oder des Verzeichnisses. */
        String   name;
        /** Die Größe der Datei in Bytes. */
        long     size;
        /** Der Zeitstempel der letzten Änderung. */
        Instant timestamp;
    }

    /**
     * Erstellt eine neue SFtpWrapper-Instanz und initialisiert eine SFTP-Verbindung.
     * Baut eine Sitzung und einen Kanal für die Kommunikation mit dem angegebenen SFTP-Host auf.
     *
     * @param benutzername Der Benutzername für die Authentifizierung am SFTP-Server.
     * @param passwort Das Passwort für die Authentifizierung am SFTP-Server.
     * @param host Der Hostname oder die IP-Adresse des SFTP-Servers.
     * @param port Die Portnummer des SFTP-Servers.
     * @throws IOException Wenn ein Fehler während des Verbindungsaufbaus oder der Kanalerstellung auftritt.
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

    /**
     * Schließt die SFTP-Verbindung und gibt die mit der Sitzung und dem Kanal verbundenen Ressourcen frei.
     *
     * Die Methode stellt sicher, dass sowohl der SFTP-Kanal als auch die Sitzung ordnungsgemäß getrennt
     * und auf null gesetzt werden, um Ressourcenlecks zu vermeiden. Falls der Kanal existiert, wird dieser
     * zuerst getrennt, gefolgt von der Sitzung.
     *
     * Diese Methode ist Teil der {@code SFtpWrapper}-Klasse und überschreibt die {@code close}-Methode
     * des {@code AutoCloseable}-Interfaces.
     */
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

    /**
     * Ruft das aktuelle lokale Arbeitsverzeichnis des SFTP-Kanals ab.
     *
     * Diese Methode verwendet die {@code lpwd()}-Methode des zugrunde liegenden SFTP-Kanals,
     * um den Pfad zum aktuell aktiven lokalen Verzeichnis zu bestimmen.
     *
     * @return Der Pfad des aktuellen lokalen Arbeitsverzeichnisses als String.
     */
    public String getLocalActualDir()
    {
       return channel.lpwd();
    }

    /**
     * Ruft das aktuelle entfernte Arbeitsverzeichnis des SFTP-Kanals ab.
     *
     * Diese Methode verwendet die {@code pwd()}-Methode des zugrunde liegenden SFTP-Kanals,
     * um den Pfad des aktuell aktiven entfernten Verzeichnisses zu bestimmen.
     *
     * @return Der Pfad des aktuellen entfernten Arbeitsverzeichnisses als String.
     * @throws IOException Wenn ein Fehler beim Abrufen des entfernten Verzeichnisses auftritt.
     */
    public String getRemoteActualDir() throws IOException
    {
       try {
          return channel.pwd();
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Ruft Metadaten über eine durch ihren Pfad spezifizierte entfernte Datei ab.
     *
     * Diese Methode greift über den aktuellen SFTP-Kanal auf das entfernte Dateisystem zu
     * und extrahiert Informationen über die Datei, wie Name, Größe, Verzeichnisstatus,
     * übergeordneter Pfad und Zeitstempel der letzten Änderung.
     *
     * @param remoteFilePath Der vollständige Pfad zur Datei auf dem entfernten SFTP-Server.
     * @return Ein {@code FileData}-Objekt mit den Metadaten der Datei, oder {@code null}, wenn
     * die Datei nicht existiert oder mehrere Einträge auf den angegebenen Pfad passen.
     * @throws IOException Wenn ein Fehler beim Abrufen der Dateimetadaten auftritt.
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
          int i = remoteFilePath.lastIndexOf( '/' );
          String parentPath  = ( i < 0 ) ? "" : remoteFilePath.substring( 0, i );
          return FileData.builder()
                         .parentPath( parentPath )
                         .isDirectory( lsEntry.getAttrs().isDir() )
                         .isFile( !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink() )
                         .name( lsEntry.getFilename() )
                         .size( lsEntry.getAttrs().getSize() )
                         .timestamp( Instant.ofEpochSecond( lsEntry.getAttrs().getMTime() ) )
                         .build();
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Ruft eine Liste von Dateimetadaten aus einem angegebenen entfernten Verzeichnis auf einem SFTP-Server ab.
     *
     * Diese Methode greift auf das entfernte Verzeichnis zu und generiert eine Liste von {@code FileData}-Objekten,
     * die jeweils Metadaten über eine Datei oder ein Unterverzeichnis enthalten.
     * Symbolische Links werden ignoriert.
     *
     * @param remoteDir Der Pfad zum entfernten Verzeichnis auf dem SFTP-Server.
     * @return Eine Liste von {@code FileData}-Objekten, die die Dateien und Verzeichnisse im angegebenen Verzeichnis darstellen.
     * @throws IOException Wenn ein Fehler beim Abrufen der Dateiliste auftritt.
     */
    public List<FileData> getFileDataList( String remoteDir ) throws IOException
    {
       try {
          @SuppressWarnings("unchecked")
          List<LsEntry> lsEntryLst = channel.ls( remoteDir );
          return lsEntryLst.stream()
                           .map( lsEntry -> FileData.builder()
                                                    .parentPath( remoteDir )
                                                    .isDirectory( lsEntry.getAttrs().isDir() )
                                                    .isFile( !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink() )
                                                    .name( lsEntry.getFilename() )
                                                    .size( lsEntry.getAttrs().getSize() )
                                                    .timestamp( Instant.ofEpochSecond( lsEntry.getAttrs().getMTime() ) )
                                                    .build() )
                           .collect( Collectors.toList() );
       } catch( SftpException ex ) {
          throw new IOException( ex );
       }
    }

    /**
     * Erstellt eine Datei auf einem entfernten SFTP-Server und schreibt den Inhalt des bereitgestellten InputStreams in die Datei.
     *
     * @param is Der InputStream, der die in die entfernte Datei zu schreibenden Daten enthält.
     * @param remoteDstFilePath Der vollständige Pfad zur Zieldatei auf dem entfernten SFTP-Server.
     * @throws IOException Wenn ein Fehler während der Dateierstellung oder Übertragung auftritt.
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
     * Lädt eine Datei vom lokalen Dateisystem auf den entfernten SFTP-Server hoch.
     *
     * @param localSrcFilePath Der vollständige Pfad zur Quelldatei auf dem lokalen Dateisystem.
     * @param remoteDstFilePath Der vollständige Pfad zur Zieldatei auf dem entfernten SFTP-Server.
     * @throws IOException Wenn ein Fehler während des Upload-Vorgangs auftritt.
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
     * Lädt eine Datei von einem entfernten SFTP-Server auf das lokale Dateisystem herunter.
     *
     * @param remoteSrcFilePath Der vollständige Pfad zur Quelldatei auf dem entfernten SFTP-Server.
     * @param localDstFilePath Der vollständige Pfad zur Zieldatei auf dem lokalen Dateisystem.
     * @throws IOException Wenn ein Fehler während des Download-Vorgangs auftritt.
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
     * Entfernt eine Datei vom entfernten SFTP-Server.
     *
     * @param remoteFilePath Der vollständige Pfad der zu entfernenden Datei auf dem entfernten SFTP-Server.
     * @throws IOException Wenn ein Fehler während des Löschvorgangs auftritt.
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
     * Dekomprimiert eine GZIP-komprimierte Datei von einer entfernten Quelle und schreibt den dekomprimierten Inhalt
     * in eine angegebene lokale Zieldatei.
     *
     * @param remoteSourceZipFile Der Pfad der GZIP-komprimierten Datei auf der entfernten Quelle.
     * @param localDestFilePath Der lokale Dateipfad, unter dem der dekomprimierte Inhalt gespeichert wird.
     * @throws IOException Wenn ein Fehler beim Zugriff oder beim Dekomprimieren der Datei auftritt.
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
     * Dekomprimiert eine lokale GZIP-komprimierte Datei und speichert den dekomprimierten Inhalt
     * im angegebenen Zieldateipfad.
     *
     * @param localSourceZipFile Der Pfad zur lokalen GZIP-komprimierten Quelldatei.
     * @param localDestFilePath Der Pfad zur Zieldatei, in der der dekomprimierte Inhalt gespeichert wird.
     * @throws IOException Wenn ein Fehler während der Dekomprimierung auftritt.
     */
    public static void ungzipLocal( String localSourceZipFile, String localDestFilePath ) throws IOException
    {
       ZipUtils.ungzipLocal( localSourceZipFile, localDestFilePath );
    }

    /**
     * Dekomprimiert einen GZIP-komprimierten InputStream und schreibt die dekomprimierten Daten
     * in einen angegebenen Dateipfad.
     *
     * @param instreamZipped Der InputStream, der GZIP-komprimierte Daten enthält.
     * @param localDestFilePath Der Pfad der Datei, in die die dekomprimierten Daten geschrieben werden sollen.
     * @throws IOException Wenn ein Fehler während der Dekomprimierung oder beim Schreiben in die Datei auftritt.
     */
    public static void ungzipStream( InputStream instreamZipped, String localDestFilePath ) throws IOException
    {
       ZipUtils.ungzipStream( instreamZipped, localDestFilePath );
    }

    /**
     * Entpackt eine entfernte ZIP-Datei in ein angegebenes lokales Verzeichnis.
     *
     * @param remoteSourceZipFile Der Pfad zur entfernten ZIP-Datei, die entpackt werden soll.
     * @param localDestDir Der Pfad zum lokalen Verzeichnis, in das die Dateien extrahiert werden sollen.
     * @return Die Gesamtzahl der extrahierten Dateien.
     * @throws IOException Wenn ein Fehler beim Zugriff auf die entfernte ZIP-Datei oder während der Extraktion auftritt.
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
     * Entpackt eine lokale ZIP-Datei in ein angegebenes lokales Zielverzeichnis.
     *
     * @param localSourceZipFile Der Pfad zur lokalen ZIP-Quelldatei.
     * @param localDestDir Das lokale Verzeichnis, in das der Inhalt der ZIP-Datei extrahiert werden soll.
     * @return Die Gesamtzahl der extrahierten Einträge.
     * @throws IOException Wenn ein Fehler während des Entpackungsvorgangs auftritt.
     */
    public static long unzipLocal( String localSourceZipFile, String localDestDir ) throws IOException
    {
       return ZipUtils.unzipLocal( localSourceZipFile, localDestDir );
    }

    /**
     * Entpackt den Inhalt einer ZIP-Datei aus dem bereitgestellten InputStream in das angegebene lokale Zielverzeichnis.
     * Extrahiert Dateien aus dem ZIP-Stream, behält die Verzeichnisstruktur bei
     * und schreibt sie in das lokale Dateisystem.
     *
     * @param instreamZipped Der InputStream, der die komprimierten ZIP-Daten enthält.
     * @param localDestDir Das lokale Verzeichnis, in das der ZIP-Inhalt extrahiert werden soll.
     *                     Wenn null oder leer, wird das aktuelle Arbeitsverzeichnis verwendet.
     * @return Die Anzahl der erfolgreich aus dem ZIP-Archiv extrahierten Einträge (Dateien/Verzeichnisse).
     * @throws IOException Wenn ein Fehler während der Extraktion oder beim Schreiben in das Zielverzeichnis auftritt.
     */
    public static long unzipStream( InputStream instreamZipped, String localDestDir ) throws IOException
    {
       return ZipUtils.unzipStream( instreamZipped, localDestDir );
    }

    /**
     * Lädt Dateien aus einem entfernten Quellverzeichnis in ein lokales Zielverzeichnis herunter und verarbeitet diese
     * basierend auf spezifischen Filterkriterien. Dateien können wie besehen heruntergeladen oder dekomprimiert werden,
     * wenn sie im ZIP- oder GZ-Format vorliegen.
     *
     * @param remoteSrcDir Der Pfad zum entfernten Quellverzeichnis.
     * @param localDstDir  Der Pfad zum lokalen Zielverzeichnis.
     * @param filenameMustContain Ein String, der im Dateinamen enthalten sein muss.
     * @param maxAlterInTagen Das maximale Alter in Tagen für zu berücksichtigende Dateien.
     * @throws IOException Wenn ein Fehler beim Herunterladen oder Dekomprimieren auftritt.
     */
    public void downloadAndUnzip( String remoteSrcDir, String localDstDir, String filenameMustContain, int maxAlterInTagen ) throws IOException
    {
       Instant threshold = Instant.now().minus( maxAlterInTagen, ChronoUnit.DAYS );
       Path localPath = Paths.get( localDstDir );
       if (!Files.exists(localPath)) {
           Files.createDirectories(localPath);
       }
       List<FileData> fds = getFileDataList( remoteSrcDir );
       for( FileData fd : fds ) {
          if( fd.isFile && fd.name.contains( filenameMustContain ) && fd.timestamp.isAfter( threshold ) ) {
             String remoteSrcFilePath = fd.parentPath + "/" + fd.name;
             Path localDstFilePath  = localPath.resolve( fd.name );
             if( fd.name.toLowerCase().endsWith( ".zip" ) ) {
                unzipRemote( remoteSrcFilePath, localDstDir );
             } else if( fd.name.toLowerCase().endsWith( ".gz" ) ) {
                String localFilePathStr = localDstFilePath.toString();
                ungzipRemote( remoteSrcFilePath, localFilePathStr.substring( 0, localFilePathStr.length() - 3 ) );
             } else {
                downloadFile( remoteSrcFilePath, localDstFilePath.toString() );
             }
          }
       }
    }

    /**
     * Lädt Dateien herunter und entpackt diese (.zip und .gz).
     *
     * @param remoteSrcDir         Das entfernte Quellverzeichnis.
     * @param localDstDir          Das lokale Zielverzeichnis.
     * @param filenameMustContain  Filter für Dateinamen.
     * @param maxAlterInTagen      Maximales Alter der Dateien in Tagen.
     * @param benutzername         Benutzername für SFTP.
     * @param passwort             Passwort für SFTP.
     * @param host                 SFTP-Host.
     * @param port                 SFTP-Port.
     * @throws IOException         Bei Fehlern während des Vorgangs.
     */
    public static void downloadAndUnzip( String remoteSrcDir, String localDstDir, String filenameMustContain, String maxAlterInTagen,
                                         String benutzername, String passwort, String host, String port ) throws IOException
    {
       try( SFtpWrapper sftpWrapper = new SFtpWrapper( benutzername, passwort, host, Integer.parseInt( port ) ) ) {
          DateTimeFormatter df = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" ).withZone( ZoneId.systemDefault() );
          lombokLog.info( "Remote in " + remoteSrcDir + ":" );
          List<FileData> fds = sftpWrapper.getFileDataList( remoteSrcDir );
          for( FileData fd : fds ) {
             if( fd.isFile ) {
                lombokLog.info( df.format( fd.timestamp ) + ", " + fd.size + " Bytes, " + fd.name );
             }
          }
          sftpWrapper.downloadAndUnzip( remoteSrcDir, localDstDir, filenameMustContain, Integer.parseInt( maxAlterInTagen ) );
          lombokLog.info( "Lokal in " + localDstDir + ":" );
          File[] fls = (new File( localDstDir )).listFiles();
          if (fls != null) {
              for( File fl : fls ) {
                 lombokLog.info( df.format( Instant.ofEpochMilli( fl.lastModified() ) ) + ", " + fl.length() + " Bytes, " + fl.getName() );
              }
          }
       }
    }


}

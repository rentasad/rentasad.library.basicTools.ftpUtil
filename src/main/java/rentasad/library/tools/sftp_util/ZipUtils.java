package rentasad.library.tools.sftp_util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility-Klasse für die Handhabung von Archiv-Operationen (ZIP, GZIP).
 * Diese Klasse wurde aus dem SFtpWrapper extrahiert, um die Verantwortlichkeiten zu trennen.
 */
public class ZipUtils {

    /**
     * Dekomprimiert eine lokale GZIP-komprimierte Datei und speichert den dekomprimierten Inhalt
     * im angegebenen Zieldateipfad.
     *
     * @param localSourceZipFile Der Pfad zur lokalen GZIP-komprimierten Quelldatei.
     * @param localDestFilePath Der Pfad zur Zieldatei, in der der dekomprimierte Inhalt gespeichert wird.
     * @throws IOException Wenn ein Fehler während der Dekomprimierung auftritt.
     */
    public static void ungzipLocal(String localSourceZipFile, String localDestFilePath) throws IOException {
        try (InputStream instreamZipped = new FileInputStream(localSourceZipFile)) {
            ungzipStream(instreamZipped, localDestFilePath);
        } catch (Exception ex) {
            throw new IOException("Fehler beim Unzip von " + localSourceZipFile + ",", ex);
        }
    }

    /**
     * Dekomprimiert einen GZIP-komprimierten InputStream und schreibt die dekomprimierten Daten
     * in einen angegebenen Dateipfad.
     *
     * @param instreamZipped Der InputStream, der GZIP-komprimierte Daten enthält.
     * @param localDestFilePath Der Pfad der Datei, in die die dekomprimierten Daten geschrieben werden sollen.
     * @throws IOException Wenn ein Fehler während der Dekomprimierung oder beim Schreiben in die Datei auftritt.
     */
    public static void ungzipStream(InputStream instreamZipped, String localDestFilePath) throws IOException {
        try (GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(instreamZipped))) {
            try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(localDestFilePath))) {
                int size;
                byte[] buffer = new byte[64 * 1024];
                while ((size = zin.read(buffer, 0, buffer.length)) > 0) {
                    os.write(buffer, 0, size);
                }
            }
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
    public static long unzipLocal(String localSourceZipFile, String localDestDir) throws IOException {
        try (InputStream instreamZipped = new FileInputStream(localSourceZipFile)) {
            return unzipStream(instreamZipped, localDestDir);
        } catch (Exception ex) {
            throw new IOException("Fehler beim Unzip von " + localSourceZipFile + ",", ex);
        }
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
    public static long unzipStream(InputStream instreamZipped, String localDestDir) throws IOException {
        long anzahlEntries = 0;
        String remoteResultFilename = null;
        String destDir = (localDestDir == null) ? "" : localDestDir.trim();
        destDir = (destDir.endsWith("/") || destDir.endsWith("\\")) ? destDir : (destDir + File.separator);
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(instreamZipped))) {
            ZipEntry zipEntry;
            while ((zipEntry = zin.getNextEntry()) != null) {
                remoteResultFilename = zipEntry.getName();
                if (remoteResultFilename != null && remoteResultFilename.startsWith("/") && remoteResultFilename.length() > 1) {
                    remoteResultFilename = remoteResultFilename.substring(1);
                }
                
                File outFile = new File(destDir + remoteResultFilename);
                if (zipEntry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    // Sicherstellen, dass das Elternverzeichnis existiert
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outFile))) {
                        int size;
                        byte[] buffer = new byte[64 * 1024];
                        while ((size = zin.read(buffer, 0, buffer.length)) > 0) {
                            os.write(buffer, 0, size);
                        }
                    }
                }
                zin.closeEntry();
                anzahlEntries++;
            }
        } catch (Exception ex) {
            throw new IOException("Fehler beim Unzip, letzter Zip-Entry " + remoteResultFilename + ",", ex);
        }
        return anzahlEntries;
    }
}

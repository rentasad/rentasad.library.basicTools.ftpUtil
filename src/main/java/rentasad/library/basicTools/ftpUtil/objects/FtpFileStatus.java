package rentasad.library.basicTools.ftpUtil.objects;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPFile;

@Setter @Getter
public class FtpFileStatus
{

	/**
	 * Das FTP-File-Objekt.
	 */
	private FTPFile ftpFile;

	/**
	 * Das lokale File-Objekt.
	 */
	private File localFile;

	/**
	 * Der Dateiname.
	 */
	private String fileName;

	/**
	 * Gibt an, ob die Datei auf dem FTP-Server existiert.
	 */
	private boolean existFTPFile;

	/**
	 * Gibt an, ob die lokale Datei existiert.
	 */
	private boolean existLocalFile;

	/**
	 * Differenz zwischen lokaler Systemzeit und FTP-Serverzeit in Minuten.
	 */
	private int ftpServerTimeDifference;

	/**
	 * Zeitstempel der Datei auf dem FTP-Server.
	 */
	private Calendar timestampFtpFile;

	/**
	 * Zeitstempel der lokalen Datei.
	 */
	private Calendar timestampLocalFile;

	/**
	 * Zeitdifferenz zwischen den Dateien.
	 */
	private Date timeBetweenFiles;

	/**
	 * Maximales Alter der Datei in Minuten.
	 */
	private int maxAgeInMinutes;

	/**
	 * Gibt an, ob eine Semaphor-Datei existiert.
	 */
	private boolean semaphoreExist;

	/**
	 * Gibt an, ob eine CRC-Prüfung erforderlich ist.
	 */
	private boolean crcCheckNeeded;
	
	
	
	
	public FtpFileStatus(
							FTPFile ftpFile,
							File localFile)
	{
		super();
		this.ftpFile = ftpFile;
		this.localFile = localFile;
	}

	/**
	 * Alter in Millisekunden
	 * @return the ageOfFtpFile
	 * Gibt bei unbekanntem Alter null zurueck
	 */
	public Long getAgeOfFtpFile()
	{
		if (this.timestampFtpFile == null)
		{
			return null;
		}else
		{
			long timeNow = Calendar.getInstance().getTimeInMillis();
			long timeFtpFile = this.timestampFtpFile.getTimeInMillis();
			return timeNow - timeFtpFile;
		}

	}

	/**
	 * Alter in Millisekunden
	 * @return the ageOfLocalFile
	 */
	public Long getAgeOfLocalFile()
	{
		long timeNow = Calendar.getInstance().getTimeInMillis();
		long timeLocalFile = this.timestampLocalFile.getTimeInMillis();
		return timeNow - timeLocalFile;
	}

}

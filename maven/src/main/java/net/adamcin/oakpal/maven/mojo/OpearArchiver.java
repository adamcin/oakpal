package net.adamcin.oakpal.maven.mojo;

import org.codehaus.plexus.archiver.jar.JarArchiver;

public class OpearArchiver extends JarArchiver {
    public static final String ARCHIVE_TYPE = "opear";
    public OpearArchiver() {
        this.archiveType = ARCHIVE_TYPE;
    }
}

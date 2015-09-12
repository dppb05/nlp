package br.ufpe.cin.srmqnlp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileUtils {

	public static int countLines(File textFile) throws IOException {
		   InputStream is = new BufferedInputStream(new FileInputStream(textFile));
		    try {
		        byte[] c = new byte[1024];
		        int count = 0;
		        int readChars = 0;
		        boolean empty = true;
		        while ((readChars = is.read(c)) != -1) {
		            empty = false;
		            for (int i = 0; i < readChars; ++i) {
		                if (c[i] == '\n') {
		                    ++count;
		                }
		            }
		        }
		        return (count == 0 && !empty) ? 1 : count;
		    } finally {
		        is.close();
		    }		
	}
	
	public static List<File> getFiles(File dir, boolean verbose) {
		File[] subFiles = dir.listFiles();
		List<File> clusters = new ArrayList<File>(subFiles.length);
		for (int i = 0; i < subFiles.length; i++) {
			final File f = subFiles[i];
			if (f.isDirectory()) {
				clusters.add(f);
			}
		}
		Collections.sort(clusters);
		List<File> allFiles = new ArrayList<File>();
		for (int c = 0; c < clusters.size(); c++) {
			File []textFiles = clusters.get(c).listFiles();
			Arrays.sort(textFiles);
			for (File file : textFiles) {
				if (file.isFile()) {
					allFiles.add(file);
					if(verbose) {
						System.out.println(c + ",\"" + file.getParentFile().getName() + File.separator + file.getName() + "\"");
					}
				}
			}
		}
		return allFiles;
	}
}

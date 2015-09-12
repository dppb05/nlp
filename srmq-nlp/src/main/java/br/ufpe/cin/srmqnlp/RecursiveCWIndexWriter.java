package br.ufpe.cin.srmqnlp;

//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
//import java.nio.channels.FileChannel;
//import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class RecursiveCWIndexWriter {

	/**
	 * 
	 * @param baseInputPath
	 * @param baseOutputPath
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2 || args.length > 3) {
			System.err.println("Should provide 2 arguments <baseInputPath> and <baseOutputPath> OR 3 arguments <baseInputPath> <baseOutputPath> <keep_percent>");
			System.exit(-1);
		}
		File baseInputPath = new File(args[0]);
		File baseOutputPath = new File (args[1]);
		if (!baseInputPath.isDirectory()) {
			System.err.println("baseInputPath should be a directory");
			System.exit(-2);
		}
		if (!baseOutputPath.isDirectory()) {
			System.err.println("baseOutputPath should be a directory");
			System.exit(-3);
		}
		if (!baseInputPath.canRead()) {
			System.err.println("Application does not have read permissions to baseInputPath");
			System.exit(-4);
		}
		if (!baseOutputPath.canWrite()) {
			System.err.println("Application does not have write permissions to baseOutputPath");
			System.exit(-5);
		}
		double keepPercent = 1.0;
		if (args.length == 3) {
			keepPercent = Double.parseDouble(args[2]);
		}
		CWEmbeddingWriter cwWriter = new CWEmbeddingWriter();
		recursiveProcess(baseInputPath, baseOutputPath, cwWriter, keepPercent);

	}

	private static void recursiveProcess(File baseInputPath, File baseOutputPath, CWEmbeddingWriter cwWriter, double keepPercent) throws IOException {
		File[] stuffToProcess = baseInputPath.listFiles();
		int totalFiles = 0;
		Set<Integer> elimIndices = null;
		if (keepPercent < 1.0) {
			for (File file : stuffToProcess) {
				if (file.isFile()) totalFiles++;
			}
			final int keepFiles = Math.round(totalFiles * (float)keepPercent);
			final int excludeFiles = totalFiles - keepFiles;
			elimIndices = new HashSet<Integer>(excludeFiles);
			while (elimIndices.size() < excludeFiles) {
				final int elimIndex = Math.round((float)Math.random()*totalFiles);
				elimIndices.add(elimIndex);
			}
		}

		int elimFileIndex = 0;
		for (File file : stuffToProcess) {
			final String name = file.getName();
			if (file.isDirectory()) {
				final File newDir = new File(baseOutputPath.getCanonicalFile().toString() + File.separator + name);
				newDir.mkdir();
				recursiveProcess(file, newDir, cwWriter, keepPercent); 
			} else {
				if (elimIndices == null || !elimIndices.contains(elimFileIndex)) {
					BufferedReader bufr = new BufferedReader(new FileReader(file));
					BufferedWriter bufw = new BufferedWriter(new FileWriter(new File(baseOutputPath.getCanonicalPath() + File.separator + name)));
					cwWriter.cwIndicesForDocument(bufr, bufw);
//					FileChannel source = new FileInputStream(file).getChannel();
//					FileChannel dest = new FileOutputStream(new File(baseOutputPath.getCanonicalPath() + File.separator + name)).getChannel();
//					dest.transferFrom(source, 0, source.size());
//					source.close();
//					dest.close();
					bufw.close();
					bufr.close();
				}
				elimFileIndex++;
			}
		}
		
	}

}

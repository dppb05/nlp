package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class SimpleLSADissimGenerator {

	static boolean VERBOSIS = false;
	
	public static void main(String[] args) {
		int i;
		Integer k = 5000;
		String distance = "euclidean";
		File vocabFile = null;
		for(i = 0; i < args.length; ++i) {
			if(args[i].equals("-v") || args[i].equals("--verbosis")) {
				VERBOSIS = true;
			} else if(args[i].equals("--topk")) {
				k = Integer.parseInt(args[++i]);
			} else if(args[i].equals("--dist")) {
				distance = args[++i];
			} else if(args[i].equals("--vocab")) {
				vocabFile = new File(args[++i]);
			} else {
				break;
			}
		}
		if(i >= args.length) {
			System.err.println("Not enough arguments.");
			System.exit(1);
		}
		File parentDir = new File(args[i]);
		if (!parentDir.isDirectory()) {
			System.err.println("Argument given is not a directory");
			System.exit(2);
		}
		if (!parentDir.canRead()) {
			System.err.println("Cannot read from given directory");
			System.exit(3);
		}
		
		List<File> allFiles = getFiles(parentDir);
		try {
			String[] topWords = null;
			if(vocabFile != null) {
				Set<String> words = new HashSet<String>();
				BufferedReader reader = new BufferedReader(new FileReader(vocabFile));
				String word = reader.readLine();
				while(word != null) {
					words.add(word.trim().toLowerCase());
					word = reader.readLine();
				}
				reader.close();
				topWords = new String[words.size()];
				words.toArray(topWords);
			} else {
				topWords = TopKVocab.topKWords(allFiles, null, k, true);
				Arrays.sort(topWords);
//				PrintWriter writer = new PrintWriter(new FileOutputStream("/home/diogo/Documents/Universidade/mestrado/data/out.lst"));
//				for(String s : topWords) {
//					writer.write(s + "\n");
//				}
//				writer.close();
			}
			TermDocMatrix tdMatrix = new TermDocMatrix(topWords, allFiles);
			RConnection rConn = new RConnection();
			rConn.voidEval("library(\"lsa\");library(\"proxy\")");
			REXP termDocMtx = REXP.createDoubleMatrix(tdMatrix.wf().idf().getMatrix());
			rConn.assign("tdmat", termDocMtx);
			termDocMtx = null;
			rConn.voidEval("tdmat <- lsa(tdmat)");
			rConn.voidEval("tdmat <- dist(t(as.textmatrix(tdmat)), method=\""+ distance + "\")");
			double[][] dmatrix = rConn.eval("as.matrix(tdmat)").asDoubleMatrix();
			int j;
			for(i = 0; i < dmatrix.length; ++i) {
				System.out.print(dmatrix[i][0]);
				for(j = 1; j <= i; ++j) {
					System.out.print("," + dmatrix[i][j]);
				}
				System.out.println();
			}
//			for(double[] row : dmatrix) {
//				for(i = 0; i < row.length-1; ++i) {
//					System.out.print(row[i] + " ");
//				}
//				System.out.println(row[i]);
//			}
			rConn.voidEval("rm(tdmat)");
		} catch (RserveException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	private static List<File> getFiles(File dir) {
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
					System.out.println(c + ",\"" + file.getParentFile().getName() + File.separator + file.getName() + "\"");
				}
			}
		}
		return allFiles;
	}
	
}

package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

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
		for(i = 0; i < args.length; ++i) {
			if(args[i].equals("-v") || args[i].equals("--verbosis")) {
				VERBOSIS = true;
			} else if(args[i].equals("--topk")) {
				k = Integer.parseInt(args[++i]);
			} else if(args[i].equals("--dist")) {
				distance = args[++i];
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
			Vocabulary vocab = new Vocabulary(new File(CWEmbeddingWriter.CW_WORDS));
			EnStopWords stopWords = new EnStopWords(vocab);
			Integer[] wordsId = topKVocab(allFiles, stopWords, k);
			vocab = null;
			stopWords = null;
			TermDocMatrix tdMatrix = new TermDocMatrix(wordsId, allFiles);
			RConnection rConn = new RConnection();
			rConn.voidEval("library(\"lsa\");library(\"proxy\")");
			REXP termDocMtx = REXP.createDoubleMatrix(tdMatrix.wf().idf().getMatrix());
			rConn.assign("tdmat", termDocMtx);
			termDocMtx = null;
			rConn.voidEval("tdmat <- lsa(tdmat)");
			if(distance.equals("cosine")) {
				rConn.voidEval("library(\"inline\")");
				distance = "cosineDist";
				String[] code = {"src <- '" +
								"Rcpp::NumericVector xa(a); " +
								"Rcpp::NumericVector xb(b); " +
								"int n = xa.size(); " +
								"double sumAiBi = 0.0, aiSq = 0.0, biSq = 0.0; " +
								"for (int i = 2; i < n; i++) { " +
									"sumAiBi += xa[i] * xb[i]; " +
									"aiSq += xa[i] * xa[i]; " +
									"biSq += xb[i] * xb[i]; " +
								"} " +
								"const double cosine = sumAiBi / (sqrt(aiSq)*sqrt(biSq)); " +
								"const double cosineDist =  1.0 - (1.0 + cosine)*0.5; " +
								"return Rcpp::wrap(cosineDist);" +
								"'",
								"cosineDist <- cxxfunction(signature(a = \"numeric\", b = \"numeric\"), src, plugin=\"Rcpp\")",
								"pr_DB$set_entry(FUN = cosineDist, names = c(\"cosineDist\"))",
								"rm(src)"};
				for(String codeLine : code) {
					rConn.voidEval(codeLine);
				}
			}
			rConn.voidEval("tdmat <- dist(t(as.textmatrix(tdmat)), method=\""+ distance + "\")");
			double[][] dmatrix = rConn.eval("as.matrix(tdmat)").asDoubleMatrix();
			for(double[] row : dmatrix) {
				for(i = 0; i < row.length-1; ++i) {
					System.out.print(row[i] + " ");
				}
				System.out.println(row[i]);
			}
			rConn.voidEval("rm(tdmat)");
		} catch (IOException e) {
			System.err.println("Error reading vocabulary file " + CWEmbeddingWriter.CW_WORDS + ".");
		} catch (RserveException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
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
	
	private static class EntryIntIntComp implements Comparator<Entry<Integer, Integer>> {

		public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
			int ret = 0;
			if(o1.getKey() != o2.getKey()) {
				if(o1.getValue() != o2.getValue()) {
					ret = o1.getValue() - o2.getValue();
				} else {
					ret = o1.getKey() - o2.getKey();
				}
			}
			return ret;
		}
		
	}
	
	private static Integer[] topKVocab(List<File> files, EnStopWords stopWords, int k) {
		Map<Integer, Integer> wordCount = new HashMap<Integer, Integer>();
		Scanner in = null;
		Integer wordId;
		for(File f : files) {
			try {
				in = new Scanner(f);
				while(in.hasNextInt()) {
					wordId = in.nextInt();
					if((stopWords != null && stopWords.isStopWordIndex(wordId)) || wordId == CWEmbeddingWriter.UNK_WORD_ID) {
						continue;
					}
					Integer val = wordCount.get(wordId);
					if(val == null) {
						wordCount.put(wordId, 1);
					} else {
						wordCount.put(wordId, val + 1);
					}
				}
			} catch (FileNotFoundException e) {
				System.out.printf("File %s not found. Skipping.\n", f.getAbsolutePath());
			} finally {
				in.close();
				in = null;
			}
		}
		Entry<Integer, Integer>[] topWords = (Entry<Integer, Integer>[]) new Entry[k];
		int topWordsSize = 0;
		int i;
		for(Entry<Integer, Integer> e : wordCount.entrySet()) {
			if(topWordsSize < topWords.length) {
				topWords[topWordsSize++] = e;
				if(topWordsSize == topWords.length) {
					Arrays.sort(topWords, new EntryIntIntComp());
//					System.out.print("( ");
//					for(Entry<Integer, Integer> en : topWords) {
//						System.out.printf("[%d,%d] ", en.getKey(), en.getValue());
//					}
//					System.out.println(")");
				}
			} else {
				if(topWords[0].getValue() < e.getValue()) {
					for(i = 1; i < topWords.length && topWords[i].getValue() < e.getValue(); ++i);
					--i;
					for(int j = 0; j < i; ++j) {
						topWords[j] = topWords[j+1];
					}
					topWords[i] = e;
//					System.out.print("( ");
//					for(Entry<Integer, Integer> en : topWords) {
//						System.out.printf("[%d,%d] ", en.getKey(), en.getValue());
//					}
//					System.out.println(")");
				}
			}
		}
		Integer[] ret = new Integer[topWordsSize];
		for(i = 0; i < ret.length; ++i) {
			ret[i] = topWords[i].getKey();
		}
		return ret;
	}
}

package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

public class TopKVocab {
	
	private static boolean VERBOSIS = false;
	private static boolean DEBUG =  false;
	
	public static void main(String[] args) {
		int i;
		Integer k = 5000;
		String outpath = "";
		for(i = 0; i < args.length; ++i) {
			if(args[i].equals("-v") || args[i].equals("--verbosis")) {
				VERBOSIS = true;
			} else if(args[i].equals("--debug")) {
				DEBUG = true;
			} else if(args[i].equals("--topk")) {
				k = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-O") || args[i].equals("--outdir")) {
				outpath = args[++i] + "/";
			} else {
				break;
			}
		}
		File corpus = new File(args[i]);
		if(!corpus.isDirectory()) {
			System.err.println("Corpus is not a directory.");
			System.exit(4);
		}
		outpath = outpath + corpus.getName() + "-" + k + "-words.txt";
		if(VERBOSIS) {
			System.out.println("Output path: " + outpath);
		}
		String[] topWords = topKWords(corpus, k, true);
		PrintWriter wordWrite = null;
		try {
			wordWrite = new PrintWriter(outpath);
			for(String word : topWords) {
				wordWrite.println(word);
				if(VERBOSIS) {
					System.out.println(word);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(5);
		} finally {
			if(wordWrite != null) {
				wordWrite.close();
			}
		}
		System.exit(0);
	}
	
	public static String[] topKWords(File corpus, int k, boolean stopWords) {
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		Scanner in = null;
		String word;
		List<File> files = FileUtils.getFiles(corpus, false);
		for(File f : files) {
			try {
				in = new Scanner(f);
				while(in.hasNext()) {
					word = in.next();
					if(stopWords && !EnStopWords.isStopWord(word)) {
						Integer val = wordCount.get(word);
						if(val == null) {
							wordCount.put(word, 1);
						} else {
							wordCount.put(word, val + 1);
						}
					}
				}
			} catch (FileNotFoundException e) {
				System.out.printf("File %s not found. Skipping.\n", f.getAbsolutePath());
			} finally {
				in.close();
				in = null;
			}
		}
		@SuppressWarnings("unchecked")
		Entry<String, Integer>[] topWords = (Entry<String, Integer>[]) new Entry[k];
		int topWordsSize = 0;
		int i;
		EntryStrIntComp comp = new EntryStrIntComp();
		for(Entry<String, Integer> e : wordCount.entrySet()) {
			if(topWordsSize < topWords.length) {
				topWords[topWordsSize++] = e;
				if(topWordsSize == topWords.length) {
					Arrays.sort(topWords, comp);
					if(DEBUG) {
						System.out.print("( ");
						for(Entry<String, Integer> en : topWords) {
							System.out.printf("[%s,%d] ", en.getKey(), en.getValue());
						}
						System.out.printf(") %d\n", topWordsSize);
					}
				}
			} else {
				if(topWords[0].getValue() < e.getValue()) {
					for(i = 1; i < topWords.length && topWords[i].getValue() < e.getValue(); ++i);
					--i;
					for(int j = 0; j < i; ++j) {
						topWords[j] = topWords[j+1];
					}
					topWords[i] = e;
					if(DEBUG) {
						System.out.print("( ");
						for(Entry<String, Integer> en : topWords) {
							System.out.printf("[%s,%d] ", en.getKey(), en.getValue());
						}
						System.out.printf(") %d\n", topWordsSize);
					}
				}
			}
		}
		String[] ret = new String[topWordsSize];
		for(i = 0; i < ret.length; ++i) {
			ret[i] = topWords[i].getKey();
		}
		return ret;
	}
	
	private static class EntryStrIntComp implements Comparator<Entry<String, Integer>> {

		public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
			int ret = 0;
			if(o1.getKey() != o2.getKey()) {
				if(o1.getValue() != o2.getValue()) {
					ret = o1.getValue() - o2.getValue();
				} else {
					ret = o1.getKey().compareTo(o2.getKey());
				}
			}
			return ret;
		}
		
	}

}

package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;

public class TopKVocab {
	
	private static boolean VERBOSIS = false;
	private static boolean DEBUG =  false;
	
	public static void main(String[] args) {
		int i;
		Integer k = 5000;
		String outpath = "";
		Vocabulary vocab = null;
		File vocabFile = null;
		Embeddings embed = null;
		File embedFile = null;
		int vecSize = 50;
		for(i = 0; i < args.length; ++i) {
			if(args[i].equals("-v") || args[i].equals("--verbosis")) {
				VERBOSIS = true;
			} else if(args[i].equals("--debug")) {
				DEBUG = true;
			} else if(args[i].equals("--topk")) {
				k = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-O") || args[i].equals("--outdir")) {
				outpath = args[++i] + "/";
			} else if(args[i].equals("--vocab")) {
				try {
					vocabFile = new File(args[++i]);
					vocab = new Vocabulary(vocabFile);
				} catch (IOException e) {
					System.err.println("Error reading vocabulary file " + CWEmbeddingWriter.CW_WORDS + ".");
					System.exit(1);
				}
			} else if(args[i].equals("--embed")) {
				embedFile = new File(args[++i]);
			} else if(args[i].equals("--vec-size")) {
				vecSize = 50;
			} else {
				break;
			}
		}
		if(embedFile != null && vocab == null) {
			System.err.println("Cannot create embedding without vocabulary.");
			System.exit(2);
		}
		File corpus = new File(args[i]);
		if(!corpus.isDirectory()) {
			System.err.println("Corpus is not a directory.");
			System.exit(3);
		}
		String vocabFilename = null;
		String embedFilename = null;
		if(vocab != null) {
			vocabFilename = corpus.getName() + "-" + k + "-" + vocabFile.getName() + "-words.lst";
			embedFilename = corpus.getName() + "-" + k + "-" + embedFile.getName() + "-embeddings.txt";
		} else {
			vocabFilename = corpus.getName() + "-" + k + "-words.lst";
		}
		if(VERBOSIS) {
			System.out.println("Output path: " + outpath);
		}
		String[] topWords = topKWords(corpus, vocab, k, true);
		Arrays.sort(topWords);
		PrintWriter wordWrite = null;
		PrintWriter embedWrite = null;
		try {
			if(embedFile != null) {
				embed = new Embeddings(embedFile, vocab, vecSize);
				embedWrite = new PrintWriter(outpath + embedFilename);
				float[] e = null;
				wordWrite = new PrintWriter(outpath + vocabFilename);
				int j;
				for(String word : topWords) {
					wordWrite.println(word);
					e = embed.embeddingFor(word);
					for(j = 0; j < e.length-1; ++j) {
						embedWrite.write(e[j] + " ");
					}
					embedWrite.write(e[j] + "\n");
					if(VERBOSIS) {
						System.out.println(word);
					}
				}
			} else {
				wordWrite = new PrintWriter(outpath + vocabFilename);
				for(String word : topWords) {
					wordWrite.println(word);
					if(VERBOSIS) {
						System.out.println(word);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(4);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(5);
		} finally {
			if(wordWrite != null) {
				wordWrite.close();
			}
			if(embedWrite != null) {
				embedWrite.close();
			}
		}
		System.exit(0);
	}
	
	public static String[] topKWords(File corpus, Vocabulary vocab, int k, boolean stopWords) {
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		BufferedReader reader = null;
		String word;
		List<File> files = FileUtils.getFiles(corpus, false);
		Locale locale = Locale.ENGLISH;
		if(vocab != null) {
			locale = vocab.getLocale();
		}
		for(File f : files) {
			try {
				reader = new BufferedReader(new FileReader(f));
				Tokenizer<CoreLabel> tokenizer = PTBTokenizerFactory.newCoreLabelTokenizerFactory("").getTokenizer(reader);
				while(tokenizer.hasNext()) {
					word = tokenizer.next().toString().toLowerCase(locale);
					if((vocab != null && !vocab.contains(word)) || (stopWords && EnStopWords.isStopWord(word))) {
						continue;
					}
					Integer val = wordCount.get(word);
					if(val == null) {
						wordCount.put(word, 1);
					} else {
						wordCount.put(word, val + 1);
					}
				}
				reader.close();
			} catch (FileNotFoundException e) {
				System.out.printf("File %s not found. Skipping.\n", f.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
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
//					if(DEBUG) {
//						System.out.print("( ");
//						for(Entry<String, Integer> en : topWords) {
//							System.out.printf("[%s,%d] ", en.getKey(), en.getValue());
//						}
//						System.out.printf(") %d\n", topWordsSize);
//					}
				}
			} else {
				if(topWords[0].getValue() < e.getValue()) {
					for(i = 1; i < topWords.length && topWords[i].getValue() < e.getValue(); ++i);
					--i;
					for(int j = 0; j < i; ++j) {
						topWords[j] = topWords[j+1];
					}
					topWords[i] = e;
//					if(DEBUG) {
//						System.out.print("( ");
//						for(Entry<String, Integer> en : topWords) {
//							System.out.printf("[%s,%d] ", en.getKey(), en.getValue());
//						}
//						System.out.printf(") %d\n", topWordsSize);
//					}
				}
			}
		}
		String[] ret = new String[topWordsSize];
		for(i = 0; i < ret.length; ++i) {
			ret[i] = topWords[i].getKey();
			if(DEBUG) {
				System.out.printf("[%s,%d]\n", topWords[i].getKey(), topWords[i].getValue());
			}
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

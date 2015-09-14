package br.ufpe.cin.srmqnlp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

public class W2VConverter {
	
	private static boolean VERBOSIS = false;
	private static boolean DEBUG = false;
	
	public static void main(String[] args) {
		int i;
		Integer k = 5000;
		File corpus = null;
		String outpath = "";
		for(i = 0; i < args.length; ++i) {
			if(args[i].equals("-v") || args[i].equals("--verbosis")) {
				VERBOSIS = true;
			} else if(args[i].equals("--debug")) {
				DEBUG = true;
			} else if(args[i].equals("--topk")) {
				k = Integer.parseInt(args[++i]);
			} else if(args[i].equals("--corpus")) {
				corpus = new File(args[++i]);
				if(!corpus.isDirectory()) {
					System.err.println("Corpus is not a directory.");
					System.exit(4);
				}
			} else if(args[i].equals("-O") || args[i].equals("--outdir")) {
				outpath = args[++i] + "/";
			} else {
				break;
			}
		}
		if(i >= args.length) {
			System.err.println("Not enough arguments.");
			System.exit(1);
		}
		File file = new File(args[i]);
		if (!file.isFile()) {
			System.err.println("Argument given is not a file");
			System.exit(2);
		}
		if (!file.canRead()) {
			System.err.println("Cannot read from given file");
			System.exit(3);
		}
		
		String[] topWords = null;
		if(corpus != null) {
			if(VERBOSIS) {
				System.out.println("Checking top "+ k + " words from corpus...");
			}
			topWords = TopKVocab.topKWords(corpus, k, true);
			Arrays.sort(topWords);
		}
		long wordsWritten = 0;
		try {
			BufferedInputStream inFileBis = new BufferedInputStream(new FileInputStream(file));
			if(VERBOSIS) {
				System.out.println("Starting conversion.");
			}
			long words = readLongText(inFileBis);
			int embedSize = (int) readLongText(inFileBis);
			if(VERBOSIS) {
				System.out.printf("Words: %s\nEmbedding size: %d\n", words, embedSize);
			}
			PrintWriter embedWriter = new PrintWriter(outpath+"embeddings.txt");
			PrintWriter wordWriter = new PrintWriter(outpath+"words.lst");
			byte[] floatBytes = new byte[4];
			float[] embed = new float[embedSize];
			float len;
			while(--words >= 0) {
				String word = readWord(inFileBis);
				if(topWords != null && Arrays.binarySearch(topWords, word) < 0) {
					inFileBis.skip(floatBytes.length * embedSize);
					continue;
				}
				if(VERBOSIS) {
					System.out.printf("Adding \"%s\"\n", word);
				}
				++wordsWritten;
				wordWriter.println(word);
				len = 0;
				for (i = 0; i < embedSize; i++) {
					for (int j = 0; j < floatBytes.length ; j++) {
						floatBytes[j] = (byte)inFileBis.read();
					}
					final float f = ByteBuffer.wrap(floatBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
					embed[i] = f;
					len += f * f;
				}
				len = (float) Math.sqrt(len);
				for(i = 0; i < embedSize-1; ++i) {
					embedWriter.print(embed[i] / len + " ");
				}
				embedWriter.println(embed[i] / len);
			}
			embedWriter.close();
			wordWriter.close();
			if(VERBOSIS) {
				System.out.printf("Process terminated. Words written: %d.\n", wordsWritten);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(5);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(6);
		}
		System.exit(0);
	}
	
	private static long readLongText(BufferedInputStream bis) throws IOException {
		String longText = readCharToken(bis);
		return Long.parseLong(longText);
	}

	private static String readCharToken(BufferedInputStream bis) throws IOException {
		StringBuffer buf = new StringBuffer();
		char c = (char) bis.read();
		while (Character.isWhitespace(c)) {
			c = (char) bis.read();
		}
		while (!Character.isWhitespace(c)) {
			buf.append(c);
			c = (char) bis.read();
		}
		return buf.toString();
	}
	
	private static String readWord(BufferedInputStream bis) throws IOException {
		StringBuffer buf = new StringBuffer();
		char c = (char) bis.read();
		while (c != ' ') {
			if (c != '\n')
				buf.append(c);
			c = (char) bis.read();
		}
		return buf.toString();
	}
	
	
	
	

}

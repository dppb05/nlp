package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Vocabulary {
	private Map<String, Integer> wordToIndex;
	private Locale locale;
	private Integer unkWordID;
	
	public Vocabulary(File wordListFile, Locale locale, boolean genUnkWordId) throws IOException {
		this.locale = locale;
		int lineNum = 1;
		BufferedReader bufw = new BufferedReader(new FileReader(wordListFile));
		String line;
		wordToIndex = new HashMap<String, Integer>();
		while ((line = bufw.readLine()) != null) {
			line = line.trim().toLowerCase(locale);
			if (line.length() > 0) {
				wordToIndex.put(line, lineNum);
			}
			lineNum++;
		}
		if(genUnkWordId) {
			this.unkWordID = lineNum;
		} else {
			this.unkWordID = null;
		}
		bufw.close();
	}
	
	public boolean contains(String word) {
		return this.wordToIndex.containsKey(word);
	}
	
	public int size() {
		return this.wordToIndex.size();
	}
	
	public Vocabulary(File wordListFile) throws IOException {
		this(wordListFile, Locale.getDefault(), false);
	}
	
	public Vocabulary(File wordListFile, boolean genUnkWordId) throws IOException {
		this(wordListFile, Locale.getDefault(), genUnkWordId);
	}
	
	public Integer getId(String word) {
		return this.wordToIndex.get(word);
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public Integer getUnkWordId() {
		return this.unkWordID;
	}
}

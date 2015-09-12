package br.ufpe.cin.srmqnlp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EnStopWords {
	public static final String[] EN_STOP_WORDS = { 
		"a", "about", "across", "after", "all", "almost", "also", "am", "among", 
		"an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", 
		"can", "could", "did", "does", "either", "else", "ever", "every",
		"for", "from", "he", "her", "hers", "him", "his", "how", "however", "i", 
		"if", "in", "into", "is", "it", "its", "just", "least", "let", "me", "most", 
		"must", "my", "neither",
		"no", "not", "of", "off", "often", "on", "only", "or", "other", "our", "rather", "she", "should", "since", "so", 
		"some", "than", 
		"that", "the", "their", "them", "then", "there", "these", 
		"they", "this", "to", "too", "us", "was", "we", "were", "what", "when", "where", "which", "while", "who", 
		"whom", "why", "will", "with", "would", "yet", "you", "your", ",", ".", ";", ":", "(", ")", "[", "]", "{",
		"}", "'", "\"", "-", "?"
		};
	
	static {
		Arrays.sort(EN_STOP_WORDS);
	}
	
	private Set<Integer> stopWordIndices;
	
	private Set<String> stopWordSet;
	public EnStopWords(Vocabulary vocab) {
		this.stopWordSet = new HashSet<String>(EN_STOP_WORDS.length);
		this.stopWordSet.addAll(Arrays.asList(EN_STOP_WORDS));
		
		this.stopWordIndices = new HashSet<Integer>(EN_STOP_WORDS.length);
		
		for (int i = 0; i < EN_STOP_WORDS.length; i++) {
			Integer id = vocab.getId(EN_STOP_WORDS[i]);
			if (id != null) this.stopWordIndices.add(id);
		}
	}
	
	public boolean isStopWordIndex(Integer id) {
		return this.stopWordIndices.contains(id);
	}
	
	public static boolean isStopWord(String word) {
		return Arrays.binarySearch(EN_STOP_WORDS, word) >= 0;
	}
}

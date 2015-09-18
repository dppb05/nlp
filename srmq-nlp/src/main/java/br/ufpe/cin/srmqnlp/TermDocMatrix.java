package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;

public class TermDocMatrix {
	private double[][] matrix;
	
	public TermDocMatrix(String[] words, List<File> files) throws IOException {
		this(words, files, Locale.ENGLISH);
	}
	
	public TermDocMatrix(String[] words, List<File> files, Locale locale) throws IOException {
		this.matrix = new double[words.length][files.size()];
		for(int i = 0; i < words.length; ++i) {
			for(int j = 0; j < files.size(); ++j) {
				this.matrix[i][j] = 0.0;
			}
		}
		Arrays.sort(words);
		String word;
		int row;
		BufferedReader reader = null;
		for(int i = 0; i < files.size(); ++i) {
			try {
				reader = new BufferedReader(new FileReader(files.get(i)));
				Tokenizer<CoreLabel> tokenizer = PTBTokenizerFactory.newCoreLabelTokenizerFactory("").getTokenizer(reader);
				while(tokenizer.hasNext()) {
					word = tokenizer.next().toString().toLowerCase(locale);
					row = Arrays.binarySearch(words, word);
					if(row >= 0) {
						++this.matrix[row][i];
					}
				}
				reader.close();
			} catch (FileNotFoundException e) {
				System.err.println("Could not find " + files.get(i).getAbsolutePath() + " (will be disregarded).");
			}
		}
	}
	
	public TermDocMatrix wf() {
		for(int i = 0; i < this.matrix.length; ++i) {
			for(int j = 0; j < this.matrix[i].length; ++j) {
				if(this.matrix[i][j] > 0) {
					this.matrix[i][j] = 1 + Math.log(this.matrix[i][j]);
				}
			}
		}
		return this;
	}
	
	public TermDocMatrix idf() {
		double df;
		for(int i = 0; i < this.matrix.length; ++i) {
			df = 0;
			for(int j = 0; j < this.matrix[i].length; ++j) {
				df += this.matrix[i][j];
			}
			for(int j = 0; j < this.matrix[i].length; ++j) {
				this.matrix[i][j] *= Math.log(this.matrix[i].length / df);
			}
		}
		return this;
	}
	
	public void print() {
		int j;
		for(int i = 0; i < this.matrix.length; ++i) {
			for(j = 0; j < this.matrix[i].length-1; ++j) {
				System.out.print(this.matrix[i][j] + " ");
			}
			System.out.println(this.matrix[i][j]);
		}
	}
	
	public double[][] getMatrix() {
		return this.matrix;
	}
}
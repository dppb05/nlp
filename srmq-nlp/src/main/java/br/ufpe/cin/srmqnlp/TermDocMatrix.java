package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class TermDocMatrix {
	private double[][] matrix;
	
	public TermDocMatrix(Integer[] wordsId, List<File> files) {
		this.matrix = new double[wordsId.length][files.size()];
		Arrays.sort(wordsId);
		Scanner in = null;
		Integer wordId;
		int row;
		for(int i = 0; i < files.size(); ++i) {
			try {
				in = new Scanner(files.get(i));
				while(in.hasNextInt()) {
					wordId = in.nextInt();
					row = Arrays.binarySearch(wordsId, wordId);
					if(row >= 0) {
						++matrix[row][i];
					}
				}
			} catch (FileNotFoundException e) {
				System.err.println("Could not find " + files.get(i).getAbsolutePath() + " (will be disregarded).");
			} finally {
				if(in != null) { 
					in.close();
					in = null;
				}
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
				System.err.print(this.matrix[i][j] + " ");
			}
			System.out.println(this.matrix[i][j]);
		}
	}
	
	public double[][] getMatrix() {
		return this.matrix;
	}
}
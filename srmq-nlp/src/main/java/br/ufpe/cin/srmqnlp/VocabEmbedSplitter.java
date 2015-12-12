package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class VocabEmbedSplitter {

	public static void main(String[] args) {
		if(args.length != 1) {
			System.err.println("Must pass only one file as parameter.");
			System.exit(1);
		}
		File infile = new File(args[0]);
		if(!infile.isFile()) {
			System.err.println("Argument is not a file.");
			System.exit(2);
		}
		String infileName = infile.getName().substring(0, infile.getName().lastIndexOf('.'));
//		String vocabFileName = infile.getName() + "-words.lst";
//		String embedFileName = infile.getName() + "-embeddings.txt";
		PrintWriter vocabWriter = null;
		PrintWriter embedWriter = null;
		Scanner scan = null;
		try {
			vocabWriter = new PrintWriter(infileName + "-words.lst");
			embedWriter = new PrintWriter(infileName + "-embeddings.txt");
			scan = new Scanner(infile);
			int k = scan.nextInt();
			int size = scan.nextInt();
			for(int i = 0; i < k; ++i) {
				vocabWriter.println(scan.next());
				embedWriter.println(scan.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(3);
		} finally {
			if(vocabWriter != null) {
				vocabWriter.close();
			}
			if(embedWriter != null) {
				embedWriter.close();
			}
			if(scan != null) {
				scan.close();
			}
		}
		System.exit(0);
	}

}

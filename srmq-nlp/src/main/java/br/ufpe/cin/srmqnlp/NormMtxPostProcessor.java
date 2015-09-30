package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import wrdca.util.DissimMatrix;
import wrdca.util.SimpleTextInputFileNormalizer;

public class NormMtxPostProcessor {

	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("Invalid args. Must pass dissimMtxFile and number of objects.");
		}
		File dissimMtxFile = new File(args[0]);
		if(!dissimMtxFile.isFile()) {
			System.err.println(args[0] + " is not a file.");
			System.exit(1);
		}
		int n = Integer.parseInt(args[1]);
		DissimMatrix dissimMatrix;
		try {
			dissimMatrix = readDissimMtxFile(dissimMtxFile, n);
			List<Integer> negatives = new ArrayList<Integer>();
			for(int i = 0; i < n; ++i) {
				double val = 0.0;
				int posit = 0;
				for(int j = 0; j < n; ++j) {
					if(dissimMatrix.getDissim(i, j) > 0) {
						val += dissimMatrix.getDissim(i, j);
						++posit;
					} else if(dissimMatrix.getDissim(i, j) < 0) {
						negatives.add(j);
					}
				}
				val /= posit;
				for(int j = 0; j < negatives.size(); ++j) {
					dissimMatrix.putDissim(i, negatives.get(j), val);
				}
				negatives.clear();
			}
			SimpleTextInputFileNormalizer.printObjects(dissimMatrix, dissimMtxFile, n, new HashSet<Integer>());
			for(int i = 0; i < n; ++i) {
				int j = 0;
				for(; j < i; ++j) {
					System.out.print(dissimMatrix.getDissim(i, j) + ",");
				}
				System.out.println(dissimMatrix.getDissim(i, j));
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		System.exit(0);
	}
	
	private static DissimMatrix readDissimMtxFile(File file, int n) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(file));
		for (int i = 0; i < n; i++) {
			buf.readLine();
		}
		DissimMatrix dissimMatrix = new DissimMatrix(n);
		for(int i = 0; i < n; ++i) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			for(int j = 0; j <= i; j++) {
				final double dissim = Double.parseDouble(strtok.nextToken());
				dissimMatrix.putDissim(i, j, dissim);
			}
		}
		buf.close();
		return dissimMatrix;
	}

}

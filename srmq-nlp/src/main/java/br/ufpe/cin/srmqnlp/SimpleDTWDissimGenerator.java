package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

public class SimpleDTWDissimGenerator {
	private static Map<String, String[]> customDistFunctions;
	// accepted distances: everything R (Euclidian, cosine...); meanWfIdfEuclidian, meanWfIdfCosineDist
	static {
		customDistFunctions = new HashMap<String, String[]>(); // maps from function name to function code
		{ // Custom Euclidian with log(tf)*idf weighting (mean of the two words)
			String meanWfIdfEuclidian = "meanWfIdfEuclidian";
			String[] meanWfIdfEuclidianCode = {"src <- '" +  
												"Rcpp::NumericVector xa(a); " +
												"Rcpp::NumericVector xb(b); " +
												"int n = xa.size(); " +
												"double dist = 0.0; " +
												"for (int i = 2; i < n; i++) { " +
													"const double idist = xa[i] - xb[i]; " +
													"dist += idist*idist; " +
												"} "						   +
												"const double wfdfA = (xa[0] > 0) ? (1.0 + log(xa[0])) : 0.0; " +
												"const double wfdfB = (xb[0] > 0) ? (1.0 + log(xb[0])) : 0.0; " +
												"const double meanWfIdf = ((wfdfA * xa[1]) + (wfdfB * xb[1]))*0.5; " +
												"return Rcpp::wrap(meanWfIdf * sqrt(dist)); " +
												"'",
											"meanWfIdfEuclidian <- cxxfunction(signature(a = \"numeric\", b = \"numeric\"), src, plugin=\"Rcpp\")",
											"pr_DB$set_entry(FUN = meanWfIdfEuclidian, names = c(\"test_meanWfIdfEuclidian\", \"meanWfIdfEuclidian\"))",
											"rm(src)"};
			customDistFunctions.put(meanWfIdfEuclidian, meanWfIdfEuclidianCode);
		}
		
		{ // Custom Cosine dist with log(tf)*idf weighting (mean of the two words)
			String meanWfIdfCosineDist = "meanWfIdfCosineDist";
			String[] meanWfIdfCosineDistCode = {"src <- '" +  
												"Rcpp::NumericVector xa(a); " +
												"Rcpp::NumericVector xb(b); " +
												"int n = xa.size(); " +
												"double sumAiBi = 0.0, aiSq = 0.0, biSq = 0.0; " +
												"for (int i = 2; i < n; i++) { " +
													"sumAiBi += xa[i] * xb[i]; " +
													"aiSq += xa[i] * xa[i]; " +
													"biSq += xb[i] * xb[i]; " +
												"} "						   +
												"const double cosine = sumAiBi / (sqrt(aiSq)*sqrt(biSq)); " +
												"const double cosineDist =  1.0 - (1.0 + cosine)*0.5; " +
												"const double wfdfA = (xa[0] > 0) ? (1.0 + log(xa[0])) : 0.0; " +
												"const double wfdfB = (xb[0] > 0) ? (1.0 + log(xb[0])) : 0.0; " +
												"const double meanWfIdf = ((wfdfA * xa[1]) + (wfdfB * xb[1]))*0.5; " +
												"return Rcpp::wrap(meanWfIdf * cosineDist); " +
												"'",
											"meanWfIdfCosineDist <- cxxfunction(signature(a = \"numeric\", b = \"numeric\"), src, plugin=\"Rcpp\")",
											"pr_DB$set_entry(FUN = meanWfIdfCosineDist, names = c(\"test_meanWfIdfCosineDist\", \"meanWfIdfCosineDist\"))",
											"rm(src)"};
			customDistFunctions.put(meanWfIdfCosineDist, meanWfIdfCosineDistCode);
		}
		
		{
			String meanWfIdfManhattanDist = "meanWfIdfManhattanDist";
			String[] meanWfIdfManhattanDistCode = {"src <- '" +  
												"Rcpp::NumericVector xa(a); " +
												"Rcpp::NumericVector xb(b); " +
												"int n = xa.size(); " +
												"double manDist = 0.0;" +
												"for (int i = 2; i < n; i++) { " +
													"if(xb[i] > xa[i]) {" +
														"manDist += xb[i] - xa[i]; " +
													"} else { " +
														"manDist += xa[i] - xb[i]; " +
													"} " +
												"} "						   +
												"const double wfdfA = (xa[0] > 0) ? (1.0 + log(xa[0])) : 0.0; " +
												"const double wfdfB = (xb[0] > 0) ? (1.0 + log(xb[0])) : 0.0; " +
												"const double meanWfIdf = ((wfdfA * xa[1]) + (wfdfB * xb[1]))*0.5; " +
												"return Rcpp::wrap(meanWfIdf * manDist); " +
												"'",
											"meanWfIdfManhattanDist <- cxxfunction(signature(a = \"numeric\", b = \"numeric\"), src, plugin=\"Rcpp\")",
											"pr_DB$set_entry(FUN = meanWfIdfManhattanDist, names = c(\"test_meanWfIdfManhattanDist\", \"meanWfIdfManhattanDist\"))",
											"rm(src)"};
			customDistFunctions.put(meanWfIdfManhattanDist, meanWfIdfManhattanDistCode);
		}
		
		{
			String meanWfIdfChebyshevDist = "meanWfIdfChebyshevDist";
			String[] meanWfIdfChebyshevDistCode = {"src <- '" +  
												"Rcpp::NumericVector xa(a); " +
												"Rcpp::NumericVector xb(b); " +
												"int n = xa.size(); " +
												"double chebDist; " +
												"if(xb[2] > xa[2]) { " +
													"chebDist = xb[2] - xa[2]; " +
												"} else { " +
													"chebDist = xa[2] - xb[2]; " +
												"} " +
												"double val;" +
												"for (int i = 3; i < n; i++) { " +
													"if(xb[i] > xa[i]) {" +
														"val = xb[i] - xa[i]; " +
													"} else { " +
														"val = xa[i] - xb[i]; " +
													"} " +
													"if(val > chebDist) { " +
														"chebdist = val; " +
													"} " +
												"} "						   +
												"const double wfdfA = (xa[0] > 0) ? (1.0 + log(xa[0])) : 0.0; " +
												"const double wfdfB = (xb[0] > 0) ? (1.0 + log(xb[0])) : 0.0; " +
												"const double meanWfIdf = ((wfdfA * xa[1]) + (wfdfB * xb[1]))*0.5; " +
												"return Rcpp::wrap(meanWfIdf * chebDist); " +
												"'",
											"meanWfIdfChebyshevDist <- cxxfunction(signature(a = \"numeric\", b = \"numeric\"), src, plugin=\"Rcpp\")",
											"pr_DB$set_entry(FUN = meanWfIdfChebyshevDist, names = c(\"test_meanWfIdfChebyshevDist\", \"meanWfIdfChebyshevDist\"))",
											"rm(src)"};
			customDistFunctions.put(meanWfIdfChebyshevDist, meanWfIdfChebyshevDistCode);
		}
	}
	
public static void main(String[] args) throws IOException, REXPMismatchException, REngineException {
	int i;
	File vocabFile = null;
	File embedFile = null;
	int vecSize = 50;
	for(i = 0; i < args.length; ++i) {
		if(args[i].equals("--vocab")) {
			vocabFile = new File(args[++i]);
		} else if(args[i].equals("--embed")) {
			embedFile = new File(args[++i]);
		} else if(args[i].equals("--vec-size")) {
			vecSize = Integer.parseInt(args[++i]);
		} else {
			break;
		}
	}
	File parentDir = new File(args[i++]);
	if (!parentDir.isDirectory()) {
		System.err.println("Argument given is not a directory");
		System.exit(-2);
	}
	if (!parentDir.canRead()) {
		System.err.println("Cannot read from given directory");
		System.exit(-3);
		
	}
	final String distanceFunction = (i < args.length) ? args[i++] : "Euclidean";
	System.out.println(distanceFunction);
	
	boolean useCustomTfIdf = (i < args.length);
	
	File dfFile = null;
	if (useCustomTfIdf) dfFile = new File(args[i]);
	File []subFiles = parentDir.listFiles();
	List<File> clusters = new ArrayList<File>(subFiles.length);
	for (i = 0; i < subFiles.length; i++) {
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
	//System.out.println("total files: " + allFiles.size());

	Vocabulary vocab = null;
	if(vocabFile != null) {
		vocab = new Vocabulary(vocabFile, true);
	} else {
		vocab = new Vocabulary(new File(CWEmbeddingWriter.CW_WORDS));
	}
	Embeddings embed = null;
	if(embedFile != null) {
		embed = new Embeddings(embedFile, vocab, vecSize);
	} else {
		new Embeddings(new File(CWEmbeddingWriter.CW_EMBEDDINGS), vocab, vecSize);
	}
	EnStopWords stopWords = new EnStopWords(vocab);
		TokenIndexDocumentProcessor docProcessor = new TokenIndexDocumentProcessor(
				vocabFile != null ? vocab.getUnkWordId() : Constants._UNK_WORD_ID);
	TokenIndexDocumentProcessor.DFMapping dfMapping = null;
	if (useCustomTfIdf) dfMapping = docProcessor.generateDFMapping(dfFile);
	RConnection rConn = new RConnection();
	rConn.voidEval("library(\"dtw\")");
	if (useCustomTfIdf) { 
		rConn.voidEval("library(\"inline\")");
		if (!customDistFunctions.containsKey(distanceFunction)) {
			throw new IllegalStateException("Custom function " + distanceFunction + " does not exist");
		}
		String[] codeToExecute = customDistFunctions.get(distanceFunction);
		for (String codeLine : codeToExecute) {
			rConn.voidEval(codeLine);
		}
	}
	byte gcCount = 0;
	for (int me = 0; me < allFiles.size(); me++) {
		double[][] myEmbeddings;
		if (!useCustomTfIdf) myEmbeddings = docProcessor.toEmbeddings(allFiles.get(me), embed, stopWords);
		else myEmbeddings = docProcessor.toEmbeddings(allFiles.get(me), embed, stopWords, dfMapping);
		REXP embedDoc = REXP.createDoubleMatrix(myEmbeddings);
		rConn.assign("myEmbeds", embedDoc);
		embedDoc = null;
		for (int other = 0; other < me; other++) {
			double[][] otherEmbeddings;
			if (!useCustomTfIdf) otherEmbeddings = docProcessor.toEmbeddings(allFiles.get(other), embed, stopWords);
			else otherEmbeddings = docProcessor.toEmbeddings(allFiles.get(other), embed, stopWords, dfMapping);
			final boolean hasZeroLengthDoc = myEmbeddings.length == 0 || otherEmbeddings.length == 0;
			embedDoc = REXP.createDoubleMatrix(otherEmbeddings);
			rConn.assign("otherEmbeds", embedDoc);
			embedDoc = null;
			String myCode;
			double distance = -1.0;
			if (!hasZeroLengthDoc) {
				distance = -1.1;
				String method = "\"" + distanceFunction + "\"";
				if (myEmbeddings.length > otherEmbeddings.length) {
					myCode = "OAlign <- dtw(otherEmbeds, myEmbeds, dist.method=" + method + ", step=asymmetric, distance.only=TRUE, open.begin=TRUE, open.end=TRUE)"; 
				} else {
					myCode = "OAlign <- dtw(myEmbeds, otherEmbeds, dist.method=" + method + ", step=asymmetric, distance.only=TRUE, open.begin=TRUE, open.end=TRUE)"; 
				}
				final REXP r = rConn.parseAndEval("try("+myCode+",silent=TRUE)");
				if (r.inherits("try-error")) { 
					System.err.println("Error: "+r.asString());
				} else {
					distance = rConn.eval("OAlign$normalizedDistance").asDouble();
				}
			}
			System.out.print(distance + ",");
			if (++gcCount % 100 == 0){
				rConn.voidEval("gc()");
				gcCount = 0;
			}
			rConn.voidEval("rm(otherEmbeds)");
		}
		
		System.out.println("0.0");
		rConn.voidEval("rm(myEmbeds)");
	}
	rConn.close();
} 

}

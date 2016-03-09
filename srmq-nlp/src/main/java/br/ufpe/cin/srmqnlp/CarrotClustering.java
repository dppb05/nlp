package br.ufpe.cin.srmqnlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;
import org.carrot2.core.attribute.CommonAttributesDescriptor;

import wrdca.util.ConfusionMatrix;

public class CarrotClustering {
	
	private int numberOfObjects;
	private String basePath;
	private String dissimFile;
	private int numInit;
	
	public CarrotClustering(int numberOfObjects, String basePath, String dissimFile, int numInit) {
		this.numberOfObjects = numberOfObjects;
		this.basePath = basePath;
		this.dissimFile = dissimFile;
		this.numInit = numInit;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			System.err.println("Should give 4 arguments: numberOfObjects basePath dissimFile numInit");
			System.exit(-1);
		}
		final int nObjects = Integer.parseInt(args[0]);
		final String baseP = args[1];
		final String dissimF = args[2];
		final int nInit = Integer.parseInt(args[3]);
		CarrotClustering cClust = new CarrotClustering(nObjects, baseP, dissimF, nInit);
		cClust.cluster();
		System.exit(0);

	}
	
	public void cluster() throws IOException {
		final int k = 20;
		final int aPrioriNumber = 20;
		Map<Document, String> docsAndIds = new HashMap<Document, String>(numberOfObjects);
		BufferedReader bufr = null;
		Map<String, Pair<Integer, String>> idDocument2IndexDocumentAndIdClass = new HashMap<String, Pair<Integer, String>>(numberOfObjects);
		try {
			bufr = new BufferedReader(new FileReader(this.dissimFile));
			for (int i = 0; i < this.numberOfObjects; i++) {
				final String line = bufr.readLine();
				StringTokenizer sttok = new StringTokenizer(line, "\"");
				String idClass = sttok.nextToken();
				idClass = idClass.substring(0, idClass.lastIndexOf(','));
				String docId = sttok.nextToken();
				idDocument2IndexDocumentAndIdClass.put(docId, new Pair<Integer, String>(i, idClass));
				File docFile = new File(this.basePath + File.separator + docId);
				StringBuffer stb = new StringBuffer((int)docFile.length());
				BufferedReader bufrFile = new BufferedReader(new FileReader(docFile));
				String lineFile;
				while ((lineFile = bufrFile.readLine()) != null) {
					stb.append(lineFile);
					stb.append(' ');
				}
				bufrFile.close();
				Document carrotDocument = new Document(stb.toString());
				docsAndIds.put(carrotDocument,	docId);
			}
			long timeInMilis = System.currentTimeMillis();
			final Controller controller = ControllerFactory.createSimple();
			final Map<String, Object> attributes = new HashMap<String, Object>();
			final Set<Document> docsToCluster = docsAndIds.keySet();
			System.out.println("INFO: We have " + docsToCluster.size() + " documents to cluster");
			attributes.put(CommonAttributesDescriptor.Keys.DOCUMENTS, new ArrayList<Document>(docsToCluster));
			attributes.put("BisectingKMeansClusteringAlgorithm.clusterCount", k);
			ProcessingResult result = null;
			ProcessingResult bestResult = null;
			int bestInst = 0;
			double maxj = -1.0;
			double curj;
			for(int i = 1; i <= this.numInit; ++i) {
				System.out.println("Running " + i);
				result = controller.process(attributes, BisectingKMeansClusteringAlgorithm.class);
				if(maxj < 0) {
					bestInst = 1;
					maxj = result.getAttribute("energia");
					bestResult = result;
					System.out.println("Energy: " + maxj);
				} else {
					curj = result.getAttribute("energia");
					System.out.println("Energy: " + curj);
					if(curj > maxj) {
						bestInst = i;
						maxj = curj;
						bestResult = result;
					}
				}
			}
			timeInMilis = System.currentTimeMillis() - timeInMilis;
			List<Cluster> clusters = bestResult.getClusters();
			//CarrotConsoleFormatter.displayClusters(clusters);
			int docCount = 0;
			ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, aPrioriNumber);
			for (int i = 0; i < clusters.size(); i++) {
				final Cluster clusterI = clusters.get(i);
				if (clusterI.getSubclusters() != null && clusterI.getSubclusters().size() > 0) {
					System.out.println("INFO: Cluster " + i + " has subclusters");
				}
				System.out.println("Cluster " + i + ":");
				System.out.println(clusterI.getLabel());
				List<Document> docs = clusterI.getAllDocuments();
				docCount += docs.size();
				for (Document document : docs) {
					final String docId = docsAndIds.get(document);
					final Pair<Integer, String> myDocumentIdAndClassId = idDocument2IndexDocumentAndIdClass.get(docId);
					confusionMatrix.putObject(myDocumentIdAndClassId.getFirst(), i, Integer.parseInt(myDocumentIdAndClassId.getSecond()));
					System.out.println(docId);
				}
				System.out.println("");
			}
			System.out.println("Total document count in clusters: " + docCount);
			PrintStream outStream;
			/*if (runner.outputFile == null) {
				outStream = System.out;
			} else {
				outStream = new PrintStream(runner.outputFile, "UTF-8");
			}*/
			outStream = System.out;
			outStream.println("Best energy in instance " + bestInst);
			outStream.println("------CONFUSION MATRIX-------");
			confusionMatrix.printMatrix(outStream);
			outStream.println("-----------------------------");
			outStream.println(">>>>>>>>>>>> The Energy    is: "+ maxj);
			outStream.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
			outStream.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
			outStream.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
			outStream.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;
			outStream.println("Total time in seconds: " + timeInMilis/1000.0);
			outStream.flush();
			System.exit(0);

			
		} finally {
			if (bufr != null) bufr.close();
		}
		
	}

}

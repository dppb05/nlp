package br.ufpe.cin.srmqnlp;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;

/**
 * Hello world!
 *
 */
public class CWEmbeddingWriter 
{
	public static final String SENNA_PATH = "/media/OS/Users/Diogo/Documents/Universidade/mestrado/codes/senna"; 
	public static final String CW_EMBEDDINGS = SENNA_PATH + "/embeddings/embeddings.txt";
	public static final String CW_WORDS = SENNA_PATH + "/hash/words.lst";
	
	public static final int UNK_WORD_ID = 2;
	
	private static final Locale locale = Locale.ENGLISH;
	private Vocabulary vocab;
	
	public CWEmbeddingWriter() throws IOException {
    	this.vocab = new Vocabulary(new File(CW_WORDS));		
	}
	
    public void cwIndicesForDocument(Reader inputDocument, Writer outputIndices) throws IOException {
    	for (Tokenizer<CoreLabel> tokenizer = PTBTokenizerFactory.newCoreLabelTokenizerFactory("").getTokenizer(inputDocument);
    			tokenizer.hasNext();) {
    		CoreLabel token = tokenizer.next();
    		String tokenString = token.toString().toLowerCase(locale);
    		Integer id = this.vocab.getId(tokenString);
    		if (id == null) {
    			id = UNK_WORD_ID;
    		}
    		outputIndices.write(id.toString());
    		outputIndices.write('\n');
    	}
    	outputIndices.flush();
    }
    
    
}

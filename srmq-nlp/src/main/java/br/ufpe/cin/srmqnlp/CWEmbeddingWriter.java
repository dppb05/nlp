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
	
	private static final Locale locale = Locale.ENGLISH;
	private Vocabulary vocab;
	private int UNK_WORD_ID;
	
	public CWEmbeddingWriter(Vocabulary vocab) {
		this.vocab = vocab;
		if(this.vocab.getUnkWordId() != null) {
			this.UNK_WORD_ID = this.vocab.getUnkWordId();
		} else {
			this.UNK_WORD_ID = Constants._UNK_WORD_ID;
		}
	}
	
	public CWEmbeddingWriter() throws IOException {
    	this.vocab = new Vocabulary(new File(CW_WORDS));
    	this.UNK_WORD_ID = Constants._UNK_WORD_ID;
	}
	
    public void cwIndicesForDocument(Reader inputDocument, Writer outputIndices) throws IOException {
    	for (Tokenizer<CoreLabel> tokenizer = PTBTokenizerFactory.newCoreLabelTokenizerFactory("").getTokenizer(inputDocument);
    			tokenizer.hasNext();) {
    		CoreLabel token = tokenizer.next();
    		String tokenString = token.toString().toLowerCase(locale);
    		Integer id = this.vocab.getId(tokenString);
    		if (id == null) {
    			id = this.UNK_WORD_ID;
    		}
    		outputIndices.write(id.toString());
    		outputIndices.write('\n');
    	}
    	outputIndices.flush();
    }
    
    
}

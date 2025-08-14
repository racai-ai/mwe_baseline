package mwe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;

public class Ollama {
	public static List<Float> getEmbeddings(String s, OllamaAPI ollamaAPI, String ollamaEmbeddingsModel) throws IOException, InterruptedException, OllamaBaseException {
    	OllamaEmbedResponseModel embeddings = ollamaAPI.embed(ollamaEmbeddingsModel, Arrays.asList(s));
    	List<Double> ld=embeddings.getEmbeddings().get(0);
    	List<Float> lf=new ArrayList<Float>(ld.size());
    	for(Double d:ld)lf.add(d.floatValue());
    	return lf;
	}
	
	
}

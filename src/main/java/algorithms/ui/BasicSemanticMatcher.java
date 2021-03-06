package algorithms.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import algorithms.alignmentcombination.ProfileWeight;
import algorithms.equivalencematching.BasicEQMatcher;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import rita.wordnet.jwnl.JWNLException;
import services.parsers.CellParser;

/**
 * A basic semantic matching prototype without any ontology profiling, mismatch detection and with only two matchers (basic EQ matcher and basic SUB matcher)
 * @author audunvennesland
 *
 */
public class BasicSemanticMatcher {
	
	
	static File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-301.rdf");
	static File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-302.rdf");
	static String finalAlignmentStorePath = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/FINAL_ALIGNMENT/";

	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException, AlignmentException, URISyntaxException, JWNLException {
    runBasicSemanticMatching();
  }

  public static String textFromURIAlignment(URIAlignment uriAlignment){
      ArrayList<String> list = new ArrayList<String>();
      
      for(Cell cell : uriAlignment){
		String source = CellParser.getSource(cell);
		String target = CellParser.getTarget(cell);
		String relation = CellParser.getRelation(cell);
		String confidence = CellParser.getConfidence(cell);
        list.add("Source: " + source + ", Target: " + target + ", Relation: " + relation + ", Confidence: " + confidence);
	  }
      return String.join(" | ", list);
  }
  
  public static String runBasicSemanticMatching() throws OWLOntologyCreationException, IOException, AlignmentException, URISyntaxException, JWNLException {
    long startTimeMatchingProcess = System.currentTimeMillis();

    /* run matcher ensemble EQ */
    ArrayList<URIAlignment> eqAlignments = computeEQAlignments(ontoFile1, ontoFile2);

    /* combine using ProfileWeight EQ */
    URIAlignment combinedEQAlignment = combineEQAlignments(eqAlignments);    

    // store the EQ alignment
    File outputAlignment = new File(finalAlignmentStorePath + "EQAlignment.rdf");

    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputAlignment)), true);
    AlignmentVisitor renderer = new RDFRendererVisitor(writer);

    System.out.println("See EQAlignment.rdf for results");

    combinedEQAlignment.render(renderer);

    writer.flush();
    writer.close();

    long endTimeMatchingProcess = System.currentTimeMillis();

    System.out.println("The semantic matching operation took "
        + String.format("%.1f", (endTimeMatchingProcess - startTimeMatchingProcess) / 1000f) + " seconds.");

    return textFromURIAlignment(combinedEQAlignment);
  }
	
	/**
	 * This method makes a call to the individual equivalence matchers which produce their alignments.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology 
	 * @param ontologyProfilingScores a map holding scores from the ontology profiling process
	 * @param vectorFile a file holding terms and corresponding embedding vectors
	 * @return an ArrayList of URIAlignments produced by the individual equivalence matchers.
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	 * @throws URISyntaxException
	   Jul 15, 2019
	 */
	private static ArrayList<URIAlignment> computeEQAlignments(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

		System.out.print("Computing Basic Equivalence Matcher Alignment");
		long startTimeBWM = System.currentTimeMillis();
		URIAlignment BEMAlignment = BasicEQMatcher.returnBasicEQMatcherAlignment(ontoFile1, ontoFile2);	
		eqAlignments.add(BEMAlignment);
		long endTimeBEM = System.currentTimeMillis();
		System.out.print("... " + String.format("%.1f", (endTimeBEM - startTimeBWM)  / 1000f) + " seconds.\n");

		return eqAlignments;

	}
	
	/**
	 * Combines individual equivalence alignments into a single alignment
	 * @param inputAlignments individual alignments produced by an emsemble of matchers
	 * @return a URIAlignment holding equivalence relations produced by an ensemble of matchers
	 * @throws AlignmentException
	 * @throws IOException
	 * @throws URISyntaxException
	   Jul 15, 2019
	 */
	private static URIAlignment combineEQAlignments (ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {

		URIAlignment combinedEQAlignment = ProfileWeight.computeProfileWeightingEquivalence(inputAlignments);

		return combinedEQAlignment;

	}

}

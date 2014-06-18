
/**
 * 
 * @author Thomas Bosch
 *
 */
public class ValidationEnvironment 
{
	public static void main(String[] args) 
	{
		// base directory
		String baseDir = "C:\\Daten\\Dokumente\\Dropbox\\Diss\\Spin";
//		String baseDir = "D:\\Thomas\\Dropbox\\Diss\\Spin\\Spin\\src\\main\\java";
		
		// result text file ( relative path and file name )
		String resultTxt_RelativePathAndFileName = "\\results\\results.txt";
		
		// result RDF graph ( relative path and file name )
		String resultGraph_RelativePathAndFileName = "\\RDFGraph.txt";
				
		Spin spin = new Spin( baseDir, resultTxt_RelativePathAndFileName, resultGraph_RelativePathAndFileName );
		
		// parameter: RDF graphs ( containing SPIN mappings, constraints, data, rules ) [ relative paths and file names ] 
		spin.runInferences_checkConstraints( 
//			"templates\\OWL2_SPIN-Mapping.ttl",
//			"templates\\OWL2_Constraints_Data.ttl",
//			"rules.ttl",
			"templates\\DSP_SPIN-Mapping.ttl",
			"templates\\DSP_Constraints_Data.ttl"
		);
		
//		spin.SPARQL2SPIN( "SELECT" );

		spin.finalize();
	}
}

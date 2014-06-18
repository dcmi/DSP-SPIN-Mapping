import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.constraints.SimplePropertyPath;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.model.Construct;
import org.topbraid.spin.model.Function;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.SystemTriples;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * @author Thomas Bosch
 *
 */
public class Spin 
{
	// base directory
	String baseDir = null;
	
	// reader
	BufferedReader br = null;
	
	// writer
	StringBuilder results_sb = null;
	PrintWriter writer = null;
	
	// result RDF graph ( relative path and file name )
	String resultGraph_RelativePathAndFileName = null;
	
	// locally stored SPIN-related templates and functions
	OntModel ontModel_TemplatesFunctions = null;
	
	/**
	 * 
	 * 
	 * @param baseDir
	 * @param resultTxt_RelativePathAndFileName
	 * @param resultGraph_RelativePathAndFileName
	 */
	public Spin( String baseDir, String resultTxt_RelativePathAndFileName, String resultGraph_RelativePathAndFileNam )
	{
		// initialization
		init( baseDir, resultTxt_RelativePathAndFileName, resultGraph_RelativePathAndFileNam );
	}
	
	/**
	 * initialization
	 * 
	 * @param baseDir
	 * @param resultTxt_RelativePathAndFileName
	 * @param resultGraph_RelativePathAndFileName
	 */
	private void init( String baseDir, String resultTxt_RelativePathAndFileName, String resultGraph_RelativePathAndFileName )
	{
		// base directory
		this.baseDir = baseDir;
		
		// writer
		results_sb = new StringBuilder();
        try 
		{
			writer = new PrintWriter( baseDir + resultTxt_RelativePathAndFileName, "UTF-8" );
		} 
		catch ( FileNotFoundException | UnsupportedEncodingException e ) { e.printStackTrace(); }
        
        // result RDF graph ( relative path and file name )
     	this.resultGraph_RelativePathAndFileName = resultGraph_RelativePathAndFileName;
		
		// initialize SPIN system functions ( such as sp:gt (>) ) and templates
		SPINModuleRegistry.get().init();
		
		// locally stored SPIN-related templates and functions
		Model graph_TemplatesFunctions = getRDFGraph( "functions\\functions.ttl", "TTL" ); 
		graph_TemplatesFunctions.add( getRDFGraph( "functions\\dspFunctions.ttl", "TTL" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "templates\\templates.ttl", "TTL" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\sp.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\spl.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\spif.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\spinx.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\sparqlmotionfunctions.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\functions-smf.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\functions-afn.rdf", "RDF/XML" ) );
//		graph_TemplatesFunctions.add( getRDFGraph( "SPIN_functions_templates\\functions-fn.rdf", "RDF/XML" ) );
		ontModel_TemplatesFunctions = JenaUtil.createOntologyModel( OntModelSpec.OWL_MEM, graph_TemplatesFunctions );			
	}
	
	/**
	 * check constraints
	 * 
	 * @param String... rdfGraphs_RelativePathAndFileName
	 */
	public void runInferences_checkConstraints( String... rdfGraphs_RelativePathAndFileName )
	{
		// fill RDF graph ( containing SPIN mappings, constraints, data, rules ) 
		Model graph = getRDFGraph( rdfGraphs_RelativePathAndFileName[0], "TTL" ); // concrete syntaxes: TTL, RDF/XML, ...
		for ( int i = 1; i < rdfGraphs_RelativePathAndFileName.length; i++)
		{
			graph.add( getRDFGraph( rdfGraphs_RelativePathAndFileName[i], "TTL" ) );
		}
		
		// create OntModel with imports
		OntModel ontModel = JenaUtil.createOntologyModel( OntModelSpec.OWL_MEM, graph );
		
		// create and add model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel();
		ontModel.addSubModel( newTriples );
		
		// add locally stored SPIN-related templates and functions to model
		ontModel.add( ontModel_TemplatesFunctions );
		
		// register locally stored SPIN-related templates and functions
		SPINModuleRegistry.get().registerAll( ontModel, null );
//		for ( Function f : SPINModuleRegistry.get().getFunctions() )
//		{
//			System.out.println(f.getURI());
//		}
		
		// run all inferences
		SPINInferences.run( ontModel, newTriples, null, null, false, null );
		System.out.println( "Inferred triples: " + newTriples.size() );
		newTriples.write( System.out, "TTL" );
		
		// check constraints
		List<ConstraintViolation> constraintViolations = SPINConstraints.check( ontModel, null );
		
		// write constraint violations
		results_sb.append( "Constraint violations" );
		results_sb.append( System.lineSeparator() );
		results_sb.append( "---------------------" );
		results_sb.append( System.lineSeparator() );
		results_sb.append( System.lineSeparator() );
		for( ConstraintViolation constraintViolation : constraintViolations ) 
		{
			results_sb.append( " - source: " ).append( SPINLabels.get().getLabel( constraintViolation.getSource() ) );
			results_sb.append( System.lineSeparator() );
			results_sb.append( " - root: " ).append( SPINLabels.get().getLabel( constraintViolation.getRoot() ) );
			results_sb.append( System.lineSeparator() );
			results_sb.append( " - message: " ).append( constraintViolation.getMessage() );
			results_sb.append( System.lineSeparator() );
			for ( SimplePropertyPath violationPath : constraintViolation.getPaths() ) 
			{
				results_sb.append( " - path: " ).append( violationPath.toString() );
				results_sb.append( System.lineSeparator() );
		    }
			results_sb.append( " - # fixes: " ).append( constraintViolation.getFixes().size() );
	        results_sb.append( System.lineSeparator() );
	        results_sb.append( System.lineSeparator() );
		}
		
		results_sb.append( "violation root | violation message | # violation path | # violation fixes" );
		results_sb.append( System.lineSeparator() );
		results_sb.append( "-------------------------------------------------------------------------" );
		results_sb.append( System.lineSeparator() );
		results_sb.append( System.lineSeparator() );
		for( ConstraintViolation constraintViolation : constraintViolations ) 
		{
			results_sb.append( SPINLabels.get().getLabel( constraintViolation.getRoot() ) ).append( " | " );
			results_sb.append( constraintViolation.getMessage() ).append( " | " );
			for ( SimplePropertyPath violationPath : constraintViolation.getPaths() ) 
			{
				results_sb.append( violationPath.toString() ).append( " | " );
		    }
			results_sb.append( constraintViolation.getFixes().size() );
	        results_sb.append( System.lineSeparator() );
		}
		
		// write result graph
		try 
		{
			PrintWriter w = new PrintWriter( baseDir + resultGraph_RelativePathAndFileName, "UTF-8" );
			graph.write( w, "N3" );
			w.close();
		} 
		catch ( FileNotFoundException | UnsupportedEncodingException e ) { e.printStackTrace(); }
		
		// write results
		if ( writer != null )
		{
			writer.println( results_sb.toString() );
		}
	}
	
	/**
	 * SPARQL 2 SPIN
	 * 
	 * @param queryForm
	 */
	public void SPARQL2SPIN ( String queryForm )
	{
		// Create an empty OntModel importing SP
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix( "xsd", "http://www.w3.org/2001/XMLSchema#" );
		model.setNsPrefix( "rdf", RDF.getURI() );
		model.setNsPrefix( "rdfs", "http://www.w3.org/2000/01/rdf-schema#" );
		model.setNsPrefix( "owl", "http://www.w3.org/2002/07/owl#" );
		model.setNsPrefix( "sp", "http://spinrdf.org/sp#" );
		model.setNsPrefix( "ex", "http://example.org/demo#" );
		model.setNsPrefix( "kennedys", "http://topbraid.org/examples/kennedys#" );
		
		// query string
		String query_RelativePathAndFileName = "SPARQL2SPIN.rq";
		String queryString = getQueryString( query_RelativePathAndFileName );
		
		Query arqQuery = ARQFactory.get().createQuery( model, queryString );
		ARQ2SPIN arq2SPIN = new ARQ2SPIN( model );
		
		if ( StringUtils.equals( queryForm, "SELECT" ) )
		{
			Select spinQuery = ( Select ) arq2SPIN.createQuery( arqQuery, null );
			
			System.out.println("SPIN query in Turtle:");
			model.write( System.out, FileUtils.langTurtle );
			
			System.out.println("-----");
			String str = spinQuery.toString();
			System.out.println( "SPIN query:\n" + str );
			
			// Now turn it back into a Jena Query
			Query parsedBack = ARQFactory.get().createQuery( spinQuery );
			System.out.println( "Jena query:\n" + parsedBack );
		}
		else
		{
			Construct spinQuery = ( Construct ) arq2SPIN.createQuery( arqQuery, null );
			
			System.out.println("SPIN query in Turtle:");
			model.write( System.out, FileUtils.langTurtle );
			
			System.out.println("-----");
			String str = spinQuery.toString();
			System.out.println( "SPIN query:\n" + str );
			
			// Now turn it back into a Jena Query
			Query parsedBack = ARQFactory.get().createQuery( spinQuery );
			System.out.println( "Jena query:\n" + parsedBack );
		}
		
		
		
		
	}
	
	public void callSPINTemplate()
	{
		String nsURI = "dcap#";
		
		String nsPrefix = "dcap";
		
		// Query of the template - argument will be arg:predicate
		String queryString =
			"SELECT *\n" +
			"WHERE {\n" +
			"    owl:Thing ?predicate ?object .\n" +
			"}";
		
		// create main model
//		Model model = JenaUtil.createDefaultModel();
		Model model = getRDFGraph( "instances_1.ttl", "TTL" );
		
		// get RDF graph ( constraints )
		Model graphConstraints = getRDFGraph( "constraints_Full.ttl", "TTL" ); 
		model.add( graphConstraints );
		
		JenaUtil.initNamespaces( model.getGraph() );
		model.add( SystemTriples.getVocabularyModel() ); // Add some queryable triples
		model.setNsPrefix( nsPrefix, nsURI );
		model.setNsPrefix( ARG.PREFIX, ARG.NS );
		
		// create SPIN template
		String template_name = "cardinality";
		Template template = createSPINTemplate( model, queryString, template_name, nsURI );

		// call the template
		com.hp.hpl.jena.query.Query arqQuery = ARQFactory.get().createQuery( ( Select ) template.getBody() );
		QueryExecution qexec = ARQFactory.get().createQueryExecution( arqQuery, model );
		QuerySolutionMap arqBindings = new QuerySolutionMap();
		arqBindings.add( "predicate", RDFS.label );
		qexec.setInitialBinding( arqBindings ); // Pre-assign the arguments
		ResultSet rs = qexec.execSelect();
		RDFNode object = rs.next().get( "object" );
		System.out.println( "Label is " + object );
	}
	
	/**
	 * create SPIN template
	 * 
	 * @param graph
	 * @param queryString
	 * @param template_Name
	 * @param template_NS
	 * @return template
	 */
	private static Template createSPINTemplate( Model graph, String queryString, String template_Name, String template_NS ) 
	{	
		// Jena ARQ SPARQL query
		com.hp.hpl.jena.query.Query arqQuery = ARQFactory.get().createQuery( graph, queryString );
		
		// SPIN query
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN( graph ).createQuery( arqQuery, null );
		
		// create a SPIN template
		Template template = graph.createResource( template_NS + template_Name, SPIN.Template ).as( Template.class );
		
		// spin:body
		template.addProperty( SPIN.body, spinQuery );
		
		// spl:Argument
		Resource argument = graph.createResource( SPL.Argument );
		argument.addProperty( SPL.predicate, graph.getProperty( ARG.NS + "predicate" ) );
		argument.addProperty( SPL.valueType, RDF.Property );
		argument.addProperty( RDFS.comment, "restricted predicate" );
		template.addProperty( SPIN.constraint, argument );
		
		return template;
	}
	
	/**
	 * get RDF graph
	 * 
	 * @param rdfGraph_RelativePathAndFileName
	 * @param rdfGraph_ConcreteSyntax
	 */
	public Model getRDFGraph( String rdfGraph_RelativePathAndFileName, String rdfGraph_ConcreteSyntax )
	{
		Model model = ModelFactory.createDefaultModel();
		
		try 
		{
			model.read( new FileInputStream( baseDir + "\\SPIN\\" + rdfGraph_RelativePathAndFileName ), null, rdfGraph_ConcreteSyntax );
		} 
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		return model;
	}
	
	/**
	 * get query string
	 * 
	 * @param query_RelativePathAndFileName
	 * @return queryString
	 */
	public String getQueryString( String query_RelativePathAndFileName )
	{
		String queryString = null;
		
		try 
	    {
	    	br = new BufferedReader( new FileReader( baseDir + "\\SPARQL\\" + query_RelativePathAndFileName ) );
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while ( line != null ) 
	        {
	            sb.append( line );
	            sb.append( System.lineSeparator() );
	            line = br.readLine();
	        }
	        
	        queryString = sb.toString();
	        
	    	br.close();
	    }
	    catch (FileNotFoundException e){}
	    catch ( IOException e ){}
		
		return queryString;
	}
	
	public void finalize()
	{
		// close writer
		if ( writer != null )
		{
			writer.close();
		}
	}
}

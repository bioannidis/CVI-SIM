package gui.wizards.createsubstratewizard;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.filters.FilterUtils;
import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator;
import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import gui.SimulatorConstants;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Factory;

import tools.XMLParser;

import model.NetworkGraph;
import model.Substrate;
import model.SubstrateLinkFactory;
import model.SubstrateNodeFactory;
import model.components.Link;
import model.components.Node;

import com.nexes.wizard.WizardPanelDescriptor;

public class CSFinishPanelDescriptor extends WizardPanelDescriptor {
	
	public static final String IDENTIFIER = "CS_FINISH_PANEL";
	
	List<Substrate> substrates;
	
	NetworkGraph g;
	
	// Random Request parameters
	private String prefix;
	private int numSubstrates;
	private int minNodes;
	private int maxNodes;
	private int minLinks;
	private int maxLinks;
	private String linkConnectivity;
	private double linkProbability;
	
	// Random generation parameters
	Factory<Graph<Node, Link>> graphFactory;
	SubstrateNodeFactory nodeFactory;
	SubstrateLinkFactory linkFactory;
	
	// Import generation parameters
	private File file;
	
	CSFinishPanel finishPanel;
    
    public CSFinishPanelDescriptor() { 	
    	finishPanel = new CSFinishPanel();
    	setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(finishPanel);
    }
    
    public Object getNextPanelDescriptor() {
        return FINISH;
    }
    
    public Object getBackPanelDescriptor() {
    	if (((CSuWizard) this.getWizard()).getMethod()
    			.equals(SimulatorConstants.RANDOM_SUBSTRATE))
    		return CSRandomPanelDescriptor.IDENTIFIER;
    	else
    		return CSImportPanelDescriptor.IDENTIFIER;
    }
    
    public void aboutToDisplayPanel() {

    	substrates = new ArrayList<Substrate>();
    	// getting parameters from wizard
    	if (((CSuWizard) this.getWizard()).getMethod()
    			.equals(SimulatorConstants.RANDOM_SUBSTRATE)) {
	    	prefix = ((CSuWizard) getWizard()).getPrefix();
	    	numSubstrates = ((CSuWizard) getWizard()).getNumSubstrates();
	    	minNodes = ((CSuWizard) getWizard()).getMinNodes();
	    	maxNodes = ((CSuWizard) getWizard()).getMaxNodes();
	    	linkConnectivity = ((CSuWizard) getWizard()).getLinkConnectivity();
	    	if (this.linkConnectivity.equals(SimulatorConstants.LINK_PER_NODE_CONNECTIVITY)) {
		    	minLinks = ((CSuWizard) getWizard()).getMinLinks();
		    	maxLinks = ((CSuWizard) getWizard()).getMaxLinks();
	    	} else {
	    		linkProbability = ((CSuWizard) getWizard()).getLinkProbability();
	    	}
    	} else if (((CSuWizard) this.getWizard()).getMethod()
    			.equals(SimulatorConstants.IMPORT_SUBSTRATE)) {
    		file = ((CSuWizard) getWizard()).getFile();
    	}
        getWizard().setNextFinishButtonEnabled(false);
        getWizard().setBackButtonEnabled(false);
        getWizard().setCancelButtonEnabled(true);
        
    }
    
    public void aboutToHidePanel() {
        // nothing to do
    }
    
    /** Create substrates **/
    public void displayingPanel() {

    	if (((CSuWizard) this.getWizard()).getMethod()
    			.equals(SimulatorConstants.RANDOM_SUBSTRATE)) {
    		
    		// launch random substrate generation
    		randomSubstrateGeneration();
    	}
    	
    	else if (((CSuWizard) this.getWizard()).getMethod()
    			.equals(SimulatorConstants.IMPORT_SUBSTRATE)) {
    		// launch parse of the imported file
    		importSubstrateGeneration();
    	}
    	
    	// set generated substrates
    	((CSuWizard) getWizard()).setSubstrates(substrates);
    }

	private void randomSubstrateGeneration() {
		Thread t = new Thread() {
			
	        @SuppressWarnings({ "rawtypes", "unchecked" })
			public void run() {
	        	
				int progress = 0;
				int notCreated = 0;
				finishPanel.setProgressMinimum(0);
				finishPanel.setProgressMaximum(numSubstrates);
				finishPanel.setProgressValue(0);
		    	finishPanel.addProgressText(Color.BLACK, "Creating substrates...\n");
		    	
				for (int i=0; i<numSubstrates; i++) {
					Substrate substrate = new Substrate(prefix+i);			
				
					// Random num of nodes inside range (minNodes-maxNodes)
					int numNodes = minNodes + (int)(Math.random()*((maxNodes - minNodes) + 1));
					// Random num of links inside range (minLinks-maxLinks)
					int numLinks = minLinks + (int)(Math.random()*((maxLinks - minLinks) + 1));
					
					
					/** Selecting the random generation algorithm
					 * depending on the link connectivity
					 */
					
					SparseMultigraph<Node, Link> g = null;
					nodeFactory = new SubstrateNodeFactory();
					linkFactory = new SubstrateLinkFactory();
					
					if (linkConnectivity.equals(SimulatorConstants.LINK_PER_NODE_CONNECTIVITY)) {
						//Random Graph Generation
						Factory<Graph<Node, Link>> graphFactory = new Factory<Graph<Node, Link>>() {
							public UndirectedGraph<Node, Link> create() {
								return new NetworkGraph();
							}
						};
						//Barabasi-Albert generation
						BarabasiAlbertGenerator<Node, Link> randomGraph = new BarabasiAlbertGenerator<Node, Link>(graphFactory, nodeFactory, linkFactory, 1, numLinks, new HashSet<Node>());
						randomGraph.evolveGraph(numNodes-1);
						g = (SparseMultigraph<Node, Link>) randomGraph.create();
					}
					else if (linkConnectivity.equals(SimulatorConstants.PERCENTAGE_CONNECTIVITY)) {
						//Random Graph Generation
						Factory<UndirectedGraph<Node, Link>> graphFactory = new Factory<UndirectedGraph<Node, Link>>() {
							public UndirectedGraph<Node, Link> create() {
								return new NetworkGraph();
							}
						};
						//ErdosRenyiGenerator generation
						ErdosRenyiGenerator<Node, Link> randomGraph = new ErdosRenyiGenerator<Node, Link>(graphFactory, nodeFactory, linkFactory, numNodes, linkProbability);
						g = (SparseMultigraph<Node, Link>) randomGraph.create();
						//Remove unconnected nodes
						((NetworkGraph) g).removeUnconnectedNodes();
						//Remove disconnected graphs
						WeakComponentClusterer<Node, Link> wcc = new WeakComponentClusterer<Node, Link>();
						Set<Set<Node>> nodeSets = wcc.transform(g);
						Collection<SparseMultigraph<Node, Link>> gs = FilterUtils.createAllInducedSubgraphs(nodeSets, g); 
						if (gs.size()>1) {
							Iterator itr = gs.iterator();
							g = (NetworkGraph)itr.next();
							while (itr.hasNext()) {
								SparseMultigraph<Node, Link> nextGraph = (SparseMultigraph<Node, Link>) itr.next();
								if (nextGraph.getVertexCount()>g.getVertexCount())
									g = (NetworkGraph)nextGraph;
							}
						}
						// do not add if g is empty
						if (g.getVertexCount()==0) {
							notCreated++;
							progress++;
							finishPanel.setProgressValue(progress);
							finishPanel.addProgressText(Color.RED, substrate.getId()+" not created. " +
									"It has been generated without links\n");
							continue;
						}
						else {
							// Change id of nodes to consecutive int (0,1,2,3...)
							Iterator itr = g.getVertices().iterator();
							int id = 0;
							while (itr.hasNext()) {
								((Node) itr.next()).setId(id);
								id++;
							}
							// refresh nodeFactory's nodeCount
							nodeFactory.setNodeCount(id);
							// Change id of edges to consecutive int (0,1,2,3...)
							itr = g.getEdges().iterator();
							id = 0;
							while (itr.hasNext()) {
								((Link) itr.next()).setId(id);
								id++;
							}
							// refresh linkFactory's linkCount
							linkFactory.setLinkCount(id);
						}
					}
					
					// do not add substrate if topology is wrong
					if (!((NetworkGraph)g).hasCorrectTopology()) {
						notCreated++;
						progress++;
						finishPanel.setProgressValue(progress);
						finishPanel.addProgressText(Color.RED, substrate.getId()+" not created. " +
								"It has been generated with incorrect topology\n");
						continue;
					}
					
					substrate.setGraph(g);
					substrate.setNodeFactory(nodeFactory);
					substrate.setLinkFactory(linkFactory);
				
					// Domain
					// Not defined
					
					substrates.add(substrate);
					progress++;
					finishPanel.setProgressValue(progress);
					finishPanel.addProgressText(Color.BLACK, "Substrate "+substrate.getId()+
    						" successfully created\n");
				}

				finishPanel.addProgressText(Color.BLACK, "Summary:\n");
				finishPanel.addProgressText(Color.BLACK, numSubstrates-notCreated+" substrates successfully created\n");
				finishPanel.addProgressText(Color.BLACK, notCreated+" substrates not created\n");
				
				getWizard().setNextFinishButtonEnabled(true);
				getWizard().setCancelButtonEnabled(false);
	        }
        };

        t.start();
		
	}
	
	private void importSubstrateGeneration() {
		
		Thread t = new Thread() {
			
	        public void run() {
	        	
				numSubstrates = 1;
	        	int progress = 0;
	        	int notCreated = 0;
				finishPanel.setProgressMinimum(0);
				finishPanel.setProgressMaximum(numSubstrates);
				finishPanel.setProgressValue(0);
		    	finishPanel.addProgressText(Color.BLACK, "Creating substrates...\n");
				
		    	for (int i=0; i<numSubstrates; i++) {
		    			
	    			// Substrate id = file name
	    			String fileName = file.getName().split("\\.")[0];
	    			//Create Substrate
	    			Substrate substrate = new Substrate(fileName);
	    			// ID CONTROL: check id availability
	    			if (((CSuWizard) getWizard())
	    					.getSimulator().existSubstrateId(fileName)) {
	    				for (int j = 1; true; j++) {
	    					if (!((CSuWizard) getWizard())
	    							.getSimulator().existSubstrateId(fileName+"("+j+")")) {
	    						substrate.setId(fileName+"("+j+")");
	    						break;
	    					}
	    				}
	    			}
	    			g = new NetworkGraph();
	    			nodeFactory = new SubstrateNodeFactory();
					linkFactory = new SubstrateLinkFactory();
	    			
	    			substrate.setGraph(g);
					substrate.setNodeFactory(nodeFactory);
					substrate.setLinkFactory(linkFactory);
					
					// Parse file
	    			String errorMessage = XMLParser.parseXMLSubstrate(file, substrate);
	    			// Handle errors
	    			if (errorMessage==null) {
	    				// No parse errors
	    				substrates.add(substrate);
	    				finishPanel.addProgressText(Color.BLACK, "Substrate "+substrate.getId()+
	    						" successfully created\n");
	    			}
	    			else {
	    				// Parse errors - Do not add substrate and write error in the wizard
	    				notCreated++;
	    				finishPanel.addProgressText(Color.RED, "Error importing file "+file.getName()+":\n");
	    				finishPanel.addProgressText(Color.RED, errorMessage+"\n");
	    			}
					
					progress++;
					finishPanel.setProgressValue(progress);
				}
		    	
		    	finishPanel.addProgressText(Color.BLACK, "Summary:\n");
				finishPanel.addProgressText(Color.BLACK, numSubstrates-notCreated+" substrates successfully created\n");
				finishPanel.addProgressText(Color.BLACK, notCreated+" substrates not created\n");
				
				getWizard().setNextFinishButtonEnabled(true);
				getWizard().setCancelButtonEnabled(false);
	        }

        };

        t.start();

	}
}

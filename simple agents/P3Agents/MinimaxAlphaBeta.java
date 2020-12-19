package sepia1;


import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import sepia1.GameState.MapLocation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }
    class MapLocation
    {
        public int x, y;
        MapLocation cameFrom;
        float cost;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }
  
    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	List<GameStateChild> children = node.state.getChildren();
    	
    	if(depth == 0)
    		return node;
    	
    	children = orderChildrenWithHeuristics(children);
    		
    	double max = -99999;
    	double min = 99999;
    	
    	for(int i = 0; i < children.size(); i++) {
    		
    		double val = alphaBetaSearch(children.get(i), depth-1, alpha, beta).state.getUtility();
    		
    		if(val > max) 
    			max = val;
    		
    		if(val > alpha) 
    			alpha = val;
    		
    		if(val < min) 
    			min = val;
    		
    		if(val > beta) 
    			beta = val;
    		
    		if(beta <= alpha) 
    			break;
    	}
        return node;  
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
    	List<Double> childUtils = new ArrayList<Double>();
    	List<Double> tempUtils = new ArrayList<Double>();
    	List<GameStateChild> tempChildren = new ArrayList<GameStateChild>();
    	
    	double tempUtil;
    	int tempIndex = 0;
    	int priority = 1;
    	
    	for(int i = 0; i < children.size(); i++) {
    		GameStateChild child = children.get(i);
    		Set<MapLocation> myUnitLocations = new HashSet<MapLocation>();
    		childUtils.add(i, child.state.getUtility());
    	}
    	
    	tempUtils = childUtils;
    	Collections.sort(childUtils);
    	
    	for(int j = 0; j < children.size(); j++) {
    		
    		//Utility value to be found in unsorted array
    		tempUtil = childUtils.get(j);
    		
    		for(int k = 0; k < children.size(); k++) {
    			
    			//If the utility value is the same as this value in the unsorted array, store the index
    			if(tempUtil == tempUtils.get(k))
    				tempIndex = k;
    		}
    		
    		//Add the child to the correct position in a temporary list
    		tempChildren.add(j, children.get(tempIndex));
    	}  	
        return tempChildren;
    }
}

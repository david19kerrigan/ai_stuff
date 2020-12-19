package edu.cwru.sepia.agent;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
//import edu.cwru.sepia.agent.AstarAgent.MapLocation;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	
	private State.StateView theState;
	
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
	
	class DistanceToUnit
	{
		public float unit1, unit2;
		
		public DistanceToUnit(float unit1, float unit2)
		{
			this.unit1 = unit1;
			this.unit2 = unit2;
		}
		
		
		
	}
	

	
	//private List<Integer> myUnitIds;
	//private List<Integer> enemyUnitIds;


    /**

     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
	private int xMap;
    private int yMap;
    private Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
    private Set<MapLocation> myUnitLocations = new HashSet<MapLocation>();
    private Set<MapLocation> enemyUnitLocations = new HashSet<MapLocation>();
    private List<DistanceToUnit> distanceList = new ArrayList<DistanceToUnit>();
    private List<Integer> myHealthValues = new ArrayList<Integer>();
    private List<Integer> enemyHealthValues = new ArrayList<Integer>();
    private List<Integer> myUnitIds = new ArrayList<Integer>();
    private List<Integer> enemyUnitIds = new ArrayList<Integer>();
    private Integer closestEnemyId;
    private Boolean obstacles = false;
    
    public GameState(State.StateView state) {
    	xMap = state.getXExtent();
    	yMap = state.getYExtent();
        theState = state;
        
        //gets all resource node locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        for(Integer resourceID : resourceIDs)
        {
        	ResourceNode.ResourceView resource = state.getResourceNode(resourceID);
        	resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }
    
        myUnitIds = state.getUnitIds(0);
        //get Locations of all of my units
        for(Integer unitId: myUnitIds)
        {
            Unit.UnitView unit = state.getUnit(unitId);
            myUnitLocations.add(new MapLocation(unit.getXPosition(), unit.getYPosition(), null, 0));
        }
        
        //get locations of all enemy units
        enemyUnitIds = state.getUnitIds(1);
        System.out.println("enemy unit size " + enemyUnitIds.size());
        for(Integer unitId: enemyUnitIds)
        {
            Unit.UnitView unit = state.getUnit(unitId);
            
            enemyUnitLocations.add(new MapLocation(unit.getXPosition(), unit.getYPosition(), null, 0));
        }
    
        //computes distances between my units and enemy units and adds them to a list
        float minDist = 9999;
        for(Integer unitId: myUnitIds)
        {
            float[] distances = new float[2];
            int i = 0;
            for(Integer enemyId: enemyUnitIds)
            {
                    distances[i] = (distance(unitId, enemyId, state));
                    if(distance(unitId, enemyId, state) < minDist)
                    {
                    	minDist = distance(unitId, enemyId, state);
                    	closestEnemyId = enemyId;
                    }
                    i++;
            }
            distanceList.add(new DistanceToUnit(distances[0], distances[1]));
        }
        
        for(Integer unitId: myUnitIds)
        {
        	Unit.UnitView myUnit = state.getUnit(unitId);
    		Unit.UnitView closestEnemy = state.getUnit(closestEnemyId);
    		int xMin = Math.min(myUnit.getXPosition(), closestEnemy.getXPosition());
    		int xMax = Math.max(myUnit.getXPosition(), closestEnemy.getXPosition());
    		int yMin = Math.min(myUnit.getYPosition(), closestEnemy.getYPosition());
    		int yMax = Math.max(myUnit.getYPosition(), closestEnemy.getYPosition());
    		
        	for(MapLocation loc: resourceLocations)
        	{
        		int xLoc = loc.x;
        		int yLoc = loc.y;
        		for(int i = xMin; i <= xMax; i++)
        		{
        			for(int j = yMin; j <= yMax; j++)
        			{
        				if(xLoc == i && yLoc == j)
        				{
        					obstacles = true;
        				}
        			}
        		}
        		
        	}
        }
        
        
        //computes health values for all units and adds them to relevant list 
        for(Integer unitId: myUnitIds)
        {
            Unit.UnitView unit = state.getUnit(unitId);
            int health = unit.getHP();
            myHealthValues.add(health);
        }
    
        List<Integer> enemyHealthValues = new ArrayList<Integer>();
    
        for(Integer unitId: enemyUnitIds)
        {
            Unit.UnitView unit = state.getUnit(unitId);
            int health = unit.getHP();
            enemyHealthValues.add(health);
            System.out.println("size " + enemyHealthValues.size());
        }
    }
    
    //Returns distance between unit1 and unit2
    public float distance(int unitId1, int unitId2, StateView state)
    {

    	Unit.UnitView unit1 = state.getUnit(unitId1);
    	Unit.UnitView unit2 = state.getUnit(unitId2);
    	
    	double xDistance = Math.abs((unit1.getXPosition() - unit2.getXPosition()));
    	double yDistance = Math.abs((unit1.getYPosition() - unit2.getYPosition()));
    	double xSquare = Math.pow(xDistance, 2);
    	double ySquare = Math.pow(yDistance, 2);
    	
    	float totalDistance =  (float) Math.sqrt(xSquare + ySquare);
    	return totalDistance;
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    private int AstarPathFinder(MapLocation start, MapLocation goal, int xExtent, int yExtent, Set<MapLocation> resourceLocations)
    {
		start = new MapLocation(start.x, start.y, null, Heuristic(start.x, start.y, goal.x, goal.y));
    	Stack<MapLocation> newPath = new Stack<MapLocation>();//stack of MapLocations which will be returned
    	Set<MapLocation> willSearch = new HashSet<>(); //set of MapLocations which will store nodes to be searched
    	Set<MapLocation> ignore = new HashSet<>(); //set of MapLocations which will store nodes to be ignored
    	ignore.add(start); //shouldn't search the start node

    	Object[] resObjArray = resourceLocations.toArray(); //Object array to store instances of the resourceLocation set for easier looping
    	Object[] willSearchArray = new Object[xExtent * yExtent]; //Object array to store instances of the willSearch set for easier looping
    	MapLocation[] neighbors = FindNeighbors(start, goal); //MapLocation array to store neighbors of a node
    	MapLocation temp; //placeholder node
    	MapLocation prevNode = null; //stores the previous node
    	MapLocation resource = null; //stores a resource node
    	MapLocation optimalNode = null; //stores the optimal node to travel to
    	MapLocation current = start; //current node who's neighbors are being investigated (initialized to start)

    	//loop which adds all resourceLocations to the ignore set
    	for(int i = 0; i < resObjArray.length; i++) {
    		resource = (MapLocation) resObjArray[i];
    		resource = new MapLocation(resource.x, resource.y, null, Heuristic(resource.x, resource.y, goal.x, goal.y));
    		ignore.add(resource);
    	}
    	
    	//adds the neighbors of the starting point to the nodes to be ignore
    	for(int i = 0; i < 8; i++)
    		if(!(neighbors[i].x < 0 || neighbors[i].y < 0 || neighbors[i].x > xExtent || neighbors[i].y > yExtent || HasLoc(neighbors[i], ignore)))
    			willSearch.add(neighbors[i]);

    	while(Math.abs(current.x - goal.x) > 1 || Math.abs(current.y - goal.y) > 1) {
    		willSearchArray = willSearch.toArray(); //makes an array of MapLocation objects to be ignore
    		float maxF = 99999999; //heuristic to be compared to (set way too high so the first will always be smaller)
    		
    		//loop which will get the optimal node to travel to from the nodes to be searched
    		for(int i = 0; i < willSearchArray.length; i++) {
    			temp = (MapLocation) willSearchArray[i]; //stores individual MapLocations from the willSearch set

    			//if the heuristic of this neighbor is the smallest, that is the optimalNode
    			if(temp.cost < maxF) {
    				maxF = temp.cost;
    				optimalNode = temp;
    			}
    		}

    		//ignores nodes which aren't the  optimal node
    		for(int i = 0; i < willSearchArray.length; i++) {
    			temp = (MapLocation) willSearchArray[i];  //stores individual MapLocations from the willSearch set

    			if(!(temp == optimalNode)) {
    				willSearch.remove(temp);
    				ignore.add(temp);
    			}
    		}

    		//if already on the next node, or just moved from the next node -- restart while blocking off this path
    		if(current == optimalNode || optimalNode == prevNode) {
    			System.out.println("Restarting...");
    			resourceLocations.add(current);
    			resourceLocations.add(optimalNode);
    			
    		}


    		//Move to the Next Node --


    		prevNode = current; //sets previous node to the current node
    		current = optimalNode; //travels to the optimalNode
    		newPath.push(new MapLocation(current.x, current.y, null, 0)); //adds the node traveled to to the path stack
    		willSearch.remove(current); //removes current node from nodes to be ignore
    		ignore.add(current); //adds current node to ignore nodes
    		neighbors = FindNeighbors(current, goal); //finds neighbors of this node

    		//loop which adds which neighbors are to be ignored to the ignored set
    		for(int i = 0; i < 8; i++)
    			if(!(HasLoc(neighbors[i], ignore) || neighbors[i].x < 0
    					|| neighbors[i].y < 0 || neighbors[i].x > xExtent || neighbors[i].y > yExtent))
    				willSearch.add(neighbors[i]);

    		System.out.println("Added to path: " + current.x + " " + current.y + " " + !((Math.abs(current.x - goal.x) == 1) && (Math.abs(current.y - goal.y) == 1)));
    		
    	}

    	//reverse the discovered path from the stack so it is in the correct order
    	int sizeOfPath = newPath.size();
    	MapLocation[] reversePath = new MapLocation[sizeOfPath];
    	for(int i = 0; i < sizeOfPath; i++) {
    		reversePath[i] = newPath.pop();
    	}
    	for(int j = 0; j < sizeOfPath; j++) {
    		newPath.push(reversePath[j]);
    	}
        // return the created path
        return newPath.size();
    }

	//this function finds if a set of MapLocations contains a certain MapLocation
	private boolean HasLoc(MapLocation current, Set<MapLocation> locations) {

		Object[] locArray = locations.toArray();
		MapLocation loc;
		boolean hasLoc = false;

		for(int i = 0; i < locArray.length; i++) {
			loc = (MapLocation) locArray[i];

			if(loc.x == current.x && loc.y == current.y && loc.cameFrom == current.cameFrom && loc.cost == current.cost)
				return true;
		}

		return hasLoc;
	}

	//this function finds the eight neighbors of a given MapLocation, and returns them as an array of MapLocations
    private MapLocation[] FindNeighbors(MapLocation currentLoc, MapLocation goal) {
    	MapLocation[] neighbors = new MapLocation[8];
    	neighbors[0] = new MapLocation(currentLoc.x + 1, currentLoc.y, null, Heuristic(currentLoc.x + 1, currentLoc.y, goal.x, goal.y));
    	neighbors[1] = new MapLocation(currentLoc.x - 1, currentLoc.y, null, Heuristic(currentLoc.x - 1, currentLoc.y, goal.x, goal.y));
    	neighbors[2] = new MapLocation(currentLoc.x, currentLoc.y + 1, null, Heuristic(currentLoc.x, currentLoc.y + 1, goal.x, goal.y));
    	neighbors[3] = new MapLocation(currentLoc.x, currentLoc.y - 1, null, Heuristic(currentLoc.x, currentLoc.y - 1, goal.x, goal.y));
    	neighbors[4] = new MapLocation(currentLoc.x + 1, currentLoc.y + 1, null, Heuristic(currentLoc.x + 1, currentLoc.y + 1, goal.x, goal.y));
    	neighbors[5] = new MapLocation(currentLoc.x - 1, currentLoc.y + 1, null, Heuristic(currentLoc.x - 1, currentLoc.y + 1, goal.x, goal.y));
    	neighbors[6] = new MapLocation(currentLoc.x + 1, currentLoc.y - 1, null, Heuristic(currentLoc.x + 1, currentLoc.y - 1, goal.x, goal.y));
    	neighbors[7] = new MapLocation(currentLoc.x - 1, currentLoc.y - 1, null, Heuristic(currentLoc.x - 1, currentLoc.y - 1, goal.x, goal.y));
    	return neighbors;
    	
    }
    
    private float Heuristic(int x1, int y1, int x2, int y2) {
    	return (float) Math.sqrt((double) ((Math.abs(x2 - x1) * Math.abs(x2 - x1)) + (Math.abs(y2 - y1) * Math.abs(y2 - y1))));
    }
    
    //Determines the utility of a state based off of locations of both friendly and enemy units,
    //as well as enemy and friendly health values
    public double getUtility() {
    	double util = 0;
        
        for(int i = 0; i < myHealthValues.size(); i++)
                util += 0.5 * myHealthValues.get(i);
        
        for(int j = 0; j < enemyHealthValues.size(); j++) {
        		System.out.println("enemy health length "+enemyHealthValues.size());
                util += -0.5 * enemyHealthValues.get(j);
        }
        
        if(obstacles == true)
        {	
        	for(MapLocation me: myUnitLocations)
        	{
        		for(MapLocation enemy: enemyUnitLocations)
        		{		
        		int pathSize = AstarPathFinder(me, enemy, xMap, yMap, resourceLocations );
        		util += -0.5 * pathSize;
        		}
        	}
        }
        else{
        	for(int i = 0; i < myUnitLocations.size(); i++) {
                util += -0.5 * distanceList.get(i).unit1;
                util += -0.5 * distanceList.get(i).unit2;        
        	}	
        }
        return util;
    }


    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	List<GameStateChild> result = new ArrayList<GameStateChild>();
    	
    	//finds all possible movements
    	for(Direction direction : Direction.values()) {
    		Action action1 = Action.createPrimitiveMove(myUnitIds.get(0), direction);
    		
    		for(Direction direction2 : Direction.values()) {
    			Action action2 = Action.createPrimitiveMove(myUnitIds.get(1), direction2);
    			Map<Integer, Action> actions = new HashMap<Integer, Action>();
    			
    			actions.put(myUnitIds.get(0), action1);
    			actions.put(myUnitIds.get(1), action2);
    			result.add(new GameStateChild(actions, this));
    		}
    		
    		for(Integer enemyId : enemyUnitIds) {
    			if(distance((int)myUnitIds.get(1), (int)enemyId, theState) <= 1) {
    				
    				Map<Integer, Action> actions = new HashMap<Integer, Action>();
    				Action attack2 = Action.createPrimitiveAttack(myUnitIds.get(1), enemyId);
    				
    				actions.put(myUnitIds.get(0), action1);
        			actions.put(myUnitIds.get(1), attack2);
        			result.add(new GameStateChild(actions, this));
    			}
    		}
    	}
    	
    	for(Integer enemyId : enemyUnitIds) {
			if(distance((int)myUnitIds.get(0), (int)enemyId, theState) <= 1) {

				for(Direction direction2 : Direction.values()) {
					Map<Integer, Action> actions = new HashMap<Integer, Action>();
					Action attack1 = Action.createPrimitiveAttack(myUnitIds.get(0), enemyId);
	    			Action action2 = Action.createPrimitiveMove(myUnitIds.get(1), direction2);
	    			
	    			actions.put(myUnitIds.get(0), attack1);
	    			actions.put(myUnitIds.get(1), action2);
	    			result.add(new GameStateChild(actions, this));
	    		}
	    		
	    		for(Integer enemyId1 : enemyUnitIds) {
	    			if(distance((int)myUnitIds.get(1), (int)enemyId1, theState) <= 1) {
	    				Map<Integer, Action> actions = new HashMap<Integer, Action>();
	    				Action attack1 = Action.createPrimitiveAttack(myUnitIds.get(0), enemyId);
	    				Action attack2 = Action.createPrimitiveAttack(myUnitIds.get(1), enemyId1);
	    				
	    				actions.put(myUnitIds.get(0), attack1);
	        			actions.put(myUnitIds.get(1), attack2);
	        			result.add(new GameStateChild(actions, this));
	    			}
	    		}
			}
		}
        return result;
    }
}


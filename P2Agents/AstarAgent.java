package sepia;

import java.lang.Math;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

	//Edited to add utilization of cameFrom and cost
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

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;
        System.out.println("totalPlanTime: " + totalPlanTime);

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        
        if(shouldReplanPath(newstate, statehistory, path)) {
      
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
            System.out.println("totalPlanTime: " + totalPlanTime);
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();


        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // start moving to the next step in the path
            nextLoc = path.pop();
            
            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * You can check the position of the enemy footman with the following code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     *
     * There are more examples of getting the positions of objects in SEPIA in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	//find where the enemy footman is
    	MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }
        
        
        //if the footman is where I want to go I must replan
        if(footmanLoc != null)
        {
        	if(footmanLoc.x == currentPath.peek().x && footmanLoc.y == currentPath.peek().y) {
        		System.out.println("Time to replan!");
        		return true;
        	}
        }
        
        
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * Therefore your you need to find some possible adjacent steps which are in range
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @param MapLocation
     * @return Stack of positions with top of stack being first move in plan
     */

	private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
		//xExtent = 16;
		//yExtent = 16;
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
    	
    	//store the enemy footman's location in the ignore set with the heuristic info as well
    	if(enemyFootmanLoc != null)
    	{
    		enemyFootmanLoc = new MapLocation(enemyFootmanLoc.x, enemyFootmanLoc.y, null, Heuristic(enemyFootmanLoc.x, enemyFootmanLoc.y, goal.x, goal.y));
    		ignore.add(enemyFootmanLoc);
    	}
    	
    	//adds the neighbors of the starting point to the nodes to be searched
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
    			return AstarSearch(start, goal, xExtent, yExtent, enemyFootmanLoc, resourceLocations);
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
        return newPath;
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

    //the heuristic function
    private float Heuristic(int x1, int y1, int x2, int y2) {
    	return (float) Math.sqrt((double) ((Math.abs(x2 - x1) * Math.abs(x2 - x1)) + (Math.abs(y2 - y1) * Math.abs(y2 - y1))));
    }
    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}

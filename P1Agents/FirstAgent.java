package sepia1;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class FirstAgent extends Agent {

	public FirstAgent(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<Integer, Action> initialStep(StateView arg0, HistoryView arg1) {
		
		return middleStep(arg0, arg1);
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Integer, Action> middleStep(StateView arg0, HistoryView arg1) {
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		List<Integer> myUnitIDs = arg0.getUnitIds(playernum);
		List<Integer> peasantIds = new ArrayList<Integer>();
        List<Integer> townhallIds = new ArrayList<Integer>();
        List<Integer> farmIds = new ArrayList<Integer>();
        List<Integer> barracksIds = new ArrayList<Integer>();
        List<Integer> footmenIds = new ArrayList<Integer>();
        
        for(Integer unitID : myUnitIDs)
        {
            
                UnitView unit = arg0.getUnit(unitID);
                String unitTypeName = unit.getTemplateView().getName();

                if(unitTypeName.equals("TownHall"))
                        townhallIds.add(unitID);
                else if(unitTypeName.equals("Peasant"))
                        peasantIds.add(unitID);
                else if(unitTypeName.contentEquals("Farm"))
                		farmIds.add(unitID);
                else if(unitTypeName.contentEquals("Barracks"))
                	barracksIds.add(unitID);
                else if(unitTypeName.contentEquals("Footman"))
            		footmenIds.add(unitID);
                else
                        System.err.println("Unexpected Unit type: " + unitTypeName);
        }
        int currentGold = arg0.getResourceAmount(playernum, ResourceType.GOLD);
        int currentWood = arg0.getResourceAmount(playernum, ResourceType.WOOD);

        List<Integer> goldMines = arg0.getResourceNodeIds(Type.GOLD_MINE);
        List<Integer> trees = arg0.getResourceNodeIds(Type.TREE);
        
        for(Integer peasantID : peasantIds)
        {
                Action action = null;
                if(arg0.getUnit(peasantID).getCargoAmount() > 0)
                {
                        
                        action = new TargetedAction(peasantID, ActionType.COMPOUNDDEPOSIT, townhallIds.get(0));
                }
                else
                {
                        if(currentGold < currentWood)
                        {
                                action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, goldMines.get(0));
                        }
                        else
                        {
                                action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, trees.get(0));
                        }
                }
                actions.put(peasantID, action);
        }
        
        if(peasantIds.size() < 3)
        {
                if(currentGold >= 400)
                {
                        TemplateView peasantTemplate = arg0.getTemplate(playernum, "Peasant");
                        int peasantTemplateID = peasantTemplate.getID();
                        int townhallID = townhallIds.get(0);
                        actions.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
                }
        }
       if(farmIds.size()<1)
       {
    	   		if(currentGold >= 500 && currentWood >= 250)
        		{
        				TemplateView farmTemplate = arg0.getTemplate(playernum, "Farm");
        				int farmTemplateID = farmTemplate.getID();
        				int peasantID = peasantIds.get(0);
        				actions.put(peasantID, Action.createCompoundBuild(peasantID, farmTemplateID, 8, 12));
        		}
       }
       if(barracksIds.size()<1)
       {
    	   		if(currentGold >= 700 && currentWood >= 400)
        		{
        				TemplateView barracksTemplate = arg0.getTemplate(playernum, "Barracks");
        				int barracksTemplateID = barracksTemplate.getID();
        				int peasantID = peasantIds.get(0);
        				actions.put(peasantID, Action.createCompoundBuild(peasantID, barracksTemplateID, 10, 12));
        		}
       }
       if(footmenIds.size()<2 && barracksIds.size()>0)
       {
    	   		if(currentGold >= 600)
        		{
        				TemplateView footmanTemplate = arg0.getTemplate(playernum, "Footman");
        				int footmanTemplateID = footmanTemplate.getID();
        				int barracksID = barracksIds.get(0);
        				actions.put(barracksID, Action.createCompoundProduction(barracksID, footmanTemplateID));
        		}
       }
        return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
		// TODO Auto-generated method stub

	}

}

package sepia;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;


public class MyCombatAgent extends Agent {
	private int enemyPlayerNum = 1;

	public MyCombatAgent(int playernum, String[] otherargs) {
        super(playernum);

        if(otherargs.length > 0)
        {
                enemyPlayerNum = new Integer(otherargs[0]);
        }

        System.out.println("Constructed MyCombatAgent");
}

	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
		// This stores the action that each unit will perform
        // if there are no changes to the current actions then this
        // map will be empty
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // This is a list of all of your units
        // Refer to the resource agent example for ways of
        // differentiating between different unit types based on
        // the list of IDs
        List<Integer> myUnitIDs = newstate.getUnitIds(playernum);

        List<Integer> enemyUnitIds = newstate.getUnitIds(enemyPlayerNum);
        List<Integer> enemyFootmen = new ArrayList<Integer>();
        List<Integer> enemyTowers = new ArrayList<Integer>();
        
        for(Integer unitID : enemyUnitIds)
        {
            
                UnitView unit = newstate.getUnit(unitID);
                String unitTypeName = unit.getTemplateView().getName();

                if(unitTypeName.equals("Footman"))
                        enemyFootmen.add(unitID);
                else if(unitTypeName.contentEquals("ScoutTower"))
                		enemyTowers.add(unitID);
                else
                        System.err.println("Unexpected Unit type: " + unitTypeName);
        }
        

        if(enemyUnitIds.size() == 0)
        {
                return actions;
        }
 
        for(Integer myUnitID : myUnitIDs)
        {
        	UnitView unitView = newstate.getUnit(myUnitID);
        	String unitTypeName = unitView.getTemplateView().getName();
        	int xpos = unitView.getXPosition();
        	int ypos = unitView.getYPosition();
        	int footmanID = myUnitIDs.get(0);
        	if(unitTypeName.contentEquals("Footman")) {
        		actions.put(myUnitID, Action.createCompoundMove(footmanID, xpos+6, ypos-3));
        	}	
        }
        return actions;
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
		
		
		 Map<Integer, Action> actions = new HashMap<Integer, Action>();
		 List<Integer> myUnitIDs = newstate.getUnitIds(playernum);
         // This is a list of enemy units
		 List<Integer> enemyUnitIds = newstate.getUnitIds(enemyPlayerNum);
	     List<Integer> enemyFootmen = new ArrayList<Integer>();
	     List<Integer> enemyTowers = new ArrayList<Integer>();
	        
	        for(Integer unitID : enemyUnitIds)
	        {
	            
	                UnitView unit = newstate.getUnit(unitID);
	                String unitTypeName = unit.getTemplateView().getName();

	                if(unitTypeName.equals("Footman"))
	                        enemyFootmen.add(unitID);
	                else if(unitTypeName.contentEquals("ScoutTower"))
	                		enemyTowers.add(unitID);
	                else
	                        System.err.println("Unexpected Unit type: " + unitTypeName);
	        }


         if(enemyUnitIds.size() == 0)
         {
                 // Nothing to do because there is no one left to attack
                 return actions;
         }
         
         int currentStep = newstate.getTurnNumber();

         // go through the action history
         for(ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep-1).values())
         {
                 // if the previous action is no longer in progress (either due to failure or completion)
                 // then add a new action for this unit
                 if(feedback.getFeedback() != ActionFeedback.INCOMPLETE)
                 {
                         // attack the first enemy unit in the list
                         int unitID = feedback.getAction().getUnitId();
                         int i = 1;
                         for(Integer myUnitID : myUnitIDs) {
                        	 UnitView unitView = newstate.getUnit(myUnitID);
                         	 String unitTypeName = unitView.getTemplateView().getName();
                         	 int xpos = unitView.getXPosition();
                         	 int ypos = unitView.getYPosition();
                        	 int footmanID = myUnitIDs.get(0);
                        	 if(myUnitID != footmanID) {
                        		 actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyUnitIds.get(i)));	
                        	 }
                         }   
                 }      
         }
         for(ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep-1).values())
         {
                 // if the previous action is no longer in progress (either due to failure or completion)
                 // then add a new action for this unit
                 if(feedback.getFeedback() != ActionFeedback.INCOMPLETE)
                 {
                         // attack the first enemy unit in the list
                         int unitID = feedback.getAction().getUnitId();
                         int i = 1;
                         for(Integer myUnitID : myUnitIDs) {
                        	 int footmanID = myUnitIDs.get(0);
                        	 if(myUnitID == footmanID) {
                        		 actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyUnitIds.get(i)));	 
                        	 }
                         }      
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

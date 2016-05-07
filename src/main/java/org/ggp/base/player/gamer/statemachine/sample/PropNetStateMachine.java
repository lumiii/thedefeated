package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class PropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    //private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
//            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean clearPropNet()
    {

    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	for(Entry<GdlSentence, Proposition> val : props.entrySet())
    	{
    		if(props.containsKey(val.getKey()))
    		{
    			props.get(val.getKey()).setValue(false);
    		}
    	}
    	return true;
    }

    private boolean propMarkP(Proposition p) throws Exception
    {
    	if(propNet.getBasePropositions().containsKey(p.getName()))
    	{
    		return p.getValue();
    	}
    	else if(propNet.getInputPropositions().containsKey(p.getName()))
    	{
    		return p.getValue();
    	}
    	else
    	{
    		Component input = p.getSingleInput();
    		return input.getValue();
    	}

		//throw new Exception("Undefined type");
    }



    private boolean markBases(Map<GdlSentence, Boolean> vals)
    {

    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	for(Map.Entry<GdlSentence, Boolean> val : vals.entrySet())
    	{
    		if(props.containsKey(val.getKey()))
    		{
    			props.get(val.getKey()).setValue(val.getValue());
    		}
    	}
    	return true;
    }

    private void markMoves(List<Move> moves)
    {
    	Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
    	for (Move move : moves)
    	{
    		GdlSentence sentence = move.getContents().toSentence();
    		Proposition prop = props.get(sentence);
    		prop.setValue(true);
    	}
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        // TODO: Compute whether the MachineState is terminal.
    	Set<GdlSentence> contents = state.getContents();
    	return contents.contains(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
        // TODO: Compute the goal for role in state.
        Set<Proposition> goals = propNet.getGoalPropositions().get(role);
        for(Proposition goal : goals)
        {
        	if(state.getContents().contains(goal.getName()))
        	{
        		List<GdlTerm> body = goal.getName().getBody();
        		return Integer.parseInt(body.get(body.size()-1).toString());
        	}
        }
        throw new GoalDefinitionException(state, role);
        /**
        Set<GdlSentence> contents = state.getContents();
        Set<GdlSentence> tempSet = new HashSet<GdlSentence>(contents);

        Map<GdlSentence, Proposition> goalMap = goalMap(goals);

        tempSet.retainAll(goalMap.keySet());

        if (tempSet.size() == 1)
        {
        	return goalMap.get(tempSet.iterator().next()).getValue();
        }
        **/
    }

    private Map<GdlSentence, Proposition> goalMap(Set<Proposition> goalPropositions)
    {
    	HashMap<GdlSentence, Proposition> sentenceMap = new HashMap<GdlSentence, Proposition>();
    	for (Proposition p : goalPropositions)
    	{
    		sentenceMap.put(p.getName(), p);
    	}

    	return sentenceMap;
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
        // TODO: Compute the initial state.
    	propNet.getInitProposition().setValue(true);
        return new MachineState(propNet.getBasePropositions().keySet());
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        return null;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        return null;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {

    	markMoves(moves);


        return null;
    }

    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */


    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }
}
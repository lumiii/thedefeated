package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class AugmentedStateMachine extends StateMachine
{
	public int getGoal(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return getGoal(state, role);
    	}

    	return getGoalSub(state, role, subgame);
    }

	public boolean isTerminal(MachineState state, Subgame subgame)
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return isTerminal(state);
    	}

    	return isTerminalSub(state, subgame);
    }

	public List<Move> getLegalMoves(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return getLegalMoves(state, role);
    	}

    	return getLegalMovesSub(state, role, subgame);
    }

	public List<Move> getLegalMovesComplement(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		throw new IllegalStateException("Function undefined when not factoring");
    	}

    	return getLegalMovesComplementSub(state, role, subgame);
    }

	public MachineState getNextState(MachineState state, List<Move> moves, Subgame subgame) throws TransitionDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return getNextState(state, moves);
    	}

    	return getNextStateSub(state, moves, subgame);
    }

	public List<Move> getRandomJointMove(MachineState state, Subgame subgame) throws MoveDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return getRandomJointMove(state);
    	}

        List<Move> random = new ArrayList<Move>();
        for (Role role : getRoles()) {
            random.add(getRandomMove(state, role, subgame));
        }

        return random;
    }

	public Move getRandomMove(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
    {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return getRandomMove(state, role);
    	}

        List<Move> legals = getLegalMoves(state, role, subgame);
        return legals.get(new Random().nextInt(legals.size()));
    }

	public MachineState performDepthCharge(MachineState state, int depthBound, Subgame subgame, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
    	if (!RuntimeParameters.FACTOR_SUBGAME)
    	{
    		return performDepthCharge(state, theDepth);
    	}

        int nDepth = 0;

        while(!isTerminal(state, subgame) && nDepth <= depthBound) {
            nDepth++;

            state = getNextStateDestructively(state, getRandomJointMove(state));
        }

        if(theDepth != null)
            theDepth[0] = nDepth;
        return state;
    }

    public abstract Set<Subgame> getSubgames();
    public abstract void findLatches(Role role, int minGoal);
    // these can just be overloaded to have the subgame parameter
    // instead of having new names, but that detracts from the readability of the intentions
    protected abstract int getGoalSub(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException;
    protected abstract boolean isTerminalSub(MachineState state, Subgame subgame);
    protected abstract List<Move> getLegalMovesSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException;
    protected abstract List<Move> getLegalMovesComplementSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException;
    protected abstract MachineState getNextStateSub(MachineState state, List<Move> moves, Subgame subgame) throws TransitionDefinitionException;
}

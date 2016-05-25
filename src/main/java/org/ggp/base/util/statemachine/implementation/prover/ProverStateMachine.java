package org.ggp.base.util.statemachine.implementation.prover;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.player.gamer.statemachine.thedefeated.Subgame;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.implementation.prover.result.ProverResultParser;

import com.google.common.collect.ImmutableList;


public class ProverStateMachine extends StateMachine
{
    private MachineState initialState;
    private Prover prover;
    private ImmutableList<Role> roles;

    /**
     * Initialize must be called before using the StateMachine
     */
    public ProverStateMachine()
    {

    }

    @Override
    public void initialize(List<Gdl> description)
    {
        prover = new AimaProver(description);
        roles = ImmutableList.copyOf(Role.computeRoles(description));
        initialState = computeInitialState();
    }

    private MachineState computeInitialState()
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getInitQuery(), new HashSet<GdlSentence>());
        return new ProverResultParser().toState(results);
    }

    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getGoalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() != 1)
        {
            GamerLogger.logError("StateMachine", "Got goal results of size: " + results.size() + " when expecting size one.");
            throw new GoalDefinitionException(state, role);
        }

        try
        {
            GdlRelation relation = (GdlRelation) results.iterator().next();
            GdlConstant constant = (GdlConstant) relation.get(1);

            return Integer.parseInt(constant.toString());
        }
        catch (Exception e)
        {
            throw new GoalDefinitionException(state, role);
        }
    }

    @Override
    public MachineState getInitialState()
    {
        return initialState;
    }

    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException
    {
    	Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getInputQuery(role), ProverQueryBuilder.getContext(initialState));

        if (results.size() == 0)
        {
            throw new MoveDefinitionException(initialState, role);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getLegalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() == 0)
        {
            throw new MoveDefinitionException(state, role);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getNextQuery(), ProverQueryBuilder.getContext(state, getRoles(), moves));

        for (GdlSentence sentence : results)
        {
            if (!sentence.isGround())
            {
                throw new TransitionDefinitionException(state, moves);
            }
        }

        return new ProverResultParser().toState(results);
    }

    @Override
    public List<Role> getRoles()
    {
        return roles;
    }

    @Override
    public boolean isTerminal(MachineState state)
    {
        return prover.prove(ProverQueryBuilder.getTerminalQuery(), ProverQueryBuilder.getContext(state));
    }

	@Override
	public Set<Subgame> getSubgames()
	{
		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public int getGoalSub(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException
	{
		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public boolean isTerminalSub(MachineState state, Subgame subgame)
	{
		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public List<Move> getLegalMovesSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
	{
		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public MachineState getNextStateSub(MachineState state, List<Move> moves, Subgame subgame)
			throws TransitionDefinitionException
	{
		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public boolean canPlaySubgames()
	{
		return false;
	}

	@Override
	public List<Move> getLegalMovesComplementSub(MachineState state, Role role, Subgame subgame)
			throws MoveDefinitionException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Proposition> findBaseInhibitors(Role role)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Proposition, Boolean> getLatchInhibitors(List<Proposition> inhibitors)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
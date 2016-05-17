package org.ggp.base.util.statemachine.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.player.gamer.statemachine.thedefeated.Subgame;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import com.google.common.collect.ImmutableList;

public final class CachedStateMachine extends StateMachine
{
    private final StateMachine backingStateMachine;
    private final TtlCache<MachineState, Entry> ttlCache;

    private final class Entry
    {
        public Map<Role, Integer> goals;
        public Map<Role, List<Move>> actions;
        public Map<Role, List<Move>> moves;
        public Map<List<Move>, MachineState> nexts;
        public Boolean terminal;

        public Entry()
        {
            goals = new HashMap<Role, Integer>();
            actions = new HashMap<Role, List<Move>>();
            moves = new HashMap<Role, List<Move>>();
            nexts = new HashMap<List<Move>, MachineState>();
            terminal = null;
        }
    }

    public CachedStateMachine(StateMachine backingStateMachine)
    {
        this.backingStateMachine = backingStateMachine;
        ttlCache = new TtlCache<MachineState, Entry>(1);
    }

    private Entry getEntry(MachineState state)
    {
        if (!ttlCache.containsKey(state))
        {
            ttlCache.put(state, new Entry());
        }

        return ttlCache.get(state);
    }

    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (!entry.goals.containsKey(role))
            {
                entry.goals.put(role, backingStateMachine.getGoal(state, role));
            }

            return entry.goals.get(role);
        }
    }

    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException
    {
    	MachineState initialState = backingStateMachine.getInitialState();
        Entry entry = getEntry(initialState);
        synchronized (entry)
        {
            if (!entry.actions.containsKey(role))
            {
                entry.actions.put(role, ImmutableList.copyOf(backingStateMachine.findActions(role)));
            }

            return entry.actions.get(role);
        }
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (!entry.moves.containsKey(role))
            {
                entry.moves.put(role, ImmutableList.copyOf(backingStateMachine.getLegalMoves(state, role)));
            }

            return entry.moves.get(role);
        }
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (!entry.nexts.containsKey(moves))
            {
                entry.nexts.put(moves, backingStateMachine.getNextState(state, moves));
            }

            return entry.nexts.get(moves);
        }
    }

    @Override
    public boolean isTerminal(MachineState state)
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (entry.terminal == null)
            {
                entry.terminal = backingStateMachine.isTerminal(state);
            }

            return entry.terminal;
        }
    }

    @Override
    public void doPerMoveWork()
    {
        prune();
    }

    public void prune()
    {
        ttlCache.prune();
    }

    @Override
    public void initialize(List<Gdl> description) {
        backingStateMachine.initialize(description);
    }

    @Override
    public List<Role> getRoles() {
        // TODO(schreib): Should this be cached as well?
        return backingStateMachine.getRoles();
    }

    @Override
    public MachineState getInitialState() {
        // TODO(schreib): Should this be cached as well?
        return backingStateMachine.getInitialState();
    }

    // TODO: these all need to be cached! figure out how this class works
	@Override
	public Set<Subgame> getSubgames()
	{
		return backingStateMachine.getSubgames();
	}

	@Override
	public int getGoalSub(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException
	{
		return backingStateMachine.getGoalSub(state, role, subgame);
	}

	@Override
	public boolean isTerminalSub(MachineState state, Subgame subgame)
	{
		return backingStateMachine.isTerminalSub(state, subgame);
	}

	@Override
	public List<Move> getLegalMovesSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
	{
		return backingStateMachine.getLegalMovesSub(state, role, subgame);
	}

	@Override
	public MachineState getNextStateSub(MachineState state, List<Move> moves, Subgame subgame)
			throws TransitionDefinitionException
	{
		return backingStateMachine.getNextStateSub(state, moves, subgame);
	}

	@Override
	public boolean canPlaySubgames()
	{
		return backingStateMachine.canPlaySubgames();
	}
}
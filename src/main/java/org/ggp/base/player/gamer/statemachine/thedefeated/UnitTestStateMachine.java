package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class UnitTestStateMachine extends StateMachine
{
	private static final Logger log = GLog.getLogger(UnitTestStateMachine.class);
	StateMachine testMachine;
	StateMachine referenceMachine;

	public UnitTestStateMachine(StateMachine testMachine, StateMachine referenceMachine)
	{
		this.testMachine = testMachine;
		this.referenceMachine = referenceMachine;
	}

	@Override
	public List<Move> findActions(Role role) throws MoveDefinitionException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(List<Gdl> description)
	{
		testMachine.initialize(description);
		referenceMachine.initialize(description);
	}

	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException
	{
		int reference = referenceMachine.getGoal(state, role);
		try
		{
			int test = testMachine.getGoal(state, role);

			if (checkResults(new Integer(test), new Integer(reference)))
			{
				return test;
			}
		}
		catch (GoalDefinitionException e)
		{
			checkException(e);
		}

		return -1;
	}

	@Override
	public boolean isTerminal(MachineState state)
	{
		boolean reference = referenceMachine.isTerminal(state);
		boolean test = testMachine.isTerminal(state);

		if (checkResults(new Boolean(test), new Boolean(reference)))
		{
			return test;
		}

		return false;
	}

	@Override
	public List<Role> getRoles()
	{
		List<Role> reference = referenceMachine.getRoles();
		List<Role> test = testMachine.getRoles();

		if (checkResults(test, reference))
		{
			return test;
		}

		return null;
	}

	@Override
	public MachineState getInitialState()
	{
		MachineState reference = referenceMachine.getInitialState();
		MachineState test = testMachine.getInitialState();

		if (checkResults(test, reference))
		{
			return test;
		}

		return null;
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
	{
		List<Move> reference = referenceMachine.getLegalMoves(state, role);
		try
		{
			List<Move> test = testMachine.getLegalMoves(state, role);

			if (checkResults(test, reference))
			{
				return test;
			}
		}
		catch (MoveDefinitionException e)
		{
			checkException(e);
		}

		return null;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		MachineState reference = referenceMachine.getNextState(state, moves);
		try
		{
			MachineState test = testMachine.getNextState(state, moves);

			if (checkResults(test, reference))
			{
				return test;
			}
		}
		catch (TransitionDefinitionException e)
		{
			checkException(e);
		}

		return null;
	}

	private boolean checkResults(Object test, Object reference)
	{
		boolean match;
		if (test instanceof List<?> && reference instanceof List<?>)
		{
			List<?> testList = (List<?>) test;
			List<?> referenceList = (List<?>) reference;
			match = testList.containsAll(referenceList);
		}
		else
		{
			match = test.equals(reference);
		}

		if (!match)
		{
			StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
			StackTraceElement e = stacktrace[2];// maybe this number needs to be
												// corrected
			log.error(GLog.UNITTEST, "Mismatch: " + e.getMethodName());
			log.error(GLog.UNITTEST, "Test:\n" + test);
			log.error(GLog.UNITTEST, "Reference:\n" + reference);

			return false;
		}

		return true;
	}

	private void checkException(Exception e)
	{
		log.catching(e);
	}

	@Override
	public Set<Subgame> getSubgames()
	{
		if (testMachine.canPlaySubgames())
		{
			if (referenceMachine.canPlaySubgames())
			{
				Set<Subgame> reference = referenceMachine.getSubgames();
				Set<Subgame> test = testMachine.getSubgames();

				if (checkResults(test, reference))
				{
					return test;
				}

				return null;
			}

			// else
			return testMachine.getSubgames();
		}

		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public int getGoalSub(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException
	{
		if (testMachine.canPlaySubgames())
		{
			if (referenceMachine.canPlaySubgames())
			{
				int reference = referenceMachine.getGoalSub(state, role, subgame);
				int test = testMachine.getGoalSub(state, role, subgame);

				if (checkResults(test, reference))
				{
					return test;
				}

				return 0;
			}

			// else
			return testMachine.getGoalSub(state, role, subgame);
		}

		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public boolean isTerminalSub(MachineState state, Subgame subgame)
	{
		if (testMachine.canPlaySubgames())
		{
			if (referenceMachine.canPlaySubgames())
			{
				boolean reference = referenceMachine.isTerminalSub(state, subgame);
				boolean test = testMachine.isTerminalSub(state, subgame);

				if (checkResults(test, reference))
				{
					return test;
				}

				return false;
			}

			// else
			return testMachine.isTerminalSub(state, subgame);
		}

		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public List<Move> getLegalMovesSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
	{
		if (testMachine.canPlaySubgames())
		{
			if (referenceMachine.canPlaySubgames())
			{
				List<Move> reference = referenceMachine.getLegalMovesSub(state, role, subgame);
				List<Move> test = testMachine.getLegalMovesSub(state, role, subgame);

				if (checkResults(test, reference))
				{
					return test;
				}

				return null;
			}

			// else
			return testMachine.getLegalMovesSub(state, role, subgame);
		}

		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public MachineState getNextStateSub(MachineState state, List<Move> moves, Subgame subgame)
			throws TransitionDefinitionException
	{
		if (testMachine.canPlaySubgames())
		{
			if (referenceMachine.canPlaySubgames())
			{
				MachineState reference = referenceMachine.getNextStateSub(state, moves, subgame);
				MachineState test = testMachine.getNextStateSub(state, moves, subgame);

				if (checkResults(test, reference))
				{
					return test;
				}

				return null;
			}

			// else
			return testMachine.getNextStateSub(state, moves, subgame);
		}

		throw new UnsupportedOperationException("Subgame operations not supported!");
	}

	@Override
	public boolean canPlaySubgames()
	{
		return testMachine.canPlaySubgames();
	}

	@Override
	public List<Move> getLegalMovesComplementSub(MachineState state, Role role, Subgame subgame)
			throws MoveDefinitionException
	{
		// TODO Auto-generated method stub
		return null;
	}
}

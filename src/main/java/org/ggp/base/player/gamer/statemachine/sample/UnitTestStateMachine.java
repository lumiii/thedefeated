package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

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
		int test = testMachine.getGoal(state, role);

		if (checkResults(new Integer(test), new Integer(reference)))
		{
			return test;
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
		List<Move> test = testMachine.getLegalMoves(state, role);


		if (checkResults(test, reference)){
			return test;
		}

		return null;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		MachineState reference = testMachine.getNextState(state, moves);
		MachineState test = testMachine.getNextState(state, moves);

		if (checkResults(test, reference))
		{
			return test;
		}

		return null;
	}

	private boolean checkResults(Object test, Object reference)
	{
		boolean match;
		if (test instanceof List<?> && reference instanceof List<?>)
		{
			List<?> testList = (List<?>)test;
			List<?> referenceList = (List<?>)reference;
			match = testList.containsAll(referenceList);
		}
		else
		{
			match = test.equals(reference);
		}

		if (!match)
		{
			log.error(GLog.UNITTEST,
					"getNextStateMismatch");
			log.error(GLog.UNITTEST,
					"Test:\n" + test);
			log.error(GLog.UNITTEST,
					"Reference:\n" + reference);

			return false;
		}

		return true;
	}
}

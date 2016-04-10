package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class RandomGamer extends SampleGamer {

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();

		List<Move> moves = stateMachine.findLegals(getRole(), getCurrentState());
		Random random = new Random();
		int index = random.nextInt(moves.size());
		Move nextMove = moves.get(index);

		return nextMove;
	}

}

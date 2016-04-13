package org.ggp.base.player.gamer.statemachine.sample;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class TreeSearchGamer extends SampleGamer {

	// amount of time to buffer before the timeout
	private static final long TIME_BUFFER = 3000;
	private static final int MAX_DEFAULT = 0;
	private static final int MIN_DEFAULT = 100;
	private long endTime = 0;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		Move selectMove = null;
		setTimeout(timeout);

		MachineState currentState = getCurrentState();
		List<Move> moves = getStateMachine().findLegals(getRole(), currentState);
		// skip processing if there are no options
		if (moves.size() == 1) {
			selectMove = moves.get(0);
		}
		else {
			SimpleImmutableEntry<Move, Integer> bestMove = getMaxMove(currentState);
			selectMove = bestMove.getKey();
		}

		setTimeout(0);

		return selectMove;
	}

	private SimpleImmutableEntry<Move, Integer> getMaxMove(MachineState currentState) throws MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<Move> moves = stateMachine.findLegals(getRole(), currentState);

		Move bestMove = stateMachine.findLegalx(getRole(), currentState);
		int bestScore = MAX_DEFAULT;
		for (Move move : moves) {
			List<Move> nextMoves = getOrderedMoves(move, getRole(), currentState);

			MachineState nextState = null;
			try {
				nextState = stateMachine.findNext(nextMoves, currentState);
			} catch (TransitionDefinitionException e1) {
				e1.printStackTrace();
				continue;
			}

			int nextScore = MAX_DEFAULT;

			try {
				nextScore = getMaxScore(nextState);

				if (nextScore > bestScore) {
					bestScore = nextScore;
					bestMove = move;
				}
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
				continue;
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
				continue;
			}
		}

		SimpleImmutableEntry<Move, Integer> maxMove = new SimpleImmutableEntry<Move, Integer>(bestMove, bestScore);

		return maxMove;
	}

	private int getMaxScore(MachineState currentState) throws MoveDefinitionException, GoalDefinitionException {
		if (isTimeout()) {
			System.out.println("Out of time, playing best guess.");
			return MAX_DEFAULT;
		}

		StateMachine stateMachine = getStateMachine();

		int maxScore = MAX_DEFAULT;

		if (stateMachine.findTerminalp(currentState)) {
			maxScore = stateMachine.findReward(getRole(), currentState);
		}
		else {
			SimpleImmutableEntry<Move, Integer> maxMove = getMaxMove(currentState);
			maxScore = maxMove.getValue();
		}

		return maxScore;
	}






	private List<Move> getOrderedMoves(Move move, Role moveOwner, MachineState currentState) throws MoveDefinitionException {
		List<Move> moves = new ArrayList<Move>();
		moves.add(move);

		return moves;
	}

	private void setTimeout(long timeout) {
		if (timeout != 0){
			endTime = timeout - TIME_BUFFER;
		}
		else {
			endTime = 0;
		}
	}

	private boolean isTimeout() {
		Date now = new Date();
		long nowTime = now.getTime();

		return nowTime >= endTime;
	}
}
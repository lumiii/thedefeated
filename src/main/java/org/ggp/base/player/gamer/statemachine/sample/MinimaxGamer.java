package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxGamer extends SampleGamer {

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();
		List<Move> moves = stateMachine.findLegals(getRole(), getCurrentState());

		int maxScore = 0;
		Move bestMove = null;
		for (Move move : moves) {
			List<Move> nextMoves = new ArrayList<Move>();
			nextMoves.add(move);

			Move opponentMove = stateMachine.findLegalx(getOpponent(), getCurrentState());
			nextMoves.add(opponentMove);

			MachineState nextState;
			try {
				nextState = stateMachine.findNext(nextMoves, getCurrentState());
			} catch (TransitionDefinitionException e1) {
				continue;
			}

			int nextScore;
			try {
				nextScore = getMinScore(nextState);

				if (nextScore > maxScore) {
					maxScore = nextScore;
				}
			} catch (GoalDefinitionException e) {
				continue;
			} catch (MoveDefinitionException e) {
				continue;
			}
		}

		return bestMove;
	}

	private int getMaxScore(MachineState currentState) throws MoveDefinitionException, GoalDefinitionException {
		StateMachine stateMachine = getStateMachine();

		if (stateMachine.findTerminalp(currentState)) {
			System.out.println("hi");
			int maxScore = stateMachine.findReward(getRole(), currentState);
			return maxScore;
		}
		else {
			List<Move> moves = stateMachine.findLegals(getRole(), currentState);

			int maxScore = 0;
			for (Move move : moves) {
				List<Move> nextMoves = new ArrayList<Move>();
				nextMoves.add(move);

				Move opponentNoop = stateMachine.findLegalx(getOpponent(), currentState);
				nextMoves.add(opponentNoop);

				MachineState nextState;
				try {
					nextState = stateMachine.findNext(nextMoves, currentState);
				} catch (TransitionDefinitionException e1) {
					continue;
				}

				int nextScore;
				try {
					nextScore = getMinScore(nextState);

					if (nextScore > maxScore) {
						maxScore = nextScore;
					}
				} catch (GoalDefinitionException e) {
					continue;
				} catch (MoveDefinitionException e) {
					continue;
				}
			}

			return maxScore;
		}
	}

	private Role getOpponent() {
		List<Role> roles = getStateMachine().findRoles();
		if (roles.size() > 2) {
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

		Role opponent = null;
		for (Role role : roles) {
			if (!role.equals(getRole())) {
				opponent = role;
				break;
			}
		}

		return opponent;
	}

	private int getMinScore(MachineState currentState) throws GoalDefinitionException, MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();

		// TODO, find all other roles except the current
		Role opponent = getOpponent();

		if (stateMachine.findTerminalp(currentState)) {
			System.out.println("Ho");
			int minScore = stateMachine.findReward(getRole(), currentState);
			return minScore;
		}
		else {
			List<Move> moves = stateMachine.findLegals(opponent, currentState);

			int minScore = 100;
			for (Move move : moves) {
				List<Move> nextMoves = new ArrayList<Move>();
				Move ourNoop = stateMachine.findLegalx(getRole(), currentState);
				nextMoves.add(ourNoop);

				nextMoves.add(move);

				MachineState nextState;
				try {
					nextState = stateMachine.findNext(nextMoves, currentState);
				} catch (TransitionDefinitionException e1) {
					continue;
				}

				int nextScore;
				try {
					nextScore = getMaxScore(nextState);

					if (minScore > nextScore) {
						minScore = nextScore;
					}
				} catch (GoalDefinitionException e) {
					continue;
				} catch (MoveDefinitionException e) {
					continue;
				}
			}

			return minScore;
		}
	}
}

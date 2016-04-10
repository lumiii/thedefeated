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
		Move bestMove = stateMachine.findLegalx(getRole(), getCurrentState());
		for (Move move : moves) {
			List<Move> nextMoves = getOrderedMoves(move, getCurrentState(), true);

			MachineState nextState;
			try {
				nextState = stateMachine.findNext(nextMoves, getCurrentState());
			} catch (TransitionDefinitionException e1) {
				e1.printStackTrace();
				continue;
			}

			int nextScore;
			try {
				nextScore = getMinScore(nextState);

				if (nextScore > maxScore) {
					maxScore = nextScore;
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

		if (bestMove.toString() != "noop") {
			System.out.println("Returning " + bestMove);
		}

		return bestMove;
	}

	private int getMaxScore(MachineState currentState) throws MoveDefinitionException, GoalDefinitionException {
		StateMachine stateMachine = getStateMachine();

		if (stateMachine.findTerminalp(currentState)) {
			int maxScore = stateMachine.findReward(getRole(), currentState);
			return maxScore;
		}
		else {
			List<Move> moves = stateMachine.findLegals(getRole(), currentState);

			int maxScore = 0;
			for (Move move : moves) {
				List<Move> nextMoves = getOrderedMoves(move, currentState, true);

				MachineState nextState;
				try {
					nextState = stateMachine.findNext(nextMoves, currentState);
				} catch (TransitionDefinitionException e1) {
					e1.printStackTrace();
					continue;
				}

				int nextScore;
				try {
					nextScore = getMinScore(nextState);

					if (nextScore > maxScore) {
						maxScore = nextScore;
					}
				} catch (GoalDefinitionException e) {
					e.printStackTrace();
					continue;
				} catch (MoveDefinitionException e) {
					e.printStackTrace();
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

	private List<Move> getOrderedMoves(Move move, MachineState currentState, boolean isOwnMove) throws MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<Role> roles = stateMachine.findRoles();

		if (roles.size() > 2) {
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

		List<Move> moves = new ArrayList<Move>();

		Role ownRole = getRole();
		for (Role role : roles) {
			if (ownRole.equals(role)) {
				if (isOwnMove) {
					moves.add(move);
				}
				else {
					moves.add(stateMachine.findLegalx(getRole(), currentState));
				}
			}
			else {
				if (!isOwnMove) {
					moves.add(move);
				}
				else {
					moves.add(stateMachine.findLegalx(getOpponent(), currentState));
				}
			}
		}


		System.out.print(moves);
		System.out.println("Own moves: " + isOwnMove);

		return moves;
	}

	private int getMinScore(MachineState currentState) throws GoalDefinitionException, MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();

		// TODO, find all other roles except the current
		Role opponent = getOpponent();

		if (stateMachine.findTerminalp(currentState)) {
			int minScore = stateMachine.findReward(getRole(), currentState);
			return minScore;
		}
		else {
			List<Move> moves = stateMachine.findLegals(opponent, currentState);

			int minScore = 100;
			for (Move move : moves) {
				List<Move> nextMoves = getOrderedMoves(move, currentState, false);

				MachineState nextState;
				try {
					nextState = stateMachine.findNext(nextMoves, currentState);
				} catch (TransitionDefinitionException e1) {
					e1.printStackTrace();
					continue;
				}

				int nextScore;
				try {
					nextScore = getMaxScore(nextState);

					if (minScore > nextScore) {
						minScore = nextScore;
					}
				} catch (GoalDefinitionException e) {
					e.printStackTrace();
					continue;
				} catch (MoveDefinitionException e) {
					e.printStackTrace();
					continue;
				}
			}

			return minScore;
		}
	}
}

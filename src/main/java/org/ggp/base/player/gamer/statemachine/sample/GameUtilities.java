package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class GameUtilities
{
	public static final int WIN_SCORE = 100;

	private StateMachine stateMachine;
	private Role playerRole;
	private Random rng;
	private final int roleSize;

	public GameUtilities(StateMachine stateMachine, Role role)
	{
		this.stateMachine = stateMachine;
		this.playerRole = role;
		this.rng = new Random();

		this.roleSize = stateMachine.getRoles().size();
	}

	public boolean isSinglePlayer()
	{
		List<Role> roles = stateMachine.findRoles();

		return (roles.size() == 1);
	}

	public Role getOpponent()
	{
		List<Role> roles = stateMachine.findRoles();
		if (roles.size() > 2)
		{
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

		for (Role r : roles)
		{
			if (!r.equals(playerRole))
			{
				return r;
			}
		}

		return null;
	}

	public List<Move> getRandomJointMove(Move move, Role moveOwner, MachineState currentState)
			throws MoveDefinitionException
	{
		List<Role> roles = stateMachine.findRoles();
		List<Move> moves = new ArrayList<Move>();

		for (Role role : roles)
		{
			if (moveOwner.equals(role))
			{
				moves.add(move);
			}
			else
			{
				moves.add(stateMachine.findLegalx(role, currentState));
			}
		}

		return moves;
	}

	public Move getRandomMove(MachineState currentState) throws MoveDefinitionException
	{
		List<Move> moveList = stateMachine.findLegals(playerRole, currentState);
		int randomIndex = rng.nextInt(moveList.size());

		return moveList.get(randomIndex);
	}

	public List<List<Move>> findAllMoves(MachineState state) throws MoveDefinitionException
	{
		List<List<Move>> moves = new ArrayList<List<Move>>();
		List<Role> roles = stateMachine.getRoles();

		moves.add(new ArrayList<Move>());

		for(Role r : roles)
		{
			List<Move> roleMoves = stateMachine.findLegals(r, state);
			List<List<Move>> newMoves = new ArrayList<List<Move>>();

			for (List<Move> l : moves)
			{
				for(Move m : roleMoves)
				{
					ArrayList<Move> tempList = new ArrayList<Move>(l);
					tempList.add(m);
					newMoves.add(tempList);
				}
			}

			moves = newMoves;
		}

		return moves;
	}

	public boolean playerHasMoves(MachineState currentState) throws MoveDefinitionException
	{
		List<Move> playerMoves = stateMachine.getLegalMoves(currentState, playerRole);

		return playerMoves.size() > 1;
	}

	public boolean opponentHasMoves(MachineState currentState) throws MoveDefinitionException
	{
		// check if all opponents have any choice
		// if they have no choices (alternating game)
		// then we can possibly force a win
		List<Role> roles = stateMachine.getRoles();
		for (Role role : roles)
		{
			if (!playerRole.equals(role))
			{
				List<Move> opponentMoves = stateMachine.getLegalMoves(currentState, role);
				if (opponentMoves.size() > 1)
				{
					return true;
				}
			}
		}

		return false;
	}

	// winning state if score == 100

	public boolean isWinningState(MachineState state)
	{
		if (stateMachine.findTerminalp(state))
		{
			try
			{
				int goalScore = stateMachine.getGoal(state, playerRole);
				if (goalScore >= WIN_SCORE)
				{
					return true;
				}
			}
			catch (GoalDefinitionException e)
			{
				e.printStackTrace();
			}
		}

		return false;
	}

	public int getRoleSize()
	{
		return roleSize;
	}

	public int[] getScoreForAllRoles(MachineState state) throws GoalDefinitionException
	{
		int[] scores = new int[roleSize];
		List<Role> roles = stateMachine.getRoles();
		int index = 0;
		for (Role role : roles)
		{
			int score = stateMachine.getGoal(state, role);
			scores[index] = score;
			index++;
		}

		return scores;
	}

	public int getPlayerRoleIndex()
	{
		return getRoleIndex(playerRole);
	}

	public int getFirstOpponentRoleIndex()
	{
		int playerRoleIndex = getPlayerRoleIndex();
		if (roleSize > 1)
		{
			if (playerRoleIndex == 0)
			{
				return 1;
			}

			return 0;
		}

		return -1;
	}

	public int getRoleIndex(Role role)
	{
		List<Role> roles = stateMachine.getRoles();
		return roles.indexOf(role);
	}
}

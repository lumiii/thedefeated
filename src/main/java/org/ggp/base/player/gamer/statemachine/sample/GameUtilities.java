package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class GameUtilities
{
	private StateMachine stateMachine;
	private Role role;

	public GameUtilities(StateMachine stateMachine, Role role)
	{
		this.stateMachine = stateMachine;
		this.role = role;
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
			if (!r.equals(role))
			{
				return r;
			}
		}

		return null;
	}

	public List<Move> getOrderedMoves(Move move, Role moveOwner, MachineState currentState)
			throws MoveDefinitionException
	{
		List<Role> roles = stateMachine.findRoles();

		if (roles.size() > 2)
		{
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

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
}

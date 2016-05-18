package org.ggp.base.player.gamer.statemachine.thedefeated.node;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.thedefeated.Subgame;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class NodePool
{
	public static Node newNode(Node par, MachineState stat, List<Move> m, boolean playerHasChoice, Subgame sub)
	{
		return new Node(par, stat, m, playerHasChoice, sub);
	}
}

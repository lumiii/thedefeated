package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class Node
{
	public int utility;
	public int visit;

	public final boolean maxNode;
	public volatile boolean selected;

	public boolean locked;

	public Node parent;
	public AbstractQueue<Node> children;
	public final MachineState state;
	public final List<Move> move;
	public final Subgame subgame;

	public Node(Node par, MachineState stat, List<Move> m, boolean playerHasChoice, Subgame sub)
	{
		utility = 0;
		visit = 0;
		selected = false;
		parent = par;
		children = new ConcurrentLinkedQueue<Node>();
		state = stat;
		move = m;
		maxNode = playerHasChoice || !RuntimeParameters.MINIMAX;
		locked = false;
		subgame = sub;
	}
}
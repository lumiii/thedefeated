package org.ggp.base.player.gamer.statemachine.sample;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class Node
{
	public int utility;
	public int visit;

	public boolean max;
	public volatile boolean selected;

	public Node parent;
	public AbstractQueue<Node> children;
	public final MachineState state;
	public final List<Move> move;

	public Node(Node par, MachineState stat, List<Move> m, Boolean maxBool)
	{
		utility = 0;
		visit = 0;
		selected = false;
		parent = par;
		children = new ConcurrentLinkedQueue<Node>();
		state = stat;
		move = m;
		max = maxBool;
	}
}
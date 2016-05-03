package org.ggp.base.player.gamer.statemachine.sample;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class Node
{
	public int[] utilities;
	public int visit;

	public final boolean maxNode;
	public volatile boolean selected;
	public volatile boolean completed;

	public volatile int completedChildren;

	public Node parent;
	public AbstractQueue<Node> children;
	public final MachineState state;
	public final List<Move> move;

	public Node(Node par, MachineState stat, List<Move> m, int roleSize, boolean playerHasChoice)
	{
		utilities = new int[roleSize];
		visit = 0;
		selected = false;
		parent = par;
		children = new ConcurrentLinkedQueue<Node>();
		state = stat;
		move = m;
		maxNode = playerHasChoice;
		completed = false;
		completedChildren = 0;
	}

	public static class NodeComparator implements Comparator<Node>
	{
		private int roleIndex;
		public NodeComparator(int roleIndex)
		{
			this.roleIndex = roleIndex;
		}

		@Override
		public int compare(Node o1, Node o2)
		{
			double n1 = TreeSearchWorker.selectFn(o1, roleIndex);
			double n2 = TreeSearchWorker.selectFn(o2, roleIndex);

			int result = 0;
			if (n1 < n2)
			{
				result = 1;
			}
			else if (n2 < n1)
			{
				result = -1;
			}

			return result;
		}
	}
}
package org.ggp.base.player.gamer.statemachine.sample;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class Node
{
	public static NodeComparator comparator = new NodeComparator();

	public int utility;
	public int visit;

	public boolean max;
	public volatile boolean selected;
	public volatile boolean completed;

	public volatile int completedChildren;

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
		completed = false;
		completedChildren = 0;
	}

	public static class NodeComparator implements Comparator<Node>
	{
		public NodeComparator()
		{

		}

		@Override
		public int compare(Node o1, Node o2)
		{
			double n1 = TreeSearchWorker.selectFn(o1);
			double n2 = TreeSearchWorker.selectFn(o2);

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
package org.ggp.base.player.gamer.statemachine.thedefeated.node;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.player.gamer.statemachine.thedefeated.GLog;
import org.ggp.base.player.gamer.statemachine.thedefeated.MachineParameters;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class NodePool
{
	private static AbstractQueue<Node> queue = new ConcurrentLinkedQueue<>();
	private static volatile int nodeCount = 0;

	public static void initialize(int size)
	{
		queue.clear();
		nodeCount = 0;
		allocateNodes(size);
	}

	private static void allocateNodes(int size)
	{
		for (int i = 0; i < size; i++)
		{
			queue.add(new Node());
		}

		nodeCount += size;
		GLog.getRootLogger().info(GLog.MEMORY,
				"Node count currently: " + nodeCount);
	}

	public static void collect(Node node)
	{
		if (node.parent() != null)
		{
			GLog.getRootLogger().error(GLog.ERRORS,
					"Garbage nodes should not have a parent!");
		}

		queue.add(node);
	}

	public static Node newNode(Node par, MachineState stat, List<Move> m, boolean playerHasChoice)
	{
		Node node = getNode(par, stat, m, playerHasChoice);

		if (node == null && canGrow())
		{
			allocateNodes(MachineParameters.LOW_NODE_THRESHOLD);
			node = getNode(par, stat, m, playerHasChoice);
		}

		return node;
	}

	private static Node getNode(Node par, MachineState stat, List<Move> m, boolean playerHasChoice)
	{
		if (queue.isEmpty())
		{
			return null;
		}

		Node node = queue.poll();

		if (node != null)
		{
			queue.addAll(node.children());
			// this clears the children - the queue should
			// be the only thing that has references to the child nodes now
			node.set(par, stat, m, playerHasChoice);
		}

		return node;
	}

	public static boolean canGrow()
	{
		return nodeCount < MachineParameters.MAX_NODES;
	}
}

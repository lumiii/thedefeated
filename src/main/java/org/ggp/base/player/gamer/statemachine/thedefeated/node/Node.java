package org.ggp.base.player.gamer.statemachine.thedefeated.node;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.player.gamer.statemachine.sample.TreeSearchWorker;
import org.ggp.base.player.gamer.statemachine.thedefeated.RuntimeParameters;
import org.ggp.base.player.gamer.statemachine.thedefeated.Subgame;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class Node
{
	private static final int LOCKED = -1;

	private int utility;
	private int visit;

	private boolean maxNode;
	private volatile boolean selected;

	private Node parent;
	private AbstractQueue<Node> children;
	private MachineState state;
	private List<Move> move;
	private Subgame subgame;

	protected Node()
	{
		children = new ConcurrentLinkedQueue<Node>();
	}

	protected void set(Node par, MachineState stat, List<Move> m, boolean playerHasChoice, Subgame sub)
	{
		utility = 0;
		visit = 0;
		maxNode = playerHasChoice || !RuntimeParameters.MINIMAX;
		selected = false;
		parent = par;
		children.clear();
		state = stat;
		move = m;
		subgame = sub;
	}

	public void lockToMax()
	{
		visit = LOCKED;
		utility = TreeSearchWorker.WIN_SCORE;
	}

	public void lockToMin()
	{
		visit = LOCKED;
		utility = TreeSearchWorker.LOSE_SCORE;
	}

	public Subgame subgame()
	{
		return subgame;
	}

	public boolean isMaxNode()
	{
		return maxNode;
	}

	public Node parent()
	{
		return parent;
	}

	public void orphan()
	{
		Node oldParent = parent;
		if (oldParent != null)
		{
			oldParent.children.remove(this);
		}

		parent = null;
	}

	public AbstractQueue<Node> children()
	{
		return children;
	}

	public List<Move> move()
	{
		return move;
	}

	public MachineState state()
	{
		return state;
	}

	public int utility()
	{
		return utility;
	}

	public int visit()
	{
		if (isLocked())
		{
			if (utility == TreeSearchWorker.WIN_SCORE)
			{
				return TreeSearchWorker.MIN_VISITS;
			}
			else if (utility == TreeSearchWorker.LOSE_SCORE)
			{
				return TreeSearchWorker.MAX_VISITS;
			}

			throw new IllegalStateException("Inconsistent utility when node locked");
		}

		return visit;
	}

	public boolean isLocked()
	{
		return visit == LOCKED;
	}

	public void utilityIncrement(int increment)
	{
		if (!isLocked())
		{
			utility += increment;
		}
	}

	public void visitIncrement(int increment)
	{
		if (!isLocked())
		{
			visit += increment;
		}
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void select()
	{
		selected = true;
	}
}
package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class TreeSearchWorker implements Runnable
{
	private static final int WIN_SCORE = 100;
	private static final int LOSE_SCORE = 0;
	private static final int MAX_VISITS = Integer.MAX_VALUE / 2;
	private static final int MIN_VISITS = 1;
	private volatile static int nodesVisited = 0;
	private volatile static int nodesUpdated = 0;

	private int id;

	private StateMachine stateMachine;

	private Role playerRole;
	private Node root;

	private Node newRoot;

	private GameUtilities utility;

	public TreeSearchWorker(int id)
	{
		this.id = id;
	}

	public static void globalInit()
	{
		nodesVisited = 0;
		nodesUpdated = 0;
	}

	public void init(StateMachine stateMachine, Role role)
	{
		this.stateMachine = new CachedStateMachine(stateMachine);
		this.playerRole = role;
		this.root = null;
		this.newRoot = null;
		this.utility = new GameUtilities(stateMachine, role);
	}

	public void cleanup()
	{
		this.stateMachine = null;
		this.playerRole = null;
		this.root = null;
		this.newRoot = null;
		this.utility = null;
	}

	public void setRoot(Node root)
	{
		this.newRoot = root;
	}

	private void update()
	{
		if (this.root != this.newRoot)
		{
			this.root = this.newRoot;
			System.out.println("Thread " + Thread.currentThread().getName() + " active");
			//TreeSearchWorker.printStats();
		}
	}

	@Override
	public void run()
	{
		System.out.println("Starting worker" + id + " thread " + Thread.currentThread().getName());
		Thread currentThread = Thread.currentThread();

		while (!currentThread.isInterrupted())
		{
			try
			{
				update();
				treeSearch();
			}

			// catch all exceptions
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		cleanup();
	}


	private void treeSearch() throws MoveDefinitionException, TransitionDefinitionException
	{
		Node node = select(root);

		if (node != null)
		{
			nodesVisited++;

			if (!stateMachine.findTerminalp(node.state))
			{
				expand(node);

				int visits = 0;

				int totalScore = 0;
				for (int i = 0; i < RuntimeParameters.DEPTH_CHARGE_COUNT; i++)
				{
					try
					{
						MachineState terminalState = stateMachine.performDepthCharge(node.state, null);
						totalScore += stateMachine.getGoal(terminalState, playerRole);
						visits++;
					}
					catch (GoalDefinitionException | TransitionDefinitionException | MoveDefinitionException e)
					{
						e.printStackTrace();
					}
				}

				backPropagate(node, totalScore, visits);
			}
			else
			{
				try
				{
					int score = stateMachine.getGoal(node.state, playerRole);
					if (score <= LOSE_SCORE)
					{
						Node parent = node.parent;
						if (!parent.maxNode)
						{
							parent.utility = LOSE_SCORE;
							parent.visit = MAX_VISITS;
							parent.locked = true;
						}
					}
					else if (score >= WIN_SCORE)
					{
						Node parent = node.parent;
						if (parent.maxNode)
						{
							parent.utility = WIN_SCORE;
							parent.visit = MIN_VISITS;
							parent.locked = true;
						}
					}

					backPropagate(node, score * RuntimeParameters.DEPTH_CHARGE_COUNT, RuntimeParameters.DEPTH_CHARGE_COUNT);
				}
				catch (GoalDefinitionException e)
				{
					// this is a common occurrence
					// just ignore it
				}
			}
		}
	}

	public static void printStats()
	{
		System.out.println("Nodes visited: " + nodesVisited);
		System.out.println("Nodes updated: " + nodesUpdated);
	}

	private Node select(Node node)
	{
		synchronized (node)
		{
			if (!node.selected)
			{
				node.selected = true;
				return node;
			}
		}

		for (Node child : node.children)
		{
			synchronized (child)
			{
				if (!child.selected)
				{
					child.selected = true;
					return child;
				}
			}
		}

		if (!node.children.isEmpty())
		{
			// if it's a max node, start with the minimum value and look up
			// otherwise, start with the max value and look down
			double score = node.maxNode ? 0 : Double.MAX_VALUE;

			Node result = null;
			for (Node child : node.children)
			{
				double newScore = selectFn(child);

				// use the highest score if it's a max node
				if (node.maxNode && newScore > score)
				{
					score = newScore;
					result = child;
				}
				// use the lowest score if it's a min node
				else if (!node.maxNode && newScore < score)
				{
					score = newScore;
					result = child;
				}
			}

			if (result != null)
			{
				return select(result);
			}
		}

		return null;
	}

	private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException
	{
		List<List<Move>> moves = utility.findAllMoves(node.state);
		for (List<Move> m : moves)
		{
			MachineState newState = stateMachine.getNextState(node.state, m);
			boolean maxNode = utility.playerHasMoves(newState);
			Node newNode = new Node(node, newState, m, maxNode);

			node.children.add(newNode);
		}
	}

	private void backPropagate(Node node, int totalScore, int visits)
	{
		synchronized (node)
		{
			if (!node.locked)
			{
				node.visit += visits;
				node.utility += totalScore;
			}

			nodesUpdated += visits;
		}

		if (node.parent != null)
		{
			backPropagate(node.parent, totalScore, visits);
		}
	}

	private double selectFn(Node node)
	{
		synchronized (node)
		{
			if (node.visit == 0)
			{
				return 0;
			}

			int parentVisit = 0;

			if (node.parent != null)
			{
				parentVisit = node.parent.visit;
			}

			return (node.utility / node.visit
					+ RuntimeParameters.EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(parentVisit) / node.visit));
		}
	}
}
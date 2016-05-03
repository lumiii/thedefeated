package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

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
	private volatile static int nodesVisited = 0;
	private volatile static int nodesUpdated = 0;

	private volatile static Node[] searchResults = null;

	private volatile static boolean usePriorityQueue = false;

	private static Set<Node> terminalNodes = null;

	private int id;

	private StateMachine stateMachine;

	private Role role;
	private Node root;

	private Node newRoot;

	private GameUtilities utility;

	public TreeSearchWorker(int id)
	{
		this.id = id;
	}

	public static void globalInit(int numWorkers)
	{
		nodesVisited = 0;
		nodesUpdated = 0;

		terminalNodes = Collections.synchronizedSet(new HashSet<Node>());
		searchResults = new Node[numWorkers];
	}

	public void init(StateMachine stateMachine, Role role)
	{
		this.stateMachine = new CachedStateMachine(stateMachine);
		this.role = role;
		this.root = null;
		this.newRoot = null;
		this.utility = new GameUtilities(stateMachine, role);
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
		}
	}

	private static boolean emptyResults()
	{
		if (nodesVisited == 0)
		{
			return false;
		}

		for (int i = 0; i < searchResults.length; i++)
		{
			if (searchResults[i] != null)
			{
				return false;
			}
		}

		return true;
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
				if (!usePriorityQueue && emptyResults())
				{
					usePriorityQueue = true;
					System.out.println("Turning on queues");
				}

				update();
				if (!root.completed)
				{
					treeSearch();
				}
				else
				{
					backPropagateOnTerminalNodes();
				}
			}

			// catch all exceptions
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void backPropagateOnTerminalNodes()
	{
		System.out.println("From thread " + Thread.currentThread().getName());
		System.out.println("Back prop on terminal nodes");
		System.out.println("Size " + terminalNodes.size());
		for (Node node : terminalNodes)
		{
			try
			{
				int score = stateMachine.getGoal(node.state, role);
				System.out.println("Backpropagating score " + score);
				backPropagate(node, score, 1, false);
			}
			catch (GoalDefinitionException e)
			{
				// this is a common occurrence
				// just ignore it
			}
		}
	}

	private void treeSearch() throws MoveDefinitionException, TransitionDefinitionException
	{
		Node node = select(root);

		if (node != null)
		{
			searchResults[id] = node;
			nodesVisited++;

			if (!stateMachine.findTerminalp(node.state))
			{
				expand(node);

				int visits = 0;
				int totalScore = 0;

				for (int i = 0; i < Parameters.DEPTH_CHARGE_COUNT; i++)
				{
					try
					{
						MachineState terminalState = stateMachine.performDepthCharge(node.state, null);
						totalScore += stateMachine.getGoal(terminalState, role);
						visits++;
					}
					catch (GoalDefinitionException | TransitionDefinitionException | MoveDefinitionException e)
					{
						e.printStackTrace();
					}
				}

				backPropagate(node, totalScore, visits, node.completed);
			}
			else
			{
				terminalNodes.add(node);

				try
				{
					node.completed = true;
					int score = stateMachine.getGoal(node.state, role);
					backPropagate(node, score, 1, true);
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
			if (!usePriorityQueue)
			{
				double score = 0;
				Node result = null;
				for (Node child : node.children)
				{
					double newScore = selectFn(child);
					// System.out.println(newScore);
					if (newScore > score)
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
			else
			{
				PriorityQueue<Node> queue = new PriorityQueue<>(Node.comparator);
				queue.addAll(node.children);

				//System.out.println("Using priority queue size" + queue.size());

				while (!queue.isEmpty())
				{
					Node result = select(queue.peek());

					if (result != null)
					{
						return result;
					}
					else
					{
						queue.poll();
					}
				}
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
			Node newNode = new Node(node, newState, m, !node.max);

			node.children.add(newNode);
		}
	}

	private void backPropagate(Node node, int totalScore, int visits, boolean isCompleted)
	{
		synchronized (node)
		{
			node.visit += visits;
			node.utility += totalScore;
			nodesUpdated += visits;

			if (isCompleted && !root.completed)
			{
				int childrenSize = node.children.size();
				if (childrenSize == 0)
				{
					node.completed = true;
				}
				else
				{
					node.completedChildren++;
					if (node.completedChildren == childrenSize)
					{
						node.completed = true;
					}
					else if (node.completedChildren > childrenSize)
					{
						System.out.println("MISCOUNT");
						System.out.println("Completed children count: " + node.completedChildren);
						System.out.println("Children size: " + childrenSize);
					}
				}
			}
		}

		if (node.parent != null)
		{
			backPropagate(node.parent, totalScore, visits, node.completed);
		}
	}

	public static double selectFn(Node node)
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
					+ Parameters.EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(parentVisit) / node.visit));
		}
	}
}
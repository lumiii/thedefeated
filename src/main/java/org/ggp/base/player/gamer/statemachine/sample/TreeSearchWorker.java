package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.apache.logging.log4j.Logger;
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
	private static final Logger log = GLog.getLogger(TreeSearchWorker.class);

	private static final int WIN_SCORE = 100;
	private static final int LOSE_SCORE = 0;
	private static final int MAX_VISITS = Integer.MAX_VALUE / 2;
	private static final int MIN_VISITS = 1;

	private static final int FULL_TREE_WARNING_THRESHOLD = 10;

	private volatile static int nodesVisited = 0;
	private volatile static int nodesUpdated = 0;
	private volatile static int terminalNodeVisited = 0;

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
			log.info(GLog.THREAD_ACTIVITY,
					"Thread active");
		}
	}

	@Override
	public void run()
	{
		Thread currentThread = Thread.currentThread();
		log.info(GLog.THREAD_ACTIVITY,
				"Starting thread");

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
				log.error(GLog.ERRORS,
						"Exception encountered within thread", e);
			}
		}

		cleanup();
		log.info(GLog.THREAD_ACTIVITY,
				"Stopping thread");
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
						log.error(GLog.ERRORS,
								"Error encountered performing depth charge", e);
					}
				}

				backPropagate(node, totalScore, visits);
			}
			else
			{
				try
				{
					int score = stateMachine.getGoal(node.state, playerRole);

					terminalNodeVisited++;

					if (score <= LOSE_SCORE)
					{
						Node parent = node.parent;
						if (!parent.maxNode)
						{
							log.debug(GLog.TREE_SEARCH,
								"Found min terminal node");

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
							log.debug(GLog.TREE_SEARCH,
								"Found max terminal node");

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
		log.info(GLog.NODE_STATS, "Nodes visited: " + nodesVisited);
		log.info(GLog.NODE_STATS, "Terminal nodes visited: " + terminalNodeVisited);
		log.info(GLog.NODE_STATS, "Nodes updated: " + nodesUpdated);
	}

	private Node select(Node node)
	{
		Node currentNode = node;
		int depth = 0;

		while (currentNode != null)
		{
			depth++;

			synchronized (currentNode)
			{
				if (!currentNode.selected)
				{
					currentNode.selected = true;

					log.info(GLog.TREE_SEARCH,
							"Searched depth " + depth);

					return currentNode;
				}
			}

			for (Node child : currentNode.children)
			{
				synchronized (child)
				{
					if (!child.selected)
					{
						child.selected = true;
						log.info(GLog.TREE_SEARCH,
								"Searched depth " + depth);

						return child;
					}
				}
			}

			Node result = null;
			if (!currentNode.children.isEmpty())
			{
				// if it's a max node, start with the minimum value and look up
				// otherwise, start with the max value and look down
				double score = currentNode.maxNode ? 0 : Double.MAX_VALUE;

				for (Node child : currentNode.children)
				{
					double newScore = selectFn(child);

					// use the highest score if it's a max node
					if (currentNode.maxNode && newScore > score)
					{
						score = newScore;
						result = child;
					}
					// use the lowest score if it's a min node
					else if (!currentNode.maxNode && newScore < score)
					{
						score = newScore;
						result = child;
					}
				}
			}

			currentNode = result;
		}

		if (nodesVisited > FULL_TREE_WARNING_THRESHOLD)
		{
			log.warn(GLog.TREE_SEARCH,
					"No nodes selected after searching depth " + depth);
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
		Node currentNode = node;
		int depth = 0;
		while (currentNode != null)
		{
			depth++;

			synchronized (currentNode)
			{
				if (!currentNode.locked)
				{
					currentNode.visit += visits;
					currentNode.utility += totalScore;
				}
			}

			nodesUpdated += visits;
			currentNode = currentNode.parent;
		}

		log.info(GLog.TREE_SEARCH,
				"Backpropagated up depth " + depth);
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
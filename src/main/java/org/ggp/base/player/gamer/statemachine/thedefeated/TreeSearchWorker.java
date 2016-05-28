package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.Node;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.NodePool;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class TreeSearchWorker implements Runnable
{
	private static final Logger log = GLog.getLogger(TreeSearchWorker.class);

	public static final int WIN_SCORE = 100;
	public static final int LOSE_SCORE = 0;
	public static final int MAX_VISITS = Integer.MAX_VALUE / 2;
	public static final int MIN_VISITS = 1;

	private static final int FULL_TREE_WARNING_THRESHOLD = 10;

	private volatile static int nodesVisited = 0;
	private volatile static int nodesUpdated = 0;
	private volatile static int terminalNodeVisited = 0;
	private volatile static int depthChargeCount = 0;

	private int id;
	private ThreadManager manager;

	private AugmentedStateMachine stateMachine;

	private Role playerRole;

	private Node root;
	private Node newRoot;

	private GameUtilities utility;

	public TreeSearchWorker(int id, ThreadManager manager)
	{
		this.id = id;
		this.manager = manager;
	}

	public static void globalInit()
	{
		nodesVisited = 0;
		nodesUpdated = 0;
	}

	public void init(AugmentedStateMachine stateMachine, Role role)
	{
		// disable caching behaviour if unittesting
		// so we can hit our propagation routines every time
		// try to use this with a single thread - otherwise there will be a lot
		// of
		// locked up threads
		// TODO: find a way to better parallelize our propnet
		// right now we can't service more than one propagation at the same time
		// perhaps multiple propnet statemachines (same # as # of threads?)
		// but this could cause memory blowup
		if (!RuntimeParameters.UNITTEST_PROPNET)
		{
			this.stateMachine = new AugmentedCachedStateMachine(stateMachine);
		}
		else
		{
			this.stateMachine = stateMachine;
		}

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
			manager.updateReference(this.root, this.newRoot);
			this.root = this.newRoot;

			log.info(GLog.THREAD_ACTIVITY, "Thread active");
		}
	}

	@Override
	public void run()
	{
		Thread currentThread = Thread.currentThread();
		log.info(GLog.THREAD_ACTIVITY, "Starting thread");

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
				log.error(GLog.ERRORS, "Exception encountered within thread");
				log.catching(e);
			}
		}

		cleanup();
		log.info(GLog.THREAD_ACTIVITY, "Stopping thread");
	}

	private void treeSearch() throws MoveDefinitionException, TransitionDefinitionException
	{
		Node node = select(root);

		if (node != null)
		{
			nodesVisited++;

			if (!stateMachine.isTerminal(node.state()))
			{
				expand(node);

				int visits = 0;
				int totalScore = 0;
				for (int i = 0; i < RuntimeParameters.DEPTH_CHARGE_COUNT; i++)
				{
					try
					{
						MachineState terminalState = stateMachine.performDepthCharge(node.state(), null);

						depthChargeCount++;
						totalScore += stateMachine.getGoal(terminalState, playerRole);

						// TODO: verify whether this is the count we want:
						// if a node explored is not terminal within the counted
						// depth, we just say it has a score of 0?
						visits++;
					}
					catch (GoalDefinitionException | TransitionDefinitionException | MoveDefinitionException e)
					{
						log.error(GLog.ERRORS, "Error encountered performing depth charge");
						log.catching(e);
					}
				}

				backPropagate(node, totalScore, visits);
			}
			else
			{
				try
				{
					int score = stateMachine.getGoal(node.state(), playerRole);

					terminalNodeVisited++;

					if (score <= LOSE_SCORE)
					{
						Node parent = node.parent();
						if (!parent.isMaxNode())
						{
							log.debug(GLog.TREE_SEARCH, "Found min terminal node");

							parent.lockToMin();
						}
					}
					else if (score >= WIN_SCORE)
					{
						Node parent = node.parent();
						if (parent.isMaxNode())
						{
							log.debug(GLog.TREE_SEARCH, "Found max terminal node");

							parent.lockToMax();
						}
					}

					backPropagate(node, score * RuntimeParameters.DEPTH_CHARGE_COUNT,
							RuntimeParameters.DEPTH_CHARGE_COUNT);
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
		log.info(GLog.NODE_STATS, "Depth charges: " + depthChargeCount);
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
				if (!currentNode.isSelected())
				{
					currentNode.select();

					log.info(GLog.TREE_SEARCH, "Searched depth " + depth);

					return currentNode;
				}
			}

			for (Node child : currentNode.children())
			{
				synchronized (child)
				{
					if (!child.isSelected())
					{
						child.select();
						log.info(GLog.TREE_SEARCH, "Searched depth " + depth);

						return child;
					}
				}
			}

			Node result = null;
			if (!currentNode.children().isEmpty())
			{
				// if it's a max node, start with the minimum value and look up
				// otherwise, start with the max value and look down
				double score = currentNode.isMaxNode() ? 0 : Double.MAX_VALUE;

				for (Node child : currentNode.children())
				{
					double newScore = selectFn(child);

					// use the highest score if it's a max node
					if ((currentNode.isMaxNode() && newScore > score) ||
						// use the lowest score if it's a min node
						(!currentNode.isMaxNode() && newScore < score))
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
			log.warn(GLog.TREE_SEARCH, "No nodes selected after searching depth " + depth);
		}

		return null;
	}

	private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException
	{
		List<List<Move>> moves = utility.findAllMoves(node.state());

		for (List<Move> m : moves)
		{
			MachineState newState = stateMachine.getNextState(node.state(), m);

			if (!stateMachine.isDeadState(newState))
			{
				boolean maxNode = utility.playerHasMoves(newState);

				Node newNode = NodePool.newNode(node, newState, m, maxNode);

				// just do an early return because there's no possible way for
				// us to get
				// more memory out of this - hopefully some will be freed up
				// next time
				if (newNode == null)
				{
					return;
				}

				node.children().add(newNode);
			}
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
				if (!currentNode.isLocked())
				{
					currentNode.visitIncrement(visits);
					currentNode.utilityIncrement(totalScore);
				}
			}

			nodesUpdated += visits;
			currentNode = currentNode.parent();
		}

		log.info(GLog.TREE_SEARCH, "Backpropagated up depth " + depth);
	}

	private double selectFn(Node node)
	{
		synchronized (node)
		{
			if (node.visit() == 0)
			{
				return 0;
			}

			int parentVisit = 0;

			if (node.parent() != null)
			{
				parentVisit = node.parent().visit();
			}

			return (node.utility() / node.visit()
					+ RuntimeParameters.EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(parentVisit) / node.visit()));
		}
	}
}
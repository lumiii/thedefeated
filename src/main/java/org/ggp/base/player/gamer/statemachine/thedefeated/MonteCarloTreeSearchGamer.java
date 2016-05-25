package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.Logger;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.player.gamer.statemachine.sample.TreeSearchWorker;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.Node;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.NodePool;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class MonteCarloTreeSearchGamer extends SampleGamer
{
	private static final Logger log = GLog.getLogger(MonteCarloTreeSearchGamer.class);
	private long endTime = 0;

	private ThreadManager threadManager = new ThreadManager(MachineParameters.NUM_CORES);
	private Map<MachineState, Node> childStates = new HashMap<>();
	private Node root = null;
	private GameUtilities utility = null;

	private Timer timer = null;
	private ThreadTimer threadTimer = null;
	private Object lock = null;
	private Set<Subgame> subs;

	static
	{
		printParameters();
	}

	class ThreadTimer extends TimerTask
	{
		private boolean disable;
		private Object lock;

		public ThreadTimer(Object lock)
		{
			this.lock = lock;
			this.disable = false;
		}

		public void disable()
		{
			this.disable = true;
		}

		@Override
		public void run()
		{
			if (!this.disable)
			{
				synchronized (lock)
				{
					lock.notify();
				}

				log.info(GLog.MAIN_THREAD_ACTIVITY, "Waking up main thread from sleep");
			}
		}
	}

	public MonteCarloTreeSearchGamer()
	{
		Thread.currentThread().setPriority(MachineParameters.MAIN_THREAD_PRIORITY);

		lock = new Object();
	}

	@Override
	public void stateMachineStop()
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Stop called");
		endGame();
	}

	@Override
	public void stateMachineAbort()
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Abort called");
		endGame();
	}

	private void endGame()
	{
		Thread.interrupted();
		if (threadTimer != null)
		{
			threadTimer.disable();
			threadTimer = null;
		}
		timer.cancel();
		timer.purge();
		timer = null;

		threadManager.stopWorkers();
		root = null;
		childStates.clear();

		TreeSearchWorker.printStats();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY, GLog.BANNER + " Beginning meta game " + GLog.BANNER);
		setMetaTimeout(timeout);

		timer = new Timer();


		StateMachine stateMachine = getStateMachine();
		Role role = getRole();
		subs = stateMachine.getSubgames();
		log.info(GLog.FACTOR, "Found " + subs.size() + " subgames");


		List<Proposition> inhibitors = stateMachine.findBaseInhibitors(role);
		Map<Proposition, Boolean> latches = stateMachine.getLatchInhibitors(inhibitors);

		threadManager.initializeWorkers(stateMachine, role);

		utility = new GameUtilities(stateMachine, role);

		childStates.clear();

		updateRoot(getCurrentState());
		expandWithSubgame(root);
		childStates.put(root.state(), root);

		int minDepth = findMinDepth(stateMachine.getInitialState());
		threadManager.updateWorkers(root, minDepth);

		threadManager.startWorkers();

		waitForTimeout();

		setTimeout(0);
		log.info(GLog.MAIN_THREAD_ACTIVITY, GLog.BANNER + " Ending meta game " + GLog.BANNER);
	}

	private void expandWithSubgame(Node node)
			throws TransitionDefinitionException, MoveDefinitionException
	{
		if (!RuntimeParameters.FACTOR_SUBGAME)
		{
			return;
		}

		StateMachine stateMachine = getStateMachine();
		Role role = getRole();

		MachineState state = node.state();

		List<List<Move>> moves = utility.findAllMoves(state);
		int roleIndex = stateMachine.getRoleIndices().get(role);
		for (List<Move> m : moves)
		{
			MachineState newState = stateMachine.getNextState(state, m);
			boolean maxNode = utility.playerHasMoves(newState);
			boolean found = false;
			for (Subgame sub : subs)
			{
				Move playerMove = m.get(roleIndex);
				Set<Move> inputMoves = sub.getInputMoves();
				if (inputMoves.contains(playerMove))
				{
					Node newNode = NodePool.newNode(node, newState, m, maxNode, sub);
					node.children().add(newNode);
					found = true;
					log.info(GLog.FACTOR, "Subgame found");
				}
			}

			if (!found)
			{
				log.info(GLog.FACTOR, "No subgame for move found");
			}
		}

		node.visitIncrement(1);
		node.select();
		if (node.children().isEmpty())
		{
			for (List<Move> m : moves)
			{
				MachineState newState = stateMachine.getNextState(state, m);
				boolean maxNode = utility.playerHasMoves(newState);
				Node newNode = NodePool.newNode(node, newState, m, maxNode, null);
				node.children().add(newNode);

				expandWithSubgame(newNode);
			}
		}
	}

	private int findMinDepth(MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException
	{
		if (!RuntimeParameters.FACTOR_SUBGAME)
		{
			return 0;
		}

		StateMachine stateMachine = getStateMachine();
		int minDepth = 1000;
		Random rand = new Random();
		int numCharges = 10;
		for (Subgame sub : subs)
		{
			for (int i = 0; i < numCharges; i++)
			{
				int depth = 0;
				MachineState state = new MachineState(currentState.getContents());
				while (!stateMachine.isTerminal(state, sub))
				{
					List<Move> randMove = new ArrayList<Move>();
					for (Role r : stateMachine.getRoles())
					{
						if (r.equals(getRole()))
						{
							List<Move> moves = stateMachine.getLegalMovesComplement(state, r, sub);
							Move m = moves.get(rand.nextInt(moves.size()));
							randMove.add(m);
						}
						else
						{
							List<Move> moves = stateMachine.getLegalMoves(state, r, sub);
							Move m = moves.get(rand.nextInt(moves.size()));
							randMove.add(m);
						}
					}
					state = stateMachine.getNextState(state, randMove, sub);
					depth++;
				}
				if (depth < minDepth)
				{
					minDepth = depth;
				}
			}

		}
		return minDepth;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY, GLog.BANNER + " Beginning move selection " + GLog.BANNER);

		setTimeout(timeout);

		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();

		updateRoot(currentState);

		threadManager.updateWorkers(root, findMinDepth(getCurrentState()));

		for (Node child : root.children())
		{
			childStates.put(child.state(), child);
		}

		waitForTimeout();

		Role role = getRole();

		List<Move> moves = stateMachine.findLegals(role, currentState);

		Move bestMove = null;
		if (moves.size() > 1)
		{
			bestMove = selectBestMove(currentState, moves);
		}

		// if no choices or no good moves were found
		if (bestMove == null)
		{
			bestMove = utility.getRandomMove(currentState);
			log.info(GLog.MOVE_EVALUATION, "Playing random move: " + bestMove);
		}

		setTimeout(0);

		log.info(GLog.MAIN_THREAD_ACTIVITY, GLog.BANNER + " Ending move selection " + GLog.BANNER);
		TreeSearchWorker.printStats();

		return bestMove;
	}

	private void waitForTimeout()
	{
		while (!isTimeout())
		{
			log.info(GLog.MAIN_THREAD_ACTIVITY, "Before waiting...");
			try
			{
				threadTimer = new ThreadTimer(lock);
				long sleep_interval = getRemainingTime();
				log.info(GLog.MAIN_THREAD_ACTIVITY, "Sleeping for: " + sleep_interval);

				synchronized (lock)
				{
					timer.schedule(threadTimer, sleep_interval);
					lock.wait(sleep_interval);
				}
			}
			catch (InterruptedException e)
			{
			}

			threadTimer = null;
			Thread.interrupted();
			log.info(GLog.MAIN_THREAD_ACTIVITY, "After waiting");
		}
	}

	private Move selectBestMove(MachineState currentState, List<Move> moves) throws MoveDefinitionException
	{
		Map<Move, Integer> moveScore = new HashMap<Move, Integer>();
		Map<Move, Integer> moveCount = new HashMap<Move, Integer>();
		int roleIndex = utility.getPlayerRoleIndex();

		for (Node child : root.children())
		{
			Move m = child.move().get(roleIndex);

			moveScore.put(m, 0);
			moveCount.put(m, 0);
		}

		boolean opponentHasMoves = utility.opponentHasMoves(currentState);

		for (Node child : root.children())
		{
			Move m = child.move().get(roleIndex);

			// see if we can find an immediate winning move
			if (!opponentHasMoves && utility.isWinningState(child.state()))
			{
				log.info(GLog.MOVE_EVALUATION, "Performing winning move");
				log.info(GLog.MOVE_EVALUATION, m);
				log.info(GLog.MOVE_EVALUATION, "Congratulations");
				return m;
			}

			int score;
			if (child.visit() == 0)
			{
				score = 0;
			}
			else
			{
				score = child.utility() / child.visit();
			}

			moveScore.put(m, moveScore.get(m) + score);
			moveCount.put(m, moveCount.get(m) + 1);
		}

		int maxScore = 0;
		Move bestMove = null;
		for (Entry<Move, Integer> move : moveScore.entrySet())
		{
			Move m = move.getKey();
			int count = moveCount.get(m);
			int score = 0;
			if (count > 0)
			{
				score = move.getValue() / moveCount.get(m);
			}

			log.info(GLog.MOVE_EVALUATION, "Considering move");
			log.info(GLog.MOVE_EVALUATION, score + " : " + m);

			if (score > maxScore)
			{
				maxScore = score;
				bestMove = m;
			}
		}

		log.info(GLog.MOVE_EVALUATION, "Best move: ");
		log.info(GLog.MOVE_EVALUATION, maxScore + " : " + bestMove);

		return bestMove;
	}

	private void updateRoot(MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException
	{
		if (root == null)
		{
			root = NodePool.newNode(null, currentState, null, true, null);
			if (root == null)
			{
				throw new IllegalStateException("No memory can be obtained for even one node");
			}
		}
		else
		{
			Node childNode = childStates.get(currentState);

			if (childNode != null)
			{
				root = childNode;
				root.orphan();
			}
			else
			{
				root.orphan();
				root = NodePool.newNode(null, currentState, null, true, null);
				log.error(GLog.ERRORS,
						"Missed finding the tree - investigate");

				expandWithSubgame(root);

				root.visitIncrement(1);
				root.select();
			}

			childStates.clear();
		}
	}

	private void setMetaTimeout(long timeout)
	{
		if (timeout != 0)
		{
			setTimeout(timeout + MachineParameters.META_TIME_PAD);
		}
		else
		{
			setTimeout(0);
		}
	}

	private void setTimeout(long timeout)
	{
		if (timeout != 0)
		{
			endTime = timeout - MachineParameters.TIME_BUFFER;
		}
		else
		{
			endTime = 0;
		}
	}

	private boolean isTimeout()
	{
		Date now = new Date();
		long nowTime = now.getTime();

		return nowTime >= endTime;
	}

	private long getRemainingTime()
	{
		Date now = new Date();
		long nowTime = now.getTime();

		return endTime - nowTime;
	}

	@Override
	public StateMachine getInitialStateMachine()
	{
		if (RuntimeParameters.UNITTEST_PROPNET)
		{
			return new UnitTestStateMachine(new PropNetStateMachine(), new ProverStateMachine());
		}

		return new CachedStateMachine(new PropNetStateMachine());
	}

	private static void printParameters()
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Running with the following parameters: ");
		log.info(GLog.MAIN_THREAD_ACTIVITY, "# of threads: " + MachineParameters.NUM_CORES);
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Exploration parameter: " + RuntimeParameters.EXPLORATION_FACTOR);
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Depth charge count: " + RuntimeParameters.DEPTH_CHARGE_COUNT);
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Time buffer: " + MachineParameters.TIME_BUFFER);
		log.info(GLog.MAIN_THREAD_ACTIVITY, "Minimax: " + RuntimeParameters.MINIMAX);
	}
}
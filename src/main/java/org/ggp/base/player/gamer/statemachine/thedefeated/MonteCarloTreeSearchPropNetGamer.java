package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.Logger;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.player.gamer.statemachine.sample.TreeSearchWorker;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearchPropNetGamer extends SampleGamer
{
	private static final Logger log = GLog.getLogger(MonteCarloTreeSearchGamer.class);
	private long endTime = 0;

	private Thread[] threads = new Thread[MachineParameters.NUM_CORES];
	private TreeSearchWorker[] workers = new TreeSearchWorker[MachineParameters.NUM_CORES];
	private Map<MachineState, Node> childStates = new HashMap<>();
	private Node root = null;
	private GameUtilities utility = null;

	private Timer timer = null;
	private ThreadTimer threadTimer = null;

	static
	{
		printParameters();
	}

	class ThreadTimer extends TimerTask
	{
		private boolean disable;
		private Thread wakeupThread = null;

		public ThreadTimer(Thread thread)
		{
			this.wakeupThread = thread;
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
				wakeupThread.interrupt();
				log.info(GLog.MAIN_THREAD_ACTIVITY,
						"Waking up main thread from sleep");
			}
		}
	}
	@Override
	public StateMachine getInitialStateMachine() {
        return new PropNetStateMachine();
    }

	public MonteCarloTreeSearchPropNetGamer()
	{
		Thread.currentThread().setPriority(MachineParameters.MAIN_THREAD_PRIORITY);
		for (int i = 0; i < workers.length; i++)
		{
			workers[i] = new TreeSearchWorker(i);
			threads[i] = new Thread(workers[i]);
		}

		timer = new Timer();
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
		stopWorkers();
		root = null;
		childStates.clear();

		TreeSearchWorker.printStats();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				GLog.BANNER + " Beginning meta game " + GLog.BANNER);
		setTimeout(timeout + 1000);

		StateMachine stateMachine = getStateMachine();
		Role role = getRole();

		TreeSearchWorker.globalInit();

		for (int i = 0; i < workers.length; i++)
		{
			workers[i].init(stateMachine, role);
		}

		utility = new GameUtilities(stateMachine, role);

		childStates.clear();

		updateRoot(getCurrentState());
		childStates.put(root.state, root);

		updateWorkers(root);
		startWorkers();

		waitForTimeout();

		setTimeout(0);
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				GLog.BANNER + " Ending meta game " + GLog.BANNER);
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				GLog.BANNER + " Beginning move selection " + GLog.BANNER);

		setTimeout(timeout);

		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();

		updateRoot(currentState);
		updateWorkers(root);

		for (Node child : root.children)
		{
			childStates.put(child.state, child);
		}

		log.info(GLog.MAIN_THREAD_ACTIVITY, "Before waiting...");
		waitForTimeout();
		log.info(GLog.MAIN_THREAD_ACTIVITY, "After waiting");

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


		log.info(GLog.MAIN_THREAD_ACTIVITY,
				GLog.BANNER + " Ending move selection " + GLog.BANNER);
		TreeSearchWorker.printStats();

		return bestMove;
	}

	private void startWorkers()
	{
		for (int i = 0; i < workers.length; i++)
		{
			threads[i].setName("TreeSearchWorker-" + i);
			threads[i].setPriority(MachineParameters.WORKER_THREAD_PRIORITY);
			threads[i].start();
		}
	}

	private void stopWorkers()
	{
		for (int i = 0; i < workers.length; i++)
		{
			threads[i].interrupt();
			threads[i] = new Thread(workers[i]);
		}
	}

	private void waitForTimeout()
	{
		while (!isTimeout())
		{
			try
			{
				threadTimer = new ThreadTimer(Thread.currentThread());
				timer.schedule(threadTimer, getRemainingTime());
				Thread.sleep(getRemainingTime());
				threadTimer.disable();
			}
			catch (InterruptedException e)
			{
			}

			threadTimer = null;
		}
	}

	private Move selectBestMove(MachineState currentState, List<Move> moves) throws MoveDefinitionException
	{
		Map<Move, Integer> moveScore = new HashMap<Move, Integer>();
		Map<Move, Integer> moveCount = new HashMap<Move, Integer>();
		for (Move m : moves)
		{
			moveScore.put(m, 0);
			moveCount.put(m, 0);
		}

		int roleIndex = utility.getPlayerRoleIndex();

		boolean opponentHasMoves = utility.opponentHasMoves(currentState);

		for (Node child : root.children)
		{
			Move m = child.move.get(roleIndex);

			// see if we can find an immediate winning move
			if (!opponentHasMoves && utility.isWinningState(child.state))
			{
				log.info(GLog.MOVE_EVALUATION,
						"Performing winning move");
				log.info(GLog.MOVE_EVALUATION, m);
				log.info(GLog.MOVE_EVALUATION,
						"Congratulations");
				return m;
			}

			int score;
			if (child.visit == 0)
			{
				score = 0;
			}
			else
			{
				score = child.utility / child.visit;
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

			log.info(GLog.MOVE_EVALUATION,
					"Considering move");
			log.info(GLog.MOVE_EVALUATION,
					score + " : " + m);

			if (score > maxScore)
			{
				maxScore = score;
				bestMove = m;
			}
		}

		log.info(GLog.MOVE_EVALUATION,
				"Best move: ");
		log.info(GLog.MOVE_EVALUATION,
				maxScore + " : " + bestMove);

		return bestMove;
	}

	private void updateRoot(MachineState currentState)
	{
		if (root == null)
		{
			root = new Node(null, currentState, null, true);
		}
		else
		{
			Node childNode = childStates.get(currentState);

			if (childNode != null)
			{
				root = childNode;
				root.parent = null;
			}
			else
			{
				root = new Node(null, currentState, null, true);
				log.error(GLog.ERRORS, "Missed finding the tree - investigate");
			}

			childStates.clear();
		}
	}

	private void updateWorkers(Node root) throws MoveDefinitionException, TransitionDefinitionException
	{
		for (int i = 0; i < workers.length; i++)
		{
			workers[i].setRoot(root);
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

	private static void printParameters()
	{
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"Running with the following parameters: ");
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"# of threads: " + MachineParameters.NUM_CORES);
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"Exploration parameter: " + RuntimeParameters.EXPLORATION_FACTOR);
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"Depth charge count: " + RuntimeParameters.DEPTH_CHARGE_COUNT);
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"Time buffer: " + MachineParameters.TIME_BUFFER);
		log.info(GLog.MAIN_THREAD_ACTIVITY,
				"Minimax: " + RuntimeParameters.MINIMAX);
	}
}

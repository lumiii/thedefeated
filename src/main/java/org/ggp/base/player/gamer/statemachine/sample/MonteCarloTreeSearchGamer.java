package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearchGamer extends SampleGamer
{
	private long endTime = 0;

	private Thread[] threads = new Thread[MachineParameters.NUM_CORES];
	private TreeSearchWorker[] workers = new TreeSearchWorker[MachineParameters.NUM_CORES];
	private Map<MachineState, Node> childStates = new HashMap<>();
	private Node root = null;
	private GameUtilities utility = null;

	private Timer timer = null;
	private ThreadTimer threadTimer = null;

	class ThreadTimer extends TimerTask
	{
		Thread wakeupThread = null;
		public ThreadTimer(Thread thread)
		{
			this.wakeupThread = thread;
		}

		public void setThread(Thread thread)
		{
			this.wakeupThread = thread;
		}

		@Override
		public void run()
		{
			if (wakeupThread != null)
			{
				wakeupThread.interrupt();
				System.out.println("Waking up thread");
			}
		}
	}

	public MonteCarloTreeSearchGamer()
	{
		printParameters();
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
		System.out.println("Stop called");
		stopWorkers();
		endGame();
	}

	@Override
	public void stateMachineAbort()
	{
		System.out.println("Abort called");
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
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		setTimeout(timeout);

		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();

		updateRoot(currentState);
		updateWorkers(root);

		System.out.println("Before waiting");
		waitForTimeout();
		System.out.println("After waiting");

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
			System.out.println("Playing random move: " + bestMove);
		}

		for (Node child : root.children)
		{
			childStates.put(child.state, child);
		}

		setTimeout(0);

		System.out.println("All done");
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
				threadTimer.setThread(null);
			}
			catch (InterruptedException e)
			{
				if (Thread.interrupted())
				{
					System.out.println("Main thread correctly interrupted");
				}
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
			// System.out.println(m.getContents());
		}

		int roleIndex = utility.getPlayerRoleIndex();

		boolean opponentHasMoves = utility.opponentHasMoves(currentState);

		for (Node child : root.children)
		{
			Move m = child.move.get(roleIndex);

			// see if we can find an immediate winning move
			if (!opponentHasMoves && utility.isWinningState(child.state))
			{
				System.out.println("Performing winning move");
				System.out.println(m);
				System.out.println("Congratulations");
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

			// System.out.println(m.getContents());
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

			System.out.println("Considering move");
			System.out.println(score + " : " + m);
			if (score > maxScore)
			{
				maxScore = score;
				bestMove = m;
			}
		}

		System.out.println("Best move: ");
		System.out.println(maxScore + " : " + bestMove);

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
				System.out.println("Missed finding the tree - investigate");
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
			endTime = timeout - RuntimeParameters.TIME_BUFFER;
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

	private void printParameters()
	{
		System.out.println("Running with the following: ");
		System.out.println("# of threads: " + MachineParameters.NUM_CORES);
		System.out.println("Exploration parameter: " + RuntimeParameters.EXPLORATION_FACTOR);
		System.out.println("Depth charge count: " + RuntimeParameters.DEPTH_CHARGE_COUNT);
		System.out.println("Time buffer: " + RuntimeParameters.TIME_BUFFER);
		System.out.println("Minimax: " + RuntimeParameters.MINIMAX);
	}
}
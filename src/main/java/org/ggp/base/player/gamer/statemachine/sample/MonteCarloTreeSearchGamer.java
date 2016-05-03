package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	private Thread[] threads = new Thread[Parameters.NUM_CORES];
	private TreeSearchWorker[] workers = new TreeSearchWorker[Parameters.NUM_CORES];
	private Map<MachineState, Node> childStates = new HashMap<>();
	private Node root = null;
	private GameUtilities utility;

	public MonteCarloTreeSearchGamer()
	{
		Thread.currentThread().setPriority(Parameters.MAIN_THREAD_PRIORITY);
		for (int i = 0; i < workers.length; i++)
		{
			workers[i] = new TreeSearchWorker(i);
			threads[i] = new Thread(workers[i]);
		}
	}

	@Override
	public void stateMachineStop()
	{
		System.out.println("Stop called");
		stopWorkers();
	}

	@Override
	public void stateMachineAbort()
	{
		System.out.println("Abort called");
		stopWorkers();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		setTimeout(timeout + 1000);

		StateMachine stateMachine = getStateMachine();
		Role role = getRole();

		TreeSearchWorker.globalInit(workers.length);

		for (int i = 0; i < workers.length; i++)
		{
			workers[i].init(stateMachine, role);
		}

		utility = new GameUtilities(stateMachine, role);

		childStates.clear();

		root = new Node(null, getCurrentState(), null, true);

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
			threads[i].setPriority(Parameters.WORKER_THREAD_PRIORITY);
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

		TreeSearchWorker.printStats();
	}

	private void waitForTimeout()
	{
		while (!isTimeout())
		{
			try
			{
				Thread.sleep(getRemainingTime());
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	private Move selectBestMove(MachineState currentState, List<Move> moves) throws MoveDefinitionException
	{
		StateMachine stateMachine = getStateMachine();
		Map<Move, Integer> moveScore = new HashMap<Move, Integer>();
		Map<Move, Integer> moveCount = new HashMap<Move, Integer>();
		for (Move m : moves)
		{
			moveScore.put(m, 0);
			moveCount.put(m, 0);
			// System.out.println(m.getContents());
		}
		int roleIndex = 0;
		for (roleIndex = 0; !stateMachine.getRoles().get(roleIndex).equals(getRole()); roleIndex++)
			;

		boolean opponentHasMoves = utility.opponentHasMoves(currentState);

		for (Node child : root.children)
		{
			Move m = child.move.get(roleIndex);

			// see if we can find an immediate winning move
			if (!opponentHasMoves && utility.isWinningState(child.state))
			{
				{
					System.out.println("Performing winning move");
					System.out.println(m);
					return m;
				}
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

			if (score > maxScore)
			{
				maxScore = score;
				bestMove = m;
			}
		}

		System.out.println("Best score: " + maxScore);
		System.out.println("Best move: " + bestMove);

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
			endTime = timeout - Parameters.TIME_BUFFER;
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
}
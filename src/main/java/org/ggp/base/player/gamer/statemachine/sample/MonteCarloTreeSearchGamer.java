package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

	// private ExecutorService threadPool;
	private Thread[] threads = new Thread[Parameters.NUM_CORES];
	private TreeSearchWorker[] workers = new TreeSearchWorker[Parameters.NUM_CORES];
	private Map<MachineState, Node> grandchildren = new HashMap<>();
	private Set<MachineState> previousStates = new HashSet<>();
	private Node root = null;
	private GameUtilities utility;

	public MonteCarloTreeSearchGamer()
	{
		Thread.currentThread().setPriority(Parameters.MAIN_THREAD_PRIORITY);
		// get a thread safe set via a map
		for (int i = 0; i < workers.length; i++)
		{
			workers[i] = new TreeSearchWorker(i);
			threads[i] = new Thread(workers[i]);
		}

		// threadPool = Executors.newFixedThreadPool(Parameters.NUM_CORES);
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

		for (int i = 0; i < workers.length; i++)
		{
			workers[i].init(stateMachine, role);
		}

		utility = new GameUtilities(stateMachine, role);

		grandchildren.clear();
		previousStates.clear();
		previousStates.add(getCurrentState());

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

		// TODO: evaluate whether we really need this or not
		previousStates.add(currentState);

		if (root == null)
		{
			root = new Node(null, currentState, null, true);
		}
		else
		{
			// TODO: write this as a neater and more general pruning algorithm
			Node n = grandchildren.get(currentState);
			if (n != null)
			{
				System.out.println("Found tree");
				root = n;
				root.parent = null;
			}
			else
			{
				root = new Node(null, currentState, null, true);
			}

			grandchildren.clear();
		}

		updateWorkers(root);

		System.out.println("Before waiting");
		waitForTimeout();
		System.out.println("After waiting");

		Role role = getRole();

		boolean singleMove = (stateMachine.findLegals(role, currentState).size() == 1);

		Move bestMove = null;

		//if (singleMove)
		if(false)
		{
			bestMove = stateMachine.findLegalx(role, currentState);
		}
		else
		{
			System.out.println("Computing score...");
			int maxScore = 0;

			System.out.println("Iterating through " + root.children.size());

			Map<Move,Integer> moveScore = new HashMap<Move,Integer>();
			Map<Move,Integer> moveCount = new HashMap<Move,Integer>();
			for (Move m : stateMachine.findLegals(getRole(), currentState))
			{
				moveScore.put(m, 0);
				moveCount.put(m, 0);
				//System.out.println(m.getContents());
			}
			int roleIndex =0;
			for(roleIndex =0; !stateMachine.getRoles().get(roleIndex).equals(getRole());roleIndex++);
			for (Node child : root.children)
			{
				int score;
				if (child.visit == 0)
				{
					score = 0;
				}
				else
				{
					score = child.utility / child.visit;
				}
				Move m = child.move.get(roleIndex);
				//System.out.println(m.getContents());
				moveScore.put(m, moveScore.get(m)+score);
				moveCount.put(m, moveCount.get(m)+1);

			}

			for(Entry<Move, Integer> move : moveScore.entrySet())
			{
				Move m = move.getKey();
				int score = moveScore.get(m)/moveCount.get(m);
				if(score > maxScore)
				{
					maxScore= score;
					bestMove = m;
				}
			}

			System.out.println("Finished iterating");
		}

		// make sure to have at least one move to return
		// in the case that all scores come back as 0

		// TODO: hacky, see if there's a better way to do this
		if (bestMove == null)
		{
			System.out.println("No best move, getting unvisited");
			bestMove = getUnvisitedMove();
			System.out.println("Finished getting unvisited");
			if (bestMove == null)
			{
				bestMove = stateMachine.findLegalx(getRole(), currentState);
			}
		}

		// TODO: hack for tree pruning. Do this properly
		System.out.println("Adding all grandchildren");
		for (Node child : root.children)
		{
			grandchildren.put(child.state, child);
			//for (Node grandchild : child.children)
			//{
				//grandchildren.put(grandchild.state, grandchild);
			//}
		}

		setTimeout(0);

		System.out.println("All done");
		return bestMove;
	}

	private Move getUnvisitedMove() throws MoveDefinitionException, TransitionDefinitionException
	{
		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();
		Role role = getRole();
		List<Move> moveList = stateMachine.findLegals(role, currentState);
		for (Move move : moveList)
		{
			List<Move> jointMove = utility.getOrderedMoves(move, role, currentState);
			MachineState nextState = stateMachine.findNext(jointMove, currentState);
			if (!previousStates.contains(nextState))
			{
				return move;
			}
		}

		return null;
	}

	private void startWorkers()
	{
		for (int i = 0; i < workers.length; i++)
		{
			// threadPool.submit(workers[i]);
			threads[i].setName("TreeSearchWorker-" + i);
			threads[i].setPriority(Parameters.WORKER_THREAD_PRIORITY);
			threads[i].start();
		}
	}

	private void stopWorkers()
	{
		// threadPool.shutdownNow();
		// threadPool = Executors.newFixedThreadPool(Parameters.NUM_CORES);
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
				Thread.sleep(getRemainingTime());
			}
			catch (InterruptedException e)
			{
			}
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
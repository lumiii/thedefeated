package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearchGamer extends SampleGamer
{
	// amount of time to buffer before the timeout
	private static final long TIME_BUFFER = 2000;
	private static final int NUM_CORES = 2;

	private long endTime = 0;

	private ExecutorService threadPool;
	private ExecutorCompletionService<Integer> completionQueue;
	private TreeSearchWorker[] workers = new TreeSearchWorker[NUM_CORES];
	private Map<MachineState, Node> grandchildren = new HashMap<>();
	private Set<MachineState> previousStates = new HashSet<>();
	private Node root = null;
	private GameUtilities utility;

	public MonteCarloTreeSearchGamer()
	{
		// get a thread safe set via a map
		for (int i = 0; i < workers.length; i++)
		{
			workers[i] = new TreeSearchWorker(i);
		}

		threadPool = Executors.newFixedThreadPool(NUM_CORES);
		completionQueue = new ExecutorCompletionService<Integer>(threadPool);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		setTimeout(timeout + 1000);

		for (int i = 0; i < workers.length; i++)
		{
			workers[i].setStateMachine(getStateMachine());
		}

		utility = new GameUtilities(getStateMachine(), getRole());

		grandchildren.clear();
		previousStates.add(getCurrentState());

		root = new Node(null, getCurrentState(), null, true);
		treeSearch(root);

		setTimeout(0);
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		setTimeout(timeout);

		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();

		previousStates.add(getCurrentState());

		boolean singleMove = (stateMachine.findLegals(getRole(), currentState).size() == 1);

		if (root == null)
		{
			root = new Node(null, currentState, null, true);
		}
		else
		{
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


		treeSearch(root);

		// make sure to have at least one move to return
		// in the case that all scores come back as 0
		Move bestMove = null;

		if (!singleMove)
		{
			int maxScore = 0;
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
				System.out.println(child.move.getContents().toString() + "," + score);
				if (score > maxScore)
				{
					maxScore = score;
					bestMove = child.move;
				}
			}
		}

		if (bestMove == null)
		{
			bestMove = getUnvisitedMove();
			if (bestMove == null)
			{
				bestMove = stateMachine.findLegalx(getRole(), currentState);
			}
		}

		for (Node child : root.children)
		{
			grandchildren.put(child.state, child);
			for (Node grandchild : child.children)
			{
				grandchildren.put(grandchild.state, grandchild);
			}
		}

		setTimeout(0);

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

	private void treeSearch(Node root) throws MoveDefinitionException, TransitionDefinitionException
	{
		Role role = getRole();

		for (int i = 0; i < workers.length; i++)
		{
			TreeSearchWorker worker = workers[i];
			worker.set(role, root);
			System.out.println("Submitting worker : " + worker.getId());
			completionQueue.submit(worker);
		}

		while (!isTimeout())
		{
			try
			{
				Future<Integer> future = completionQueue.take();
				int index = future.get();
				System.out.println("Submitting worker : " + index);
				completionQueue.submit(workers[index]);
			} catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}

		// drain the thread pool
		Future<Integer> future = completionQueue.poll();
		while (future != null)
		{
			future.cancel(false);
			future = completionQueue.poll();
		}
	}

	private void setTimeout(long timeout)
	{
		if (timeout != 0)
		{
			endTime = timeout - TIME_BUFFER;
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
}
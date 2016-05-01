package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;
import java.util.concurrent.Callable;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class TreeSearchWorker implements Callable<Integer>
{
	private static final int EXPLORATION_FACTOR = 50;
	private static final int DEPTH_CHARGE_COUNT = 4;
	private volatile static int nodesVisited = 0;
	private volatile static int nodesUpdated = 0;

	private int id;
	private GameUtilities utility;
	private StateMachine stateMachine;
	private Role role;
	private Node root;

	public TreeSearchWorker(int id)
	{
		this.id = id;
	}

	public void setStateMachine(StateMachine stateMachine)
	{
		this.stateMachine = new CachedStateMachine(stateMachine);
	}

	public void set(Role role, Node root)
	{
		this.role = role;
		this.root = root;
		this.utility = new GameUtilities(this.stateMachine, role);
	}

	public int getId()
	{
		return id;
	}

	@Override
	public Integer call() throws Exception
	{
		System.out.println("Starting worker" + getId() + " thread " + Thread.currentThread().getName());
		treeSearch();
		System.out.println("Stopping worker" + getId() + " thread " + Thread.currentThread().getName());

		return getId();
	}

	private void treeSearch() throws MoveDefinitionException, TransitionDefinitionException
	{
		Node node = select(root);

		if (node != null)
		{
			nodesVisited++;

			expand(node);
			if (!stateMachine.findTerminalp(node.state))
			{
				int visits = 0;
				int totalScore = 0;

				for (int i = 0; i < DEPTH_CHARGE_COUNT; i++)
				{
					MachineState terminalState;
					try
					{
						terminalState = stateMachine.performDepthCharge(node.state, null);
						totalScore += stateMachine.getGoal(terminalState, role);
						visits++;
					} catch (GoalDefinitionException | TransitionDefinitionException | MoveDefinitionException e)
					{
						e.printStackTrace();
					}
				}

				backPropogate(node, totalScore, visits);
			}
			else
			{
				int score = 0;
				try
				{
					score = stateMachine.getGoal(node.state, role);
					backPropogate(node, score, 1);
				} catch (GoalDefinitionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			System.out.println("From thread " + Thread.currentThread().getName());
			System.out.println("Nodes visited: " + nodesVisited);
			System.out.println("Nodes updated: " + nodesUpdated);
		}
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

		double score = 0;
		Node result = node;
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

		if (result != node)
		{
			return select(result);
		}

		return null;

	}

	private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException
	{
		List<Move> moves = stateMachine.findLegals(role, node.state);
		for (Move m : moves)
		{
			List<Move> nextMoves = utility.getOrderedMoves(m, role, node.state);
			MachineState newState = stateMachine.getNextState(node.state, nextMoves);
			Node newNode = new Node(node, newState, m, !node.max);
			node.children.add(newNode);
		}
	}

	private void backPropogate(Node node, int totalScore, int visits)
	{
		synchronized (node)
		{
			node.visit += visits;
			node.utility += totalScore;
			nodesUpdated += visits;
		}

		if (node.parent != null)
		{
			backPropogate(node.parent, totalScore, visits);
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

			return (node.utility / node.visit
				+ EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(node.parent.visit) / node.visit));
		}
	}
}
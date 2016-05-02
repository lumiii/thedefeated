package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

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

	private int id;

	private StateMachine stateMachine;

	private GameUtilities utility;
	private Role role;
	private Node root;

	private Node newRoot;

	public TreeSearchWorker(int id)
	{
		this.id = id;
	}

	public void init(StateMachine stateMachine, Role role)
	{
		this.stateMachine = new CachedStateMachine(stateMachine);
		this.role = role;
		this.utility = new GameUtilities(this.stateMachine, role);
		this.root = null;
		this.newRoot = null;

		nodesVisited = 0;
		nodesUpdated = 0;
	}

	public void setRoot(Node root)
	{
		this.newRoot = root;
	}

	private void update()
	{
		this.root = this.newRoot;
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
				update();
				treeSearch();
			}
			// catch all exceptions
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
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

				backPropogate(node, totalScore, visits);
			}
			else
			{
				try
				{
					int score = stateMachine.getGoal(node.state, role);
					backPropogate(node, score, 1);
				}
				catch (GoalDefinitionException e)
				{
					// this is a common occurrence
					// just ignore it
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

	private List<List<Move>> findAllMoves(Node node) throws MoveDefinitionException
	{
		List<List<Move>> moves = new ArrayList<List<Move>>();
		List<Role> roles = stateMachine.getRoles();
		int numRoles = roles.size();
		for(int i =0; i<numRoles*numRoles; i++)
		{
			moves.add(new ArrayList<Move>());
		}
		for(Role r : stateMachine.getRoles())
		{
			List<Move> roleMoves = stateMachine.findLegals(r, node.state);
			List<List<Move>> newMoves = new ArrayList<List<Move>>();

			for (List<Move> l : moves)
			{

				for(Move m : roleMoves)
				{
					ArrayList<Move> tempList = new ArrayList<Move>(l);
					tempList.add(m);
					newMoves.add(tempList);
				}
			}
			moves = newMoves;
		}

		return moves;
	}

	private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException
	{
		List<List<Move>> moves = findAllMoves(node);
		for (List<Move> m : moves)
		{
			//List<Move> nextMoves = utility.getOrderedMoves(m, role, node.state);
			MachineState newState = stateMachine.getNextState(node.state, m);
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
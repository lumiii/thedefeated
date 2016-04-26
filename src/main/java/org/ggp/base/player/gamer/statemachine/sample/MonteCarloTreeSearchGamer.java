package org.ggp.base.player.gamer.statemachine.sample;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearchGamer extends SampleGamer {

	class Node
	{
		public int utility;
		public int visit;
		public Node parent;
		public ArrayList<Node> children;
		public MachineState state;
		public Boolean max;
		public Move move;

		public Node(Node par, MachineState stat, Move m, Boolean maxBool)
		{
			utility =0;
			visit = 0;
			parent = par;
			children = new ArrayList<Node>();
			state = stat;
			move = m;
			max = maxBool;

		}

	}

	// amount of time to buffer before the timeout
	private static final long TIME_BUFFER = 3000;
	private static final long META_TIME_BUFFER = 1000;
	private static final int MAX_DEFAULT = 0;
	private static final int MIN_DEFAULT = 100;
	private static final int WORST_MAX = 100;
	private static final int WORST_MIN = 0;

	private static final int DEPTH_DEFAULT = 0;

	private long endTime = 0;
	private long endMetaTime = 0;

	private MachineState bestState = null;
	private int bestScore = 0;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		Move selectMove = null;
		setTimeout(timeout);

		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();
		Node root = new Node(null, currentState, null, true);
		if(stateMachine.findLegals(getRole(), currentState).size()==1)
		{
			return stateMachine.findLegalx(getRole(), currentState);
		}
		Role role = getRole();
		while(!isTimeout())
		{
			Node node = select(root);
			expand(node);
			int[] outDepth = {0};
			if(!stateMachine.findTerminalp(node.state))
			{
				MachineState terminalState = stateMachine.performDepthCharge(node.state, outDepth);
				int currentScore = stateMachine.getGoal(terminalState, role);
				backPropogate(node, currentScore);
			}
		}
		int maxScore =0;
		Move bestMove = null;
		for (Node child : root.children)
		{
			int score;
			if(child.visit ==0)
			{
				score =0;
			}
			else
			{
				score = child.utility/child.visit;
			}
			System.out.println(child.move.getContents().toString() + "," + score );
			if(score > maxScore)
			{
				maxScore = score;
				bestMove = child.move;
			}
		}

		setTimeout(0);

		return bestMove;
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {

    }

	private Node select(Node node)
	{
		if(node.visit==0)
		{
			return node;
		}
		for(Node child : node.children)
		{
			if(child.visit==0)
			{
				return child;
			}
		}
		double score =0;
		Node result = node;
		for(Node child : node.children)
		{
			double newScore = selectFn(child);
			//System.out.println(newScore);
			if (newScore > score)

			{
				score = newScore;
				result = child;
			}
		}
		return select(result);
	}

	private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException
	{
		Role role = getRole();
		StateMachine stateMachine = getStateMachine();

		List<Move> moves = stateMachine.findLegals(role, node.state);
		for(Move m : moves)
		{
			List<Move> nextMoves = getOrderedMoves(m, role, node.state);
			MachineState newState = stateMachine.getNextState(node.state, nextMoves);
			Node newNode = new Node(node, newState, m, !node.max);
			node.children.add(newNode);
		}
	}

	private void backPropogate(Node node, int score)
	{
		node.visit++;
		node.utility += score;
		//if(node.move != null)
		//	System.out.println(node.move.getContents().toString()+"," + node.visit + "," + node.utility);

		if(node.parent != null)
		{
			backPropogate(node.parent, score);
		}
	}
	private double selectFn(Node node)
	{
		return  (node.utility/node.visit + 50* Math.sqrt(2*Math.log(node.parent.visit)/node.visit));
	}
	private boolean isSinglePlayer() {
		StateMachine stateMachine = getStateMachine();
		List<Role> roles = stateMachine.findRoles();

		return (roles.size() == 1);
	}

	public SimpleImmutableEntry<Move, Integer>montecarlo(MachineState currentState, int count) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		StateMachine stateMachine = getStateMachine();
		Role role = getRole();
		int total = 0;
		int [] theDepth = {0};
		for(int i=0; i<count; i++)
		{

			MachineState terminalState = stateMachine.performDepthCharge(getCurrentState(), theDepth);
			total += stateMachine.getGoal(terminalState, role);
		}
		int score = total/ count;
		System.out.println("Monte Carlo score of " + score);
		return new SimpleImmutableEntry(currentState, score);
	}

	private SimpleImmutableEntry<Move, Integer> getMaxMove(
			MachineState currentState,
			int alpha,
			int beta,
			int depth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

		StateMachine stateMachine = getStateMachine();




		if (depth < 0) {
			System.out.println("Hit max bottom, returning");
			return montecarlo(currentState, 100);
		}

		List<Move> moves = stateMachine.findLegals(getRole(), currentState);

		Move bestMove = stateMachine.findLegalx(getRole(), currentState);
		for (Move move : moves) {
			if (isTimeout()) {
				System.out.println("Out of time, playing best guess.");
				break;
			}
			if(depth == DEPTH_DEFAULT)
			{
				System.out.println(move.getContents());
			}
			List<Move> nextMoves = getOrderedMoves(move, getRole(), currentState);

			MachineState nextState = null;
			try {
				nextState = stateMachine.findNext(nextMoves, currentState);
			} catch (TransitionDefinitionException e1) {
				e1.printStackTrace();
				continue;
			}

			int nextScore = MAX_DEFAULT;
			try {
				if (!isSinglePlayer()) {
					nextScore = getMinScore(nextState, alpha, beta, depth - 1);
				}
				else {
					nextScore = getMaxScore(nextState, alpha, beta, depth - 1);
				}

				if (nextScore > alpha) {
					alpha = nextScore;
					bestMove = move;
				}

				if(alpha >= beta)
				{
					break;
				}
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
				continue;
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
				continue;
			}
		}

		SimpleImmutableEntry<Move, Integer> maxMove = new SimpleImmutableEntry<Move, Integer>(bestMove, alpha);

		return maxMove;
	}

	private int getMaxScore(
			MachineState currentState,
			int alpha,
			int beta,
			int depth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		StateMachine stateMachine = getStateMachine();

		int maxScore = MAX_DEFAULT;

		if (stateMachine.findTerminalp(currentState)) {
			maxScore = stateMachine.findReward(getRole(), currentState);
		}
		else {
			SimpleImmutableEntry<Move, Integer> maxMove = getMaxMove(currentState, alpha, beta, depth);
			maxScore = maxMove.getValue();
		}

		return maxScore;
	}

	private int getMinScore(MachineState currentState,
			int alpha,
			int beta,
			int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine stateMachine = getStateMachine();

		Role opponent = getOpponent();

		if (stateMachine.findTerminalp(currentState)) {
			int minScore = stateMachine.findReward(getRole(), currentState);
			return minScore;
		}
		else {


			//depth--;
			if (depth < 0) {
				System.out.println("Hit min bottom, returning");
				return montecarlo(currentState, 100).getValue();
			}

			List<Move> moves = stateMachine.findLegals(opponent, currentState);

			for (Move move : moves) {
				List<Move> nextMoves = getOrderedMoves(move, opponent, currentState);
				if (isTimeout()) {
					System.out.println("Out of time, playing best guess.");
					break;
				}
				MachineState nextState = null;
				try {
					nextState = stateMachine.findNext(nextMoves, currentState);
				} catch (TransitionDefinitionException e1) {
					e1.printStackTrace();
					continue;
				}

				int nextScore = MIN_DEFAULT;
				try {
					nextScore = getMaxScore(nextState, alpha, beta, depth);

					if (beta > nextScore) {
						beta = nextScore;
					}
					if(beta <= alpha)
					{
						return beta;
					}
				} catch (GoalDefinitionException e) {
					e.printStackTrace();
					continue;
				} catch (MoveDefinitionException e) {
					e.printStackTrace();
					continue;
				}
			}

			return beta;
		}
	}

	private Role getOpponent() {
		List<Role> roles = getStateMachine().findRoles();
		if (roles.size() > 2) {
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

		for (Role role : roles) {
			if (!role.equals(getRole())) {
				return role;
			}
		}

		return null;
	}

	private List<Move> getOrderedMoves(Move move, Role moveOwner, MachineState currentState) throws MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<Role> roles = stateMachine.findRoles();

		if (roles.size() > 2) {
			throw new ArrayIndexOutOfBoundsException("Unexpected: more than 2 players");
		}

		List<Move> moves = new ArrayList<Move>();

		for (Role role : roles) {
			if (moveOwner.equals(role)) {
				moves.add(move);
			}
			else {
				moves.add(stateMachine.findLegalx(role, currentState));
			}
		}

		return moves;
	}

	private void setTimeout(long timeout) {
		if (timeout != 0){
			endTime = timeout - TIME_BUFFER;
		}
		else {
			endTime = 0;
		}
	}

	private boolean isTimeout() {
		Date now = new Date();
		long nowTime = now.getTime();

		return nowTime >= endTime;
	}

	private void setMetaTimeout(long timeout) {
		if (timeout != 0){
			endMetaTime = timeout - META_TIME_BUFFER;
		}
		else {
			endMetaTime = 0;
		}
	}

	private boolean isMetaTimeout() {
		Date now = new Date();
		long nowTime = now.getTime();

		return nowTime >= endMetaTime;
	}
}
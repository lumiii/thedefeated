package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.Component.Type;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

@SuppressWarnings("unused")
public class PropNetStateMachine extends StateMachine
{
	private static final Logger log = GLog.getLogger(PropNetStateMachine.class);
	/** The underlying proposition network */
	private PropNet propNet;
	/** The player roles */
	private List<Role> roles;

	private Set<Proposition> trueBaseProps;
	private Set<Proposition> trueInputProps;

	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you may compute the initial state here, at
	 * your discretion.
	 */
	@Override
	public void initialize(List<Gdl> description)
	{
		try
		{
			log.info(GLog.PROPNET,
				"Initializing prop net");

			propNet = OptimizingPropNetFactory.create(description);
			roles = propNet.getRoles();
			trueBaseProps = new LinkedHashSet<Proposition>();
			trueInputProps = new LinkedHashSet<Proposition>();
			//setOrdering();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void clearPropNet()
	{
		// TODO: this isn't actually what we want, because base/input
		// propositions should be preserved for performance
		Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
		for (Entry<GdlSentence, Proposition> val : props.entrySet())
		{
			val.getValue().setValue(false);
		}
	}

	private boolean getMarking(Proposition p) throws Exception
	{
		// TODO: determine behaviour for all types
		if (p.getType() == Type.base || p.getType() == Type.input || p.getType() == Type.logic)
		{
			return p.getValue();
		}
		else if (p.getType() == Type.view)
		{
			Component input = p.getSingleInput();
			return input.getValue();
		}

		throw new Exception("Unexpected type");
	}

	private void markBaseProps(Set<GdlSentence> stateSentences)
	{
		markProps(propNet.getBasePropositions(), stateSentences);
	}

	private void markMoves(List<Move> moves)
	{
		Set<GdlSentence> inputSentences = new HashSet<GdlSentence>();
		for (Move move : moves)
		{
			GdlSentence sentence = move.getContents().toSentence();
			inputSentences.add(sentence);
		}

		markInputProps(inputSentences);
	}

	private void markInputProps(Set<GdlSentence> inputSentence)
	{
		markProps(propNet.getInputPropositions(), inputSentence);
	}

	private void markProps(Map<GdlSentence, Proposition> props, Set<GdlSentence> markSentences)
	{
		// TODO: can optimize by having a separate set of base/input
		// propositions
		// that are true from before, then just iterate and adjust over those
		// + set true for the rest that weren't in the set
		for (Map.Entry<GdlSentence, Proposition> entry : props.entrySet())
		{
			Proposition prop = entry.getValue();
			// sets the value to true if it's part of the moves, false otherwise
			boolean value = markSentences.contains(entry.getKey());

			prop.setValue(value);

			addPropToTrueSet(prop, value);
		}
	}

    private void propagateMoves()
    {
    	Queue<Component> nodes = new LinkedList<Component>();

    	nodes.addAll(propNet.getBasePropositions().values());
    	nodes.addAll(propNet.getInputPropositions().values());

    	while(!nodes.isEmpty())
    	{
    		Component node = nodes.poll();
    		if(node instanceof Proposition)
    		{
    			Proposition prop = (Proposition)node;
    			if (prop.isChanged())
    			{
    				boolean value = prop.getValue();

    				for (Component child : prop.getOutputs())
    				{
    					if (child instanceof Proposition)
    					{
    						Proposition childProp = (Proposition)child;
    						childProp.setValue(value);

    						addPropToTrueSet(childProp, value);
    					}

    					nodes.add(child);
    				}
    			}
    		}
    		else
    		{
    			nodes.addAll(node.getOutputs());
    		}
    	}
    }

    private void addPropToTrueSet(Proposition prop, boolean value)
    {
		if (prop.getType() == Type.base)
		{
			if (value)
			{
				trueBaseProps.add(prop);
			}
			else
			{
				trueBaseProps.remove(prop);
			}
		}
		else if (prop.getType() == Type.input)
		{
			if (value)
			{
				trueInputProps.add(prop);
			}
			else
			{
				trueInputProps.remove(prop);
			}
		}
    }

	/**
	 * Computes if the state is terminal. Should return the value of the
	 * terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state)
	{
		Set<GdlSentence> contents = state.getContents();
		return contents.contains(propNet.getTerminalProposition());
	}

	/**
	 * Computes the goal for a role in the current state. Should return the
	 * value of the goal proposition that is true for that role. If there is not
	 * exactly one goal proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException
	{
		Set<Proposition> goals = propNet.getGoalPropositions().get(role);

		int goalValue = 0;
		boolean hasSingleGoal = false;

		for (Proposition goal : goals)
		{
			if (state.getContents().contains(goal.getName()))
			{
				if (hasSingleGoal)
				{
					throw new GoalDefinitionException(state, role);
				}

				goalValue = getGoalValue(goal);
				hasSingleGoal = true;
			}
		}

		if (!hasSingleGoal)
		{
			throw new GoalDefinitionException(state, role);
		}

		return goalValue;
	}

	/**
	 * Returns the initial state. The initial state can be computed by only
	 * setting the truth value of the INIT proposition to true, and then
	 * computing the resulting state.
	 */
	@Override
	public MachineState getInitialState()
	{
		Proposition init = propNet.getInitProposition();

		init.setValue(true);

		Set<GdlSentence> emptySet = Collections.emptySet();
		MachineState nextState = getNextState(emptySet, emptySet);

		init.setValue(false);

		return nextState;
	}

	/**
	 * Computes all possible actions for role.
	 */
	@Override
	public List<Move> findActions(Role role) throws MoveDefinitionException
	{
		// TODO: Compute legal moves.
		return null;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
	{
    	markBaseProps(state.getContents());
    	propagateMoves();
    	Map<Role, Set<Proposition>> legals = propNet.getLegalPropositions();

    	List<Move> m = new ArrayList<Move>();
        for(Proposition p : legals.get(role))
        {
        	m.add(getMoveFromProposition(p));
        }

        return m;
	}

	private MachineState getNextState(Set<GdlSentence> baseSentences, Set<GdlSentence> inputSentences)
	{
		markBaseProps(baseSentences);
    	markInputProps(inputSentences);

    	propagateMoves();

        return getStateFromCachedBase();
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		Set<GdlSentence> baseSentences = state.getContents();
		Set<GdlSentence> inputSentences = new HashSet<GdlSentence>();

		for (Move move : moves)
		{
			GdlSentence sentence = move.getContents().toSentence();
			inputSentences.add(sentence);
		}

		return getNextState(baseSentences, inputSentences);
	}

	/**
	 * This should compute the topological ordering of propositions. Each
	 * component is either a proposition, logical gate, or transition. Logical
	 * gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt from
	 * this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from the
	 * Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be
	 *         set.
	 */
	public void setOrdering()
	{
		Queue<Component> queue = new LinkedList<Component>();

		int order = 1;
		for (Entry<GdlSentence, Proposition> entry : propNet.getBasePropositions().entrySet())
		{
			Proposition prop = entry.getValue();

			Component transition = prop.getSingleInput();
			transition.setOrder(order);
			order++;

			queue.add(transition);
		}

		while (!queue.isEmpty())
		{
			Component component = queue.poll();
			for (Component child : component.getInputs())
			{
				// prevents cycles, either a local one (loop)
				// or a global one (back to transition gates)
				if (!child.hasOrder())
				{
					child.setOrder(order);
					order++;
					queue.add(child);
				}
			}
		}

		log.info(GLog.PROPNET, "Ordering added up to " + order);
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles()
	{
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply
	 * ?action) into GdlSentences that can be used to get Propositions from
	 * inputPropositions. and accordingly set their values etc. This is a naive
	 * implementation when coupled with setting input values, feel free to
	 * change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding
	 * Move
	 *
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 *
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
	private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState from the true
	 * BasePropositions. This is correct but slower than more advanced
	 * implementations You need not use this method!
	 *
	 * @return PropNetMachineState
	 */
	private MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}

	private MachineState getStateFromCachedBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : trueBaseProps)
		{
			contents.add(p.getName());
		}

		return new MachineState(contents);
	}
}
package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

	private Set<Proposition> trueBaseProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Proposition> trueInputProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Proposition> changedBaseProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Component> startingComponents = new HashSet<Component>();

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

			populateStartingComponents();

			log.info(GLog.PROPNET,
				"Size: " + propNet.getComponents().size());

			if (RuntimeParameters.OUTPUT_GRAPH_FILE)
			{
				String filePath = MachineParameters.outputFilename();
				log.info(GLog.PROPNET,
					"Logging graph output to:\n" + filePath);

				propNet.renderToFile(filePath);
			}

			roles = propNet.getRoles();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void populateStartingComponents()
	{
		for (Component c : propNet.getComponents())
		{
			if (c.getInputs().isEmpty())
			{
				startingComponents.add(c);
			}
		}
	}

	private void reset()
	{
		for (Component c : propNet.getComponents())
		{
			c.reset();
		}
	}

	private void markBaseProps(Set<GdlSentence> stateSentences)
	{
		changedBaseProps.clear();

		for (Map.Entry<GdlSentence, Proposition> entry : propNet.getBasePropositions().entrySet())
		{
			Proposition prop = entry.getValue();
			// sets the value to true if it's part of the moves, false otherwise
			boolean value = stateSentences.contains(entry.getKey());
			boolean previousValue = prop.getValue();

			if (value != previousValue)
			{
				markComponent(prop, value, false);

				changedBaseProps.add(prop);
			}
		}
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

	private void markInputProps(Set<GdlSentence> inputSentences)
	{
		for (Map.Entry<GdlSentence, Proposition> entry : propNet.getInputPropositions().entrySet())
		{
			Proposition prop = entry.getValue();
			// sets the value to true if it's part of the moves, false otherwise
			boolean value = inputSentences.contains(entry.getKey());
			boolean previousValue = prop.getValue();

			if (value != previousValue)
			{
				markComponent(prop, value, false);
			}
		}
	}

    private void propagateMoves()
    {
    	Queue<Component> queue = new LinkedList<Component>();

    	queue.addAll(propNet.getBasePropositions().values());
    	queue.addAll(startingComponents);

    	while(!queue.isEmpty())
    	{
    		Component node = queue.poll();

    		boolean updateChildren = node.shouldPropagate();

    		if (updateChildren)
    		{
    			boolean value = node.getValue();

    			for (Component child : node.getOutputs())
    			{
    				markComponent(child, value, !node.hasPropagatedOnce());

    				if (child.getType() != Type.base)
    				{
    					queue.add(child);
    				}
    			}

    			node.setPropagated();
    		}
    	}

		// this is required because we could have marked a base prop to be a different value than it was
		// but its parent transition may not necessarily propagate a new value to the base prop
		// if it's stale. this results in a base prop having an inconsistent value for that iteration
		// of propagation. for performance, update only the base props that we manually toggled
		for (Proposition p : changedBaseProps)
		{
			boolean transitionValue = p.getSingleInput().getValue();
			boolean value = p.getValue();

			if (value != transitionValue)
			{
				markComponent(p, transitionValue, false);
			}
		}
    }

    private void markComponent(Component component, boolean value, boolean firstPropagation)
    {
    	component.setValueFromParent(value, firstPropagation);

		if (component.getType() == Type.base)
		{
			Proposition prop = (Proposition)component;
			if (value)
			{
				if (!trueBaseProps.contains(prop))
				{
					trueBaseProps.add(prop);
				}
			}
			else
			{
				if (trueBaseProps.contains(prop))
				{
					trueBaseProps.remove(prop);
				}
			}
		}
		else if (component.getType() == Type.input)
		{
			Proposition prop = (Proposition)component;
			if (value)
			{
				if (!trueInputProps.contains(prop))
				{
					trueInputProps.add(prop);
				}
			}
			else
			{
				if (trueInputProps.contains(prop))
				{
					trueInputProps.remove(prop);
				}
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
		synchronized(this)
		{
			propagateMoves(state.getContents());

			boolean terminal = propNet.getTerminalProposition().getValue();

			return terminal;
		}
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
		synchronized(this)
		{
			propagateMoves(state.getContents());

			Set<Proposition> goals = propNet.getGoalPropositions().get(role);

			int goalValue = 0;
			boolean hasSingleGoal = false;

			for (Proposition goal : goals)
			{
				if (goal.getValue())
				{
					if (hasSingleGoal)
					{
						log.error(GLog.ERRORS,
								"Found multiple goals");
						throw new GoalDefinitionException(state, role);
					}

					goalValue = getGoalValue(goal);
					hasSingleGoal = true;
				}
			}

			if (!hasSingleGoal)
			{
				log.error(GLog.ERRORS,
						"Found no goals");
				throw new GoalDefinitionException(state, role);
			}

			return goalValue;
		}
	}

	/**
	 * Returns the initial state. The initial state can be computed by only
	 * setting the truth value of the INIT proposition to true, and then
	 * computing the resulting state.
	 */
	@Override
	public MachineState getInitialState()
	{
		synchronized(this)
		{
			List<Proposition> list = Collections.singletonList(propNet.getInitProposition());

			Set<GdlSentence> emptySet = Collections.emptySet();
			MachineState nextState = getNextState(emptySet, emptySet, list);

			propNet.getInitProposition().setValueFromParent(false);

			return nextState;
		}
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
		synchronized(this)
		{
			propagateMoves(state.getContents());

	    	Map<Role, Set<Proposition>> legals = propNet.getLegalPropositions();

	    	List<Move> m = new ArrayList<Move>();
	        for(Proposition p : legals.get(role))
	        {
	        	if (p.getValue())
	        	{
	        		m.add(getMoveFromProposition(p));
	        	}
	        }

	        return m;
		}
	}

	private void propagateMoves(Set<GdlSentence> baseSentences)
	{
		propagateMoves(baseSentences, null, null);
	}

	private void propagateMoves(
			Set<GdlSentence> baseSentences,
			Set<GdlSentence> inputSentences)
	{
		propagateMoves(baseSentences, inputSentences, null);
	}

	private void propagateMoves(
			Set<GdlSentence> baseSentences,
			Set<GdlSentence> inputSentences,
			List<Proposition> additionalProps)
	{
		markBaseProps(baseSentences);

		if (inputSentences == null)
		{
			inputSentences = Collections.emptySet();
		}

    	markInputProps(inputSentences);

    	if (additionalProps != null)
    	{
	    	for (Proposition p : additionalProps)
	    	{
	    		p.markValue(true);
	    	}
    	}

    	propagateMoves();
	}

	private MachineState getNextState(
			Set<GdlSentence> baseSentences,
			Set<GdlSentence> inputSentences)
	{
		return getNextState(baseSentences, inputSentences, null);
	}

	private MachineState getNextState(
			Set<GdlSentence> baseSentences,
			Set<GdlSentence> inputSentences,
			List<Proposition> additionalProps)
	{
		propagateMoves(baseSentences, inputSentences, additionalProps);

        return getStateFromCachedBase();
	}


	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		synchronized(this)
		{
			Set<GdlSentence> baseSentences = state.getContents();
			Set<GdlSentence> inputSentences = toDoes(moves);

			return getNextState(baseSentences, inputSentences);
		}
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
	private Set<GdlSentence> toDoes(List<Move> moves)
	{
		Set<GdlSentence> doeses = new HashSet<GdlSentence>(moves.size());
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
	private static Move getMoveFromProposition(Proposition p)
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
			p.setValueFromParent(p.getSingleInput().getValue());
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

		synchronized(this)
		{
			for (Proposition p : trueBaseProps)
			{
				contents.add(p.getName());
			}
		}

		return new MachineState(contents);
	}
}
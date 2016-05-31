package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
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
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

@SuppressWarnings("unused")
public class PropNetStateMachine extends AugmentedStateMachine
{
	private static final Logger log = GLog.getLogger(PropNetStateMachine.class);
	/** The underlying proposition network */
	private PropNet propNet;
	/** The player roles */
	private List<Role> roles;

	private Set<Proposition> trueBaseProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Proposition> trueInputProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Proposition> changedBaseProps = Collections
			.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Component> startingComponents = SetPool.newComponentSet();

	private Set<GdlSentence> trueLatches = null;
	private Set<GdlSentence> falseLatches = null;

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
			log.info(GLog.PROPNET, "Initializing prop net");

			propNet = OptimizingPropNetFactory.create(description);

			populateStartingComponents();

			log.info(GLog.PROPNET, "Size: " + propNet.getComponents().size());

			if (RuntimeParameters.OUTPUT_GRAPH_FILE)
			{
				String filePath = MachineParameters.outputFilename();
				log.info(GLog.PROPNET, "Logging graph output to:\n" + filePath);

				propNet.renderToFile(filePath);
			}

			roles = propNet.getRoles();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void findLatches(Role role, int minGoal)
	{
		if (trueLatches == null || falseLatches == null)
		{
			Map<Proposition, Boolean> baseInhibitors = findInhibitorsForRole(role, minGoal);
			Map<Proposition, Boolean> latches = getLatchInhibitors(baseInhibitors);

			trueLatches = SetPool.newSentenceSet();
			falseLatches = SetPool.newSentenceSet();

			for (Entry<Proposition, Boolean> entry : latches.entrySet())
			{
				Proposition proposition = entry.getKey();
				if (entry.getValue())
				{
					trueLatches.add(proposition.getName());
				}
				else
				{
					falseLatches.add(proposition.getName());
				}
			}
		}
	}

	@Override
	public boolean isDeadState(MachineState state)
	{
		if (trueLatches == null || falseLatches == null)
		{
			return false;
		}

		Set<GdlSentence> stateSentences = state.getContents();
		boolean foundTrueLatch = sentenceIntersect(stateSentences, trueLatches);

		if (foundTrueLatch)
		{
			return true;
		}

		for (GdlSentence sentence : falseLatches)
		{
			if (!stateSentences.contains(sentence))
			{
				return true;
			}
		}

		return false;
	}

	private boolean sentenceIntersect(Set<GdlSentence> set1, Set<GdlSentence> set2)
	{
		// this trick should help us reduce the runtime
		// given that set1 is size m and set2 is size n
		// we may be doing O(m) or O(n) work depending on which one we use to check
		// use the smaller one to check the larger set, since a check is O(1) time
		Set<GdlSentence> smallerSet = (set1.size() < set2.size()) ? set1 : set2;
		Set<GdlSentence> largerSet = (smallerSet == set2) ? set1 : set2;

		for (GdlSentence sentence : smallerSet)
		{
			if (largerSet.contains(sentence))
			{
				return true;
			}
		}

		return false;
	}

	// a small inner class to help reverse sort goals by score
	private static class ScoreComparator implements Comparator<Entry<Proposition, Integer>>
	{
		public static final ScoreComparator comparator = new ScoreComparator();

		@Override
		public int compare(Entry<Proposition, Integer> arg0, Entry<Proposition, Integer> arg1)
		{
			int score1 = arg0.getValue();
			int score2 = arg1.getValue();
			return Integer.compare(score1, score2);
		}
	}

	private Map<Proposition, Boolean> findInhibitorsForRole(Role role, int minGoal)
	{
		List<Entry<Proposition, Integer>> goals = new ArrayList<>();
		Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);

		for (Proposition p : goalProps)
		{
			int score = getGoalValue(p);
			if (score >= minGoal)
			{
				Entry<Proposition, Integer> entry = new SimpleImmutableEntry<>(p, score);
				goals.add(entry);
			}
		}

		if (goals.size() > 1)
		{
			// sort it in ascending order
			goals.sort(ScoreComparator.comparator);
		}

		Map<Proposition, Boolean> inhibitors = findInhibitorForGoal(goals);

		return inhibitors;
	}

	private Map<Proposition, Boolean> findInhibitorForGoal(List<Entry<Proposition, Integer>> goals)
	{
		Map<Proposition, Boolean> inhibitors = new HashMap<>();

		// using a stack because that will allow us to prioritize the
		// ancestors of the best goals first
		Stack<Entry<Component, Boolean>> ancestors = new Stack<>();
		Set<Component> visited = SetPool.newComponentSet();

		// start by finding what makes goals false
		for (Entry<Proposition, Integer> entry : goals)
		{
			Entry<Component, Boolean> newEntry = new SimpleImmutableEntry<Component, Boolean>(entry.getKey(), false);
			ancestors.add(newEntry);
		}

		while (!ancestors.isEmpty())
		{
			Entry<Component, Boolean> entry = ancestors.pop();
			Component component = entry.getKey();
			boolean inhibitingValue = entry.getValue();

			// mark this entry as visited
			visited.add(component);

			// TODO: note that by never visiting the same component twice
			// we miss the case where a single component is both a true and
			// false
			// inhibitor to two different goal nodes
			// i.e. p -> goal100 and !p -> goal99
			// we can potentially process both by revisiting components using
			// different inhibiting values, but not sure what that can do for
			// us:
			// all states are goal inhibiting states if our threshold is >= 99
			// in this example
			// best we can do is pass in goals in descending goal value order
			// that way we only process the best goals first
			switch (component.type())
			{
			case base:
			{
				inhibitors.put((Proposition) component, inhibitingValue);

				Component parent = component.getSingleInput();
				if (!visited.contains(parent))
				{
					Entry<Component, Boolean> parentEntry = new SimpleImmutableEntry<>(parent, inhibitingValue);
					ancestors.add(parentEntry);
				}

				break;
			}

			case and:
			{
				// if this AND gate is an inhibitor if it's false, then any of
				// its inputs is an inhibitor
				// if they are false
				// however, if it's an inhibitor if it's true, then all of its
				// inputs must be true
				// to be goal inhibiting. this results in a computational
				// complexity where we need to
				// cross reference multiple sets of components as opposed to a
				// bunch of single components
				// for now, we are avoiding this
				if (!inhibitingValue)
				{
					for (Component parent : component.getInputs())
					{
						if (!visited.contains(parent))
						{
							// reminder here: inhibitingValue is necessarily
							// false
							Entry<Component, Boolean> parentEntry = new SimpleImmutableEntry<>(parent, inhibitingValue);
							ancestors.add(parentEntry);
						}
					}
				}

				break;
			}

			case or:
			{
				// same logic as the AND case, see above
				// if OR is inhibiting when it's true, then any of its parents
				// being true is inhibiting
				// don't consider when OR is false, otherwise we need to
				// consider all of its parents being false
				if (inhibitingValue)
				{
					for (Component parent : component.getInputs())
					{
						if (!visited.contains(parent))
						{
							// reminder here: inhibitingValue is necessarily
							// true
							Entry<Component, Boolean> parentEntry = new SimpleImmutableEntry<>(parent, inhibitingValue);
							ancestors.add(parentEntry);
						}
					}
				}

				break;
			}

			case not:
			{
				// this has a single input, but negates the current value
				Component parent = component.getSingleInput();
				if (!visited.contains(parent))
				{
					boolean newValue = !inhibitingValue;
					Entry<Component, Boolean> parentEntry = new SimpleImmutableEntry<>(parent, newValue);
					ancestors.add(parentEntry);
				}

				break;
			}

			case goal:
			case view:
			case transition:
			{
				// these don't change the truth values, and also only have one
				// parent
				if(component.getInputs().size()!=0)
				{
					Component parent = component.getSingleInput();
					if (!visited.contains(parent))
					{
						Entry<Component, Boolean> parentEntry = new SimpleImmutableEntry<>(parent, inhibitingValue);
						ancestors.add(parentEntry);
					}
				}

				break;
			}

			case input:
			case constant:
			{
				// do nothing - these don't have parents
				// but also cannot be latches
				break;
			}

			case terminal:
			case unknown:
			default:
			{
				// nothing should be an unknown
				// terminals are also unexpected because we don't start
				// propagating there
				// nor can we reach it through a backwards propagation as it has
				// no children
				log.error(GLog.PROPNET, "Encountered unexpected component type");
				break;
			}
			}
		}

		SetPool.collectComponentSet(visited);

		return inhibitors;
	}

	private Map<Proposition, Boolean> getLatchInhibitors(Map<Proposition, Boolean> inhibitors)
	{
		Map<Proposition, Boolean> latches = new HashMap<>();

		Set<Proposition> ancestors = SetPool.newPropositionSet();
		Set<Component> seen = SetPool.newComponentSet();
		for (Entry<Proposition, Boolean> entry : inhibitors.entrySet())
		{
			Proposition inhibitor = entry.getKey();

			ancestors.clear();
			seen.clear();
			Stack<Component> uncheckedProps = new Stack<>();

			for (Component inputs : inhibitor.getInputs())
			{
				uncheckedProps.add(inputs);
			}

			seen.add(inhibitor);

			boolean exceededSize = false;

			while (!uncheckedProps.isEmpty())
			{
				Component currentComp = uncheckedProps.pop();
				seen.add(currentComp);

				if (currentComp.type() == Type.base || currentComp.type() == Type.input)
				{
					ancestors.add((Proposition) currentComp);

					if (ancestors.size() > RuntimeParameters.MAX_LATCH_ANCESTOR)
					{
						exceededSize = true;
						break;
					}
				}
				else
				{
					for (Component component : currentComp.getInputs())
					{
						if (!seen.contains(component))
						{
							uncheckedProps.add(component);
						}
					}
				}
			}

			// prevent running this on excessively large combinations
			// this results in 2^n runtime
			if (exceededSize)
			{
				continue;
			}

			ancestors.remove(inhibitor);

			boolean inhibitingValue = entry.getValue();
			if (checkLatch(inhibitor, ancestors, inhibitingValue))
			{
				latches.put(inhibitor, inhibitingValue);
			}
		}

		SetPool.collectPropositionSet(ancestors);
		SetPool.collectComponentSet(seen);

		log.info(GLog.PROPNET, "Latches: " + latches);

		return latches;
	}

	private boolean checkLatch(Proposition prop, Set<Proposition> ancestors, boolean latchType)
	{
		prop.setValueFromParent(latchType);

		Proposition[] props = new Proposition[ancestors.size()];
		boolean[] truthValues = new boolean[props.length];

		Set<Proposition> baseMarkings = SetPool.newPropositionSet();
		Set<Proposition> inputMarkings = SetPool.newPropositionSet();

		int index = 0;
		for (Proposition ancestor : ancestors)
		{
			truthValues[index] = false;
			props[index] = ancestor;
			index++;
		}

		boolean done = false;
		Set<GdlSentence> baseSentences = SetPool.newSentenceSet();
		Set<GdlSentence> inputSentences = SetPool.newSentenceSet();

		while (!done)
		{
			// could have made this a proper function but
			// type erasure for generics prevents us from doing that
			baseSentences.clear();
			for (Proposition marking : baseMarkings)
			{
				baseSentences.add(marking.getName());
			}

			inputSentences.clear();
			for (Proposition marking : inputMarkings)
			{
				inputSentences.add(marking.getName());
			}

			if (prop.getValue())
			{
				baseSentences.add(prop.getName());
			}

			propagateMoves(baseSentences, inputSentences);

			if (prop.getValue() != latchType)
			{
				SetPool.collectPropositionSet(baseMarkings);
				SetPool.collectPropositionSet(inputMarkings);
				SetPool.collectSentenceSet(baseSentences);
				SetPool.collectSentenceSet(inputSentences);
				return false;
			}

			boolean carryOver = true;
			int i = 0;

			while (carryOver)
			{
				Proposition currentProp = props[i];
				if (!truthValues[i])
				{
					truthValues[i] = true;

					if (currentProp.type() == Type.base)
					{
						baseMarkings.add(currentProp);
					}
					else if (currentProp.type() == Type.input)
					{
						inputMarkings.add(currentProp);
					}

					carryOver = false;
				}
				else
				{
					truthValues[i] = false;

					if (currentProp.type() == Type.base)
					{
						baseMarkings.remove(currentProp);
					}
					else if (currentProp.type() == Type.input)
					{
						inputMarkings.remove(currentProp);
					}

					i++;

					if (i == props.length)
					{
						carryOver = false;
						done = true;
					}
				}
			}
		}

		SetPool.collectPropositionSet(baseMarkings);
		SetPool.collectPropositionSet(inputMarkings);
		SetPool.collectSentenceSet(baseSentences);
		SetPool.collectSentenceSet(inputSentences);

		return true;
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
		Set<GdlSentence> inputSentences = SetPool.newSentenceSet();
		for (Move move : moves)
		{
			GdlSentence sentence = move.getContents().toSentence();
			inputSentences.add(sentence);
		}

		markInputProps(inputSentences);
		SetPool.collectSentenceSet(inputSentences);
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

		while (!queue.isEmpty())
		{
			Component node = queue.poll();

			boolean updateChildren = node.shouldPropagate();

			if (updateChildren)
			{
				boolean value = node.getValue();

				for (Component child : node.getOutputs())
				{
					markComponent(child, value, !node.hasPropagatedOnce());

					if (child.type() != Type.base)
					{
						queue.add(child);
					}
				}

				node.setPropagated();
			}
		}

		// this is required because we could have marked a base prop to be a
		// different value than it was
		// but its parent transition may not necessarily propagate a new value
		// to the base prop
		// if it's stale. this results in a base prop having an inconsistent
		// value for that iteration
		// of propagation. for performance, update only the base props that we
		// manually toggled
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

		if (component.type() == Type.base)
		{
			Proposition prop = (Proposition) component;
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
		else if (component.type() == Type.input)
		{
			Proposition prop = (Proposition) component;
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
		synchronized (this)
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
		synchronized (this)
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
						log.error(GLog.ERRORS, "Found multiple goals");
						throw new GoalDefinitionException(state, role);
					}

					goalValue = getGoalValue(goal);
					hasSingleGoal = true;
				}
			}

			if (!hasSingleGoal)
			{
				log.error(GLog.ERRORS, "Found no goals");
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
		synchronized (this)
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
		synchronized (this)
		{
			propagateMoves(state.getContents());

			Map<Role, Set<Proposition>> legals = propNet.getLegalPropositions();

			List<Move> m = new ArrayList<Move>();
			for (Proposition p : legals.get(role))
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

	private void propagateMoves(Set<GdlSentence> baseSentences, Set<GdlSentence> inputSentences)
	{
		propagateMoves(baseSentences, inputSentences, null);
	}

	private void propagateMoves(Set<GdlSentence> baseSentences, Set<GdlSentence> inputSentences,
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

	private MachineState getNextState(Set<GdlSentence> baseSentences, Set<GdlSentence> inputSentences)
	{
		return getNextState(baseSentences, inputSentences, null);
	}

	private MachineState getNextState(Set<GdlSentence> baseSentences, Set<GdlSentence> inputSentences,
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
		synchronized (this)
		{
			Set<GdlSentence> baseSentences = state.getContents();

			Set<GdlSentence> inputSentences = SetPool.newSentenceSet();
			inputSentences = toDoes(moves, inputSentences);

			MachineState nextState = getNextState(baseSentences, inputSentences);

			SetPool.collectSentenceSet(inputSentences);
			return nextState;
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
	private Set<GdlSentence> toDoes(List<Move> moves, Set<GdlSentence> doeses)
	{
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
		Set<GdlSentence> contents = SetPool.newSentenceSet();
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
		Set<GdlSentence> contents = SetPool.newSentenceSet();

		synchronized (this)
		{
			for (Proposition p : trueBaseProps)
			{
				contents.add(p.getName());
			}
		}

		// don't return the pool, machinestate has ownership
		// it will return it to the pool once it's been garbage collected
		return new MachineState(contents);
	}
}
package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Or;
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
	private Set<Proposition> changedBaseProps = Collections.newSetFromMap(new ConcurrentHashMap<Proposition, Boolean>());
	private Set<Component> startingComponents = new HashSet<Component>();

	private Map<Proposition, Boolean> latches = null;

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

	@Override
	public void findLatches(Role role, int minGoal)
	{
		if (latches == null)
		{
			List<Proposition> baseInhibitors = findBaseInhibitors(role, minGoal);
			latches = getLatchInhibitors(baseInhibitors);
		}
	}

	private List<Proposition> findBaseInhibitors(Role role, int minGoal)
	{
		Set<Proposition> goalProps  = propNet.getGoalPropositions().get(role);
		Set<Proposition> bestGoals  = new HashSet<Proposition>();

		for(Proposition p: goalProps)
		{
			int score = getGoalValue(p);
			if(score == minGoal)
			{
				bestGoals.add(p);
			}
		}


		//ArrayList<List<Component>> allPaths = new ArrayList<List<Component>>();
		//for(Proposition p: bestGoals)
		//{
			//ArrayList<List<Component>> path = new ArrayList<List<Component>>();
			//path.add(new ArrayList<Component>());
			//List<List<Component>> paths = expandPath(p, path);
			//allPaths.addAll(paths);
		//}

		ArrayList<Proposition> inhibitors = new ArrayList<Proposition>();
		for(Proposition p: bestGoals)
		{
			List<Proposition> paths = searchTree(p);
			inhibitors.addAll(paths);
		}

		//ArrayList<Proposition> inhibitors = new ArrayList<Proposition>();
		//Collection<Proposition> bases =  propNet.getBasePropositions().values();
		//for(Proposition base : bases)
	//	{
			//boolean inhibitor = true;
		//	for(List<Component> path : allPaths)
			//{
				//if(!path.contains(base) && !path.contains(null))
			//	{
				//	inhibitor = false;
					//break;
				//}
			//}
			//if(inhibitor)
			//{
//				inhibitors.add(base);
	//		}
		//}

		return inhibitors;
	}

	private List<List<Component>> expandPath(Component c, List<List<Component>> paths)
	{
		Set<Component> inputs = c.getInputs();
		ArrayList<List<Component>> finalPath = new ArrayList<List<Component>>();


		for(Component i : inputs)
		{

			ArrayList<List<Component>> newPaths = new ArrayList<List<Component>>();
			for(List<Component> path : paths)
			{
				if(path.contains(i))
				{
					return paths;
				}
				else
				{
					ArrayList<Component> newPath = new ArrayList<Component>(path);
					if(i instanceof Proposition)
					{
						Proposition p = (Proposition)i;
						if(i.getType() == Type.base)
						{
							newPath.add(i);
						}
					}
					newPaths.add(newPath);
				}
			}
			List<List<Component>> result = expandPath(i, newPaths);
			if(result!=null)
			{
				finalPath.addAll(result);
			}
		}
		return finalPath;
	}


	public List<Proposition> searchTree(Component c)
	{
		List<Proposition> inhibitors = new ArrayList<Proposition>();
		if(c instanceof And)
		{
			for(Component i : c.getInputs())
			{
				if(i instanceof Proposition && ((Proposition)i).getType()== Type.base)
				{
					inhibitors.add((Proposition)i);
				}
				else
				{
					inhibitors.addAll(searchTree(c));
				}
			}


		}
		else if(c instanceof Or)
		{

			List<List<Proposition>> l = new ArrayList<List<Proposition>>();
			for(Component i : c.getInputs())
			{

				if(i instanceof Proposition && ((Proposition)i).getType()== Type.base)
				{
					List<Proposition> temp = new ArrayList<Proposition>();
					temp.add((Proposition)i);
					l.add(temp);
				}
				else
				{
					List<Proposition> temp = searchTree(c);
					l.add(temp);
				}
			}
			for(Component co : l.get(0))
			{
				boolean inhib = true;
				for(List<Proposition> li : l)
				{
					if(!li.contains(co))
					{
						inhib = false;
						break;
					}

				}
				if(inhib)
				{
					inhibitors.add((Proposition)co);
				}
			}
		}
		else if(c instanceof Proposition)
		{
			if(c.getType() == Type.base)
			{
				inhibitors.add((Proposition)c);
			}
			else
			{
				inhibitors.addAll(searchTree(c.getSingleInput()));
			}
		}
		else if(c.getInputs().size()==1)
		{
			inhibitors.addAll(searchTree(c.getSingleInput()));
		}
		return inhibitors;

	}

	private Map<Proposition, Boolean> getLatchInhibitors(List<Proposition> inhibitors)
	{
		Map<Proposition, Boolean> latches = new HashMap<>();

		for(Proposition inhibitor : inhibitors)
		{
			Set<Proposition> ancestors = new HashSet<>();
			Set<Component> seen = new HashSet<>();

			Stack<Component> uncheckedProps = new Stack<>();
			Component currentComp = inhibitor;
			uncheckedProps.add(currentComp);

			while(!uncheckedProps.isEmpty())
			{
				currentComp = uncheckedProps.pop();
				seen.add(currentComp);
				uncheckedProps.addAll(currentComp.getInputs());
				if(currentComp.getType() == Type.base || currentComp.getType() == Type.input)
				{
					ancestors.add((Proposition)currentComp);
				}
				uncheckedProps.removeAll(seen);
			}

			if(checkLatch(inhibitor, ancestors, true))
			{
				latches.put(inhibitor, true);
			}
			else if(checkLatch(inhibitor, ancestors, false))
			{
				latches.put(inhibitor, false);
			}
		}

		log.info(GLog.PROPNET,
				"Latches: " +latches);

		return latches;
	}

	private boolean checkLatch(Proposition prop, Set<Proposition> ancestors, boolean latchType)
	{
		prop.setValueFromParent(latchType);

		Proposition[] props = new Proposition[ancestors.size()];
		boolean[] truthValues = new boolean[props.length];

		Set<Proposition> markings = new HashSet<>();

		int index = 0;
		for(Proposition ancestor : ancestors)
		{
			truthValues[index] = false;
			props[index] = ancestor;
			index++;
		}

		boolean done = false;
		while(!done)
		{
			Set<GdlSentence> sentenceMarkings = new HashSet<>();
			for (Proposition marking : markings)
			{
				sentenceMarkings.add(marking.getName());
			}

			propagateMoves(sentenceMarkings);

			if(prop.getSingleInput().getValue() != latchType) return false;

			boolean carryOver = true;
			int i = 0;

			while(carryOver)
			{
				if(!truthValues[i])
				{
					truthValues[i] = true;
					markings.add(props[i]);
					carryOver = false;
				}
				else
				{
					truthValues[i] = false;
					markings.remove(props[i]);
					i++;

					if(i == props.length)
					{
						carryOver = false;
						done = true;
					}
				}
			}
		}

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

	@Override
	public Set<Subgame> getSubgames()
	{

		Proposition terminalProp = propNet.getTerminalProposition();
		Stack<Component> tempTerminals = new Stack<>();
		Set<Component> terminalNodes = new HashSet<>();
		Set<Component> componentsSeen = new HashSet<>();

		tempTerminals.add(terminalProp);

		boolean negation = false;

		while(!tempTerminals.isEmpty())
		{
			Component terminalNode = tempTerminals.pop();
			componentsSeen.add(terminalNode);
			Set<Component> inputs = terminalNode.getInputs();

			if(inputs.size() == 1)
			{
				Component terminalInput = terminalNode.getSingleInput();
				if(!negation && terminalInput.getType() == Type.and)
				{
					//stop exploring
					terminalNodes.add(terminalNode);
					negation = false;
				}
				else if(negation && terminalInput.getType() == Type.or)
				{
					//stop exploring
					terminalNodes.add(terminalNode);
					negation = false;
				}
				else
				{
					if(terminalInput.getType() == Type.not) negation = !negation;
					if(!componentsSeen.contains(terminalInput)) tempTerminals.push(terminalInput);
				}
			}
			else
			{
				for(Component each : inputs)
				{
					tempTerminals.push(each);
				}
			}
		}

		//as a failsafe kind of thing. really shouldn't be necessary (?)
		if(terminalNodes.size() == 0) {
			terminalNodes.add(terminalProp);
		}

		return getSubgamesFromTerminalNodes(terminalNodes);
	}

	public Set<Subgame> getSubgamesFromTerminalNodes(Set<Component> terminalNodes)
	{
		Set<Subgame> subgames = new HashSet<>();
		Set<Component> seen = new HashSet<>();
		Stack<Component> toBeTraversed = new Stack<>();

		for(Component eachTerminal : terminalNodes)
		{
			toBeTraversed.push(eachTerminal);
			Set<Proposition> baseProps = new HashSet<>();
			Set<Proposition> inputProps = new HashSet<>();

			while(!toBeTraversed.isEmpty())
			{
				Component comp = toBeTraversed.pop();
				seen.add(comp);
				if(comp.getType() == Type.base) baseProps.add((Proposition) comp);
				if(comp.getType() == Type.input) inputProps.add((Proposition) comp);

				for(Component eachInput : comp.getInputs())
				{
					if(!seen.contains(eachInput)) toBeTraversed.push(eachInput);
				}
			}

			seen.clear();
			Subgame subgame = new Subgame(baseProps, inputProps, eachTerminal);
			subgames.add(subgame);
		}

		//check to see if subgames are disjoint
		combineSubgames(subgames);

		return subgames;
	}

	public void combineSubgames(Set<Subgame> subgames)
	{
		Map<Proposition, List<Subgame>> allProps = new HashMap<>();

		for(Subgame eachSubgame : subgames)
		{
			Set<Proposition> subgameProps = eachSubgame.getBaseProps();
			subgameProps.addAll(eachSubgame.getInputProps());

			for(Proposition eachProp : subgameProps)
			{
				if(allProps.containsKey(eachProp))
				{
					allProps.get(eachProp).add(eachSubgame);
				}
				else
				{
					List<Subgame> assocSubgames = new ArrayList<>();
					assocSubgames.add(eachSubgame);
					allProps.put(eachProp, assocSubgames);
				}
			}
		}

		Set<Subgame> newSubgames = new HashSet<>();

		while(!allProps.isEmpty())
		{
			Set<Proposition> visited = new HashSet<>();
			Set<Proposition> unvisited = new HashSet<>();


			Component terminalNode = null;

			unvisited.add(allProps.keySet().iterator().next());
			while(!unvisited.isEmpty())
			{
				Proposition key = unvisited.iterator().next();
				List<Subgame> assocSubgames = allProps.get(key);
				for(Subgame each : assocSubgames)
				{
					if(terminalNode == null)
					{
						terminalNode = each.getTerminalNode();
					}
					unvisited.addAll(each.getBaseProps());
					unvisited.addAll(each.getInputProps());
					unvisited.removeAll(visited);
				}

				visited.add(key);
				unvisited.remove(key);
				allProps.remove(key);
			}

			Set<Proposition> newSubgameBases = new HashSet<>();
			Set<Proposition> newSubgameInputs = new HashSet<>();

			for(Proposition prop : visited)
			{
				if(prop.getType() == Type.base)
				{
					newSubgameBases.add(prop);
				}
				if(prop.getType() == Type.input)
				{
					newSubgameInputs.add(prop);
				}
			}

			Subgame newSubgame = new Subgame(newSubgameBases, newSubgameInputs, terminalNode);
			newSubgames.add(newSubgame);

		}


		subgames.clear();
		subgames.addAll(newSubgames);

		/*for(Proposition eachProp : allProps.keySet())
		{


			List<Subgame> assocSubgames = allProps.get(eachProp);
			int numAssocSubgames = assocSubgames.size();

			if(numAssocSubgames > 1)
			{
				Subgame sgzero = assocSubgames.get(0);
				Set<Proposition> sgzeroBases = sgzero.getBaseProps();
				Set<Proposition> sgzeroInputs = sgzero.getInputProps();

				for(int i = 1; i < numAssocSubgames; i++)
				{
					Subgame currentSG = assocSubgames.get(i);
					sgzeroBases.addAll(currentSG.getBaseProps());
					sgzeroInputs.addAll(currentSG.getInputProps());
				}

				subgames.removeAll(assocSubgames);
				subgames.add(sgzero);
			}
		}*/
	}

	@Override
	public int getGoalSub(MachineState state, Role role, Subgame subgame) throws GoalDefinitionException
	{
		return getGoal(state, role);
	}

	@Override
	public boolean isTerminalSub(MachineState state, Subgame subgame)
	{
		return isTerminal(state);
	}

	@Override
	public List<Move> getLegalMovesSub(MachineState state, Role role, Subgame subgame) throws MoveDefinitionException
	{
		List<Move> legals = getLegalMoves(state, role);
		ArrayList<Move> diff = new ArrayList<Move>(legals);
		ArrayList<Move> intersect = new ArrayList<Move>(legals);
		if(subgame.getInputProps() == null)
		{
			int x =0;
		}
		Set<Proposition> inputs = subgame.getInputProps();
		for(Proposition input : inputs)
		{
			Move m = getMoveFromProposition(input);
			diff.remove(m);
		}

		intersect.removeAll(diff);

		if(intersect.isEmpty())
		{
			intersect.addAll(legals);
		}

		//GdlTerm noopTerm = new GdlTerm("noop");

		//Move noop =

		//intersect.add(noop);

		return intersect;
	}

	@Override
	public MachineState getNextStateSub(MachineState state, List<Move> moves, Subgame subgame)
			throws TransitionDefinitionException
	{
		// TODO Auto-generated method stub
		return getNextState(state, moves);
	}

	@Override
	public List<Move> getLegalMovesComplementSub(MachineState state, Role role, Subgame subgame)
			throws MoveDefinitionException
	{
		List<Move> legals = getLegalMoves(state, role);
		ArrayList<Move> diff = new ArrayList<Move>(legals);
		Set<Proposition> inputs = subgame.getInputProps();
		for(Proposition input : inputs)
		{
			diff.remove(getMoveFromProposition(input));
		}

		return diff;
	}
}
package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class SetPool
{
	private static AbstractQueue<Set<Component>> componentQueue = new ConcurrentLinkedQueue<>();
	private static AbstractQueue<Set<Proposition>> propositionQueue = new ConcurrentLinkedQueue<>();
	private static AbstractQueue<Set<GdlSentence>> sentenceQueue = new ConcurrentLinkedQueue<>();
	private static volatile int setCount = 0;

	static
	{
		initialize(MachineParameters.LOW_NODE_THRESHOLD);
	}

	public static void initialize(int size)
	{
		componentQueue.clear();
		propositionQueue.clear();
		sentenceQueue.clear();
		setCount = 0;
		allocateComponents(size);
		allocatePropositions(size);
		allocateSentences(size);
	}

	private static void allocateComponents(int size)
	{
		for (int i = 0; i < size; i++)
		{
			componentQueue.add(new HashSet<Component>());
		}

		setCount += size;
		GLog.getRootLogger().info(GLog.MEMORY,
				"Set count currently: " + setCount);
	}

	private static void allocatePropositions(int size)
	{
		for (int i = 0; i < size; i++)
		{
			propositionQueue.add(new HashSet<Proposition>());
		}

		setCount += size;
		GLog.getRootLogger().info(GLog.MEMORY,
				"Set count currently: " + setCount);
	}

	private static void allocateSentences(int size)
	{
		for (int i = 0; i < size; i++)
		{
			sentenceQueue.add(new HashSet<GdlSentence>());
		}

		setCount += size;
		GLog.getRootLogger().info(GLog.MEMORY,
				"Set count currently: " + setCount);
	}

	public static void collectComponentSet(Set<Component> component)
	{
		component.clear();
		componentQueue.add(component);
	}

	public static void collectPropositionSet(Set<Proposition> proposition)
	{
		proposition.clear();
		propositionQueue.add(proposition);
	}

	public static void collectSentenceSet(Set<GdlSentence> sentence)
	{
		sentence.clear();
		sentenceQueue.add(sentence);
	}

	public static Set<Component> newComponentSet()
	{
		Set<Component> component = componentQueue.poll();

		if (component == null && canGrow())
		{
			allocateComponents(MachineParameters.LOW_NODE_THRESHOLD);
			component = componentQueue.poll();
		}

		return component;
	}

	public static Set<Proposition> newPropositionSet()
	{
		Set<Proposition> proposition = propositionQueue.poll();

		if (proposition == null && canGrow())
		{
			allocatePropositions(MachineParameters.LOW_NODE_THRESHOLD);
			proposition = propositionQueue.poll();
		}

		return proposition;
	}

	public static Set<GdlSentence> newSentenceSet()
	{
		Set<GdlSentence> sentence = sentenceQueue.poll();

		if (sentence == null && canGrow())
		{
			allocateSentences(MachineParameters.LOW_NODE_THRESHOLD);
			sentence = sentenceQueue.poll();
		}

		return sentence;
	}

	public static boolean canGrow()
	{
		// TODO: magic number?
		return setCount < 10 * MachineParameters.MAX_NODES;
	}

}

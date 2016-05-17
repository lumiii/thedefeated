package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class Subgame
{
	private final Set<Proposition> basePropositions;
	private final Set<Proposition> inputPropositions;
	private final Component terminalNode;

	public Subgame(
			Set<Proposition> basePropositions,
			Set<Proposition> inputPropositions,
			Component terminalNode)
	{
		this.basePropositions = basePropositions;
		this.inputPropositions = inputPropositions;
		this.terminalNode = terminalNode;
	}

	public Set<Proposition> getBaseProps()
	{
		return basePropositions;
	}

	public Set<Proposition> getInputProps()
	{
		return inputPropositions;
	}

	public Component getTerminalNode()
	{
		return terminalNode;
	}
}

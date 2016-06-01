package org.ggp.base.util.propnet.architecture.components;

import java.util.Arrays;

import org.ggp.base.player.gamer.statemachine.thedefeated.MachineParameters;
import org.ggp.base.player.gamer.statemachine.thedefeated.ThreadID;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Proposition class is designed to represent named latches.
 */
@SuppressWarnings("serial")
public final class Proposition extends Component
{
    /** The name of the Proposition. */
    private GdlSentence name;
    /** The value of the Proposition. */
    private boolean[] valueArray = new boolean[MachineParameters.GAME_THREADS];

    /**
     * Creates a new Proposition with name <tt>name</tt>.
     *
     * @param name
     *            The name of the Proposition.
     */
    public Proposition(GdlSentence name)
    {
        this.name = name;
        Arrays.fill(valueArray, false);
    }

    /**
     * Getter method.
     *
     * @return The name of the Proposition.
     */
    public GdlSentence getName()
    {
        return name;
    }

    /**
     * Setter method.
     *
     * This should only be rarely used; the name of a proposition
     * is usually constant over its entire lifetime.
     */
    public void setName(GdlSentence newName)
    {
        name = newName;
    }

    /**
     * Returns the current value of the Proposition.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        return value();
    }

    // functionally the same as setValueFromParent
    // but semantically different - use this when explicitly marking a proposition node
    // outside of propagation
    public void markValue(boolean value)
    {
    	this.value(value);
    }

    public void ensurePropagate()
    {
    	this.propagatedValue(!value());
    }

    /**
     * Setter method.
     *
     * @param value
     *            The new value of the Proposition.
     */
    // propositions have no logic, just inherit the value
    @Override
	public void setValueFromParent(boolean value)
    {
    	this.value(value);
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
    	//return name.toString();
        return toDot("circle", value() ? "red" : "white", name.toString());
    }

    @Override
	public Type type()
    {
    	if (type == Type.base || type == Type.input)
    	{
    		return type;
    	}

    	// all propositions that are not base or input are view
    	return Type.view;
    }

	@Override
	public void resetState()
	{
		value(false);
	}

	private boolean value()
	{
		return valueArray[ThreadID.get()];
	}

	private void value(boolean value)
	{
		valueArray[ThreadID.get()] = value;
	}
}
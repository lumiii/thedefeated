package org.ggp.base.util.propnet.architecture.components;

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
    private boolean init;
    private boolean value;
    private boolean prevValue;


    /**
     * Creates a new Proposition with name <tt>name</tt>.
     *
     * @param name
     *            The name of the Proposition.
     */
    public Proposition(GdlSentence name)
    {
        this.name = name;
        this.init = false;
        this.value = false;
        this.prevValue = false;
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
        return value;
    }

    @Override
	public boolean isChanged()
    {
        return (prevValue == value);
    }

    /**
     * Setter method.
     *
     * @param value
     *            The new value of the Proposition.
     */
    public void setValue(boolean value)
    {
    	if (this.init)
    	{
    		this.prevValue = this.value;
        	this.value = value;
    	}
    	else
    	{
        	this.value = value;
    		this.init = true;
    		this.prevValue = !this.value;
    	}
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
    	return name.toString();
        //return toDot("circle", value ? "red" : "white", name.toString());
    }

    @Override
	public Type getType()
    {
    	if (type != Type.base && type != Type.input)
    	{
    		return Type.view;
    	}

		return type;
    }
}
package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	private boolean value = false;
    /**
     * Returns the inverse of the input to the not.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        return value;
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
        return toDot("invtriangle", "grey", "NOT");
    }

    @Override
    public Type getType()
    {
    	return Type.logic;
    }

	@Override
	public void setValueFromParent(boolean value)
	{
		// inverting parent's value
		this.value = !value;
	}

	@Override
	public void resetState()
	{
		value = false;
	}
}
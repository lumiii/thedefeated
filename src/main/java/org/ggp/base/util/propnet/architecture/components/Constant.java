package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Constant class is designed to represent nodes with fixed logical values.
 */
@SuppressWarnings("serial")
public final class Constant extends Component
{
    /** The value of the constant. */
    private final boolean value;

    /**
     * Creates a new Constant with value <tt>value</tt>.
     *
     * @param value
     *            The value of the Constant.
     */
    public Constant(boolean value)
    {
        this.value = value;
    }

    /**
     * Returns the value that the constant was initialized to.
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
        return toDot("doublecircle", "grey", Boolean.toString(value).toUpperCase());
    }

    @Override
	public Type type()
    {
    	return Type.constant;
    }

	@Override
	public void setValueFromParent(boolean value)
	{
		throw new IllegalArgumentException("Can't set a value on a constant - it should have no parent");
	}

	@Override
	public void setPropagated()
	{
		if (!propagatedOnce)
		{
			propagatedOnce = true;
		}
	}

	@Override
	public boolean shouldPropagate()
	{
		// the value of this will never change, just pivot on whether it's been ever used once
		return !propagatedOnce;
	}

	@Override
	public void resetState()
	{
		// nothing to do
	}


}
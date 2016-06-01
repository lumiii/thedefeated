package org.ggp.base.util.propnet.architecture.components;

import java.util.Arrays;

import org.ggp.base.player.gamer.statemachine.thedefeated.MachineParameters;
import org.ggp.base.player.gamer.statemachine.thedefeated.ThreadID;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	private boolean[] valueArray = new boolean[MachineParameters.GAME_THREADS];

	public Not()
	{
		Arrays.fill(valueArray, false);
	}

    /**
     * Returns the inverse of the input to the not.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        return value();
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
    public Type type()
    {
    	return Type.not;
    }

	@Override
	public void setValueFromParent(boolean value)
	{
		// inverting parent's value
		this.value(!value);
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
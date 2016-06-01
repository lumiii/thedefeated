package org.ggp.base.util.propnet.architecture.components;

import java.util.Arrays;

import org.ggp.base.player.gamer.statemachine.thedefeated.MachineParameters;
import org.ggp.base.player.gamer.statemachine.thedefeated.ThreadID;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class Transition extends Component
{
	private boolean[] valueArray = new boolean[MachineParameters.GAME_THREADS];

	public Transition()
	{
		Arrays.fill(valueArray, false);
	}

    /**
     * Returns the value of the input to the transition.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        return value();
    }

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
        return toDot("box", "grey", "TRANSITION");
    }

    @Override
    public Type type()
    {
    	return Type.transition;
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
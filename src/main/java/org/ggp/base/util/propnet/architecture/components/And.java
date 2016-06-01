package org.ggp.base.util.propnet.architecture.components;

import java.util.Arrays;

import org.ggp.base.player.gamer.statemachine.thedefeated.GLog;
import org.ggp.base.player.gamer.statemachine.thedefeated.MachineParameters;
import org.ggp.base.player.gamer.statemachine.thedefeated.ThreadID;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
	private int[] numTrueArray = new int[MachineParameters.GAME_THREADS];

	public And()
	{
		Arrays.fill(numTrueArray, 0);
	}

    /**
     * Returns true if and only if every input to the and is true.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
    	return numTrue() == getInputs().size();
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
        return toDot("invhouse", "grey", "AND");
    }

    @Override
    public Type type()
    {
    	return Type.and;
    }

	@Override
	public void setValueFromParent(boolean value)
	{
		// this is very dangerous because our correctness depends
		// on our parents not double calling us on stale values
		// keep an eye out for errors in AND/OR gates
		if (value)
		{
			numTrueIncrement();
		}
		else
		{
			numTrueDecrement();
		}

		int val = numTrue();

		if (val < 0 || val > getInputs().size())
		{
			GLog.getRootLogger().error(GLog.PROPNET,
					"AND gate counting invariant violated!");
		}
	}

	@Override
	public void setValueFromParent(boolean value, boolean firstPropagation)
	{
		if (firstPropagation)
		{
			if (value)
			{
				numTrueIncrement();
			}
		}
		else
		{
			setValueFromParent(value);
		}
	}

	@Override
	public void resetState()
	{
		numTrue(0);
	}

	private int numTrue()
	{
		return numTrueArray[ThreadID.get()];
	}

	private void numTrue(int value)
	{
		numTrueArray[ThreadID.get()] = value;
	}

	private void numTrueIncrement()
	{
		numTrueArray[ThreadID.get()]++;
	}

	private void numTrueDecrement()
	{
		numTrueArray[ThreadID.get()]--;
	}
}

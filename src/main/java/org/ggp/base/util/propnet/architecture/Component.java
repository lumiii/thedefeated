package org.ggp.base.util.propnet.architecture;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.player.gamer.statemachine.thedefeated.RuntimeParameters;
import org.ggp.base.util.propnet.architecture.components.Proposition;

/**
 * The root class of the Component hierarchy, which is designed to represent
 * nodes in a PropNet. The general contract of derived classes is to override
 * all methods.
 */

public abstract class Component implements Serializable
{
	public enum Type
	{
		base,
		input,
		view,
		transition,
		and,
		or,
		not,
		constant,
		goal,
		terminal,
		unknown
	}

    private static final long serialVersionUID = 352524175700224447L;
    /** The inputs to the component. */
    private final Set<Component> inputs;
    /** The outputs of the component. */
    private final Set<Component> outputs;

    private int order;

    protected Type type;

    protected boolean propagatedOnce = false;
    protected boolean propagatedValue = false;

    /**
     * Creates a new Component with no inputs or outputs.
     */
    public Component()
    {
        this.inputs = new HashSet<Component>();
        this.outputs = new HashSet<Component>();
        this.order = -1;
    }

    /**
     * Adds a new input.
     *
     * @param input
     *            A new input.
     */
    public void addInput(Component input)
    {
        inputs.add(input);
    }

    public void removeInput(Component input)
    {
        inputs.remove(input);
    }

    public void removeOutput(Component output)
    {
        outputs.remove(output);
    }

    public void removeAllInputs()
    {
        inputs.clear();
    }

    public void removeAllOutputs()
    {
        outputs.clear();
    }

    public void setOrder(int order)
    {
    	if (order < 0)
    	{
    		throw new IllegalArgumentException();
    	}

    	this.order = order;
    }

    public int getOrder()
    {
    	return order;
    }

    public boolean hasOrder()
    {
    	return (order != -1);
    }


    /**
     * Adds a new output.
     *
     * @param output
     *            A new output.
     */
    public void addOutput(Component output)
    {
        outputs.add(output);
    }

    /**
     * Getter method.
     *
     * @return The inputs to the component.
     */
    public Set<Component> getInputs()
    {
        return inputs;
    }

    /**
     * A convenience method, to get a single input.
     * To be used only when the component is known to have
     * exactly one input.
     *
     * @return The single input to the component.
     */
    public Component getSingleInput() {
        assert inputs.size() == 1;
        return inputs.iterator().next();
    }

    /**
     * Getter method.
     *
     * @return The outputs of the component.
     */
    public Set<Component> getOutputs()
    {
        return outputs;
    }

    /**
     * A convenience method, to get a single output.
     * To be used only when the component is known to have
     * exactly one output.
     *
     * @return The single output to the component.
     */
    public Component getSingleOutput() {
        assert outputs.size() == 1;
        return outputs.iterator().next();
    }

    /**
     * Returns the value of the Component.
     *
     * @return The value of the Component.
     */
    public abstract boolean getValue();

    // let the parent of this component call this with its own value
    // that is, you should only use this function as
    // child.setValueFromParent(parent.getValue())
    public abstract void setValueFromParent(boolean value);

    public void setValueFromParent(boolean value, boolean firstPropagation)
    {
    	setValueFromParent(value);
    }

    // call this function whenever this component's value is propagated to its children
    public void setPropagated()
    {
    	if (!propagatedOnce)
    	{
    		propagatedOnce = true;
    	}

    	propagatedValue = getValue();
    }

    public void unsetPropagated()
    {
    	// just a debug function
    	// use this to ensure this node propagates forwards
    	propagatedOnce = false;
    }

    public abstract void resetState();

    public void reset()
    {
    	propagatedOnce = false;
    	propagatedValue = false;
    	resetState();
    }

    // check whether or not this component should be propagated
    // aka has the truth value of this component changed since last propagated?
    public boolean shouldPropagate()
    {
    	boolean currentValue = getValue();
    	return (propagatedValue != currentValue) || !propagatedOnce;
    }

    public boolean hasPropagatedOnce()
    {
    	return propagatedOnce;
    }

    /**
     * Returns a representation of the Component in .dot format.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns a configurable representation of the Component in .dot format.
     *
     * @param shape
     *            The value to use as the <tt>shape</tt> attribute.
     * @param fillcolor
     *            The value to use as the <tt>fillcolor</tt> attribute.
     * @param label
     *            The value to use as the <tt>label</tt> attribute.
     * @return A representation of the Component in .dot format.
     */
    protected String toDot2(String shape, String fillcolor, String label)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\"@" + Integer.toHexString(hashCode()) + "\"[shape=" + shape + ", style= filled, fillcolor=" + fillcolor + ", label=\"" + label + "\"]; ");
        for ( Component component : getOutputs() )
        {
            sb.append("\"@" + Integer.toHexString(hashCode()) + "\"->" + "\"@" + Integer.toHexString(component.hashCode()) + "\"; ");
        }

        return sb.toString();
    }

    protected String toDot(String shape, String fillcolor, String label)
    {
    	if (RuntimeParameters.GRAPH_TOSTRING)
    	{
    		return toDot2(shape, fillcolor, label);
    	}
    	// else
    	return toList();
    }

    public String toList()
    {
        StringBuilder sb = new StringBuilder();

        String thisLabel = labelString();

        sb.append(thisLabel + "\n");

        for ( Component component : getOutputs() )
        {
        	sb.append(thisLabel + "->" + component.labelString() + "\n");
        }

        return sb.toString();
    }

    protected String labelString()
    {
        String thisLabel = "@" + Integer.toHexString(hashCode()) + ":";

        if (this instanceof Proposition)
        {
        	thisLabel += ((Proposition)this).getName() + "(" + type().toString() + ")";
        }
        else
        {
        	if (type == Type.and || type == Type.or || type == Type.not)
        	{
        		thisLabel += this.getClass().getSimpleName();
        	}
        	else
        	{
        		thisLabel += type().toString();
        	}
        }

        return thisLabel;
    }

    public void setType(Type type)
    {
    	this.type = type;
    }

    public Type type()
    {
    	return type;
    }
}
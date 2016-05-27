package org.ggp.base.player.gamer.statemachine.thedefeated;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public abstract class AugmentedStateMachine extends StateMachine
{
    public abstract void findLatches(Role role, int minGoal);
    public abstract boolean isDeadState(MachineState state);
}

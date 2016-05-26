package org.ggp.base.player.gamer.statemachine.thedefeated;

import org.ggp.base.player.gamer.statemachine.sample.TreeSearchWorker;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.Node;
import org.ggp.base.player.gamer.statemachine.thedefeated.node.NodePool;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class ThreadManager
{
	private final int size;
	private Thread[] threads;
	private TreeSearchWorker[] workers;

	private volatile Node oldRoot;
	private volatile int oldRootReference;

	private volatile Node newRoot;
	private volatile int newRootReference;

	public ThreadManager(int size)
	{
		this.size = size;
		threads = new Thread[size];
		workers = new TreeSearchWorker[size];
		for (int i = 0; i < size; i++)
		{
			workers[i] = new TreeSearchWorker(i, this);
			threads[i] = new Thread(workers[i]);
		}

		oldRoot = null;
		newRoot = null;
		oldRootReference = 0;
	}

	public void initializeWorkers(AugmentedStateMachine stateMachine, Role role)
	{
		TreeSearchWorker.globalInit();

		for (int i = 0; i < size; i++)
		{
			workers[i].init(stateMachine, role);
		}
	}

	public void startWorkers()
	{
		for (int i = 0; i < size; i++)
		{
			threads[i].setName("TreeSearchWorker-" + i);
			threads[i].setPriority(MachineParameters.WORKER_THREAD_PRIORITY);
			threads[i].start();
		}
	}

	public void stopWorkers()
	{
		for (int i = 0; i < size; i++)
		{
			threads[i].interrupt();
			threads[i] = new Thread(workers[i]);
		}
	}

	public synchronized void updateReference(Node oldRoot, Node newRoot)
	{
		if (this.oldRoot == oldRoot)
		{
			oldRootReference--;
		}

		if (this.newRoot == newRoot)
		{
			newRootReference++;
		}

		if (oldRootReference == 0)
		{
			NodePool.collect(this.oldRoot);
			this.oldRoot = null;
			oldRootReference = 0;
		}
	}

	public void updateWorkers(Node root, int minDepth) throws MoveDefinitionException, TransitionDefinitionException
	{
		oldRoot = newRoot;
		oldRootReference = newRootReference;

		newRoot = root;
		newRootReference = 0;

		for (int i = 0; i < size; i++)
		{
			workers[i].setRoot(root);
			workers[i].setMinDepth(minDepth);
		}
	}
}

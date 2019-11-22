package lpbcast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;

public class TopologyBuilder implements ContextBuilder<Object> {

	private Random rnd;
	private ArrayList<Node> nodes;
	private int broadcast_rate;
	private double unsubscribe_rate;

	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("Lpbcast");

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50);

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("view network", context, true);
		Network<Object> network = netBuilder.buildNetwork();

		Parameters params = RunEnvironment.getInstance().getParameters();

		int seed = params.getInteger("randomSeed");
		double crash_pr = params.getDouble("crash_pr");
		double recoveryInterval = params.getDouble("recoveryInterval");
		double message_loss = params.getDouble("message_loss");
		int retrieve_delay = params.getInteger("retrieve_delay");
		int r = params.getInteger("r");
		int nodes_size = params.getInteger("nodes_size");
		this.broadcast_rate = params.getInteger("broadcast_rate");
		this.unsubscribe_rate = params.getDouble("unsubscribe_rate");
		int F = params.getInteger("fanout");

		int l = params.getInteger("l");
		int init_view_size = params.getInteger("init_view_size");
		int long_ago = params.getInteger("long_ago");
		int events_size = params.getInteger("events_size");
		int eventIds_size = params.getInteger("eventIds_size");
		int subs_size = params.getInteger("subs_size");
		double k = params.getDouble("k");
		int unsubs_size = params.getInteger("unsubs_size");
		int unsubs_max_age = params.getInteger("unsubs_max_age");
		double unsubscribe_interval = params.getDouble("unsubscribe_interval");

		this.rnd = new Random(seed);
		this.nodes = new ArrayList<>();
		for (int i = 0; i < nodes_size; i++) {
			Node node = new Node(network, rnd, i, crash_pr, recoveryInterval, message_loss, l, init_view_size, F,
					events_size, long_ago, eventIds_size, subs_size, k, unsubs_size, unsubs_max_age,
					unsubscribe_interval, retrieve_delay, r);
			this.nodes.add(node);
			context.add(node);
		}

		for (int i = 0; i < nodes_size; i++) {
			HashSet<Integer> randomTargets = new HashSet<>();
			while (randomTargets.size() < init_view_size) {
				int randomId = rnd.nextInt(this.nodes.size());
				if (randomId != i) {
					randomTargets.add(randomId);
				}
			}

			ArrayList<Node> neighbours = new ArrayList<>();
			for (Integer id : randomTargets) {
				neighbours.add(this.nodes.get(id));
			}

			this.nodes.get(i).initView(neighbours);
		}

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters scheduleParams = ScheduleParameters.createRepeating(0.5, 1);
		schedule.schedule(scheduleParams, this, "eventGenerator");

		ScheduleParameters scheduleParamsUnsubs = ScheduleParameters.createRepeating(this.unsubscribe_rate + 0.25,
				this.unsubscribe_rate);
		schedule.schedule(scheduleParamsUnsubs, this, "startUnsubProcedure");

		return context;
	}

	public void startUnsubProcedure() {
		int targetId = rnd.nextInt(this.nodes.size());
		this.nodes.get(targetId).unsubscribe();
	}

	public void eventGenerator() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		int generatedEvents = 0;
		while (generatedEvents < this.broadcast_rate) {
			int index = rnd.nextInt(this.nodes.size());
			Node node = this.nodes.get(index);
			if (node.isAlive()) {
				node.broadcast(schedule.getTickCount(), generatedEvents);
				generatedEvents++;
			}
		}
	}
}

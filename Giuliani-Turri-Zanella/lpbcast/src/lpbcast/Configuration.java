package lpbcast;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Configuration {
	public static int INITIAL_NODES_NUMBER;
	public static double SUBSCRIBE_PROBABILITY;
	public static double UNSUBSCRIBE_PROBABILITY;
	public static double EVENT_GENERATION_PROBABILITY;
	public static int INITIAL_EVENTS_IN_SYSTEM;
	public static int EVENTS_MAX_SIZE;
	public static int EVENTIDS_MAX_SIZE;
	public static int VIEW_MAX_SIZE;
	public static int SUBS_MAX_SIZE;
	public static int UNSUBS_MAX_SIZE;
	public static int ARCHIVED_MAX_SIZE;
	public static int UNSUBS_VALIDITY;
	public static int F;
	public static int LONG_AGO;
	public static double K;
	public static int RECOVERY_TIMEOUT;
	public static int K_RECOVERY;
	public static boolean SYNC;
	public static int MESSAGE_MAX_DELAY;
	public static boolean AGE_BASED_MESSAGE_PURGING;
	public static boolean FREQUENCY_BASED_MEMBERSHIP_PURGING;
	public static int EVENT_VISUAL_TIME;
	public static int SUB_VISUAL_TIME;
	public static int UNSUB_VISUAL_TIME;
	public static int EVENTS_GENERATED_PER_ROUND;
	
	public static void load() {
		Parameters p = RunEnvironment.getInstance().getParameters();
		Configuration.INITIAL_NODES_NUMBER = p.getInteger("node_number");
		Configuration.SUBSCRIBE_PROBABILITY = p.getFloat("sub_probability");
		Configuration.UNSUBSCRIBE_PROBABILITY = p.getFloat("unsub_probability");		
		Configuration.EVENT_GENERATION_PROBABILITY = p.getFloat("event_generation_probability");
		Configuration.INITIAL_EVENTS_IN_SYSTEM = p.getInteger("initial_event_in_system");
		Configuration.EVENTS_MAX_SIZE = p.getInteger("events_max_size");
		Configuration.EVENTIDS_MAX_SIZE = p.getInteger("eventids_max_size");
		Configuration.VIEW_MAX_SIZE = p.getInteger("view_max_size");
		Configuration.SUBS_MAX_SIZE = p.getInteger("subs_max_size");
		Configuration.UNSUBS_MAX_SIZE = p.getInteger("unsubs_max_size");
		Configuration.ARCHIVED_MAX_SIZE = p.getInteger("archived_max_size");
		Configuration.UNSUBS_VALIDITY = p.getInteger("unsubs_validity");
		Configuration.F = p.getInteger("f");
		Configuration.LONG_AGO = p.getInteger("long_ago");
		Configuration.K = p.getFloat("k");
		Configuration.RECOVERY_TIMEOUT = p.getInteger("recovery_timeout");
		Configuration.K_RECOVERY = p.getInteger("k_recovery");
		Configuration.SYNC = p.getBoolean("sync");
		Configuration.MESSAGE_MAX_DELAY = p.getInteger("message_max_delay");
		Configuration.AGE_BASED_MESSAGE_PURGING = p.getBoolean("age_based_message_purging");
		Configuration.FREQUENCY_BASED_MEMBERSHIP_PURGING = p.getBoolean("frequency_based_membership_purging");
		Configuration.EVENT_VISUAL_TIME = p.getInteger("event_visual_time");
		Configuration.SUB_VISUAL_TIME = p.getInteger("sub_visual_time");
		Configuration.UNSUB_VISUAL_TIME = p.getInteger("unsub_visual_time");
		Configuration.EVENTS_GENERATED_PER_ROUND = p.getInteger("events_generated_per_round");
	}
}

package message;

import pbcast.Process;

public class Handler {
	protected Process from;
	protected Process to;
	protected Message msg;

	public Handler(Message msg, Process from, Process to) {
		this.msg = msg;
		this.from = from;
		this.to = to;
	}
	
	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public Process getFrom() {
		return this.from;
	}

	public void setTo(Process to) {
		this.to = to;
	}

	public Process getTo() {
		return this.to;
	}

	public void setFrom(Process from) {
		this.from = from;
	}
}

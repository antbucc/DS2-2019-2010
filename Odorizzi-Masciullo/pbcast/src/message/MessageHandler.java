package message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import pbcast.Process;
import tree.Tree;

public class MessageHandler extends Handler{
	private Tree tree;
	private int gossip_count;
	
	public MessageHandler(Message msg, Process from, Process to, Tree tree, int gossip_count) {
		super(msg, from, to);
		this.tree = tree;
		this.gossip_count = gossip_count;
	}
	
	public String summary() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    md.update(this.msg.getContent().getBytes());
	    byte[] digest = md.digest();
	    StringBuilder sb = new StringBuilder();
	    for (byte b : digest)
	        sb.append(String.format("%02X ", b));
	    String myHash = sb.toString().toUpperCase();
		
		String id_tree = Integer.toString(this.tree.getId());
		//String gossip = Integer.toString(this.gossip_count);
		return  myHash + " ; " + id_tree;
	}

	public int keep_gossip() {
		return ++gossip_count;
	}

	public Tree getTree() {
		return this.tree;
	}

	public void setTree(Tree tree) {
		this.tree = tree;
	}

	public int getGossipCount() {
		return gossip_count;
	}
	
	@ Override
	public boolean equals(Object o) {
		boolean comparation = false;
		if(o instanceof MessageHandler) {
			MessageHandler msg_hnd = (MessageHandler) o;
			if(this.summary() == msg_hnd.summary())
				comparation = true;
		}
		return comparation;
	}
	
}

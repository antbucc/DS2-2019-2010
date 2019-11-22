package tree;

import java.util.ArrayList;

import pbcast.Process;

public class Node {
	private int label;
	private Process p;
	private ArrayList<Node> children;
	
	Node(int label, Process p) {
		this.label = label;
		this.p = p;
		this.children = new ArrayList<Node>();
	}
	int getLabel() {
		return label;
	}
	void setLabel(int label) {
		this.label = label;
	}
	public Process getP() {
		return p;
	}
	void setP(Process p) {
		this.p = p;
	}
	void addChild(Node n) {
		this.children.add(n);
	}
	public ArrayList<Node> getChildren() {
		return this.children;
	}
	int getChildrenAmount() {
		return this.children.size();
	}
}

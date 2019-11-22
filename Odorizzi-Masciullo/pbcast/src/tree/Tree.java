package tree;

import java.util.ArrayList;

import pbcast.Process;

public class Tree {
	private Node root;
	ArrayList<Process> border;
	ArrayList<Node> nodes;
	private int id;
	
	public Tree(int id, Process p_root) {
		this.id = id;
		this.root = new Node(1, p_root);
		this.border = new ArrayList<Process>(); 
		this.border.add(p_root);
		this.nodes = new ArrayList<Node>();
		this.nodes.add(root);
	}
	
	public Process getRoot() {
		return this.root.getP();
	}
	
	public int getId() {
		return this.id;
	}
	
	public ArrayList<Node> getNodes() {
		return this.nodes;
	}
	
	public Node find_node(Process p) {
		for (Node n : this.nodes) 
			if (n.getP() == p)
				return n;
		return null;
	}
	
	public ArrayList<Process> getChildrenOf(Process p) {
		ArrayList<Node> children_nodes = this.find_node(p).getChildren();
		ArrayList<Process> children_processes = new ArrayList<Process>();
		for(Node node : children_nodes)
			children_processes.add(node.getP());
		return children_processes;
	}
	
	public void add_children(Process father, ArrayList<Process> children) {
		Node node_father = this.find_node(father);
		if(node_father != null) {
			int new_label = node_father.getLabel() + 1;
			for(Process child : children)
				if (this.find_node(child) == null) {
					Node node_child = new Node(new_label, child);
					this.nodes.add(node_child);
					node_father.addChild(node_child);
				}
		}
	}
	
	public int getTreeSize() {
		return this.nodes.size();
	}
	
	private void composeBorder(Node node) {
		if (node.getChildrenAmount() == 0)
			this.border.add(node.getP());
		else
			for(Node adj : node.getChildren())
				composeBorder(adj);
	}
	
	public ArrayList<Process> getBorder() {
		this.border.clear();
		this.composeBorder(this.root);
		return this.border;
	}
	
	public boolean hasProcess(Process p) {
		for(Node n : this.nodes)
			if(n.getP() == p)
				return true;
		return false;
	}
	
}

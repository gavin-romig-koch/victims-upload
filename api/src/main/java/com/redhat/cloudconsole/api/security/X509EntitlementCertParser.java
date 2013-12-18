package com.redhat.cloudconsole.api.security;

/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */



import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 *  Most of this class is lifted almost verbatim from the Candlepin source code
 *  @see  org.candlepin.util.X509V3ExtensionUtil.java (https://github.com/candlepin/candlepin/tree/master/src)
 */
public class X509EntitlementCertParser {

	private static final Object END_NODE = new Object();
	private long pathNodeId = 0;
	private long huffNodeId = 0;
   
	
	public void printTree(PathNode pn, int tab) {
		StringBuffer nodeRep = new StringBuffer();
		for (int i = 0; i <= tab; i++) {
			nodeRep.append("  ");
		}
		nodeRep.append("Node [");
		nodeRep.append(pn.getId());
		nodeRep.append("]");

		for (PathNode parent : pn.getParents()) {
			nodeRep.append(" ^ [");
			nodeRep.append(parent.getId());
			nodeRep.append("]");
		}
		for (NodePair cp : pn.getChildren()) {
			nodeRep.append(" v [");
			nodeRep.append(cp.getName());
			nodeRep.append(" {");
			nodeRep.append(cp.getConnection().getId());
			nodeRep.append("} ]");
		}
		// log.debug("{}", nodeRep);
		for (NodePair cp : pn.getChildren()) {
			printTree(cp.getConnection(), tab + 1);
		}
	}

	public void printTrie(HuffNode hn, int tab) {
		StringBuffer nodeRep = new StringBuffer();
		for (int i = 0; i <= tab; i++) {
			nodeRep.append("  ");
		}
		nodeRep.append("Node [");
		nodeRep.append(hn.getId());
		nodeRep.append("]");

		nodeRep.append(", Weight [");
		nodeRep.append(hn.getWeight());
		nodeRep.append("]");

		nodeRep.append(", Value = [");
		nodeRep.append(hn.getValue());
		nodeRep.append("]");

		// log.debug("{}", nodeRep);
		if (hn.getLeft() != null) {
			printTrie(hn.getLeft(), tab + 1);
		}
		if (hn.getRight() != null) {
			printTrie(hn.getRight(), tab + 1);
		}
		//System.out.println("print node " + nodeRep.toString());
	}



	public List<String> orderStrings(PathNode parent) throws IOException {
		List<String> parts = new ArrayList<String>();
		// walk tree to make string map
		Map<String, Integer> segments = new HashMap<String, Integer>();
		Set<PathNode> nodes = new HashSet<PathNode>();
		buildSegments(segments, nodes, parent);
		for (String part : segments.keySet()) {
			if (!part.equals("")) {
				int count = segments.get(part);
				if (parts.size() == 0) {
					parts.add(part);
				} else {
					int pos = parts.size();
					for (int i = 0; i < parts.size(); i++) {
						if (count < segments.get(parts.get(i))) {
							pos = i;
							break;
						}
					}
					parts.add(pos, part);
				}
			}
		}
		// if (treeDebug) {
		// log.debug("Parts List: " + parts);
		// }
		return parts;
	}

	private void buildSegments(Map<String, Integer> segments,
			Set<PathNode> nodes, PathNode parent) {
		if (!nodes.contains(parent)) {
			nodes.add(parent);
			for (NodePair np : parent.getChildren()) {
				Integer count = segments.get(np.getName());
				if (count == null) {
					count = 0;
				}
				segments.put(np.getName(), ++count);
				buildSegments(segments, nodes, np.getConnection());
			}
		}
	}



	public String findHuffPath(HuffNode trie, Object need) {
		HuffNode left = trie.getLeft();
		HuffNode right = trie.getRight();
		if (left != null && left.getValue() != null) {
			if (need.equals(left.getValue())) {
				return "0";
			}
		}
		if (right != null && right.getValue() != null) {
			if (need.equals(right.getValue())) {
				return "1";
			}
		}
		if (left != null) {
			String leftPath = findHuffPath(left, need);
			if (leftPath.length() > 0) {
				return "0" + leftPath;
			}
		}
		if (right != null) {
			String rightPath = findHuffPath(right, need);
			if (rightPath.length() > 0) {
				return "1" + rightPath;
			}
		}
		return "";
	}

	public static String toJson(Object anObject) {
		String output = "";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
		mapper.setVisibility(JsonMethod.FIELD, Visibility.ANY);
		try {
			output = mapper.writeValueAsString(anObject);
		} catch (Exception e) {
			// log.error("Could no serialize the object to json " + anObject,
			// e);
		}
		return output;
	}


	public HuffNode makeTrie(List<HuffNode> nodesList) {
		// drop the first node if path node value, it is not needed
		if (nodesList.get(0).getValue() instanceof PathNode) {
			nodesList.remove(0);
		}
		while (nodesList.size() > 1) {
			int node1 = findSmallest(-1, nodesList);
			int node2 = findSmallest(node1, nodesList);
			HuffNode hn1 = nodesList.get(node1);
			HuffNode hn2 = nodesList.get(node2);
			HuffNode merged = mergeNodes(hn1, hn2);
			nodesList.remove(hn1);
			nodesList.remove(hn2);
			nodesList.add(merged);
		}
		// if (treeDebug) {
		printTrie(nodesList.get(0), 0);
		// }
		return nodesList.get(0);
	}

	private int findSmallest(int exclude, List<HuffNode> nodes) {
		int smallest = -1;
		for (int index = 0; index < nodes.size(); index++) {
			if (index == exclude) {
				continue;
			}
			if (smallest == -1
					|| nodes.get(index).getWeight() < nodes.get(smallest)
							.getWeight()) {
				smallest = index;
			}
		}
		return smallest;
	}

	private HuffNode mergeNodes(HuffNode node1, HuffNode node2) {
		HuffNode left = node1;
		HuffNode right = node2;
		HuffNode parent = new HuffNode(null, left.weight + right.weight, left,
				right);
		return parent;
	}

	

//	byte[] entityData = cert
//			.getExtensionValue("1.3.6.1.4.1.2312.9.7");
//	 System.out.println(cert.getExtensionValue("1.3.6.1.4.1.2312.9.7"));
//     ASN1Primitive parsed = JcaX509ExtensionUtils
//	// .parseExtensionValue(entityData);
//	// System.out.println("Parsed structure  : " +
//	// ASN1Dump.dumpAsString(parsed));
//	// DEROctetString dataOct = new DEROctetString(entityData);
//	org.bouncycastle.asn1.ASN1Primitive prim = JcaX509ExtensionUtils
//			.parseExtensionValue(entityData);
//	org.bouncycastle.asn1.ASN1OctetString octs = (org.bouncycastle.asn1.ASN1OctetString) prim;
//	List<String> list = new X509EntitlementCertParser()
//			.hydrateContentPackage(octs.getOctets());
//	System.out.println(list);
	public List<String> hydrateContentPackage(byte[] payload)
			throws IOException, UnsupportedEncodingException {
		List<HuffNode> pathDictionary = new ArrayList<HuffNode>();
		List<HuffNode> nodeDictionary = new ArrayList<HuffNode>();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Inflater i = new Inflater();
		InflaterOutputStream ios = new InflaterOutputStream(baos, i);
		ios.write(payload);
		ios.finish();
		long read = i.getBytesRead();

		String name = "";
		int weight = 1;
		for (byte b : baos.toByteArray()) {
			if (b == '\0') {
				pathDictionary.add(new HuffNode(name, weight++));
				name = "";
			} else {
				name += (char) b;
			}
		}
		pathDictionary.add(new HuffNode(END_NODE, weight));
		List<HuffNode> triePathDictionary = new ArrayList<HuffNode>();
		triePathDictionary.addAll(pathDictionary);
		HuffNode pathTrie = makeTrie(triePathDictionary);

		StringBuffer nodeBits = new StringBuffer();
		ByteArrayInputStream bais = new ByteArrayInputStream(payload,
				(int) read, (int) (payload.length - read));

		int value = bais.read();
		// check for size bits
		int nodeCount = value;
		if (value > 127) {
			byte[] count = new byte[value - 128];
			bais.read(count);
			int total = 0;
			for (int k = 0; k < value - 128; k++) {
				total = (total << 8) | (count[k] & 0xFF);
			}
			nodeCount = total;
		}
		value = bais.read();
		while (value != -1) {
			String someBits = Integer.toString(value, 2);
			for (int pad = 0; pad < 8 - someBits.length(); pad++) {
				nodeBits.append("0");
			}
			nodeBits.append(someBits);
			value = bais.read();
		}
		for (int j = 0; j < nodeCount; j++) {
			nodeDictionary.add(new HuffNode(new PathNode(), j));
		}
		List<HuffNode> trieNodeDictionary = new ArrayList<HuffNode>();
		trieNodeDictionary.addAll(nodeDictionary);
		HuffNode nodeTrie = makeTrie(trieNodeDictionary);

		// populate the PathNodes so we can rebuild the cool url tree
		Set<PathNode> pathNodes = populatePathNodes(nodeDictionary, pathTrie,
				nodeTrie, nodeBits);
		// find the root, he has no parents
		PathNode root = null;
		for (PathNode pn : pathNodes) {
			if (pn.getParents().size() == 0) {
				root = pn;
				break;
			}
		}
		// time to make the doughnuts
		List<String> urls = new ArrayList<String>();
		StringBuffer aPath = new StringBuffer();
		makeURLs(root, urls, aPath);
		return urls;
	}

	public List<String> hydrateContentPackage2(byte[] payload)
			throws IOException, UnsupportedEncodingException {
		List<HuffNode> pathDictionary = new ArrayList<HuffNode>();
		List<HuffNode> nodeDictionary = new ArrayList<HuffNode>();

		// ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Inflater i = new Inflater();
		// InflaterOutputStream ios = new InflaterOutputStream(baos, i);
		// ios.write(payload);
		// ios.finish();
		long read = payload.length;

		String name = "";
		int weight = 1;
		// for (byte b : baos.toByteArray()) {
		for (byte b : payload) {
			if (b == '\0') {
				pathDictionary.add(new HuffNode(name, weight++));
				name = "";
			} else {
				name += (char) b;
			}
		}
		pathDictionary.add(new HuffNode(END_NODE, weight));
		List<HuffNode> triePathDictionary = new ArrayList<HuffNode>();
		triePathDictionary.addAll(pathDictionary);
		HuffNode pathTrie = makeTrie(triePathDictionary);

		StringBuffer nodeBits = new StringBuffer();
		ByteArrayInputStream bais = new ByteArrayInputStream(payload,
				(int) read, (int) (payload.length - read));

		int value = bais.read();
		// check for size bits
		int nodeCount = value;
		if (value > 127) {
			byte[] count = new byte[value - 128];
			bais.read(count);
			int total = 0;
			for (int k = 0; k < value - 128; k++) {
				total = (total << 8) | (count[k] & 0xFF);
			}
			nodeCount = total;
		}
		value = bais.read();
		while (value != -1) {
			String someBits = Integer.toString(value, 2);
			for (int pad = 0; pad < 8 - someBits.length(); pad++) {
				nodeBits.append("0");
			}
			nodeBits.append(someBits);
			value = bais.read();
		}
		for (int j = 0; j < nodeCount; j++) {
			nodeDictionary.add(new HuffNode(new PathNode(), j));
		}
		//System.out.println("Dictionary is " + nodeDictionary);
		List<HuffNode> trieNodeDictionary = new ArrayList<HuffNode>();
		trieNodeDictionary.addAll(nodeDictionary);
		HuffNode nodeTrie = makeTrie(trieNodeDictionary);

		// populate the PathNodes so we can rebuild the cool url tree
		Set<PathNode> pathNodes = populatePathNodes(nodeDictionary, pathTrie,
				nodeTrie, nodeBits);
		// find the root, he has no parents
		PathNode root = null;
		for (PathNode pn : pathNodes) {
			if (pn.getParents().size() == 0) {
				root = pn;
				break;
			}
		}
		// time to make the doughnuts
		List<String> urls = new ArrayList<String>();
		StringBuffer aPath = new StringBuffer();
		makeURLs(root, urls, aPath);
		return urls;
	}

	public Object findHuffNodeValueByBits(HuffNode trie, String bits) {
		HuffNode left = trie.getLeft();
		HuffNode right = trie.getRight();

		if (bits.length() == 0) {
			return trie.getValue();
		}

		char bit = bits.charAt(0);
		if (bit == '0') {
			if (left == null) {
				throw new RuntimeException("Encoded path not in trie");
			}
			return findHuffNodeValueByBits(left, bits.substring(1));
		} else if (bit == '1') {
			if (right == null) {
				throw new RuntimeException("Encoded path not in trie");
			}
			return findHuffNodeValueByBits(right, bits.substring(1));
		}
		return null;
	}

	private Set<PathNode> populatePathNodes(List<HuffNode> nodeDictionary,
			HuffNode pathTrie, HuffNode nodeTrie, StringBuffer nodeBits) {
		Set<PathNode> pathNodes = new HashSet<PathNode>();
		for (HuffNode node : nodeDictionary) {
			pathNodes.add((PathNode) node.getValue());
			boolean stillNode = true;
			while (stillNode) {
				// get first child name
				// if its END_NODE we are done
				String nameValue = null;
				StringBuffer nameBits = new StringBuffer();
				while (nameValue == null && stillNode) {
					nameBits.append(nodeBits.charAt(0));
					nodeBits.deleteCharAt(0);
					Object lookupValue = findHuffNodeValueByBits(pathTrie,
							nameBits.toString());
					if (lookupValue != null) {
						if (lookupValue.equals(END_NODE)) {
							stillNode = false;
							break;
						}
						nameValue = (String) lookupValue;
						//System.out.println("Name Value is "+ nameValue);
					}
					if (nodeBits.length() == 0) {
						stillNode = false;
					}
				}

				PathNode nodeValue = null;
				StringBuffer pathBits = new StringBuffer();
				while (nodeValue == null && stillNode) {
					pathBits.append(nodeBits.charAt(0));
					nodeBits.deleteCharAt(0);
					PathNode lookupValue = (PathNode) findHuffNodeValueByBits(
							nodeTrie, pathBits.toString());
					if (lookupValue != null) {
						nodeValue = lookupValue;
						nodeValue.addParent((PathNode) node.getValue());
						((PathNode) node.getValue()).addChild(new NodePair(
								nameValue, nodeValue));
					}
					if (nodeBits.length() == 0) {
						stillNode = false;
					}
				}
			}
		}
		return pathNodes;
	}

	private void makeURLs(PathNode root, List<String> urls, StringBuffer aPath) {
		if (root.getChildren().size() == 0) {
			urls.add(aPath.toString());
		}
		for (NodePair child : root.getChildren()) {
			StringBuffer childPath = new StringBuffer(aPath.substring(0));
			childPath.append("/");
			childPath.append(child.getName());
			makeURLs(child.getConnection(), urls, childPath);
		}
	}

	private byte[] processPayload(String payload) throws IOException,
			UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(baos);
		dos.write(payload.getBytes("UTF-8"));
		dos.finish();
		dos.close();
		return baos.toByteArray();
	}

	/**
	 * 
	 * HuffNode
	 */
	public class HuffNode {
		private long id = 0;
		private Object value = null;
		private int weight = 0;
		private HuffNode left = null;
		private HuffNode right = null;

		public HuffNode(Object value, int weight, HuffNode left, HuffNode right) {
			this.value = value;
			this.weight = weight;
			this.left = left;
			this.right = right;
			this.id = huffNodeId++;
		}

		public HuffNode(Object value, int weight) {
			this.value = value;
			this.weight = weight;
			this.id = huffNodeId++;
		}

		public Object getValue() {
			return this.value;
		}

		public int getWeight() {
			return this.weight;
		}

		public HuffNode getLeft() {
			return this.left;
		}

		public HuffNode getRight() {
			return this.right;
		}

		long getId() {
			return this.id;
		}

		public String toString() {
			return "ID: " + id + ", Value: " + value + ", Weight: " + weight
					+ ", Left: " + left + ", Right: " + right;
		}
	}

	/**
	 * 
	 * PathNode
	 */

	public class PathNode {
		private long id = 0;
		private List<NodePair> children = new ArrayList<NodePair>();
		private List<PathNode> parents = new ArrayList<PathNode>();

		public PathNode() {
			this.id = pathNodeId++;
		}

		public long getId() {
			return id;
		}

		void addChild(NodePair cp) {
			this.children.add(cp);
		}

		void addParent(PathNode cp) {
			if (!parents.contains(cp)) {
				this.parents.add(cp);
			}
		}

		public List<NodePair> getChildren() {
			Collections.sort(this.children);
			return this.children;
		}

		List<PathNode> getParents() {
			return this.parents;
		}

		void setParents(List<PathNode> parents) {
			this.parents = parents;
		}

		void addParents(List<PathNode> parents) {
			for (PathNode pn : parents) {
				addParent(pn);
			}
		}

		boolean isEquivalentTo(PathNode that) {
			if (this.getId() == that.getId()) {
				return true;
			}
			// same number of children with the same names for child nodes
			if (this.getChildren().size() != that.getChildren().size()) {
				return false;
			}
			for (NodePair thisnp : this.getChildren()) {
				boolean found = false;
				for (NodePair thatnp : that.getChildren()) {
					if (thisnp.getName().equals(thatnp.getName())) {
						if (thisnp.getConnection().isEquivalentTo(
								thatnp.getConnection())) {
							found = true;
							break;
						} else {
							return false;
						}
					}
				}
				if (!found) {
					return false;
				}
			}
			return true;
		}

		public String toString() {
			StringBuffer parentList = new StringBuffer("ID: ");
			parentList.append(id).append(", Parents");
			for (PathNode parent : parents) {
				parentList.append(": ").append(parent.getId());
			}

			// "ID: " + id + ", Parents" + parentList + ", Children: " +
			// children;
			return parentList.append(", Children: ").append(children)
					.toString();
		}
	}

	/**
	 * 
	 * NodePair
	 */

	public static class NodePair implements Comparable {
		private String name;
		private PathNode connection;

		NodePair(String name, PathNode connection) {
			this.name = name;
			this.connection = connection;
		}

		public String getName() {
			return name;
		}

		public PathNode getConnection() {
			return connection;
		}

		void setConnection(PathNode connection) {
			this.connection = connection;
		}

		public String toString() {
			return "Name: " + name + ", Connection: " + connection.getId();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Object other) {
			return this.name.compareTo(((NodePair) other).name);
		}

		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (!(other instanceof NodePair)) {
				return false;
			}

			return this.name.equals(((NodePair) other).getName());
		}

		public int hashCode() {
			return name.hashCode();
		}
	}
}

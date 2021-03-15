package ctrmap.pokescript.ide.autocomplete.nodes;

import ctrmap.pokescript.ide.autocomplete.gui.ACHintPanel;
import java.util.ArrayList;
import java.util.List;

public class AbstractNode {

	public String name;
	public List<String> aliases = new ArrayList<>();
	public AbstractNode parent;
	public List<AbstractNode> children = new ArrayList<>();

	public AbstractNode(String name) {
		this.name = name;
		this.parent = null;
	}

	public final String getFullName() {
		return parent != null ? parent.getFullName() + "." + name : name;
	}

	public String getPrintableShortName(){
		return name;
	}
	
	public final void addAlias(String alias) {
		aliases.add(alias);
	}

	public final void removeAlias(String alias) {
		aliases.remove(alias);
	}

	public void setParent(AbstractNode parent) {
		this.parent = parent;
	}

	public final void addChild(AbstractNode n) {
		n.setParent(this);
		children.add(n);
	}

	public final void addChildUnbound(AbstractNode n) {
		children.add(n);
	}

	public List<AbstractNode> getRecommendations(String query) {
		return getRecommendations(query, null);
	}

	public List<AbstractNode> getRecommendations(String query, String matchingParentAlias) {
		//The query should be the full beginning of a member call, e.g. Thing.Thing2.Thi<end of query>
		//If the query gets matched to the start of the node's full name, the node is added as a recommendation
		//Otherwise, if the node's full name gets matched to the start of the query, recommendations from child nodes are requested
		List<AbstractNode> r = new ArrayList<>();

		boolean queryHasAlias = false;
		boolean fullEquals = false;

		String fullName = matchingParentAlias != null ? matchingParentAlias + "." + name : getFullName();
		addAlias(fullName);
		for (String a : aliases) {
			String lcA = lastNameToLowerCase(a);
			if (lcA.equals(query)) {
				fullEquals = true;
				queryHasAlias = true;
				//aliasHasQuery = false;
				break;
			} else if (lcA.startsWith(query)) {
				queryHasAlias = true;
			}
			/*else if (query.startsWith(a)) {
				aliasHasQuery = true;
			}*/
		}
		removeAlias(fullName);

		if (query.length() == 0) {
			if (fullEquals) {
				//aliasHasQuery = true;	//root node, add children only (don't recomment the root node hack)
				queryHasAlias = false;
			} else if (parent != null) {
				queryHasAlias = false;	//presumably one of the children obtained by the above loop - this case should normally never occur, but just to be sure
			}
		}

		if (queryHasAlias) {
			r.add(this);
		} else {
			//we can't optimize this with aliasHasQuery since children can have aliases that don't start with the parent's full path
			String matchingAlias = null;
			for (String a : aliases) {
				if (query.startsWith(a)) {
					matchingAlias = a;
				}
			}
			for (AbstractNode n : children) {
				r.addAll(n.getRecommendations(query, matchingAlias));
			}
		}

		return r;
	}

	public static String lastNameToLowerCase(String fullName) {
		int liodot = fullName.lastIndexOf('.');
		if (liodot == -1) {
			return fullName.toLowerCase();
		}
		return fullName.substring(0, liodot) + fullName.substring(liodot, fullName.length()).toLowerCase();
	}

	public AbstractNode findNode(String fullName) {
		if (getFullName().equals(fullName)) {
			return this;
		} else {
			AbstractNode find = null;
			for (AbstractNode c : children) {
				find = c.findNode(fullName);
				if (find != null) {
					break;
				}
			}
			return find;
		}
	}
	
	private ACHintPanel hintPanel = null;
	
	public ACHintPanel getHintPanel(){
		if (hintPanel == null){
			hintPanel = new ACHintPanel(this);
		}
		return hintPanel;
	}
}

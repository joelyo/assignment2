package a2;

import java.util.*;

public class IterativeAllocator {

	/**
	 * @precondition: Neither of the inputs are null or contain null elements.
	 *                The parameter donations is a list of distinct donations
	 *                such that for each d in donations, d.getTotal() equals
	 *                d.getUnspent(); and for each p in projects
	 *                p.allocatedFunding() equals 0.
	 * @postcondition: returns false if there no way to completely fund all of
	 *                 the given projects using the donations, leaving both the
	 *                 input list of donations and set of projects unmodified;
	 *                 otherwise returns true and allocates to each project
	 *                 funding from the donations. The allocation to each
	 *                 project must be complete and may not violate the
	 *                 conditions of the donations.
	 */
	public static boolean canAllocate(List<Donation> donations,
			Set<Project> projects) {
		// get set of potential paths
		Set<List<Project>> paths = paths(donations, projects);
		// check to see if a positive integer and path still exists that satisfies properties (1)-(3)
		while (checkValidPaths(paths, donations)) {
			// iterate over each donation and path to allocate funds if possible
			for (Donation d : donations) {
				for (List<Project> path : paths) {
					if (checkProperties(Math.min(path.get(path.size()-1).neededFunds(), d.getUnspent()), path, donations)) {
						allocateDonations(Math.min(path.get(path.size()-1).neededFunds(), d.getUnspent()), path, donations);
					}
				}
			}
		}
		// check if all projects are allocated
		if (allAllocated(projects)) {
			return true;
		}
		// deallocates all projects
		deallocateAll(projects);
		return false;
	}
	
	/**
	 * returns a map that represents a graph of the given projects
	 */
	private static Map<Project, Set<Project>> graph(List<Donation> donations, Set<Project> projects) {
		Map<Project, Set<Project>> map = new HashMap<Project, Set<Project>>();
		for (Project p1 : projects) {
			// create key that represents a vertex
			map.put(p1, new HashSet<Project>());
			for (Project p2: projects) {
				if (!p1.equals(p2)) {
					// check to see if an edge exists between p1 and p2
					for (Donation d : donations) {
						if (d.canBeUsedFor(p1) && d.canBeUsedFor(p2)) {
							map.get(p1).add(p2);
						}
					}
				}
			}
		}
		return map;
	}
	
	/**
	 * returns a map to track whether each project has been visited
	 */
	private static Map<Project, Boolean> visited(Set<Project> projects) {
		Map<Project, Boolean> map = new HashMap<Project, Boolean>();
		for (Project p : projects) {
			map.put(p, false);
		}
		return map;
	}
	
	/**
	 * populates a list with a path between 2 projects found using DFS
	 */
	private static Boolean pathDFS(Map<Project, Set<Project>> map, List<Project> list, Project start, Project end, Map<Project, Boolean> visited) {
		// mark start project as visited
		visited.put(start, true);
		// add start project to path list
		list.add(start);
		// check to see if end project is reached
		if (start.equals(end)) {
			return true;
		}
		// iterate over the outgoing edges of the start project
		for (Project p : map.get(start)) {
			// check if the connecting project has been visited
			if (visited.get(p).equals(false)) {
				visited.put(p, true);
				if (pathDFS(map, list, p, end, visited)) {
					return true;
				}
				// remove connecting project from list
				list.remove(list.size()-1);
			}
		}
		// remove start project from list
		list.remove(list.size()-1);
		return false;
	}
	
	/**
	 * returns a set of paths that can be traversed in the graph
	 */
	private static Set<List<Project>> paths(List<Donation> donations, Set<Project> projects) {
		// a set that contains paths
		Set<List<Project>> paths = new HashSet<List<Project>>();
		// get the graph of projects
		Map<Project, Set<Project>> map = graph(donations, projects);
		// iterate over every project pair and add a path to the set if found
		for (Project p :  projects) {
			for (Project p2 : projects) {
				List<Project> path = new ArrayList<Project>();
				pathDFS(map, path, p, p2, visited(projects));
				if (!path.isEmpty()) {
					paths.add(path);
				}
			}
		}
		return paths;
	}
	
	/**
	 * returns true if a positive integer x and a path of n>0 distinct projects path satisfies properties (1)-(3)
	 */
	private static boolean checkProperties(int x, List<Project> path, List<Donation> donations) {
		// check that project path[n-1] is currently underfunded by at least x dollars and that x is a positive number
		if (path.get(path.size()-1).neededFunds() < x || !(x > 0)) {
			return false;
		}
		// check that x dollars of funding could be allocated from the available donations to project path[0].
		int available = 0;
		for (Donation d : donations) {
			if (d.canBeUsedFor(path.get(0))) {
				available = available + d.getUnspent();
			}
		}
		if (available < x) {
			return false;
		}
		// check that for each i=n-2 to 0, x dollars from project path[i] could be transferred to project path[i+1]
		for (int i = path.size()-2; i >= 0; i--) {
			Set<Map.Entry<Donation, Integer>> it = path.get(i).getAllocations().entrySet();
			int amount = 0;
			for (Map.Entry<Donation, Integer> entry : it) {
				if (entry.getKey().canBeUsedFor(path.get(i+1))) {
					amount = amount + entry.getValue();
				}
			}
			if (amount < x) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * returns true if there exists a positive integer x and a path of n>0 distinct projects path satisfying properties (1)-(3)
	 */
	private static boolean checkValidPaths(Set<List<Project>> paths, List<Donation> donations) {
		boolean check = false;
		for (Donation d : donations) {
			for (List<Project> path : paths) {
				if (checkProperties(Math.min(path.get(path.size()-1).neededFunds(), d.getUnspent()), path, donations)) {
					check = true;
				}
			}
		}
		return check;
	}
	
	/**
	 * allocates x to the first project in the path and transfers x along the path of projects
	 */
	private static void allocateDonations(int x, List<Project> path, List<Donation> donations) {
		// transfer x along the path of projects
		for (int i=path.size()-2; i >= 0; i--) {
			path.get(i+1).transfer(x, path.get(i));
		}
		// allocate x from available donations to path[0]
		for (Donation d : donations) {
			if (d.canBeUsedFor(path.get(0)) && x != 0) {
				int available = d.getUnspent();
				path.get(0).allocate(d, Math.min(x, available));
				x = x - Math.min(x, available);
			}
		}
	}
	
	/**
	 * returns true if all projects are allocated
	 */
	private static boolean allAllocated(Set<Project> projects) {
		boolean check = true;
		for (Project p : projects) {
			if (!p.fullyFunded()) {
				check = false;
			}
		}
		return check;
	}
	
	/**
	 * deallocates all projects
	 */
	private static void deallocateAll(Set<Project> projects) {
		for (Project p : projects) {
			p.deallocateAll();
		}
	}

}

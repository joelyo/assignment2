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
		Set<List<Project>> paths = paths(donations, projects);
		while (checkValidPaths(paths, donations)) {
			for (Donation d : donations) {
				for (List<Project> path : paths) {
					if (checkProperties(Math.min(path.get(path.size()-1).neededFunds(), d.getUnspent()), path, donations)) {
						allocateDonations(Math.min(path.get(path.size()-1).neededFunds(), d.getUnspent()), path, donations);
					}
				}
			}
			errorTest(projects);
		}
		
		if (allAllocated(projects)) {
			return true;
		}
		deallocateAll(projects);
		return false; // TASK 3: REMOVE THIS LINE AND WRITE THIS METHOD
	}
	
	/**
	 * checks if a positive integer x and a path of n>0 distinct projects path satisfies properties (1)-(3)
	 */
	private static boolean checkProperties(int x, List<Project> path, List<Donation> donations) {
		// check that project path[n-1] is currently underfunded by at least x dollars
		if (path.get(path.size()-1).neededFunds() < x || x <= 0) {
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
		if (path.size() > 1) {
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
			
		}
		return true;
	}
	
	/**
	 * allocate donations
	 */
	private static void allocateDonations(int x, List<Project> path, List<Donation> donations) {
		if (path.size() > 1) {
			for (int i=path.size()-2; i >= 0; i--) {
				path.get(i+1).transfer(x, path.get(i));
			}
		}
		for (Donation d : donations) {
			if (d.canBeUsedFor(path.get(0)) && x != 0) {
				int available = d.getUnspent();
				path.get(0).allocate(d, Math.min(x, available));
				x = x - Math.min(x, available);
			}
		}
	}
	
	/**
	 * check to see if there is still a positive integer x and a path of n>0 distinct projects path satisfying properties (1)-(3)
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
	 * check if all projects are allocated
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
	
	/**
	 * check allocations of projects
	 */
	private static void errorTest(Set<Project> projects) {
		for (Project p : projects) {
			System.out.println(p.getName());
			System.out.println(p.getAllocations());
		}
		System.out.println();
	}
	
	/**
	 * returns a map that represents a graph of the given projects
	 */
	private static Map<Project, Set<Project>> graph(List<Donation> donations, Set<Project> projects) {
		Map<Project, Set<Project>> map = new HashMap<Project, Set<Project>>();
		for (Project p1 : projects) {
			map.put(p1, new HashSet<Project>());
			for (Project p2: projects) {
				if (!p1.equals(p2)) {
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
	 * returns a map to track whether each node has been visited
	 */
	private static Map<Project, Integer> visited(Set<Project> projects, Project project) {
		Map<Project, Integer> map = new HashMap<Project, Integer>();
		for (Project p : projects) {
			map.put(p, 0);
			if (p.equals(project)) {
				map.put(p, 2);
			}
		}
		return map;
	}
	
	/**
	 * returns a set containing paths that can be traversed in the graph
	 */
	private static Set<List<Project>> paths(List<Donation> donations, Set<Project> projects) {
		Set<List<Project>> paths = new HashSet<List<Project>>();
		Map<Project, Set<Project>> map = graph(donations, projects);
		for (Project p :  projects) {
			paths.add(dfs(map, p, visited(projects, p)));
		}
		subPaths(paths);
		return paths;
	}
	
	
	/**
	 * expand the given set of paths to include it's sub-paths
	 */
	private static void subPaths(Set<List<Project>> paths) {
		Set<List<Project>> tempPaths = new HashSet<List<Project>>();
		for (List<Project> p : paths) {
			for (int i = p.size()-1; i> 0; i--) {
				tempPaths.add(p.subList(0, i));
			}
		}
		for (List<Project> p : tempPaths) {
			paths.add(p);
		}
	}
	
	/**
	 * return a path traversal of a graph of projects starting at the given node using depth-first search
	 */
	private static List<Project> dfs(Map<Project, Set<Project>> map, Project project, Map<Project, Integer> visited) {
		List<Project> list = new ArrayList<Project>();
		Set<Project> edges = map.get(project);
		for (Project p : edges) {
			if (visited.get(p) == 0) {
				visited.put(p, 2);
				List<Project> tempList = (dfs(map, p, visited));
				for (Project p2 : tempList) {
					list.add(p2);
				}
			} else {
				visited.put(p, 1);
			}
		}
		list.add(0, project);
		return list;
	}

}

package a2;

import java.util.*;

public class NaiveAllocator {

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
		return canAllocateHelper(donations, projects, 0);
	}
	
	public static boolean canAllocateHelper(List<Donation> donations,
			Set<Project> projects, int i) {
		// check if all of the projects have been completely allocated
		boolean projectsComplete = true;
		for (Project p : projects){
			if (!p.fullyFunded()) {
				projectsComplete = false;
			}
		}
		if (projectsComplete) {
			return true;
		}
		// check if index i is at the end of the donations list
		if (i == donations.size()) {
			return false;
		}
		Donation donation = donations.get(i);
		// check if the projects the donation could be spent on are completely allocated
		boolean donationProjectsComplete = true;
		for (Project p: projects) {
			if (donation.canBeUsedFor(p)) {
				if(!p.fullyFunded()) {
					donationProjectsComplete = false;
				} 
			}
		}
		if (donation.spent() || donationProjectsComplete) {
			return canAllocateHelper(donations, projects, i+1);
		}
		// allocate one dollar for each project that donation could be spent on that still needs funding
		for (Project p : projects) {
			if (donation.canBeUsedFor(p)) {
				if (!p.fullyFunded()) {
					p.allocate(donation, 1);
					if (canAllocateHelper(donations, projects, i)) {
						return true;
					} else {
						p.deallocate(donation, 1);
					}
				}
			}
		}
		return false;
	}

}

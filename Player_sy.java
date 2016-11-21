package sqdance.g4;

import sqdance.sim.Point;
	// a = (x,y) we want to find least distance between (x+eps/3, y) (x-eps/3, y) (x, y+eps/3) (x, y-eps/3) and b

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {

	private static double eps = 1e-7;

	private double delta = 1e-4;
	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 60; // 2 minutes

	private int numDancer = -1;
	private int roomSide = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1
	private int[][] danced; // cumulatived time in seconds for dance together

	//============= parameter for dance in turn strategy ========================
	private int numRowAuditoriumBlock = 10;

	private int[] sequence;
	private Point[] position;

	private int timeStamp = 0;


	public void init(int d, int room_side) {
		numDancer = d; roomSide = room_side;
		// initialize position
		position = new Point[d + 10];
		fixDancerPositions();
		// initialize sequence
		sequence = new int[d];
		for (int i = 0; i < d; ++i) {
			sequence[i] = i;
		}
	}

	public Point[] generate_starting_locations() {
		Point[] res = new Point[numDancer];
		for (int i = 0; i < numDancer; ++i) {
			res[sequence[i]] = position[i];
		}
		return res;
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		timeStamp += 6;
		Point[] res = new Point[numDancer];

		if (timeStamp % boredTime == 0) {

			sequenceSwap2();
			for (int i = 0; i < numDancer; ++i) {
				res[sequence[i]] = new Point(position[i].x - old_positions[sequence[i]].x,
						position[i].y - old_positions[sequence[i]].y);
			}

		} else {

			for (int i = 0; i < numDancer; ++i)
				res[i] = new Point(0, 0);

		}
		return res;
	}

	private void sequenceSwap2() {
		for (int i = 0; i + 1 < numDancer; i += 2) {
			int tmp = sequence[i];
			sequence[i] = sequence[i + 1];
			sequence[i + 1] = tmp;
		}

		for (int i = 1; i + 1 < numDancer; i += 2) {
			int tmp = sequence[i];
			sequence[i] = sequence[i + 1];
			sequence[i + 1] = tmp;
		}
	}

	private void fixDancerPositions() {
		// binary search the scale of the auditorium and stage
		boolean fitin = false;
		int l = 1, r = 100;
		while (l < r) {
			int mid = (l + r) >> 1;
			boolean ret = arrangePosition(mid);
			if (ret) r = mid; else l = mid + 1;
		}
		System.out.println("*************** numCol: " + l);
		if (!arrangePosition(l)) {
			System.out.println("************** change to crowd auditorium");
			arrangePositionCrowdAuditorium();
		}
	}

	private boolean arrangePosition(int numCol) {
		double yAudRange = (safeDis + delta) * (numCol - 1) + delta * 2;
		double yStageRange = (minDis + delta) * 2 + delta;
		double xrange = (safeDis + delta) * numRowAuditoriumBlock + delta;

		int cur = 0;
		for (int j = 0; ; ++j) {
			int indexl = cur;

			double yleft = j * (yAudRange + yStageRange);
			double yright = yleft + (yAudRange + yStageRange);
			if (yleft + yAudRange + (minDis + delta) + delta > roomSide - eps) break;

			for (int i = 0; ; ++i) {

				double xleft = i * xrange;
				double xright = xleft + xrange;
				if (xleft + (minDis + delta) * 1.5 + delta > roomSide - eps) break;
				
				// arrange two positions in stage
				Point tmp = new Point(xleft + (minDis + delta) / 2., yleft + yAudRange + (minDis + delta));
				if (!inside(tmp)) return false;
				position[cur++] = tmp;

				tmp = new Point(tmp.x + (minDis + delta), tmp.y);
				if (!inside(tmp)) return false;
				position[cur++] = tmp;
				if (cur >= numDancer) break;

				// arrange positions in auditorium
				double y = yleft + delta;
				for (int col = 0; col < numCol && y < yright - eps; ++col) {
					double x = xleft + (safeDis + delta) / 2.;
					for (int row = 0; row < numRowAuditoriumBlock && x < xright - eps; ++row) {
						
						tmp = new Point(x, y);
						if (!inside(tmp)) break;
						position[cur++] = tmp;
						if (cur >= numDancer) break;

						x += safeDis + delta;
					}
					if (cur >= numDancer) break;
					y += safeDis + delta;
				}
				if (cur >= numDancer) break;
			}

			int indexr = cur - 1;

			if (j % 2 == 1) {
				double maxX = 0.;
				for (int k = indexl; k <= (indexl + indexr) / 2; ++k) {
					maxX = Math.max(maxX, position[k].x);

					Point tmp = position[k];
					position[k] = position[indexl + indexr - k];
					position[indexl + indexr - k] = tmp;

					maxX = Math.max(maxX, position[k].x);
				}

				double shift = roomSide - maxX; 
				for (int k = indexl; k <= indexr; ++k) {
					position[k] = new Point(position[k].x + shift, position[k].y);
				}
			}
			if (cur >= numDancer) return true;
		}
		if (cur < numDancer) return false;
		return true;
	}

	private void arrangePositionCrowdAuditorium() {
		double yrange = (minDis + delta * 2.) * 3.;
		double xrange = (minDis + delta) + (minDis + delta * 2) + delta;

		int numBlock = (int)((roomSide - eps) / yrange) * (int)((roomSide - eps) / xrange);
		int numPitAuditorium = ((numDancer - 1) / numBlock) + 1 - 4;

		int cnt = 0;
		for (int j = 0; ; ++j) {
			int indexl = cnt;

			double yleft = yrange * j;
			double yright = yleft + yrange;

			for (int i = 0; ; ++i) {

				double xleft = xrange * i;
				double xright = xleft + xrange;
				if (xright > roomSide - eps) break;
				
				// arrange positions of two dancer pairs in stage
				double x1 = xleft + (minDis + delta * 2) / 2.;
				double x2 = x1 + minDis + delta;
				double y1 = yleft + (minDis + delta * 2);
				double y2 = y1 + (minDis + delta * 2);

				position[cnt++] = new Point(x1, y1);
				position[cnt++] = new Point(x2, y1);
				position[cnt++] = new Point(x1, y2);
				position[cnt++] = new Point(x2, y2);

				if (cnt >= numDancer) break;

				// arrange positions in crowd auditorium
				int done = 0;
				for (double x = xleft; x < xright; x += safeDis + delta, ++done) {
					position[cnt++] = new Point(x, yleft);
					if (cnt >= numDancer) break;
				}
				if (cnt >= numDancer) break;

				for (int k = done; k < numPitAuditorium; ++k) {
					position[cnt++] = new Point(xright, yleft);
				}
			}

			int indexr = cnt - 1;

			if (j % 2 == 1) {
				double maxX = 0.;
				for (int k = indexl; k <= (indexl + indexr) / 2; ++k) {
					maxX = Math.max(maxX, position[k].x);

					Point tmp = position[k];
					position[k] = position[indexl + indexr - k];
					position[indexl + indexr - k] = tmp;

					maxX = Math.max(maxX, position[k].x);
				}

				double shift = roomSide - maxX; 
				for (int k = indexl; k <= indexr; ++k) {
					position[k] = new Point(position[k].x + shift, position[k].y);
				}
			}

			if (cnt >= numDancer) return;
		}
	}

	private boolean inside(Point p) {
		if (p.x < eps || p.x > roomSide - eps || p.y < eps || p.y > roomSide - eps) return false;
		return true;
	}
}

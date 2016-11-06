package sqdance.g4;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {

	private static double eps = 1e-7;

	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 180; // 180 seconds

	private int d = -1;
	private double room_side = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1

	private int[][] danced; // cumulatived time in seconds for dance together

	//======= data structures for snake strategy =========
	private Point[] positions;

	private double delta = 1e-3;
	private double danceDis = minDis + delta;
	private double keepDis = danceDis + delta;
	//====================== end =========================

	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = (double) room_side;
		
		//data structure initialization
		soulmate = new int[d];
		for (int i = 0; i < d; ++i) soulmate[i] = -1;

		relation = new int[d][d];
		danced = new int[d][d];
		for (int i = 0; i < d; ++i)
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
				danced[i][j] = 0;
			}
	
		//initialization for snake strategy
		ArrayList<ArrayList<Point>> pits = new ArrayList<ArrayList<Point>>();
		int numSlot = 0, numPit = 0;
		for (double x = eps; x < room_side && numPit < d; x += danceDis + keepDis) {
			pits.add(new ArrayList<Point>());
			for (double y = eps; y < room_side && numPit < d; y += keepDis) {
				pits.get(numSlot).add(new Point(x, y));
				numPit += 2;
			}
			++numSlot;
		}

		positions = new Point[d];
		int cur = 0;
		for (int i = 0; i < numSlot; ++i) {
			if ((i&1) == 1) {
				for (int j = pits.get(i).size() - 1; j >= 0; --j) {
					Point p = pits.get(i).get(j);
					positions[cur++] = new Point(p.x + danceDis, p.y);
				}
			} else {
				for (int j = 0; j < pits.get(i).size(); ++j) {
					positions[cur++] = pits.get(i).get(j);
				}
			}
		}
		for (int i = numSlot - 1; i >= 0; --i) {
			if ((i&1) == 1) {
				for (int j = 0; j < pits.get(i).size(); ++j) {
					positions[cur++] = pits.get(i).get(j);
				}
			} else {
				for (int j = pits.get(i).size() - 1; j >= 0; --j) {
					Point p = pits.get(i).get(j);
					positions[cur++] = new Point(p.x + danceDis, p.y);
				}
			}
		}

	//	printSnakePositions();
	}

	public Point[] generate_starting_locations() {
		return positions;
	}

	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] instructions = new Point[d];
		for (int i = 0; i < d; ++i) {
			instructions[i] = new Point(0., 0.);
		}
		return instructions;
	}

	private void printSnakePositions() {
		for (int i = 0; i < d; ++i) {
			System.out.println(positions[i].x + ' ' + positions[i].y);
		}
	}
}

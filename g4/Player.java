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
	private int boredTime = 120; // 2 minutes

	private int d = -1;
	private double room_side = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1

	private int[][] danced; // cumulatived time in seconds for dance together

	//======= data structures for snake strategy =========
	private Point[] positions;

	private int stayed;


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
		for (int i = 0; i < d; ++i){
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
				danced[i][j] = 0;
			}
		}

		this.stayed =  0;

		//initialization for snake strategy
		ArrayList<ArrayList<Point>> pits = new ArrayList<ArrayList<Point>>();
		int numSlot = 0, numPit = 0;
		for (double x = eps; x < room_side && numPit < d; x += danceDis + keepDis) {
			pits.add(new ArrayList<Point>());
			if ((numSlot&1) == 0) {
				for (double y = eps; y < room_side && numPit < d; y += keepDis) {
					pits.get(numSlot).add(new Point(x, y));
					numPit += 2;
				}
			} else {
				for (double y = room_side - eps; y > 0 && numPit < d; y -= keepDis) {
					pits.get(numSlot).add(new Point(x, y));
					numPit += 2;
				}
			}
			++numSlot;
		}

		positions = new Point[d];
		int cur = 0;
		for (int i = 0; i < numSlot; ++i) {
			if ((i&1) == 1) {
				for (int j = 0; j < pits.get(i).size(); ++j) {
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
				for (int j = pits.get(i).size() - 1; j >= 0; --j) {
					positions[cur++] = pits.get(i).get(j);
				}
			} else {
				for (int j = pits.get(i).size() - 1; j >= 0; --j) {
					Point p = pits.get(i).get(j);
					positions[cur++] = new Point(p.x + danceDis, p.y);
				}
			}
		}	
		//printSnakePositions();
	}

	public Point[] generate_starting_locations() {
		return positions;
	}

	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] stay = new Point[d];
		for (int i = 0; i < d; ++i) {
			stay[i] = new Point(0., 0.);
		}

		//first update partner information and culmulative time danced
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				soulmate[i] = partner_ids[i];
				relation[i][partner_ids[i]] = 1;
			}
			else if(enjoyment_gained[i] == 4){
				relation[i][partner_ids[i]] = 2;
			}
			else if(enjoyment_gained[i] == 3){
				relation[i][partner_ids[i]] = 3;
			}
			danced[i][partner_ids[i]] += 6;
		}

		if(stayed < boredTime){
			stayed += 6;
			return stay;
		}
		stayed = 0;

		Point[] instructions = new Point[d];

		Point[] swaped = new Point[d];
		for (int i = 0; i < d; ++i) {
			swaped[i] = new Point(0., 0.);
		}
		//no enjoyment last round, swap dancer across slot
		for(int j = 0; j < d; j++){
			if(enjoyment_gained[j] == 0 && swaped[j].x == 0 && swaped[j].y == 0){
				Point curr = positions[j];
				for(int k = 0; k < d; k++){
					if(k == j) continue;
					Point swap = positions[k];
					if(swap.y == curr.y && Math.abs(swap.x - curr.x) == danceDis){
						swaped[k] = curr;
						swaped[j] = swap;
						//System.out.print("swaped " + (int)curr.x + "," + curr.y + "with:");
						//System.out.println((int)swap.x + "," + swap.y);
						break;
					}
				}
			}
		}
		
		//move snake by one rotation
		//if swaped, move to swap position instead of next rotation
		Point[] newPositions = new Point[d];
		for (int i = 0; i < d; ++i) {
			Point old_p = positions[i];
			Point new_p = positions[(i+1)%d];
			/*
			if(swaped[i].x != 0 || swaped[i].y != 0){
				new_p = swaped[i];
			}
			*/
			instructions[i] = new Point(new_p.x-old_p.x,new_p.y-old_p.y);
			newPositions[i] = new_p;
		}

		positions = newPositions;
		return instructions;
	}

	private void printSnakePositions() {
		for (int i = 0; i < d; ++i) {
			System.out.println(positions[i].x + "," + positions[i].y);
		}
	}
}

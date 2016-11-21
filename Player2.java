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
	private int stayed;

	private double delta = 1e-3;
	private double danceDis = minDis + delta;
	private double keepDis = danceDis + delta;

	private class Slot {
		int bottom, top; // inclusive, base 0
		int surface; // base 0

		private Slot() {
			bottom = top = -1;
			surface = -1;
		}
	};

	ArrayList<Slot> slot = new ArrayList<Slot>();

	private class Pair {
		int prev, next;

		Point left, right;
		int leftPlayer, rightPlayer;

		private Pair() {
			prev = -1; next = -1;
			leftPlayer = -1; rightPlayer = -1;
			left = null; right = null;
		}
	};

	ArrayList<Pair> pair = new ArrayList<Pair>();

	
	//private MetaData[] data;
	//====================== end =========================

	public void init(int d, int room_side) {
		//System.out.println("init");
		this.d = d;
		this.room_side = (double) room_side;
		
		//data structure initialization
		soulmate = new int[d];
		for (int i = 0; i < d; ++i) soulmate[i] = -1;

		relation = new int[d][d];
		danced = new int[d][d];
		for (int i = 0; i < d; ++i) {
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
				danced[i][j] = 0;
			}
		}

		this.stayed =  0;

		int lenSlot = (room_side - eps) / (keepDis) + 1;
		int numSlot = (d - 1) / lenSlot + 1;
		
		int curPlayer = 0;
		for (int i = 0; i < numSlot; ++i) {
			Slot newSlot;
			if (i < numSlot - 1) {
				newSlot.top = lenSlot * i; newSlot.bottom = lenSlot * i + lenSlot - 1;
			} else {
				newSlot.top = lenSlot * i; newSlot.bottom = lenSlot * i + (d - 1) % lenSlot;
			}
			slot.add(newSlot);

			for (int j = newSlot.top; j <= newSlot.bottom; ++j) {
				Pair newPair;
				double x = eps + keepDis * (j - newSlot.top);
				double y = eps + (keepDis + danceDis) * i;
				newPair.left = new Point(x, y);
				newPair.right = new Point(x, y + danceDis);
				
				if (i&1) {
					newPair.prev = i + 1; newPair.next = i - 1;
					if (j == newSlot.top) newPair.next = newSlot.bottom + 1;
					if (j == newSlot.bottom) newPair.prev = newSlot.top - 1;
				} else {
					newPair.prev = i - 1; newPair.next = i + 1;
					if (j == newSlot.top) newPair.prev = newPair.top - lenSlot;
					if (j == newSlot.bottom) newPair.next = newPair.bottm + lenSlot;
				}
				if (j == 0) {
					newPair.prev = -1;
				}
				if (i == numSlot - 1 && j == newSlot.bottom) {
					numPair.next = -1;
				}
				newPair.leftPlayer = curPlayer++;
				newPair.rightPlayer = curPlayer++;
				pair.add(newPair);
			}
		}
	}

	public Point[] generate_starting_locations() {
		Point[] res = new Point[pair.size() * 2];
		for (int i = 0; i < pair.size(); ++i) {
			res[pair[i].leftPlayer] = pair[i].left;
			res[pair[i].rightPlayer] = pair[i].right;
		}
		return res;
	}

	public Point[] update(int[] soulmates) {
		if (soulmates.size() == 0) return;

		Arrays.sort(soulmates);
		
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
		

		Point[] new_Positions = new Point[d];
		int[] new_pit_ids = new int[d];
		int[] new_player_ids = new int[d];
		//move snake by one rotation
		for (int i = 0; i < d; ++i) {
			new_Positions[i] = new Point(positions[(pit_ids[i]+1)%d].x,positions[(pit_ids[i]+1)%d].y);
			new_pit_ids[i] = (pit_ids[i] + 1)%d;
			new_player_ids[(pit_ids[i]+1)%d] = i;
		}

		boolean[] swaped = new boolean[d];
		//won't have enjoyment next round, swap dancer across slot
		for(int j = 0; j < d; j++){
			if(!swaped[j]){
				for(int k = 0; k < d; k++){
					Point partner = new_Positions[k];
					if(k == j || relation[j][k] != 3 ||  danced[j][k] < boredTime || !samepos(partner,partnerPos[new_pit_ids[j]])) continue;
					swaped[j] = true;
					swaped[k] = true;
					System.out.println("swaped player: " + j + "," + k);
					Point p1 = new_Positions[j];
					Point p2 = partner;
					new_Positions[j] = p2;
					new_Positions[k] = p1;
					int pit_id1 = new_pit_ids[j];
					int pit_id2 = new_pit_ids[k];
					new_pit_ids[j] = pit_id2;
					new_pit_ids[k] = pit_id1;
					new_player_ids[pit_id1] = k;
					new_player_ids[pit_id2] = j;
					break;
				}
			}
		}

		pit_ids = new_pit_ids;
		player_ids = new_player_ids;
		return generateInstructions(dancers,new_Positions);
	}


	private Point[] generateInstructions(Point[] oldPositions,Point[] new_Positions){
		Point[] res = new Point[d];
		for(int i = 0; i < d; i++){
			res[i] = new Point(new_Positions[i].x-oldPositions[i].x,new_Positions[i].y-oldPositions[i].y);
		}
		return res;
	}

	private boolean samepos(Point p1,Point p2){
		return p1.x == p2.x && p1.y == p2.y;
	}

	private void printSnakePositions() {
		boolean print = false;
		for(int i = 0; i < d; i++){
			if(copy[i].x != positions[i].x || copy[i].y != positions[i].y) print = true;
		}
		
		if(print){
			for (int i = 0; i < d; ++i) {
				System.out.format("%.1f,%.1f\n",positions[i].x,positions[i].y);
			}
		}
	}
}

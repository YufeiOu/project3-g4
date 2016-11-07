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
	//pit positions; don't change after initialization
	private Point[] positions;
	private Point[] partnerPos;


	//player ids occupying each pit
	private int[] player_ids;
	//pit ids occupied by each player
	private int[] pit_ids;

	private int stayed;

	private double delta = 1e-3;
	private double danceDis = minDis + delta;
	private double keepDis = danceDis + delta;

	
	//private MetaData[] data;
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
		

		player_ids = new int[d];
		partnerPos = new Point[d];
		pit_ids	 = new int[d];
		for(int i = 0; i < d; i++){
			player_ids[i] = i;
			pit_ids[i] = i;
		} 
		int cur = 0;
		for (int i = 0; i < numSlot; ++i) {
			for (int j = 0; j < pits.get(i).size(); ++j) {
				Point p = pits.get(i).get(j);
				if ((i&1) == 1){
					partnerPos[cur] = p;
					positions[cur++] = new Point(p.x + danceDis, p.y);
				}
				else{
					partnerPos[cur] = new Point(p.x + danceDis, p.y);
					positions[cur++] = p;
				}
			}
		}
		for (int i = numSlot - 1; i >= 0; --i) {
			for (int j = pits.get(i).size() - 1; j >= 0; --j) {
				Point p = pits.get(i).get(j);
				if ((i&1) == 0){
					partnerPos[cur] = p;
					positions[cur++] = new Point(p.x + danceDis, p.y);
				}
				else{
					partnerPos[cur] = new Point(p.x + danceDis, p.y);
					positions[cur++] = p;
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

		//Point[] instructions = new Point[d];
		Point[] new_Positions = new Point[d];
		int[] new_pit_ids = new int[d];
		int[] new_player_ids = new int[d];
		//move snake by one rotation
		for (int i = 0; i < d; ++i) {
			new_Positions[i] = new Point(positions[(pit_ids[i]+1)%d].x,positions[(pit_ids[i]+1)%d].y);
			new_pit_ids[i] = (pit_ids[i] + 1)%d;
			new_player_ids[(pit_ids[i]+1)%d] = i;
		}

		//System.out.println("players in pit:");
		//for(int p:new_pit_ids) System.out.print(p+",");
		//System.out.println();

		printSnakePositions();
		System.out.println("===================");

		//System.out.println("pits occupied by players:");
		//for(int p:new_player_ids) System.out.print(p+",");
		//System.out.println();

		/*
		boolean[] swaped = new boolean[d];
		//won't have enjoyment next round, swap dancer across slot
		for(int j = 0; j < d; j++){
			if(!swaped[j]){
				for(int k = 0; k < d; k++){
					Point partner = new_Positions[k];
					if(k == j || danced[j][k] < boredTime || !samepos(partner,partnerPos[new_pit_ids[j]])) continue;
					swaped[j] = true;
					swaped[k] = true;
					System.out.println("swaped player: " + j + "," + k);
					Point p1 = new_Positions[j];
					Point p2 = partner;
					instructions[j].add(new Point(p2.x-p1.x,p2.y-p1.y));
					instructions[k].add(new Point(p1.x-p2.x,p1.y-p2.y));
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
		*/
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
		for (int i = 0; i < d; ++i) {
			System.out.format("%.1f,%.1f\n",positions[i].x,positions[i].y);
		}
	}
}

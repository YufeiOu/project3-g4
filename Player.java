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
	private int room_side = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1

	private int[][] danced; // cumulatived time in seconds for dance together

	private int stayed;

	public class Dancer{
		int id = -1;
		int soulmate = -1;
		int des_pit = -1;
		Point next_pos = null;
	}

	//dancers never stay at pit, legal positions are up/down/left/right eps/3;
	public class Pit{
		Point pos = null;
		Point prev = null;
		Point next = null;
	}

	private Dancer[] dancers;
	private boolean connected;
	private Point[] starting_positions;
	private Pit[] pits;
	//====================== end =========================

	public void init(int d, int room_side) {
		//System.out.println("init");
		this.d = d;
		this.room_side = room_side;
		
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

		this.connected = true;
		this.starting_positions = new Point[d];
		this.pits = new Pit[d];
		boolean odd = true;
		Point prev = null;
		Point curr = null;
		double x = eps;
		double y = eps;
		double increment = 0.5 + eps;
		while(i<d){
			int sign = (((i&1) == 1) ? 1: -1);
			if(y + sign * increment <= this.room_side - eps && y + sign * increment >= eps){
				y += sign * increment;
			}
			else{
				x += increment;
			}
			curr = new Point(x,y);
			this.starting_positions[i] = curr;
			this.pits[i] = new Pit();
			this.pits[i].prev = prev;
			this.pits[i].curr = curr;
			prev = curr;
			Dancer dancer = new Dancer();
			dancer.id = i;
			dancer.des_pit = i;
			this.dancers[i] = dancer;
		}
	}
}

public Point[] generate_starting_locations() {
	return this.starting_positions;
}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// first update partner information and culmulative time danced
		boolean new_soulmate_found = updatePartnerInfo(partner_ids, enjoyment_gained);
		if (connected && !new_soulmate_found) {
			swap();
		}
		else{
			// generate a sequence of pit indexes of de-coupled dancers
			genShape();
			//try to connect the dancers
			connect();
		} 
		move_couple();
		//generate instructions using target positions and current positions
		return generateInstructions(current_positions);
	}

	//modify the desination positions of active dancers;
	void swap(){

	}





	boolean updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		boolean found = false;
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				soulmate[i] = partner_ids[i];
				if(relation[i][partner_ids[i]] != 1) found = true;
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
		return found;
	}


	private Point[] generateInstructions(Point[] currentPositions){
		Point[] res = new Point[d];
		for(Dancer dancer:dancers){
			res[i] = new Point(dancer.next_pos.x-currentPositions[i].x,dancer.next_pos.y-currentPositions[i].y);
		}
		return res;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < eps && Math.abs(p1.y - p2.y) < eps;
	}
}

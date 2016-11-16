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

	public class Dancer{
		int id = -1;
		int soulmate = -1;
		int honeymoon_pit = -1;
		Point next_pos = null;

		//only used by singles	
		int pit_id = -1;
	}

	//dancers never stay at pit, legal positions are up/down/left/right eps/3;
	public class Pit{
		Point pos = null;
		Pit prev = null;
		Pit next = null;
		int player_id = -1;
		int pit_id = -1;
	}

	private Dancer[] dancers;
	private boolean connected;
	private Point[] starting_positions;
	private Pit[] pits;
	private int[] target_single_shape; // a list of Pit indexes
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
		Pit prev_pit = null;
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
			Point curr_pos = new Point(x,y);
			Pit curr_pit = new Pit();
			this.starting_positions[i] = curr;
			this.pits[i] = curr_pit;
			if(prev != null) prev.next = curr_pit;
			curr_pit.prev = prev_pit;
			curr_pit.pos = curr_pos;
			curr_pit.player_id = i;
			curr_pit.pit_id = i;
			prev_pit = curr_pit;
			Dancer dancer = new Dancer();
			dancer.id = i;
			dacner.pid_id = i;
			this.dancers[i] = dancer;
		}
	}

	public Point[] generate_starting_locations() {
		return this.starting_positions;
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// first update partner information and culmulative time danced
		boolean new_soulmate_found = updatePartnerInfo(partner_ids, enjoyment_gained);
		if (this.connected && !new_soulmate_found) {
			swap();
		}
		else{
			if (new_soulmate_found) {
				// assert
				this.connected = false;
				// remove couples from this.currShape
				//int[] holeShape = removeCouples();
				// generate a sequence of pit indexes of de-coupled dancers
				int[] newShape = genShape();
				// generate new_soulmate_destination
				//int[] soulmateShape = findSoulmateDestination();
				// update 
				this.target_single_shape = newShape;
			}
			//try to connect the dancers
			this.connected = connect();
		} 
		move_couple();
		//generate instructions using target positions and current positions
		return generateInstructions(this.dancers);
	}

	boolean updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		boolean found = false;
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				soulmate[i] = partner_ids[i];
				if(relation[i][partner_ids[i]] != 1) found = true;
				relation[i][partner_ids[i]] = 1;
				dancers[i].soulmate = partner_ids[i];
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

	//modify the desination positions of active dancers;
	void swap(){

	}

	// according to the this.dancers, calculate the destination indexes set of de-coupled dancers
	int[] genShape(){

	}

	// update single dancer's next position using target_single_shape, return true target_single_shape is connected;
	boolean connect() {
		int single_index = 0;
		boolean connected = true;
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate != -1) continue;
			int target_pit_id = this.target_single_shape[single_index++];
			Pit curr_pit = pits[dancers[i].pit_id];
			Pit pointer = curr_pit;
			boolean stop = false;
			while(!stop){
				if(pointer.pit_id < target_pit_id){
					pointer = pointer.next;
				}
				else if(pointer.pit_id > target_pit_id){
					pointer = pointer.prev;
				}
				stop = distance(pointer.pos,curr_pit.pos) > 2 || pointer.pit_id == target_pit_id;
			}
			if(!samepos(pointer.pos,curr_pit.pos)) connected = false;
			dancers[i].pit_id = pointer.pit_id;
			dancers[i].next_pos = pointer.pos;
		}
		return connected;
	}

	double distance(Point p1,Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	// according to the information of the dancers, 
	void move_couple() {

	}

	// generate instruction according to this.dancers
	private Point[] generateInstructions(Point[] old_positions){
		Point[] movement = new Point[this.d];
		for(int i = 0; i < d; i++){
			movement[i] = new Point(dancers[i].next_pos.x-old_positions[i].x,dancers[i].next_pos.y-old_positions[i].y);
		}
		return movement;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < eps && Math.abs(p1.y - p2.y) < eps;
	}
}

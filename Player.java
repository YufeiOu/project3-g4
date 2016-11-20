package sqdance.g4;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {

	private static double eps = 1e-3;

	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 6; // 6 seconds

	private int d = -1;
	private int room_side = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1
	private int[][] danced; // cumulatived time in seconds for dance together
	private int couples_found = 0;
	private int stay = 6;

	public class Dancer{
		int id = -1;
		int soulmate = -1;
		Point next_pos = null;
		Point des_pos = null;
		int pit_id = -1;

		public Dancer(int id,int pit_id){
			this.id = id;
			this.pit_id = pit_id;
		}
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
	private int[] target_couple_shape;
	private int state; // 1 represents 1-2 3-4 5-6, 2 represents 1 2-3 4-5 6
	//====================== end =========================

	public void init(int d, int room_side) {
		//System.out.println("init");
		this.d = d;
		this.room_side = room_side;
		this.state = 2;
		
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
		this.target_single_shape = new int[d];
		this.pits = new Pit[1561];
		this.dancers = new Dancer[d];
		Pit prev_pit = null;
		double x = eps;
		double y = eps;
		double increment = 0.5 + eps;
		int i = 0;
		int old_i = -1;
		int sign = 1;

		double x_min = eps;
		double x_max = this.room_side;
		double y_min = eps;
		double y_max = this.room_side;
		//create the pits in a spiral fashion
		while(old_i != i){
			//go right
			old_i = i;
			while(x + safeDis < x_max){
				prev_pit = addPit(prev_pit,new Point(x,y),i++);
				x += increment;
			}
			x = prev_pit.pos.x;
			y += increment;
			x_max = x;

			//go down
			while(y + safeDis < y_max){
				prev_pit = addPit(prev_pit,new Point(x,y),i++);
				y += increment;
			}
			y = prev_pit.pos.y; 
			x -= increment;
			y_max = y;

			//go left
			while(x - safeDis > x_min){
				prev_pit = addPit(prev_pit,new Point(x,y),i++);
				x -= increment;
			}
			x = prev_pit.pos.x; 
			y -= increment;
			x_min = x;

			//go up
			while(y - safeDis > y_min){
				prev_pit = addPit(prev_pit,new Point(x,y),i++);
				y -= increment;

			}
			y = prev_pit.pos.y;
			x += increment;
			y_min = y;
		}

		/*
		Point[] pit_poses = new Point[d];
		for(int j= 0; j < d; j++){
			pit_poses[j] = pits[j].pos;
		}

		printPosition(pit_poses);
		*/


		//put players in pits
		for(int j = 0; j < d; j++){
			Dancer dancer = new Dancer(j,j);
			Point my_pos = this.pits[j].pos;
			Point partner_pos = j%2 == 0? this.pits[j].next.pos : this.pits[j].prev.pos;
			//System.out.println("dancer: " + j);
			//System.out.println("cur pos: " + my_pos.x + "," + my_pos.y);
			//System.out.println("partner pos: " + partner_pos.x + "," + partner_pos.y);
			dancer.next_pos = findNearestActualPoint(my_pos,partner_pos);
			//System.out.println("next pos: " + dancer.next_pos.x + "," + dancer.next_pos.y);
			this.dancers[j] = dancer;
			this.pits[j].player_id = j;
			this.starting_positions[j] = dancer.next_pos;
			this.target_single_shape[j] = j;
		}
	}

	//add pit number i;
	//return reference to current pit
	private Pit addPit(Pit prev_pit,Point curr_pos,int i){
		Pit curr_pit = new Pit();
		this.pits[i] = curr_pit;
		if(prev_pit != null) prev_pit.next = curr_pit;
		curr_pit.prev = prev_pit;
		curr_pit.pos = curr_pos;
		curr_pit.pit_id = i;
		return curr_pit;
	}


	public Point[] generate_starting_locations() {
		printPosition(this.starting_positions);
		return this.starting_positions;
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// first update partner information and culmulative time danced
		if (this.connected && updatePartnerInfo(partner_ids, enjoyment_gained)) {
			if(this.stay > this.boredTime){
				//swap(partner_ids,old_positions);
				this.stay = 0;
			}
			else{
				this.stay += 6;
			}
			//System.out.println("swaped");
		}
		else{
			//if the snake is not connected, try to connect the dancers
			this.connected = connect();
		} 
		move_couple();
		//generate instructions using target positions and current positions
		return generateInstructions(old_positions);
	}


	//update dancer relations based on enjoyment gained;
	//also arrange couple's destination honeymoon pit number, set them close to each other 
	boolean updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		boolean new_couple_found = false;
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				soulmate[i] = partner_ids[i];
				if(relation[i][partner_ids[i]] != 1 && relation[partner_ids[i]][i] != 1) {
					new_couple_found = true;
					//arrange destination for newly found couples
					Point des1 = this.pits[this.pits.length-1-this.couples_found].pos;
					Point des2 = this.pits[this.pits.length-2-this.couples_found].pos;
					this.couples_found += 2;
					dancers[i].des_pos = findNearestActualPoint(des1,des2);
					dancers[partner_ids[i]].des_pos = findNearestActualPoint(des2,des1);
				}
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
		if(new_couple_found) System.out.println("new couples_found");
		return new_couple_found;
	}

	// a = (x,y) we want to find least distance between (x+eps/3, y) (x-eps/3, y) (x, y+eps/3) (x, y-eps/3) and b
	Point findNearestActualPoint(Point a, Point b) {
		Point left = new Point(a.x-eps/3,a.y);
		Point right = new Point(a.x+eps/3,a.y);
		Point down = new Point(a.x,a.y-eps/3);
		Point up = new Point(a.x,a.y+eps/3);
		Point a_neighbor = left;
		if (distance(right,b) < distance(a_neighbor,b)) a_neighbor = right;
		if (distance(down,b) < distance(a_neighbor,b)) a_neighbor = down;
		if (distance(up,b) < distance(a_neighbor,b)) a_neighbor = up;

		return a_neighbor;
	}

	//modify the desination positions of active dancers;
	void swap(int[] partner_ids, Point[] old_positions) {
		System.out.println("swaped");
		boolean[] swaped = new boolean[d];
		//first, swap pit numbers with partners danced last round
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate != -1 || swaped[i]) continue;
			int curr = dancers[i].pit_id;
			int swap = dancers[partner_ids[i]].pit_id;
			dancers[i].pit_id = swap;
			dancers[partner_ids[i]].pit_id = curr;
			swaped[i] = true;
			swaped[partner_ids[i]] = true;
		}

		//then, move pairs closer according to odd/even rounds
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate != -1) continue;
			Pit my_pit = this.pits[dancers[i].pit_id];
			if(this.state == 1){
				Pit partner_pit = my_pit.pit_id%2 == 0 ? this.pits[my_pit.pit_id].next : this.pits[my_pit.pit_id].prev;
				if(partner_pit != null) dancers[i].next_pos = findNearestActualPoint(my_pit.pos,partner_pit.pos);
			}
			else{
				Pit partner_pit = my_pit.pit_id%2 == 0 ? this.pits[my_pit.pit_id].prev : this.pits[my_pit.pit_id].next;
				if(partner_pit != null) dancers[i].next_pos = findNearestActualPoint(my_pit.pos,partner_pit.pos);
			}
		}
		
		for (int i = 0; i < this.target_single_shape.length; i++) {
			System.out.println(dancers[pits[this.target_single_shape[i]].player_id].next_pos.x + " " + dancers[pits[this.target_single_shape[i]].player_id].next_pos.y);
		}
		System.out.println("--------------------------------");
		
		this.state = 3 - this.state;
	}

	// update single dancer's next position, shrink everyone to the head of the snake;
	boolean connect() {
		if(this.d == this.couples_found) return true;
		int single_index = 0;
		int couple_index = 0;
		boolean connected = true;
		int[] target_single_shape = new int[d - couples_found];

		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate != -1){
				continue;
			}
			target_single_shape[single_index] = single_index;
			int target_pit_id = single_index;
			single_index++;
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
			this.pits[pointer.pit_id].player_id = i;
		}
		return connected;
	}

	//calculate Euclidean distance between two points
	double distance(Point p1,Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	// according to the information of the dancers, 
	void move_couple() {
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate == -1) continue;
			Point curr = this.dancers[i].next_pos;
			Point des = this.dancers[i].des_pos;
			this.dancers[i].next_pos = findNextPosition(curr, des);
		}
	}

	Point findNextPosition(Point curr, Point des) {
		if (distance(curr,des) < 2) return des;
		else {
			double x = des.x - curr.x;
			double y = des.y - curr.y;
			Point next = new Point(curr.x + (2-eps)*x/Math.sqrt(x*x+y*y), curr.y + (2-eps)*y/Math.sqrt(x*x+y*y));
			return next;
		}
	}

	// generate instruction according to this.dancers
	private Point[] generateInstructions(Point[] old_positions){
		Point[] movement = new Point[d];
		for(int i = 0; i < d; i++){
			//System.out.println("i: " + i);
			movement[i] = new Point(dancers[i].next_pos.x-old_positions[i].x,dancers[i].next_pos.y-old_positions[i].y);
			//movement[i] = new Point(0,0);

			//System.out.println(movement[i].x+","+movement[i].y);
		}
		return movement;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < eps && Math.abs(p1.y - p2.y) < eps;
	}

	void printPosition(Point[] points){
		for(int i = 0; i < d; i++){
			if(true || i == 39) System.out.println(points[i].x+","+points[i].y);
		}
		/*
		for(Point p:points){
			System.out.println(p.x+","+p.y);
		}
		*/
	}
}

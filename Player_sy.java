package sqdance.g4;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {
	
	private static int INF = (int)1e9;
	private static double eps = 1e-7;
	private static double delta = 1e-3;

	private double danceDis = 0.5 + delta;
	private double keepDis = danceDis + delta;
	private double minDis = 0.1 + delta;

	private int numDancer;
	private int roomSide;

	private int timeStamp = 0;
	private int boredTime = 90;

	private int[] score;

	private ArrayList<ArrayList<Point>> rope;
	private ArrayList<ArrayList<Integer>> sequence;

	public void init(int d, int room_side) {
		numDancer = d;
		roomSide = room_side;

		ropeInit();
		
		int cnt = 0;
		sequence = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; cnt < numDancer && i < rope.size(); ++i) {
			ArrayList<Integer> seq = new ArrayList<Integer>();
			for (int j = 0; cnt < numDancer && j < rope.get(i).size(); ++j) {
				seq.add(j);
				++cnt;
			}
			sequence.add(seq);
		}

		score = new int[d];
		for (int i = 0; i < d; ++i) score[i] = 0;
	}

	public Point[] generate_starting_locations() {
		Point[] res = new Point[numDancer];
		int start = 0;
		for (int i = 0; i < sequence.size(); ++i) {
			for (int j = 0; j < sequence.get(i).size(); ++j) {
				Point tmp = rope.get(i).get(j);
				if (start + sequence.get(i).get(j) >= numDancer) return res;
				res[start + sequence.get(i).get(j)] = rope.get(i).get(j);
			}
			start += sequence.get(i).size();
		}
		return res;
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		timeStamp += 6;
		Point[] res = new Point[numDancer];
	
		if (swap(enjoyment_gained)) {
			for (int i = 0; i < rope.size(); ++i) {
				sequenceSwap2(i);
			}
			int start = 0;
			for (int i = 0; i < sequence.size(); ++i) {
				for (int j = 0; j < sequence.get(i).size(); ++j) {
					int id = start + sequence.get(i).get(j);
					double dy = rope.get(i).get(j).y - old_positions[id].y;
					if (dy > 2 - eps) System.out.println(id + " " + old_positions[id].x + " " + old_positions[id].y + " " + rope.get(i).get(j).x + " " + rope.get(i).get(j).y);
					res[id] = new Point(rope.get(i).get(j).x - old_positions[id].x, rope.get(i).get(j).y - old_positions[id].y);
				}
				start += sequence.get(i).size();
			}
		} else {
			for (int i = 0; i < numDancer; ++i)
				res[i] = new Point(0, 0);
		}
		return res;
	}

	boolean swap(int[] enjoyment_gained) {
		if (timeStamp % boredTime == 0) return true;
		return false;

		/*
		boolean flag = false;
		for (int i = 0; i < numDancer; ++i) {
			if (enjoyment_gained[i] > 0) flag = true;
		}
		if (!flag) return false;

		int preMinS = calMinScore();
		for (int i = 0; i < numDancer; ++i) {
			score[i] += enjoyment_gained[i];
		}
		int curMinS = calMinScore();
		if (curMinS == preMinS) return true;
		return false;
		*/
	}

	int calMinScore() {
		int res = INF;
		for (int i = 0; i < numDancer; ++i)
			res = Math.min(res, score[i]);
		return res;
	}

	void sequenceSwap2(int k) {
		for (int i = 0; i + 1 < sequence.get(k).size(); i += 2) {
			int tmp = sequence.get(k).get(i);
			sequence.get(k).set(i, sequence.get(k).get(i + 1));
			sequence.get(k).set(i + 1, tmp);
		}

		for (int i = 1; i + 1 < sequence.get(k).size(); i += 2) {
			int tmp = sequence.get(k).get(i);
			sequence.get(k).set(i, sequence.get(k).get(i + 1));
			sequence.get(k).set(i + 1, tmp);
		}
	}

	void ropeInit() {
		rope = new ArrayList<ArrayList<Point>>();

		int rest = numDancer;
		int numBlock = (int)((roomSide) / (danceDis + keepDis));
		// 5 regular 3-layer ropes
		int numRegRope = (numDancer <= 7000)?7:6;
		int layer = 2;

		int numAudi = (numDancer - 1) / numRegRope / numBlock + 1 - 2 * layer * 2;
		double lenAudi = ((numAudi - 1) / 9) * minDis;

		for (int k = 0; k < numRegRope; ++k) {
			ArrayList<Point> cur = new ArrayList<Point>();

			double yleft = delta + (keepDis * layer * 2 + lenAudi + keepDis) * k;

			int b = 0;
			for (double xleft = delta; b < numBlock && xleft + danceDis + keepDis < roomSide - eps; xleft += danceDis + keepDis, ++b) {
				double x = xleft, y = yleft + keepDis * layer;
				int putAudi = 0;

				int len1 = (numAudi - 1) / layer / 2 + 1;
				int len2 = numAudi - len1 * (layer * 2 - 1);
				if (len2 <= 1) {
					--len1;
					len2 += (layer * 2 - 1);
				}

				for (int j = 0; j < layer; ++j) {
					cur.add(new Point(xleft, yleft + keepDis * j));
					cur.add(new Point(xleft + danceDis, yleft + keepDis * j));

					for (int i = 0; i < ((j == 0)?len2:len1); ++i) {
						++putAudi;
						cur.add(new Point(x, y));
						if (putAudi % 9 == 0) {
							y += minDis; x = xleft;
						} else x += minDis;
					}
				}

				double ymid = yleft + keepDis * layer + lenAudi + keepDis;
				for (int j = 0; j < layer; ++j) {
					cur.add(new Point(xleft, ymid + keepDis * j));
					cur.add(new Point(xleft + danceDis, ymid + keepDis * j));

					int tmp = putAudi;
					for (int i = 0; i < len1; ++i) {
						++putAudi;
						cur.add(new Point(x, y));
						if (putAudi % 9 == 0) {
							y += minDis; x = xleft;
						} else x += minDis;
					}
				}
			}

			rope.add(cur);
			rest -= cur.size();
		}

		if (rest > 0) {
			ArrayList<Point> cur = new ArrayList<Point>();
			double yleft = delta + (keepDis * layer * 2 + lenAudi + keepDis) * numRegRope;
			for (double x = delta; rest > 0 && x + danceDis < roomSide - eps; x += danceDis + keepDis) {
				for (double y = yleft; rest > 0 && y < roomSide - eps; y += keepDis) {
					cur.add(new Point(x, y));
					--rest;
					if (rest == 0) break;
					cur.add(new Point(x + danceDis, y));
					--rest;
				}
			}
			rope.add(cur);
		}

		// special rope: 2-layer
		/*
		int layer1 = 2;
		ArrayList<Point> cur1 = new ArrayList<Point>();
		numAudi = (rest - 1) / numBlock + 1 - 2 * layer1;

		int b = 0;
		for (double xleft = delta; b < numBlock && xleft + danceDis + keepDis < roomSide - eps; xleft += danceDis + keepDis, ++b) {
			double x = xleft;
			for (int j = 0; j < layer1; ++j) {
				for (int i = 0; i < numAudi / layer1; ++i) {
					cur1.add(new Point(x, delta));
					x += minDis;
				}
				cur1.add(new Point(xleft, delta + keepDis * (j + 1)));
				cur1.add(new Point(xleft + danceDis, delta + keepDis * (j + 1)));
			}
			++numBlock;
		}
		rope.add(cur1);
		*/
	}
}

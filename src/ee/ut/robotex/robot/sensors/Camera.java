package ee.ut.robotex.robot.sensors;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import ee.ut.robotex.renderer.Paintable;
import ee.ut.robotex.renderer.Polygon2D;
import ee.ut.robotex.simulation.Ball;
import ee.ut.robotex.simulation.GameInfo;
import ee.ut.robotex.simulation.Goal;
import ee.ut.robotex.simulation.Simulation;
import ee.ut.robotex.simulation.StepListener;

public class Camera implements StepListener, Paintable {
	protected Body body;
	protected GameInfo game;
	protected float x;
	protected float y;
	protected float angle;
	protected float angleOfView;
	protected float distance;
	protected Polygon2D view;
	protected List<BallInfo> visibleBalls;
	protected List<GoalInfo> visibleGoals;
	protected int lastVirtualId = 0;
	
	public class BallInfo {
		private int realId;
		public int id;
		public float distance;
		public float angle;
		
		public BallInfo(int realId, int virtualId, float distance, float angle) {
			this.realId = realId;
			this.id = virtualId;
			this.distance = distance;
			this.angle = angle;
		}
		
		// AI should not use this as the camera can't uniquely determine
		// which ball is which, for debugging only
		public int _getRealId() {
			return realId;
		}
	}
	
	public class GoalInfo {
		public Simulation.Side side;
		public float distance;
		public float angle;
		
		public GoalInfo(Simulation.Side side, float distance, float angle) {
			this.side = side;
			this.distance = distance;
			this.angle = angle;
		}
	}
	
	public Camera(Body body, GameInfo game, float x, float y, float angle, float angleOfView, float distance) {
		this.body = body;
		this.game = game;
		this.x = x;
		this.y = y;
		
		// these lists are updated and traversed in different threads..
		this.visibleBalls = new CopyOnWriteArrayList<BallInfo>();
		this.visibleGoals = new CopyOnWriteArrayList <GoalInfo>();
		
		update(angle, angleOfView, distance);
	}
	
	public void update(float angle, float angleOfView, float distance) {
		this.angle = angle;
		this.angleOfView = angleOfView;
		this.distance = distance;
		
		view = new Polygon2D();
		
		view.addPoint(0,  0);
		
		view.addPoint(
			distance * (float)Math.cos((-angleOfView / 2.0f + angle - 90.0f) * Math.PI / 180.0f),
			distance * (float)Math.sin((-angleOfView / 2.0f + angle - 90.0f) * Math.PI / 180.0f)
		);
		
		view.addPoint(
			distance * (float)Math.cos((angleOfView / 2.0f + angle - 90.0f) * Math.PI / 180.0f),
			distance * (float)Math.sin((angleOfView / 2.0f + angle - 90.0f) * Math.PI / 180.0f)
		);
	}
	
	public List<BallInfo> getVisibleBalls() {
		return visibleBalls;
	}
	
	public List<GoalInfo> getVisibleGoals() {
		return visibleGoals;
	}

	@Override
	public void paint(Graphics2D g) {
		Graphics2D g2 = (Graphics2D)g.create();
		g2.translate(x, y);
				
		g2.setColor(new Color(255, 255, 255, 20));
		g2.fill(view);
		
		// draw camera position arc
		float arcRadius = 0.1f;
		g.setColor(new Color(255, 255, 255, 100));
		g.fill(new Arc2D.Float(-arcRadius + x, -arcRadius + y, arcRadius * 2.0f, arcRadius * 2.0f, -angleOfView / 2.0f - angle + 90.0f, angleOfView, Arc2D.PIE));
		
		// to display visible balls, we have to get back to global coordinates
		Graphics2D g3 = (Graphics2D)g.create();
		g3.rotate(-body.getAngle());
		g3.translate(-body.getPosition().x, -body.getPosition().y);
		
		for (BallInfo ballInfo : visibleBalls) {
			Ball ball = game.getBallById(ballInfo.realId);
			
			float radius = 0.075f;
			
			g3.setColor(new Color(255, 255, 255, 100));
			g3.fill(new Ellipse2D.Float(ball.getX() - radius, ball.getY() - radius, radius * 2.0f, radius * 2.0f));
			
			g3.setFont((new Font("Consolas", Font.PLAIN, 1)).deriveFont(0.1f));
			g3.setColor(new Color(255, 255, 255));
			g3.drawString(
				"#" + Integer.toString(ballInfo.id) + " - " + Long.toString(Math.round(ballInfo.distance * 100.0f)) + "cm / " + Long.toString(Math.round(ballInfo.angle / Math.PI * 180.0f)) + "�",
				ball.getX(),
				ball.getY() - 0.1f
			);
		}
		for (GoalInfo goalInfo : visibleGoals) {
			Goal goal = null;
			
			if (goalInfo.side == Simulation.Side.YELLOW) {
				goal = game.getYellowGoal();
			} else {
				goal = game.getBlueGoal();
			}
			
			/*
			float width = 0.33f;
			float height = 0.78f;
			
			g3.setColor(new Color(255, 255, 255, 100));
			g3.fill(new Rectangle2D.Float(goal.getBody().getPosition().x - width / 2.0f, goal.getBody().getPosition().y - height / 2.0f, width, height));
			*/
			
			g3.setFont((new Font("Consolas", Font.PLAIN, 1)).deriveFont(0.1f));
			g3.setColor(new Color(255, 255, 255));
			g3.drawString(
				Long.toString(Math.round(goalInfo.distance * 100.0f)) + "cm / " + Long.toString(Math.round(goalInfo.angle / Math.PI * 180.0f)) + "�",
				goal.getBody().getPosition().x,
				goal.getBody().getPosition().y - 0.2f
			);
		}
	}

	@Override
	public void stepBeforePhysics(float dt) {
		Map<Integer, Integer> lastIds = new HashMap<Integer, Integer>();
		
		for (BallInfo ball : visibleBalls) {
			lastIds.put(ball.realId, ball.id);
		}
		
		visibleBalls.clear();
		visibleGoals.clear();
		
		Polygon2D globalView = new Polygon2D();
		
		for (int i = 0; i < view.npoints; i++) {
			float localX = view.xpoints[i];
			float localY = view.ypoints[i];
			
			Vec2 worldPos = body.getWorldPoint(new Vec2(localX + x, localY + y));
			
			globalView.addPoint(worldPos.x, worldPos.y);
		}
		
		for (Ball ball : game.getBalls()) {
			// ignore inactive balls
			if (!ball.isActive()) {
				continue;
			}
			
			if (globalView.contains(ball.getX(), ball.getY())) {
				Vec2 ballPos = new Vec2(ball.getX(), ball.getY());
				Vec2 cameraPos = body.getWorldPoint(new Vec2(x, y));
				Vec2 forwardVec = new Vec2((float)Math.cos(body.getAngle()), (float)Math.sin(body.getAngle()));
				Vec2 ballHeading = ballPos.sub(cameraPos);
				
				forwardVec.normalize();
				ballHeading.normalize();
				
				float distance = MathUtils.distance(ballPos, cameraPos);
				float angle = Vec2.dot(ballHeading, forwardVec);
				
				int virtualId;
				
				if (lastIds.containsKey(ball.getId())) {
					virtualId = lastIds.get(ball.getId());
				} else {
					virtualId = lastVirtualId++;
				}
				
				visibleBalls.add(new BallInfo(ball.getId(), virtualId, distance, angle));
			}
		}
		
		Goal yellowGoal = game.getYellowGoal();
		Goal blueGoal = game.getBlueGoal();

		List<Goal> goals = new ArrayList<Goal>();
		goals.add(yellowGoal);
		goals.add(blueGoal);
		
		for (Goal goal : goals) {
			if (globalView.contains(goal.getBody().getPosition().x, goal.getBody().getPosition().y)) {
				Vec2 ballPos = new Vec2(goal.getBody().getPosition().x, goal.getBody().getPosition().y);
				Vec2 cameraPos = body.getWorldPoint(new Vec2(x, y));
				Vec2 forwardVec = new Vec2((float)Math.cos(body.getAngle()), (float)Math.sin(body.getAngle()));
				Vec2 ballHeading = ballPos.sub(cameraPos);
				
				forwardVec.normalize();
				ballHeading.normalize();
				
				float distance = MathUtils.distance(ballPos, cameraPos);
				float angle = Vec2.dot(ballHeading, forwardVec);
				
				visibleGoals.add(new GoalInfo(goal.getSide(), distance, angle));
			}
		}
	}

	@Override
	public void stepAfterPhysics(float dt) {

	}

}

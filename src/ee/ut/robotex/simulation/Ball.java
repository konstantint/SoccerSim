package ee.ut.robotex.simulation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import ee.ut.robotex.renderer.Paintable;

public class Ball implements Paintable, StepListener {

	private int id;
	private float radius = 0.04267f;
	private float density = 1.12909f; //<http://www.madsci.org/posts/archives/2004-08/1091747977.Ph.r.html
	private float restitution = 0.5f;
	private float friction = 0.3f;
	private float angularDamping = 0.5f;
	private float linearDamping = 0.5f;
	private static int instances = 0;
	private Body body;
	
	public Ball(World world, float x, float y) {
		id = instances++;
		
		CircleShape shape = new CircleShape();
        shape.m_radius = radius;
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(x, y);
		
		body = world.createBody(bodyDef);
		body.setUserData(this);
		body.setAngularDamping(angularDamping);
		body.setLinearDamping(linearDamping);
		
		Fixture fixture = body.createFixture(shape, density);
		fixture.setRestitution(restitution);
		fixture.setFriction(friction);
		fixture.setUserData(this);
	}
	
	public int getId() {
		return id;
	}
	
	public Body getBody() {
		return body;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public float getX() {
		return body.getPosition().x;
	}
	
	public float getY() {
		return body.getPosition().y;
	}
	
	public float distanceTo(float x, float y) {
		return (float)Math.sqrt(Math.pow(this.getX() - x, 2) + Math.pow(this.getY() - y, 2));
	}
	
	public void paint(Graphics2D g) {
		g.setColor(new Color(225, 128, 0));
		g.fill(new Ellipse2D.Float(-radius, -radius, radius * 2.0f, radius * 2.0f));
	}
	
	@Override
	public void stepBeforePhysics(float dt) {
		
	}

	@Override
	public void stepAfterPhysics(float dt) {
		
	}
}
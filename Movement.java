package cda;

public class Movement {
	private GLaDOS owner;
	
	public Movement(GLaDOS g){
		owner = g;
	}
	
	public void update () {
		owner.ahead(1);
	}
	
	public void init(){}
	
}
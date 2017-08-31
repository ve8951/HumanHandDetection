package application;

import java.util.List;

import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class MODELConvexityDefects {

	Point startIndex, endIndex, farthestPoint;

	Integer depth;

	public MODELConvexityDefects(List<MatOfPoint> contours, MatOfInt4 convexityDefect, int boundPos, int pos){
		super();
		this.startIndex = contours.get(boundPos).toList().get(convexityDefect.toList().get(pos));
		this.endIndex = contours.get(boundPos).toList().get(convexityDefect.toList().get(pos+1));
		this.farthestPoint = contours.get(boundPos).toList().get(convexityDefect.toList().get(pos+2));
	}
	
	public MODELConvexityDefects(){
		super();
	}
	
	public Integer getDepth() {
		return depth;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public Point getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Point startIndex) {
		this.startIndex = startIndex;
	}

	public Point getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(Point endIndex) {
		this.endIndex = endIndex;
	}

	public Point getFarthestPoint() {
		return farthestPoint;
	}

	public void setFarthestPoint(Point farthestPoint) {
		this.farthestPoint = farthestPoint;
	}

}

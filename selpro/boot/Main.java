package selpro.boot;

import javax.swing.UIManager;

import selpro.CameraReader;
import selpro.frames.ControlFrame;
import selpro.frames.ProjectionFrame;

public class Main {

	public static void main(String args[]){
		//Set the window theming
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//open up a connection to an available webcam
		CameraReader camera = new CameraReader();	
		
		//Create the three frames
		ProjectionFrame projectionFrame = new ProjectionFrame("Projector Output");
		ControlFrame controlFrame = new ControlFrame("Control Panel", projectionFrame);

		//Set where the camera callback send it's frames
		camera.addFrameListener(controlFrame);
		
		//Show the frames and move them to their proper locations relative to each other.
		projectionFrame.setVisible(true);
		controlFrame.setVisible(true);
		//projectionFrame.setLocation(controlFrame.getWidth()+controlFrame.getX(), controlFrame.getY());
		//projectionFrame.setVisible(true);
	}
}

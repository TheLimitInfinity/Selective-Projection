package selpro.frames;

import static org.bytedeco.javacpp.opencv_core.cvScalar;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_photo;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import selpro.CameraReader;

public class ControlFrame extends CanvasFrame implements CameraReader.FrameListener{
	private static final long serialVersionUID = 0L;
	
	//Main variables
	private ProjectionFrame projectionFrame;
	private OpenCVFrameConverter.ToIplImage ocv_converter = new OpenCVFrameConverter.ToIplImage();
	private Java2DFrameConverter img_converter = new Java2DFrameConverter();
	private Mat texture = null;
	private static CvScalar threshMin = cvScalar(0, 0, 0, 0);//BGR-A
	private static CvScalar threshMax = cvScalar(255, 255, 255, 0);//BGR-A
	private DrawState drawState = DrawState.CHECKER;
	private Rect projectionBounds = null;
	private IplImage projectionMask = null;

	//Control booleans
	private boolean findProjection = false;
	private boolean saveMask = false;
	private boolean whiteBackground = false;
	private boolean saveInvertedMask = false;
	private boolean AA = false;
	

	public ControlFrame(String title, ProjectionFrame projectionFrame) {
		super(title);
		this.projectionFrame = projectionFrame;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 700);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(super.canvas);
		addButtons(mainPanel);
		this.setContentPane(mainPanel);
	}

	private void addButtons(JPanel panel) {
		JPanel[] panels = new JPanel[4];
		for (int i = 0; i < panels.length; i++) {
			panels[i] = new JPanel();
			panel.add(panels[i]);
		}
		
		JButton findProjection_btn = new JButton("Find Projection (Black)");
		findProjection_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.WHITE_FILL;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {}
				findProjection = true;
			}
		});
		
		JCheckBox background_cbx = new JCheckBox("Use White Background");
		background_cbx.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(((JCheckBox)e.getSource()).isSelected()){
					findProjection_btn.setText("Find Projection (White)");
					whiteBackground = true;
				}else{
					findProjection_btn.setText("Find Projection (Black)");
					whiteBackground = false;
				}
			}
		});

		
		panels[0].add(findProjection_btn);
		panels[0].add(background_cbx);

		//Colors row
		JButton red_btn = new JButton("RED");
		red_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.RED_FILL;
			}
		});

		panels[1].add(red_btn);

		JButton green_btn = new JButton("GREEN");
		green_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.GREEN_FILL;
			}
		});

		panels[1].add(green_btn);

		JButton blue_btn = new JButton("BLUE");
		blue_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.BLUE_FILL;
			}
		});

		panels[1].add(blue_btn);

		JButton white_btn = new JButton("WHITE");
		white_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.WHITE_FILL;
			}
		});

		panels[1].add(white_btn);
		
		JButton checker_btn = new JButton("CHECKER");
		checker_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.CHECKER;
			}
		});

		panels[1].add(checker_btn);
		
		JButton texture_btn = new JButton("TEXTURE");
		texture_btn.setEnabled(false);
		texture_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawState = DrawState.TEXTURE;
			}
		});

		panels[1].add(texture_btn);
		
		JFileChooser fc = new JFileChooser();
		JButton load_btn = new JButton("Load Texture");
		load_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(fc.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
					File file = fc.getSelectedFile();
					if(file.isFile()){
						String name = file.getName();
						if(name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg")){
							try {
								BufferedImage img = ImageIO.read(file);
								texture = ocv_converter.convertToMat(img_converter.convert(img));
								texture_btn.setText("TEXTURE (" + name + ")");
								texture_btn.setEnabled(true);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		});

		panels[1].add(load_btn);
		
		//Mask Row
		JButton applyMask_btn = new JButton("Apply Mask");
		applyMask_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				saveMask = true;
			}
		});

		panels[2].add(applyMask_btn);
		
		JCheckBox AA_cbx = new JCheckBox("Anti-Aliasing");
		AA_cbx.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AA = ((JCheckBox)e.getSource()).isSelected();
			}
		});

		panels[2].add(AA_cbx);
		
		JButton applyInvertedMask_btn = new JButton("Apply Inverted Mask");
		applyInvertedMask_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				saveInvertedMask  = true;
			}
		});

		panels[2].add(applyInvertedMask_btn);
		
		JButton clearMask_btn = new JButton("Clear Mask");
		clearMask_btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				projectionMask = null;
			}
		});

		panels[2].add(clearMask_btn);
		
		panels[3].setLayout(new BoxLayout(panels[3], BoxLayout.Y_AXIS));
		
		addSliders(panels[3]);
	}
	
	public void addSliders(JPanel panel){
		JLabel red_lbl = new JLabel("Red: 0.0-255.0");
		RangeSlider red_sld = new RangeSlider(0, 255, 0, 255);
		red_sld.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				red_lbl.setText("Red: " + threshMin.red(red_sld.getValue()).red() + "-" + threshMax.red(red_sld.getUpperValue()).red());

			}
		});

		JLabel green_lbl = new JLabel("Green: 0.0-255.0");
		RangeSlider green_sld = new RangeSlider(0, 255, 0, 255);
		green_sld.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				green_lbl.setText("Green: " + threshMin.green(green_sld.getValue()).green() + "-" + threshMax.green(green_sld.getUpperValue()).green());
			}
		});

		JLabel blue_lbl = new JLabel("Blue: 0.0-255.0");
		RangeSlider blue_sld = new RangeSlider(0, 255, 0, 255);
		blue_sld.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				blue_lbl.setText("Blue: " + threshMin.blue(blue_sld.getValue()).blue() + "-" + threshMax.blue(blue_sld.getUpperValue()).blue());
			}
		});
		panel.add(red_lbl);
		panel.add(red_sld);
		panel.add(green_lbl);
		panel.add(green_sld);
		panel.add(blue_lbl);
		panel.add(blue_sld);
	}

	@Override
	public void processFrame(Frame img) {
		Mat img_mat = ocv_converter.convertToMat(img);
		IplImage img_ipl = ocv_converter.convert(img);

		if(findProjection){
			/*
			 * Projection Mapping
			 */
			Mat gray_mat = new Mat();

			//Show the original frame
			this.showImage(img);

			//Change to grayscale
			opencv_imgproc.cvtColor(img_mat, gray_mat, opencv_imgproc.CV_BGR2GRAY);
			this.showImage(ocv_converter.convert(gray_mat));
			//try {Thread.sleep(1000);} catch (InterruptedException e) {}

			//de-noise
			opencv_photo.fastNlMeansDenoising(gray_mat, gray_mat);
			this.showImage(ocv_converter.convert(gray_mat));

			//Smooth any jagged areas
			IplImage gray_ipl = ocv_converter.convert(ocv_converter.convert(gray_mat));
			opencv_imgproc.cvSmooth(gray_ipl, gray_ipl, opencv_imgproc.CV_MEDIAN, 3, 0, 0, 0);
			this.showImage(ocv_converter.convert(gray_ipl));

			//Threshold Filter
			IplImage mask_ipl = opencv_core.cvCreateImage(opencv_core.cvGetSize(img_ipl), 8, 1);
			if(whiteBackground){
				opencv_core.cvInRangeS(gray_ipl, cvScalar(160), cvScalar(255), mask_ipl);
			}else{
				opencv_core.cvInRangeS(gray_ipl, cvScalar(6), cvScalar(255), mask_ipl);
			}
			Mat final_mat = ocv_converter.convertToMat(ocv_converter.convert(mask_ipl));
			opencv_core.bitwise_and(gray_mat, gray_mat, final_mat, ocv_converter.convertToMat(ocv_converter.convert(mask_ipl)));
			gray_mat = final_mat;
			this.showImage(ocv_converter.convert(gray_mat));

			//increase the contrast
			if(!whiteBackground){
				opencv_imgproc.equalizeHist(gray_mat, gray_mat);
				this.showImage(ocv_converter.convert(gray_mat));
			}
			
			//Threshold Filter
			if(!whiteBackground){
				gray_ipl = ocv_converter.convert(ocv_converter.convert(gray_mat));
				mask_ipl = opencv_core.cvCreateImage(opencv_core.cvGetSize(img_ipl), 8, 1);
				opencv_core.cvInRangeS(gray_ipl, cvScalar(10), cvScalar(255), mask_ipl);
				gray_mat = ocv_converter.convertToMat(ocv_converter.convert(mask_ipl));
				this.showImage(ocv_converter.convert(gray_mat));
			}

			//Edge Detection
			Mat bw_mat = new Mat();
			opencv_imgproc.Canny(gray_mat, bw_mat, 30, 90, 3, true);
			this.showImage(ocv_converter.convert(bw_mat));

			//Find the contours
			MatVector contours = new MatVector();
			Mat approx_mat = new Mat();
			opencv_imgproc.findContours(bw_mat.clone(), contours, opencv_imgproc.CV_RETR_EXTERNAL, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);

			for(long i = 0; i < contours.size(); i++){
				Mat tmp_mat = new Mat(contours.get(i));
				
				// Approximate contour with accuracy proportional to the contour perimeter
				opencv_imgproc.approxPolyDP(tmp_mat, approx_mat, opencv_imgproc.arcLength(tmp_mat, true)*0.02, true);

				// Skip small or non-convex objects 
				if (Math.abs(opencv_imgproc.contourArea(tmp_mat)) < 100 || !opencv_imgproc.isContourConvex(approx_mat)){
					continue;
				}

				// Number of vertices of polygonal curve
				if (approx_mat.arrayHeight() == 4){
					projectionBounds = opencv_imgproc.boundingRect(contours.get(i));
					findProjection = false;
				}
			}
		}

		/*
		 * Find the mask
		 */

		//create binary image of original size
		IplImage mask_ipl = opencv_core.cvCreateImage(opencv_core.cvGetSize(img_ipl), 8, 1);

		//apply thresholding
		opencv_core.cvInRangeS(img_ipl, threshMin, threshMax, mask_ipl);

		//smooth filter the mask
		opencv_imgproc.cvSmooth(mask_ipl, mask_ipl, opencv_imgproc.CV_MEDIAN, 7, 0, 0, 0);

		Mat mask_mat = ocv_converter.convertToMat(ocv_converter.convert(mask_ipl));
		if(saveMask){
			Mat cropped_mat = new Mat(mask_mat, projectionBounds);
			projectionMask = ocv_converter.convertToIplImage(ocv_converter.convert(cropped_mat));
			saveMask = false;
		}else if(saveInvertedMask){
			Mat cropped_mat = new Mat(mask_mat, projectionBounds);
			opencv_core.bitwise_not(cropped_mat, cropped_mat);
			projectionMask = ocv_converter.convertToIplImage(ocv_converter.convert(cropped_mat));
			saveInvertedMask = false;
		}

		Mat final_mat = ocv_converter.convertToMat(ocv_converter.convert(mask_ipl));
		opencv_core.bitwise_and(img_mat, img_mat, final_mat, mask_mat);

		if(projectionBounds != null){
			opencv_core.rectangle(final_mat, projectionBounds, new Scalar(0,255,255,0));
		}

		this.showImage(ocv_converter.convert(final_mat));
		draw();
	}

	public void draw() {
		Dimension size = projectionFrame.getCanvas().getSize();
		int height = size.height;
		int width = size.width;
		Mat base_mat = new Mat(height, width, 16);

		switch(drawState){
		case TEXTURE:
			if(texture != null){
				base_mat = texture;
			}
			break;
		case CHECKER:
			//Color in the background to be all white
			opencv_core.rectangle(base_mat, new Rect(0, 0, width, height), new Scalar(255,255,255,0), opencv_core.CV_FILLED, 8, 0);
			
			//Draw 50x50px black squares to form the checkers.
			for(int y = 0; y < height; y+=50){
				for(int x = 0; x < width; x+=50){
					if((x/10+y/10) % 2 == 1)opencv_core.rectangle(base_mat, new Rect(x, y, 50, 50), new Scalar(0,0,0,0), opencv_core.CV_FILLED, 8, 0);
				}	
			}
			break;
		case RED_FILL:
			opencv_core.rectangle(base_mat, new Rect(0, 0, width, height), new Scalar(0,0,255,0), opencv_core.CV_FILLED, 8, 0);
			break;
		case GREEN_FILL:
			opencv_core.rectangle(base_mat, new Rect(0, 0, width, height), new Scalar(0,255,0,0), opencv_core.CV_FILLED, 8, 0);
			break;
		case BLUE_FILL:
			opencv_core.rectangle(base_mat, new Rect(0, 0, width, height), new Scalar(255,0,0,0), opencv_core.CV_FILLED, 8, 0);
			break;
		case WHITE_FILL:
			opencv_core.rectangle(base_mat, new Rect(0, 0, width, height), new Scalar(255,255,255,0), opencv_core.CV_FILLED, 8, 0);
			break;
		default:
			break;
		}

		Mat final_mat = new Mat(height, width, 16);

		if(projectionMask != null && projectionBounds != null){
			//Resize the mask to the screen size.
			Mat mask_mat = ocv_converter.convertToMat(ocv_converter.convert(projectionMask));
			Mat final_mask_mat = new Mat();
			opencv_imgproc.resize(mask_mat, final_mask_mat, new Size(width, height));
			
			//smooth filter the mask after the resize so it isn't jagged.
			IplImage final_mask_ipl = ocv_converter.convert(ocv_converter.convert(final_mask_mat));
			if(AA)opencv_imgproc.cvSmooth(final_mask_ipl, final_mask_ipl, opencv_imgproc.CV_MEDIAN, 13, 0, 0.0, 0.0);
			final_mask_mat = ocv_converter.convertToMat(ocv_converter.convert(final_mask_ipl));
			
			//Apply the mask to the projection area
			opencv_core.bitwise_and(base_mat, base_mat, final_mat, final_mask_mat);
			final_mask_mat.deallocate();
		}else{
			//And the image with itself and don't include the mask.
			opencv_core.bitwise_and(base_mat, base_mat, final_mat);
		}
		//Show the image on the projector
		projectionFrame.showImage(ocv_converter.convert(final_mat));
		
		//Clean up some memory.
		base_mat.deallocate();
		final_mat.deallocate();
	}

	private enum DrawState{
		CHECKER(),
		RED_FILL(),
		GREEN_FILL(),
		BLUE_FILL(),
		WHITE_FILL(),
		TEXTURE();
	}
}

package editor.addMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import editor.FileChooserInit;
import gcmDataAccess.GcmDAO;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import mainApp.GcmClient;
import maps.City;
import maps.Coordinates;
import maps.Map;
import utility.TextFieldUtility;

public class AddMapController implements Initializable
{
	private GcmDAO gcmDAO;
	
	@FXML
	TextField mapName;
	@FXML
	TextField mapDescription;
	@FXML
	TextField height;
	@FXML
	TextField width;
	@FXML
	TextField xOffset;
	@FXML
	TextField yOffset;
	@FXML
	TextField errors;
	@FXML
	Button uploadMap;
	@FXML
	Button addMap;
	
	File file;
	int cityId;
	int mapId;
	Image image;
	private FileChooser fileChooser;
	BufferedImage bufferedImage;
	TextFieldUtility utilities;
	GcmClient gcmClient;
	
	public AddMapController(GcmClient gcmClient, int cityId, int mapId, TextFieldUtility utilities) {
		this.gcmClient = gcmClient;
		this.gcmDAO = gcmClient.getDataAccessObject();
		fileChooser = new FileChooserInit().getFileChooser();
		this.cityId = cityId;
		this.mapId = mapId;
		this.utilities = utilities;
	}
	
	public void addMapListener() {	 
		addMap.setOnMouseClicked((new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	String name = mapName.getText();
	            	String description = mapDescription.getText();
	            	String mapHeight = height.getText();
	            	String mapWidth = width.getText();
	            	String xCoordinates = xOffset.getText();
	            	String yCoordinates = yOffset.getText();
	            	
	            	List<String> list = Arrays.asList(name, description, mapHeight, mapWidth, xCoordinates, yCoordinates);;
	            	if(utilities.checkFilledFields(list)) {
	            		List<String> numericList = Arrays.asList(mapHeight, mapWidth, xCoordinates, yCoordinates);
	            		String checkResult = utilities.areAllFieldsNumeric(numericList);
	            		if(checkResult.equals("yes")) {

		            		if(file != null) {
		            			errors.setVisible(false);
			            		Map newMap = new Map(name, description, Float.parseFloat(mapWidth), Float.parseFloat(mapHeight), new Coordinates(Float.parseFloat(xCoordinates), Float.parseFloat(yCoordinates)));
			            		gcmDAO.addMapToCity(cityId, newMap, file);
			            		gcmClient.back();
		            		}else {
		            			utilities.setErrors("No file added", errors);
		            		}
	            		}else {
	            			utilities.setErrors(checkResult + " is not numeric value!", errors);
	            		}
	            		
	            	}else {
	            		utilities.setErrors("Please fill all the fields", errors);
	            	}
	            }
			})
		);
	}

	public void uploadMapListener() {	
		uploadMap.setOnMouseClicked((new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) { 
	            	 //Show open file dialog
	                file = fileChooser.showOpenDialog(null);
	                if(file != null) {
	                	try {
							bufferedImage = ImageIO.read(file);
		                    image = SwingFXUtils.toFXImage(bufferedImage, null);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	            }
			})
		);
	}
	@FXML
	public void onBackButton() {
		gcmClient.back();
	}
	 
	public void initializeFields() {
		Map map = gcmClient.getDataAccessObject().getMapDetails(mapId);
		System.out.println(map);
		if(map != null) {
			mapName.setText(map.getName());
			mapDescription.setText(map.getName());
			height.setText(Float.toString(map.getHeight()));
			width.setText(Float.toString(map.getWidth()));
			xOffset.setText(Float.toString(map.getOffset().getX()));
			yOffset.setText(Float.toString(map.getOffset().getY()));
		}

	}
	
    /**
	* @param url
	* @param rb
	**/
    @Override 
	public void initialize(URL url, ResourceBundle rb) {
    	errors.setVisible(false);
    	initializeFields();
    	addMapListener();
    	uploadMapListener();
    }
    
    public void initalizeControl(int cityId) {
    	this.cityId = cityId;
    }
}
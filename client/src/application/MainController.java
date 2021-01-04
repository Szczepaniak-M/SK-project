package application;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class MainController implements Initializable {
	@FXML
	private Pane rootPane;
	@FXML
	private Button btnLogOut;
	@FXML
	private Button btnLoadMsgs;
	@FXML
	private Button btnAddChannel;
	@FXML
	private Button btnRemoveChannel;
	@FXML
	private Label currentChannel;
	@FXML
	private TextField tfMessage;
	@FXML
	private ListView<String> listViewMessages;
	
	private List<String> communicationContainer;
	private CommunicationThread communicationThread;
	private ObservableList<String> posts = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<String>()));
	private ObservableList<String> numberOfNewMsgs = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<String>()));
	private List<Channel> channels;
	private List<Button> buttons = new ArrayList<>();
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		channels = communicationThread.getChannels();
		generateChannelButtons(channels);
		communicationContainer.clear();				
		numberOfNewMsgs.addListener(new ListChangeListener<String>() {
			@Override
			public void onChanged(Change<? extends String> change) {				
				while(change.next()) {
					if(change.wasAdded()) {
						synchronized (numberOfNewMsgs) {
							Platform.runLater(() -> {
								btnLoadMsgs.setText("Load " + numberOfNewMsgs.get(0) + " new messages");
							});
						}						
					}									
				}
			}
		});
		numberOfNewMsgs.add("");
	}
	
	public void initData(CommunicationThread communicationThread, List<String> communicationContainer) {
		this.communicationThread = communicationThread;
		this.communicationContainer = communicationContainer;
		communicationThread.setObservables(posts, numberOfNewMsgs);		        
	}
	
	@FXML
	public void btnRemoveChannelClickListener() {
		List<String> names = new ArrayList<>();
		for(Channel c : channels) {
			names.add(c.getName());
		}
		Dialog dialog = new ChoiceDialog(names.get(0), names);
		dialog.setTitle("Remove Channel");
		dialog.setHeaderText("Please choose channel to unsubscribe");

		Optional<String> result = dialog.showAndWait();				
		if (result.isPresent()) {
			int index = -1;
			for(Button b : buttons) {
				if(b.getId().equals(result.get())) {
					index = buttons.indexOf(b);
					rootPane.getChildren().remove(b);
					break;
				}
			}		
			for(int i = 0; i < buttons.size(); i++) {
				if(i > index) {
					Button btn = buttons.get(i);
					btn.setLayoutY(btn.getLayoutY() - 40);
				}
			}
			buttons.remove(index);
			for(Channel c : channels) {
				if(c.getName().equals(result.get() )) {
					String request = c.getId().length() + ";5;" + c.getId();
					synchronized(communicationContainer) {
						communicationContainer.add(request);
					}
					channels.remove(c);
					break;
				}				
			}
		}		
	}
	
	@FXML
	public void logoutBtnOnClickListener() throws IOException {
		communicationContainer.add(";logout;");
		System.out.println("Log Out");
		//load login scene
    	Pane pane = FXMLLoader.load(getClass().getResource("LoginWindow.fxml"));
    	rootPane.getChildren().setAll(pane);    	
	}
	
	@FXML
	public void sendBtnOnClickListener() {
		String content = tfMessage.getText();
		String channelName = currentChannel.getText();
		if(!channelName.isEmpty() && !content.isEmpty()) 
			synchronized (communicationContainer) {
				communicationContainer.add("x;2;x;" + channelName + ";" + content);
			}
		tfMessage.setText("");
	}
	
	@FXML
	public void btnLoadMsgsListener() {
		String requestForNewMsgs = ";n;";
		posts.clear();
		synchronized (communicationContainer) {
			communicationContainer.add(requestForNewMsgs);
		}					
		printMessages();
	}
	
	private void generateChannelButtons(List<Channel> channels) {
		int startY = 220;
		int shiftY = 0;
		int positionX = 50;
		int height = 30;
		int width = 130;
		for(Channel c : channels) {
			String channelID = c.getId();
			String channelName = c.getName();
			Button b = new Button();
			b.setId(channelName);
			b.setLayoutX(positionX);
			b.setLayoutY(startY + shiftY);
			b.setPrefHeight(height);
			b.setPrefWidth(width);
			b.setMnemonicParsing(false);
			b.setText(channelName + " | [" + c.getObsList().get(0) + "]");
			c.getObsList().addListener(new ListChangeListener<Integer>() {
				@Override
				public void onChanged(Change<? extends Integer> change) {
					while(change.next()) {
						if(change.wasAdded()) {
							Platform.runLater(() -> {
								b.setText(channelName + " | [" + c.getObsList().get(0) + "]");
							});										
						}
					}
				}
			});			
			b.setOnAction(e -> {
				currentChannel.setText(channelName);				
				String requestForPosts = channelID.length() + ";8;" + channelID;
				posts.clear();
				synchronized (communicationContainer) {
					communicationContainer.add(requestForPosts);
				}					
				printMessages();
			});
			rootPane.getChildren().add(b);
			buttons.add(b);
			shiftY += 40;
		}
	}
	
	private void printMessages() {
		synchronized (posts) {
			int index = posts.size();
			listViewMessages.setItems(posts);
			listViewMessages.scrollTo(index);
		}
	}
}

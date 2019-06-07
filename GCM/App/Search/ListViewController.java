package Search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListViewController
{
	@FXML
    private ListView listView;
    private List<String> stringSet= new ArrayList<>(5);
    private ObservableList<String> observableList = FXCollections.observableArrayList();
   
    public ListViewController()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("listView.fxml"));
        fxmlLoader.setController(this);
        try
        {
            Parent parent = (Parent)fxmlLoader.load();
            Scene scene = new Scene(parent, 400.0 ,500.0);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setListView()
    {
        stringSet.add("String 1");
        stringSet.add("String 2");
        stringSet.add("String 3");
        stringSet.add("String 4");
        observableList.setAll(stringSet);
        
        listView.setItems(observableList);

        listView.setCellFactory(
                new Callback<ListView<String>, javafx.scene.control.ListCell<String>>() {
                    @Override
                    public ListCell<String> call(ListView<String> listView) {
                        return new ListViewCell();
                    }
                });
    }

    @FXML
    void initialize() {
        assert listView != null : "fx:id=\"listView\" was not injected: check your FXML file 'CustomList.fxml'.";

        setListView();
    }
}
package com.client;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class Client extends Application {

    private static final String SERVER_URL = "http://localhost:8080/contracts";

    private TableView<Contract> table = new TableView<>();

    @Override
    public void start(Stage primaryStage) {
        TableColumn<Contract, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Contract, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Contract, String> numberColumn = new TableColumn<>("Number");
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));

        TableColumn<Contract, String> lastUpdatedColumn = new TableColumn<>("Last Updated");
        lastUpdatedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdatedDate"));

        TableColumn<Contract, Boolean> checkBoxColumn = new TableColumn<>("Check");
        checkBoxColumn.setCellValueFactory(cellData -> {
            String dateStr = cellData.getValue().getDate();
            String lastUpdatedDateStr = cellData.getValue().getLastUpdatedDate();
            boolean isChecked = lastUpdatedDateStr != null && isWithin60Days(dateStr, lastUpdatedDateStr);
            return new SimpleBooleanProperty(isChecked);
        });
        checkBoxColumn.setCellFactory(col -> {
            CheckBox checkBox = new CheckBox();
            return new TableCell<Contract, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        checkBox.setSelected(item);
                        setGraphic(checkBox);
                    }
                }
            };
        });

        table.getColumns().addAll(idColumn, dateColumn, numberColumn, lastUpdatedColumn, checkBoxColumn);

        fetchContracts(); // Получаем данные с сервера


        Scene scene = new Scene(table, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Contracts Table");
        primaryStage.show();
    }
    private void fetchContracts() {
        try {
            StringBuilder response = getStringBuilder();

            Gson gson = new Gson();
            Contract[] contractsArray = gson.fromJson(response.toString(), Contract[].class);
            List<Contract> contracts = Arrays.asList(contractsArray);

            table.getItems().addAll(contracts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static StringBuilder getStringBuilder() throws IOException {
        URL url = new URL(SERVER_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response;
    }

    private boolean isWithin60Days(String dateStr, String lastUpdatedDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, formatter);
        LocalDate lastUpdatedDate = LocalDate.parse(lastUpdatedDateStr, formatter);
        long daysBetween = ChronoUnit.DAYS.between(date, lastUpdatedDate);
        return daysBetween < 60;
    }
    public static void main(String[] args) {
        launch(args);
    }
}

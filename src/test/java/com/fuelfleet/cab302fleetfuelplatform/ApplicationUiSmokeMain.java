package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.*;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

/** Opt-in real-window regression check. Run only from a fresh disposable directory. */
public final class ApplicationUiSmokeMain {
    private static Stage stage;
    private static int vehicleId;
    private static final CompletableFuture<Void> finished=new CompletableFuture<>();

    public static void main(String[] args) throws Exception {
        if(!Boolean.getBoolean("fleet.disposableDemo")) throw new IllegalStateException("Disposable demo flag required");
        DBManager.initialize();
        new ManagerRegistrationService().registerFirstManager("ui-manager","Ui-Test-Only!","Ui-Test-Only!");
        var users=new UserService();
        var driver=users.createUser("ui-driver","Ui-Test-Only!",Role.DRIVER);
        users.createUser("ui-spare","Ui-Test-Only!",Role.DRIVER);
        var vehicles=new VehicleService();
        var a=vehicles.createVehicle("UI,ONE","Synthetic","Car","Petrol","0");
        var b=vehicles.createVehicle("UI-TWO","Synthetic","Van","Diesel","0");
        vehicles.createVehicle("UI-FREE","Synthetic","Available","Petrol","0");
        vehicleId=a.id();
        var assignment=new VehicleAssignmentService();
        assignment.assignDriver(a.id(),driver.id());assignment.assignDriver(b.id(),driver.id());
        new MaintenanceService().scheduleService(a.id(),250);
        AppSession.getInstance().signIn(driver);
        var fuel=new FuelLogService();
        fuel.save(a.id(),LocalDate.now().minusDays(2),"30","2","100",true);
        fuel.save(a.id(),LocalDate.now().minusDays(1),"20","2","200",true);
        AppSession.getInstance().signOut();
        Platform.startup(()->{
            try {
                stage=new Stage();new HelloApplication().start(stage);
                login("ui-manager","wrong");
                check(label("statusLabel").getText().contains("Invalid"),"Invalid login must remain on login screen");
                login("ui-manager","Ui-Test-Only!");
                button("Vehicles").fire();
                check(columnValue("vehicleTable","nextServiceColumn",0).equals("250"),"Manager maintenance schedule missing");
                check(columnValue("vehicleTable","maintenanceStatusColumn",0).equals("DUE SOON"),"Manager maintenance status missing");
                button("Back to dashboard").fire();
                button("Drivers").fire();
                check(table("driverTable").getItems().size()==3,"Both assignments and spare driver must be shown");
                button("Back to dashboard").fire();button("Unassigned vehicles").fire();
                check(table("vehicleTable").getItems().size()==1,"Only unassigned vehicle expected");
                ((CheckBox)root().lookup("#unassignedOnlyBox")).fire();
                check(table("vehicleTable").getItems().size()==3,"All vehicle filter expected");
                button("Back to dashboard").fire();button("Profile").fire();
                field("nameField").setText("UI Manager");field("emailField").setText("ui@example.test");
                button("Save changes").fire();check(label("statusLabel").getText().equals("Profile saved."),"Profile did not save");
                button("Back to dashboard").fire();button("Profile").fire();
                check(field("nameField").getText().equals("UI Manager"),"Profile did not reload persisted name");
                button("Back to dashboard").fire();button("Reports").fire();
                check(table("comparisonTable").getItems().size()==3,"Vehicle comparison not wired");
                ((DatePicker)root().lookup("#fromDate")).getEditor().setText("bad-date");button("Apply / refresh").fire();
                check(label("status").getText().contains("Cannot load report"),"Invalid report date not rejected");
                ((DatePicker)root().lookup("#fromDate")).getEditor().clear();button("Apply / refresh").fire();
                check(!((LineChart<?,?>)root().lookup("#costChart")).getData().isEmpty(),"Cost chart missing");
                check(!((LineChart<?,?>)root().lookup("#emissionsChart")).getData().isEmpty(),"Emissions chart missing");
                check(!((LineChart<?,?>)root().lookup("#efficiencyChart")).getData().isEmpty(),"Efficiency chart missing");
                button("Back").fire();button("Sign out").fire();
                login("ui-driver","Ui-Test-Only!");
                check(columnValue("vehicleTable","nextServiceColumn",0).equals("250"),"Driver maintenance schedule missing");
                check(columnValue("vehicleTable","maintenanceStatusColumn",0).equals("DUE SOON"),"Driver maintenance status missing");
                button("My profile").fire();
                check(field("nameField").getText().isEmpty(),"Driver must not receive manager profile");
                button("Back to dashboard").fire();button("+ Add fuel log").fire();
                field("litresField").setText("10");field("pricePerLitreField").setText("2");field("odometerField").setText("150");
                button("Save fuel log").fire();check(label("statusLabel").getText().contains("Odometer"),"Low odometer not rejected");
                field("odometerField").setText("300");button("Save fuel log").fire();
                check(table("historyTable").getItems().size()==3,"Saved fuel not reflected");
                var delay=new PauseTransition(Duration.millis(200));
                delay.setOnFinished(event->Platform.runLater(ApplicationUiSmokeMain::finishFuelChecks));delay.play();
            } catch(Throwable failure){finished.completeExceptionally(failure);}
        });
        try {finished.get(30,TimeUnit.SECONDS);System.out.println("Application UI smoke passed: login, maintenance, live pages, profiles, reports, fuel validation/save/delete/CSV and readable columns.");}
        finally {Platform.exit();}
    }

    private static void finishFuelChecks(){
        try {
            root().applyCss();root().layout();
            var history=table("historyTable");
            check(history.getColumns().getFirst().getWidth()>=85,"Fuel date column squeezed below readable width: "+history.getColumns().getFirst().getWidth());
            history.getSelectionModel().selectFirst();
            answerNextConfirmation(ButtonType.CANCEL);button("Delete selected").fire();
            check(history.getItems().size()==3,"Cancelled delete removed data");
            answerNextConfirmation(ButtonType.OK);button("Delete selected").fire();
            check(table("historyTable").getItems().size()==2,"Confirmed delete did not refresh history");
            check(new com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao().findById(vehicleId).orElseThrow().currentOdometer()==300,"Deleting a log reduced vehicle odometer");
            check(new FuelLogService().exportHistoryToCsv().contains("\"UI,ONE\""),"Export did not escape CSV registration");
            try(var connection=DBManager.getConnection();var statement=connection.createStatement()){
                statement.executeUpdate("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type,full_tank) VALUES("+vehicleId+",'broken',10,20,250,'Petrol',1)");
            }
            button("Back").fire();
            check(columnValue("vehicleTable","nextServiceColumn",0).equals("250"),"Fuel changes removed maintenance schedule");
            check(columnValue("vehicleTable","maintenanceStatusColumn",0).equals("OVERDUE"),"Maintenance status did not follow saved odometer");
            button("+ Add fuel log").fire();
            check(new FuelLogService().listHistory().size()==2,"Corrupt history crashed or leaked invalid row");
            check(label("historyWarningLabel").isVisible()&&!label("historyWarningLabel").getText().isBlank(),"Corrupt history warning missing");
            finished.complete(null);stage.close();
        } catch(Throwable failure){finished.completeExceptionally(failure);}
    }

    private static void answerNextConfirmation(ButtonType type){
        Platform.runLater(()->{
            try {
                for(var window:List.copyOf(Window.getWindows())){
                    var pane=window.getScene()==null?null:window.getScene().lookup(".dialog-pane");
                    if(pane instanceof DialogPane dialog){((Button)dialog.lookupButton(type)).fire();return;}
                }
                throw new AssertionError("Delete confirmation dialog missing");
            } catch(Throwable failure){finished.completeExceptionally(failure);}
        });
    }
    private static void login(String name,String password){field("usernameField").setText(name);field("passwordField").setText(password);button("Log in").fire();}
    private static Parent root(){var current=stage.getScene().getRoot();current.applyCss();current.layout();return current;}
    private static TextInputControl field(String id){return (TextInputControl)root().lookup("#"+id);}
    private static Label label(String id){return (Label)root().lookup("#"+id);}
    private static TableView<?> table(String id){return (TableView<?>)root().lookup("#"+id);}
    private static Object columnValue(String tableId,String columnId,int index){return table(tableId).getColumns().stream().filter(column->columnId.equals(column.getId())).findFirst().orElseThrow(()->new AssertionError("Missing column: "+columnId)).getCellData(index);}
    private static Button button(String text){root().applyCss();return root().lookupAll(".button").stream().filter(node->node instanceof Button b&&b.getText().equals(text)).map(node->(Button)node).findFirst().orElseThrow(()->new AssertionError("Missing button: "+text));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}

package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.concurrent.*;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Opt-in visual smoke check; run only in a disposable working directory. */
public final class ReportsSmokeMain {
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("fleet.disposableDemo")) throw new IllegalStateException("Disposable demo flag required");
        DBManager.initialize();
        try (var c=DBManager.getConnection();var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO vehicles(id,registration,make,model,fuel_type) VALUES(901,'DEMO-PETROL','Synthetic','Car','Petrol'),(902,'DEMO-DIESEL','Synthetic','Van','Diesel')");
            s.executeUpdate("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type,full_tank) VALUES(901,'2026-07-01',40,80,1000,'Petrol',1),(901,'2026-07-20',30,60,1400,'Petrol',1),(901,'2026-08-20',35,72,1900,'Petrol',1),(901,'2026-09-15',32,65,2400,'Petrol',1),(902,'2026-07-01',50,110,1000,'Diesel',1),(902,'2026-08-01',45,95,1500,'Diesel',1),(902,'2026-09-01',48,102,2100,'Diesel',1)");
        }
        AppSession.getInstance().signIn(new User(901,"Synthetic demo manager",Role.MANAGER));
        var finished = new CompletableFuture<Void>();
        Platform.startup(() -> {
            try {
                Parent root = FXMLLoader.load(HelloApplication.class.getResource("reports.fxml"));
                Stage stage=new Stage(); stage.setScene(new Scene(root,900,600)); stage.show();
                root.applyCss();root.layout();
                var tabs=(TabPane)root.lookup(".tab-pane");
                for(int i=0;i<3;i++) {
                    tabs.getSelectionModel().select(i);root.applyCss();root.layout();
                    var chart=(LineChart<?,?>)tabs.getTabs().get(i).getContent();
                    if(chart.getData().isEmpty()) throw new AssertionError("Expected chart data");
                    var snap=root.snapshot(null,null);
                    BufferedImage png=new BufferedImage((int)snap.getWidth(),(int)snap.getHeight(),BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,snap.getPixelReader().getArgb(x,y));
                    ImageIO.write(png,"png",Path.of(args[0],"reports-"+i+".png").toFile());
                }
                ((DatePicker)root.lookup("#fromDate")).getEditor().setText("bad-date");
                root.lookupAll(".button").stream().filter(n -> n instanceof Button b && b.getText().equals("Apply / Refresh")).map(n -> (Button)n).findFirst().orElseThrow().fire();
                if(!((Label)root.lookup("#status")).getText().contains("Cannot load report")) throw new AssertionError("Invalid date accepted");
                ((DatePicker)root.lookup("#fromDate")).getEditor().clear();
                ((DatePicker)root.lookup("#fromDate")).setValue(java.time.LocalDate.of(2099,1,1));
                root.lookupAll(".button").stream().filter(n -> n instanceof Button b && b.getText().equals("Apply / Refresh")).map(n -> (Button)n).findFirst().orElseThrow().fire();
                if(!((Label)root.lookup("#status")).getText().contains("No valid fuel records")) throw new AssertionError("Empty data message missing");
                stage.close();finished.complete(null);
            } catch(Throwable failure) {finished.completeExceptionally(failure);}
        });
        try { finished.get(30,TimeUnit.SECONDS); System.out.println("Reports FXML, three populated charts, invalid-date handling, and empty-data handling passed."); }
        finally { Platform.exit(); }
    }
}
